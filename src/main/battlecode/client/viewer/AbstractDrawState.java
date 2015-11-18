package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.serial.RoundStats;
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
    protected Map<MapLocation, Double> locationOre = new HashMap<MapLocation,
            Double>();
    protected static MapLocation origin = null;
    protected GameMap gameMap;
    protected int currentRound;
    protected RoundStats stats = null;
    protected double[] teamResources = new double[4];
    protected double[][] researchProgress = new double[4][4];
    protected List<IndicatorDotSignal> newIndicatorDots = new
            ArrayList<IndicatorDotSignal>();
    protected List<IndicatorLineSignal> newIndicatorLines = new
            ArrayList<IndicatorLineSignal>();
    protected IndicatorDotSignal[] indicatorDots = new IndicatorDotSignal[0];
    protected IndicatorLineSignal[] indicatorLines = new IndicatorLineSignal[0];
    protected Map<Team, Map<RobotType, Integer>> totalRobotTypeCount = new
            EnumMap<Team, Map<RobotType, Integer>>(Team.class); // includes
            // inactive buildings
    protected Map<Team, ArrayList<RobotType>> buildingArray = new
                    EnumMap<Team, ArrayList<RobotType>>(Team.class);
    protected Map<Team, ArrayList<RobotType>> unitArray = new EnumMap<Team,
            ArrayList<RobotType>>(Team.class);
    protected int[] teamStrength = new int[4];


    protected Iterable<Map.Entry<Integer, DrawObject>> drawables =
            new Iterable<Map.Entry<Integer, DrawObject>>() {

                public Iterator<Map.Entry<Integer, DrawObject>> iterator() {
                    return groundUnits.entrySet().iterator();
                }
            };

    public AbstractDrawState() {
        totalRobotTypeCount.put(Team.A, new EnumMap<RobotType, Integer>
                (RobotType.class));
        totalRobotTypeCount.put(Team.B, new EnumMap<RobotType, Integer>
                (RobotType.class));
        totalRobotTypeCount.put(Team.NEUTRAL, new EnumMap<RobotType, Integer>
                (RobotType.class));
        totalRobotTypeCount.put(Team.ZOMBIE, new EnumMap<RobotType, Integer>
                (RobotType.class));
        buildingArray.put(Team.A, new ArrayList<RobotType>());
        buildingArray.put(Team.B, new ArrayList<RobotType>());
        buildingArray.put(Team.NEUTRAL, new ArrayList<RobotType>());
        buildingArray.put(Team.ZOMBIE, new ArrayList<RobotType>());
        unitArray.put(Team.A, new ArrayList<RobotType>());
        unitArray.put(Team.B, new ArrayList<RobotType>());
        unitArray.put(Team.NEUTRAL, new ArrayList<RobotType>());
        unitArray.put(Team.ZOMBIE, new ArrayList<RobotType>());
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

        locationOre.clear();
        locationOre.putAll(src.locationOre);

        coreIDs = src.coreIDs;
        stats = src.stats;

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

        totalRobotTypeCount.put(Team.A, new EnumMap<RobotType, Integer>(src
                .totalRobotTypeCount.get(Team.A)));
        totalRobotTypeCount.put(Team.B, new EnumMap<RobotType, Integer>(src
                .totalRobotTypeCount.get(Team.B)));
        buildingArray.put(Team.A, new ArrayList<RobotType>(src.buildingArray
                .get(Team.A)));
        buildingArray.put(Team.B, new ArrayList<RobotType>(src.buildingArray
                .get(Team.B)));
        unitArray.put(Team.A, new ArrayList<RobotType>(src.unitArray.get(Team
                .A)));
        unitArray.put(Team.B, new ArrayList<RobotType>(src.unitArray.get(Team
                .B)));
    }

    public int getTeamStrength(Team t) {
        return teamStrength[t.ordinal()];
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

    public void incrementRobotTypeCount(Team team, RobotType type) {
        if (totalRobotTypeCount.get(team).containsKey(type)) {
            totalRobotTypeCount.get(team).put(type, totalRobotTypeCount.get
                    (team).get(type) + 1);
        } else {
            totalRobotTypeCount.get(team).put(type, 1);
            if (type.isBuilding) {
                buildingArray.get(team).add(type);
            } else {
                unitArray.get(team).add(type);
            }
        }
        teamStrength[team.ordinal()] += type.strengthWeight;
    }

    public void decrementRobotTypeCount(Team team, RobotType type) {
        totalRobotTypeCount.get(team).put(type, totalRobotTypeCount.get(team)
                .get(type) - 1);
        teamStrength[team.ordinal()] -= type.strengthWeight;
    }

    public int getRobotTypeCount(Team team, RobotType type) {
        if (totalRobotTypeCount.get(team).containsKey(type)) {
            return totalRobotTypeCount.get(team).get(type);
        } else {
            return 0;
        }
    }

    public ArrayList<RobotType> getAppearedUnitTypes(Team team) {
        return new ArrayList<>(unitArray.get(team));
    }

    public ArrayList<RobotType> getAppearedBuildingTypes(Team team) {
        return new ArrayList<>(buildingArray.get(team));
    }

    public DrawObject getPowerCore(Team t) {
        int id = coreIDs[t.ordinal()];
        if (id != 0)
            return getRobot(id);
        else
            return null;
    }

    protected Iterable<Map.Entry<Integer, DrawObject>> getDrawableSet() {
        return drawables;
    }

    protected double getOreAtLocation(MapLocation loc) {
        if (locationOre.containsKey(loc)) {
            return locationOre.get(loc);
        } else {
            return 0.0;
        }
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

    public RoundStats getRoundStats() {
        return stats;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setGameMap(GameMap map) {
        gameMap = new GameMap(map);
        origin = gameMap.getMapOrigin();
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

    public void visitBashSignal(BashSignal s) {
        DrawObject robot = getRobot(s.robotID);
        robot.setAttacking(robot.getLocation());
    }

    public void visitBroadcastSignal(BroadcastSignal s) {
        getRobot(s.getRobotID()).setBroadcast();
    }

    public void visitDeathSignal(DeathSignal s) {
        DrawObject robot = getRobot(s.getObjectID());
        int team = robot.getTeam().ordinal();

        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getEnergon();
        }
        decrementRobotTypeCount(robot.getTeam(), robot.getRobotType());
        robot.destroyUnit();
    }

    public void visitTeamOreSignal(TeamOreSignal s) {
        if (s.team == Team.A || s.team == Team.B) {
            teamResources[s.team.ordinal()] = s.ore;
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
        obj.setMoving(s.getMovingForward(), s.getDelay());
    }

    public void visitCastSignal(CastSignal s) {
        //TODO(npinsker): update this with various spells

        DrawObject obj = getRobot(s.getRobotID());
        MapLocation oldloc = obj.loc;
        obj.setLocation(s.getTargetLoc());
        obj.setDirection(oldloc.directionTo(s.getTargetLoc()));
    }

    public DrawObject spawnRobot(SpawnSignal s) {
        DrawObject spawn = createDrawObject(s.getType(), s.getTeam(), s
                .getRobotID());
        spawn.setLocation(s.getLoc());
//        spawn.setDirection(s.getDirection());
        spawn.setDirection(Direction.NORTH);
        spawn.setBuildDelay(s.getDelay());
        if (s.getParentID() != 0) {
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
        incrementRobotTypeCount(s.getTeam(), s.getType());
    }

    public void visitBytecodesUsedSignal(BytecodesUsedSignal s) {
        int[] robotIDs = s.getRobotIDs();
        int[] bytecodes = s.getNumBytecodes();
        for (int i = 0; i < robotIDs.length; i++) {
            DrawObject robot = getRobot(robotIDs[i]);
            if (robot != null) robot.setBytecodesUsed(bytecodes[i]);
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

    public void visitRobotInfoSignal(RobotDelaySignal s) {
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

    public void visitXPSignal(XPSignal s) {
        getRobot(s.getRobotID()).setXP(s.getXP());
    }

    public void visitLocationOreChangeSignal(LocationOreChangeSignal s) {
        locationOre.put(s.getLoc(), s.getOre());
    }
}

