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

import dev.blackilykat.parsers.FlacFileParser;
import dev.blackilykat.widgets.playbar.PlayBarWidget;

import java.io.File;
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
        true
    );

    private Line.Info info = new DataLine.Info(
        SourceDataLine.class,
        audioFormat,
        2200
    );

    private SourceDataLine sourceDataLine;

    public AudioPlayingThread audioPlayingThread = new AudioPlayingThread(this);
    public final Object audioLock = new Object();

    ThreadPoolExecutor songLoadingExecutor  = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    Future<?> latestSongLoadingFuture;

    public Audio(Library library) {
        currentSession = new PlaybackSession(library);
        currentSession.register();
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

    public void startPlaying(Track track, boolean fromStart, boolean unpause) {
        if(latestSongLoadingFuture != null) {
            boolean thing = latestSongLoadingFuture.cancel(true);
            System.out.println("THING: " + thing);
        }
        latestSongLoadingFuture = songLoadingExecutor.submit(() -> {
            if (!canPlay) return;
            try {
                currentSession.queueManager.currentTrack = track;
                if(fromStart) {
                    currentSession.setPosition(0, true);
                }
                if(unpause) {
                    setPlaying(true);
                }
                if(track != null) {
                    reloadSong();
                    PlayBarWidget.timeBar.setMinimum(0);
                    PlayBarWidget.timeBar.setMaximum(currentSession.queueManager.currentTrack.pcmData.length);
                } else {
                    PlayBarWidget.timeBar.setMinimum(0);
                    PlayBarWidget.timeBar.setMaximum(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public boolean reloadSong() {
        return FlacFileParser.parse(currentSession.queueManager.currentTrack.getFile(), this);
    }

    public void setPlaying(boolean playing) {
        if (!canPlay) return;
        this.currentSession.setPlaying(playing);
        PlayBarWidget.setPlaying(playing);
    }


    public class AudioPlayingThread extends Thread {

        private final Audio instance;

        public AudioPlayingThread(Audio instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            try {
                int bufferSize = instance.sourceDataLine.getBufferSize();
                while (true) {
                    if(!instance.canPlay) return;

                    if(!instance.currentSession.getPlaying()) {
                        Thread.sleep(100);
                        continue;
                    }
                    if(instance.currentSession.queueManager.currentTrack == null) {
                        setPlaying(false);
                        continue;
                    }

                    if(instance.currentSession.queueManager.currentTrack.pcmData == null) {
                        synchronized(instance.currentSession.queueManager.currentTrack) {
                            instance.currentSession.queueManager.currentTrack.wait();
                        }
                        continue;
                    }

                    byte[] buffer = new byte[bufferSize];
                    synchronized(instance.audioLock) {

                        if(instance.currentSession.getPosition() >= instance.currentSession.queueManager.currentTrack.pcmData.length - 4) {
                            instance.setPlaying(false);
                            currentSession.queueManager.nextTrack();
                            if(currentSession.queueManager.currentTrack != null) {
                                startPlaying(currentSession.queueManager.currentTrack, true, true);
                            }
                            continue;
                        }

                        PlayBarWidget.timeBar.update();

                        for(
                                int i = 0;
                                i < bufferSize - 3 &&
                                        i + instance.currentSession.getPosition() < instance.currentSession.queueManager.currentTrack.pcmData.length - 3;
                                i += 4
                        ) {
                            // RIFF is little endian...
                            int position = instance.currentSession.getPosition();
                            // L
                            buffer[i + 1] = instance.currentSession.queueManager.currentTrack.pcmData[position];
                            buffer[i] = instance.currentSession.queueManager.currentTrack.pcmData[position + 1];

                            // R
                            buffer[i + 3] = instance.currentSession.queueManager.currentTrack.pcmData[position + 2];
                            buffer[i + 2] = instance.currentSession.queueManager.currentTrack.pcmData[position + 3];
                            instance.currentSession.setPosition(position + 4, false);
                        }
                    }
                    // do NOT move this in the synchronized block (it won't let go and will freeze the entire gui on ChangeSessionMenu)
                    instance.sourceDataLine.write(buffer, 0, bufferSize);

                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            sourceDataLine.drain();
            sourceDataLine.close();
        }
    }

    public static boolean isSupported(File file) {
        return file.getName().endsWith(".flac");
    }
}
