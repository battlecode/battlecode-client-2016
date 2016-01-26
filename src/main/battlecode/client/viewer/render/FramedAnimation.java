package battlecode.client.viewer.render;

import battlecode.client.util.ImageResource;
import battlecode.common.MapLocation;

import java.awt.*;
import java.awt.geom.AffineTransform;

abstract class FramedAnimation extends UnitAnimation {
    static final ImageResource<String> ir = new ImageResource<>();

    FramedAnimation(int maxFrames) {
        super(maxFrames);
    }

    protected boolean shouldDraw() {
        return true;
    }

    abstract String fileFormatString();

    abstract int offset();

    @Override
    public void draw(Graphics2D g2, boolean isHUD) {
        if (shouldDraw()) {
            String path = String.format(fileFormatString(), this.curFrame +
                    offset());
            java.awt.image.BufferedImage img = ir.getResource(path, path).image;
            AffineTransform pushed = g2.getTransform();
            {
                // not sure why I wanted things centered
                //g2.translate(-0.5*(width - 1), -0.5*(width - 1));
                g2.scale(1. / img.getWidth(), 1. / img.getWidth());
                g2.drawImage(img, null, null);
            }
            g2.setTransform(pushed);
        }
    }

}
