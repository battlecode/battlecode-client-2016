package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
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
    protected double[] teamHP = new double[2];
	protected Map<Team, List<DrawObject>> archons;
	protected int [] coreIDs = new int [2];
	protected Map<Team,MapLocation> coreLocs = new EnumMap<Team,MapLocation>(Team.class);
    protected static MapLocation origin = null;
    protected GameMap gameMap;
    protected int currentRound;
    protected RoundStats stats = null;
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

	private class Graph implements battlecode.world.GameWorld.Graph {

		private Set<MapLocation> [] connected;

		public Graph() {
			connected = new Set [] { new HashSet<MapLocation>(), new HashSet<MapLocation>() };
		}

		public List<MapLocation> neighbors(MapLocation loc) {
			return neighbors.get(loc);
		}

		public MapLocation baseNode(Team t) {
			return coreLocs.get(t);
		}

		public Team team(MapLocation loc) {
			if(nodeTeams.containsKey(loc))
				return nodeTeams.get(loc);
			else
				return Team.NEUTRAL;
		}

		public void setConnected(MapLocation loc, Team t) {
			connected[t.ordinal()].add(loc);
		}

		public void colorLinks() {
			
			for(int i=0;i<2;i++) {
				Team tm = Team.values()[i];
				for(Link l: links) {
					boolean tConn = team(l.to)==tm && connected[i].contains(l.to);
					boolean fConn = team(l.from)==tm && connected[i].contains(l.from);
					l.connected[i] = tConn || fConn; 
				}
			}
		}

	}

	public AbstractDrawState() {
		archons = new EnumMap<Team, List<DrawObject>>(Team.class);
		archons.put(Team.A,new ArrayList<DrawObject>());
		archons.put(Team.B,new ArrayList<DrawObject>());
	}

	protected synchronized void copyStateFrom(AbstractDrawState<DrawObject> src) {
        groundUnits.clear();
        archons.get(Team.A).clear();
        archons.get(Team.B).clear();
        for (Map.Entry<Integer, DrawObject> entry : src.groundUnits.entrySet()) {
            DrawObject copy = createDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
            tryAddArchon(copy);
        }
        airUnits.clear();
        for (Map.Entry<Integer, DrawObject> entry : src.airUnits.entrySet()) {
            DrawObject copy = createDrawObject(entry.getValue());
            airUnits.put(entry.getKey(), copy);
        }
        fluxDeposits.clear();
        for (Map.Entry<Integer, FluxDepositState> entry : src.fluxDeposits.entrySet()) {
            fluxDeposits.put(entry.getKey(), new FluxDepositState(entry.getValue()));
        }
        coreIDs = src.coreIDs;
		stats = src.stats;

		nodeTeams = new HashMap<MapLocation,Team>(src.nodeTeams);
	
		neighbors = src.neighbors;
		coreLocs = src.coreLocs;

		links.clear();
		for(Link l : src.links) {
			links.add(new Link(l));
		}
	
        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }

        currentRound = src.currentRound;
    }

	public void recomputeConnected() {
		if(neighbors!=null) {
			Graph g = new Graph();
			new battlecode.world.GameWorld.Connections(g,Team.A).findAll();
			new battlecode.world.GameWorld.Connections(g,Team.B).findAll();
			g.colorLinks();
		}
	}

	public List<DrawObject> getArchons(Team t) {
		return archons.get(t);
	}

	public DrawObject getPowerCore(Team t) {
		int id = coreIDs[t.ordinal()];
		if(id!=0)
			return getRobot(id);
		else
			return null;
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
        if (unit.getType().isAirborne()) {
            DrawObject previous = airUnits.put(id, unit);
            assert previous == null : "Robot #" + id + " already exists";
        } else {
            DrawObject previous = groundUnits.put(id, unit);
            assert previous == null : "Robot #" + id + " already exists";
        }
    }

    protected void tryAddArchon(DrawObject archon) {
        if (archon.getType() == RobotType.ARCHON)
			archons.get(archon.getTeam()).add(archon);
    }

    //public List<DrawObject> getArchons(Team team) {
    //    return (team == Team.A ? archonsA : archonsB);
    //}
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

    public void visitBroadcastSignal(BroadcastSignal s) {
        getRobot(s.getRobotID()).setBroadcast();
    }

    public void visitDeathSignal(DeathSignal s) {
        int team = getRobot(s.getObjectID()).getTeam().ordinal();
        DrawObject robot = getRobot(s.getObjectID());
        if (team < 2) {
            teamHP[team] -= getRobot(s.getObjectID()).getEnergon();
        }

		if(robot.getType()==RobotType.TOWER) {
			nodeTeams.remove(robot.getLocation());
			recomputeConnected();
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

    public void visitFluxChangeSignal(FluxChangeSignal s) {
        int[] robotIDs = s.getRobotIDs();
        double[] flux = s.flux;
        for (int i = 0; i < robotIDs.length; i++) {
            getRobot(robotIDs[i]).setFlux(flux[i]);
        }
        
    }

	public void visitTransferFluxSignal(TransferFluxSignal s) {
		DrawObject from = getRobot(s.fromID);
		DrawObject to = getRobot(s.toID);
		from.setFluxTransfer(to,s.amount);
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
        obj.setLocation(s.getNewLoc());
        if (teleported) {
            obj.setTeleport(obj.loc, s.getNewLoc());
        } else {

            obj.setMoving(s.isMovingForward());
        }
        
    }

	public void connectNode(MapLocation l1, MapLocation l2) {
		if(!neighbors.containsKey(l1))
			neighbors.put(l1,new ArrayList<MapLocation>());
		neighbors.get(l1).add(l2);

	}

	public void visitNodeConnectionSignal(NodeConnectionSignal s) {
		neighbors = new HashMap<MapLocation,List<MapLocation>>();
		for(MapLocation [] l : s.connections) {
			links.add(new Link(l[0],l[1]));
			connectNode(l[0],l[1]);
			connectNode(l[1],l[0]);
		}
		recomputeConnected();
	}

    public void visitSetDirectionSignal(SetDirectionSignal s) {
        getRobot(s.getRobotID()).setDirection(s.getDirection());
        
    }

    public DrawObject spawnRobot(SpawnSignal s) {
        DrawObject spawn = createDrawObject(s.getType(), s.getTeam(), s.getRobotID());
        spawn.setLocation(s.getLoc());
        spawn.setDirection(s.getDirection());
        putRobot(s.getRobotID(), spawn);
        tryAddArchon(spawn);
        int team = getRobot(s.getRobotID()).getTeam().ordinal();
        if (team < 2) {
            teamHP[team] += getRobot(s.getRobotID()).getEnergon();
        }
		if(spawn.getType()==RobotType.TOWER) {
			nodeTeams.put(spawn.getLocation(),spawn.getTeam());
			if(!coreLocs.containsKey(spawn.getTeam())) {
				coreIDs[spawn.getTeam().ordinal()]=spawn.getID();
				coreLocs.put(spawn.getTeam(),spawn.getLocation());
			}
			recomputeConnected();
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
		getRobot(s.robotID).setRegen();
	}

    public void visitTurnOffSignal(TurnOffSignal s) {
        getRobot(s.robotID).setPower(false);
    }
}
