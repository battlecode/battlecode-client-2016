package battlecode.client.viewer.sound;

import battlecode.client.viewer.*;
import battlecode.client.viewer.sound.GameSoundBank.ClipGroup;

import java.util.*;
import javax.sound.sampled.*;
import battlecode.client.viewer.render.RenderConfiguration;
public class AmbientPlayer {
	private static int currentIntensity = 0;
	private static final int AMBIENT_LEVELS = 3;
	private static boolean ambientInitialized = true;
	
	public static void playAmbient(int intensity){
		if(!ambientInitialized) return;
		currentIntensity = intensity;
		/*if(intensity <= 0){
			//Turn off ambient music
			for(int i = 0; i < AMBIENT_LEVELS; i++){
				getAmbientClip(i+1).setActive(false);
			}
			return;
		}*/
//		System.out.println(RenderConfiguration.playAmbientMusic());
		for(int i = 1; i < AMBIENT_LEVELS+1; i++){
			if(i != currentIntensity || !RenderConfiguration.playAmbientMusic()){
				getAmbientClip(i).setGain(-80.0f);
				getAmbientClip(i).setActive(false);
			}
			else{
				getAmbientClip(i).setActive(true);
				getAmbientClip(i).setGain(1.0f);
        getAmbientClip(i).play();
			}
		}
	}
	
	
	public static ClipGroup AMBIENT_1;// = new ClipGroup("../music/Intensity1Loop.wav", 30500000L, 10, false);
	public static ClipGroup AMBIENT_2;// = new ClipGroup("../music/Intensity 2 Loop 6.wav", 43500000L, 10, false);
	public static ClipGroup AMBIENT_3;// = new ClipGroup("music/Intensity3Loop7.wav", 38500000L, 10, false);
	
	static{
		try{
		AMBIENT_1 = new ClipGroup("music/1loop.wav", 30500000L, 10, false);
		AMBIENT_2 = new ClipGroup("music/2loop.wav", 43500000L, 10, false);
		AMBIENT_3 = new ClipGroup("music/3loop.wav", 38500000L, 10, false);
		}catch(Exception e){
			ambientInitialized = false;
		}
	}
	public static ClipGroup getAmbientClip(int intensity){
		switch(intensity){
		case 1: return AMBIENT_1;
		case 2: return AMBIENT_2;
		case 3: return AMBIENT_3;
			
		}
		return null;
	
	}
	
}
