package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.world.GameMap;
import battlecode.world.signal.*;

import java.util.*;

public abstract class AbstractDrawState<DrawObject extends
        AbstractDrawObject> extends GameState {

    protected abstract DrawObject createDrawObject(RobotType type, Team team,
                                                   int id);

    protected abstract DrawObject createDrawObject(DrawObject o);

    protected Map<Integer, DrawObject> groundUnits;
    protected final double[] teamHP = new double[4];
    protected int[] coreIDs = new int[4];
    protected double[][] rubble = new double[0][0];
    protected double[][] parts = new double[0][0];
    protected static MapLocation origin = null;
    protected GameMap gameMap;
    protected int currentRound;
    protected final double[] teamResources = new double[4];
    protected final double[][] researchProgress = new double[4][4];
    protected final List<IndicatorDotSignal> newIndicatorDots = new
            ArrayList<>();
    protected final List<IndicatorLineSignal> newIndicatorLines = new
            ArrayList<>();
    protected IndicatorDotSignal[] indicatorDots = new IndicatorDotSignal[0];
    protected IndicatorLineSignal[] indicatorLines = new IndicatorLineSignal[0];
    protected final int[] teamStrength = new int[4];

    protected final Iterable<Map.Entry<Integer, DrawObject>> drawables =
            new Iterable<Map.Entry<Integer, DrawObject>>() {

                public Iterator<Map.Entry<Integer, DrawObject>> iterator() {
                    return groundUnits.entrySet().iterator();
                }
            };

    public AbstractDrawState() {
    }

    protected synchronized void copyStateFrom(AbstractDrawState<DrawObject>
                                                      src) {
        currentRound = src.currentRound;

        groundUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet
                ()) {
            DrawObject copy = createDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
        }

        if (src.rubble.length == 0) {
            rubble = new double[0][0];
        } else {
            rubble = new double[src.rubble.length][src.rubble[0].length];
            for (int i = 0; i < rubble.length; ++i) {
                System.arraycopy(src.rubble[i], 0, rubble[i], 0, rubble[i].length);
            }
        }

        if (src.parts.length == 0) {
            parts = new double[0][0];
        } else {
            parts = new double[src.parts.length][src.parts[0].length];
            for (int i = 0; i < parts.length; ++i) {
                System.arraycopy(src.parts[i], 0, parts[i], 0, parts[i].length);
            }
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

    public int[] getRobotCounts(Team t) {
        // naive way for now...
        int[] counts = new int[RobotType.values().length];
        for (Map.Entry<Integer, DrawObject> e : drawables) {
            if (e.getValue().getTeam() == t)
                counts[e.getValue().getType().ordinal()]++;
        }
        return counts;
    }

    public int getTeamStrength(Team t) {
        int sum = 0;
        for (Map.Entry<Integer, DrawObject> e : drawables) {
            if (e.getValue().getTeam() == t)
                sum += e.getValue().getType().strengthWeight;
        }
        return sum;
    }

    protected Iterable<Map.Entry<Integer, DrawObject>> getDrawableSet() {
        return drawables;
    }

    protected double getRubbleAtLocation(int x, int y) {
        return rubble[x - origin.x][y - origin.y];
    }

    protected double getPartsAtLocation(int x, int y) {
        return parts[x - origin.x][y - origin.y];
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

    public void setGameMap(GameMap map) {
        gameMap = new GameMap(map);
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    protected void preUpdateRound() {
        currentRound++;
    }

    protected void postUpdateRound() {
        for (Iterator<Map.Entry<Integer, DrawObject>> it = drawables.iterator();
             it.hasNext(); ) {
            DrawObject obj = it.next().getValue();
            obj.updateRound();
            if (!obj.isAlive()) {
                it.remove();
            }
        }
        indicatorDots = newIndicatorDots.toArray(new
                IndicatorDotSignal[newIndicatorDots.size()]);
        indicatorLines = newIndicatorLines.toArray(new
                IndicatorLineSignal[newIndicatorLines.size()]);
        newIndicatorDots.clear();
        newIndicatorLines.clear();
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
        // TODO: put an animation here
    }

    @SuppressWarnings("unused")
    public void visitRubbleChangeSignal(RubbleChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        rubble[x][y] = s.getAmount();
    }

    @SuppressWarnings("unused")
    public void visitPartsChangeSignal(PartsChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        rubble[x][y] = s.getAmount();
    }

    @SuppressWarnings("unused")
    public void visitDeathSignal(DeathSignal s) {
        DrawObject robot = getRobot(s.getObjectID());
        int team = robot.getTeam().ordinal();

        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getEnergon();
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
        MapLocation oldloc = obj.loc;
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
        int team = getRobot(s.getRobotID()).getTeam().ordinal();
        if (team < 2) {
            teamHP[team] += getRobot(s.getRobotID()).getEnergon();
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
                robot.setEnergon(health[i]);
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
}

