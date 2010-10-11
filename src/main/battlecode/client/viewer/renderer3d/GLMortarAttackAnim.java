package battlecode.client.viewer.renderer3d;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Vector3f;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

class GLMortarAttackAnim extends GLAnimation {
	private final MapLocation src;
	private final MapLocation dst;
	private final int dx;
	private final int dy;
	private Vector3f ballPos;
	private GLUquadric q = null;
	private int counter;
	
	public GLMortarAttackAnim(MapLocation src, MapLocation dst) {
		super( 5 );
		this.src = src;
		this.dst = dst;
		dx = dst.getX() - src.getX();
		dy = dst.getY() - src.getY();
		ballPos = new Vector3f();
		counter = 0;
	}

	/*
	 * public void draw(Graphics2D g2) { AffineTransform pushed =
	 * g2.getTransform(); { double frac = (double) roundAge() /
	 * GameConstants.MORTAR_DELAY_ROUNDS; g2.translate(src.getX() + dx*frac,
	 * src.getY() + dy*frac - (1 - (2*frac-1)*(2*frac-1)));
	 * java.awt.image.BufferedImage shell = mortarShell.image;
	 * g2.scale(1.0/shell.getWidth(), 1.0/shell.getHeight());
	 * g2.drawImage(shell, null, null); } g2.setTransform(pushed); }
	 */

	public Object clone() {
		GLMortarAttackAnim clone = new GLMortarAttackAnim(src, dst);
		clone.roundsToLive = roundsToLive;
		return clone;
	}

	public MapLocation getTargetLoc() {
		return dst;
	}


	public void draw(GL gl, GLU glu) {
		
	}
	
	public void draw(GL gl, GLU glu, GLDrawMap map, MapLocation origin) {
		/*if (q == null)
			q = glu.gluNewQuadric();

		float frac = (float) roundAge() / 5;
		float srcHeight = map.getMapHeight(src.getX() - origin.getX() + 0.5f,
				src.getY() - origin.getY() + 0.5f);
		float dstHeight = map.getMapHeight(dst.getX() - origin.getX() + 0.5f,
				dst.getY() - origin.getY() + 0.5f);
		
		ballPos.x = (1.0f - frac) * (src.getX()) + frac * (dst.getX()) + 0.5f;
		ballPos.z = (1.0f - frac) * (src.getY()) + frac * (dst.getY()) + 0.5f;
		final float m = Math.max(srcHeight, dstHeight) + 4;
		final float a = 2 * dstHeight + 2 * srcHeight - 2 * m;
		final float b = 2 * m - dstHeight - 3 * srcHeight;
		final float c = srcHeight;
		ballPos.y = frac * frac * a + frac * b + c;

		gl.glPushMatrix();
		gl.glTranslatef(ballPos.x, ballPos.y, ballPos.z);
		counter = (counter + 1) % 10;
		if(counter < 5)
			gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		else
			gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
		glu.gluSphere(q, 0.1f, 4, 4);
		gl.glPopMatrix();*/
	}
}
