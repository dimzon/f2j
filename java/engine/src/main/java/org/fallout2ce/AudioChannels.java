package org.fallout2ce;

/**
 * Port of {@code src/audio_channels.h} / {@code src/audio_channels.cc}
 * (audio engine channel pools and their configured counts).
 */
public final class AudioChannels {

    private AudioChannels() {
    }

    public static final int AUDIO_CHANNEL_MUSIC = 0;
    public static final int AUDIO_CHANNEL_SPEECH = 1;
    public static final int AUDIO_CHANNEL_SFX = 2;
    public static final int AUDIO_CHANNEL_MOVIE = 3;
    public static final int AUDIO_CHANNEL_SCRIPT = 4;
    public static final int AUDIO_CHANNEL_FLOAT = 5;
    public static final int AUDIO_CHANNEL_PIPBOY = 6;
    public static final int AUDIO_CHANNEL_TYPE_COUNT = 7;

    /** Allowed count per pool ({@code {min, max}}). */
    static final int[][] CHANNEL_LIMITS = {
        { 1, 1 },
        { 1, 1 },
        { 4, 16 },
        { 1, 1 },
        { 1, 16 },
        { 1, 8 },
        { 1, 1 },
    };

    private static int configuredCount(int type) {
        switch (type) {
            case AUDIO_CHANNEL_MUSIC:
                return Settings.settings.sound.music_channels;
            case AUDIO_CHANNEL_SPEECH:
                return Settings.settings.sound.speech_channels;
            case AUDIO_CHANNEL_SFX:
                return Settings.settings.sound.sfx_channels;
            case AUDIO_CHANNEL_MOVIE:
                return Settings.settings.sound.movie_channels;
            case AUDIO_CHANNEL_SCRIPT:
                return Settings.settings.sound.script_channels;
            case AUDIO_CHANNEL_FLOAT:
                return Settings.settings.sound.float_channels;
            case AUDIO_CHANNEL_PIPBOY:
                return Settings.settings.sound.pipboy_channels;
            default:
                return 0;
        }
    }

    /** Port of {@code audioChannelsGetCount} (out-of-range types yield 0). */
    public static int audioChannelsGetCount(int type) {
        if (type < 0 || type >= AUDIO_CHANNEL_TYPE_COUNT) {
            return 0;
        }

        int[] limits = CHANNEL_LIMITS[type];
        return Math.min(Math.max(configuredCount(type), limits[0]), limits[1]);
    }

    /** Port of {@code audioChannelsGetTotal}. */
    public static int audioChannelsGetTotal() {
        int total = 0;
        for (int type = 0; type < AUDIO_CHANNEL_TYPE_COUNT; type++) {
            total += audioChannelsGetCount(type);
        }
        return total;
    }
}
