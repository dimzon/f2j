package org.fallout2ce.platform.audio;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import org.fallout2ce.spi.AudioBackend;

/**
 * Working audio backend on plain JRE APIs ({@code javax.sound.sampled}).
 *
 * <p>No SDL_mixer or other third-party native bindings. Where no mixer or
 * line exists (headless servers, muted CI) the backend latches into silent
 * mode once and every call becomes a no-op — it never throws out of
 * playback paths.
 */
public final class SampledAudioBackend implements AudioBackend {

    private final Object lock = new Object();

    private final boolean forceSilent;
    private boolean probed;
    private boolean silent;
    private float volume = 1.0f;
    private final List<Clip> open = new ArrayList<>();
    private Clip music;

    /** Real backend (probes the mixer lazily on first use). */
    public SampledAudioBackend() {
        this(false);
    }

    /** Test seam: skip probing, stay silent from the start. */
    SampledAudioBackend(boolean forceSilent) {
        this.forceSilent = forceSilent;
        this.silent = forceSilent;
        this.probed = forceSilent;
    }

    /** Whether playback degraded to silent (test seam). */
    boolean isSilent() {
        synchronized (lock) {
            return silent;
        }
    }

    /** Open clips currently held (test seam). */
    int openClips() {
        synchronized (lock) {
            return open.size() + (music != null ? 1 : 0);
        }
    }

    @Override
    public void playOneShot(byte[] wavData) {
        Clip clip;
        synchronized (lock) {
            if (!readyLocked()) {
                return;
            }
            clip = openLocked(wavData);
            if (clip == null) {
                return;
            }
            open.add(clip);
        }
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                synchronized (lock) {
                    open.remove(clip);
                }
                clip.close();
            }
        });
        clip.start();
    }

    @Override
    public MusicHandle playMusic(byte[] wavData) {
        synchronized (lock) {
            stopMusicLocked();
            if (!readyLocked()) {
                return () -> {
                };
            }
            music = openLocked(wavData);
            if (music == null) {
                return () -> {
                };
            }
            music.loop(Clip.LOOP_CONTINUOUSLY);
            Clip started = music;
            return () -> {
                synchronized (lock) {
                    if (music == started) {
                        stopMusicLocked();
                    }
                }
            };
        }
    }

    @Override
    public void stopAll() {
        List<Clip> clips;
        synchronized (lock) {
            stopMusicLocked();
            clips = new ArrayList<>(open);
            open.clear();
        }
        clips.forEach(Clip::close);
    }

    @Override
    public void setVolume(float volume) {
        List<Clip> clips;
        synchronized (lock) {
            this.volume = NullAudioBackend.clamp(volume);
            clips = new ArrayList<>(open);
            if (music != null) {
                clips.add(music);
            }
        }
        clips.forEach(this::applyVolume);
    }

    @Override
    public float volume() {
        synchronized (lock) {
            return volume;
        }
    }

    private boolean readyLocked() {
        if (!probed) {
            probed = true;
            silent = forceSilent || AudioSystem.getMixerInfo().length == 0;
        }
        return !silent;
    }

    private void goSilentLocked() {
        silent = true;
        stopMusicLocked();
        List<Clip> clips = new ArrayList<>(open);
        open.clear();
        clips.forEach(Clip::close);
    }

    private void stopMusicLocked() {
        if (music != null) {
            Clip stopped = music;
            music = null;
            stopped.close();
        }
    }

    private Clip openLocked(byte[] wavData) {
        try (AudioInputStream stream =
                AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavData))) {
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            applyVolume(clip);
            return clip;
        } catch (Exception e) {
            goSilentLocked();
            return null;
        }
    }

    private void applyVolume(Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float range = gain.getMaximum() - gain.getMinimum();
            gain.setValue(gain.getMinimum() + range * volume());
        }
    }
}
