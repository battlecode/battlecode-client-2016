package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;

class ExplosionAnim extends FramedAnimation {
    public static enum ExplosionToggle {
	EXPLOSIONS,
    }

    protected ExplosionToggle toggle = ExplosionToggle.EXPLOSIONS; 

    public ExplosionAnim() { this(null, 1); }

    public ExplosionAnim(MapLocation loc) { this(loc, 1); }

    public ExplosionAnim(MapLocation loc, double width) {
	super(loc, width, 4);
    }

    protected boolean loops() {
	return false;
    }

    int offset() {
	return 1;
    }

    public String fileFormatString() {
	return "art/explode/explode64_f%02d.png";
    }

    public void setExplosionToggle(ExplosionToggle t) {
	toggle = t;
    }

    protected boolean shouldDraw() {
	switch(toggle) {
	case EXPLOSIONS:
	    return RenderConfiguration.showExplosions();
	default:
	    return false;
	}
    }


    public Object clone() {
	ExplosionAnim clone = new ExplosionAnim(loc, width);
	clone.curFrame = curFrame;
	clone.toggle = toggle;
	return clone;
    }
}
