package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Channel counts from {@code golden/v26.txt} ({@code [graph]}), driven
 * through the settings the harness forces.
 */
class AudioChannelsTest {

    private int[] saved;
    private boolean isSaved;

    @AfterEach
    void reset() {
        if (isSaved) {
            Settings.settings.sound.music_channels = saved[0];
            Settings.settings.sound.speech_channels = saved[1];
            Settings.settings.sound.sfx_channels = saved[2];
            Settings.settings.sound.movie_channels = saved[3];
            Settings.settings.sound.script_channels = saved[4];
            Settings.settings.sound.float_channels = saved[5];
            Settings.settings.sound.pipboy_channels = saved[6];
        }
    }

    void sound(int music, int speech, int sfx, int movie, int script, int floatChannels,
            int pipboy) {
        if (!isSaved) {
            saved = new int[] {
                Settings.settings.sound.music_channels,
                Settings.settings.sound.speech_channels,
                Settings.settings.sound.sfx_channels,
                Settings.settings.sound.movie_channels,
                Settings.settings.sound.script_channels,
                Settings.settings.sound.float_channels,
                Settings.settings.sound.pipboy_channels,
            };
            isSaved = true;
        }
        Settings.settings.sound.music_channels = music;
        Settings.settings.sound.speech_channels = speech;
        Settings.settings.sound.sfx_channels = sfx;
        Settings.settings.sound.movie_channels = movie;
        Settings.settings.sound.script_channels = script;
        Settings.settings.sound.float_channels = floatChannels;
        Settings.settings.sound.pipboy_channels = pipboy;
    }

    @Test
    void defaults() {
        sound(1, 1, 4, 1, 4, 4, 1);

        assertEquals(1, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_MUSIC));
        assertEquals(1, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_SPEECH));
        assertEquals(4, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_SFX));
        assertEquals(1, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_MOVIE));
        assertEquals(4, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_SCRIPT));
        assertEquals(4, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_FLOAT));
        assertEquals(1, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_PIPBOY));
        assertEquals(16, AudioChannels.audioChannelsGetTotal());
    }

    @Test
    void clamps() {
        sound(1, 1, 100, 1, 0, 4, 1);

        assertEquals(16, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_SFX));
        assertEquals(1, AudioChannels.audioChannelsGetCount(AudioChannels.AUDIO_CHANNEL_SCRIPT));
        assertEquals(0, AudioChannels.audioChannelsGetCount(-1));
        assertEquals(0, AudioChannels.audioChannelsGetCount(99));
        assertEquals(25, AudioChannels.audioChannelsGetTotal());
    }
}
