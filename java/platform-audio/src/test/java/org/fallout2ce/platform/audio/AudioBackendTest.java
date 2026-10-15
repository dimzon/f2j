package org.fallout2ce.platform.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioSystem;

import org.fallout2ce.spi.AudioBackend;
import org.fallout2ce.spi.PlatformPlugin;
import org.junit.jupiter.api.Test;

/** Backends never throw; silent degradation is deterministic everywhere. */
class AudioBackendTest {

    /** 0.1s 440Hz 8-bit mono WAV at 8kHz, built in-test (no fixtures). */
    static byte[] sineWav() {
        int samples = 800;
        byte[] pcm = new byte[samples];
        for (int i = 0; i < samples; i++) {
            pcm[i] = (byte) (128 + 100 * Math.sin(2 * Math.PI * 440 * i / 8000));
        }
        ByteBuffer wav = ByteBuffer.allocate(44 + samples).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes());
        wav.putInt(36 + samples);
        wav.put("WAVE".getBytes());
        wav.put("fmt ".getBytes());
        wav.putInt(16);
        wav.putShort((short) 1);
        wav.putShort((short) 1);
        wav.putInt(8000);
        wav.putInt(8000);
        wav.putShort((short) 1);
        wav.putShort((short) 8);
        wav.put("data".getBytes());
        wav.putInt(samples);
        wav.put(pcm);
        return wav.array();
    }

    @Test
    void serviceFileDiscoversIt() {
        List<String> names = new ArrayList<>();
        for (PlatformPlugin plugin : ServiceLoader.load(PlatformPlugin.class)) {
            names.add(plugin.name());
        }
        assertTrue(names.contains(AudioPlatformProvider.NAME), "discovered: " + names);
    }

    @Test
    void nullBackendDropsEverything() {
        NullAudioBackend backend = new NullAudioBackend();

        backend.playOneShot(sineWav());
        AudioBackend.MusicHandle music = backend.playMusic(sineWav());
        music.stop();
        backend.stopAll();

        backend.setVolume(2.0f);
        assertEquals(1.0f, backend.volume());
        backend.setVolume(-1.0f);
        assertEquals(0.0f, backend.volume());
    }

    @Test
    void forcedSilentNeverThrows() {
        SampledAudioBackend backend = new SampledAudioBackend(true);

        backend.playOneShot(sineWav());
        backend.playOneShot(new byte[] { 1, 2, 3 });
        AudioBackend.MusicHandle music = backend.playMusic(sineWav());
        music.stop();
        backend.setVolume(0.5f);
        assertEquals(0.5f, backend.volume());
        backend.stopAll();

        assertTrue(backend.isSilent());
        assertEquals(0, backend.openClips());
    }

    @Test
    void liveMixerSmoke() {
        assumeTrue(AudioSystem.getMixerInfo().length > 0, "no mixer");
        SampledAudioBackend backend = new SampledAudioBackend();
        try {
            backend.setVolume(0.0f);
            backend.playOneShot(sineWav());
            AudioBackend.MusicHandle music = backend.playMusic(sineWav());
            music.stop();
            backend.stopAll();
        } finally {
            backend.stopAll();
        }
    }

    @Test
    void providerWiresSampledAudio() {
        AudioPlatformProvider provider = new AudioPlatformProvider();
        assertEquals(AudioPlatformProvider.NAME, provider.name());
        assertTrue(provider.audio() instanceof SampledAudioBackend);
        assertTrue(provider.clock().millis() > 0);
        provider.audio().stopAll();
    }
}
