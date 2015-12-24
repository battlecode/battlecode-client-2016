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
    protected double[] teamHP = new double[4];
    protected int[] coreIDs = new int[4];
    protected double[][] rubble = new double[0][0];
    protected double[][] parts = new double[0][0];
    protected static MapLocation origin = null;
    protected GameMap gameMap;
    protected int currentRound;
    protected double[] teamResources = new double[4];
    protected double[][] researchProgress = new double[4][4];
    protected List<IndicatorDotSignal> newIndicatorDots = new
            ArrayList<IndicatorDotSignal>();
    protected List<IndicatorLineSignal> newIndicatorLines = new
            ArrayList<IndicatorLineSignal>();
    protected IndicatorDotSignal[] indicatorDots = new IndicatorDotSignal[0];
    protected IndicatorLineSignal[] indicatorLines = new IndicatorLineSignal[0];
    protected int[] teamStrength = new int[4];

    protected Iterable<Map.Entry<Integer, DrawObject>> drawables =
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
                for (int j = 0; j < rubble[i].length; ++j) {
                    rubble[i][j] = src.rubble[i][j];
                }
            }
        }

        if (src.parts.length == 0) {
            parts = new double[0][0];
        } else {
            parts = new double[src.parts.length][src.parts[0].length];
            for (int i = 0; i < parts.length; ++i) {
                for (int j = 0; j < parts[i].length; ++j) {
                    parts[i][j] = src.parts[i][j];
                }
            }
        }

        coreIDs = src.coreIDs;

        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }

        for (int x = 0; x < teamResources.length; x++)
            teamResources[x] = src.teamResources[x];
        for (int t = 0; t < researchProgress.length; t++)
            for (int r = 0; r < researchProgress[t].length; r++)
                researchProgress[t][r] = src.researchProgress[t][r];
        for (int x = 0; x < teamStrength.length; x++)
            teamStrength[x] = src.teamStrength[x];

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

    protected void removeRobot(int id) {
        DrawObject previous = groundUnits.remove(id);
        assert previous != null : "Robot #" + id + " not found";
    }

    protected void putRobot(int id, DrawObject unit) {
        DrawObject previous = groundUnits.put(id, unit);
        assert previous == null : "Robot #" + id + " already exists";
    }

    public int getCurrentRound() {
        return currentRound;
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

    public void visitAttackSignal(AttackSignal s) {
        DrawObject robot = getRobot(s.getRobotID());
        robot.setDirection(robot.getLocation().directionTo(s.getTargetLoc()));
        robot.setAttacking(s.getTargetLoc());
    }

    public void visitBroadcastSignal(BroadcastSignal s) {
        getRobot(s.getRobotID()).setBroadcast();
    }

    public void visitClearRubbleSignal(ClearRubbleSignal s) {
        // TODO: put an animation here
    }

    public void visitRubbleChangeSignal(RubbleChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        rubble[x][y] = s.getAmount();
    }

    public void visitPartsChangeSignal(PartsChangeSignal s) {
        int x = s.getLoc().x - origin.x;
        int y = s.getLoc().y - origin.y;
        rubble[x][y] = s.getAmount();
    }

    public void visitDeathSignal(DeathSignal s) {
        DrawObject robot = getRobot(s.getObjectID());
        int team = robot.getTeam().ordinal();

        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getEnergon();
        }

        robot.destroyUnit();
    }

    public void visitTeamResourceSignal(TeamResourceSignal s) {
        if (s.team == Team.A || s.team == Team.B) {
            teamResources[s.team.ordinal()] = s.resource;
        }
    }

    public void visitIndicatorStringSignal(IndicatorStringSignal s) {
        if (!RenderConfiguration.isTournamentMode()) {
            getRobot(s.getRobotID()).setString(s.getStringIndex(), s
                    .getNewString());
        }
    }

    public void visitIndicatorDotSignal(IndicatorDotSignal s) {
        newIndicatorDots.add(s);
    }

    public void visitIndicatorLineSignal(IndicatorLineSignal s) {
        newIndicatorLines.add(s);
    }

    public void visitControlBitsSignal(ControlBitsSignal s) {
        getRobot(s.getRobotID()).setControlBits(s.getControlBits());

    }

    public void visitMovementOverrideSignal(MovementOverrideSignal s) {
        getRobot(s.getRobotID()).setLocation(s.getNewLoc());
    }

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

    public void visitSpawnSignal(SpawnSignal s) {
        spawnRobot(s);
    }

    public void visitBytecodesUsedSignal(BytecodesUsedSignal s) {
        int[] robotIDs = s.getRobotIDs();
        int[] bytecodes = s.getNumBytecodes();
        for (int i = 0; i < robotIDs.length; i++) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) robot.setBytecodesUsed(bytecodes[i]);
        }

    }

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
    public void visitTypeChangeSignal(TypeChangeSignal s) {
        int ID = s.getRobotID();
        RobotType newType = s.getType();

        DrawObject robot = getRobot(ID);
        if (robot != null) {
            robot.setType(newType);
        }
    }
}

