package battlecode.client.viewer.renderer3d;

import java.awt.Color;



import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Vector3f;

import battlecode.client.util.ImageFile;
import battlecode.client.viewer.AbstractAnimation;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.client.viewer.ActionType;
import battlecode.common.ComponentType;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

class GLDrawObject extends AbstractDrawObject<GLAnimation> {

    private static final Color tintTeamA = new Color(1, 0, 0, 0.125f);
    private static final Color tintTeamB = new Color(0, 0, 1, 0.125f);

    //private GLEnergonTransferAnim energonTransferAnim = null;
    //private GLTeleportAnim teleportAnim = null;
    //private GLExplosionAnim deathExplosionAnim = null;
    //private GLMortarAttackAnim mortarAttackAnim = null;
	//private GLExplosionAnim mortarExplosionAnim = null;

    private RobotLevel targetHeight = null;
    // point list of the circle of spawn radius
    public Vector3f[] spawnCircle = null;
   
    public GLDrawObject(RobotType type, Team team) {
	super(type, team);
    }

    public GLDrawObject(GLDrawObject copy) {
	super(copy);
	targetHeight = copy.targetHeight;
	if(animations.containsKey(ENERGON_TRANSFER)) {
	    GLEnergonTransferAnim a = (GLEnergonTransferAnim)animations.get(ENERGON_TRANSFER);
	    a.setSource(this);
	}
    }

    public static void loadAll() {
    }

    public RobotLevel getTargetHeight() {
	return targetHeight;
    }

    public GLEnergonTransferAnim getEnergonTransferAnim() {
	return (GLEnergonTransferAnim)animations.get(AbstractAnimation.AnimationType.ENERGON_TRANSFER);
    }

    public GLExplosionAnim getExplosionAnim() {
	return (GLExplosionAnim)animations.get(AbstractAnimation.AnimationType.DEATH_EXPLOSION);
    }
    
    public GLTeleportAnim getTeleportAnim(){
	
	return (GLTeleportAnim)animations.get(AbstractAnimation.AnimationType.TELEPORT);
    }

    public GLMortarAttackAnim getMortarAttackAnim() {
	return (GLMortarAttackAnim)animations.get(AbstractAnimation.AnimationType.MORTAR_ATTACK);
    }

    public GLExplosionAnim getMortarExplosionAnim() {
	return (GLExplosionAnim)animations.get(AbstractAnimation.AnimationType.MORTAR_EXPLOSION);
    }

	public MapLocation getMortarAttackTarget() {
		return getMortarExplosionAnim().getLocation();
    }

    // get the time until idle
    public int timeUntilIdle() {
		return roundsUntilMovementIdle;
    }

    public void draw(GL gl, GLU glu, boolean focused) {
    }

    public GLTeleportAnim createTeleportAnim(MapLocation src, MapLocation teleportLoc){
	return new GLTeleportAnim(this, teleportLoc);
	
    }

	public GLExplosionAnim createDeathExplosionAnim(boolean isArchon) {
		return new GLExplosionAnim(isArchon);
	}

	public GLMortarAttackAnim createMortarAttackAnim(MapLocation target) {
		return new GLMortarAttackAnim(loc, target);
	}

	public GLEnergonTransferAnim createEnergonTransferAnim(MapLocation loc, RobotLevel height, float amt, boolean isFlux) {
		return new GLEnergonTransferAnim(this,loc,height,amt,isFlux);
	}

	public GLExplosionAnim createMortarExplosionAnim(GLAnimation mortarAttackAnim) {
		return new GLExplosionAnim(false,((GLMortarAttackAnim)mortarAttackAnim).getTargetLoc(), 1.8);
	}
    
    public void setAttacking(MapLocation target, RobotLevel height) {
		super.setAttacking(target, height);
		targetHeight = height;
    }

    @Override
    public void addComponent(ComponentType type) {
        components.add(type);
    }

}
