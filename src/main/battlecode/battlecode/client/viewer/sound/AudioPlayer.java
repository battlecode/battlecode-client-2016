package battlecode.client.viewer.sound;

import battlecode.client.viewer.GameStateTimeline;

import java.util.Observable;
import java.util.Observer;

public class AudioPlayer {

    private AudioTimeline timeline;
    private PlayState playState = new PlayState();

    public void setTimeline(GameStateTimeline gst) {
        timeline = new AudioTimeline(gst);
        timeline.setTargetState(playState);
        timeline.addObserver(new Observer() {
            public void update(Observable o, Object arg) {
                playState.play();
            }
        });
    }
}
