package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
import battlecode.world.InternalRobot;
import battlecode.world.signal.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDrawState<DrawObject extends AbstractDrawObject> extends GameState {

    protected abstract DrawObject createDrawObject(RobotType type, Team team, int id);
	protected abstract DrawObject createDrawObject(DrawObject o);
    protected Map<Integer, DrawObject> groundUnits;
    protected Map<Integer, DrawObject> airUnits;
    protected Map<Integer, FluxDepositState> fluxDeposits;
    protected Set<MapLocation> encampments;
    protected double[] teamHP = new double[2];
//	protected Map<Team, List<DrawObject>> archons;
	protected Map<Team, DrawObject> hqs;
	protected int [] coreIDs = new int [2];
//	protected Map<Team,MapLocation> coreLocs = new EnumMap<Team,MapLocation>(Team.class);
	protected Map<MapLocation,Team> mineLocs = new HashMap<MapLocation, Team>();
    protected static MapLocation origin = null;
    protected GameMap gameMap;
    protected int currentRound;
    protected RoundStats stats = null;
    protected double[] teamResources = new double[2];
		protected double[][] researchProgress = new double[2][5];
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
	}

	protected synchronized void copyStateFrom(AbstractDrawState<DrawObject> src) {
        groundUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet()) {
            DrawObject copy = createDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
            tryAddHQ(copy);
        }
        airUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.airUnits.entrySet()) {
            DrawObject copy = createDrawObject(entry.getValue());
            airUnits.put(entry.getKey(), copy);
        }
        
        mineLocs.clear();
        mineLocs.putAll(src.mineLocs);
        
        
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

        currentRound = src.currentRound;
        for (int x=0; x<teamResources.length; x++)
        	teamResources[x] = src.teamResources[x];
				for (int t = 0; t < researchProgress.length; t++)
						for (int r = 0; r < researchProgress[t].length; r++)
								researchProgress[t][r] = src.researchProgress[t][r];
    }

	public DrawObject getHQ(Team t) {
		return hqs.get(t);
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
        if (!RenderConfiguration.showGround() && !RenderConfiguration.showAir()) {
            return null;
        }

        if (!RenderConfiguration.showGround()) {
            return airUnits.entrySet();
        }
        if (!RenderConfiguration.showAir()) {
            return groundUnits.entrySet();
        }
        return drawables;
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

    public RoundStats getRoundStats() {
        return stats;
    }

    public void setGameMap(GameMap map) {
        gameMap = new GameMap(map);
        origin = gameMap.getMapOrigin();
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    protected void updateRound() {
        currentRound++;
        for (Iterator<Map.Entry<Integer, DrawObject>> it = drawables.iterator();
                it.hasNext();) {
            DrawObject obj = it.next().getValue();
            obj.updateRound();
            if (!obj.isAlive()) {
                it.remove();
                //if (obj.getType() == RobotType.ARCHON) {
                //	(obj.getTeam() == Team.A ? archonsA : archonsB).remove(obj);
                //}
            }
            //if (obj.getType() == RobotType.WOUT) {
            //	mineFlux(obj);
            //}
        }
    }

    public void visitAttackSignal(AttackSignal s) {
        getRobot(s.getRobotID()).setAttacking(s.getTargetLoc(), s.getTargetHeight());
        
    }
    
    public void visitShieldSignal(ShieldSignal s) {
//    	getRobot(s.robotID).setAction(1, ActionType.SHIELDING);
    	getRobot(s.robotID).setAttacking(null, null);
    }

    public void visitBroadcastSignal(BroadcastSignal s) {
        getRobot(s.getRobotID()).setBroadcast();
    }

    public void visitDeathSignal(DeathSignal s) {
        int team = getRobot(s.getObjectID()).getTeam().ordinal();
        DrawObject robot = getRobot(s.getObjectID());
        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getEnergon();
        }

        robot.destroyUnit();
        
    }

    public void visitEnergonChangeSignal(EnergonChangeSignal s) {
        int[] robotIDs = s.getRobotIDs();
        double[] energon = s.getEnergon();
        for (int i = 0; i < robotIDs.length; i++) {
            int team = getRobot(robotIDs[i]).getTeam().ordinal();
            if (team < 2)
                teamHP[team] -= getRobot(robotIDs[i]).getEnergon();
            getRobot(robotIDs[i]).setEnergon(energon[i]);
            if (team < 2)
                teamHP[team] += energon[i];
        }
    }
    
    public void visitShieldChangeSignal(ShieldChangeSignal s) {
    	int[] robotIDs = s.getRobotIDs();
        double[] shield = s.getShield();
        for (int i = 0; i < robotIDs.length; i++) {
//            int team = getRobot(robotIDs[i]).getTeam().ordinal();
//            if (team < 2)
//                teamHP[team] -= getRobot(robotIDs[i]).getEnergon();
            getRobot(robotIDs[i]).setShields(shield[i]);
//            if (team < 2)
//                teamHP[team] += energon[i];
        }
    }

    public void visitFluxChangeSignal(FluxChangeSignal s) {
//        int[] robotIDs = s.getRobotIDs();
//        double[] flux = s.flux;
//        for (int i = 0; i < robotIDs.length; i++) {
//            getRobot(robotIDs[i]).setFlux(flux[i]);
//        }
//        teamResources = s.flux;
        for (int x=0; x<teamResources.length; x++)
        	teamResources[x] = s.flux[x];
    }

		public void visitResearchChangeSignal(ResearchChangeSignal s) {
				for (int t = 0; t < researchProgress.length; t++)
						for (int r = 0; r < researchProgress[t].length; r++)
								researchProgress[t][r] = s.progress[t][r];
		}

	public void visitTransferFluxSignal(TransferFluxSignal s) {
		DrawObject from = getRobot(s.fromID);
		DrawObject to = getRobot(s.toID);
		//from.setFluxTransfer(to,s.amount);
	}

	public void visitIndicatorStringSignal(IndicatorStringSignal s) {
        if (!RenderConfiguration.isTournamentMode()) {
            getRobot(s.getRobotID()).setString(s.getStringIndex(), s.getNewString());
        }
        
    }

    public void visitControlBitsSignal(ControlBitsSignal s) {
        getRobot(s.getRobotID()).setControlBits(s.getControlBits());
        
    }

    public void visitMovementOverrideSignal(MovementOverrideSignal s) {
        getRobot(s.getRobotID()).setLocation(s.getNewLoc());
        
    }

    public void visitMovementSignal(MovementSignal s) {
        DrawObject obj = getRobot(s.getRobotID());
        boolean teleported = !obj.loc.isAdjacentTo(s.getNewLoc());
        //TODO: this should probably be from a teleported signal
        MapLocation oldloc = obj.loc;
        obj.setLocation(s.getNewLoc());
        if (teleported) {
            obj.setTeleport(obj.loc, s.getNewLoc());
        } else {
        	obj.setDirection(oldloc.directionTo(s.getNewLoc()));
            obj.setMoving(s.isMovingForward());
        }
        
    }
    
    public void visitMineSignal(MineSignal s) {
    	if (s.shouldAdd()) {
    		if (mineLocs.get(s.getMineLoc()) == null)
    			mineLocs.put(s.getMineLoc(), s.getMineTeam());
    	} else {
    		mineLocs.remove(s.getMineLoc());
    	}
    }
    
    public void visitMinelayerSignal(MinelayerSignal s) {
    	if (s.isDefusing())
    		getRobot(s.getRobotID()).setAction(GameConstants.MINE_DEFUSE_DELAY, ActionType.DEFUSING);
    	else
    		getRobot(s.getRobotID()).setAction(GameConstants.MINE_LAY_DELAY, ActionType.MINING);
    }
    
    public void visitCaptureSignal(CaptureSignal s) {
    	getRobot(s.getParentID()).setAction(GameConstants.CAPTURE_ROUND_DELAY, ActionType.CAPTURING);
    		
    }

    public void visitSetDirectionSignal(SetDirectionSignal s) {
        getRobot(s.getRobotID()).setDirection(s.getDirection());
        
    }

    public DrawObject spawnRobot(SpawnSignal s) {
        DrawObject spawn = createDrawObject(s.getType(), s.getTeam(), s.getRobotID());
        spawn.setLocation(s.getLoc());
//        spawn.setDirection(s.getDirection());
        spawn.setDirection(Direction.NORTH);
        
        putRobot(s.getRobotID(), spawn);
        tryAddHQ(spawn);
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
            getRobot(robotIDs[i]).setBytecodesUsed(bytecodes[i]);
        }
        
    }

	public void visitLoadSignal(LoadSignal s) {
		getRobot(s.passengerID).load();
	}

	public void visitUnloadSignal(UnloadSignal s) {
		getRobot(s.passengerID).unload(s.unloadLoc);
	}

    public void visitTurnOnSignal(TurnOnSignal s) {
        for (int i : s.robotIDs)
            getRobot(i).setPower(true);
    }

	public void visitRegenSignal(RegenSignal s) {
//		getRobot(s.robotID).setRegen();
//		getRobot(s.robotID).setAction(1, ActionType.REGENING);
		getRobot(s.robotID).setAttacking(null, null);
	}

    public void visitTurnOffSignal(TurnOffSignal s) {
        getRobot(s.robotID).setPower(false);
    }
    
    public void visitNodeBirthSignal(NodeBirthSignal s) {
    	encampments.add(s.location);
    }
}
