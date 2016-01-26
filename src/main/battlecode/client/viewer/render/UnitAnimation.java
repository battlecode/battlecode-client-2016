package battlecode.client.viewer.render;

import java.awt.*;

public abstract class UnitAnimation implements Cloneable {

    protected final int maxFrame;
    protected int curFrame;

    protected UnitAnimation(int roundsToLive) {
        this.maxFrame = roundsToLive;
        this.curFrame = 0;
    }

    // NOTE: g2 is in a coordinate system where 1 is the width of a sprite
    // and (0,0) is the upper left corner of the *tile containing this animation's
    // parent*.
    public abstract void draw(Graphics2D g2, boolean isHUD);

    public abstract boolean shown();

    public abstract void unitMoved(int dx, int dy);

    protected boolean loops() {
        return false;
    }

    public void updateRound() {
        this.curFrame++;
        if (loops()) {
            curFrame %= maxFrame;
        }
    }

    public boolean isAlive() {
        return loops() || (curFrame < maxFrame);
    }

    public abstract Object clone();
}
