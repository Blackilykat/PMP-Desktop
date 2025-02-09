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

public class FlacFileParser implements PCMProcessor {
    public Audio audio;
    public Metadata[] metadata;
    public StreamInfo streamInfo;
    public int bytesProcessed = 0;
    public FlacFileParser(Audio audio) {
        this.audio = audio;
    }

    public static boolean parse(File file, Audio audio) {
        try {
            FLACDecoder decoder = new FLACDecoder(new FileInputStream(file));
            FlacFileParser instance = new FlacFileParser(audio);
            decoder.addPCMProcessor(instance);
            instance.metadata = decoder.readMetadata();
            audio.currentSession.getCurrentTrack().loaded = 0;
            try {
                decoder.decodeFrames();
            } catch (EOFException ignored) {}
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void processStreamInfo(StreamInfo streamInfo) {
        Track currentTrack = audio.currentSession.getCurrentTrack();
        long unprocessedLength = streamInfo.getTotalSamples() * streamInfo.getBitsPerSample() * streamInfo.getChannels() / 8;
        currentTrack.pcmData = new byte[((Double)(unprocessedLength * (2.0/streamInfo.getBitsPerSample()*8) * ((double) audio.audioFormat.getChannels()/streamInfo.getChannels()) * (audio.audioFormat.getSampleRate() / streamInfo.getSampleRate()))).intValue()];
        synchronized(currentTrack) {
            currentTrack.notifyAll();
        }
        this.streamInfo = streamInfo;
        System.out.println("Streaminfo: " + streamInfo.toString());
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
        Track currentTrack = audio.currentSession.getCurrentTrack();

        System.arraycopy(parsedData, 0, currentTrack.pcmData, bytesProcessed, parsedData.length);
        bytesProcessed += parsedData.length;
        currentTrack.loaded = bytesProcessed;
        PlayBarWidget.timeBar.repaint();
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
