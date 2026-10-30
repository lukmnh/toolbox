package io.github.lukmnh.toolbox.productivity.ambient.infrastructure;

import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioDeviceUnavailableException;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioLine;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioSink;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioSpec;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** {@link AudioSink} lewat {@code javax.sound.sampled}: PCM 16-bit signed little-endian. */
public final class JavaSoundAudioSink implements AudioSink {

    private static final int BITS_PER_SAMPLE = 16;
    private static final int BYTES_PER_SAMPLE = BITS_PER_SAMPLE / Byte.SIZE;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    @Override
    public AudioLine open(AudioSpec spec) {
        AudioFormat format =
                new AudioFormat(spec.sampleRate(), BITS_PER_SAMPLE, AudioSpec.CHANNELS, SIGNED, BIG_ENDIAN);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, spec.bufferFrames() * format.getFrameSize());
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // IllegalArgumentException = tidak ada mixer yang mendukung format (umumnya: tidak ada perangkat sama
            // sekali)
            throw new AudioDeviceUnavailableException(e);
        }
        line.start();
        return new PcmLine(line);
    }

    /** Konversi float → PCM memakai satu buffer byte yang dipakai ulang (tanpa alokasi per blok). */
    private static final class PcmLine implements AudioLine {

        private static final double FULL_SCALE = Short.MAX_VALUE;

        private final SourceDataLine line;
        private byte[] bytes = new byte[0];

        PcmLine(SourceDataLine line) {
            this.line = line;
        }

        @Override
        public void write(float[] samples, int frames) {
            int sampleCount = frames * AudioSpec.CHANNELS;
            int length = sampleCount * BYTES_PER_SAMPLE;
            if (bytes.length < length) {
                bytes = new byte[length];
            }
            for (int index = 0; index < sampleCount; index++) {
                int pcm = (int) Math.round(Math.clamp(samples[index], -1.0, 1.0) * FULL_SCALE);
                bytes[index * BYTES_PER_SAMPLE] = (byte) pcm;
                bytes[index * BYTES_PER_SAMPLE + 1] = (byte) (pcm >> Byte.SIZE);
            }
            // SourceDataLine.write blok sampai semua data masuk, kecuali line dihentikan/ditutup dari luar
            int written = line.write(bytes, 0, length);
            if (written < length) {
                throw new IllegalStateException(
                        "Line audio berhenti saat menulis (" + written + "/" + length + " byte)");
            }
        }

        @Override
        public void drain() {
            line.drain();
        }

        @Override
        public void close() {
            line.stop();
            line.close();
        }
    }
}
