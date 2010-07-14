package battlecode.client.viewer;

import java.awt.Graphics2D;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import battlecode.client.viewer.render.RenderConfiguration;

public abstract class AbstractAnimation implements Cloneable {

	public static enum AnimationType {
		ENERGON_TRANSFER,
		DEATH_EXPLOSION,
		AURA,
		MORTAR_ATTACK,
		MORTAR_EXPLOSION,
		TELEPORT;
	
		public boolean shown() {
			switch(this) {
			case ENERGON_TRANSFER:
				return RenderConfiguration.showTransfers();
			case DEATH_EXPLOSION:
				return RenderConfiguration.showExplosions();
			case AURA:
				return RenderConfiguration.showSpawnRadii();
			case MORTAR_ATTACK:
				return true;
			case MORTAR_EXPLOSION:
				return RenderConfiguration.showDetonates();
			case TELEPORT:
				return RenderConfiguration.showTeleport();
			default:
				System.out.println("FIXME: AbstractAnimation.AnimationType.shown() missing case");
				return false;
			}
		}

	}

	protected final int lifetime;
	protected int roundsToLive;

	protected AbstractAnimation(int roundsToLive) {
		lifetime = roundsToLive;
		this.roundsToLive = roundsToLive;
	}

	public void updateRound() { roundsToLive--; }

	public boolean isAlive() { return (roundsToLive > 0); }

	public int roundAge() { return lifetime - roundsToLive; }

	public abstract Object clone();

}