package battlecode.client.viewer.render;

import battlecode.common.MapLocation;
import battlecode.common.Team;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

public class LargeExplosionAnim extends ExplosionAnim {

    private Team team;

    public LargeExplosionAnim(Team team) {
        super();
        this.team = team;
    }

    public LargeExplosionAnim(MapLocation loc, double width, Team team) {
        super(loc, width);
        this.team = team;
    }

    public void draw(Graphics2D g2) {
        super.draw(g2);
        if (shouldDraw()) {
            AffineTransform pushed = g2.getTransform();
            if (loc != null) {
                g2.translate(loc.x, loc.y);
            }
            g2.translate(-0.5 * (width - 1), -0.5 * (width - 1));
            Color color;
            switch (this.team) {
                case A: color = new Color(1, 0, 0, 0.5f); break;
                case B: color = new Color(0, 0, 1, 0.5f); break;
                case NEUTRAL: color = new Color(0f, 0f, 0, 0.5f); break;
                case ZOMBIE: color = new Color(0, 1, 0, 0.5f); break;
                default: color = new Color(1, 0, 1, 0.3f);
            }
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new Ellipse2D.Float(-.5f, -.5f, 2, 2));
            g2.setTransform(pushed);
        }
    }
}
