package battlecode.client.viewer.render;

import battlecode.common.*;

import java.awt.*;
import java.awt.geom.*;

class EMPDetonationAnim extends Animation {

	private static final int duration = 20;
	private static final float dColor = 1.0f / duration;

	private MapLocation loc;

	public EMPDetonationAnim(MapLocation center) {
		super(duration);
		loc = center;
	}
    
    
	public void draw(Graphics2D g2) {
		g2.setColor(new Color(1-roundAge()*dColor, 1, 1, 1-roundAge()*dColor));
		float r = 25 *
		    (1 - (float) Math.pow((double) (maxFrame - curFrame) / maxFrame, 3.5));
		g2.fill(new Ellipse2D.Float(loc.x+0.5f-r, loc.y+0.5f-r,	2*r, 2*r));
	}

	public Object clone() {
		EMPDetonationAnim clone = new EMPDetonationAnim(loc);
		clone.curFrame = curFrame;
		return clone;
	}
}
