package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractDrawObject.RobotInfo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

import battlecode.client.util.ImageFile;
import battlecode.client.util.ImageResource;
import battlecode.client.viewer.AbstractAnimation;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.ActionType;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;
import java.util.ArrayList;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

class DrawObject extends AbstractDrawObject<Animation> {

    private static final double diagonalFactor = Math.sqrt(2);
    private static final Stroke thinStroke = new BasicStroke(0.05f);
    private static final Stroke mediumStroke = new BasicStroke(0.075f);
    private static final Stroke thickStroke = new BasicStroke(0.1f);
    private static final Stroke broadcastStroke = thinStroke;
    private static final Stroke attackStroke = mediumStroke;
    private static final Color tintTeamA = new Color(1, 0, 0, 0.125f);
    private static final Color tintTeamB = new Color(0, 0, 1, 0.125f);
//	private static final Color regenColor = new Color(0.f,.6f,0.f);
    private static final Stroke outlineStroke = new BasicStroke(0.10f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{0.5f, 0.5f}, 0.25f);
    private static final Shape outline = new Rectangle2D.Float(0, 0, 1, 1);
    private static final RescaleOp oneHalf = new RescaleOp(new float[]{1f, 1f, 1f, .5f}, new float[4], null);
    private static final ImageResource<RobotInfo> ir = new ImageResource<RobotInfo>();
    private static final ImageResource<String> cir = new ImageResource<String>();
    private static final ImageFile crosshair = new ImageFile("art/crosshair.png");
    private static final ImageFile crosshairBlue = new ImageFile("art/crosshair2.png");
    private static final ImageFile hatchSensor = new ImageFile("art/hatch_sensor.png");
    private static final ImageFile hatchAttack = new ImageFile("art/hatch_attack.png");
    private static final ImageFile workerRedBlock = new ImageFile("art/worker1_block.png");
    private static final ImageFile workerBlueBlock = new ImageFile("art/worker2_block.png");
    private static final ImageFile[] activeTeleporterRed = new ImageFile[]{new ImageFile("art/teleporter1_t1.png"), new ImageFile("art/teleporter1_t2.png"), new ImageFile("art/teleporter1_t3.png")};
    private static final ImageFile[] activeTeleporterBlue = new ImageFile[]{new ImageFile("art/teleporter2_t1.png"), new ImageFile("art/teleporter2_t2.png"), new ImageFile("art/teleporter2_t3.png")};
    private ImageFile img;
    private ImageFile preEvolve;
    private static final int maxTransfers = 10;
    public static final AbstractAnimation.AnimationType[] preDrawOrder = new AbstractAnimation.AnimationType[]{TELEPORT};
    public static final AbstractAnimation.AnimationType[] postDrawOrder = new AbstractAnimation.AnimationType[]{MORTAR_ATTACK, MORTAR_EXPLOSION, ENERGON_TRANSFER};
    private int teleportRounds;
    private MapLocation teleportLoc;
    private RobotType rtype = null;
    private static final double medbayRadius = Math.sqrt(RobotType.MEDBAY.attackRadiusMaxSquared);
    private static final double shieldsRadius = Math.sqrt(RobotType.SHIELDS.attackRadiusMaxSquared);
    private static final double soldierRadius = Math.sqrt(RobotType.SOLDIER.attackRadiusMaxSquared);
    private static final double artilleryRadius = Math.sqrt(GameConstants.ARTILLERY_SPLASH_RADIUS_SQUARED);
    private static final Color shieldColor = new Color(150,150,255,150);
    private static final Color regenColor = new Color(150,255,150,150);
    

    public DrawObject(RobotType type, Team team, int id) {
        super(type, team, id);
        img = preEvolve = ir.getResource(info, getAvatarPath(info));
        maxEnergon = type.maxEnergon;
        rtype = type;
    }


