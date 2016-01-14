package battlecode.client.util;

import battlecode.client.viewer.render.RenderConfiguration;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that pre-renders images so that we don't have to draw them later.
 */
public class PrerenderedGraphics {
    private BufferedImage broadcastImage;
    private BufferedImage zombieInfectionImage;
    private BufferedImage viperInfectionImage;
    private Map<Integer, BufferedImage> partsImages; // key = radius * 100
    private int spriteSize = 0;

    public PrerenderedGraphics() {
        partsImages = new HashMap<>();
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
        prerenderPartsImages();
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
        zombieInfectionImage = makeInfectionImage(green);
        viperInfectionImage = makeInfectionImage(purple);
    }

    public void prerenderPartsImages() {
        for (int i = 10; i <= 100; i += 10) {
            partsImages.put(i, makePartsImage(i * 0.01));
        }
    }

    public BufferedImage makeInfectionImage(Color infectionColor) {
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
        rectLeft = new Rectangle2D.Float(0.12f * spriteSize, 0.9f *
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

    public BufferedImage makePartsImage(double radius) {
        BufferedImage result = new BufferedImage(spriteSize, spriteSize,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        Color parts = new Color(255, 140, 25, 255);
        g2.setColor(parts);
        Ellipse2D.Double circle = new Ellipse2D.Double((0.5 - radius) *
                spriteSize, (0.5 - radius) * spriteSize, 2 * radius *
                spriteSize, 2 * radius * spriteSize);
        g2.fill(circle);
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

    public BufferedImage getPartsImage(double radius) {
        updateSpriteSize();
        // map to 10, 20, 30, ..., 100
        int radius2 = (int) (radius * 100);
        radius2 -= radius2 % 10;
        radius2 = Math.min(radius2, 100);
        radius2 = Math.max(radius2, 10);
        return partsImages.get(radius2);
    }
}
