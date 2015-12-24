package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractAnimation;

import java.awt.*;

abstract class Animation extends AbstractAnimation {

    protected Animation(int roundsToLive) {
        super(roundsToLive);
    }

    public abstract void draw(Graphics2D g2);

}
