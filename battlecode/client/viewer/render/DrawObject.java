package battlecode.client.viewer.render;

import battlecode.common.AuraType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Map;

import battlecode.client.util.ImageFile;
import battlecode.client.util.ImageResource;
import battlecode.client.viewer.AbstractAnimation;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.common.ActionType;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.world.GameMap;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

class DrawObject extends AbstractDrawObject<Animation> {

	private static final double diagonalFactor = Math.sqrt(2);
	private static final Stroke thinStroke = new BasicStroke(0.05f);
	private static final Stroke mediumStroke = new BasicStroke(0.075f);
	private static final Stroke broadcastStroke = thinStroke;
	private static final Stroke attackStroke = mediumStroke;
	private static final Color tintTeamA = new Color(1, 0, 0, 0.125f);
	private static final Color tintTeamB = new Color(0, 0, 1, 0.125f);
	private static final Stroke outlineStroke = new BasicStroke(0.05f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{0.5f, 0.5f}, 0.25f);
	private static final Shape outline = new Rectangle2D.Float(0, 0, 1, 1);
	private static final RescaleOp oneHalf = new RescaleOp(new float [] { 1f,1f,1f,.5f }, new float[4],null);

	private static final ImageResource<RobotInfo> ir = new ImageResource<RobotInfo>();
	private static final ImageFile crosshair = new ImageFile("art/crosshair.png");
	private static final ImageFile crosshairBlue = new ImageFile("art/crosshair2.png");
	private static final ImageFile hatchSensor = new ImageFile("art/hatch_sensor.png");
	private static final ImageFile hatchAttack = new ImageFile("art/hatch_attack.png");
	private static final ImageFile workerRedBlock = new ImageFile("art/worker1_block.png");
	private static final ImageFile workerBlueBlock = new ImageFile("art/worker2_block.png");
	private static final ImageFile [] activeTeleporterRed = new ImageFile [] { new ImageFile("art/teleporter1_t1.png"), new ImageFile("art/teleporter1_t2.png"), new ImageFile("art/teleporter1_t3.png") };
	private static final ImageFile [] activeTeleporterBlue = new ImageFile [] { new ImageFile("art/teleporter2_t1.png"), new ImageFile("art/teleporter2_t2.png"), new ImageFile("art/teleporter2_t3.png") };
	private ImageFile img;
	private ImageFile preEvolve;
	//private EnergonTransferAnim energonTransferAnim = null;
	
	//private boolean shouldAura = false;
	//private AuraAnimation auraAnimation = null;
	//private ExplosionAnim deathExplosionAnim = null;
	//private MortarAttackAnim mortarAttackAnim = null;
	//private ExplosionAnim mortarExplosionAnim = null;
	//private TeleportAnim teleportAnim = null;

	private static final int maxTransfers = 10;

	public static final AbstractAnimation.AnimationType [] preDrawOrder = new AbstractAnimation.AnimationType [] { TELEPORT };
	public static final AbstractAnimation.AnimationType [] postDrawOrder = new AbstractAnimation.AnimationType [] { MORTAR_ATTACK, MORTAR_EXPLOSION, ENERGON_TRANSFER };
	private int teleportRounds;
	private MapLocation teleportLoc;

	public DrawObject(RobotType type, Team team) {
		super(type, team);
		img = preEvolve = ir.getResource(info, getAvatarPath(info));
		maxEnergon = type.maxEnergon();
	}

