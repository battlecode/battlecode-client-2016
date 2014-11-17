package battlecode.client.viewer.renderer3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;


import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

class GLTeleportAnim extends GLAnimation {
	private final MapLocation target;

	
	private GLDrawObject src;

	public GLTeleportAnim(GLDrawObject src, MapLocation target) {
		super(10);
		this.src = src;
		this.target = target;
	}

	public void setSource(GLDrawObject src) {
		this.src = src;
	}

	public void draw(GL2 gl, GLU glu) {
		final GLUquadric quadric= glu.gluNewQuadric();
		int delta = lifetime - roundsToLive;
		float frac = 1.0f - (float)delta / lifetime;
		
		gl.glPushMatrix();
		gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
		gl.glColor4f(0.0f, 0.7f, 0.4f, 0.7f);
		glu.gluSphere(quadric, 1.0 + frac * 2.0, 12, 12);
		
		gl.glPopMatrix();
	}
/*
	public void draw(GL2 gl, GLDrawMap map, MapLocation origin) {

		// do nothing
		
	}
	*/

	public Object clone() {
		GLTeleportAnim clone = new GLTeleportAnim(src, target);
		clone.roundsToLive = roundsToLive;
		return clone;
	}
}
