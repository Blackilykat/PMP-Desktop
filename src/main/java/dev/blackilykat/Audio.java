/*
 * PMP-Desktop - A desktop client for Personal Music Platform, a
 * self-hosted platform to play music and make sure everything is
 * always synced across devices.
 * Copyright (C) 2024 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import dev.blackilykat.messages.PlaybackSessionCreateMessage;
import dev.blackilykat.messages.PlaybackSessionUpdateMessage;
import dev.blackilykat.parsers.FlacFileParser;
import dev.blackilykat.widgets.playbar.PlayBarWidget;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Audio {

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
            2200);

    private SourceDataLine sourceDataLine;

    public AudioPlayingThread audioPlayingThread = new AudioPlayingThread(this);
    public final Object audioLock = new Object();

    ThreadPoolExecutor songLoadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    Future<?> latestSongLoadingFuture;

    public Audio(Library library) {
        currentSession = new PlaybackSession(library, 0);
        currentSession.register();
        if (ServerConnection.INSTANCE != null) {
            ServerConnection.INSTANCE.send(new PlaybackSessionCreateMessage(0, null));
            currentSession.setOwnerId(ServerConnection.INSTANCE.clientId);
        }
        ServerConnection.addConnectListener(conn -> {
            ServerConnection.INSTANCE.send(new PlaybackSessionCreateMessage(0, null));
            if (currentSession.getOwnerId() == -1)
                currentSession.setOwnerId(conn.clientId);
        });
        try {
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat, 2200);
            sourceDataLine.start();

            audioPlayingThread.start();

            System.out.println(sourceDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            canPlay = false;
            System.out.println("Can't play!");
        }
    }

    public void startPlaying(Track track, boolean reset) {
        if(latestSongLoadingFuture != null) {
            boolean thing = latestSongLoadingFuture.cancel(true);
            System.out.println("THING: " + thing);
        }
        latestSongLoadingFuture = songLoadingExecutor.submit(() -> {
            if (!canPlay) return;
            // reset is true when the action was caused by this client (so send it to the server) and false when it was caused by the server
            if(reset) {
                PlaybackSessionUpdateMessage.doUpdate(currentSession.id, track.getFile().getName(), null, null, true, 0, null);
            }
            PlaybackSessionUpdateMessage.messageBuffer = new PlaybackSessionUpdateMessage(currentSession.id, null, null,
                    null, null, null, null, Instant.now());
            try {
                currentSession.setCurrentTrack(track);
                if (reset) {
                    currentSession.setPosition(0, true);
                    setPlaying(true);
                    if(ServerConnection.INSTANCE != null) {
                        ServerConnection.INSTANCE.send(PlaybackSessionUpdateMessage.messageBuffer);
                    }
                }
                if(track != null) {
                    reloadSong();
                    PlayBarWidget.timeBar.setMinimum(0);
                    PlayBarWidget.timeBar.setMaximum(track.pcmData.length);
                } else {
                    PlayBarWidget.timeBar.setMinimum(0);
                    PlayBarWidget.timeBar.setMaximum(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            PlaybackSessionUpdateMessage.messageBuffer = null;
        });

    }

    public boolean reloadSong() {
        return FlacFileParser.parse(currentSession.getCurrentTrack().getFile(), this);
    }

    public void setPlaying(boolean playing) {
        if (!canPlay) return;
        this.currentSession.setPlaying(playing);
        this.currentSession.lastSharedPosition = this.currentSession.getPosition();
        this.currentSession.lastSharedPositionTime = Instant.now();
        PlayBarWidget.setPlaying(playing);
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
                        audio.setPlaying(false);
                        continue;
                    }

                    if(currentTrack.pcmData == null) {
                        synchronized(currentTrack) {
                            currentTrack.wait();
                        }
                        continue;
                    }

                    byte[] buffer = new byte[bufferSize];
                    synchronized(audio.audioLock) {

                        if(audio.currentSession.getPosition() >= currentTrack.pcmData.length - 4) {
                            audio.setPlaying(false);
                            audio.currentSession.nextTrack();
                            audio.startPlaying(audio.currentSession.getCurrentTrack(), true);
                            continue;
                        }

                        PlayBarWidget.timeBar.update();

                        for (int i = 0; i < bufferSize - 3 &&
                                i + audio.currentSession.getPosition() < currentTrack.pcmData.length - 3; i += 4) {
                            // RIFF is little endian...
                            int position = audio.currentSession.getPosition();

                            if (ServerConnection.INSTANCE == null || (audio.currentSession.getOwnerId() == ServerConnection.INSTANCE.clientId)) {
                                // System.out.println("AAAAAAA " + audio.currentSession.getOwnerId() + " " +
                                // (ServerConnection.INSTANCE != null ? ServerConnection.INSTANCE.clientId :
                                // "no"));
                                // L
                                buffer[i + 1] = currentTrack.pcmData[position];
                                buffer[i] = currentTrack.pcmData[position + 1];

                                // R
                                buffer[i + 3] = currentTrack.pcmData[position + 2];
                                buffer[i + 2] = currentTrack.pcmData[position + 3];
                            }
                            audio.currentSession.setPosition(position + 4, false);
                        }
                    }
                    // do NOT move this in the synchronized block (it won't let go and will freeze
                    // the entire gui on ChangeSessionMenu)
                    audio.sourceDataLine.write(buffer, 0, bufferSize);

                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            audio.sourceDataLine.drain();
            audio.sourceDataLine.close();
        }
    }

    public static boolean isSupported(File file) {
        return file.getName().endsWith(".flac");
    }
}
