/*
 * Copyright (C) 2025 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import dev.blackilykat.messages.PlaybackSessionUpdateMessage;
import dev.blackilykat.parsers.FlacFileParser;
import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import jnr.ffi.annotations.In;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.mpris.MPRISMP2None;
import org.mpris.MPRISMediaPlayer;
import org.mpris.Metadata;
import org.mpris.mpris.LoopStatus;
import org.mpris.mpris.PlaybackStatus;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static dev.blackilykat.messages.PlaybackSessionUpdateMessage.VOID_BUFFER;

import static dev.blackilykat.Main.LOGGER;

public class Audio {
    public static final int BUFFER_SIZE = 8800;

    public Track loadedTrack = null;
    public Track preLoadedTrack = null;

    public static Audio INSTANCE = null;
    boolean canPlay = true;

    public PlaybackSession currentSession;

    // any other configuration doesn't work
    // might try to make it configurable in the future but also that sounds like a pain to implement
    public final AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,
            16,
            2,
            4,
            44100,
            true);

    private Line.Info info = new DataLine.Info(
            SourceDataLine.class,
            audioFormat,
            BUFFER_SIZE);

    private SourceDataLine sourceDataLine;

    public AudioPlayingThread audioPlayingThread = new AudioPlayingThread(this);
    public final Object audioLock = new Object();

    ThreadPoolExecutor songLoadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    Future<?> latestSongLoadingFuture;

    ThreadPoolExecutor songPreLoadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    Future<?> latestSongPreLoadingFuture;

    public final Library library;

    public final MPRISMP2None mpris;

    public Audio(Library library) {
        this.library = library;
        currentSession = new PlaybackSession(this, 0);
        currentSession.register();
        try {
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat, BUFFER_SIZE);
            sourceDataLine.start();

            audioPlayingThread.start();

            LOGGER.info("Line buffer size: {}", sourceDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            canPlay = false;
            LOGGER.error("Can't play!", e);
        }

        mpris = maybeCreateMprisInstance();
    }

    public void startPlaying(PlaybackSessionUpdateMessage updateBuffer, Track track, boolean reset) {
        if(!canPlay) return;

        currentSession.setCurrentTrack(track);
        if(reset) {
            PlaybackSessionUpdateMessage buffer = new PlaybackSessionUpdateMessage(currentSession.id);
            currentSession.setPosition(buffer, 0, true);
            setPlaying(buffer, true);
            buffer.track = track.getFile().getName();
            buffer.time = Instant.now();
            if(ServerConnection.INSTANCE != null) {
                ServerConnection.INSTANCE.send(buffer);
            }
        }

        if(preLoadedTrack != track) {
            if(latestSongLoadingFuture != null) {
                latestSongLoadingFuture.cancel(true);
            }

            if(loadedTrack != track) {
                if(loadedTrack != null) {
                    loadedTrack.pcmData = null;
                }
                loadedTrack = track;

                latestSongLoadingFuture = songLoadingExecutor.submit(() -> {
                    try {
                        if(track != null) {
                            FlacFileParser.parse(track, this);
                            PlayBarWidget.timeBar.setMinimum(0);
                            PlayBarWidget.timeBar.setMaximum(track.pcmData.length);
                        } else {
                            PlayBarWidget.timeBar.setMinimum(0);
                            PlayBarWidget.timeBar.setMaximum(0);
                        }
                    } catch(Exception e) {
                        LOGGER.error("Unknown error", e);
                    }
                });
            }

            if(preLoadedTrack != null) {
                preLoadedTrack.pcmData = null;
            }
        } else {
            PlayBarWidget.timeBar.setMinimum(0);
            PlayBarWidget.timeBar.setMaximum(track != null ? track.pcmData != null ? track.pcmData.length : 0 : 0);
        }
        preLoadedTrack = null;
    }

    public void preLoad(Track track) {
        if(preLoadedTrack == track) return;

        if(preLoadedTrack != null) {
            preLoadedTrack.pcmData = null;
        }

        preLoadedTrack = track;

        if(latestSongPreLoadingFuture != null) {
            latestSongPreLoadingFuture.cancel(true);
        }

        latestSongPreLoadingFuture = songPreLoadingExecutor.submit(() -> {
            FlacFileParser.parse(track, this);
        });
    }

    public void setPlaying(PlaybackSessionUpdateMessage updateBuffer, boolean playing) {
        if (!canPlay) return;
        this.currentSession.setPlaying(updateBuffer, playing);
        this.currentSession.lastSharedPosition = this.currentSession.getPosition();
        this.currentSession.lastSharedPositionTime = Instant.now();
        PlayBarWidget.setPlaying(playing);
    }

    public void setCurrentSession(PlaybackSession session) {
        if(ServerConnection.INSTANCE != null) {
            if(this.currentSession.getOwnerId() == ServerConnection.INSTANCE.clientId) {
                this.currentSession.setOwnerId(null, -1);
            }
        }

        // avoid changing stuff null WHILE it's processing
        synchronized(this.audioLock) {
            if(ServerConnection.INSTANCE != null && session.getOwnerId() != ServerConnection.INSTANCE.clientId) {
                session.recalculatePosition(VOID_BUFFER, Instant.now());
            }
            this.currentSession = session;
            this.startPlaying(VOID_BUFFER, session.getCurrentTrack(), false);
            this.setPlaying(VOID_BUFFER, session.getPlaying());

            // update icons
            session.setShuffle(VOID_BUFFER, session.getShuffle());
            session.setRepeat(VOID_BUFFER, session.getRepeat());
        }
        Main.playBarWidget.repaint();

        Main.libraryFiltersWidget.reloadPanels();
        Main.libraryFiltersWidget.reloadElements();
    }

    public void reselectSession(boolean takeOwnership) {
        PlaybackSession toSelect = null;
        int highestRanking = -1;
        for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
            int currentRanking = session.getPrecedenceRanking();
            if(currentRanking > highestRanking) {
                toSelect = session;
                highestRanking = currentRanking;
            }
        }
        assert toSelect != null;
        this.setCurrentSession(toSelect);
        if(takeOwnership && toSelect.getOwnerId() == -1 && ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.loggedIn) {
            toSelect.setOwnerId(null, ServerConnection.INSTANCE.clientId);
        }

        library.reloadFilters(VOID_BUFFER);
        library.reloadSorting();
    }

    public void nextTrack() {
        if(currentSession == null) return;
        currentSession.setPlaying(VOID_BUFFER, false);
        Track next = currentSession.nextTrack();
        if(next != null) startPlaying(null, next, true);
    }

    public void preLoadNextTrack() {
        if(currentSession == null) return;
        Track next = currentSession.peekTrack();
        if(next != null) preLoad(next);
    }

    public void previousTrack() {
        if(currentSession == null) return;
        currentSession.setPlaying(null, false);
        Track previous = currentSession.previousTrack();
        if(previous != null) startPlaying(null, previous, true);
    }

    public static class AudioPlayingThread extends Thread {

        private final Audio audio;

        public AudioPlayingThread(Audio audio) {
            this.audio = audio;
        }

        @Override
        public void run() {
            try {
                int bufferSize = audio.sourceDataLine.getBufferSize();
                while (true) {
                    if(!audio.canPlay) return;

                    if(!audio.currentSession.getPlaying()) {
                        Thread.sleep(100);
                        continue;
                    }
                    Track currentTrack = audio.currentSession.getCurrentTrack();
                    if(currentTrack == null) {
                        audio.setPlaying(null, false);
                        continue;
                    }

                    if(currentTrack.pcmData == null) {
                        synchronized(currentTrack) {
                            currentTrack.wait();
                        }
                        continue;
                    }

                    byte[] buffer = new byte[bufferSize];
                    int wrote = 0;
                    synchronized(audio.audioLock) {
                        if(audio.currentSession.getPosition() >= currentTrack.pcmData.length - (audio.audioFormat.getSampleSizeInBits() * audio.audioFormat.getSampleRate() / 8)) {
                            audio.preLoadNextTrack();
                        }

                        if(audio.currentSession.getPosition() >= currentTrack.pcmData.length - 4) {
                            audio.nextTrack();
                            continue;
                        }

                        PlayBarWidget.timeBar.update();
                        if(audio.mpris != null) {
                            try {
                                double positionSeconds = audio.currentSession.getPosition() / audio.audioFormat.getSampleRate() / audio.audioFormat.getChannels() / audio.audioFormat.getSampleSizeInBits() * 8.0;
                                audio.mpris.setPosition((int) (positionSeconds * 1_000_000));
                            } catch(DBusException ignored) {}
                        }

                        for (int i = 0; i < bufferSize - 3 &&
                                i + audio.currentSession.getPosition() < currentTrack.pcmData.length - 3; i += 4) {
                            // RIFF is little endian...
                            int position = audio.currentSession.getPosition();

                            boolean justReconnectedAndOwnedSessionBefore = !audio.currentSession.acknowledgedByServer && audio.currentSession.getOwnerId() == ServerConnection.oldClientId;
                            boolean justCreatedSession = !audio.currentSession.acknowledgedByServer && audio.currentSession.getOwnerId() == -1;
                            // is ServerConnection.INSTANCE is null, the client never connected to the server in the first place
                            boolean ownsSession = ServerConnection.INSTANCE == null || audio.currentSession.getOwnerId() == ServerConnection.INSTANCE.clientId;
                            if (ownsSession || justReconnectedAndOwnedSessionBefore || justCreatedSession) {
                                // L
                                buffer[i + 1] = currentTrack.pcmData[position];
                                buffer[i] = currentTrack.pcmData[position + 1];

                                // R
                                buffer[i + 3] = currentTrack.pcmData[position + 2];
                                buffer[i + 2] = currentTrack.pcmData[position + 3];
                            }
                            audio.currentSession.setPosition(null, position + 4, false);
                            wrote += 4;
                        }
                    }
                    // do NOT move this in the synchronized block (it won't let go and will freeze
                    // the entire gui on ChangeSessionMenu)
                    audio.sourceDataLine.write(buffer, 0, wrote);

                }
            } catch(InterruptedException e) {
                LOGGER.error("Interrupted", e);
            } catch(Exception e) {
                LOGGER.error("Unknown exception", e);
            }
            audio.sourceDataLine.drain();
            audio.sourceDataLine.close();
        }
    }

    public static boolean isSupported(File file) {
        return file.getName().endsWith(".flac");
    }

    public MPRISMP2None maybeCreateMprisInstance() {
        MPRISMP2None toReturn;
        try {
            MPRISMediaPlayer mprisMediaPlayer = new MPRISMediaPlayer(null, "pmpdesktop");
            toReturn = mprisMediaPlayer.buildMPRISMediaPlayer2None(
                    new MPRISMediaPlayer.MediaPlayer2Builder()
                            .setIdentity("PMP Desktop"),
                    new MPRISMediaPlayer.PlayerBuilder()
                            .setPlaybackStatus(PlaybackStatus.STOPPED)
                            .setLoopStatus(LoopStatus.NONE)
                            .setRate(1.0)
                            .setShuffle(false)
                            .setMetadata(new Metadata.Builder().setTrackID(new DBusPath("/")).setLength(0).build())
                            .setVolume(1.0)
                            .setPosition(0)
                            .setMinimumRate(1.0)
                            .setMaximumRate(1.0)
                            .setCanGoNext(true)
                            .setCanGoPrevious(true)
                            .setCanPlay(true)
                            .setCanPause(true)
                            .setCanSeek(true)
                            .setCanControl(true)
                            .setOnPlayPause(value -> {
                                if(currentSession == null) {
                                    throw new IllegalStateException();
                                }
                                currentSession.setPlaying(null, !currentSession.getPlaying());
                            })
                            .setOnPlay(value -> {
                                if(currentSession == null) {
                                    throw new IllegalStateException();
                                }
                                currentSession.setPlaying(null, true);
                            })
                            .setOnPause(value -> {
                                if(currentSession == null) {
                                    throw new IllegalStateException();
                                }
                                currentSession.setPlaying(null, false);
                            })
                            .setOnNext(value -> {
                                if(currentSession == null) {
                                    throw new IllegalStateException();
                                }
                                this.nextTrack();
                            })
                            .setOnPrevious(value -> {
                                if(currentSession == null) {
                                    throw new IllegalStateException();
                                }
                                this.previousTrack();
                            })
            );
            mprisMediaPlayer.create();
            PlaybackSession.SessionListener listener = session -> {
                if(session.audio.currentSession != session) return;
                MPRISMP2None mpris = session.audio.mpris;

                try {

                    Track track = session.getCurrentTrack();

                    if(track != null) {
                        //METADATA
                        Map<String, List<String>> metadataMap = new HashMap<>();
                        for(Pair<String, String> metadatum : track.metadata) {
                            List<String> existingList = metadataMap.get(metadatum.key.toLowerCase());
                            if(existingList == null) {
                                metadataMap.put(metadatum.key.toLowerCase(), new ArrayList<>(List.of(metadatum.value)));
                            } else {
                                existingList.add(metadatum.value);
                            }
                        }
                        double seconds;
                        if(track.pcmData != null) {
                            seconds = track.pcmData.length / audioFormat.getSampleRate() / audioFormat.getChannels() / audioFormat.getSampleSizeInBits() * 8.0;
                        } else {
                            seconds = 0;
                        }
                        mpris.setMetadata(new Metadata.Builder()
                                .setTrackID(new DBusPath("/PMPDesktop/" + track.getFile().getName().replace(".flac", "").replaceAll("[^A-Za-z0-9_]", "")))
                                .setLength((int) (seconds * 1_000_000))
                                .setArtURL(track.getFile().toURI())
                                .setXesamMetadata(metadataMap)
                                .build());
                    }

                    // PLAYBACK STATUS
                    if(track == null) {
                        mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                    } else if(session.getPlaying() && track.pcmData != null) {
                        mpris.setPlaybackStatus(PlaybackStatus.PLAYING);
                    } else {
                        mpris.setPlaybackStatus(PlaybackStatus.PAUSED);
                    }

                } catch(DBusException e) {
                    LOGGER.error("Unknown DBUS error", e);
                }
            };

            for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
                session.registerUpdateListener(listener);
            }
            PlaybackSession.registerRegisterListener(session -> {
                session.registerUpdateListener(listener);
            });
        } catch(DBusException e) {
            return null;
        }
        return toReturn;
    }
}
