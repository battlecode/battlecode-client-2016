package battlecode.client.viewer;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.client.util.ImageFile;
import battlecode.client.viewer.render.RenderConfiguration;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;


public abstract class AbstractDrawObject<Animation extends AbstractAnimation> {
  protected static int moveDelay;
  private static Random hatGenerator = new Random();
  private static final int numHats;
  static {
    int nhats = 0;
    File[] files = (new File("art/hats/")).listFiles();
    for (int x = 0; x < files.length; ++x) {
        final String fname = files[x].getAbsolutePath();
        final String extension = fname.substring(fname.lastIndexOf(".") + 1, fname.length());
        if (extension.toLowerCase().equals("png")) {
            ++nhats;
        }
    }
    numHats = nhats;
  }
	
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

    public AbstractDrawObject(int currentRound, RobotType type, Team team, int id) {
	this.currentRound = currentRound;
    info = new RobotInfo(type, team);
    robotID = id;
    hats = "";
    actions = new LinkedList<Action>();
  }

  @SuppressWarnings("unchecked")
  public AbstractDrawObject(int currentRound, AbstractDrawObject<Animation> copy) {
      this(currentRound, copy.info.type, copy.info.team, copy.getID());
      loc = copy.loc;
    dir = copy.dir;
    energon = copy.energon;
    shields = copy.shields;
    supplyLevel = copy.supplyLevel;
    xp = copy.xp;
    missileCount = copy.missileCount;
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

    actions = (LinkedList<Action>)actions.clone();
    attackDelay = copy.attackDelay;
    movementDelay = copy.movementDelay;
    
    hats = copy.hats;

    for (Map.Entry<AbstractAnimation.AnimationType, Animation> entry : copy.animations.entrySet()) {
      animations.put(entry.getKey(), (Animation) entry.getValue().clone());
    }

    updateDrawLoc();
  }

  public abstract Animation createDeathExplosionAnim(boolean isSuicide);

  public abstract Animation createMortarAttackAnim(MapLocation target);

  public abstract Animation createMortarExplosionAnim(Animation mortarAttackAnim);
  public abstract Animation createEnergonTransferAnim(MapLocation loc, float amt, boolean isFlux);

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
  protected double supplyLevel = 0;
  protected int missileCount = 0;
  protected int xp = 0;
  protected int buildDelay=0;
  protected int aliveRounds=0;
    protected int currentRound = 0;
    
    protected LinkedList<Action> actions = null;
  protected Map<AbstractAnimation.AnimationType, Animation> animations = new EnumMap<AbstractAnimation.AnimationType, Animation>(AbstractAnimation.AnimationType.class) {

    private static final long serialVersionUID = 0;
    // if we've written an animation for one client but not the other, we don't
    // want to be putting null values in the animations list

    @Override
    public Animation put(AbstractAnimation.AnimationType key, Animation value) {
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

  protected int getMoveDelay() {
    return moveDelay;
  }
  

  public int getID() {
    return robotID;
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
  
  public RobotType getRobotType(){
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

  public double getSupplyLevel() {
    return supplyLevel;
  }

  public int getMissileCount() {
    return missileCount;
  }

  public int getXP() {
    return xp;
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

  public double getAttackDelay(){
      return attackDelay;
  }
  
  public double getMovementDelay(){
      return movementDelay;
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

  public void setLocation(MapLocation loc) {
    this.loc = loc;
  }

  public void setBroadcast() {
    broadcast++;
  }

  public void setXP(int xp) {
    this.xp = xp;
  }

  public void setSupplyLevel(double supplyLevel) {
    this.supplyLevel = supplyLevel;
  }

  public void setMissileCount(int missileCount) {
    this.missileCount = missileCount;
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

  public void setBuildDelay(int delay){
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

  public void setAttackDelay(double delay){
      attackDelay = delay;
  }
  
  public void setMovementDelay(double delay){
      movementDelay = delay;
  }

  public void setRegen() { regen = 2; }
	
  public void addHat(int hat) {
    hats += ""+(char)(((hat%numHats)+numHats)%numHats);
  }

  public boolean isAlive() {
    Animation deathAnim = animations.get(AbstractAnimation.AnimationType.DEATH_EXPLOSION);
    return deathAnim == null || deathAnim.isAlive()
      || animations.get(AbstractAnimation.AnimationType.MORTAR_ATTACK) != null
      || animations.get(AbstractAnimation.AnimationType.MORTAR_EXPLOSION) != null;
  }

  public void setAttacking(MapLocation target) {
      actions.add(new Action(ActionType.ATTACKING, currentRound,
			     info.type.attackDelay, target));
      attackDir = dir;
    //componentType = component;
    //if (info.type == RobotType.CHAINER) {
    //	animations.put(MORTAR_ATTACK,createMortarAttackAnim(target));
    //}
  }

  public void setSupplyTransfer(AbstractDrawObject<Animation> target, double amount) {
    Animation anim = createEnergonTransferAnim(target.getLocation(),(float)amount,true);
    animations.put(ENERGON_TRANSFER,anim);
  }

      public void setMoving(boolean isMovingForward, int delay) {
	  actions.add(new Action(ActionType.MOVING, currentRound,
				 delay));
	  moving = (isMovingForward ? 1 : -1);
//    totalActionRounds = delay;
	  updateDrawLoc();
      }
 
  public void setAction(int totalrounds, ActionType type){
      actions.add(new Action(type, currentRound, totalrounds));
  }

  public void setAction(int totalrounds, ActionType type, MapLocation target){
      actions.add(new Action(type, currentRound, totalrounds, target));
      
  }
  
  public void destroyUnit() {
    energon = 0;
    shields = 0;
    missileCount = 0;
    xp = 0;
    supplyLevel = 0;
    animations.put(DEATH_EXPLOSION, createDeathExplosionAnim(false));
    animations.remove(ENERGON_TRANSFER);
  }

  public void updateRound() {
      ListIterator<Action> actionIterator = actions.listIterator();
      while (actionIterator.hasNext()) {
	  Action a = actionIterator.next();
	  //System.out.println(currentRound + ", " + (a.roundStarted + a.length));
	  if (currentRound >= (a.roundStarted + a.length)) {
	      actionIterator.remove();
	  }
      }
		
      aliveRounds += 1;
		
      updateDrawLoc();

    broadcast = (broadcast << 1) & 0x000FFFFF;
    if(regen>0) regen--;

    Iterator<Map.Entry<AbstractAnimation.AnimationType, Animation>> it = animations.entrySet().iterator();
    Map.Entry<AbstractAnimation.AnimationType, Animation> entry;
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
	float dist = (float)Math.max(Math.min(moving * (movementDelay / info.type.movementDelay), 1), 0);
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
