package battlecode.client.viewer.sound;

import battlecode.client.util.SoundFile;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class GameSoundBank {
    private static boolean bankInitialized = true;

    public static class ClipGroup {
        private final SoundFile soundFile;
        private final Clip clip;
        private volatile float gain = -80.0f;
        private int fadeTime = 200000;
        private long repeat;
        private boolean gainSupported = true;


        private long lastPlayed = Long.MIN_VALUE;

        public ClipGroup(String path) {
            this(path, 150000);
        }

        public ClipGroup(String path, long repeat) {
            soundFile = new SoundFile(path);
            clip = soundFile.createClip();
            this.repeat = repeat;
        }

        private static FloatControl getGainControl(Clip clip) {
            return (FloatControl) (clip.getControl(FloatControl.Type
                    .MASTER_GAIN));
        }

        public synchronized void play() {
            if (clip == null || !bankInitialized) return;
            long now = System.nanoTime();
            if (now < lastPlayed + 1000 * repeat) {
                if (gainSupported) {
                    try {
                        FloatControl level;
                        level = getGainControl(clip);
                        float currentValue = level.getValue();
                        float delta = currentValue - gain;
                        delta /= 2.0f;

                        level.shift(currentValue, currentValue - delta,
                                fadeTime); // linshift
                    } catch (IllegalArgumentException e) {
                        gainSupported = false;
                    }
                }
                return;
            }

            clip.setFramePosition(0);
            if (gainSupported) {
                try {
                    FloatControl level = getGainControl(clip);
                    if (gain <= level.getMinimum()) return;
                    level.setValue(gain);
                } catch (IllegalArgumentException e) {
                    gainSupported = false;
                }
            }

            clip.start();
            lastPlayed = now;
        }

        public void setGain(float gain) {
            this.gain = gain;
        }

        public void setFadeTime(int interval) {
            fadeTime = interval;
        }
    }


    public static final ClipGroup OPEN = new ClipGroup("sound/metagame/open" +
            ".wav", 100000);
    public static final ClipGroup CLOSE = new ClipGroup("sound/metagame/close" +
            ".wav", 100000);
    public static final ClipGroup SNARE = new ClipGroup("sound/metagame/snare" +
            ".wav", 50000);
    public static final ClipGroup BASS = new ClipGroup("sound/metagame/bass" +
            ".wav", 200000);

    public static ClipGroup ATTACK;
    public static ClipGroup ATTACK2;
    public static ClipGroup BEEPS;
    public static ClipGroup DEATH;
    public static ClipGroup MORTAR;
    public static ClipGroup SPAWN;
    public static ClipGroup SNIPE;
    public static ClipGroup FLOP;
    public static ClipGroup GLOP;
    public static ClipGroup CAPTURE;


    // extra game sounds used in 2013
    public static ClipGroup NUKE_READY;
    public static ClipGroup NUKE_DETECTED;
    public static ClipGroup NUKE_SIREN;

    static {

    }

    public static void preload() {
        try {

            ATTACK = new ClipGroup("sound/blaster1.wav");
            ATTACK2 = new ClipGroup("sound/blaster2.wav");
            BEEPS = new ClipGroup("sound/beep.wav", 50000);
            DEATH = new ClipGroup("sound/explosion.wav", 450000);
            MORTAR = new ClipGroup("sound/mortar.wav");
            SPAWN = new ClipGroup("sound/metal.wav");
            SNIPE = new ClipGroup("sound/snipe.wav", 250000);
            FLOP = new ClipGroup("sound/flop.wav");
            GLOP = new ClipGroup("sound/glop.wav");
            CAPTURE = new ClipGroup("sound/laser_rocket.wav", 500000);

            NUKE_READY = new ClipGroup("sound/glop.wav");
            NUKE_READY.setGain(1);
            NUKE_SIREN = new ClipGroup("sound/glop.wav");
            NUKE_SIREN.setGain(1);
            NUKE_DETECTED = new ClipGroup("sound/glop.wav");
            NUKE_DETECTED.setGain(1);


            DEATH.setFadeTime(1000000);
            OPEN.setGain(1);
            CLOSE.setGain(1);
            SNARE.setGain(1);
            BASS.setGain(1);
            ATTACK.setGain(1);
            ATTACK2.setGain(1);
            BEEPS.setGain(1);
            MORTAR.setGain(1);
            DEATH.setGain(1);
            SPAWN.setGain(1);
            SNIPE.setGain(1);
            FLOP.setGain(1);
            GLOP.setGain(1);
            CAPTURE.setGain(1);
        } catch (Exception e) {
            bankInitialized = false;
        }

    }


}
