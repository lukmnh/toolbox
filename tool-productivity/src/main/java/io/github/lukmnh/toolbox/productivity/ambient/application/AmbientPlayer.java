package io.github.lukmnh.toolbox.productivity.ambient.application;

import io.github.lukmnh.toolbox.productivity.ambient.domain.AmbientMixer;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioLine;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioSink;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioSpec;
import io.github.lukmnh.toolbox.productivity.ambient.domain.MixLevels;
import io.github.lukmnh.toolbox.productivity.ambient.domain.SleepTimer;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memutar {@link AmbientMixer} ke {@link AudioSink} di satu thread audio khusus (platform thread, daemon).
 *
 * <p>Method publik adalah kontrol dan wajib dipanggil dari satu thread yang sama (FX thread). Thread audio hanya membaca
 * field {@code volatile}/atomic, jadi loop audio berjalan tanpa lock. Thread baru dibuat saat {@link #start}, tidak
 * sebelumnya.
 */
public final class AmbientPlayer {

    /** Dipanggil dari thread audio saat sesi berakhir; implementasi wajib memindahkan kerja UI ke thread-nya sendiri. */
    public interface Listener {

        /** Sesi berhenti normal (stop/sleep timer) setelah fade out selesai. */
        void onStopped();

        /** Sesi berhenti karena error, misalnya {@code AudioDeviceUnavailableException}. */
        void onFailed(RuntimeException error);
    }

    private static final String THREAD_NAME = "toolbox-ambient-audio";
    /** Blok 50 ms: setengah buffer perangkat, jadi selalu ada satu blok cadangan saat scheduler telat. */
    private static final int BLOCKS_PER_SECOND = 20;

    private static final int RUNNING = 0;
    private static final int STOPPING = 1;
    private static final int ENDED = 2;

    private final AudioSink sink;
    private final AudioSpec spec;
    private volatile MixLevels levels = MixLevels.DEFAULT;
    private volatile boolean paused;
    /** {@code null} = sleep timer mati. Nilai dari {@link System#nanoTime()} supaya kebal perubahan jam sistem. */
    private volatile SleepDeadline sleepDeadline;
    /** Hanya diakses thread kontrol. */
    private SleepTimer sleepTimer = SleepTimer.OFF;
    /** Hanya diakses thread kontrol. */
    private Session session;

    public AmbientPlayer(AudioSink sink, AudioSpec spec) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    /**
     * Mulai memutar dengan fade in. Idempotent: bila sedang jalan tidak terjadi apa-apa, bila sedang fade out sesi yang
     * sama dilanjutkan lagi (tidak membuka perangkat kedua). Selalu membatalkan jeda dan mengulang sleep timer dari awal.
     */
    public void start(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        paused = false;
        setSleepTimer(sleepTimer);
        if (session != null && session.resume()) {
            return;
        }
        session = new Session(listener);
        session.thread.start();
    }

    /** Fade out lalu berhenti. Idempotent dan tidak memblok; {@link Listener#onStopped()} menandai selesai. */
    public void stop() {
        if (session != null) {
            session.requestStop();
        }
    }

    /** Jeda = fade out ke senyap tetapi line tetap terbuka, sehingga lanjut terasa instan. */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setLevels(MixLevels levels) {
        this.levels = Objects.requireNonNull(levels, "levels");
    }

    /** Hitung mundur dimulai dari sekarang (dan diulang setiap {@link #start}). */
    public void setSleepTimer(SleepTimer timer) {
        sleepTimer = Objects.requireNonNull(timer, "timer");
        sleepDeadline = timer.duration()
                .map(duration -> new SleepDeadline(System.nanoTime() + duration.toNanos()))
                .orElse(null);
    }

    /** @return kosong bila sleep timer mati */
    public Optional<Duration> sleepRemaining() {
        SleepDeadline deadline = sleepDeadline;
        if (deadline == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofNanos(Math.max(0, deadline.nanoTime() - System.nanoTime())));
    }

    /** {@code true} selama thread audio sesi terakhir belum selesai (termasuk saat fade out). */
    public boolean isActive() {
        return session != null && !session.isEnded();
    }

    private record SleepDeadline(long nanoTime) {

        boolean hasPassed() {
            // Dibandingkan lewat selisih karena nanoTime boleh overflow
            return System.nanoTime() - nanoTime >= 0;
        }
    }

    /** Satu kali buka perangkat sampai tutup. State atomic supaya resume dari FX thread tidak balapan dengan akhir sesi. */
    private final class Session {

        private final AtomicInteger state = new AtomicInteger(RUNNING);
        private final Listener listener;
        private final Thread thread;

        Session(Listener listener) {
            this.listener = listener;
            this.thread = Thread.ofPlatform().name(THREAD_NAME).daemon(true).unstarted(this::run);
        }

        boolean resume() {
            return state.compareAndSet(STOPPING, RUNNING) || state.get() == RUNNING;
        }

        void requestStop() {
            state.compareAndSet(RUNNING, STOPPING);
        }

        boolean isEnded() {
            return state.get() == ENDED;
        }

        /** Semua exception ditangkap di sini supaya thread audio tidak pernah mati diam-diam atau menjatuhkan app (#22). */
        private void run() {
            RuntimeException failure = null;
            try {
                play();
            } catch (RuntimeException e) {
                failure = e;
            } finally {
                state.set(ENDED);
            }
            if (failure == null) {
                listener.onStopped();
            } else {
                listener.onFailed(failure);
            }
        }

        private void play() {
            AmbientMixer mixer = new AmbientMixer(spec.sampleRate(), new SplittableRandom());
            int frames = spec.sampleRate() / BLOCKS_PER_SECOND;
            // Dialokasikan sekali per sesi; loop di bawah tidak membuat objek baru
            float[] block = new float[frames * AudioSpec.CHANNELS];
            try (AudioLine line = sink.open(spec)) {
                boolean playing = true;
                while (playing) {
                    playing = renderBlock(mixer, line, block, frames);
                }
                line.drain();
            }
        }

        /** @return {@code false} bila sesi selesai: stop diminta dan fade out sudah mencapai senyap */
        private boolean renderBlock(AmbientMixer mixer, AudioLine line, float[] block, int frames) {
            SleepDeadline deadline = sleepDeadline;
            if (deadline != null && deadline.hasPassed()) {
                requestStop();
            }
            boolean audible = state.get() == RUNNING && !paused;
            mixer.render(block, frames, levels, audible);
            line.write(block, frames);
            // CAS gagal berarti start() melanjutkan sesi ini di tengah fade out, jadi loop jalan terus
            return !(mixer.isSilent() && state.compareAndSet(STOPPING, ENDED));
        }
    }
}
