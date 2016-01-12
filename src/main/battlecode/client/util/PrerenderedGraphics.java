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
    private int spriteSize = 0;

    public PrerenderedGraphics() {
        updateSpriteSize();
    }

    public void updateSpriteSize() {
        if (spriteSize != (int) RenderConfiguration.getInstance()
                .getSpriteSize()) {
            spriteSize = (int) RenderConfiguration.getInstance()
                    .getSpriteSize();
            prerender();
        }
    }

    public void prerender() {
        prerenderBroadcastImage();
        prerenderInfectionImages();
    }

    public void prerenderBroadcastImage() {
        broadcastImage = new BufferedImage(spriteSize, spriteSize,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = broadcastImage.createGraphics();
        g2.dispose();
    }

    public void prerenderInfectionImages() {
        Color green = new Color(0.5f, 1.0f, 0.5f, 1.0f);
        Color purple = new Color(0.5f, 0.15f, 0.8f, 1.0f);
        zombieInfectionImage = getInfectionImage(green);
        viperInfectionImage = getInfectionImage(purple);
    }

    public BufferedImage getInfectionImage(Color infectionColor) {
        BufferedImage result = new BufferedImage(spriteSize,
                spriteSize, BufferedImage.TYPE_INT_ARGB);
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
        updateSpriteSize();
        return broadcastImage;
    }

    public BufferedImage getZombieInfectionImage() {
        updateSpriteSize();
        return zombieInfectionImage;
    }

    public BufferedImage getViperInfectionImage() {
        updateSpriteSize();
        return viperInfectionImage;
    }
}
