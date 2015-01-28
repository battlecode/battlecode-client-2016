package battlecode.client.viewer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
import battlecode.world.signal.AttackSignal;
import battlecode.world.signal.BashSignal;
import battlecode.world.signal.BroadcastSignal;
import battlecode.world.signal.BytecodesUsedSignal;
import battlecode.world.signal.CastSignal;
import battlecode.world.signal.ControlBitsSignal;
import battlecode.world.signal.DeathSignal;
import battlecode.world.signal.HealthChangeSignal;
import battlecode.world.signal.IndicatorDotSignal;
import battlecode.world.signal.IndicatorLineSignal;
import battlecode.world.signal.IndicatorStringSignal;
import battlecode.world.signal.LocationOreChangeSignal;
import battlecode.world.signal.MineSignal;
import battlecode.world.signal.MissileCountSignal;
import battlecode.world.signal.MovementOverrideSignal;
import battlecode.world.signal.MovementSignal;
import battlecode.world.signal.RobotInfoSignal;
import battlecode.world.signal.SelfDestructSignal;
import battlecode.world.signal.SpawnSignal;
import battlecode.world.signal.TeamOreSignal;
import battlecode.world.signal.TransferSupplySignal;
import battlecode.world.signal.XPSignal;

public abstract class AbstractDrawState<DrawObject extends AbstractDrawObject> extends GameState {

  protected abstract DrawObject createDrawObject(RobotType type, Team team, int id);

  protected abstract DrawObject createDrawObject(DrawObject o);
  protected Map<Integer, DrawObject> groundUnits;
  protected Map<Integer, DrawObject> airUnits;
  protected Map<Integer, FluxDepositState> fluxDeposits;
  protected Set<MapLocation> encampments;
  protected double[] teamHP = new double[2];
  protected Map<Team, DrawObject> hqs;
  protected Map<Team, Map<Integer, DrawObject>> towers
  = new EnumMap<Team, Map<Integer, DrawObject>>(Team.class); // includes dead towers
  protected Map<Team, DrawObject> commanders;
  protected int [] coreIDs = new int [2];
  protected Map<MapLocation,Team> mineLocs = new HashMap<MapLocation, Team>();
  protected Map<MapLocation, Double> locationOre = new HashMap<MapLocation, Double>();
  protected static MapLocation origin = null;
  protected GameMap gameMap;
  protected int currentRound;
  protected RoundStats stats = null;
  protected double[] teamResources = new double[2];
  protected double[][] researchProgress = new double[2][4];
  protected List<IndicatorDotSignal> newIndicatorDots = new ArrayList<IndicatorDotSignal>();
  protected List<IndicatorLineSignal> newIndicatorLines = new ArrayList<IndicatorLineSignal>();
  protected IndicatorDotSignal [] indicatorDots = new IndicatorDotSignal [0];
  protected IndicatorLineSignal [] indicatorLines = new IndicatorLineSignal [0];
  protected Map<Team, Map<RobotType, Integer>> totalRobotTypeCount = new EnumMap<Team, Map<RobotType, Integer>>(Team.class); // includes inactive buildings
  protected Map<Team, ArrayList<RobotType>> buildingArray = new EnumMap<Team, ArrayList<RobotType>>(Team.class);
  protected Map<Team, ArrayList<RobotType>> unitArray = new EnumMap<Team, ArrayList<RobotType>>(Team.class);
  protected int [] teamStrength = new int[2];
  
  
  protected Iterable<Map.Entry<Integer, DrawObject>> drawables =
    new Iterable<Map.Entry<Integer, DrawObject>>() {

    public Iterator<Map.Entry<Integer, DrawObject>> iterator() {
      return new UnitIterator();
    }
  };

  private class UnitIterator implements Iterator<Map.Entry<Integer, DrawObject>> {

    private Iterator<Map.Entry<Integer, DrawObject>> it =
      groundUnits.entrySet().iterator();
    private boolean ground = true;

    public boolean hasNext() {
      return it.hasNext() || (ground && !airUnits.isEmpty());
    }

