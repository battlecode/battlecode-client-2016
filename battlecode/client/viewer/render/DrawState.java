package battlecode.client.viewer.render;

import battlecode.world.signal.DeploySignal;

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
import battlecode.common.RobotType;
import battlecode.common.Team;

import battlecode.world.InternalTerrainTile;
import battlecode.world.GameMap;
import battlecode.serial.MatchHeader;
import battlecode.serial.RoundStats;
import battlecode.world.signal.AttackSignal;
import battlecode.world.signal.AwesomenessSignal;
import battlecode.world.signal.BroadcastSignal;
import battlecode.world.signal.BytecodesUsedSignal;
import battlecode.world.signal.ControlBitsSignal;
import battlecode.world.signal.ConvexHullSignal;
import battlecode.world.signal.DeathSignal;
import battlecode.world.signal.DoTeleportSignal;
import battlecode.world.signal.EnergonChangeSignal;
import battlecode.world.signal.EnergonTransferSignal;
import battlecode.world.signal.EvolutionSignal;
import battlecode.world.signal.FluxChangeSignal;
import battlecode.world.signal.FluxTransferSignal;
import battlecode.world.signal.IndicatorStringSignal;
import battlecode.world.signal.LightningShieldSignal;
import battlecode.world.signal.MapOriginSignal;
import battlecode.world.signal.MatchObservationSignal;
import battlecode.world.signal.MovementOverrideSignal;
import battlecode.world.signal.MovementSignal;
import battlecode.world.signal.SetDirectionSignal;
import battlecode.world.signal.SpawnSignal;
import battlecode.world.signal.StartTeleportSignal;

import battlecode.common.GameConstants;
import battlecode.common.TerrainTile.TerrainType;
import battlecode.world.signal.SetAuraSignal;

public class DrawState extends AbstractDrawState<DrawObject> {

	protected static final Color dragShadow = new Color(0.5f, 0.5f, 0.5f, 0.5f);

    private static class Factory implements GameStateFactory<DrawState> {

        public DrawState createState(MatchHeader header) {
            return new DrawState(header);
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
    private ArrayList<AuraAnimation> auraAnimsNextRound;
    private ArrayList<AuraAnimation> auraAnimsThisRound;
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
        auraAnimsThisRound = new ArrayList<AuraAnimation>();
        auraAnimsNextRound = new ArrayList<AuraAnimation>();
        //teleportAnims = new ArrayList<TeleportAnim>();
        towers = new LinkedList<DrawObject>();
        fluxDeposits = new LinkedHashMap<Integer, FluxDepositState>();
        currentRound = -1;
        // HACK: from DrawMap update hack
		/*
        blockNumber = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++)
        blockNumber[i][j] = 0;
        }
         */
        convexHullsA = new MapLocation[0][];
        convexHullsB = new MapLocation[0][];

        /*
        flux = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
        flux[i][j] = 0;
        }
        }
         */
    }

    private DrawState(MatchHeader header) {
        this();
        this.setGameMap(header.getMap());
        flux = new byte[gameMap.getHeight()][gameMap.getWidth()];
    }

    private DrawState(DrawState clone) {
        this();
        copyStateFrom(clone);
    }

	protected DrawObject createDrawObject(RobotType type, Team team) { return new DrawObject(type,team); }

    public void setGameMap(GameMap map) {
        this.gameMap = new GameMap(map);
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public MapLocation[][] getConvexHullsA() {
        return convexHullsA;
    }

    public MapLocation[][] getConvexHullsB() {
        return convexHullsB;
    }

    public byte[][] getFlux() {
        return flux;
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

        // the anims themselves don't need to be cloned
        auraAnimsThisRound = new java.util.ArrayList<AuraAnimation>(src.auraAnimsThisRound);
        auraAnimsNextRound = new java.util.ArrayList<AuraAnimation>(src.auraAnimsNextRound);

        // HACK: from DrawMap update hack
		/*
        blockNumber = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
        blockNumber[i][j] = src.blockNumber[i][j];
        }
        }
         */

        flux = new byte[src.flux.length][];

        for (int i = 0; i < flux.length; i++)
            flux[i] = java.util.Arrays.copyOf(src.flux[i], src.flux[i].length);

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
        if (gameMap != null) {
            MapLocation robotLoc = obj.getLocation();
            int x = robotLoc.getX();
            int y = robotLoc.getY();
            for (int i = fluxMineOffsetsX.length - 1; i >= 0; i--) {
                int normalX = x + fluxMineOffsetsX[i] - origin.getX();
                int normalY = y + fluxMineOffsetsY[i] - origin.getY();
                if (normalX < 0 || normalX >= gameMap.getWidth())
                    continue;
                if (normalY < 0 || normalY >= gameMap.getHeight())
                    continue;
                flux[normalY][normalX] = 0;
            }
        }
    }

    // return the set of all flux deposits to be drawn in proper order by GameRenderer
    public Collection<FluxDepositState> getFluxDeposits() {
        return fluxDeposits.values();
    }

    public Void visitSetAuraSignal(SetAuraSignal s) {
        auraAnimsNextRound.add(new AuraAnimation(s.getAura(), getRobot(s.getRobotID()).getLocation()));
        //getRobot(s.getRobotID()).setAura(s.getAura(), gameMap);
        return null;
    }

    public Void visitStartTeleportSignal(StartTeleportSignal s) {
        getRobot(s.getFromTeleporterID()).activateTeleporter();
        getRobot(s.getToTeleporterID()).activateTeleporter();
        getRobot(s.getRobotID()).activateTeleport(s.getTeleportLoc());
        return null;
    }

    public Void visitDoTeleportSignal(DoTeleportSignal s) {
        //System.out.println("DTS");
        DrawObject obj = getRobot(s.getRobotID());
        obj.setTeleport(obj.getLocation(), s.getTeleportLoc());
        obj.setLocation(s.getTeleportLoc());
        return null;
    }

    public Void visitConvexHullSignal(ConvexHullSignal s) {
        if (s.getTeam() == s.getTeam().A) {
            convexHullsA = s.getConvexHulls();
        } else if (s.getTeam() == s.getTeam().B) {
            convexHullsB = s.getConvexHulls();
        }
        //System.out.println("CH signal " + s.getConvexHulls().length);
        return null;
    }

    public synchronized void apply(RoundStats stats) {
        this.stats = stats;
    }

    protected synchronized void updateRound() {
			
			super.updateRound();
        
        //teleportAnims.clear();
        ArrayList<AuraAnimation> tmp = auraAnimsThisRound;
        auraAnimsThisRound = auraAnimsNextRound;
        tmp.clear();
        auraAnimsNextRound = tmp;

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

        //System.out.println("draw "+currentRound);
        // TODO: this draws the aura one turn too early
        if (RenderConfiguration.showSpawnRadii())
            for (AuraAnimation anim : auraAnimsThisRound) {
                anim.draw(g2);
            }


        /*for (TeleportAnim anim : teleportAnims) {
        anim.draw(g2);
        }*/


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

    // get the map origin
    public Void visitMapOriginSignal(MapOriginSignal s) {
        origin = s.getMapOrigin();
        return null;
    }

    public Void visitDeploySignal(DeploySignal s) {
        return null; //FIXME I AM NOT SUPPORTED YET!
    }

    public Void visitLightningShieldSignal(LightningShieldSignal s) {
        return null;//FIXME I AM NOT IMPLEMENTED!
    }
}
