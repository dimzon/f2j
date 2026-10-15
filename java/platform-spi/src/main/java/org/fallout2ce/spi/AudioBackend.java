package org.fallout2ce.spi;

/**
 * Sound backend (future {@code game_sound}/{@code audio_engine} port).
 *
 * <p>Payloads are complete WAV bytes; parsing stays inside the
 * implementation. Managed providers use {@code javax.sound.sampled} (JRE
 * only); native providers (separate modules) may talk to SDL_mixer and the
 * like.
 */
public interface AudioBackend {

    /** Looping-music handle. */
    interface MusicHandle {
        void stop();
    }

    /** Fire-and-forget one-shot effect. Never throws. */
    void playOneShot(byte[] wavData);

    /**
     * Start looped background music. Never throws; the handle stops it.
     * Starting a new track stops the previous one.
     */
    MusicHandle playMusic(byte[] wavData);

    /** Stop everything. Never throws. */
    void stopAll();

    /** Master volume, clamped to 0..1. Never throws. */
    void setVolume(float volume);

    /** Current master volume (0..1). */
    float volume();
}
