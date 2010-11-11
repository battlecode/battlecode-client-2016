package battlecode.client.viewer.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.FluxDepositState;
import battlecode.client.viewer.GameState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.common.MapLocation;
import battlecode.common.Chassis;
import battlecode.common.Team;

import battlecode.world.GameMap;
import battlecode.serial.MatchHeader;
import battlecode.serial.RoundStats;

import battlecode.common.GameConstants;
import battlecode.common.TerrainTile;

public class DrawState extends AbstractDrawState<DrawObject> {

	protected static final Color dragShadow = new Color(0.5f, 0.5f, 0.5f, 0.5f);

    private static class Factory implements GameStateFactory<DrawState> {

        public DrawState createState(GameMap map) {
            return new DrawState(map);
        }

        public DrawState cloneState(DrawState state) {
            return new DrawState(state);
        }

        public void copyState(DrawState src, DrawState dst) {
            dst.copyStateFrom(src);
        }
    }
    public static final GameStateFactory<DrawState> FACTORY = new Factory();

    //private Map<Integer, DrawObject> groundUnits;
    //private Map<Integer, DrawObject> airUnits;
    private List<DrawObject> towers;
    // these need to be drawn before all the units,
    // so don't draw them from DrawObjects
    //private ArrayList<TeleportAnim> teleportAnims;
    private Map<Integer, FluxDepositState> fluxDeposits;
    private MapLocation[][] convexHullsA, convexHullsB;
    // number of blocks in current draw state
    //int[][] blockNumber;

    public DrawState() {
        groundUnits = new LinkedHashMap<Integer, DrawObject>();
        airUnits = new LinkedHashMap<Integer, DrawObject>();
        archonsA = new ArrayList<DrawObject>(6);
        archonsB = new ArrayList<DrawObject>(6);
        towers = new LinkedList<DrawObject>();
        fluxDeposits = new LinkedHashMap<Integer, FluxDepositState>();
        currentRound = -1;
        convexHullsA = new MapLocation[0][];
        convexHullsB = new MapLocation[0][];

    }

    private DrawState(GameMap map) {
        this();
        this.setGameMap(map);
    }

    private DrawState(DrawState clone) {
        this();
        copyStateFrom(clone);
    }

	protected DrawObject createDrawObject(Chassis type, Team team) { return new DrawObject(type,team); }

    public MapLocation[][] getConvexHullsA() {
        return convexHullsA;
    }

    public MapLocation[][] getConvexHullsB() {
        return convexHullsB;
    }

    private synchronized void copyStateFrom(DrawState src) {
        groundUnits.clear();
        archonsA.clear();
        archonsB.clear();
        towers.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet()) {
            DrawObject copy = new DrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
        }
        airUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.airUnits.entrySet()) {
            DrawObject copy = new DrawObject(entry.getValue());
            airUnits.put(entry.getKey(), copy);
            tryAddArchon(copy);
        }
        fluxDeposits.clear();
        for (Map.Entry<Integer, FluxDepositState> entry : src.fluxDeposits.entrySet()) {
            fluxDeposits.put(entry.getKey(), new FluxDepositState(entry.getValue()));
        }
        stats = src.stats;

        // HACK: from DrawMap update hack
		/*
        blockNumber = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
        blockNumber[i][j] = src.blockNumber[i][j];
        }
        }
         */

        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }

        currentRound = src.currentRound;
        convexHullsA = new MapLocation[src.convexHullsA.length][];
        if (convexHullsA.length > 0) {
            for (int i = 0; i < convexHullsA.length; i++) {
                convexHullsA[i] = new MapLocation[src.convexHullsA[i].length];
                System.arraycopy(src.convexHullsA[i], 0, convexHullsA[i], 0, convexHullsA[i].length);
            }
        }

        convexHullsB = new MapLocation[src.convexHullsB.length][];
        if (convexHullsB.length > 0) {
            for (int i = 0; i < convexHullsB.length; i++) {
                convexHullsB[i] = new MapLocation[src.convexHullsB[i].length];
                System.arraycopy(src.convexHullsB[i], 0, convexHullsB[i], 0, convexHullsB[i].length);
            }
        }


    }

    protected void mineFlux(DrawObject obj) {
    }

    // return the set of all flux deposits to be drawn in proper order by GameRenderer
    public Collection<FluxDepositState> getFluxDeposits() {
        return fluxDeposits.values();
    }

    public synchronized void apply(RoundStats stats) {
        this.stats = stats;
    }

    public DrawObject getDrawObject(int id) {
        try {
            return getRobot(id);
        } catch (AssertionError e) {
            return null;
        }
    }

    private void drawDragged(Graphics2D g2, DebugState debug, DrawObject obj) {
		/*
        MapLocation loc = obj.getLocation();
        float dx = debug.getDX(), dy = debug.getDY();
        g2.setColor(dragShadow);
        g2.fill(new Rectangle2D.Float(Math.round(loc.getX() + dx),
                Math.round(loc.getY() + dy), 1, 1));
        AffineTransform pushed = g2.getTransform(); // push
        g2.translate(dx, dy);
        obj.draw(g2, true);
        g2.setTransform(pushed); // pop;
		*/
    }

    /**
     * Draws the current game state. This method is always called from the
     * Swing event-dispatch thread, and in particular blocks calls to
     * updateRound.
     * @param g2 The graphics context, transformed to MapLocation-space
     * @param debug The debug state, including MapLocation-space mouse state
     */
    public synchronized void draw(Graphics2D g2, DebugState debug) {
        if (RenderConfiguration.showSpawnRadii()) {
            /*
            for (DrawObject tower : towers) {
            tower.drawSpawnRadius(g2);
            }
             */
        }
        int dragID = debug.getDragID();
        int focusID = debug.getFocusID();
        int hoverID = -1;
        MapLocation hoverLoc = null;
        long controlBits = 0;
        Iterable<Map.Entry<Integer, DrawObject>> drawableSet = getDrawableSet();

        if (drawableSet == null)
            return;

        for (Map.Entry<Integer, DrawObject> entry : drawableSet) {

            int id = entry.getKey();
            DrawObject obj = entry.getValue();

            if (id == dragID) {
                drawDragged(g2, debug, obj);
            } else {
                if (Math.abs(debug.getX() - obj.getDrawX() - 0.5) < 0.5
                        && Math.abs(debug.getY() - obj.getDrawY() - 0.5) < 0.5) {
                    hoverID = id;
                    hoverLoc = obj.getLocation();
                    controlBits = obj.getControlBits();
                }
                obj.draw(g2, id == focusID || id == hoverID);
            }
        }
        if (!debug.isDragging()) {
            debug.setTarget(hoverID, hoverLoc, controlBits);
        }

    }
}
