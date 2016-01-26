package battlecode.client.viewer.render;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 
 * @author james
 */
public class ClearAnimation extends UnitAnimation {
    private static final Random random = new Random();
    public static final int FRAMES = 10;
    public static final int VARIANTS = 10;

    // Note: due to quirks in the animation system, clear unitAnimations will follow robots around.
    private int dx, dy;
    private int variantIndex;

    public ClearAnimation(int dx, int dy) {
        super(FRAMES);

        this.dx = dx;
        this.dy = dy;
        this.variantIndex = random.nextInt(VARIANTS);
    }

    private ClearAnimation(int dx, int dy, int variantIndex, int curFrame) {
        super(FRAMES);

        this.dx = dx;
        this.dy = dy;
        this.variantIndex = variantIndex;
        this.curFrame = curFrame;
    }

    @Override
    public void draw(Graphics2D g2, boolean isHUD) {
        if (isHUD) return;

        BufferedImage i = GameRenderer.pg.getClearingFrame(variantIndex, curFrame);
        AffineTransform pushed = g2.getTransform();
        g2.translate(dx, dy);
        g2.scale(1f/i.getWidth(), 1f/i.getHeight());
        g2.drawImage(i, null, null);
        g2.setTransform(pushed);
    }

    @Override
    public boolean shown() {
        return RenderConfiguration.showClearing();
    }

    @Override
    public void unitMoved(int dx, int dy) {
        this.dx -= dx;
        this.dy -= dy;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public ClearAnimation clone() {
        return new ClearAnimation(dx, dy, variantIndex, curFrame);
    }
}
