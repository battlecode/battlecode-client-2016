package battlecode.client.viewer.sound;

import battlecode.client.viewer.BufferedMatch;
import battlecode.client.viewer.GameStateTimeline;

class AudioTimeline extends GameStateTimeline<PlayState> {

    public AudioTimeline(BufferedMatch match) {
        super(match, PlayState.FACTORY, Integer.MAX_VALUE);
    }

    public AudioTimeline(GameStateTimeline master) {
        this(master.getMatch());
        setMasterTimeline(master);
    }
}