	public DrawObject(DrawObject copy) {
		super(copy);
		img = copy.img;
		preEvolve = copy.preEvolve;
		maxEnergon = copy.maxEnergon;
		preEvolve = copy.preEvolve;
		teleportRounds = copy.teleportRounds;
		teleportLoc = copy.teleportLoc;
		if(animations.containsKey(ENERGON_TRANSFER)) {
			EnergonTransferAnim a = (EnergonTransferAnim)animations.get(ENERGON_TRANSFER);
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
		String type = ri.type.toString().toLowerCase();
		return "art/" + type + (ri.team == Team.NEUTRAL ? "0" : (ri.team == Team.A ? "1" : "2")) + ".png";
	}

	public double getRelativeSize() {
		return 1.;
		//return this.getType().relativeSize();
	}

	public void drawRangeHatch(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform();
		{
			g2.translate(loc.getX(), loc.getY());
			try {
				BufferedImage sensorImg = hatchSensor.image;
				BufferedImage attackImg = hatchAttack.image;
				for (int i = -11; i <= 11; i++) for (int j = -11; j <= 11; j++) {
						int distSq = i * i + j * j;
						if (distSq <= info.type.sensorRadiusSquared()) {
							if (inAngleRange(i, j, info.type.sensorCosHalfTheta())) {
								AffineTransform trans = AffineTransform.getTranslateInstance(i, j);
								trans.scale(1.0 / sensorImg.getWidth(), 1.0 / sensorImg.getHeight());
								g2.drawImage(sensorImg, trans, null);
							}
						}
						if ((info.type.canAttackGround() || info.type.canAttackAir())
								&& info.type.attackRadiusMinSquared() <= distSq
								&& distSq <= info.type.attackRadiusMaxSquared()
								&& inAngleRange(i, j, info.type.attackCosHalfTheta())) {
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

	public void drawChannelerDrain(Graphics2D g2) {
		AffineTransform pushed = g2.getTransform();
		{
			g2.translate(loc.getX(), loc.getY());
			if (getTeam() == Team.A)
				g2.setColor(new Color(1.f, .7f, .7f, 0.2f));
			else
				g2.setColor(new Color(.7f, .7f, 1.f, 0.2f));
			try {
				BufferedImage attackImg = hatchAttack.image;
				for (int i = -6; i <= 6; i++) for (int j = -6; j <= 6; j++) {
						int distSq = i * i + j * j;
						if (distSq <= info.type.attackRadiusMaxSquared()) {
							AffineTransform trans = AffineTransform.getTranslateInstance(i, j);
							trans.scale(1.0 / attackImg.getWidth(), 1.0 / attackImg.getHeight());
							g2.drawImage(attackImg, trans, null);
							g2.fillRect(i, j, 1, 1);
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
		int a = dirVec.getX();
		int b = dirVec.getY();
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
			drawImmediate(g2, focused || RenderConfiguration.showEnergon(), focused);
			
			/*
			if (auraAnimation != null && RenderConfiguration.showSpawnRadii()) {
				auraAnimation.draw(g2);
			}
			*/

			if (broadcast != 0x00 && RenderConfiguration.showBroadcast()) {
				g2.setStroke(broadcastStroke);
				double drdR = info.type.broadcastRadius() * 0.05; // dradius/dRound
				for (int i = 0; i < 20; i++) {
					if ((broadcast & (1 << i)) != 0x00) {
						double r = i * drdR;
						g2.setColor(new Color(1, 0, 1, 0.05f * (20 - i)));
						g2.draw(new Ellipse2D.Double(0.5 - r, 0.5 - r, 2 * r, 2 * r));
					}
				}
			}
		}
		g2.setTransform(pushed); // pop
		// these animations shouldn't be drawn in the HUD, and they expect
		// the origin of the Graphics2D to be the MapLocation (0,0)
		for(AbstractAnimation.AnimationType type : postDrawOrder) {
			if(type.shown()&&animations.containsKey(type)) {
				animations.get(type).draw(g2);
			}
		}
		drawAction(g2);
	 }

	public void drawImmediate(Graphics2D g2, boolean drawEnergon, boolean drawOutline, boolean isHUD) {

		// these animations should be drawn in the HUD, and they expect
		// the origin of the Grpahics2D to be this robot's position
		for(AbstractAnimation.AnimationType type : preDrawOrder) {
			if(type.shown()&&animations.containsKey(type)) {
				animations.get(type).draw(g2);
			}
		}

		if(animations.containsKey(DEATH_EXPLOSION)) {
			if(DEATH_EXPLOSION.shown()||isHUD) {
				Animation deathExplosion = animations.get(DEATH_EXPLOSION);
				if(deathExplosion.isAlive())
					deathExplosion.draw(g2);
			}
		}
		else {

			if (drawEnergon) {
				Rectangle2D.Float rect = new Rectangle2D.Float(0, 1, 1, 0.2f);
				g2.setColor(Color.BLACK);
				g2.fill(rect);
				float frac = Math.min((float)(energon / maxEnergon), 1);
				rect.width = frac;
				g2.setColor(new Color(Math.min(1 - 0.5f * frac, 1.5f - 1.5f * frac),
									  Math.min(1.5f * frac, 0.5f + 0.5f * frac), 0));
				g2.fill(rect);
			}
			
			AffineTransform trans = AffineTransform.getRotateInstance((dir.ordinal() - 2) * Math.PI / 4, 0.5, 0.5);
			assert preEvolve != null;
			BufferedImage image = getTypeSprite();
			if (image != null) {
				if(isHUD)
					trans.scale(1.0 / image.getWidth(), 1.0 / image.getHeight());
				else
					trans.scale((1.0 / image.getWidth()) * this.getRelativeSize(), (1.0 / image.getHeight()) * this.getRelativeSize());
				
				// draw if worker is carrying block
				if(teleportRounds>0&&RenderConfiguration.showTeleport()) {
					if(teleportLoc==null) {
						if (getTeam() == Team.A)
							g2.drawImage(activeTeleporterRed[teleportRounds%3].image,trans,null);
						else
							g2.drawImage(activeTeleporterBlue[teleportRounds%3].image,trans,null);
					}
					else {
						if(teleportRounds%8<4) {
							BufferedImage im2 = oneHalf.filter(image,null);
							g2.drawImage(im2,trans,null);
							if(RenderConfiguration.showTeleportGhosts()&&!isHUD) {
								g2.translate(teleportLoc.getX()-loc.getX(),teleportLoc.getY()-loc.getY());
								g2.drawImage(im2,trans,null);
								g2.translate(loc.getX()-teleportLoc.getX(),loc.getY()-teleportLoc.getY());
							}
						}
						else
							g2.drawImage(image,trans,null);
					}
				} else
					g2.drawImage(image, trans, null);
				
				
			} else {
				//System.out.println("null image in DrawObject.drawImmediate");
			}
		}

		if (drawOutline) {
			g2.setColor(Color.WHITE);
			g2.setStroke(outlineStroke);
			g2.draw(outline);
		}

	}

	public void drawImmediate(Graphics2D g2, boolean drawEnergon, boolean drawOutline) {
		drawImmediate(g2,drawEnergon, drawOutline, false);
	}

	// used by the HUD
	public void drawImmediateNoScale(Graphics2D g2, boolean drawEnergon, boolean drawOutline) {
		drawImmediate(g2,drawEnergon,drawOutline, true);
	}

	private void drawAction(Graphics2D g2) {
		switch (attackAction) {
			case ATTACKING:
				BufferedImage target;
				if (getTeam() == Team.A)
					target = crosshair.image;
				else
					target = crosshairBlue.image;
				if (target != null) {
					AffineTransform trans = AffineTransform.getTranslateInstance(targetLoc.getX(), targetLoc.getY());
					trans.scale(1.0 / target.getWidth(), 1.0 / target.getHeight());
					g2.drawImage(target, trans, null);
				}
				// it's easier to see what is being attacked if we draw
				// the crosshair in addition to the cannonball
				//if (info.type != RobotType.CHAINER) {
				if (getTeam() == Team.A)
					g2.setColor(Color.RED);
				else
					g2.setColor(Color.BLUE);
				g2.setStroke(mediumStroke);
				g2.draw(new Line2D.Double(getDrawX() + 0.5, getDrawY() + 0.5,
						targetLoc.getX() + 0.5, targetLoc.getY() + 0.5));
				//}
				break;
			case DRAINING:
				// if the channeler is draining, we draw the hatch
				drawChannelerDrain(g2);
				break;
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

	public TeleportAnim createTeleportAnim(MapLocation src, MapLocation loc){
		return new TeleportAnim(src, loc);
	}

	public ExplosionAnim createDeathExplosionAnim(boolean isArchon) {
		return new ExplosionAnim();
	}

	public MortarAttackAnim createMortarAttackAnim(MapLocation target) {
		return new MortarAttackAnim(loc, target);
	}

	public EnergonTransferAnim createEnergonTransferAnim(MapLocation loc, RobotLevel height, float amt, boolean isFlux) {
		return new EnergonTransferAnim(this,loc,amt,isFlux);
	}

	public ExplosionAnim createMortarExplosionAnim(Animation mortarAttackAnim) {
		ExplosionAnim anim = new ExplosionAnim(((MortarAttackAnim)mortarAttackAnim).getTargetLoc(), 1.8);
		anim.setExplosionToggle(ExplosionAnim.ExplosionToggle.DETONATES);
		return anim;
	}

	/*
	public void setAura(AuraType auraType, GameMap m) {
		auraAnimation = new AuraAnimation(this, auraType);
		shouldAura = true;
	}
	*/

	public void updateRound() {
		super.updateRound();

		if(teleportRounds>0) {
			teleportRounds--;
			if(teleportLoc==null&&teleportRounds%3==2)
				dir = dir.rotateRight().rotateRight();
		}
/*
		if (auraAnimation != null) {
			auraAnimation.updateRound();
			
		}
*/
		/*
		if(!shouldAura){
			auraAnimation = null;
		}
		shouldAura = false;
		*/
	}

	public void activateTeleporter() {
		teleportRounds=GameConstants.TELEPORT_DELAY;
	}

	public void activateTeleport(MapLocation teleportLoc) {
		teleportRounds=GameConstants.TELEPORT_DELAY;
		this.teleportLoc=teleportLoc;
	}
}
