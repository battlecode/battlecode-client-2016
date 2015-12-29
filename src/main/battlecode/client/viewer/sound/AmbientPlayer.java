package battlecode.client.viewer.sound;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.client.viewer.sound.GameSoundBank.ClipGroup;

public class AmbientPlayer {
    private static int currentIntensity = 0;
    private static final int AMBIENT_LEVELS = 3;
    private static boolean ambientInitialized = true;

    public static void playAmbient(int intensity) {
        if (!ambientInitialized) return;
        currentIntensity = intensity;

        for (int i = 1; i < AMBIENT_LEVELS + 1; i++) {
            if (i != currentIntensity || !RenderConfiguration
                    .playAmbientMusic()) {
                getAmbientClip(i).setGain(-80.0f);
            } else {
                getAmbientClip(i).setGain(1.0f);
                getAmbientClip(i).play();
            }
        }
    }


    public static ClipGroup AMBIENT_1;
    public static ClipGroup AMBIENT_2;
    public static ClipGroup AMBIENT_3;

    static {
        try {
            AMBIENT_1 = new ClipGroup("music/1loop.wav", 30500000L);
            AMBIENT_2 = new ClipGroup("music/2loop.wav", 43500000L);
            AMBIENT_3 = new ClipGroup("music/3loop.wav", 38500000L);
        } catch (Exception e) {
            ambientInitialized = false;
        }
    }

    public static ClipGroup getAmbientClip(int intensity) {
        switch (intensity) {
            case 1:
                return AMBIENT_1;
            case 2:
                return AMBIENT_2;
            case 3:
                return AMBIENT_3;

        }
        return null;

    }

}
