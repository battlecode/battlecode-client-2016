package battlecode.client.viewer.sound;

import battlecode.client.util.SoundFile;



import java.util.*;
import javax.sound.sampled.*;

public class GameSoundBank {
	private static boolean bankInitialized = true;

	public static class ClipGroup {
		private final SoundFile soundFile;
//		private final LinkedList<Clip> clips;
		private final Clip clip;
		private final int maxClips;
		private volatile float gain = -80.0f;
		private int fadeTime = 200000;
		private long repeat;
		private boolean active = true;
		private boolean gainSupported = true;		
		

		private long lastPlayed = Long.MIN_VALUE;

		public ClipGroup(String path) {
			this(path, 150000, 4);
		}
		public ClipGroup(String path, long repeat) {
			this(path, repeat, 4);
		}
		public ClipGroup(String path, long repeat, int maxClips, boolean active) {
			this(path, repeat, maxClips);
			this.active = active;
		}
		public ClipGroup(String path, long repeat, int maxClips) {
			soundFile = new SoundFile(path);
			clip = soundFile.createClip();
//			clips = new LinkedList<Clip>();
			this.repeat = repeat;
			this.maxClips = maxClips;
		}
		private static FloatControl getGainControl(Clip clip) {
			return (FloatControl)(clip.getControl(FloatControl.Type.MASTER_GAIN));
		}
		public synchronized void play() {
			if(clip==null || !bankInitialized) return;
			long now = System.nanoTime();
			//System.out.println(now + " " + (lastPlayed + 1000*repeat - fadeTime*1000));
			if (now < lastPlayed + 1000*repeat) { 
				/*if(ambientMusic && now > lastPlayed + 1000*repeat - ((long)fadeTime*1000)){
					
					FloatControl level;
					level = getGainControl(clips.getFirst());
					float currentValue = level.getValue();
					float delta = currentValue-level.getMinimum();
					delta /= 100.0f;
					level.shift(currentValue, currentValue-delta, fadeTime); // linshift
					
				}
				else *///if(){//fade
					if(gainSupported) {
						try {
							FloatControl level;
							level = getGainControl(clip);
							float currentValue = level.getValue();
							float delta = currentValue-gain;
							delta /= 2.0f;
					
							level.shift(currentValue, currentValue-delta, fadeTime); // linshift
						} catch(IllegalArgumentException e) {
							gainSupported = false;
						}
					}
					
				
				//}
				
				//The level.shift implementation does not currently do a smooth shift.
				return; 
			}
			
//			if(!active) return;
			clip.setFramePosition(0);
			if(gainSupported) {
				try {
					FloatControl level = getGainControl(clip);
					if(gain <= level.getMinimum()) return;
					level.setValue(gain);
				} catch(IllegalArgumentException e) {
					gainSupported = false;
				}
			}
			
			clip.start();
			lastPlayed = now;/*
			Clip clip;
			if (clips.size() < maxClips && maxClips != -1) {
				clip = soundFile.createClip();
			}
			else {
				clip = clips.remove(); // clip gain will already be minimum
				clip.stop();
				clip.setFramePosition(0);
			}

			FloatControl level = getGainControl(clip);
			level.setValue(gain);
			clip.start();
			if(maxClips != -1){
				clips.add(clip);
			}m
			if (clips.size() == maxClips) {
				level = getGainControl(clips.getFirst());
				level.shift(level.getValue(), level.getMinimum(), fadeTime); // linshift
			}
			*/
		}
		public void setActive(boolean active){
			this.active = active;
			}
		public void setGain(float gain) { this.gain = gain; }
		public void setFadeTime(int interval) { fadeTime = interval; }
		public void setRepeat(long repeat) { this.repeat = repeat; }
	}
	
	
	public static final ClipGroup OPEN  = new ClipGroup("sound/metagame/open.wav", 100000);
	public static final ClipGroup CLOSE = new ClipGroup("sound/metagame/close.wav", 100000);
	public static final ClipGroup SNARE = new ClipGroup("sound/metagame/snare.wav", 50000);
	public static final ClipGroup BASS  = new ClipGroup("sound/metagame/bass.wav", 200000);

	public static ClipGroup ATTACK;// = new ClipGroup("sound/blaster1.wav");
	public static ClipGroup ATTACK2;//= new ClipGroup("sound/blaster2.wav");
	public static ClipGroup BEEPS;//  = new ClipGroup("sound/beep.wav", 50000);
	public static ClipGroup DEATH ;// = new ClipGroup("sound/explosion.wav", 450000, 3);
	//public static final ClipGroup EMPDET = new ClipGroup("sound/empdet.wav");
	public static ClipGroup MORTAR;// = new ClipGroup("sound/mortar.wav");
	
	public static ClipGroup SPAWN;//  = new ClipGroup("sound/metal.wav");
	public static ClipGroup SNIPE;//  = new ClipGroup("sound/snipe.wav", 250000);

	public static ClipGroup FLOP;// = new ClipGroup("sound/flop.wav");
	public static ClipGroup GLOP;// = new ClipGroup("sound/glop.wav");
	public static  ClipGroup CAPTURE;//= new ClipGroup("sound/laser_rocket.wav", 500000, 2);
	

	// extra game sounds used in 2013
	public static ClipGroup NUKE_READY;
	public static ClipGroup NUKE_DETECTED;
	public static ClipGroup NUKE_SIREN;
	
	
	//public static final ClipGroup FLUX_BURN = new ClipGroup("sound/flux_burn.wav");

	static {
		
	}

	public static void preload() {
		try{
			
			ATTACK = new ClipGroup("sound/blaster1.wav");
			ATTACK2= new ClipGroup("sound/blaster2.wav");
			BEEPS  = new ClipGroup("sound/beep.wav", 50000);
			DEATH  = new ClipGroup("sound/explosion.wav", 450000, 3);
			MORTAR = new ClipGroup("sound/mortar.wav");
			 SPAWN  = new ClipGroup("sound/metal.wav");
			 SNIPE  = new ClipGroup("sound/snipe.wav", 250000);
			 FLOP = new ClipGroup("sound/flop.wav");
			 GLOP = new ClipGroup("sound/glop.wav");
			 CAPTURE= new ClipGroup("sound/laser_rocket.wav", 500000, 2);

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
		}catch(Exception e){
			bankInitialized = false;
		}
		
	}
	
	
}
