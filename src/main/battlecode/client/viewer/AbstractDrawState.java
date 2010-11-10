package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Chassis;
import battlecode.common.Team;
import battlecode.common.TerrainTile;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
import battlecode.world.signal.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractDrawState<DrawObject extends AbstractDrawObject> extends GameState {

	protected abstract DrawObject createDrawObject(Chassis type, Team team);

	protected Map<Integer, DrawObject> groundUnits;
	protected Map<Integer, DrawObject> airUnits;
	protected List<DrawObject> archonsA;
	protected List<DrawObject> archonsB;
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

	protected Iterable<Map.Entry<Integer, DrawObject>> getDrawableSet() {
		if (!RenderConfiguration.showGround() && !RenderConfiguration.showAir())
			return null;

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
		//if (archon.getType() == Chassis.ARCHON) {
		//	(archon.getTeam() == Team.A ? archonsA : archonsB).add(archon);
		//}
	}

	public List<DrawObject> getArchons(Team team) {
		return (team == Team.A ? archonsA : archonsB);
	}

	public RoundStats getRoundStats() {
		return stats;
	}
    
	public void setGameMap(GameMap map) {
        gameMap = new GameMap(map);
		origin = gameMap.getMapOrigin();
    }

    public GameMap getGameMap(){
    	return gameMap;
    }
    
	protected abstract void mineFlux(DrawObject object);

	protected void updateRound() {
		currentRound++;
		for (Iterator<Map.Entry<Integer, DrawObject>> it = drawables.iterator();
				it.hasNext();) {
			DrawObject obj = it.next().getValue();
			obj.updateRound();
			if (!obj.isAlive()) {
				it.remove();
				//if (obj.getType() == Chassis.ARCHON) {
				//	(obj.getTeam() == Team.A ? archonsA : archonsB).remove(obj);
				//}
			}
			//if (obj.getType() == Chassis.WOUT) {
			//	mineFlux(obj);
			//}
		}
	}

	public Void visitAttackSignal(AttackSignal s) {
		getRobot(s.getRobotID()).setAttacking(s.getTargetLoc(), s.getTargetHeight(), s.weaponType);
		return null;
	}

	public Void visitBroadcastSignal(BroadcastSignal s) {
		getRobot(s.getRobotID()).setBroadcast();
		return null;
	}

	public Void visitDeathSignal(DeathSignal s) {
		getRobot(s.getObjectID()).destroyUnit();
		return null;
	}

	public Void visitEnergonChangeSignal(EnergonChangeSignal s) {
		int[] robotIDs = s.getRobotIDs();
		double[] energon = s.getEnergon();
		for (int i = 0; i < robotIDs.length; i++) {
			getRobot(robotIDs[i]).setEnergon(energon[i]);
		}
		return null;
	}

	public Void visitIndicatorStringSignal(IndicatorStringSignal s) {
		if (!RenderConfiguration.isTournamentMode())
			getRobot(s.getRobotID()).setString(s.getStringIndex(), s.getNewString());
		return null;
	}

	public Void visitControlBitsSignal(ControlBitsSignal s) {
		getRobot(s.getRobotID()).setControlBits(s.getControlBits());
		return null;
	}

	public Void visitMovementOverrideSignal(MovementOverrideSignal s) {
		getRobot(s.getRobotID()).setLocation(s.getNewLoc());
		return null;
	}

	public Void visitMovementSignal(MovementSignal s) {
		DrawObject obj = getRobot(s.getRobotID());
		obj.setLocation(s.getNewLoc());
		obj.setMoving(s.isMovingForward());
		return null;
	}

	public Void visitSetDirectionSignal(SetDirectionSignal s) {
		getRobot(s.getRobotID()).setDirection(s.getDirection());
		return null;
	}

	public DrawObject spawnRobot(SpawnSignal s) {
		DrawObject spawn = createDrawObject(s.getType(), s.getTeam());
		spawn.setLocation(s.getLoc());
		spawn.setDirection(s.getDirection());
		putRobot(s.getRobotID(), spawn);
		tryAddArchon(spawn);
		return spawn;
	}

	public Void visitSpawnSignal(SpawnSignal s) {
		spawnRobot(s);
		return null;
	}

	public Void visitBytecodesUsedSignal(BytecodesUsedSignal s) {
		int[] robotIDs = s.getRobotIDs();
		int[] bytecodes = s.getNumBytecodes();
		for (int i = 0; i < robotIDs.length; i++) {
			getRobot(robotIDs[i]).setBytecodesUsed(bytecodes[i]);
		}
		return null;
	}

}
