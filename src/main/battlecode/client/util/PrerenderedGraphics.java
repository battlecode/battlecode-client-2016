package battlecode.client.util;

import battlecode.client.viewer.render.RenderConfiguration;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * A class that pre-renders images so that we don't have to draw them later.
 */
public class PrerenderedGraphics {
    private BufferedImage broadcastImage;
    private BufferedImage zombieInfectionImage;
    private BufferedImage viperInfectionImage;

    public PrerenderedGraphics() {
        prerender(RenderConfiguration.getInstance().getSpriteSize());
    }

    public void prerender(float spriteSize) {
        prerenderBroadcastImage(RenderConfiguration.getInstance().getSpriteSize
                ());
        prerenderInfectionImages(RenderConfiguration.getInstance()
                .getSpriteSize());
    }

    public void prerenderBroadcastImage(float spriteSize) {
        broadcastImage = new BufferedImage((int) Math.ceil(spriteSize),
                (int) Math.ceil(spriteSize), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = broadcastImage.createGraphics();
        g2.dispose();
    }

    public void prerenderInfectionImages(float spriteSize) {
        Color green = new Color(0.5f, 1.0f, 0.5f, 1.0f);
        Color purple = new Color(0.5f, 0.15f, 0.8f, 1.0f);
        zombieInfectionImage = getInfectionImage(spriteSize, green);
        viperInfectionImage = getInfectionImage(spriteSize, purple);
    }

    public BufferedImage getInfectionImage(float spriteSize, Color
        infectionColor) {
        BufferedImage result = new BufferedImage((int) Math.ceil(spriteSize),
                (int) Math.ceil(spriteSize), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();

        g2.setColor(infectionColor);
        Rectangle2D.Float rectLeft;
        rectLeft = new Rectangle2D.Float(0.02f * spriteSize, 0, 0.1f *
                spriteSize, 1 * spriteSize);
        g2.fill(rectLeft);
        rectLeft = new Rectangle2D.Float(0.12f * spriteSize, 0, 0.2f *
                spriteSize, 0.1f * spriteSize);
        g2.fill(rectLeft);
        rectLeft = new Rectangle2D.Float(0.12f * spriteSize,0.9f *
                spriteSize, 0.2f * spriteSize, 0.1f * spriteSize);
        g2.fill(rectLeft);

        Rectangle2D.Float rectRight;
        rectRight = new Rectangle2D.Float(0.88f * spriteSize, 0, 0.1f *
                spriteSize, 1 * spriteSize);
        g2.fill(rectRight);
        rectRight = new Rectangle2D.Float(0.68f * spriteSize, 0, 0.2f *
                spriteSize, 0.1f * spriteSize);
        g2.fill(rectRight);
        rectRight = new Rectangle2D.Float(0.68f * spriteSize, 0.9f *
                spriteSize, 0.2f * spriteSize, 0.1f * spriteSize);
        g2.fill(rectRight);
        g2.dispose();

        return result;
    }

    public BufferedImage getBroadcastImage() {
        return broadcastImage;
    }

    public BufferedImage getZombieInfectionImage() {
        return zombieInfectionImage;
    }

    public BufferedImage getViperInfectionImage() {
        return viperInfectionImage;
    }
}
