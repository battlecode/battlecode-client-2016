package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

class TeleportAnim extends Animation {

	private final MapLocation loc;
	private final MapLocation src;


	public TeleportAnim(MapLocation src, MapLocation loc) {
		super(10);
		this.loc = loc;
		this.src = src;
	}

	public void draw(Graphics2D g2) {
		if(RenderConfiguration.showTeleport()) {
			g2.setColor(new Color(0.0f, 0.7f, 0.4f, 0.7f));
			int delta = lifetime - roundsToLive;
			float frac = 1.0f - (float)delta / lifetime;
			
			g2.fill(new Ellipse2D.Float(0.5f-frac*2,0.5f-frac*2,	4*frac, 4*frac));
		}
	}

	public Object clone() {
		TeleportAnim clone = new TeleportAnim(src, loc);
		clone.roundsToLive = roundsToLive;
		return clone;
	}
}
