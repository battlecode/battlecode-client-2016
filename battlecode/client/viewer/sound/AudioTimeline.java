package battlecode.client.viewer.sound;

import battlecode.client.viewer.*;

import java.util.*;

class AudioTimeline extends GameStateTimeline<PlayState> {

	public AudioTimeline(BufferedMatch match) {
		super(match, PlayState.FACTORY, Integer.MAX_VALUE);
	}

	public AudioTimeline(GameStateTimeline master) {
		this(master.getMatch());
		setMasterTimeline(master);
	}
/*
	protected void createKeyFrames() {}
	*/
/*
	public void setRound(int round) {
		if (currentState != null && round > 0 && currentRound + 1 == round) {
			battlecode.serial.RoundDelta delta = match.getRound(currentRound);
			if (delta != null) {
				currentState.apply(delta);
				currentRound = round;
				setChanged();
				notifyObservers();
			}
		}
		currentRound = round;
	}*/
}
