package battlecode.client.viewer.render;

import battlecode.common.MapLocation;

public class ExplosionAnim extends FramedAnimation {
    public enum ExplosionToggle {
        EXPLOSIONS,
    }

    protected ExplosionToggle toggle = ExplosionToggle.EXPLOSIONS;

    public ExplosionAnim() {
        super(4);
    }

    @Override
    public boolean shown() {
        return RenderConfiguration.showExplosions();
    }

    @Override
    public void unitMoved(int dx, int dy) {
        // Can't actually happen, so we don't do anything.
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

    protected boolean shouldDraw() {
        switch (toggle) {
            case EXPLOSIONS:
                return RenderConfiguration.showExplosions();
            default:
                return false;
        }
    }


    public Object clone() {
        ExplosionAnim clone = new ExplosionAnim();
        clone.curFrame = curFrame;
        clone.toggle = toggle;
        return clone;
    }
}
