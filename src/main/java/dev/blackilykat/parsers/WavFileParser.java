package dev.blackilykat.parsers;

import dev.blackilykat.Audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WavFileParser {

    // http://soundfile.sapp.org/doc/WaveFormat/
    // https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
    public static boolean parse(File fileToParse, Audio instance) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(fileToParse)) {
            int chunkId = readInt(inputStream, false);
            int chunkSize = readInt(inputStream, true);
            int format = readInt(inputStream, false);
            int subChunk1ID = readInt(inputStream, false);
            int subChunk1Size = readInt(inputStream, true);
            short audioFormat = readShort(inputStream, true);
            short numChannels = readShort(inputStream, true);
            int sampleRate = readInt(inputStream, true);
            int byteRate = readInt(inputStream, true);
            short blockAlign = readShort(inputStream, true);
            short bitsPerSample = readShort(inputStream, true);

            // optional
            short cbSize = 0;
            short validBitsPerSample = 0;
            int channelMask = 0;
            byte[] subFormat = new byte[0];
            int factChunkId = 0;
            int factChunkSize = 0;
            int factSampleLength = 0;

            if(subChunk1Size > 16) {
                cbSize = readShort(inputStream, true);
                if(cbSize == 22) {
                    validBitsPerSample = readShort(inputStream, true);
                    channelMask = readInt(inputStream, true);
                    subFormat = inputStream.readNBytes(16);
                }

                factChunkId = readInt(inputStream, false);
                factChunkSize = readInt(inputStream, true);
                factSampleLength = readInt(inputStream, true);
            }


            int subChunk2ID = readInt(inputStream, false);
            int subChunk2Size = readInt(inputStream, true);

            System.out.printf("""
                    ChunkID matches: %s
                    ChunkSize: %s
                    Format matches: %s
                    Subchunk1ID matches: %s
                    Subchunk1Size: %s
                    AudioFormat: %s
                    NumChannels: %s
                    SampleRate: %s
                    ByteRate: %s
                    BlockAlign: %s
                    BitsPerSample: %s
                    cbSize: %s
                    validBitsPerSample: %s
                    channelMask: %s
                    subFormat: %s
                    factChunkId matches: %s
                    factChunkSize: %s
                    factSampleLength: %s
                    Subchunk2ID matches: %s
                    Subchunk2Size: %s
                    """,
                    chunkId == 0x52494646,
                    chunkSize,
                    format == 0x57415645,
                    subChunk1ID == 0x666d7420,
                    subChunk1Size,
                    audioFormat,
                    numChannels,
                    sampleRate,
                    byteRate,
                    blockAlign,
                    bitsPerSample,
                    cbSize,
                    validBitsPerSample,
                    channelMask,
                    toHex(subFormat),
                    factChunkId == 0x66616374,
                    factChunkSize,
                    factSampleLength,
                    subChunk2ID == 0x64617461,
                    subChunk2Size);

            // only supports PCM
            if(audioFormat != 1 && audioFormat != -2) {
                System.out.println("Unsupported format!");
                return false;
            }
            if(bitsPerSample % 8 != 0) {
                System.out.println("Unsupported bits per sample! (not divisible by 8)");
                return false;
            }
            if(numChannels > 2) {
                System.out.println("Unsupported channel amount! (too many)");
                return false;
            }
            instance.loaded = 0;
            byte[] pcmData = inputStream.readNBytes(subChunk2Size);
            if(pcmData.length != subChunk2Size) {
                System.out.println("Incorrect length metadata!");
                return false;
            }
            int bytesPerSample = bitsPerSample/8;
            int pcmChannelLength = subChunk2Size / bytesPerSample / numChannels;
            byte[] parsedData = new byte[((Double)(subChunk2Size * (2.0/bytesPerSample) * ((double) instance.audioFormat.getChannels()/numChannels) * (instance.audioFormat.getSampleRate() / sampleRate))).intValue()];

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

                if(sampleRate == (int) instance.audioFormat.getSampleRate()) {
                    for(int j = 0; j < pcmChannelLength; j++) {
                        byte[] sample = asBytes(pcmChannel[j], true);
                        parsedData[(j*4) + (i*2)] = sample[0];
                        parsedData[(j*4) + (i*2) + 1] = sample[1];
                    }
                } else {
                    for(int j = 0; j < (pcmChannelLength-1) / (sampleRate / instance.audioFormat.getSampleRate()) - 2; j++) {
                        double positionSeconds = j / instance.audioFormat.getSampleRate();
                        double ogSamplePosition = positionSeconds * sampleRate;
                        int ogSampleFloored = (int) Math.floor(ogSamplePosition);
                        double ogSampleMidPercentage = ogSamplePosition - ogSampleFloored;
                        short averagedSample = (short) ((pcmChannel[ogSampleFloored] * (1 - ogSampleMidPercentage)) + (pcmChannel[ogSampleFloored + 1] * ogSampleMidPercentage));
                        byte[] sample = asBytes(averagedSample, true);
                        parsedData[(j * 4) + (i * 2)] = sample[0];
                        parsedData[(j * 4) + (i * 2) + 1] = sample[1];
                    }
                }
            }
            instance.song = parsedData;
            instance.loaded = parsedData.length;
        }
        return true;
    }

    private static int readInt (InputStream inputStream, boolean littleEndian) throws IOException {
        byte[] read = inputStream.readNBytes(4);
        if(littleEndian) {
            return (read[0] & 0xFF) + ((read[1] & 0xFF) << 8) + ((read[2] & 0xFF) << 16) + ((read[3] & 0xFF) << 24);
        } else {
            return (read[3] & 0xFF) + ((read[2] & 0xFF) << 8) + ((read[1] & 0xFF) << 16) + ((read[0] & 0xFF) << 24);
        }
    }

    private static short readShort (InputStream inputStream, boolean littleEndian) throws IOException {
        byte[] read = inputStream.readNBytes(2);
        if(littleEndian) {
            return (short) ((read[0] & 0xFF) + ((read[1] & 0xFF) << 8));
        } else {
            return (short) ((read[1] & 0xFF) + ((read[0] & 0xFF) << 8));
        }
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

    private static String toHex(byte[] bytes) {
        byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
