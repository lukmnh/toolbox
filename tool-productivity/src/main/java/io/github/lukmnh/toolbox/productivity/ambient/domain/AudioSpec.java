package io.github.lukmnh.toolbox.productivity.ambient.domain;

/**
 * Format output audio: selalu stereo interleaved, sampel {@code float} dalam rentang [-1, 1]. Encoding PCM (16-bit) urusan
 * {@link AudioSink}.
 *
 * @param bufferFrames ukuran buffer perangkat; kecil = latency rendah, besar = lebih tahan underrun
 */
public record AudioSpec(int sampleRate, int bufferFrames) {

    public static final int CHANNELS = 2;

    private static final int CD_SAMPLE_RATE = 44_100;
    /** 100 ms: cukup untuk menahan jitter scheduler tanpa membuat respon slider terasa telat. */
    private static final int CD_BUFFER_FRAMES = CD_SAMPLE_RATE / 10;

    public static final AudioSpec CD_STEREO = new AudioSpec(CD_SAMPLE_RATE, CD_BUFFER_FRAMES);

    public AudioSpec {
        if (sampleRate <= 0 || bufferFrames <= 0) {
            throw new IllegalArgumentException("sampleRate dan bufferFrames harus positif");
        }
    }
}
