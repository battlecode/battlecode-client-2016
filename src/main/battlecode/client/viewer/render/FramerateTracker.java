package battlecode.client.viewer.render;

public class FramerateTracker {

	private double framerate;
	private long frames = 0, prevTick = 0;

	public FramerateTracker() {
		framerate = 30;
	}

	public FramerateTracker(double fps) {
		framerate = fps;
	}

	public double getFramerate() {
		return framerate;
	}

	public void updateFramerate() {
		if (prevTick == 0) prevTick = System.nanoTime();
		if ((++frames) % 4 == 0) {
			long tick = System.nanoTime();
			framerate = (framerate + 4000000000.0/(tick-prevTick))*0.5;
			prevTick = tick;
		}
	}
}
