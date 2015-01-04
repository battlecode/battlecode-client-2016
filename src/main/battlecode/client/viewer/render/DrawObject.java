package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractDrawObject.RobotInfo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;

import battlecode.client.util.ImageFile;
import battlecode.client.util.ImageResource;
import battlecode.client.util.SpriteSheetFile;
import battlecode.client.viewer.AbstractAnimation;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.ActionType;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Direction;

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
  private ImageFile img;
  public static final AbstractAnimation.AnimationType[] preDrawOrder = new AbstractAnimation.AnimationType[]{TELEPORT};
  public static final AbstractAnimation.AnimationType[] postDrawOrder = new AbstractAnimation.AnimationType[]{MORTAR_ATTACK, MORTAR_EXPLOSION, ENERGON_TRANSFER};
  private int teleportRounds;
  private MapLocation teleportLoc;
  private static final double medbayRadius = 0;//= Math.sqrt(RobotType.MEDBAY.attackRadiusMaxSquared);
  private static final double shieldsRadius = 0;//Math.sqrt(RobotType.SHIELDS.attackRadiusMaxSquared);
  private static final double soldierRadius = 0;//Math.sqrt(RobotType.SOLDIER.attackRadiusMaxSquared);
  private static final double artilleryRadius = 0;//Math.sqrt(GameConstants.ARTILLERY_SPLASH_RADIUS_SQUARED);
    private static final double bashRadius = Math.sqrt(GameConstants.BASH_RADIUS_SQUARED);
  private static final Color shieldColor = new Color(150,150,255,150);
  private static final Color regenColor = new Color(150,255,150,150);
  private final DrawState overallstate;
    
  public static final ImageFile[] hatImages;
    
  static {
    File[] files = (new File("art/hats/")).listFiles();
	int hatCounter = 0;
	int nhats = 0;

    for (int x=0; x<files.length; x++) {
      final String fname = files[x].getAbsolutePath();
      final String extension = fname.substring(fname.lastIndexOf(".") + 1, fname.length());
      if (extension.toLowerCase().equals("png")) {
         ++nhats;
	  }
    }

    hatImages = new ImageFile[nhats];
    for (int x=0; x<files.length; x++) {
      final String fname = files[x].getAbsolutePath();
      final String extension = fname.substring(fname.lastIndexOf(".") + 1, fname.length());
      if (extension.toLowerCase().equals("png")) {
		 System.out.println("loading hat " + fname);
         hatImages[hatCounter++] = new ImageFile(fname);
	  }
    }
  }
    

  public DrawObject(RobotType type, Team team, int id, DrawState state) {
    super(type, team, id);
    img = ir.getResource(info, getAvatarPath(info), !type.isBuilding);
    maxEnergon = type.maxHealth;
    overallstate = state;
  }


  public DrawObject(DrawObject copy) {
    super(copy);
    img = copy.img;
    maxEnergon = copy.maxEnergon;
    if (animations.containsKey(ENERGON_TRANSFER)) {
      EnergonTransferAnim a = (EnergonTransferAnim) animations.get(ENERGON_TRANSFER);
      a.setSource(this);
    }
    overallstate = copy.overallstate;
  }

  public static void loadAll() {
    for (RobotType type : RobotType.values()) {
      for (Team team : Team.values()) {
        RobotInfo robotInfo = new RobotInfo(type, team);
        ir.getResource(robotInfo, getAvatarPath(robotInfo), !type.isBuilding);
      }
    }
  }

  private static String getAvatarPath(RobotInfo ri) {
    return getAvatarPath(ri.type.toString().toLowerCase(), ri.team);

  }

  private static String getAvatarPath(String type, Team team) {
    return "art/" + type + (team == Team.NEUTRAL ? "0" : (team == Team.A ? "1" : "2")) + ".png";
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
        for (int i = -21; i <= 21; i++) for (int j = -21; j <= 21; j++) {
            int distSq = i * i + j * j;
            if (distSq <= viewrange) {
            AffineTransform trans = AffineTransform.getTranslateInstance(i, j);
            trans.scale(1.0 / sensorImg.getWidth(), 1.0 / sensorImg.getHeight());
            g2.drawImage(sensorImg, trans, null);
            }
            if ((info.type.attackPower > 0)
                && distSq <= info.type.attackRadiusSquared) {
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

  public void draw(Graphics2D g2, boolean focused, boolean lastRow) {

    if (RenderConfiguration.showRangeHatch() && focused) {
      drawRangeHatch(g2);
    }


    AffineTransform pushed = g2.getTransform();
    g2.translate(getDrawX(), getDrawY());
    drawImmediate(g2, focused, lastRow);
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

  public void drawImmediate(Graphics2D g2, boolean focused, boolean isHUD, boolean lastRow) {
    
    Color c = getTeam() == Team.A ? Color.RED : Color.BLUE;
    c = c.brighter().brighter().brighter();
    
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
      drawStatusBars(g2, focused, lastRow);
    
      drawRobotImage(g2);
        
      if ( (RenderConfiguration.showActionLines() || focused) && getType() == RobotType.SOLDIER)
      {
        if (actionType == ActionType.MOVING)
        {
          g2.setColor(c);
          g2.setStroke(thickStroke);
          g2.draw(new Line2D.Double(0.5, 0.5,
                                    0.5 - dir.dx, 0.5 - dir.dy));
        }
      }
    
      if (focused) {
        g2.setColor(Color.YELLOW);
        g2.setStroke(outlineStroke);
        g2.draw(outline);
      }
      
      drawBroadcast(g2);
    }
  }

  public void drawStatusBars(Graphics2D g2, boolean focused, boolean lastRow) {
    boolean showEnergon = RenderConfiguration.showEnergon() || focused;
    if (showEnergon) {
      Rectangle2D.Float rect = new Rectangle2D.Float(0, lastRow?0.85f:1, 1, 0.15f);
      g2.setColor(Color.BLACK);
      g2.fill(rect);
      float frac = Math.min((float) (energon / maxEnergon), 1);
      rect.width = frac;
      if (frac < 0)
        frac = 0;
      g2.setColor(new Color(Math.min(1 - 0.5f * frac, 1.5f - 1.5f * frac),
                            Math.min(1.5f * frac, 0.5f + 0.5f * frac), 0));
      g2.fill(rect);
    }
      
    //building progress bar
    if (aliveRounds < buildDelay){
    	Rectangle2D.Float rect;
      rect = new Rectangle2D.Float(0, 0, 1, 0.15f);
      g2.setColor(Color.BLACK);
      g2.fill(rect);
      float frac = ((float)aliveRounds)/buildDelay;
      rect.width = frac;
      g2.setColor(new Color(1f, 0f, 0f));
      g2.fill(rect);   	
    }
  }

  // draw translated to robot location
  public void drawRobotImage(Graphics2D g2) {
    // could be used for rotations or such, remember origin for rotation
    AffineTransform trans = new AffineTransform();
    BufferedImage image = getTypeSprite();
    if (image == null) {
      System.out.println("missing image for type: " + info.type.toString());
    }
    trans.scale(1.0 / image.getWidth(), 1.0 / image.getHeight());
    g2.drawImage(image, trans, null);

    // hats
    if (RenderConfiguration.showHats()) {
      double hatscale = 1.5;
      AffineTransform pushed2 = g2.getTransform();
      g2.translate((2.0-hatscale)/4.0, 0.2);
      double width = image.getWidth();
      trans = AffineTransform.getScaleInstance(hatscale/image.getWidth(), hatscale/image.getWidth());
      for (int x=0; x<hats.length(); x++)
      {
                		
        try {
            image = hatImages[(int)hats.charAt(x)].image;
            g2.translate(0, -hatscale/width*(image.getHeight()-2));
            g2.drawImage(image, trans, null);
        } catch (Exception e) {
			System.out.println("x = " + x);
			System.out.println("value at x = " + (int)hats.charAt(x));
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
      for (int i = 0; i < 20; i++) {
        if ((broadcast & (1 << i)) != 0x00) {
          double r = i * drdR;
          g2.setColor(new Color(1, 0, 1, 0.05f * (20 - i)));
          g2.draw(new Ellipse2D.Double(0.5 - r, 0.5 - r, 2 * r, 2 * r));
        }
      }
    }
  }

  public void drawImmediate(Graphics2D g2, boolean focused, boolean lastRow) {
    drawImmediate(g2, focused, false, lastRow);
  }

  // used by the HUD
  public void drawImmediateNoScale(Graphics2D g2, boolean focused, boolean lastRow) {
    drawImmediate(g2, focused, true, lastRow);
  }

  private boolean isAttacking() {
    return actionType == ActionType.ATTACKING;
  }

  private void drawAction(Graphics2D g2) {
    if (isAttacking() && RenderConfiguration.showAttack())
    {
      g2.setColor(getTeam() == Team.A ? Color.RED : Color.BLUE);
      g2.setStroke(mediumStroke);
            
      switch (info.type) {
      case BEAVER:
      case SOLDIER:
      case MINER:
      case DRONE:
      case TANK:
      case COMMANDER:
      case TOWER:
        g2.draw(new Line2D.Double(getDrawX() + 0.5, getDrawY() + 0.5,
                                  targetLoc.x + 0.5, targetLoc.y + 0.5));
        break;
      case BASHER:
	  g2.draw(new Ellipse2D.Double(getDrawX() + .5 - bashRadius,
				       getDrawY() + .5 - bashRadius,
				       bashRadius * 2, bashRadius * 2));
	  break;
      case HQ:
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
        break;
      default:
        	
      }
    }
  }

  private BufferedImage getTypeSprite() {
      if (info.type.isBuilding) {
	  return img.image;
      } else {
	  return ((SpriteSheetFile) img).spriteForDirection(dir);
      }
  }

  public void setTeam(Team team) {
    super.setTeam(team);
    img = ir.getResource(info, getAvatarPath(info), !info.type.isBuilding);
  }

  public void setMaxEnergon(double maxEnergon) {
    this.maxEnergon = maxEnergon;
  }


  public ExplosionAnim createDeathExplosionAnim(boolean unused) {
    if(isSuiciding) {
      return new SuicideAnim(); // a subclass of explosion
    } else {
      return new ExplosionAnim();
    }
  }

  public MortarAttackAnim createMortarAttackAnim(MapLocation target) {
    return new MortarAttackAnim(loc, target);
  }

  public EnergonTransferAnim createEnergonTransferAnim(MapLocation loc, float amt, boolean isFlux) {
    return new EnergonTransferAnim(this, loc, amt, isFlux);
  }

  public ExplosionAnim createMortarExplosionAnim(Animation mortarAttackAnim) {
    ExplosionAnim anim = new ExplosionAnim(((MortarAttackAnim) mortarAttackAnim).getTargetLoc(), 1.8);
    anim.setExplosionToggle(ExplosionAnim.ExplosionToggle.DETONATES);
    return anim;
  }

  public void updateRound() {
    super.updateRound();
  }

  public void activateTeleporter() {
    teleportRounds = 1;
  }

  public void activateTeleport(MapLocation teleportLoc) {
    teleportRounds = 1;
    this.teleportLoc = teleportLoc;
  }
}