    public Map.Entry<Integer, DrawObject> next() {
      if (!it.hasNext() && ground) {
        ground = false;
        it = airUnits.entrySet().iterator();
      }
      return it.next();
    }

    public void remove() {
      it.remove();
    }
  };

  protected class Link {
    public MapLocation from;
    public MapLocation to;
    public boolean [] connected;

    public Link(MapLocation from, MapLocation to) {
      this.from = from;
      this.to = to;
      connected = new boolean [2];
    }

    public Link(Link l) {
      this.from = l.from;
      this.to = l.to;
      this.connected = new boolean [2];
      System.arraycopy(l.connected,0,this.connected,0,2);
    }
  }

  private Map<MapLocation, List<MapLocation>> neighbors = null;
  private Map<MapLocation, Team> nodeTeams = new HashMap<MapLocation,Team>();
  protected List<Link> links = new ArrayList<Link>();


  public AbstractDrawState() {
    hqs = new EnumMap<Team, DrawObject>(Team.class);
    commanders = new EnumMap<Team, DrawObject>(Team.class);
    towers.put(Team.A, new HashMap<Integer, DrawObject>());
    towers.put(Team.B, new HashMap<Integer, DrawObject>());
    totalRobotTypeCount.put(Team.A, new EnumMap<RobotType, Integer>(RobotType.class));
    totalRobotTypeCount.put(Team.B, new EnumMap<RobotType, Integer>(RobotType.class));
    buildingArray.put(Team.A, new ArrayList<RobotType>());
    buildingArray.put(Team.B, new ArrayList<RobotType>());
    unitArray.put(Team.A, new ArrayList<RobotType>());
    unitArray.put(Team.B, new ArrayList<RobotType>());    
  }

  protected synchronized void copyStateFrom(AbstractDrawState<DrawObject> src) {
      currentRound = src.currentRound;
      
      groundUnits.clear();
      for(Map<Integer, DrawObject> towerMap : towers.values()) {
	  for(Integer id : towerMap.keySet()) {
	      towerMap.put(id, null);
	  }
      }
      for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet()) {
        DrawObject copy = createDrawObject(entry.getValue());
        groundUnits.put(entry.getKey(), copy);
        tryAddHQ(copy);
	tryAddTower(copy);
      }
      airUnits.clear();
      for (Map.Entry<Integer, DrawObject> entry : src.airUnits.entrySet()) {
        DrawObject copy = createDrawObject(entry.getValue());
        airUnits.put(entry.getKey(), copy);
      }

      

      mineLocs.clear();
      mineLocs.putAll(src.mineLocs);
        
      locationOre.clear();
      locationOre.putAll(src.locationOre);
        
      fluxDeposits.clear();
      for (Map.Entry<Integer, FluxDepositState> entry : src.fluxDeposits.entrySet()) {
        fluxDeposits.put(entry.getKey(), new FluxDepositState(entry.getValue()));
      }
      coreIDs = src.coreIDs;
      stats = src.stats;

      nodeTeams = new HashMap<MapLocation,Team>(src.nodeTeams);
	
      neighbors = src.neighbors;

      links.clear();
      for(Link l : src.links) {
        links.add(new Link(l));
      }
	
      if (src.gameMap != null) {
        gameMap = src.gameMap;
      }

      for (int x=0; x<teamResources.length; x++)
        teamResources[x] = src.teamResources[x];
      for (int t = 0; t < researchProgress.length; t++)
        for (int r = 0; r < researchProgress[t].length; r++)
          researchProgress[t][r] = src.researchProgress[t][r];
      for (int x=0; x<teamStrength.length; x++)
      	teamStrength[x] = src.teamStrength[x];

      indicatorDots = src.indicatorDots;
      indicatorLines = src.indicatorLines;
      
