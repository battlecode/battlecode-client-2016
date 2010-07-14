package battlecode.client.viewer.render;

import battlecode.common.*;

import java.awt.*;
import java.awt.geom.*;

class AuraAnimation extends Animation {

    private static final int duration = 1;
    private static final int alpha = 150;
	private static final Color [] colors = new Color[] {
		new Color(255, 179, 61, alpha),
		new Color(0, 61, 245, alpha),
		new Color(0, 245, 61, alpha)
	};

    private final Color auraColor;
    private MapLocation loc;

	public AuraAnimation(AuraAnimation a) {
		super(duration);
        this.loc = a.loc;
		this.auraColor = a.auraColor;
	}
	
    public AuraAnimation(AuraType auraType, MapLocation loc) {
		super(duration);
		this.loc = loc;
		this.auraColor = colors[auraType.ordinal()];
    }

    public void draw(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform();
     
		g2.translate(loc.getX(),loc.getY());

		g2.setColor(auraColor);
        float r = (float) Math.sqrt(RobotType.AURA.sensorRadiusSquared());
        g2.fill(new Ellipse2D.Float(0 + 0.5f - r, 0 + 0.5f - r, 2 * r, 2 * r));

		g2.setTransform(pushed);
    }

    public AuraAnimation clone() {
        AuraAnimation clone = new AuraAnimation(this);
        return clone;
    }
}
