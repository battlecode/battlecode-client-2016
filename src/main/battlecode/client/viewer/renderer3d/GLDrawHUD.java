package battlecode.client.viewer.renderer3d;

import battlecode.common.*;
import battlecode.client.util.*;

import java.util.List;
import java.util.ConcurrentModificationException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;

public class GLDrawHUD {
	private static final int numArchons = GameConstants.NUMBER_OF_ARCHONS;

	private final GLDrawState ds;
	private final Team team;

	public GLDrawHUD(GLDrawState ds, Team team) {
		this.ds = ds;
		this.team = team;
	}

	public void draw(GL2 gl, GLU glu, GLDrawState ds) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glColor3f(0.5f, 0.5f, 0.5f);
		
		try {
			java.util.List<GLDrawObject> archons = ds.getArchons(team);
			int i;
			for (i = 0; i < archons.size(); ++i) {
				drawRobot(gl, glu, archons.get(i));
			}
			for (; i < numArchons; ++i) {
				drawRobot(gl, glu, null);
			}
			drawRobot(gl, glu, ds.getPowerCore(team));
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
		}
	}

	public void drawRobot(GL2 gl, GLU glu, GLDrawObject r) {
		r.draw(gl, glu, false);
	}
}

