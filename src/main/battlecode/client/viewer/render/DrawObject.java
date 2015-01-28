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
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;

import battlecode.client.util.ImageFile;
import battlecode.client.util.ImageResource;
import battlecode.client.util.SpriteSheetFile;
import battlecode.client.viewer.AbstractAnimation;
import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.ActionType;
import battlecode.client.viewer.Action;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Direction;

import java.util.ArrayList;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

class DrawObject extends AbstractDrawObject<Animation> {
    public static final int LAYER_COUNT = 3;
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
    private static final ImageFile creepRed = new ImageFile("art/creep1.png");
    private static final ImageFile creepBlue = new ImageFile("art/creep2.png");
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
		 //System.out.println("loading hat " + fname);
         hatImages[hatCounter++] = new ImageFile(fname);
	  }
    }
  }
    

    public DrawObject(int currentRound, RobotType type, Team team, int id, DrawState state) {
	super(currentRound, type, team, id);
    img = ir.getResource(info, getAvatarPath(info), !type.isBuilding);
    maxEnergon = type.maxHealth;
    overallstate = state;
  }


    public DrawObject(int currentRound, DrawObject copy) {
	super(currentRound,  copy);
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

    public void draw(Graphics2D g2, boolean focused, boolean lastRow, int layer) {
	if(layer == 0) {
	    if (RenderConfiguration.showRangeHatch() && focused) {
		drawRangeHatch(g2);
	    }
	    if (info.type.isBuilding) {
		AffineTransform pushed0 = g2.getTransform();
		g2.translate(getDrawX(), getDrawY());
		drawImageTransformed(g2, new AffineTransform(),
				     (info.team == Team.A ? creepRed.image
				      : creepBlue.image), 2);
		g2.setTransform(pushed0); // pop
	    }
	}
	if(layer == 1 || (layer == 2 && info.type == RobotType.COMMANDER)) {
	    AffineTransform pushed1 = g2.getTransform();
	    g2.translate(getDrawX(), getDrawY());
	    drawImmediate(g2, focused, lastRow);
	    g2.setTransform(pushed1); // pop
	}
	if(layer == 2) {
	    // these animations shouldn't be drawn in the HUD, and they expect
	    // the origin of the Graphics2D to be the MapLocation (0,0)
	    for (AbstractAnimation.AnimationType type : postDrawOrder) {
		if (type.shown() && animations.containsKey(type)) {
		    animations.get(type).draw(g2);
		}
	    }
	}
  }

    public void drawAction(Graphics2D g2, Action a,
			   boolean focused, boolean isHUD) {
	switch (a.type) {
	case MOVING:
	    if(!isHUD) {
		drawMoving(g2, a, focused);
	    }
	    break;
	case ATTACKING:
	    if(!isHUD) {
		drawAttacking(g2, a);
	    }
	    break;
	default:
	}
    }

    protected void setTeamColor(Graphics2D g2) {
	g2.setColor(getTeam() == Team.A ? Color.RED : Color.BLUE);
    }
            


  public void drawImmediate(Graphics2D g2, boolean focused, boolean isHUD, boolean lastRow, boolean drawXP) {
    
      setTeamColor(g2);
      g2.setStroke(mediumStroke);
    
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
	if ( (RenderConfiguration.showActionLines() || focused) )
      {
	  setTeamColor(g2);
	  g2.setStroke(thickStroke);
	  g2.draw(new Line2D.Double(0.5, 0.5,
				    0.5 - dir.dx, 0.5 - dir.dy));
      }
    }

    public void drawAttacking(Graphics2D g2, Action a) {
	if (RenderConfiguration.showAttack())
	{
	    setTeamColor(g2);
	    g2.setStroke(mediumStroke);
	    MapLocation target = new MapLocation(a.target.x - loc.x,
						 a.target.y - loc.y);
	    switch (info.type) {
	    case BEAVER:
	    case SOLDIER:
	    case MINER:
	    case DRONE:
	    case TANK:
	    case COMMANDER:
	    case TOWER:
		g2.draw(new Line2D.Double(getDrawDX() + 0.5, getDrawDY() + 0.5,
					  target.x + 0.5, target.y + 0.5));
		break;
	    case BASHER:
		g2.draw(new Ellipse2D.Double(getDrawDX() + .5 - bashRadius,
					     getDrawDY() + .5 - bashRadius,
					     bashRadius * 2, bashRadius * 2));
		break;
	    case HQ:
		BufferedImage crosshairImage;
		if (getTeam() == Team.A) {
		    crosshairImage = crosshair.image;
		} else {
		    crosshairImage = crosshairBlue.image;
		}
		if (crosshairImage != null) {
		    AffineTransform trans = AffineTransform.getTranslateInstance(target.x, target.y);
		    trans.scale(1.0 / crosshairImage.getWidth(), 1.0 / crosshairImage.getHeight());
		    g2.drawImage(crosshairImage, trans, null);
		}

		g2.draw(new Line2D.Double(getDrawDX() + 0.5, getDrawDY() + 0.5,
					  target.x + 0.5, target.y + 0.5));
		break;
	    default:
        	
	    }
	}

    }

  public void drawStatusBars(Graphics2D g2, boolean focused, boolean lastRow, boolean drawXP) {
    boolean showEnergon = RenderConfiguration.showEnergon() || focused;
    if (showEnergon) {
      Rectangle2D.Float rect = getType()==RobotType.COMMANDER ? new Rectangle2D.Float(-0.2f, lastRow?0.85f:1, 1.2f, 0.2f) : 
      	new Rectangle2D.Float(0, lastRow?0.85f:1, 1, 0.15f);
      g2.setColor(Color.BLACK);
      g2.fill(rect);
      float frac = Math.min((float) (energon / maxEnergon), 1);
      rect.width = frac*(getType()==RobotType.COMMANDER?1.4f:1f);
      if (frac < 0)
        frac = 0;
      g2.setColor(new Color(Math.min(1 - 0.5f * frac, 1.5f - 1.5f * frac),
                            Math.min(1.5f * frac, 0.5f + 0.5f * frac), 0));
      g2.fill(rect);
      
    }
    
    if (drawXP && info.type == RobotType.COMMANDER){
    	Rectangle2D.Float rect = new Rectangle2D.Float(-0.2f, -0.2f, 1.2f, 0.2f);
      g2.setColor(Color.BLACK);
      g2.fill(rect);
      float frac = Math.min((float) (xp / 2000.0), 1);
      rect.width = frac*1.4f;
      if (frac < 0)
        frac = 0;
      g2.setColor(frac<0.5?Color.red:frac<0.75?Color.orange:frac<1?Color.yellow:Color.white);
      g2.fill(rect);
    }
      
    //building progress bar
    if (aliveRounds < buildDelay){
    	Rectangle2D.Float rect;
      rect = new Rectangle2D.Float(0, lastRow?0.7f:1.15f, 1, 0.15f);
      g2.setColor(Color.BLACK);
      g2.fill(rect);
      float frac = ((float)aliveRounds)/buildDelay;
      rect.width = frac;
      g2.setColor(new Color(1f, 0f, 0f));
      g2.fill(rect);   	
    }
    
    //add a box around robot if supply is 0
    if(supplyLevel<1 && info.type.needsSupply() && RenderConfiguration.showSupplyIndicators()){
    	g2.setColor(new Color(0.95f,0.95f,0.95f));
    	Rectangle2D.Float rectLeft;
    	rectLeft = new Rectangle2D.Float(0.02f,0,0.1f, 1);   	
    	g2.fill(rectLeft);
    	rectLeft = new Rectangle2D.Float(0.12f,0,0.2f,0.1f);
    	g2.fill(rectLeft);
    	rectLeft = new Rectangle2D.Float(0.12f,0.9f,0.2f,0.1f);
    	g2.fill(rectLeft);
    	
    	Rectangle2D.Float rectRight;
    	rectRight = new Rectangle2D.Float(0.88f,0,0.1f, 1);   	
    	g2.fill(rectRight);
    	rectRight = new Rectangle2D.Float(0.68f,0,0.2f,0.1f);
    	g2.fill(rectRight);
    	rectRight = new Rectangle2D.Float(0.68f,0.9f,0.2f,0.1f);
    	g2.fill(rectRight);
    	
    }
  }

    public double drawScale() {
    	if (info.type == RobotType.COMMANDER)
    		return xp>=2000?3.0:(xp>=1500?2.5:(xp>1000?2.0:1.5));
    	if (info.type == RobotType.TOWER)
    		return 2.0;
    	if (info.type == RobotType.HQ)
    		return 1.5;
    	if (info.type.isBuilding)
    		return 1.3;
    	if (info.type == RobotType.LAUNCHER)
    		return 1.25;
    	return 1;
    }

    public static void drawImageTransformed(Graphics2D g2, AffineTransform trans,
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
      System.out.println("missing image for type: " + info.type.toString());
    }
    double size = drawScale();
    drawImageTransformed(g2, trans, image, size);

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
      for (int i = 0; i < 15; i++) {
        if ((broadcast & (1 << i)) != 0x00) {
          double r = i * drdR;
          g2.setColor(new Color(1, 0, 1, 0.05f * (15 - i)));
          g2.draw(new Ellipse2D.Double(0.5 - r, 0.5 - r, 2 * r, 2 * r));
        }
      }
    }
  }

  public void drawImmediate(Graphics2D g2, boolean focused, boolean lastRow, boolean drawXP) {
    drawImmediate(g2, focused, false, lastRow, drawXP);
  }
  
  public void drawImmediate(Graphics2D g2, boolean focused, boolean lastRow) {
    drawImmediate(g2, focused, false, lastRow, true);
  }

  // used by the HUD
  public void drawImmediateNoScale(Graphics2D g2, boolean focused, boolean lastRow) {
    drawImmediate(g2, focused, true, lastRow);
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
    return anim;
  }

  public void activateTeleporter() {
    teleportRounds = 1;
  }

  public void activateTeleport(MapLocation teleportLoc) {
    teleportRounds = 1;
    this.teleportLoc = teleportLoc;
  }
}
