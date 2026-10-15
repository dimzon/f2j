package org.fallout2ce.platform.audio;

import org.fallout2ce.spi.AudioBackend;

/**
 * Explicitly silent audio backend (headless servers, tests). Accepts every
 * call and drops it; volume is stored and clamped.
 */
public final class NullAudioBackend implements AudioBackend {

    private float volume = 1.0f;

    private final MusicHandle stopped = () -> {
    };

    @Override
    public void playOneShot(byte[] wavData) {
    }

    @Override
    public MusicHandle playMusic(byte[] wavData) {
        return stopped;
    }

    @Override
    public void stopAll() {
    }

    @Override
    public void setVolume(float volume) {
        this.volume = clamp(volume);
    }

    @Override
    public float volume() {
        return volume;
    }

    static float clamp(float volume) {
        if (volume < 0.0f) {
            return 0.0f;
        }
        if (volume > 1.0f) {
            return 1.0f;
        }
        return volume;
    }
}
