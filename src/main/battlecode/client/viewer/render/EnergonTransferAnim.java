package battlecode.client.viewer.render;

import battlecode.common.*;

import java.awt.*;
import java.awt.geom.*;

class EnergonTransferAnim extends Animation {

	private static final Color energonColor = Color.GREEN;
	private static final Color fluxColor = Color.MAGENTA;

    private static final GeneralPath polygon = new GeneralPath();
    private final MapLocation target;
    private final float amount;
    private final float dx, dy;
    private DrawObject src;
	private final Color color;

    public EnergonTransferAnim(DrawObject src, MapLocation target, float amount, boolean isFlux) {
        super(10);
        this.src = src;
        this.target = target;
        this.amount = amount;
        float maxWidth = 0.75f * amount / (amount + 10) + 0.25f;
        float Dx = (target.x - src.getDrawX());
        float Dy = (target.y - src.getDrawY());
        float len = (float) (Math.hypot(Dx, Dy));
        if (len < 0.001f) len = 1;
        dx = Dx / len * maxWidth;
        dy = Dy / len * maxWidth;
		color = isFlux?fluxColor:energonColor;
        
    }

    public void setSource(DrawObject src) {
        this.src = src;
    }

    public void draw(Graphics2D g2) {
        if (RenderConfiguration.showSupplyTransfers()) {
            polygon.reset();
            polygon.moveTo(src.getDrawX() + 0.5f, src.getDrawY() + 0.5f);
            float width = Math.min(maxFrame - curFrame, curFrame) / (float)maxFrame;
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
        EnergonTransferAnim clone = new EnergonTransferAnim(src, target, amount,color==fluxColor);
        clone.curFrame = curFrame;
        return clone;
    }
}
