package battlecode.client.viewer.render;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.Team;

import java.awt.*;
import java.awt.geom.GeneralPath;

public class TransferAnim extends Animation {

    public enum TransferAnimType {
        REPAIR, ACTIVATION, INFECTION
    }

    private static final Color activationColorRed = Color.RED;
    private static final Color activationColorBlue = Color.BLUE;
    private static final Color repairColor = Color.MAGENTA;
    private static final Color infectionColor = Color.GREEN;

    private final TransferAnimType type;
    private static final GeneralPath polygon = new GeneralPath();
    private final MapLocation target;
    private final float dx, dy;
    private DrawObject src;
    private final Color color;

    public TransferAnim(DrawObject src, MapLocation target, TransferAnimType
            type) {
        super(10);
        this.type = type;
        this.src = src;
        for (int i = 0; i < 20; ++i) {
            target = target.add(Direction.SOUTH_EAST);
        }
        this.target = target;
        float maxWidth = 1.0f;
        float Dx = (target.x - src.getDrawX());
        float Dy = (target.y - src.getDrawY());
        System.out.println(Dx  + " " + Dy);
        float len = (float) (Math.hypot(Dx, Dy));
        if (len < 0.001f) len = 1;
        dx = Dx / len * maxWidth;
        dy = Dy / len * maxWidth;
        System.out.println(dx + " | " + dy);
        switch (type) {
            case REPAIR: color = repairColor; break;
            case ACTIVATION: color = src.getTeam() == Team.A ?
                    activationColorRed : activationColorBlue; break;
            case INFECTION: color = infectionColor; break;
            default: color = repairColor;
        }
    }

    public void setSource(DrawObject src) {
        this.src = src;
    }

    public void draw(Graphics2D g2) {
        if (RenderConfiguration.showTransfers()) {
            System.out.println("DRAWING2");
            polygon.reset();
            polygon.moveTo(src.getDrawX() + 0.5f, src.getDrawY() + 0.5f);
            float width = Math.min(maxFrame - curFrame, curFrame) / (float)
                    maxFrame;
            System.out.println("Width = " + width);
            float drawX = target.x;
            float drawY = target.y;
            System.out.println((src.getDrawX() + 0.5f) + ",," + (src.getDrawY
                    () + 0.5));
            polygon.lineTo(drawX + 0.5f - dx * width,
                    drawY + 0.5f - dy * width);
            System.out.println((drawX + 0.5f - dx * width) + ",,," + (drawY +
                    0.5f - dy * width));
            polygon.lineTo(drawX + 0.5f + dx * width,
                    drawY + 0.5f + dy * width);
            System.out.println((drawX + 0.5 + dx * width) + ",,,," + (drawY +
            0.5f + dy * width));
            polygon.closePath();
            System.out.println("path is " + polygon);
            g2.setColor(color);
            g2.fill(polygon);
        }
    }

    public Object clone() {
        TransferAnim clone = new TransferAnim(src, target, type);
        clone.curFrame = curFrame;
        return clone;
    }
}