      totalRobotTypeCount.put(Team.A, new EnumMap<RobotType, Integer>(src.totalRobotTypeCount.get(Team.A)));
      totalRobotTypeCount.put(Team.B, new EnumMap<RobotType, Integer>(src.totalRobotTypeCount.get(Team.B)));
      buildingArray.put(Team.A, new ArrayList<RobotType>(src.buildingArray.get(Team.A)));
      buildingArray.put(Team.B, new ArrayList<RobotType>(src.buildingArray.get(Team.B)));
      unitArray.put(Team.A, new ArrayList<RobotType>(src.unitArray.get(Team.A)));
      unitArray.put(Team.B, new ArrayList<RobotType>(src.unitArray.get(Team.B)));
    }

  public DrawObject getHQ(Team t) {
    return hqs.get(t);
  }
  
  public DrawObject getCommander(Team t){
  	return commanders.get(t);
  }
	
    public Map<Integer, DrawObject> getTowers(Team t) {
	return towers.get(t);
    }
    
  public int getTeamStrength(Team t){
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
  	if (type != RobotType.TOWER && type != RobotType.HQ)
  		if (totalRobotTypeCount.get(team).containsKey(type)) {
  			totalRobotTypeCount.get(team).put(type, totalRobotTypeCount.get(team).get(type) + 1);
  		} else {
  			totalRobotTypeCount.get(team).put(type, 1);
  			if (type.isBuilding){
  				buildingArray.get(team).add(type);
  			}else{
  				unitArray.get(team).add(type);
  			}
  		}
  	teamStrength[team.ordinal()] += type.strengthWeight;
  }
  
  public void decrementRobotTypeCount(Team team, RobotType type){
  	if (type != RobotType.TOWER && type != RobotType.HQ)
  		totalRobotTypeCount.get(team).put(type, totalRobotTypeCount.get(team).get(type) - 1);
  	teamStrength[team.ordinal()] -= type.strengthWeight;
  }
  
  public int getRobotTypeCount(Team team, RobotType type){
  	if (totalRobotTypeCount.get(team).containsKey(type)) {
  		return totalRobotTypeCount.get(team).get(type);
  	} else {
  		return 0;
  	}
  }
  
  public ArrayList<RobotType> getAppearedUnitTypes(Team team){
  	return new ArrayList<>(unitArray.get(team));
  }

  public ArrayList<RobotType> getAppearedBuildingTypes(Team team){
  	return new ArrayList<>(buildingArray.get(team));
  }
  
  public DrawObject getPowerCore(Team t) {
    int id = coreIDs[t.ordinal()];
    if(id!=0)
      return getRobot(id);
    else
      return null;
  }
	
  public Set<MapLocation> getEncampmentLocations() {
    return encampments;
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
    if (obj == null) {
      obj = airUnits.get(id);
      assert obj != null : "Robot #" + id + " not found";
    }
    return obj;
  }

  protected void removeRobot(int id) {
    DrawObject previous = groundUnits.remove(id);
    if (previous == null) {
      previous = airUnits.remove(id);
      assert previous != null : "Robot #" + id + " not found";
    }
  }

  protected void putRobot(int id, DrawObject unit) {
    DrawObject previous = groundUnits.put(id, unit);
    assert previous == null : "Robot #" + id + " already exists";
  }

  protected void tryAddHQ(DrawObject hq) {
    if (hq.getType() == RobotType.HQ)
      hqs.put(hq.getTeam(),hq);
  }
  
  protected void tryAddTower(DrawObject t) {
      if (t.getType() == RobotType.TOWER) {
	  towers.get(t.getTeam()).put(t.getID(), t);
      }
  }
  
  protected void tryAddCommander(DrawObject t){
  	if(t.getType() == RobotType.COMMANDER){
  		commanders.put(t.getTeam(), t);
  	}
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
         it.hasNext();) {
      DrawObject obj = it.next().getValue();
      obj.updateRound();
      if (!obj.isAlive()) {
	  it.remove();
	  if(obj.getType() == RobotType.TOWER) {
	      towers.get(obj.getTeam()).put(obj.getID(), null);
	  }
        //if (obj.getType() == RobotType.ARCHON) {
        //	(obj.getTeam() == Team.A ? archonsA : archonsB).remove(obj);
        //}
      }
      //if (obj.getType() == RobotType.WOUT) {
      //	mineFlux(obj);
      //}
    }
    indicatorDots = newIndicatorDots.toArray(new IndicatorDotSignal [newIndicatorDots.size()]);
    indicatorLines = newIndicatorLines.toArray(new IndicatorLineSignal [newIndicatorLines.size()]);
    newIndicatorDots.clear();
    newIndicatorLines.clear();
  }

  public void visitAttackSignal(AttackSignal s) {
    DrawObject robot = getRobot(s.getRobotID());
    robot.setDirection(robot.getLocation().directionTo(s.getTargetLoc()));
    robot.setAttacking(s.getTargetLoc());

  }
    
    public void visitBashSignal(BashSignal s) {
	DrawObject robot = getRobot(s.getRobotID());
	robot.setAttacking(robot.getLocation());
    }

  public void visitBroadcastSignal(BroadcastSignal s) {
    getRobot(s.getRobotID()).setBroadcast();
  }

  public void visitSelfDestructSignal(SelfDestructSignal s) {
    DrawObject robot = getRobot(s.getRobotID());
    robot.setSuiciding(true);
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
    for (int x=0; x<teamResources.length; x++)
      teamResources[x] = s.ore[x];
  }

  public void visitTransferSupplySignal(TransferSupplySignal s) {
    DrawObject from = getRobot(s.fromID);
    DrawObject to = getRobot(s.toID);
    from.setSupplyTransfer(to,s.amount);
  }

  public void visitIndicatorStringSignal(IndicatorStringSignal s) {
    if (!RenderConfiguration.isTournamentMode()) {
      getRobot(s.getRobotID()).setString(s.getStringIndex(), s.getNewString());
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
    obj.setMoving(s.isMovingForward(), s.getDelay());
  }

  public void visitCastSignal(CastSignal s) {
    //TODO(npinsker): update this with various spells
    
    DrawObject obj = getRobot(s.getRobotID());
    MapLocation oldloc = obj.loc;
    obj.setLocation(s.getTargetLoc());
    obj.setDirection(oldloc.directionTo(s.getTargetLoc()));
  }

    public void visitMineSignal(MineSignal s) {
	return;
    }

  public DrawObject spawnRobot(SpawnSignal s) {
    DrawObject spawn = createDrawObject(s.getType(), s.getTeam(), s.getRobotID());
    spawn.setLocation(s.getLoc());
//        spawn.setDirection(s.getDirection());
    spawn.setDirection(Direction.NORTH);
    spawn.setBuildDelay(s.getDelay());
    if (s.getParentID() != 0) {
	DrawObject parent = getRobot(s.getParentID());
	parent.setAction(s.getDelay(), ActionType.BUILDING);
    }
    putRobot(s.getRobotID(), spawn);
    tryAddHQ(spawn);
    tryAddTower(spawn);
    tryAddCommander(spawn);
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

  public void visitHealthChange(HealthChangeSignal s){
    int[] robotIDs = s.getRobotIDs();
    double[] health = s.getHealth();
    for (int i = 0; i < robotIDs.length; i++) {
      DrawObject robot = getRobot(robotIDs[i]);
      if (robot != null) {
          robot.setEnergon(health[i]);
      }
    }
  }

  public void visitRobotInfoSignal(RobotInfoSignal s){
    int[] robotIDs = s.getRobotIDs();
    double[] coreDelays = s.getCoreDelays();
    double[] weaponDelays = s.getWeaponDelays();
    double[] supplyLevels = s.getSupplyLevels();
    for (int i = 0; i < robotIDs.length; i++) {
      DrawObject robot = getRobot(robotIDs[i]);
      if (robot != null) {
          robot.setMovementDelay(coreDelays[i]);
          robot.setAttackDelay(weaponDelays[i]);
          robot.setSupplyLevel(supplyLevels[i]);
      }
    }
  }

  public void visitXPSignal(XPSignal s) {
    getRobot(s.getRobotID()).setXP(s.getXP());
  }

  public void visitMissileCountSignal(MissileCountSignal s) {
    getRobot(s.getRobotID()).setMissileCount(s.getMissileCount());
  }

  public void visitLocationOreChangeSignal(LocationOreChangeSignal s) {
    locationOre.put(s.getLocation(), s.getOre());
  }
}

