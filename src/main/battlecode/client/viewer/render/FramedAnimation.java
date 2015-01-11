package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

abstract class FramedAnimation extends Animation {
    static ImageResource<String> ir = new ImageResource<String>();
    protected final MapLocation loc;
    // not the most descriptive name
    // to let images have arbitrary aspect ratios, this is the width scale
    protected final double width;
    
    FramedAnimation(MapLocation loc, double width, int maxFrames) {
	super(maxFrames);
	this.loc = loc;
	this.width = width;

    }
    
    protected boolean shouldDraw() {
	return true;
    }

    abstract String fileFormatString();

    abstract int offset();
    
    public void draw(Graphics2D g2) {
	if (shouldDraw()) {
	    String path = String.format(fileFormatString(), this.curFrame + offset());
	    java.awt.image.BufferedImage img = ir.getResource(path, path).image;
	    AffineTransform pushed = g2.getTransform(); {
		if (loc != null) {
		    g2.translate(loc.x, loc.y);
		}
		// not sure why I wanted things centered
		//g2.translate(-0.5*(width - 1), -0.5*(width - 1));
		g2.scale(width/img.getWidth(), width/img.getWidth());
		g2.drawImage(img, null, null);
	    } g2.setTransform(pushed);
	}
    }

}