    public DrawObject(DrawObject copy) {
        super(copy);
        img = copy.img;
        preEvolve = copy.preEvolve;
        maxEnergon = copy.maxEnergon;
        preEvolve = copy.preEvolve;
        teleportRounds = copy.teleportRounds;
        teleportLoc = copy.teleportLoc;
        rtype = copy.rtype;
        if (animations.containsKey(ENERGON_TRANSFER)) {
            EnergonTransferAnim a = (EnergonTransferAnim) animations.get(ENERGON_TRANSFER);
            a.setSource(this);
        }
    }

    public static void loadAll() {
        for (RobotType type : RobotType.values()) {
            for (Team team : Team.values()) {
                /*if (team == Team.NEUTRAL) {
                continue;
                }*/
                RobotInfo robotInfo = new RobotInfo(type, team);
                ir.getResource(robotInfo, getAvatarPath(robotInfo));
            }
        }
    }

    private static String getAvatarPath(RobotInfo ri) {
        return getAvatarPath(ri.type.toString().toLowerCase(), ri.team);

    }

    private static String getAvatarPath(String type, Team team) {
        return "art/" + type + (team == Team.NEUTRAL ? "0" : (team == Team.A ? "1" : "2")) + ".png";
    }

    public double getRelativeSize() {
        return 1.;
        //return this.getType().relativeSize();
    }

	public void drawRangeHatch(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform();
		{
			g2.translate(loc.x, loc.y);
			try {
				BufferedImage sensorImg = hatchSensor.image;
				BufferedImage attackImg = hatchAttack.image;
				for (int i = -11; i <= 11; i++) for (int j = -11; j <= 11; j++) {
						int distSq = i * i + j * j;
						if (distSq <= info.type.sensorRadiusSquared) {
							if (inAngleRange(i, j, info.type.sensorCosHalfTheta)) {
								AffineTransform trans = AffineTransform.getTranslateInstance(i, j);
								trans.scale(1.0 / sensorImg.getWidth(), 1.0 / sensorImg.getHeight());
								g2.drawImage(sensorImg, trans, null);
							}
						}
						if ((info.type.canAttack)
								&& info.type.attackRadiusMinSquared <= distSq
								&& distSq <= info.type.attackRadiusMaxSquared
								&& inAngleRange(i, j, info.type.attackCosHalfTheta)) {
							AffineTransform trans = AffineTransform.getTranslateInstance(i, j);
							trans.scale(1.0 / attackImg.getWidth(), 1.0 / attackImg.getHeight());
							g2.drawImage(attackImg, trans, null);
						}
					}
			} catch (NullPointerException npe) {
			} // oh well
		}
		g2.setTransform(pushed);
	}

    private static MapLocation origin = new MapLocation(0, 0);

    private boolean inAngleRange(int dx, int dy, double cosHalfTheta) {
        MapLocation dirVec = origin.add(dir);
        int a = dirVec.x;
        int b = dirVec.y;
        int dotProd = a * dx + b * dy;
        if (dotProd < 0) {
            if (cosHalfTheta > 0) {
                return false;
            }
        } else if (cosHalfTheta < 0) {
            return true;
        }
        double rhs = cosHalfTheta * cosHalfTheta * (dx * dx + dy * dy) * (a * a + b * b);
        if (dotProd < 0) {
            return (dotProd * dotProd <= rhs + 0.00001d);
        } else {
            return (dotProd * dotProd >= rhs - 0.00001d);
        }
    }

