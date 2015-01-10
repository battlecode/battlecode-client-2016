package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

class MortarAttackAnim extends Animation {

    private static ImageFile mortarShell = new ImageFile("art/tank_shell.png");
    private final MapLocation src;
    private final MapLocation dst;
    private final int dx;
    private final int dy;

    public MortarAttackAnim(MapLocation src, MapLocation dst) {
        super(5);
        this.src = src;
        this.dst = dst;
        dx = dst.x - src.x;
        dy = dst.y - src.y;
    }

    public void draw(Graphics2D g2) {
        AffineTransform pushed = g2.getTransform();
        {
            double frac = (double) roundAge() / 5;
            g2.translate(src.x + dx * frac,
                    src.y + dy * frac - (1 - (2 * frac - 1) * (2 * frac - 1)));
            java.awt.image.BufferedImage shell = mortarShell.image;
            g2.scale(1.0 / shell.getWidth(), 1.0 / shell.getHeight());
            g2.drawImage(shell, null, null);
        }
        g2.setTransform(pushed);
    }

    public Object clone() {
        MortarAttackAnim clone = new MortarAttackAnim(src, dst);
        clone.curFrame = curFrame;
        return clone;
    }

    public MapLocation getTargetLoc() {
        return dst;
    }

}
