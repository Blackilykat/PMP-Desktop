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
    public int loaded = 0;

    public byte[] song = new byte[0];

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
                PlayBarWidget.timeBar.setMaximum(song.length);
            } catch (InvalidPathException | IOException ignored) {}
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
                    if (instance.position >= instance.song.length-4) {
                        instance.setPlaying(false);
                        int index = Library.INSTANCE.findIndex(instance.currentlyPlayingPath.toFile());
                        Library.Track newSong = Library.INSTANCE.getItem(index+1);
                        if(newSong != null) {
                            startPlaying(newSong.getFile().getPath());
                        }
                        continue;
                    }

                    PlayBarWidget.timeBar.update();

                    byte[] buffer = new byte[bufferSize];
                    for (
                        int i = 0;
                        i < bufferSize - 3 &&
                                i + instance.position < instance.song.length - 3;
                        i += 4
                    ) {
                        // RIFF is little endian...

                        // L
                        buffer[i + 1] = instance.song[instance.position];
                        buffer[i] = instance.song[instance.position + 1];

                        // R
                        buffer[i + 3] = instance.song[instance.position + 2];
                        buffer[i + 2] = instance.song[instance.position + 3];
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
