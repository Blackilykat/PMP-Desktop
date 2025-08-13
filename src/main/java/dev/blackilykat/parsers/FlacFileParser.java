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

package dev.blackilykat.parsers;

import dev.blackilykat.Audio;
import dev.blackilykat.Main;
import dev.blackilykat.Track;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static dev.blackilykat.Main.LOGGER;

public class FlacFileParser implements PCMProcessor {
    public final Audio audio;
    public final Track track;
    public StreamInfo streamInfo;

    public int bytesProcessed = 0;
    public FlacFileParser(Audio audio, Track track) {
        this.audio = audio;
        this.track = track;
    }

    public static boolean parse(Track track, Audio audio) {
        try {
            FLACDecoder decoder = new FLACDecoder(new FileInputStream(track.getFile()));
            FlacFileParser instance = new FlacFileParser(audio, track);
            decoder.addPCMProcessor(instance);
            decoder.readMetadata();
            track.loaded = 0;
            try {
                decoder.decodeFrames();
            } catch (EOFException ignored) {}
        } catch (IOException e) {
            LOGGER.error("IO error", e);
            return false;
        }
        return true;
    }

    @Override
    public void processStreamInfo(StreamInfo streamInfo) {
        long unprocessedLength = streamInfo.getTotalSamples() * streamInfo.getBitsPerSample() * streamInfo.getChannels() / 8;
        track.pcmData = new byte[((Double)(unprocessedLength * (2.0/streamInfo.getBitsPerSample()*8) * ((double) audio.audioFormat.getChannels()/streamInfo.getChannels()) * (audio.audioFormat.getSampleRate() / streamInfo.getSampleRate()))).intValue()];
        synchronized(track) {
            track.notifyAll();
        }
        this.streamInfo = streamInfo;
        LOGGER.info("Streaminfo: {}", streamInfo);

        if(audio.currentSession != null) {
            audio.currentSession.callUpdateListeners();
        }
    }


    @Override
    public void processPCM(ByteData pcm) {
        // only way to make sure it stops loading. Doesn't seem like it causes issues
        if(Thread.interrupted()) {
            throw new RuntimeException(new InterruptedException());
        }
        byte[] pcmData = pcm.getData();

        int numChannels = streamInfo.getChannels();
        int bytesPerSample = streamInfo.getBitsPerSample()/8;
        int pcmChannelLength = pcm.getLen() / bytesPerSample / numChannels;
        int parsedDataLength = ((Double)(pcm.getLen() * (2.0/bytesPerSample) * ((double) audio.audioFormat.getChannels()/numChannels) * (audio.audioFormat.getSampleRate() / streamInfo.getSampleRate()))).intValue();
        // I don't know why this happens but sometimes the length is over by like half a sample?? so like everything
        // gets messed up audio is clicky channels get mixed up and fun stuff like that
        parsedDataLength -= parsedDataLength % 4;
        byte[] parsedData = new byte[parsedDataLength];

        for(int i = 0; i < 2; i++) {
            short[] pcmChannel = new short[pcmChannelLength];

            switch(bytesPerSample) {
                case 1 -> {
                    for(int j = i & (numChannels-1), l = 0; l < pcmChannelLength; j+=numChannels, l++) {
                        // this mess is because 8 bit pcm is unsigned
                        pcmChannel[l] = (short) ((((pcmData[j] & 0xFF) << 7) - (Short.MAX_VALUE >> 1)) << 1);
                    }
                }
                case 2 -> {
                    for(int j = (i & (numChannels-1)) * bytesPerSample, l = 0; l < pcmChannelLength; j+=numChannels*bytesPerSample, l++){
                        pcmChannel[l] = asShort(pcmData[j], pcmData[j+1], true);
                    }
                }
                case 3 -> {
                    for(int j = (i & (numChannels-1)) * bytesPerSample, l = 0; l < pcmChannelLength; j+=numChannels*bytesPerSample, l++){
                        pcmChannel[l] = asShort(pcmData[j], pcmData[j+1], pcmData[j+2], true);
                    }
                }
                case 4 -> {
                    for(int j = (i & (numChannels-1)) * bytesPerSample, l = 0; l < pcmChannelLength; j+=numChannels*bytesPerSample, l++){
                        pcmChannel[l] = asShort(pcmData[j], pcmData[j+1], pcmData[j+2], pcmData[j+3], true);
                    }
                }
            }

            if(streamInfo.getSampleRate() == (int) audio.audioFormat.getSampleRate()) {
                for(int j = 0; j < pcmChannelLength; j++) {
                    byte[] sample = asBytes(pcmChannel[j], true);
                    parsedData[(j*4) + (i*2)] = sample[0];
                    parsedData[(j*4) + (i*2) + 1] = sample[1];
                }
            } else {
                int last = (int) ((pcmChannelLength) / (streamInfo.getSampleRate() / audio.audioFormat.getSampleRate()) - 1);
                for(int j = 0; j <= last; j++) {
                    double positionSeconds = j / audio.audioFormat.getSampleRate();
                    double ogSamplePosition = positionSeconds * streamInfo.getSampleRate();
                    int ogSampleFloored = (int) Math.floor(ogSamplePosition);
                    double ogSampleMidPercentage = ogSamplePosition - ogSampleFloored;
                    short averagedSample = (short) ((pcmChannel[ogSampleFloored] * (1 - ogSampleMidPercentage)) + (pcmChannel[ogSampleFloored + 1] * ogSampleMidPercentage));
                    byte[] sample = asBytes(averagedSample, true);
                    parsedData[(j * 4) + (i * 2)] = sample[0];
                    parsedData[(j * 4) + (i * 2) + 1] = sample[1];
                }
            }
        }

        System.arraycopy(parsedData, 0, track.pcmData, bytesProcessed, parsedData.length);
        bytesProcessed += parsedData.length;
        track.loaded = bytesProcessed;
        PlayBarWidget.timeBar.update();
    }

    private static short asShort(byte a, byte b, boolean littleEndian) {
        if(littleEndian) {
            return (short) (a & 0xFF | ((b & 0xFF) << 8));
        } else {
            return (short) (b & 0xFF | ((a & 0xFF) << 8));
        }
    }

    private static short asShort(byte a, byte b, byte c, boolean littleEndian) {
        if(littleEndian) {
            return (short) ((a & 0xFF | ((b & 0xFF) << 8) | ((c & 0xFF) << 16)) >> 8);
        } else {
            return (short) ((c & 0xFF | ((b & 0xFF) << 8) | ((a & 0xFF) << 16)) >> 8);
        }
    }

    private static short asShort(byte a, byte b, byte c, byte d, boolean littleEndian) {
        if(littleEndian) {
            return (short) ((a & 0xFF | ((b & 0xFF) << 8) | ((c & 0xFF) << 16) | ((d & 0xFF) << 24)) >> 16);
        } else {
            return (short) ((d & 0xFF | ((c & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24)) >> 16);
        }
    }

    private static byte[] asBytes(int a, boolean littleEndian) {
        byte[] value = new byte[2];
        if(littleEndian) {
            value[0] = (byte) (a & 0xFF);
            value[1] = (byte) ((a >> 8) & 0xFF);
        } else {
            value[0] = (byte) ((a >> 8) & 0xFF);
            value[1] = (byte) (a & 0xFF);
        }
        return value;
    }
}
