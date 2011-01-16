package battlecode.client.viewer.render;

import battlecode.client.viewer.AbstractAnimation;
import java.awt.Color;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.FluxDepositState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.common.MapLocation;
import battlecode.common.Chassis;
import battlecode.common.GameConstants;
import battlecode.common.Team;
import battlecode.common.ComponentType;

import battlecode.world.GameMap;
import battlecode.serial.RoundStats;
import java.awt.geom.AffineTransform;
import java.util.EnumMap;

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
    private MapLocation[][] convexHullsA, convexHullsB;
    // number of blocks in current draw state
    //int[][] blockNumber;

    public DrawState() {
        groundUnits = new LinkedHashMap<Integer, DrawObject>();
        airUnits = new LinkedHashMap<Integer, DrawObject>();
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

    protected DrawObject createDrawObject(Chassis type, Team team) {
        return new DrawObject(type, team);
    }

    public MapLocation[][] getConvexHullsA() {
        return convexHullsA;
    }

    public MapLocation[][] getConvexHullsB() {
        return convexHullsB;
    }

    private synchronized void copyStateFrom(DrawState src) {
        groundUnits.clear();
        //archonsA.clear();
        //archonsB.clear();
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
        componentTypeCountA = new EnumMap<ComponentType, Integer>(src.componentTypeCountA);
        componentTypeCountB = new EnumMap<ComponentType, Integer>(src.componentTypeCountB);
        

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

        if (drawableSet == null) {
            return;
        }

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
        for (Integer id : fluxDeposits.keySet()) {
            FluxDepositState fd = fluxDeposits.get(id);

            AffineTransform pushed = g2.getTransform();
            { // push
                g2.translate(fd.getLocation().getX() + 0.5, fd.getLocation().getY() + 0.5);
                g2.scale(.3, .3);
                double scalar;
                if (fd.getRoundsAvailable() >= 0) {
                    scalar = (float) Math.sqrt(Math.max(0, (float) fd.getRoundsAvailable()) / GameConstants.MINE_ROUNDS);
                    g2.setColor(new Color((int) (255 * (1.f - scalar)), (int) (scalar * 255), 0));
                } else {
                    scalar = (float) Math.max(GameConstants.MINE_DEPLETED_RESOURCES / GameConstants.MINE_RESOURCES,
                            (GameConstants.MINE_RESOURCES + (float) fd.getRoundsAvailable() * 0.01 / GameConstants.MINE_DEPLETION_RATE) / GameConstants.MINE_RESOURCES);
                    g2.setColor(new Color((int) (255 * scalar), 0, 0));
                }
                g2.fillOval(-1, -1, 2, 2);
                g2.setColor(java.awt.Color.gray);
                g2.drawOval(-1, -1, 2, 2);
            }
            g2.setTransform(pushed);

        }
        if (!debug.isDragging()) {
            debug.setTarget(hoverID, hoverLoc, controlBits);
        }

    }
}
