package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

class DoodadAnim extends FramedAnimation {
    protected int w, h;
    
    public DoodadAnim() { this(null); }

    public DoodadAnim(MapLocation loc) { this(loc, 1, 2, 1); }

    public DoodadAnim(MapLocation loc, double size, int w, int h) {
	super(loc, size, 140);
	this.w = w;
	this.h = h;
    }

    protected boolean loops() {
	return true;
    }

    int offset() {
	return 0;
    }

    public String fileFormatString() {
	return "art/agnaktor/frame_%03d.png";
    }

    protected boolean shouldDraw() {
	return true;
    }


    public Object clone() {
	DoodadAnim clone = new DoodadAnim(loc, size, w, h);
	clone.curFrame = curFrame;
	return clone;
    }
}
