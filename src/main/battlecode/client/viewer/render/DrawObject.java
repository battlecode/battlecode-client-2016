package battlecode.client.viewer.render;

import battlecode.client.util.ImageFile;
import battlecode.client.util.ImageResource;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.Action;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class DrawObject extends AbstractDrawObject {
    public static final int LAYER_COUNT = 3;
    private static final Stroke thinStroke = new BasicStroke(0.05f);
    private static final Stroke mediumStroke = new BasicStroke(0.075f);
    private static final Stroke thickStroke = new BasicStroke(0.1f);
    private static final Stroke broadcastStroke = thinStroke;
    //	private static final Color regenColor = new Color(0.f,.6f,0.f);
    private static final Stroke outlineStroke = new BasicStroke(0.10f,
            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new
            float[]{0.5f, 0.5f}, 0.25f);
    private static final Shape outline = new Rectangle2D.Float(0, 0, 1, 1);
    private static final ImageResource<RobotInfo> ir = new
            ImageResource<>();
    private static final ImageFile hatchSensor = new ImageFile
            ("art/hatch_sensor.png");
    private static final ImageFile hatchAttack = new ImageFile
            ("art/hatch_attack.png");
    private static final ImageFile creep = new ImageFile("art/creep.png");

    private ImageFile img;

    private final DrawState overallstate;

    public static final ImageFile[] hatImages;

    static {
        final String[] hatNames = {
                "art/hats/batman.png",
                "art/hats/bird.png",
                "art/hats/bunny.png",
                "art/hats/christmas.png",
                "art/hats/duck.png",
                "art/hats/fedora.png",
                "art/hats/kipmud.png",
                "art/hats/smiley.png",
        };

        hatImages = new ImageFile[hatNames.length];
        for (int x = 0; x < hatNames.length; x++) {
            hatImages[x] = new ImageFile(hatNames[x]);
        }
    }


    public DrawObject(int currentRound, RobotType type, Team team, int id,
                      DrawState state) {
        super(currentRound, type, team, id);
        maxHealth = type.maxHealth(currentRound);
        overallstate = state;
        loadImage(true);
    }


    public DrawObject(int currentRound, DrawObject copy) {
        super(currentRound, copy);
        img = copy.img;
        maxHealth = copy.maxHealth;
        overallstate = copy.overallstate;
    }

    public static void loadAll() {
        int spriteSize = Math.round(RenderConfiguration.getInstance().getSpriteSize());
        for (RobotType type : RobotType.values()) {
            for (Team team : Team.values()) {
                RobotInfo robotInfo = new RobotInfo(type, team);
                ir.getResource(robotInfo, getAvatarPath(robotInfo),
                        spriteSize, spriteSize);
            }
        }
    }

    private int prevSpriteSize = -1;
    /**
     * Reloads the sprite image. If lazy is true, then the sprite will only
     * be reloaded if the sprite size has changed from before. If lazy is
     * false, then the sprite will always be reloaded (used when the
     * RobotType changes).
     *
     * @param lazy whether to perform lazy loading.
     */
    private void loadImage(boolean lazy) {
        // Reloads "img", the ImageFile for the sprite, if the target spriteSize
        // changes.
        int spriteSize = (int) Math.round(RenderConfiguration.getInstance()
                .getSpriteSize() * drawScale());
        if (spriteSize != prevSpriteSize || !lazy) {
            img = ir.getResource(info, getAvatarPath(info), spriteSize,
                    spriteSize);
            prevSpriteSize = spriteSize;
        }
    }

    private static String getAvatarPath(RobotInfo ri) {
        return getAvatarPath(ri.type.toString().toLowerCase(), ri.team);

    }

    private static String getAvatarPath(String type, Team team) {
        return "art/" + type + team.ordinal() + ".png";
    }

    @Override
    public void setType(RobotType type) {
        super.setType(type);
        loadImage(false);
    }

    private int getViewRange() {
        return info.type.sensorRadiusSquared;
    }

    public void drawRangeHatch(Graphics2D g2) {
        AffineTransform pushed = g2.getTransform();
        final int viewrange = getViewRange();
        {
            g2.translate(loc.x, loc.y);
            try {
                BufferedImage sensorImg = hatchSensor.image;
                BufferedImage attackImg = hatchAttack.image;
                for (int i = -21; i <= 21; i++)
                    for (int j = -21; j <= 21; j++) {
                        int distSq = i * i + j * j;
                        if (distSq <= viewrange) {
                            AffineTransform trans = AffineTransform
                                    .getTranslateInstance(i, j);
                            trans.scale(1.0 / sensorImg.getWidth(), 1.0 /
                                    sensorImg.getHeight());
                            g2.drawImage(sensorImg, trans, null);
                        }
                        if ((info.type.attackPower > 0)
                                && distSq <= info.type.attackRadiusSquared) {
                            AffineTransform trans = AffineTransform
                                    .getTranslateInstance(i, j);
                            trans.scale(1.0 / attackImg.getWidth(), 1.0 /
                                    attackImg.getHeight());
                            g2.drawImage(attackImg, trans, null);
                        }
                    }
            } catch (NullPointerException npe) {
            } // oh well
        }
        g2.setTransform(pushed);
    }

    public void draw(Graphics2D g2, boolean focused, boolean lastRow, int
            layer) {
        // Reload the image, in case the screen was resized.
        loadImage(true);
        if (layer == 0) {
            if (RenderConfiguration.showRangeHatch() && focused) {
                drawRangeHatch(g2);
            }
            if (info.type.isBuilding && RenderConfiguration.showDetails()) {
                AffineTransform pushed0 = g2.getTransform();
                g2.translate(getDrawX(), getDrawY());
                drawImageTransformed(g2, new AffineTransform(), creep.image, 2);
                g2.setTransform(pushed0); // pop
            }
        }
        if (layer == 1 || (layer == 2 && info.type == RobotType.ARCHON)) {
            AffineTransform pushed1 = g2.getTransform();
            g2.translate(getDrawX(), getDrawY());
            drawImmediate(g2, focused, lastRow);
            g2.setTransform(pushed1); // pop
        }
    }

    public void drawAction(Graphics2D g2, Action a,
                           boolean focused, boolean isHUD) {
        switch (a.type) {
            case MOVING:
                if (!isHUD) {
                    drawMoving(g2, a, focused);
                }
                break;
            case ATTACKING:
                if (!isHUD) {
                    drawAttacking(g2, a);
                }
                break;
            default:
                break;
        }
    }

    protected void setTeamColor(Graphics2D g2) {
    	Team team = getTeam();
    	if(team == Team.A) {
    		g2.setColor(Color.RED);
    	} else if (team == Team.B) {
    		g2.setColor(Color.BLUE);
    	} else if (team == Team.ZOMBIE) {
    		g2.setColor(Color.GREEN);
    	} else {
    		g2.setColor(Color.GRAY);
    	}
    }


    public void drawImmediate(Graphics2D g2, boolean focused, boolean isHUD,
                              boolean lastRow, boolean drawXP) {

        setTeamColor(g2);
        g2.setStroke(mediumStroke);

        for (UnitAnimation animation : unitAnimations) {
            if (animation.shown()) {
                animation.draw(g2, isHUD);
            }
        }

        if (deathAnimation != null) {
            if (deathAnimation.shown() || isHUD) {
                if (deathAnimation.isAlive()) {
                    deathAnimation.draw(g2, isHUD);
                }
            }
        } else {
            drawRobotImage(g2);
            drawStatusBars(g2, focused, lastRow, drawXP);

            for (Action a : actions) {
                drawAction(g2, a, focused, isHUD);
            }

            if (focused) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(outlineStroke);
                g2.draw(outline);
            }
            if (!isHUD) {
                drawBroadcast(g2);
            }
        }
    }

    public void drawMoving(Graphics2D g2, Action a, boolean focused) {
        if ((RenderConfiguration.showActionLines() || focused)) {
            setTeamColor(g2);
            g2.setStroke(thickStroke);
            g2.draw(new Line2D.Double(0.5, 0.5,
                    0.5 - dir.dx, 0.5 - dir.dy));
        }
    }

    public void drawAttacking(Graphics2D g2, Action a) {
        if (RenderConfiguration.showAttack()) {
            setTeamColor(g2);
            g2.setStroke(mediumStroke);
            MapLocation target = new MapLocation(a.target.x - loc.x,
                    a.target.y - loc.y);
            g2.draw(new Line2D.Double(getDrawDX() + 0.5, getDrawDY() + 0.5,
                    target.x + 0.5, target.y + 0.5));
        }

    }

    public void drawStatusBars(Graphics2D g2, boolean focused, boolean
            lastRow, boolean drawXP) {
        boolean showHealth = RenderConfiguration.showHealth() || focused;
        if (showHealth) {
            Rectangle2D.Float rect = new Rectangle2D.Float(0, lastRow ? 0.85f
                    : 1, 1, 0.15f);
            g2.setColor(Color.BLACK);
            g2.fill(rect);
            float frac = Math.min((float) (health / maxHealth), 1);
            rect.width = frac;
            if (frac < 0)
                frac = 0;
            g2.setColor(new Color(Math.min(1 - 0.5f * frac, 1.5f - 1.5f * frac),
                    Math.min(1.5f * frac, 0.5f + 0.5f * frac), 0));
            g2.fill(rect);

        }

        //building progress bar
        if (aliveRounds < buildDelay) {
            Rectangle2D.Float rect;
            rect = new Rectangle2D.Float(0, lastRow ? 0.7f : 1.15f, 1, 0.15f);
            g2.setColor(Color.BLACK);
            g2.fill(rect);
            rect.width = ((float) aliveRounds) / buildDelay;
            g2.setColor(new Color(1f, 0f, 0f));
            g2.fill(rect);
        }

        if (RenderConfiguration.showInfectionIndicators()) {
            if (getViperInfectedTurns() > 0 || getZombieInfectedTurns() > 0) {
                BufferedImage infection = GameRenderer.pg
                        .getZombieInfectionImage();
                if (getViperInfectedTurns() > 0) {
                    infection = GameRenderer.pg.getViperInfectionImage();
                }
                AffineTransform trans = new AffineTransform();
                trans.scale(1.0 / infection.getWidth(), 1.0 /
                        infection.getHeight());
                g2.drawImage(infection, trans, null);
            }
        }
    }

    public double drawScale() {
        if (info.type.isBuilding || info.type == RobotType.ARCHON || info.type
                == RobotType.BIGZOMBIE) {
            return 1.3;
        }
        return 1;
    }

    public static void drawImageTransformed(Graphics2D g2, AffineTransform
            trans,
                                            BufferedImage im, double size) {
        double recentering = -1 * (size - 1.0) / 2;
        trans.translate(recentering, recentering);
        trans.scale(size / im.getWidth(), size / im.getHeight());

        g2.drawImage(im, trans, null);
    }

    // draw translated to robot location
    public void drawRobotImage(Graphics2D g2) {
        // could be used for rotations or such, remember origin for rotation
        AffineTransform trans = new AffineTransform();
        BufferedImage image = getTypeSprite();
        if (image == null) {
            System.out.println("missing image for type: " + info.type
                    .toString());
        }
        double size = drawScale();
        drawImageTransformed(g2, trans, image, size);

        // hats
        if (RenderConfiguration.showHats()) {
            double hatscale = 1.5;
            AffineTransform pushed2 = g2.getTransform();
            g2.translate((2.0 - hatscale) / 4.0, 0.2);
            double width = image.getWidth();
            trans = AffineTransform.getScaleInstance(hatscale / image
                    .getWidth(), hatscale / image.getWidth());
            for (int x = 0; x < hats.length(); x++) {

                try {
                    image = hatImages[(int) hats.charAt(x)].image;
                    g2.translate(0, -hatscale / width * (image.getHeight() -
                            2));
                    g2.drawImage(image, trans, null);
                } catch (Exception e) {
                    System.out.println("x = " + x);
                    System.out.println("value at x = " + (int) hats.charAt(x));
                    System.out.println("hats string length: " + hats.length());
                    e.printStackTrace();
                }

            }
            g2.setTransform(pushed2);
        }
    }

    // draw translated to robot location
    public void drawBroadcast(Graphics2D g2) {
        if (broadcast != 0x00 && RenderConfiguration.showBroadcast()) {
            g2.setStroke(broadcastStroke);
            double drdR = visualBroadcastRadius * 0.05; // dradius/dRound
            for (int i = 0; i < 15; i++) {
                if ((broadcast & (1 << i)) != 0x00) {
                    double r = i * drdR;
                    g2.setColor(new Color(1, 0, 1, 0.05f * (15 - i)));
                    g2.draw(new Ellipse2D.Double(0.5 - r, 0.5 - r, 2 * r, 2 * r));
                }
            }
        }
    }

    public void drawImmediate(Graphics2D g2, boolean focused, boolean lastRow) {
        drawImmediate(g2, focused, false, lastRow, true);
    }

    private BufferedImage getTypeSprite() {
        return img.image;
    }

    public ExplosionAnim createDeathExplosionAnim(boolean unused) {
        if (getType() == RobotType.ARCHON || getTeam() == Team.NEUTRAL ||
            getType() == RobotType.ZOMBIEDEN) {
            return new LargeExplosionAnim(getTeam()); // a subclass of explosion
        } else {
            return new ExplosionAnim();
        }
    }
}
