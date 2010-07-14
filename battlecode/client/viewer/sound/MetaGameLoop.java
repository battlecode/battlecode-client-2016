package battlecode.client.viewer.sound;

import java.util.Random;

public class MetaGameLoop {

	private final long breveLen = 800; // length of a pattern-bar in millis
	private final long tickLen = breveLen/8;
	private final Random random = new Random();

	private Thread thread = null;

	private boolean doneCue;

	private long previous = 0;
	private long realSkip = 10;

	private int[] patterns = { 0x10401040/*, 0x10443040, 0x10403040, 0x10443044*/ };
	private int[] fills    = { /*0x50002222, 0x14214214,*/ 0x70707070 /*, 0x32222222*/ };
	
	private long startHit;
	
	private long waitUntil(long targetMillis) {
		while (true) {
			long error = System.currentTimeMillis() - targetMillis;
			if (error + realSkip/2 > 0) {
				return error;
			}
			skip();
		}
	}

	private void skip() {
		long now = System.currentTimeMillis();
		if (previous != 0) {
			realSkip = (2 * realSkip + now - previous) / 3;
		}
		previous = now;
		try { Thread.sleep(10); } catch (InterruptedException e) {}
	}

	private void playChord(int chord) {
		if ((chord & 0x4) != 0) {
			GameSoundBank.CLOSE.play();
		}
		if ((chord & 0x2) != 0) {
			GameSoundBank.SNARE.play();
		}
		if ((chord & 0x1) != 0) {
			GameSoundBank.BASS.play();
		}
	}

	private final Runnable runner = new Runnable() {
		public void run() {
			GameSoundBank.SPAWN.play();
			int[] tabs = patterns;
			while (true) {
				int pattern = tabs[random.nextInt(tabs.length)];
				for (int tick = 0; tick < 8; tick++) {
					if (waitUntil(startHit + tickLen*tick) > tickLen) {
						continue;
					}
					playChord((pattern >> (28 - 4*tick)) & 0xf);
				}
				synchronized(this) {
					startHit += breveLen;
					if (doneCue) {
						if (tabs == fills) {
							break;
						}
						tabs = fills;
					}
				}
			}
			waitUntil(startHit);
			GameSoundBank.SPAWN.play();
		}
	};
 
	public void start() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(runner);
			startHit = System.currentTimeMillis() + breveLen;
			doneCue = false;
			thread.start();
			
		}
	}

	public synchronized long stop() {
		doneCue = true;
		return startHit + 2*breveLen;
	}

	public boolean isPlaying() {
		return (thread != null && thread.isAlive());
	}
}
