package battlecode.client.viewer.renderer3d;

import battlecode.client.viewer.AbstractAnimation;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

abstract class GLAnimation extends AbstractAnimation {

	protected GLAnimation(int roundsToLive) {
		super(roundsToLive);
	}

	public abstract void draw(GL gl, GLU glu);

}
