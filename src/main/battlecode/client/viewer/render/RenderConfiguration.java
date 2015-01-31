package battlecode.client.viewer.render;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import battlecode.common.Team;

public class RenderConfiguration {

  private static RenderConfiguration instance = null;

  private static boolean broadcast = true;
  private static boolean attack = true;
  private static boolean discrete = true;
  private static boolean energon = true;
  private static boolean gridlines = true;
  private static boolean explosions = true;
  private static boolean rangeHatch = false;
  private static boolean spawnRadii = true;
  private static boolean ambientMusic = false;
  private static boolean actionlines = false;
  private static boolean hats = true;
  // the next two are related, only thresh cows if they are shown
  private static boolean cows = true;
  private static boolean threshCows = false;
  private static int indicatorDotToggles;
  private static boolean supplyTransfers = true;
  private static boolean supplyIndicators = true;

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
  public static void toggleActionLines() { actionlines = !actionlines; }
  public static void toggleDiscrete()  { discrete  = !discrete;  }
  public static void toggleEnergon()   { energon   = !energon;   }
  public static void toggleGridlines() { gridlines = !gridlines; }
  public static void toggleExplosions() { explosions = !explosions; }
  public static void toggleRangeHatch() { rangeHatch = !rangeHatch; }
  public static void toggleSpawnRadii() { spawnRadii = !spawnRadii; }
  public static void toggleAmbientMusic(){ambientMusic = !ambientMusic;}
  public static void toggleShowHats() { hats = !hats; }
  public static void toggleIndicatorDots() { indicatorDotToggles++; }
  public static void toggleSupplyTransfers() { supplyTransfers = !supplyTransfers; }
  public static void toggleSupplyIndicators() { supplyIndicators = !supplyIndicators; }

  public static void toggleCows() {
    if (!cows) {
      cows = true;
      threshCows = true;
    }
    else if (threshCows) {
      threshCows = false;
    }
    else {
      cows = false;
    }
  }
  
  public static void setTournamentMode(boolean flag) {
    tournamentMode = flag;

    // TODO remove this someday. this is a hack. I think.
    // anyways tournament streams look better without broadcasts and with discrete movement
    if (tournamentMode) {
        discrete = false;
        broadcast = false;
    }
  }

  public static boolean showBroadcast() { return broadcast; }
  public static boolean showAttack() { return attack; }
  public static boolean showActionLines() { return actionlines; }
  public static boolean showDiscrete()  { return discrete;  }
  public static boolean showEnergon()   { return energon;   }
  public static boolean showGridlines() { return gridlines; }
  public static boolean showExplosions() { return explosions; }
  public static boolean showRangeHatch() { return rangeHatch; }
  public static boolean showSpawnRadii() { return spawnRadii; }
  public static boolean showHats() { return hats; }
  public static boolean showCows  () { return cows; }
  public static boolean threshCows  () { return threshCows; }
  public static boolean showIndicatorDots(Team t) { return ((indicatorDotToggles>>t.ordinal())&1)!=0; }
  public static boolean showSupplyTransfers() { return supplyTransfers; }
  public static boolean showSupplyIndicators() { return supplyIndicators; }
  
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
