package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

class ExplosionAnim extends Animation {

	private static ImageResource<Integer> ir = new ImageResource<Integer>();

	private final MapLocation loc;
	private final double size;

	public static enum ExplosionToggle {
		EXPLOSIONS,
		DETONATES
	}

	private ExplosionToggle toggle = ExplosionToggle.EXPLOSIONS; 

	public ExplosionAnim() { this(null, 1); }

	public ExplosionAnim(MapLocation loc) { this(loc, 1); }

	public ExplosionAnim(MapLocation loc, double size) {
		super(8);
		this.loc = loc;
		this.size = size;
	}

	public void setExplosionToggle(ExplosionToggle t) {
		toggle = t;
	}

	protected boolean shouldDraw() {
		switch(toggle) {
		case EXPLOSIONS:
			return RenderConfiguration.showExplosions();
		case DETONATES:
			return RenderConfiguration.showDetonates();
		default:
			return false;
		}
	}

	public void draw(Graphics2D g2) {
		if (shouldDraw()) {
			int frame = lifetime - roundsToLive;
			String path = String.format("art/explode/explode64_f%02d.png", frame + 1);
			java.awt.image.BufferedImage img = ir.getResource(frame, path).image;
			AffineTransform pushed = g2.getTransform(); {
				if (loc != null) {
					g2.translate(loc.getX(), loc.getY());
				}
				g2.translate(-0.5*(size - 1), -0.5*(size - 1));
				g2.scale(size/img.getWidth(), size/img.getHeight());
				g2.drawImage(img, null, null);
			} g2.setTransform(pushed);
		}
	}

	public Object clone() {
		ExplosionAnim clone = new ExplosionAnim(loc, size);
		clone.roundsToLive = roundsToLive;
		clone.toggle = toggle;
		return clone;
	}
}
