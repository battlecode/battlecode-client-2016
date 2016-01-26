package battlecode.client.util;

import battlecode.client.viewer.render.ClearAnimation;
import battlecode.client.viewer.render.RenderConfiguration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A class that pre-renders images so that we don't have to draw them later.
 */
public class PrerenderedGraphics {
    private BufferedImage broadcastImage;
    private BufferedImage zombieInfectionImage;
    private BufferedImage viperInfectionImage;
    private Map<Integer, BufferedImage> partsImages; // key = radius * 100
    // We have multiple sets of these so that they don't look repetetive.
    private BufferedImage[][] clearingAnimations;
    private int spriteSize = 0;

    private Random random;

    public PrerenderedGraphics() {
        partsImages = new HashMap<>();

        // We don't have to care about determinism because this is the client!
        random = new Random();

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
        prerenderClearingFrames();
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

    public void prerenderClearingFrames() {
        clearingAnimations = new BufferedImage[ClearAnimation.VARIANTS][ClearAnimation.FRAMES];
        for (int i = 0; i < ClearAnimation.VARIANTS; i++) {
            clearingAnimations[i] = makeClearingAnimation();
        }
    }

    private BufferedImage[] makeClearingAnimation() {
        final BufferedImage[] result = new BufferedImage[ClearAnimation.FRAMES];

        float xvel = (float) random.nextGaussian() * (spriteSize / 2) / ClearAnimation.FRAMES;
        float yvel = (float) random.nextGaussian() * (spriteSize / 2) / ClearAnimation.FRAMES;
        float tvel = (float) random.nextGaussian() * (float) Math.PI / ClearAnimation.FRAMES;

        float width = (.2f + random.nextFloat() * .2f) * spriteSize;
        float x = .5f * spriteSize + (float)random.nextGaussian() * spriteSize/8;
        float y = .5f * spriteSize + (float)random.nextGaussian() * spriteSize/8;
        float t = random.nextFloat() * 2f * (float) Math.PI;

        final Rectangle2D.Float rect = new Rectangle2D.Float();

        for (int i = 0; i < ClearAnimation.FRAMES; i++) {
            result[i] = new BufferedImage(spriteSize, spriteSize, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2 = result[i].createGraphics();

            x += xvel;
            y += yvel;
            t += tvel;
            xvel *= .9;
            xvel *= .9;
            width *= .95;

            float alpha = .7f * (1f - (float) i / ClearAnimation.FRAMES);
            g2.setColor(new Color(.7f, .7f, .7f, alpha));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.rotate(t);

            rect.width = width;
            rect.height = width;
            rect.x = -width / 2;
            rect.y = -width / 2;

            g2.fill(rect);

            g2.dispose();
       }

        return result;
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

    public BufferedImage getClearingFrame(int variantIndex, int frame) {
        return clearingAnimations[variantIndex][frame];
    }
}
