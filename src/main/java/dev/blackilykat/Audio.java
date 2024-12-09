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
import dev.blackilykat.parsers.WavFileParser;
import dev.blackilykat.widgets.playbar.PlayBarWidget;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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

    public static final Audio INSTANCE = new Audio();
    boolean canPlay = true;

    private Path currentlyPlayingPath = null;
    private boolean playing = false;
    private int position = 0;

    public TrackQueueManager queueManager = new TrackQueueManager(Library.INSTANCE);

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

    private AudioPlayingThread audioPlayingThread = new AudioPlayingThread(
        this
    );

    ThreadPoolExecutor songLoadingExecutor  = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    Future<?> latestSongLoadingFuture;

    public Audio() {
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

    public void startPlaying(String path) {
        if(latestSongLoadingFuture != null) {
            boolean thing = latestSongLoadingFuture.cancel(true);
            System.out.println("THING: " + thing);
        }
        latestSongLoadingFuture = songLoadingExecutor.submit(() -> {
            if (!canPlay) return;
            try {
                currentlyPlayingPath = Path.of(path);
                if (
                        !currentlyPlayingPath.toFile().exists() ||
                                currentlyPlayingPath.toFile().isDirectory()
                ) {
                    currentlyPlayingPath = null;
                    return;
                }
                position = 0;
                setPlaying(true);
                reloadSong();
                PlayBarWidget.timeBar.setMinimum(0);
                PlayBarWidget.timeBar.setMaximum(queueManager.currentTrack.pcmData.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public boolean reloadSong() throws IOException {
        if(currentlyPlayingPath.toString().endsWith(".wav")) {
            return WavFileParser.parse(currentlyPlayingPath.toFile(), this);
        } else if(currentlyPlayingPath.toString().endsWith(".flac")) {
            return FlacFileParser.parse(currentlyPlayingPath.toFile(), this);
        }
        return false;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (!canPlay) return;
        this.position = position;
    }

    public boolean getPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        if (!canPlay) return;
        this.playing = playing;
        PlayBarWidget.setPlaying(playing);
    }


    private class AudioPlayingThread extends Thread {

        private final Audio instance;

        public AudioPlayingThread(Audio instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            try {
                int bufferSize = instance.sourceDataLine.getBufferSize();
                while (true) {
                    if (!instance.canPlay) return;
                    if (!instance.getPlaying()) {
                        Thread.sleep(100);
                        continue;
                    }
                    if(instance.queueManager.currentTrack == null) {
                        setPlaying(false);
                        continue;
                    }
                    if(instance.queueManager.currentTrack.pcmData == null) {
                        synchronized(instance.queueManager.currentTrack) {
                            instance.queueManager.currentTrack.wait();
                        }
                        continue;
                    }
                    if (instance.position >= instance.queueManager.currentTrack.pcmData.length-4) {
                        instance.setPlaying(false);
                        queueManager.nextTrack();
                        System.out.println("hi");
                        if(queueManager.currentTrack != null) {
                            System.out.println("hi");
                            startPlaying(queueManager.currentTrack.getFile().getPath());
                        }
                        continue;
                    }

                    PlayBarWidget.timeBar.update();

                    byte[] buffer = new byte[bufferSize];
                    for (
                        int i = 0;
                        i < bufferSize - 3 &&
                                i + instance.position < instance.queueManager.currentTrack.pcmData.length - 3;
                        i += 4
                    ) {
                        // RIFF is little endian...

                        // L
                        buffer[i + 1] = instance.queueManager.currentTrack.pcmData[instance.position];
                        buffer[i] = instance.queueManager.currentTrack.pcmData[instance.position + 1];

                        // R
                        buffer[i + 3] = instance.queueManager.currentTrack.pcmData[instance.position + 2];
                        buffer[i + 2] = instance.queueManager.currentTrack.pcmData[instance.position + 3];
                        instance.position += 4;
                    }
                    instance.sourceDataLine.write(buffer, 0, bufferSize);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sourceDataLine.drain();
            sourceDataLine.close();
        }
    }

    public static boolean isSupported(File file) {
        return file.getName().endsWith(".wav") || file.getName().endsWith(".flac");
    }
}