    public void draw(Graphics2D g2, boolean focused) {

        if (RenderConfiguration.showRangeHatch() && focused) {
            drawRangeHatch(g2);
        }

        AffineTransform pushed = g2.getTransform();
        { // push
            g2.translate(getDrawX(), getDrawY());
            drawImmediate(g2, focused);

            /*
            if (auraAnimation != null && RenderConfiguration.showSpawnRadii()) {
            auraAnimation.draw(g2);
            }
             */

            if (broadcast != 0x00 && RenderConfiguration.showBroadcast()) {
                g2.setStroke(broadcastStroke);
                double drdR = visualBroadcastRadius * 0.05; // dradius/dRound
                for (int i = 0; i < 20; i++) {
                    if ((broadcast & (1 << i)) != 0x00) {
                        double r = i * drdR;
                        g2.setColor(new Color(1, 0, 1, 0.05f * (20 - i)));
                        g2.draw(new Ellipse2D.Double(0.5 - r, 0.5 - r, 2 * r, 2 * r));
                    }
                }
            }

//			if(regen > 0 && RenderConfiguration.showSpawnRadii()) {
//				g2.setStroke(broadcastStroke);
//				g2.setColor(regenColor);
//				g2.draw(new Ellipse2D.Double(.5-regenRadius,.5-regenRadius,2*regenRadius,2*regenRadius));
//			}
        }
        g2.setTransform(pushed); // pop
        // these animations shouldn't be drawn in the HUD, and they expect
        // the origin of the Graphics2D to be the MapLocation (0,0)
        for (AbstractAnimation.AnimationType type : postDrawOrder) {
            if (type.shown() && animations.containsKey(type)) {
                animations.get(type).draw(g2);
            }
        }
        drawAction(g2);
    }

