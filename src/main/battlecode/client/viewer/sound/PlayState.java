package battlecode.client.viewer.sound;

import battlecode.client.viewer.GameState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.world.GameMap;
import battlecode.world.signal.AttackSignal;
import battlecode.world.signal.DeathSignal;
import battlecode.world.signal.SpawnSignal;

import java.util.*;

public class PlayState extends GameState {


    private static class Factory implements GameStateFactory<PlayState> {

        public PlayState createState(GameMap map) {
            return new PlayState();
        }

        public PlayState cloneState(PlayState state) {
            return new PlayState(state);
        }

        public void copyState(PlayState src, PlayState dst) {
            dst.copyStateFrom(src);
        }
    }

    public static final GameStateFactory<PlayState> FACTORY = new Factory();

    private Set<GameSoundBank.ClipGroup> activeClips;
    private Set<GameSoundBank.ClipGroup> futureClips;
    private Map<Integer, RobotType> robotTypes;
    private int intensityLevel = 0;
    private boolean[][] nukeFlags = new boolean[Team.values().length][3];


    public PlayState() {
        activeClips = new HashSet<>();
        futureClips = new HashSet<>();
        robotTypes = new HashMap<>();

    }

    private PlayState(PlayState clone) {
        this();
        copyStateFrom(clone);
    }

    private synchronized void copyStateFrom(PlayState src) {
        this.intensityLevel = src.intensityLevel;

        for (int i = 0; i < this.nukeFlags.length; i++) {
            this.nukeFlags[i] = Arrays.copyOf(src.nukeFlags[i], src
                    .nukeFlags[i].length);
        }
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip) {
        //futureClips.add(clip);
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip, float gain) {
        scheduleClip(clip);
        clip.setGain(gain);
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip, float minGain,
                              float maxGain) {
        scheduleClip(clip, ((float) Math.random()) * (maxGain - minGain) +
                minGain);
    }

    @SuppressWarnings("unused")
    public Void visitAttackSignal(AttackSignal s) {
        try {
            switch (robotTypes.get(s.getRobotID())) {
                default:
                    scheduleClip(GameSoundBank.ATTACK, -5, 0);
            }
        } catch (NullPointerException e) {
        }
        return null;
    }

    @SuppressWarnings("unused")
    public Void visitDeathSignal(DeathSignal s) {
        scheduleClip(GameSoundBank.DEATH);
        return null;
    }

    @SuppressWarnings("unused")
    public Void visitSpawnSignal(SpawnSignal s) {
        scheduleClip(GameSoundBank.SNIPE, -10.f);
        //System.out.println( s.getType());
        robotTypes.put(s.getRobotID(), s.getType());
        return null;
    }

    protected void preUpdateRound() {
    }

    protected void postUpdateRound() {
        activeClips.clear();
        Set<GameSoundBank.ClipGroup> tmp = activeClips;
        activeClips = futureClips;
        futureClips = tmp;
    }

    public void play() {
        if (intensityLevel > 100) {

            if (intensityLevel > 400) {
                AmbientPlayer.playAmbient(3);
            } else {
                AmbientPlayer.playAmbient(2);
            }
        } else {
            AmbientPlayer.playAmbient(1);
        }


        for (GameSoundBank.ClipGroup clipGroup : activeClips) {
            clipGroup.play();
        }
        activeClips.clear();
    }
}
