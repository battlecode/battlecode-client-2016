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
			case DEATH_EXPLOSION:
				return RenderConfiguration.showExplosions();
			case AURA:
				return RenderConfiguration.showSpawnRadii();
			case MORTAR_ATTACK:
				return true;
                        case ENERGON_TRANSFER:
                          return true;
			default:
				return false;
			}
		}

	}

	protected final int maxFrame;
	protected int curFrame;

    protected boolean loops() { return false; }

	protected AbstractAnimation(int maxFrame) {
	    this.maxFrame = maxFrame;
	    this.curFrame = 0;
	}

	public void updateRound() {
	    this.curFrame++;
	    if (loops()) {
		curFrame %= maxFrame;
	    }
	}

	public boolean isAlive() {
	    return loops() || (curFrame < maxFrame);
	}

	public int roundAge() { return curFrame; }

	public abstract Object clone();

}
