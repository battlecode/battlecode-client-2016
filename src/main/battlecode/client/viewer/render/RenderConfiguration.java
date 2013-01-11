package battlecode.client.viewer.render;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class RenderConfiguration {

	private static RenderConfiguration instance = null;

	private static boolean broadcast = true;
	private static boolean attack = true;
	private static boolean discrete = false;
	private static boolean energon = true;
	private static boolean flux = true;
	private static boolean gridlines = true;
	private static boolean blocks = true;
	private static boolean detonates = true;
	private static boolean transfers = true;
	private static boolean explosions = true;
	private static boolean rangeHatch = false;
	private static boolean spawnRadii = true;
	private static boolean ambientMusic = false;
	private static boolean teleport = true;
	private static boolean teleportGhosts = true;

	private static boolean ground = true;
	private static boolean air	= true;

	private static boolean tournamentMode = false;

  private float spriteSize = 16;
  private float pixelSize = 1.0f/spriteSize;
  private AffineTransform spriteScale;

	private boolean resized = true;
	private AffineTransform mapInverse = new AffineTransform();
	private float[] srcPt = new float[2];
	private float[] dstPt = new float[2];

	public static synchronized RenderConfiguration getInstance() {
		init();
		assert instance != null;
		return instance;
	}

  public static synchronized void init() {
    if (instance == null) {
      instance = new RenderConfiguration();
    }
  }

  private RenderConfiguration() {}

	public static void toggleBroadcast() { broadcast = !broadcast; }
	public static void toggleAttack() { attack = !attack; }
	public static void toggleDiscrete()  { discrete  = !discrete;  }
	public static void toggleEnergon()   { energon   = !energon;   }
	public static void toggleGridlines() { gridlines = !gridlines; }
	public static void toggleBlocks() { blocks = !blocks; }
	public static void toggleDetonates() { detonates = !detonates; }
	public static void toggleTransfers() { transfers = !transfers; }
	public static void toggleExplosions() { explosions = !explosions; }
	public static void toggleRangeHatch() { rangeHatch = !rangeHatch; }
	public static void toggleSpawnRadii() { spawnRadii = !spawnRadii; }
	public static void toggleAmbientMusic(){ambientMusic = !ambientMusic;}
	public static void toggleTeleport() { teleport = !teleport; }
	public static void toggleTeleportGhosts() { teleportGhosts = !teleportGhosts; }
	public static void toggleFlux() { flux = !flux; }

	public static void toggleDrawHeight() {
		if(!air && !ground) {
			ground = true;
			air = true;
		} else if(ground && !air) {
			ground = false;
			air = true;
		} else if(air && !ground) {
			ground = false;
			air = false;
		} else {
			ground = true;
			air = false;
		}
	}

	public static void setTournamentMode(boolean flag) {
		tournamentMode = flag;
	}

	public static boolean showBroadcast() { return broadcast; }
	public static boolean showAttack() { return attack; }
	public static boolean showDiscrete()  { return discrete;  }
	public static boolean showEnergon()   { return energon;   }
	public static boolean showGridlines() { return gridlines; }
	public static boolean showBlocks() { return blocks; }
	public static boolean showDetonates() { return detonates; }
	public static boolean showTransfers() { return transfers; }
	public static boolean showExplosions() { return explosions; }
	public static boolean showRangeHatch() { return rangeHatch; }
	public static boolean showSpawnRadii() { return spawnRadii; }
	public static boolean showTeleport() { return teleport; }
	public static boolean showTeleportGhosts() { return teleportGhosts; }
	public static boolean showFlux() { return flux; }

	public static boolean showGround() { return ground; }
	public static boolean showAir   () { return air;    }
	
	public static boolean playAmbientMusic(){return ambientMusic;}

	public static boolean isTournamentMode() { return tournamentMode; }

  public float getSpriteSize() {
    return spriteSize;
  }

	public void setSpriteSize(float size) {
		spriteSize = size;
		pixelSize = 1.0f/spriteSize;
		resized = true;
	}

	public float getPixelSize() {
		return pixelSize;
	}

	public boolean isPrescaled() {
		return false;
	}

  public AffineTransform getSpriteScaleTransform() {
    return AffineTransform.getScaleInstance(spriteSize, spriteSize);
  }

  public double getImageScale() {
    return 1.0/64;
  }

	public void updateMapTransform(AffineTransform trans) {
		if (resized) {
			try {
				mapInverse = trans.createInverse();
				resized = false;
			}
			catch (NoninvertibleTransformException e) {}
		}
	}

	public Point2D.Float getMapCoordinates(int x, int y, Point2D.Float dst) {
		srcPt[0] = x;
		srcPt[1] = y;
		mapInverse.transform(srcPt, 0, dstPt, 0, 1);
		if (dst == null) {
			return new Point2D.Float(dstPt[0], dstPt[1]);
		}
		else {
			dst.x = dstPt[0];
			dst.y = dstPt[1];
			return dst;
		}
	}

  public static BufferedImage createCompatibleImage(int width, int height) {
    return GraphicsEnvironment.getLocalGraphicsEnvironment()
      .getDefaultScreenDevice().getDefaultConfiguration()
      .createCompatibleImage(width, height);
  }
}
