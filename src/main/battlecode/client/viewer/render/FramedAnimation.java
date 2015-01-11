package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

abstract class FramedAnimation extends Animation {
    static ImageResource<Integer> ir = new ImageResource<Integer>();
    protected final MapLocation loc;
    protected final double size;
    
    FramedAnimation(MapLocation loc, double size, int maxFrames) {
	super(maxFrames);
	this.loc = loc;
	this.size = size;

    }
    
    protected boolean shouldDraw() {
	return true;
    }

    abstract String fileFormatString();

    abstract int offset();
    
    public void draw(Graphics2D g2) {
	if (shouldDraw()) {
	    String path = String.format(fileFormatString(), this.curFrame + offset());
	    java.awt.image.BufferedImage img = ir.getResource(curFrame, path).image;
	    AffineTransform pushed = g2.getTransform(); {
		if (loc != null) {
		    g2.translate(loc.x, loc.y);
		}
		g2.translate(-0.5*(size - 1), -0.5*(size - 1));
		g2.scale(size/img.getWidth(), size/img.getHeight());
		g2.drawImage(img, null, null);
	    } g2.setTransform(pushed);
	}
    }

}
