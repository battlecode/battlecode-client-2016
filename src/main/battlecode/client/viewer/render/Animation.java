package battlecode.client.viewer.render;

import java.awt.*;

public abstract class Animation implements Cloneable {

    protected final int maxFrame;
    protected int curFrame;

    protected Animation(int roundsToLive) {
        this.maxFrame = roundsToLive;
        this.curFrame = 0;
    }

    public abstract void draw(Graphics2D g2);

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

    public int roundAge() {
        return curFrame;
    }

    public abstract Object clone();

    public enum AnimationType {
        TRANSFER,
        DEATH_EXPLOSION,
        AURA,
        MORTAR_ATTACK,
        MORTAR_EXPLOSION,
        TELEPORT;

        public boolean shown() {
            switch (this) {
                case DEATH_EXPLOSION:
                    return RenderConfiguration.showExplosions();
                case AURA:
                    return RenderConfiguration.showSpawnRadii();
                case MORTAR_ATTACK:
                    return true;
                case TRANSFER:
                    return true;
                default:
                    return false;
            }
        }

    }
}
