package battlecode.client.viewer;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.client.viewer.render.TransferAnim;
import battlecode.common.*;

import java.util.*;
import battlecode.client.viewer.render.Animation;

import static battlecode.client.viewer.render.Animation.AnimationType.*;

public abstract class AbstractDrawObject {
    protected static int moveDelay;

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
        actions = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    public AbstractDrawObject(int currentRound, AbstractDrawObject
            copy) {
        this(currentRound, copy.info.type, copy.info.team, copy.getID());
        loc = copy.loc;
        dir = copy.dir;
        energon = copy.energon;
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

        actions = (LinkedList<Action>) actions.clone();
        attackDelay = copy.attackDelay;
        movementDelay = copy.movementDelay;

        hats = copy.hats;

        for (Map.Entry<Animation.AnimationType, Animation> entry :
                copy.animations.entrySet()) {
            animations.put(entry.getKey(), (Animation) entry.getValue().clone
                    ());
        }

        updateDrawLoc();
    }

    public abstract Animation createDeathExplosionAnim(boolean isSuicide);

    public abstract Animation createMortarExplosionAnim(Animation
                                                                mortarAttackAnim);

    public abstract Animation createTransferAnim(MapLocation loc,
                                                 TransferAnim.TransferAnimType type);

    protected String hats;
    protected RobotInfo info;
    protected MapLocation loc;
    protected Direction dir;
    protected float drawX = 0, drawY = 0;
    protected int moving = 0;
    protected double energon = 0;
    protected double shields = 0;
    protected double flux = 0;
    protected double maxEnergon;
    protected int totalActionRounds;
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
    protected Direction attackDir;
    protected boolean isSuiciding = false;
    protected int zombieInfectedTurns = 0;
    protected int viperInfectedTurns = 0;
    protected int buildDelay = 0;
    protected int aliveRounds = 0;
    protected int currentRound = 0;

    protected LinkedList<Action> actions = null;
    protected Map<Animation.AnimationType, Animation> animations =
            new EnumMap<Animation.AnimationType, Animation>
                    (Animation.AnimationType.class) {

        private static final long serialVersionUID = 0;
        // if we've written an animation for one client but not the other, we
        // don't
        // want to be putting null values in the animations list

        @Override
        public Animation put(Animation.AnimationType key, Animation
                value) {
            if (value != null)
                return super.put(key, value);
            else
                return super.remove(key);
        }
    };
    protected String[] indicatorStrings =
            new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];

    public void setSuiciding(boolean is) {
        isSuiciding = is;
    }

    public void setDirection(Direction d) {
        dir = d;
    }

    private static final double sq2 = Math.sqrt(2.);

    public int getID() {
        return robotID;
    }

    public MapLocation getLoc() {
        return loc;
    }

    public long getBroadcast() {
        return broadcast;
    }

    public int broadcastRadius() {
        return visualBroadcastRadius;
    }

    public RobotType getType() {
        return info.type;
    }

    public Team getTeam() {
        return info.team;
    }

    public RobotType getRobotType() {
        return info.type;
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

    public Direction getDirection() {
        return dir;
    }

    public double getEnergon() {
        return energon;
    }

    public double getShields() {
        return shields;
    }

    public double getFlux() {
        return flux;
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

    public void load() {
        loaded = true;
    }

    public void unload(MapLocation loc) {
        loaded = false;
        setLocation(loc);
    }

    public boolean inTransport() {
        return loaded;
    }

    public void setType(RobotType type) {
        this.info = new RobotInfo(type, info.team);
    }

    public void setLocation(MapLocation loc) {
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

    public void setEnergon(double energon) {
        this.energon = energon;
    }

    public void setShields(double shields) {
        this.shields = shields;
    }

    public void setFlux(double f) {
        flux = f;
    }

    public void setBuildDelay(int delay) {
        this.buildDelay = delay;
    }

    public void setTeam(Team team) {
        info = new RobotInfo(info.type, team);
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
        Animation deathAnim = animations.get(Animation.AnimationType
                .DEATH_EXPLOSION);
        return deathAnim == null || deathAnim.isAlive()
                || animations.get(Animation.AnimationType
                .MORTAR_ATTACK) != null
                || animations.get(Animation.AnimationType
                .MORTAR_EXPLOSION) != null;
    }

    public void setAttacking(MapLocation target) {
        actions.add(new Action(ActionType.ATTACKING, currentRound,
                (int) info.type.attackDelay, target));
        attackDir = dir;
    }

    public void setTransfer(MapLocation target, TransferAnim
            .TransferAnimType type) {
        Animation anim = createTransferAnim(target,
                type);
        animations.put(TRANSFER, anim);
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

    public void setAction(int totalrounds, ActionType type, MapLocation
            target) {
        actions.add(new Action(type, currentRound, totalrounds, target));

    }

    public void destroyUnit() {
        energon = 0;
        shields = 0;
        zombieInfectedTurns = 0;
        viperInfectedTurns = 0;
        animations.put(DEATH_EXPLOSION, createDeathExplosionAnim(false));
        animations.remove(TRANSFER);
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

        Iterator<Map.Entry<Animation.AnimationType, Animation>> it =
                animations.entrySet().iterator();
        Map.Entry<Animation.AnimationType, Animation> entry;
        Animation mortarExplosionAnim = null;
        while (it.hasNext()) {
            entry = it.next();
            entry.getValue().updateRound();
            if (!entry.getValue().isAlive()) {
                if (entry.getKey() == MORTAR_ATTACK) {
                    mortarExplosionAnim = createMortarExplosionAnim(entry.getValue());
                }
                if (entry.getKey() != DEATH_EXPLOSION)
                    it.remove();
            }
        }
        if (mortarExplosionAnim != null)
            animations.put(MORTAR_EXPLOSION, mortarExplosionAnim);
        currentRound++;
    }

    private void updateDrawLoc() {
        if (RenderConfiguration.showDiscrete()
                || !isDoing(ActionType.MOVING)) {
            drawX = drawY = 0;
        } else {
            // still waiting perfection of delay system
            // float dist = .5f;
            float dist = (float) Math.max(Math.min(moving * (movementDelay / info.type.movementDelay), 1), 0);
            //System.out.println("moving: " + moving + "actionDelay: " + actionDelay + "total " + totalActionRounds);
            drawX = -dist * dir.dx;
            drawY = -dist * dir.dy;
        }
    }

    public void setPower(boolean b) {
        turnedOn = b;
    }

    protected boolean isDoing(ActionType type) {
        for (Action a : actions) {
            if (a.type == type) {
                return true;
            }
        }
        return false;
    }

}
