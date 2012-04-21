package battlecode.client.viewer.renderer3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Vector4f;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.MapLocation;

class GLExplosionAnim extends GLAnimation {

	//private static ImageResource<Integer> ir = new ImageResource<Integer>();

	private final MapLocation loc;
	private final double size;
	private final boolean isArchon;

	public GLExplosionAnim(boolean isArchon) { this(isArchon, null, 1); }

	public GLExplosionAnim(boolean isArchon, MapLocation loc) { this(isArchon, loc, 1); }

	public GLExplosionAnim(boolean isArchon, MapLocation loc, double size) {
		super(isArchon ? 16 : 10);
		
		this.loc = loc;
		this.size = size;
		this.isArchon = isArchon;
	}

	public MapLocation getLocation() {
		return loc;
	}

	public Object clone() {
		GLExplosionAnim clone = new GLExplosionAnim(isArchon, loc, size);
		clone.roundsToLive = roundsToLive;
		return clone;
	}

	private static GLUquadric q = null;
	private final static Vector4f startColor = new Vector4f(2.0f, 2.0f, 0.0f, 1.0f);
	private final static Vector4f endColor = new Vector4f(2.0f, 1.0f, 0.0f, 0.55f);
	
	public void draw(GL2 gl, GLU glu) {
		if(q == null) 
			q = glu.gluNewQuadric();
		
		// TODO Auto-generated method stub
		if(RenderConfiguration.showExplosions()) {
			int delta = lifetime - roundsToLive;
			float frac = (float)delta / lifetime;
			
			// pick a color
			Vector4f currentColor = new Vector4f();
			currentColor.interpolate(startColor, endColor, frac);
			
			gl.glColor4f(currentColor.x, currentColor.y, currentColor.z, currentColor.w);
			gl.glPushMatrix();
			gl.glTranslatef(0.5f, 0.0f, 0.5f);
			if(isArchon)
				glu.gluSphere(q, 1.0 + frac * 2.0, 12, 12);
			else
				glu.gluSphere(q, 0.4 + frac * 0.5, 8, 8);
			gl.glPopMatrix();
		}
	}
}
