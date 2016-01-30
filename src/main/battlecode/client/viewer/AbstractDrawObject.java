package battlecode.client.viewer;

import battlecode.client.viewer.render.ClearAnimation;
import battlecode.client.viewer.render.ExplosionAnim;
import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.*;

import java.util.*;

import battlecode.client.viewer.render.UnitAnimation;

public abstract class AbstractDrawObject {
    public static class RobotInfo {

        public final RobotType type;
        public final Team team;

        public RobotInfo(RobotType type, Team team) {
            this.type = type;
            this.team = team;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof RobotInfo
                    && ((RobotInfo) obj).type == this.type
                    && ((RobotInfo) obj).team == this.team);
        }

        @Override
        public int hashCode() {
            return type.ordinal() + 561 * team.ordinal();
        }
    }

    public AbstractDrawObject(int currentRound, RobotType type, Team team,
                              int id) {
        this.currentRound = currentRound;
        info = new RobotInfo(type, team);
        robotID = id;
        hats = "";
        actions = new ArrayList<>();
        unitAnimations = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public AbstractDrawObject(int currentRound, AbstractDrawObject
            copy) {
        this(currentRound, copy.info.type, copy.info.team, copy.getID());
        loc = copy.loc;
        dir = copy.dir;
        health = copy.health;
        shields = copy.shields;
        flux = copy.flux;
        moving = copy.moving;
        broadcast = copy.broadcast;
        controlBits = copy.controlBits;
        bytecodesUsed = copy.bytecodesUsed;
        System.arraycopy(copy.indicatorStrings, 0, indicatorStrings, 0,
                GameConstants.NUMBER_OF_INDICATOR_STRINGS);
        turnedOn = copy.turnedOn;
        loaded = copy.loaded;
        regen = copy.regen;

        attackDelay = copy.attackDelay;
        movementDelay = copy.movementDelay;

        buildDelay = copy.buildDelay;
        zombieInfectedTurns = copy.zombieInfectedTurns;
        viperInfectedTurns = copy.viperInfectedTurns;
        aliveRounds = copy.aliveRounds;

        hats = copy.hats;

        for (Action action : copy.actions) {
            this.actions.add(action);
        }

        for (UnitAnimation animation : copy.unitAnimations) {
            this.unitAnimations.add((UnitAnimation) animation.clone());
        }

        if (copy.deathAnimation != null) {
            this.deathAnimation = (ExplosionAnim) copy.deathAnimation.clone();
        }

        updateDrawLoc();
    }

    public abstract ExplosionAnim createDeathExplosionAnim(boolean isSuicide);

    protected String hats;
    protected RobotInfo info;
    protected MapLocation loc;
    protected Direction dir;
    protected float drawX = 0, drawY = 0;
    protected int moving = 0;
    protected double health = 0;
    protected double shields = 0;
    protected double flux = 0;
    protected double maxHealth;
    protected int broadcast = 0;
    protected long controlBits = 0;
    protected int bytecodesUsed = 0;
    protected double attackDelay = 0;
    protected double movementDelay = 0;
    protected final int visualBroadcastRadius = 2;
    protected boolean turnedOn = true;
    protected boolean loaded = false;
    protected int regen = 0;
    protected int robotID;
    protected boolean isSuiciding = false;
    protected int zombieInfectedTurns = 0;
    protected int viperInfectedTurns = 0;
    protected int buildDelay = 0;
    protected int aliveRounds = 0;
    protected int currentRound = 0;

    protected final List<Action> actions;
    protected final List<UnitAnimation> unitAnimations;
    protected ExplosionAnim deathAnimation;

    protected final String[] indicatorStrings =
            new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];

    public void setDirection(Direction d) {
        dir = d;
    }

    public int getID() {
        return robotID;
    }

    public MapLocation getLoc() {
        return loc;
    }

    public RobotType getType() {
        return info.type;
    }

    public Team getTeam() {
        return info.team;
    }

    public float getDrawDX() {
        return drawX;
    }

    public float getDrawDY() {
        return drawY;
    }

    public float getDrawX() {
        return loc.x + drawX;
    }

    public float getDrawY() {
        return loc.y + drawY;
    }

    public MapLocation getLocation() {
        return loc;
    }

    public double getHealth() {
        return health;
    }

    public double getShields() {
        return shields;
    }

    public String getIndicatorString(int index) {
        return indicatorStrings[index];
    }

    public long getControlBits() {
        return controlBits;
    }

    public int getBytecodesUsed() {
        return bytecodesUsed;
    }

    public double getAttackDelay() {
        return attackDelay;
    }

    public double getMovementDelay() {
        return movementDelay;
    }

    public int getZombieInfectedTurns() {
        return zombieInfectedTurns;
    }

    public int getViperInfectedTurns() {
        return viperInfectedTurns;
    }

    public boolean inTransport() {
        return loaded;
    }

    public void setType(RobotType type) {
        this.info = new RobotInfo(type, info.team);
    }

    public void setLocation(MapLocation loc) {
        if (this.loc != null) {
            int dx = loc.x - this.loc.x, dy = loc.y - this.loc.y;
            for (UnitAnimation anim : this.unitAnimations) {
                anim.unitMoved(dx, dy);
            }
        }

        this.loc = loc;
    }

    public void setBroadcast() {
        broadcast++;
    }

    public void setZombieInfectedTurns(int zombieInfectedTurns) {
        this.zombieInfectedTurns = zombieInfectedTurns;
    }

    public void setViperInfectedTurns(int viperInfectedTurns) {
        this.viperInfectedTurns = viperInfectedTurns;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setBuildDelay(int delay) {
        this.buildDelay = delay;
    }

    public void setString(int index, String newString) {
        indicatorStrings[index] = newString;
    }

    public void setControlBits(long controlBits) {
        this.controlBits = controlBits;
    }

    public void setBytecodesUsed(int used) {
        bytecodesUsed = used;
    }

    public void setAttackDelay(double delay) {
        attackDelay = delay;
    }

    public void setMovementDelay(double delay) {
        movementDelay = delay;
    }

    public boolean isAlive() {
        return deathAnimation == null || deathAnimation.isAlive();
    }

    public void setAttacking(MapLocation target) {
        actions.add(new Action(ActionType.ATTACKING, currentRound,
                (int) info.type.attackDelay, target));
    }

    public void setClearing(MapLocation target) {
        unitAnimations.add(new ClearAnimation(target.x - loc.x, target.y - loc.y));
    }

    public void setMoving(int delay) {
        actions.add(new Action(ActionType.MOVING, currentRound,
                delay));
        moving = 1;
        updateDrawLoc();
    }

    public void setAction(int totalrounds, ActionType type) {
        actions.add(new Action(type, currentRound, totalrounds));
    }

    public void destroyUnit() {
        health = 0;
        shields = 0;
        zombieInfectedTurns = 0;
        viperInfectedTurns = 0;
        deathAnimation = createDeathExplosionAnim(false);
    }

    public void updateRound() {
        ListIterator<Action> actionIterator = actions.listIterator();
        while (actionIterator.hasNext()) {
            Action a = actionIterator.next();
            if (currentRound >= (a.roundStarted + a.length)) {
                actionIterator.remove();
            }
        }

        aliveRounds += 1;

        updateDrawLoc();

        broadcast = (broadcast << 1) & 0x000FFFFF;
        if (regen > 0) regen--;

        Iterator<UnitAnimation> it = unitAnimations.iterator();
        while (it.hasNext()) {
            final UnitAnimation anim = it.next();
            anim.updateRound();
            if (!anim.isAlive()) {
                it.remove();
            }
        }

        if (deathAnimation != null) {
            deathAnimation.updateRound();
        }

        currentRound++;
    }

    private void updateDrawLoc() {
        if (RenderConfiguration.showDiscrete()) {
            drawX = drawY = 0;
        } else {
            // still waiting perfection of delay system
            // float dist = .5f;
        	int rounds = 0;
        	for (Action a : actions) {
        		if (a.type == ActionType.MOVING) {
        			rounds = currentRound - a.roundStarted;
        		}
        	}
//        	assert(rounds < info.type.movementDelay && rounds >=0);
            float dist = (float) (rounds / info.type.movementDelay);
            System.out.println("moving: " + rounds + " movementdelay: " + info.type.movementDelay);
            drawX = -dist * dir.dx;
            drawY = -dist * dir.dy;
        }
    }
}
