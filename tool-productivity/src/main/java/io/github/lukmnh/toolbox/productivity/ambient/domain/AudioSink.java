package io.github.lukmnh.toolbox.productivity.ambient.domain;

/** Port ke perangkat output audio sistem ({@code javax.sound} ada di luar batas domain). Harus thread-safe. */
public interface AudioSink {

    /**
     * Buka dan mulai satu line output. Dipanggil dari thread audio karena membuka perangkat bisa lambat.
     *
     * @throws AudioDeviceUnavailableException bila tidak ada perangkat yang bisa memutar {@code spec}
     */
    AudioLine open(AudioSpec spec);
}
