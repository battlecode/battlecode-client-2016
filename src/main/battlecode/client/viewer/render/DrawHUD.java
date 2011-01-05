package battlecode.client.viewer.render;

import battlecode.common.*;
import battlecode.client.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ConcurrentModificationException;
import java.util.List;

class DrawHUD {

    private static final int numArchons = 6;
    private static final float slotSize = 0.8f / numArchons;
    private static final Font footerFont = new Font(null, Font.PLAIN, 1);
    private static final ImageFile bg = new ImageFile("art/hud_bg_new.jpg");
    private static final ImageFile unitUnder = new ImageFile("art/hud_unit_underlay.png");
    private static final ImageFile gameText = new ImageFile("art/game.png");
    private static final ImageFile barGradient = new ImageFile("art/BarGradient.png");
    private static ImageFile numberText;
    private static BufferedImage[] numbers;

    static {
        numberText = new ImageFile("art/numbers.png");
        numbers = new BufferedImage[10];
        for (int i = 0; i < 10; i++) {
            try {
                numbers[i] = numberText.image.getSubimage(48 * i, 0, 48, 64);
            } catch (NullPointerException e) {
                // e.printStackTrace();
            }
        }
    }
    private final DrawState ds;
    private final Team team;
    private final Rectangle2D.Float bgFill = new Rectangle2D.Float(0, 0, 1, 1);
    private float width;
    private float spriteScale;
    private String footerText = "";
    private int points = 0;
    private static final AffineTransform textScale =
            AffineTransform.getScaleInstance(1 / 64.0, 1 / 64.0);

    public DrawHUD(DrawState ds, Team team) {
        this.ds = ds;
        this.team = team;
        setRatioWidth(2.0f / 9.0f);
    }

    public float getRatioWidth() {
        return width;
    }

    public void setRatioWidth(float widthToHeight) {
        bgFill.width = width = widthToHeight;
        spriteScale = Math.min(slotSize / 2.5f, width / 2);
    }

    public void setPointsText(int value) {
        points = value;
    }

    public void setFooterText(String text) {
        footerText = text;
    }
    // stuff for win display
    int aWins = 0, bWins = 0;

    public void setWins(int a, int b) {
        aWins = a;
        bWins = b;
    }

    public void draw(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        //g2.setColor(Color.BLACK);
        //g2.fill(bgFill);
        AffineTransform trans = AffineTransform.getScaleInstance(bgFill.width, bgFill.height);
        BufferedImage bgImg = bg.image;
        trans.scale(1.0 / bgImg.getWidth(), 1.0 / bgImg.getHeight());
        g2.drawImage(bgImg, trans, null);


        AffineTransform pushed = g2.getTransform();
        {
            Color c = team == Team.A ? new Color(255, 0, 0, 100) : new Color(0, 0, 255, 20);
            g2.setColor(c);
            g2.scale(bgFill.width, bgFill.height);
            g2.fillRect(0, 0, 1, 1);
        }
        g2.setTransform(pushed);

        pushed = g2.getTransform();
        {
            g2.translate(width / 2, 0.9);
            g2.scale(width / 4.5, width / 4.5);
            //g2.setColor(Color.WHITE);
            //g2.setFont(footerFont);
            //g2.drawString(footerText, -footerText.length()/2, 0);
            battlecode.serial.RoundStats stats = ds.getRoundStats();

            AffineTransform pushed2 = g2.getTransform();
            {

                if (stats != null) {
                    points = (int) stats.getPoints(team);
                }
                g2.translate(-1.875, -1);

                g2.setColor(Color.BLACK);
                double x = .08;
                g2.scale(x, x);

                g2.drawString(points + "", 0, 12);
                g2.scale(1 / x, 1 / x);


//                for (int i = 10000; i > 0; i /= 10) {
//                    g2.drawImage(numbers[(points / i) % 10], textScale, null);
//                    g2.translate(0.75, 0);
//                }
            }
            g2.setTransform(pushed2);
            //Here we draw all of the domination-style bars.
            int barHeight = 1;

            //First, draw the bars that represent how much flux has been gathered that round
            g2.translate(0, -.1 * 4.5 / width);
            int gatheredPoints = 0;
            if (stats != null) {
                gatheredPoints = (int) stats.getGatheredPoints(team);
            }

            //Uhhhh. The width is arbitrary. We should take the max amount of 
            //Flux mineable in a round as the max bar length, but this works for now.
            //Why? I currently set it to 20 mines as max. I doubt any more than that can happen.
            //Well maybe if both teams turtle. Still ppl will be like OMG HE SO GOOD HE BLOWS THE ENGINE

            if (team == Team.A) {
                g2.scale(-1, 1);
            }
            g2.translate(-2.25, 0.0);

            AffineTransform pushed3 = g2.getTransform();
            {
                g2.setColor(Color.BLACK);
                g2.scale(1, barHeight / 2.0);
                g2.translate(0, -.2);
                g2.scale(1, .1);
                g2.fillRect(0, 0, 4, 1);
                g2.scale(.03, 10);
                for (int i = 0; i < 20; i++) {
                    g2.translate(0.1 * 100 / 50 / 0.03, 0);
                    //g2.fillRect(0, 0, 1, 1);
                    g2.drawLine(0, 0, 0, 1);
                }
            }
            g2.setTransform(pushed3);

            Color c = team == Team.A ? new Color(255, 0, 0, 100) : new Color(0, 0, 255, 130);
            g2.setColor(c);
            g2.scale(0.1 * gatheredPoints / 50, barHeight);
            g2.drawImage(barGradient.image, 0, 0, 1, 1, null);
            g2.fillRect(0, 0, 1, 1);



            g2.setTransform(pushed2);
            g2.translate(0, -.9 * 4.5 / width);

            g2.setColor(Color.BLACK);
            g2.translate(-2, 0);
            double x = .08;
            g2.scale(x, x);

            g2.drawString(footerText, 0, 12);
            g2.scale(1 / x, 1 / x);



            //TODO FIX THIS
            if (team == team.A) {
                if (aWins > 0) {
                    g2.translate(0.f, 1.25f);
                    g2.setColor(Color.RED);
                    g2.fillOval(0, 0, 1, 1);
                    g2.translate(0.f, -1.25f);
                }
            } else {
                // if team B won more than one round, give it a blue circle
                if (bWins > 0) {
                    // damn yangs magic offsets -_-
                    g2.translate(0.75f, 1.25f);
                    g2.setColor(Color.BLUE);
                    g2.fillOval(0, 0, 1, 1);
                    g2.translate(-0.75f, -1.25f);
                }
            }
        }
        g2.setTransform(pushed);
        g2.translate(0.5f * (width - spriteScale),
                0.5f * (slotSize - spriteScale));
        /* We don't draw the archon stuff anymore
        try {
        java.util.List<DrawObject> archons = ds.getArchons(team);
        for (int i = 0; i < numArchons; i++) {
        pushed = g2.getTransform();
        {
        g2.scale(spriteScale, spriteScale);
        AffineTransform pushed2 = g2.getTransform();
        {
        BufferedImage underImg = unitUnder.image;
        g2.translate(-0.5, -0.5);
        g2.scale(2.0 / underImg.getWidth(), 2.0 / underImg.getHeight());
        //g2.drawImage(underImg, null, null);
        }
        g2.setTransform(pushed2);
        if (i < archons.size()) {
        DrawObject archon = archons.get(i);
        archon.drawImmediateNoScale(g2, true, false);
        }
        }
        g2.setTransform(pushed);
        g2.translate(0, slotSize);
        }
        } catch (ConcurrentModificationException e) {
        }
         */
    }
}