    public void drawImmediate(Graphics2D g2, boolean drawOutline, boolean isHUD) {

        // these animations should be drawn in the HUD, and they expect
        // the origin of the Grpahics2D to be this robot's position
        for (AbstractAnimation.AnimationType type : preDrawOrder) {
            if (type.shown() && animations.containsKey(type)) {

                animations.get(type).draw(g2);
            }
        }

        if (animations.containsKey(DEATH_EXPLOSION)) {
            if (DEATH_EXPLOSION.shown() || isHUD) {
                Animation deathExplosion = animations.get(DEATH_EXPLOSION);
                if (deathExplosion.isAlive()) {
                    deathExplosion.draw(g2);
                }
            }
        } else {

			boolean showEnergon = RenderConfiguration.showEnergon() || drawOutline;
			boolean showFlux = (RenderConfiguration.showFlux() || drawOutline);

            if (showEnergon) {
                Rectangle2D.Float rect = new Rectangle2D.Float(0, 1, 1, 0.15f);
                g2.setColor(Color.BLACK);
                g2.fill(rect);
                float frac = Math.min((float) (energon / maxEnergon), 1);
                rect.width = frac;
                if (frac < 0)
                    frac = 0;
                if (turnedOn) {
                    g2.setColor(new Color(Math.min(1 - 0.5f * frac, 1.5f - 1.5f * frac),
                            Math.min(1.5f * frac, 0.5f + 0.5f * frac), 0));
                } else {
                    g2.setColor(new Color(.5f - .5f * frac, .5f - .5f * frac, .5f + .5f * frac));
                }
                g2.fill(rect);
                
                // drawing shields
                {
                	frac = Math.min((float)(shields/maxEnergon), 1);
                	rect = new Rectangle2D.Float(0, 1, 1.0f, 0.075f);
                	rect.width = frac;
                	
                	if (frac < 0)
                        frac = 0;
                    g2.setColor(new Color(.5f * frac, .5f * frac, .5f + .5f * frac));
                    g2.fill(rect);
                }
            }

//			if(showFlux) {
//			    Rectangle2D.Float rect;
//				if(showEnergon)
//					rect = new Rectangle2D.Float(0, 1.15f, 1, 0.15f);
//				else
//					rect = new Rectangle2D.Float(0, 1, 1, 0.15f);
//                g2.setColor(Color.BLACK);
//                g2.fill(rect);
//                float frac = Math.min((float) (flux / info.type.maxFlux), 1);frac = Math.min((float) (energon / maxEnergon), 1);
//                rect.width = frac;
//                if (frac < 0)
//                    frac = 0;
//                g2.setColor(new Color(frac,0,.5f+.5f*frac));
//                g2.fill(rect);
//			}
			
            // actions
			if (actionAction != null && actionAction != ActionType.IDLE) {
			    Rectangle2D.Float rect;
				if(showEnergon)
					rect = new Rectangle2D.Float(0, 1.15f, 1, 0.15f);
				else
					rect = new Rectangle2D.Float(0, 1, 1, 0.15f);
                g2.setColor(Color.BLACK);
                g2.fill(rect);
                float frac = Math.min(1-((float)roundsUntilActionIdle / Math.max(totalActionRounds,1)), 1);
                if (totalActionRounds == 0)
                	frac = 1;
                rect.width = frac;
                if (frac < 0)
                    frac = 0;
                switch (actionAction)
                {
                case MINING: g2.setColor(new Color(1.0f, 0, 0.8f)); break;
                case DEFUSING: g2.setColor(Color.cyan); break;
                case CAPTURING: g2.setColor(new Color(0.3f, 0.3f, 1.0f)); break;
                default:;
                }
//                g2.setColor(new Color(frac,0,.5f+.5f*frac));
                g2.fill(rect);
			}

	      		AffineTransform trans = AffineTransform.getRotateInstance(0, 0.5, 0.5);//(dir.ordinal() - 2) * Math.PI / 4, 0.5, 0.5);

            assert preEvolve != null;
            BufferedImage image = getTypeSprite();
            if (image != null) {
                if (isHUD) {
                    trans.scale(1.0 / image.getWidth(), 1.0 / image.getHeight());
                } else {
                    trans.scale((1.0 / image.getWidth()) * this.getRelativeSize(), (1.0 / image.getHeight()) * this.getRelativeSize());
                }

                // draw if worker is carrying block
                if (teleportRounds > 0 && RenderConfiguration.showTeleport()) {
                    if (teleportLoc == null) {
                        if (getTeam() == Team.A) {
                            g2.drawImage(activeTeleporterRed[teleportRounds % 3].image, trans, null);
                        } else {
                            g2.drawImage(activeTeleporterBlue[teleportRounds % 3].image, trans, null);
                        }
                    } else {
                        if (teleportRounds % 8 < 4) {
                            BufferedImage im2 = oneHalf.filter(image, null);
                            g2.drawImage(im2, trans, null);
                            if (RenderConfiguration.showTeleportGhosts() && !isHUD) {
                                g2.translate(teleportLoc.x - loc.x, teleportLoc.y - loc.y);
                                g2.drawImage(im2, trans, null);
                                g2.translate(loc.x - teleportLoc.x, loc.y - teleportLoc.y);
                            }
                        } else {
                            g2.drawImage(image, trans, null);
                        }
                    }
                } else {
                    g2.drawImage(image, trans, null);
                }
            } else {
                //System.out.println("null image in DrawObject.drawImmediate");
            }
        }

        if (drawOutline) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(outlineStroke);
            g2.draw(outline);
        }

    }

    public void drawImmediate(Graphics2D g2, boolean drawOutline) {
        drawImmediate(g2, drawOutline, false);
    }

    // used by the HUD
    public void drawImmediateNoScale(Graphics2D g2, boolean drawOutline) {
        drawImmediate(g2, drawOutline, true);
    }

    private void drawAction(Graphics2D g2) {
        if (roundsUntilAttackIdle>0 
        		|| attackAction == ActionType.ATTACKING 
//        		|| rtype == RobotType.SHIELDS 
//        		|| rtype == RobotType.MEDBAY
        		) {
			g2.setColor(getTeam() == Team.A ? Color.RED : Color.BLUE);
            g2.setStroke(mediumStroke);
            
            if (rtype == null)
            {
            	if(targetLoc==null) {
    				// scorcher
//    				g2.draw(new Arc2D.Double(getDrawX()-scorcherRadius+.5,getDrawY()-scorcherRadius+.5,2*scorcherRadius,2*scorcherRadius,90-RobotType.SCORCHER.attackAngle/2.+attackDir.ordinal()*(-45),RobotType.SCORCHER.attackAngle,Arc2D.PIE));
    			}
    			else {
                    BufferedImage target;
                    if (getTeam() == Team.A) {
                        target = crosshair.image;
                    } else {
                        target = crosshairBlue.image;
                    }
                    if (target != null) {
                        AffineTransform trans = AffineTransform.getTranslateInstance(targetLoc.x, targetLoc.y);
                        trans.scale(1.0 / target.getWidth(), 1.0 / target.getHeight());
                        g2.drawImage(target, trans, null);
                    }

                    g2.draw(new Line2D.Double(getDrawX() + 0.5, getDrawY() + 0.5,
                            targetLoc.x + 0.5, targetLoc.y + 0.5));
                }
            } else
            {
            	switch (rtype) {
            	case SOLDIER:
            		g2.draw(new Ellipse2D.Double(getDrawX()+.5-soldierRadius,getDrawY()+.5-soldierRadius,2*soldierRadius,2*soldierRadius));
            		break;
            	case ARTILLERY:
            		if (roundsUntilAttackIdle == RobotType.ARTILLERY.attackDelay-1)
            		{
            			BufferedImage target;
                        if (getTeam() == Team.A) {
                            target = crosshair.image;
                        } else {
                            target = crosshairBlue.image;
                        }
                        if (target != null) {
                            AffineTransform trans = AffineTransform.getTranslateInstance(targetLoc.x, targetLoc.y);
                            trans.scale(1.0 / target.getWidth(), 1.0 / target.getHeight());
                            g2.drawImage(target, trans, null);
                        }

                        g2.draw(new Line2D.Double(getDrawX() + 0.5, getDrawY() + 0.5,
                                targetLoc.x + 0.5, targetLoc.y + 0.5));
                		g2.draw(new Ellipse2D.Double(targetLoc.x+.5-artilleryRadius,targetLoc.y+.5-artilleryRadius,2*artilleryRadius,2*artilleryRadius));
            		}
            		
            		break;
            	case MEDBAY:
            		g2.setColor(regenColor);
            		g2.fill(new Ellipse2D.Double(getDrawX()+.5-medbayRadius,getDrawY()+.5-medbayRadius,2*medbayRadius,2*medbayRadius));
            		break;
            	case SHIELDS:
            		g2.setColor(shieldColor);
            		g2.fill(new Ellipse2D.Double(getDrawX()+.5-shieldsRadius,getDrawY()+.5-shieldsRadius,2*shieldsRadius,2*shieldsRadius));
            		break;
            	}
            }
            
			
        }

    }

    private BufferedImage getTypeSprite() {
        /*
        if (action == ActionType.TRANSFORMING) {

        return (roundsUntilIdle/4 % 2 == 0 ? preEvolve.image : img.image);
        }
         */
        return img.image;
    }

    public void evolve(RobotType type) {
        super.evolve(type);
        img = ir.getResource(info, getAvatarPath(info));
    }

    public void setTeam(Team team) {
        super.setTeam(team);
        img = ir.getResource(info, getAvatarPath(info));
    }

    public void setMaxEnergon(double maxEnergon) {
        this.maxEnergon = maxEnergon;
    }

    public TeleportAnim createTeleportAnim(MapLocation src, MapLocation loc) {
        return new TeleportAnim(src, loc);
    }

    public ExplosionAnim createDeathExplosionAnim(boolean isArchon) {
        return new ExplosionAnim();
    }

    public MortarAttackAnim createMortarAttackAnim(MapLocation target) {
        return new MortarAttackAnim(loc, target);
    }

    public EnergonTransferAnim createEnergonTransferAnim(MapLocation loc, RobotLevel height, float amt, boolean isFlux) {
        return new EnergonTransferAnim(this, loc, amt, isFlux);
    }

    public ExplosionAnim createMortarExplosionAnim(Animation mortarAttackAnim) {
        ExplosionAnim anim = new ExplosionAnim(((MortarAttackAnim) mortarAttackAnim).getTargetLoc(), 1.8);
        anim.setExplosionToggle(ExplosionAnim.ExplosionToggle.DETONATES);
        return anim;
    }

    public void updateRound() {
        super.updateRound();

        if (teleportRounds > 0) {
            teleportRounds--;
            if (teleportLoc == null && teleportRounds % 3 == 2) {
                dir = dir.rotateRight().rotateRight();
            }
        }
    }

    public void activateTeleporter() {
        teleportRounds = 1;
    }

    public void activateTeleport(MapLocation teleportLoc) {
        teleportRounds = 1;
        this.teleportLoc = teleportLoc;
    }
}
