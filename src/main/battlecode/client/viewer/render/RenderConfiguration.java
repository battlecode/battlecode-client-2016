package battlecode.client.viewer.render;

import battlecode.common.Team;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

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
    private static boolean parts = true;
    private static int indicatorDotToggles;
    private static boolean transfers = true;
    private static boolean infectionIndicators = true;

    private static boolean tournamentMode = false;

    private float spriteSize = 25;
    private float pixelSize = 1.0f / spriteSize;

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

    private RenderConfiguration() {
    }

    public static void toggleBroadcast() {
        broadcast = !broadcast;
    }

    public static void toggleAttack() {
        attack = !attack;
    }

    public static void toggleActionLines() {
        actionlines = !actionlines;
    }

    public static void toggleDiscrete() {
        discrete = !discrete;
    }

    public static void toggleEnergon() {
        energon = !energon;
    }

    public static void toggleGridlines() {
        gridlines = !gridlines;
    }

    public static void toggleExplosions() {
        explosions = !explosions;
    }

    public static void toggleRangeHatch() {
        rangeHatch = !rangeHatch;
    }

    public static void toggleSpawnRadii() {
        spawnRadii = !spawnRadii;
    }

    public static void toggleAmbientMusic() {
        ambientMusic = !ambientMusic;
    }

    public static void toggleShowHats() {
        hats = !hats;
    }

    public static void toggleIndicatorDots() {
        indicatorDotToggles++;
    }

    public static void toggleTransfers() {
        transfers = !transfers;
    }

    public static void toggleInfectionIndicators() {
        infectionIndicators = !infectionIndicators;
    }

    public static void toggleParts() { parts = !parts; }

    public static void setTournamentMode(boolean flag) {
        tournamentMode = flag;

        // TODO remove this someday. this is a hack. I think.
        // anyways tournament streams look better without broadcasts and with
        // discrete movement
        if (tournamentMode) {
            discrete = false;
            broadcast = false;
        }
    }

    public static boolean showParts() { return parts; }

    public static boolean showBroadcast() {
        return broadcast;
    }

    public static boolean showAttack() {
        return attack;
    }

    public static boolean showActionLines() {
        return actionlines;
    }

    public static boolean showDiscrete() {
        return discrete;
    }

    public static boolean showEnergon() {
        return energon;
    }

    public static boolean showGridlines() {
        return gridlines;
    }

    public static boolean showExplosions() {
        return explosions;
    }

    public static boolean showRangeHatch() {
        return rangeHatch;
    }

    public static boolean showSpawnRadii() {
        return spawnRadii;
    }

    public static boolean showHats() {
        return hats;
    }

    public static boolean showInfectionIndicators() {
        return infectionIndicators;
    }

    public static boolean showIndicatorDots(Team t) {
        return ((indicatorDotToggles >> t.ordinal()) & 1) != 0;
    }

    public static boolean showTransfers() {
        return transfers;
    }

    public static boolean playAmbientMusic() {
        return ambientMusic;
    }

    public static boolean isTournamentMode() {
        return tournamentMode;
    }

    public float getSpriteSize() {
        return spriteSize;
    }

    public void setSpriteSize(float size) {
        spriteSize = size;
        pixelSize = 1.0f / spriteSize;
        resized = true;
    }

    public void updateMapTransform(AffineTransform trans) {
        if (resized) {
            try {
                mapInverse = trans.createInverse();
                resized = false;
            } catch (NoninvertibleTransformException e) {
            }
        }
    }

    public Point2D.Float getMapCoordinates(int x, int y, Point2D.Float dst) {
        srcPt[0] = x;
        srcPt[1] = y;
        mapInverse.transform(srcPt, 0, dstPt, 0, 1);
        if (dst == null) {
            return new Point2D.Float(dstPt[0], dstPt[1]);
        } else {
            dst.x = dstPt[0];
            dst.y = dstPt[1];
            return dst;
        }
    }
}
