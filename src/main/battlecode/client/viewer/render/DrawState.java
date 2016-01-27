package battlecode.client.viewer.render;

import battlecode.client.viewer.*;
import battlecode.common.*;
import battlecode.util.SquareArray;
import battlecode.world.GameMap;
import battlecode.world.signal.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DrawState extends GameState {
    protected static MapLocation origin = null;
    protected final double[] teamHP = new double[4];
    protected final double[] teamResources = new double[4];
    protected final double[][] researchProgress = new double[4][4];
    protected final List<IndicatorDotSignal> newIndicatorDots = new
            ArrayList<>();
    protected final List<IndicatorLineSignal> newIndicatorLines = new
            ArrayList<>();
    protected final int[] teamStrength = new int[4];
    protected final Iterable<Map.Entry<Integer, DrawObject>> drawables =
            new Iterable<Map.Entry<Integer, DrawObject>>() {

                public Iterator<Map.Entry<Integer, DrawObject>> iterator() {
                    return groundUnits.entrySet().iterator();
                }
            };
    protected final Map<Integer, DrawObject> groundUnits;
    protected Map<Team, Map<Integer, DrawObject> > archons = new
            EnumMap<>(Team.class);
    protected int[] coreIDs = new int[4];
    protected SquareArray.Double rubble;
    protected SquareArray.Double parts;
    protected GameMap gameMap;
    protected int currentRound;
    protected IndicatorDotSignal[] indicatorDots = new IndicatorDotSignal[0];
    protected IndicatorLineSignal[] indicatorLines = new IndicatorLineSignal[0];

    protected static final Stroke indicatorLineStroke = new BasicStroke(0.075f);

    protected synchronized void copyStateFrom(DrawState
                                                      src) {
        currentRound = src.currentRound;

        groundUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet
                ()) {
            DrawObject copy = createDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
        }

        archons.get(Team.A).clear();
        archons.get(Team.B).clear();
        for (Map.Entry<Integer, DrawObject> entry : src.archons.get(Team.A)
                .entrySet()) {
            if (groundUnits.containsKey(entry.getKey())) {
                archons.get(Team.A).put(entry.getKey(), groundUnits.get(entry
                        .getKey()));
            } else {
                archons.get(Team.A).put(entry.getKey(), null);
            }
        }
        for (Map.Entry<Integer, DrawObject> entry : src.archons.get(Team.B)
                .entrySet()) {
            if (groundUnits.containsKey(entry.getKey())) {
                archons.get(Team.B).put(entry.getKey(), groundUnits.get(entry
                        .getKey()));
            } else {
                archons.get(Team.B).put(entry.getKey(), null);
            }
        }

        if (src.rubble.width == 0) {
            rubble = new SquareArray.Double(0, 0);
        } else {
            rubble = new SquareArray.Double(src.rubble);
        }

        if (src.parts.width == 0) {
            parts = new SquareArray.Double(0, 0);
        } else {
            parts = new SquareArray.Double(src.parts);

        }

        coreIDs = src.coreIDs;

        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }

        System.arraycopy(src.teamResources, 0, teamResources, 0, teamResources.length);
        for (int t = 0; t < researchProgress.length; t++)
            System.arraycopy(src.researchProgress[t], 0, researchProgress[t], 0, researchProgress[t].length);
        System.arraycopy(src.teamStrength, 0, teamStrength, 0, teamStrength.length);

        indicatorDots = src.indicatorDots;
        indicatorLines = src.indicatorLines;
    }

    public void tryToAddArchon(DrawObject archon) {
        if (archon.getType() == RobotType.ARCHON && archon.getTeam().isPlayer()) {
            archons.get(archon.getTeam()).put(archon.getID(), archon);
        }
    }

    public int[] getRobotCounts(Team t) {
        // naive way for now...
        int[] counts = new int[RobotType.values().length];
        for (Map.Entry<Integer, DrawObject> e : drawables) {
            if (e.getValue().getTeam() == t && e.getValue().getHealth() > 0)
                counts[e.getValue().getType().ordinal()]++;
        }
        return counts;
    }

    public int getTeamStrength(Team t) {
        int sum = 0;
        for (Map.Entry<Integer, DrawObject> e : drawables) {
            if (e.getValue().getTeam() == t && e.getValue().getHealth() > 0) {
                sum += e.getValue().getType().partCost;
            }
        }
        return sum;
    }

    protected Iterable<Map.Entry<Integer, DrawObject>> getDrawableSet() {
        return drawables;
    }

    public double getRubbleAtLocation(int x, int y) {
        return rubble.get(x - origin.x, y - origin.y);
    }

    public double getPartsAtLocation(int x, int y) {
        return parts.get(x - origin.x, y - origin.y);
    }

    protected DrawObject getRobot(int id) {
        DrawObject obj = groundUnits.get(id);
        assert obj != null : "Robot #" + id + " not found";
        return obj;
    }

    protected void putRobot(int id, DrawObject unit) {
        DrawObject previous = groundUnits.put(id, unit);
        assert previous == null : "Robot #" + id + " already exists";
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    protected void preUpdateRound() {
        currentRound++;
    }

    @SuppressWarnings("unused")
    public void visitAttackSignal(AttackSignal s) {
        DrawObject robot = getRobot(s.getRobotID());
        robot.setDirection(robot.getLocation().directionTo(s.getTargetLoc()));
        robot.setAttacking(s.getTargetLoc());
    }

    @SuppressWarnings("unused")
    public void visitBroadcastSignal(BroadcastSignal s) {
        getRobot(s.getRobotID()).setBroadcast();
    }

    @SuppressWarnings("unused")
    public void visitClearRubbleSignal(ClearRubbleSignal s) {
        getRobot(s.getRobotID()).setClearing(s.getLoc());
    }

    @SuppressWarnings("unused")
    public void visitRubbleChangeSignal(RubbleChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        rubble.set(x,y, s.getAmount());
    }

    @SuppressWarnings("unused")
    public void visitPartsChangeSignal(PartsChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        parts.set(x,y, s.getAmount());
    }

    @SuppressWarnings("unused")
    public void visitDeathSignal(DeathSignal s) {
        DrawObject robot = getRobot(s.getObjectID());
        int team = robot.getTeam().ordinal();

        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getHealth();
        }

        robot.destroyUnit();
    }

    @SuppressWarnings("unused")
    public void visitTeamResourceSignal(TeamResourceSignal s) {
        if (s.team == Team.A || s.team == Team.B) {
            teamResources[s.team.ordinal()] = s.resource;
        }
    }

    @SuppressWarnings("unused")
    public void visitIndicatorStringSignal(IndicatorStringSignal s) {
        if (!RenderConfiguration.isTournamentMode()) {
            getRobot(s.getRobotID()).setString(s.getStringIndex(), s
                    .getNewString());
        }
    }

    @SuppressWarnings("unused")
    public void visitIndicatorDotSignal(IndicatorDotSignal s) {
        newIndicatorDots.add(s);
    }

    @SuppressWarnings("unused")
    public void visitIndicatorLineSignal(IndicatorLineSignal s) {
        newIndicatorLines.add(s);
    }

    @SuppressWarnings("unused")
    public void visitControlBitsSignal(ControlBitsSignal s) {
        getRobot(s.getRobotID()).setControlBits(s.getControlBits());

    }

    @SuppressWarnings("unused")
    public void visitMovementOverrideSignal(MovementOverrideSignal s) {
        getRobot(s.getRobotID()).setLocation(s.getNewLoc());
    }

    @SuppressWarnings("unused")
    public void visitMovementSignal(MovementSignal s) {
        DrawObject obj = getRobot(s.getRobotID());
        MapLocation oldloc = obj.getLoc();
        obj.setLocation(s.getNewLoc());
        obj.setDirection(oldloc.directionTo(s.getNewLoc()));
        obj.setMoving(s.getDelay());
    }

    public DrawObject spawnRobot(SpawnSignal s) {
        DrawObject spawn = createDrawObject(s.getType(), s.getTeam(), s
                .getRobotID());
        spawn.setLocation(s.getLoc());
//        spawn.setDirection(s.getDirection());
        spawn.setDirection(Direction.NORTH);
        spawn.setBuildDelay(s.getDelay());
        if (s.getParentID() != SpawnSignal.NO_ID) {
            DrawObject parent = getRobot(s.getParentID());
            parent.setAction(s.getDelay(), ActionType.BUILDING);
        }
        putRobot(s.getRobotID(), spawn);
        tryToAddArchon(spawn);
        int team = getRobot(s.getRobotID()).getTeam().ordinal();
        if (team < 2) {
            teamHP[team] += getRobot(s.getRobotID()).getHealth();
        }

        return spawn;
    }

    @SuppressWarnings("unused")
    public void visitSpawnSignal(SpawnSignal s) {
        spawnRobot(s);
    }

    @SuppressWarnings("unused")
    public void visitBytecodesUsedSignal(BytecodesUsedSignal s) {
        int[] robotIDs = s.getRobotIDs();
        int[] bytecodes = s.getNumBytecodes();
        for (int i = 0; i < robotIDs.length; i++) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) robot.setBytecodesUsed(bytecodes[i]);
        }

    }

    @SuppressWarnings("unused")
    public void visitInfectionSignal(InfectionSignal s) {
        int[] robotIDs = s.getRobotIDs();
        int[] zombieInfectedTurns = s.getZombieInfectedTurns();
        int[] viperInfectedTurns = s.getViperInfectedTurns();
        for (int i = 0; i < robotIDs.length; ++i) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) {
                robot.setZombieInfectedTurns(zombieInfectedTurns[i]);
                robot.setViperInfectedTurns(viperInfectedTurns[i]);
            }
        }
    }

    @SuppressWarnings("unused")
    public void visitHealthChange(HealthChangeSignal s) {
        int[] robotIDs = s.getRobotIDs();
        double[] health = s.getHealth();
        for (int i = 0; i < robotIDs.length; i++) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) {
                robot.setHealth(health[i]);
            }
        }
    }

    @SuppressWarnings("unused")
    public void visitRobotDelaySignal(RobotDelaySignal s) {
        int[] robotIDs = s.getRobotIDs();
        double[] coreDelays = s.getCoreDelays();
        double[] weaponDelays = s.getWeaponDelays();
        for (int i = 0; i < robotIDs.length; i++) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) {
                robot.setMovementDelay(coreDelays[i]);
                robot.setAttackDelay(weaponDelays[i]);
            }
        }
    }

    @SuppressWarnings("unused")
    public void visitTypeChangeSignal(TypeChangeSignal s) {
        int ID = s.getRobotID();
        RobotType newType = s.getType();

        DrawObject robot = getRobot(ID);
        if (robot != null) {
            robot.setType(newType);
        }
    }

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
    // number of blocks in current draw state
    //int[][] blockNumber;

    public DrawState() {
        groundUnits = new LinkedHashMap<>();
        archons.put(Team.A, new HashMap<>());
        archons.put(Team.B, new HashMap<>());
        currentRound = -1;
    }

    private DrawState(GameMap map) {
        this();
        this.setGameMap(map);
    }

    private DrawState(DrawState clone) {
        this();
        copyStateFrom(clone);
    }

    public void setGameMap(GameMap map) {
        gameMap = new GameMap(map);
        Random r = new Random();
        int w = map.getWidth();
        int h = map.getHeight();

        origin = gameMap.getOrigin();

        rubble = new SquareArray.Double(map.getWidth(), map.getHeight());
        for (int i = 0; i < rubble.width; ++i) {
            for (int j = 0; j < rubble.height; ++j) {
                rubble.set(i, j,
                        map.initialRubbleAtLocation(i + origin.x, j + origin.y));
            }
        }

        parts = new SquareArray.Double(map.getWidth(), map.getHeight());
        for (int i = 0; i < parts.width; ++i) {
            for (int j = 0; j < parts.height; ++j) {
                parts.set(i, j,
                        map.initialPartsAtLocation(i + origin.x, j + origin.y));
            }
        }
    }

    protected DrawObject createDrawObject(RobotType type, Team team, int id) {
        return new DrawObject(currentRound, type, team, id, this);
    }

    protected DrawObject createDrawObject(DrawObject o) {
        return new DrawObject(currentRound, o);
    }

    public double getTeamResources(Team t) {
        return teamResources[t.ordinal()];
    }

    public double getResearchProgress(Team t, int i) {
        return researchProgress[t.ordinal()][i];
    }

    public Map<Integer, DrawObject> getArchons(Team t) {
        return archons.get(t);
    }

    public DrawObject getDrawObject(int id) {
        try {
            return getRobot(id);
        } catch (AssertionError e) {
            return null;
        }
    }

    private void drawDragged(Graphics2D g2, DebugState debug, DrawObject obj) {
    }

    protected void postUpdateRound() {
        for (Iterator<Map.Entry<Integer, DrawObject>> it = drawables.iterator();
             it.hasNext(); ) {
            DrawObject obj = it.next().getValue();
            obj.updateRound();
            if (!obj.isAlive()) {
                it.remove();
                if (obj.getType() == RobotType.ARCHON && obj.getTeam()
                        .isPlayer()) {
                    archons.get(obj.getTeam()).put(obj.getID(), null);
                }
            }
        }
        indicatorDots = newIndicatorDots.toArray(new
                IndicatorDotSignal[newIndicatorDots.size()]);
        indicatorLines = newIndicatorLines.toArray(new
                IndicatorLineSignal[newIndicatorLines.size()]);
        newIndicatorDots.clear();
        newIndicatorLines.clear();
    }

    /**
     * Draws the current game state. This method is always called from the
     * Swing event-dispatch thread, and in particular blocks calls to
     * updateRound.
     *
     * @param g2    The graphics context, transformed to MapLocation-space
     * @param debug The debug state, including MapLocation-space mouse state
     */
    public synchronized void draw(Graphics2D g2, DebugState debug) {
        int dragID = debug.getDragID();
        int focusID = debug.getFocusID();
        int hoverID = -1;
        MapLocation hoverLoc = null;
        long controlBits = 0;
        Iterable<Map.Entry<Integer, DrawObject>> drawableSet = getDrawableSet();


        if (drawableSet == null) {
            return;
        }

        AffineTransform pushed = g2.getTransform();
        g2.setTransform(pushed);

        // draw rubble and parts
        for (int i = 0; i < gameMap.getWidth(); ++i) {
            for (int j = 0; j < gameMap.getHeight(); ++j) {
                int x = i + gameMap.getOrigin().x;
                int y = j + gameMap.getOrigin().y;

                double r = rubble.get(i, j);

                // fill a tile with alpha based on how much rubble there is

                if (r > 0) {
                    g2.setColor(getRubbleColor(r));
                    g2.fillRect(x, y, 1, 1);
                }

                if (RenderConfiguration.showParts()) {
                    // draw a dot with radius depending on how many parts there are
                    if (parts.get(i, j) > 0) {
                        double radius = Math.max(0.2, Math.min(1.0, parts.get(i,
                                j) / 100) * 0.4);
                        BufferedImage parts = GameRenderer.pg.getPartsImage
                                (radius);
                        AffineTransform trans = new AffineTransform();
                        trans.translate(x, y);
                        trans.scale(1.0 / parts.getWidth(), 1.0 /
                                parts.getHeight());
                        g2.drawImage(parts, trans, null);
                    }
                }
            }
        }

        if (gameMap.isArmageddon()) {
            boolean isDay = (currentRound % (GameConstants.ARMAGEDDON_DAY_TIMER + GameConstants.ARMAGEDDON_NIGHT_TIMER)) < GameConstants.ARMAGEDDON_DAY_TIMER;
            g2.setColor(new Color(0, 0, 0, isDay ? 0 : 100));
            g2.fillRect(gameMap.getOrigin().x, gameMap.getOrigin().y, gameMap.getWidth(), gameMap.getHeight());
        }
        
        for (IndicatorDotSignal s : indicatorDots) {
            if (RenderConfiguration.showIndicatorDots(s.team) && (focusID ==
                    -1 || focusID == s.robotID)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.fill(new Ellipse2D.Double(s.location.x + .1, s.location.y
                        + .1, .8, .8));
            }
        }
        g2.setStroke(indicatorLineStroke);
        for (IndicatorLineSignal s : indicatorLines) {
            if (RenderConfiguration.showIndicatorDots(s.team)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.draw(new Line2D.Double(s.loc1.x + .5, s.loc1.y + .5, s
                        .loc2.x + .5, s.loc2.y + .5));
            }
        }
        for (IndicatorDotSignal s : indicatorDots) {
            if (RenderConfiguration.showIndicatorDots(s.team) && (focusID ==
                    -1 || focusID == s.robotID)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.fill(new Ellipse2D.Double(s.location.x + .1, s.location.y + .1, .8, .8));
            }
        }

        for (int layer = 0; layer < DrawObject.LAYER_COUNT; layer++) {
            for (Map.Entry<Integer, DrawObject> entry : drawableSet) {

                int id = entry.getKey();
                DrawObject obj = entry.getValue();
                if (obj.inTransport()) continue;

                if (id == dragID) {
                    drawDragged(g2, debug, obj);
                } else {
                    if (Math.abs(debug.getX() - obj.getDrawX() - 0.5) < 0.5
                            && Math.abs(debug.getY() - obj.getDrawY() - 0.5) < 0.5) {
                        hoverID = id;
                        hoverLoc = obj.getLocation();
                        controlBits = obj.getControlBits();
                    }
                    obj.draw(g2, id == focusID || id == hoverID,
                            obj.getLocation().y == origin.y + gameMap.getHeight() - 1,
                            layer);
                }
            }
        }

        if (!debug.isDragging()) {
            debug.setTarget(hoverID, hoverLoc, controlBits);
        }
        

    }

    // woo, hardcoded constants!
    // these are some lookup tables for colors
    private static final Color[] NO_SLOWS = new Color[10];
    private static final Color[] SLOWS = new Color[10];
    private static final Color[] OBSTRUCTS = new Color[100];
    static {
        for (int r = 0; r < 50; r += 5) {
            // for rubble from 0 to 49.99
            // bucketed in 5s
            NO_SLOWS[r/5] = new Color(0,0,0, r/50f * .2f);
        }
        for (int r = 50; r < 100; r += 5) {
            // for rubble from 50 to 99.99
            // bucketed in 5s
            SLOWS[r/5-10] = new Color(0,0,.2f, .3f + (r - 50f)/50f * .2f);
        }

        // lower than 4 won't be used
        for (int i = 4; i < 100; i++) {
            // bucketed by floored cube root
            // rubble amount in this bucket:
            final double r = i * i * i;

            OBSTRUCTS[i] = new Color(0,0,0, .4f + .6f * (float)(Math.log(r)/Math.log(1_000_001)));
        }
    }

    /**
     * @param r the amount of rubble in a tile
     * @return the color to be overlayed over the tile
     */
    public static Color getRubbleColor(double r) {
        if (r < 50) {
            return NO_SLOWS[(int)Math.floor(r) / 5];
        } else if (r < 100) {
            return SLOWS[(int)Math.floor(r) / 5 - 10];
        } else if (r < 1_000_000) {
            return OBSTRUCTS[(int)Math.floor(Math.cbrt(r))];
        } else {
            return Color.BLACK;
        }
    }
}