package battlecode.client.viewer.render;

import battlecode.common.*;

import java.awt.*;
import java.awt.geom.*;

public class TransferAnim extends Animation {

    private static final GeneralPath polygon = new GeneralPath();
    private final MapLocation target;
    private final float dx, dy;
    private DrawObject src;
    private final Color color;

    public TransferAnim(DrawObject src, MapLocation target) {
        super(10);
        this.src = src;
        this.target = target;
        float maxWidth = 1.0f;
        float Dx = (target.x - src.getDrawX());
        float Dy = (target.y - src.getDrawY());
        float len = (float) (Math.hypot(Dx, Dy));
        if (len < 0.001f) len = 1;
        dx = Dx / len * maxWidth;
        dy = Dy / len * maxWidth;
        switch (src.getTeam()) {
            case A: color = Color.RED; break;
            case B: color = Color.BLUE; break;
            case NEUTRAL: color = Color.GRAY; break;
            case ZOMBIE: color = Color.GREEN; break;
            default: color = Color.GRAY; break;
        }
    }

    public void draw(Graphics2D g2) {
        if (RenderConfiguration.showTransfers()) {
            System.out.println("drawing transfer anim!");
            polygon.reset();
            polygon.moveTo(src.getDrawX() + 0.5f, src.getDrawY() + 0.5f);
            float width = Math.min(maxFrame - curFrame, curFrame) / (float) maxFrame;
            float drawX = target.x;
            float drawY = target.y;
            polygon.lineTo(drawX + 0.5f - dx * width,
                    drawY + 0.5f - dy * width);
            polygon.lineTo(drawX + 0.5f + dx * width,
                    drawY + 0.5f + dy * width);
            polygon.closePath();
            g2.setColor(color);
            g2.fill(polygon);
        }
    }

    public Object clone() {
        TransferAnim clone = new TransferAnim(src, target);
        clone.curFrame = curFrame;
        return clone;
    }
}
