package battlecode.client.viewer.renderer3d;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Color3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import battlecode.client.util.OBJFile;
import battlecode.client.util.TextureResource;
import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.FluxDepositState;
import battlecode.client.viewer.GameState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.client.viewer.MatchPlayer;
import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.client.viewer.AbstractDrawObject.RobotInfo;
import battlecode.client.viewer.renderer3d.GLDrawObject;
import battlecode.client.viewer.ActionType;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.GameConstants;
import battlecode.common.TerrainTile;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
import battlecode.analysis.AwesomenessSignal;
import battlecode.world.signal.AttackSignal;
import battlecode.world.signal.BroadcastSignal;
import battlecode.world.signal.BytecodesUsedSignal;
import battlecode.world.signal.ControlBitsSignal;
import battlecode.world.signal.DeathSignal;
import battlecode.world.signal.EnergonChangeSignal;
import battlecode.world.signal.IndicatorStringSignal;
import battlecode.world.signal.MatchObservationSignal;
import battlecode.world.signal.MovementOverrideSignal;
import battlecode.world.signal.MovementSignal;
import battlecode.world.signal.SetDirectionSignal;
import battlecode.world.signal.SpawnSignal;

import com.jogamp.opengl.util.texture.Texture;
import battlecode.common.TerrainTile;

public class GLDrawState extends AbstractDrawState<GLDrawObject> {

    //private int mapMinPoints = 0;
    private static class Factory implements GameStateFactory<GLDrawState> {

        public GLDrawState createState(GameMap map) {
            return new GLDrawState(map);
        }

        public GLDrawState cloneState(GLDrawState state) {
            return new GLDrawState(state);
        }

        public void copyState(GLDrawState src, GLDrawState dst) {
            dst.copyStateFrom(src);
        }
    }
    public static final GameStateFactory<GLDrawState> FACTORY = new Factory();
    // awesomeness signals
    Vector2f awesomePoint = null;
    float radius = 0.0f;

    private MapLocation[][] convexHullsA, convexHullsB;

    // texture and obj files
    // archon
    private static final OBJFile archonBody = new OBJFile("art/old/units/NewArchon/Low_poly_archon.bcm");
    private static final String archonBodyTexPathR = "art/old/units/NewArchon/Red/villain_1.jpg";
    private static final String archonBodyTexPathWingR = "art/old/units/NewArchon/Red/villainWing.jpg";
    private static final String archonBodyTexPathB = "art/old/units/NewArchon/Blue/villain_1.jpg";
    private static final String archonBodyTexPathWingB = "art/old/units/NewArchon/Blue/villainWing.jpg";
    
    // scorcher
    private static final OBJFile scorcherBody = new OBJFile("art/old/units/NewChainer/guardbot.bcm");
    private static final String scorcherBodyTexPathR = "art/old/units/NewChainer/Red/guardbot_MeshShape.jpg";
    private static final String scorcherBodyTexPathB = "art/old/units/NewChainer/Blue/guardbot_MeshShape.jpg";

    // disrupter
    private static final OBJFile disrupterBody = new OBJFile("art/old/units/NewTurret/tank.bcm");
    private static final String disrupterBodyTexPathR = "art/old/units/NewTurret/Red/tank.jpg";
    private static final String disrupterBodyTexPathB = "art/old/units/NewTurret/Blue/tank.jpg";

    // scout
    private static final OBJFile scoutBody = new OBJFile("art/old/units/NewWout/wout.bcm");
    private static final String scoutBodyTexPathR = "art/old/units/NewWout/Red/wout_Color.jpg";
    private static final String scoutBodyTexPathB = "art/old/units/NewWout/Blue/wout_Color.jpg";
    
    // soldier
    private static final OBJFile soldierBody = new OBJFile("art/old/units/NewSoldier/soldier.bcm");
    private static final String soldierBodyTexPathR = "art/old/units/NewSoldier/Red/soldior.jpg";//sic
    private static final String soldierBodyTexPathB = "art/old/units/NewSoldier/Blue/soldior.jpg";//sic

    // node
    private static final OBJFile nodeBody = new OBJFile("art/old/botBuildings/tower.bcm");
    private static final String nodeBodyTexPathR = "art/old/botBuildings/Red/scifiBuildings.jpg";
    private static final String nodeBodyTexPathB = "art/old/botBuildings/Blue/scifiBuildings.jpg";

    // power core
    private static final OBJFile coreBody = new OBJFile("art/old/units/building/building.bcm");
    private static final String coreBodyTexPathR = "art/old/units/building/tower_r.jpg";
    private static final String coreBodyTexPathB = "art/old/units/building/tower_b.jpg";

    private static final float MODEL_SCALE = 0.1f;
    private static final float ARCHON_MODEL_SCALE = 0.07f;
    
    // texture cache
    public static TextureResource<String> textureCache = GLGameRenderer.textureCache; //new TextureResource<String>();

    public GLDrawState() {
        groundUnits = new LinkedHashMap<Integer, GLDrawObject>();
        airUnits = new LinkedHashMap<Integer, GLDrawObject>();
        currentRound = -1;
    }

    private GLDrawState(GameMap map) {
        this();
        this.setGameMap(map);
    }

    private GLDrawState(GLDrawState clone) {
        this();
        copyStateFrom(clone);
    }

    protected GLDrawObject createDrawObject(RobotType type, Team team, int id) {
        return new GLDrawObject(type, team, id);
    }

	protected GLDrawObject createDrawObject(GLDrawObject obj) {
		return new GLDrawObject(obj);
	}

    public Vector2f getAwesomePoint() {
        return awesomePoint;
    }

    public float getAwesomeRadius() {
        return radius;
    }

    private synchronized void copyStateFrom(GLDrawState src) {
        groundUnits.clear();
        for (Map.Entry<Integer, GLDrawObject> entry : src.groundUnits.entrySet()) {
            GLDrawObject copy = new GLDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
//            tryAddArchon(copy);
        }
        airUnits.clear();
        for (Map.Entry<Integer, GLDrawObject> entry : src.airUnits.entrySet()) {
            GLDrawObject copy = new GLDrawObject(entry.getValue());
            airUnits.put(entry.getKey(), copy);            
        }

        stats = src.stats;

        if (src.awesomePoint != null) {
            this.awesomePoint = new Vector2f(src.awesomePoint);
            this.radius = src.radius;
        }

        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }
        currentRound = src.currentRound;
    }

    public Void visitAwesomenessSignal(AwesomenessSignal s) {
        MatchPlayer mp = MatchPlayer.getCurrent();

        // get awesomeness point
        awesomePoint = new Vector2f(s.centerX, s.centerY);
        radius = s.radius;

        if (mp != null) {
            final int defaultDelta = 20;
            //final int range = defaultDelta * 2 - defaultDelta / 2;
            //int delta = defaultDelta / 2 + (int)(range * Math.log(s.relativeAwesomeness + 1));

            float stretchedAwesomeness = (s.relativeAwesomeness - 0.5f) * 4;
            int delta = (int) (defaultDelta * Math.pow(2, stretchedAwesomeness));
            mp.setTimeDelta(delta);
            /*System.out.println("awesomeness - rel:" + s.relativeAwesomeness +
            " abs: " + s.totalAwesomeness);*/
        }
        return null;
    }

    public synchronized void apply(RoundStats stats) {
        this.stats = stats;
    }

    // we explicitly call super function because it isn't synchronized
    protected synchronized void updateRound() {
        super.updateRound();
    }

    public GLDrawObject getDrawObject(int id) {
        try {
            return getRobot(id);
        } catch (AssertionError e) {
            return null;
        }
    }

    private String getAvatarPath(RobotInfo ri) {
        String type = ri.type.toString().toLowerCase();
        return "art/" + type + (ri.team == Team.NEUTRAL ? "0" : (ri.team == Team.A ? "1" : "2")) + ".png";
    }


    public void drawArchon(GL2 gl, GLDrawObject obj) {
        String archonBodyTexPath = (obj.getTeam() == Team.A) ? archonBodyTexPathR : archonBodyTexPathB;
        Texture bodyTex = textureCache.getResource(archonBodyTexPath, archonBodyTexPath).tex;

        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL2.GL_BLEND);
        // scale the model
        gl.glScalef(ARCHON_MODEL_SCALE, ARCHON_MODEL_SCALE, ARCHON_MODEL_SCALE);
        // gl.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        archonBody.draw(gl);

        gl.glEnable(GL2.GL_BLEND);
    }

    public void drawScorcher(GL2 gl, GLDrawObject obj) {
        String scorcherBodyTexPath = (obj.getTeam() == Team.A) ? scorcherBodyTexPathR : scorcherBodyTexPathB;
        Texture bodyTex = textureCache.getResource(scorcherBodyTexPath, scorcherBodyTexPath).tex;

        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL2.GL_BLEND);
        // scale the model
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        // gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        scorcherBody.draw(gl);

        gl.glEnable(GL2.GL_BLEND);

    }

    public void drawdisrupter(GL2 gl, GLDrawObject obj) {
        String disrupterBodyTexPath = (obj.getTeam() == Team.A) ? disrupterBodyTexPathR : disrupterBodyTexPathB;
        Texture bodyTex = textureCache.getResource(disrupterBodyTexPath, disrupterBodyTexPath).tex;
        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL2.GL_BLEND);
        // scale the model
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        //  gl.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        disrupterBody.draw(gl);
        gl.glEnable(GL2.GL_BLEND);
    }

    public void drawSoldier(GL2 gl, GLDrawObject obj) {
        String soldierBodyTexPath = (obj.getTeam() == Team.A) ? soldierBodyTexPathR : soldierBodyTexPathB;

        Texture bodyTex = textureCache.getResource(soldierBodyTexPath, soldierBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        gl.glTranslatef(-0.1f, 0.1f, 0.0f);
        gl.glDisable(GL2.GL_BLEND);
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        //   gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);

        soldierBody.draw(gl);
        gl.glEnable(GL2.GL_BLEND);
    }

    public void drawScout(GL2 gl, GLDrawObject obj) {

        // now draw the worker
        String scoutBodyTexPath = (obj.getTeam() == Team.A) ? scoutBodyTexPathR : scoutBodyTexPathB;

        Texture bodyTex = textureCache.getResource(scoutBodyTexPath, scoutBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        gl.glTranslatef(0.f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE * 1.f, MODEL_SCALE * 1.f, MODEL_SCALE * 1.f);
        gl.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        scoutBody.draw(gl);
    }

    public void drawNode(GL2 gl, GLDrawObject obj) {
        String nodeBodyTexPath = (obj.getTeam() == Team.A) ? nodeBodyTexPathR : nodeBodyTexPathB;

        Texture bodyTex = textureCache.getResource(nodeBodyTexPath, nodeBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        //  gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);

        gl.glDisable(GL2.GL_BLEND);
        // this model uses different a different scale because it's from 2008
        gl.glTranslatef(0.f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f);
        //gl.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        nodeBody.draw(gl);
        gl.glEnable(GL2.GL_BLEND);
    }
    
    public void drawCore(GL2 gl, GLDrawObject obj) {
        String coreBodyTexPath = (obj.getTeam() == Team.A) ? coreBodyTexPathR : coreBodyTexPathB;

        Texture bodyTex = textureCache.getResource(coreBodyTexPath, coreBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind(gl);
        }
        //  gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);

        gl.glDisable(GL2.GL_BLEND);
        // this model uses different a different scale because it's from 2008
        gl.glTranslatef(0.f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f);
        //gl.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        coreBody.draw(gl);
        gl.glEnable(GL2.GL_BLEND);
    }

    /**
     * Draws the current game state. This method is always called from the
     * Swing event-dispatch thread, and in particular blocks calls to
     * updateRound.
     * @param g2 The graphics context, transformed to MapLocation-space
     * @param debug The debug state, including MapLocation-space mouse state
     */
    public synchronized void draw(GL2 gl, GLU glu, GLGameRenderer r, DebugState debug, GLDrawMap map, MapLocation origin) {
        final GLUquadric quadric = glu.gluNewQuadric();

        // draw the flux deposits
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // draw the units
        Color3f energonColor = new Color3f();
		Color3f fluxColor = new Color3f();
        gl.glLineWidth(2.0f);

        Iterable<Map.Entry<Integer, GLDrawObject>> drawableSet = getDrawableSet();
        if (drawableSet == null) {
            return;
        }

		// power grid
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glLineWidth(4.0f);
		gl.glBegin(GL2.GL_LINES);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		for (Link l : links) {
			if (l.connected[0]) {
				if (l.connected[1]) {
					gl.glColor4f(0.75f, 0.0f, 0.75f, 1.0f); // both
				} else {
					gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f); // A
				}
			} else {
				if (l.connected[1]) {
					gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f); // B
				} else {
					gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f); // none
				}
			}
			gl.glVertex3f(l.from.x - origin.x + 0.5f, 0.01f, l.from.y - origin.y + 0.5f);
			gl.glVertex3f(l.to.x - origin.x + 0.5f, 0.01f, l.to.y - origin.y + 0.5f);
		}
		gl.glEnd();
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glLineWidth(1.0f);

        for (Map.Entry<Integer, GLDrawObject> entry : drawableSet) {
            int id = entry.getKey();
            GLDrawObject obj = entry.getValue();

            float oriX = origin.x;
            float oriY = origin.y;

            float x = obj.getDrawX() - oriX;
            float y = obj.getDrawY() - oriY;

            gl.glPushMatrix();
            // first translate to position
            gl.glTranslatef(x + 0.5f, 0.0f, y + 0.5f);

            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

            gl.glLineWidth(4.0f);

            float extraDist = 0.1f;
            float maxHeight = GLDrawMap.MAP_SCALE * 32;
            {
                if (obj.getMovementAction() == ActionType.MOVING) {
                    float delay = 0;
                    float distFrac = (float) (obj.timeUntilIdle() + 1) / delay;

                    if (distFrac > 1.0f) {
                        distFrac = 1.0f;
                    } else if (distFrac < 0.0f) {
                        distFrac = 0.0f;
                    }
                    distFrac = 1.0f - distFrac;

                    float actualZ;

                    //distfrac is the fraction of the total time traveled

                    MapLocation currentSquare = obj.getLocation().subtract(obj.getDirection());
                    //This is used to get the z position of the obj location
                    MapLocation frontSquare = obj.getLocation();

                    float srcZ = map.getTerrainHeight(x, y);

                    float destZ = map.getTerrainHeight((frontSquare.x - oriX), (frontSquare.y - oriY));
                    float deltaZ = (destZ - srcZ);

                    actualZ = srcZ + distFrac * deltaZ;
                    //obj.setString(0, new String("S " + x + " " + y + " " + srcZ + " " + destZ + " " + distFrac));

                    gl.glTranslatef(0.0f, actualZ + extraDist, 0.0f);
                } else {
                    gl.glTranslatef(0.0f, map.getTerrainHeight(x, y) + extraDist, 0.0f);
                    //	obj.setString(0, new String("N " + map.getTerrainHeight(x, y) + " " + x + " " + y ));
                }

                //This is to get the units to tilt properly
                float xPosZ = map.getTerrainHeight(x + 1, y);
                float xNegZ = map.getTerrainHeight(x - 1, y);
                float yPosZ = map.getTerrainHeight(x, y + 1);
                float yNegZ = map.getTerrainHeight(x, y - 1);
            }

            // draw selection box
            if (id == debug.getFocusID()) {
                gl.glLineWidth(2.0f);
                gl.glPushMatrix();
                gl.glScalef(0.5f, 0.5f, 0.5f);
                gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                drawBox(gl);
                gl.glPopMatrix();
            }

            // draw channeler drain
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glDisable(GL2.GL_LIGHTING);

            // draw message broadcast
            if (obj.getBroadcast() != 0x00 && RenderConfiguration.showBroadcast()) {

                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glPushMatrix();
                gl.glTranslatef(0.0f, 0.001f, 0.0f);
                gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                double drdR = obj.broadcastRadius() * 0.05;
                for (int i = 0; i < 20; i++) {
                    if ((obj.getBroadcast() & (1 << i)) != 0x00) {
                        double rad = i * drdR;
                        gl.glColor4f(0.0f, 0.75f, 0.75f, 0.05f);
                        glu.gluDisk(quadric, 0.0f, rad, 16, 1);
                    }
                }
                gl.glPopMatrix();

                gl.glDisable(GL2.GL_BLEND);
            }
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_DEPTH_TEST);

            gl.glLineWidth(4.0f);
            // draw energon transfer animation
            GLEnergonTransferAnim transferAnim = obj.getEnergonTransferAnim();
            if (transferAnim != null) {
                transferAnim.draw(gl, map, origin);
            }

            gl.glDisable(GL2.GL_LIGHTING);
            // draw crosshair if shooting
            if (obj.getAttackAction() == ActionType.ATTACKING) {
                boolean drawArch = true;
                MapLocation target = obj.getTargetLoc();
				// aha, target is sometimes null!! I'm not sure why, though ~shewu
				// BECAUSE SCORCHERS ~shewu
				if (target != null) {
					float tx = target.x - origin.x;
					float ty = target.y - origin.y;
					float tz = map.getTerrainHeight(tx + 0.5f, ty + 0.5f);
					float deltax = tx - x;
					float deltay = ty - y;
					float deltaz = tz;

					deltaz -= map.getTerrainHeight(x, y);

					gl.glEnable(GL2.GL_BLEND);
					gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
					gl.glPushMatrix();
					gl.glTranslatef(deltax, deltaz, deltay);
					if (obj.getTeam() == Team.A) {
						gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
					} else {
						gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
					}
					gl.glLineWidth(2.0f);
					float angleDelta = (float) 3.14159 / 8;
					float circleScale = .5f;
					gl.glBegin(GL2.GL_LINE_LOOP);
					for (float angle = 0; angle < 3.14159 * 2; angle += angleDelta) {
						gl.glVertex3f((float) Math.cos(angle) * circleScale, 0.01f, (float) Math.sin(angle) * circleScale);
					}
					gl.glEnd();

					gl.glBegin(GL2.GL_LINES);
					angleDelta = (float) 3.14159 / 2;
					for (float angle = 0; angle < 3.14159 * 2; angle += angleDelta) {
						gl.glVertex3f((float) (Math.cos(angle) * circleScale * 1.5), 0.01f, (float) (Math.sin(angle) * circleScale * 1.5));
						gl.glVertex3f((float) (Math.cos(angle) * circleScale / 2), 0.01f, (float) (Math.sin(angle) * circleScale / 2));
					}
					gl.glEnd();

					gl.glLineWidth(1.0f);

					gl.glPopMatrix();
					gl.glDisable(GL2.GL_BLEND);

					gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
					if (obj.getTeam() == Team.A) {
						gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
					} else {
						gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
					}

					gl.glLineWidth(1.0f);
					gl.glBegin(GL2.GL_LINES);
					gl.glVertex3f(0.0f, 0.5f, 0.0f);
					gl.glVertex3f(deltax, deltaz, deltay);
					gl.glEnd();
				} else { // target null, scorcher
					// we need to draw a semicircle; approximate with linestrips
//					final float radius = (float)Math.sqrt(RobotType.SCORCHER.attackRadiusMaxSquared);
					final float angleDelta = (float) Math.PI / 32;
					final float circleScale = 5.f;

					if (obj.getTeam() == Team.A) {
						gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
					} else {
						gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
					}
					gl.glLineWidth(2.0f);

					gl.glBegin(GL2.GL_LINE_LOOP);
					for (double angle = -Math.PI/2.0f; angle <= Math.PI/2.0f + angleDelta; angle += angleDelta) {
						gl.glVertex3f((float)(Math.cos(angle) * radius), 0.0f, (float)(Math.sin(angle) * radius));
						gl.glVertex3f((float)(Math.cos(angle) * radius), 0.0f, (float)(Math.sin(angle) * radius));
					}
					gl.glEnd();

					gl.glLineWidth(1.0f);
				}
            }

						// rotate into position
						final Vector3f startNormal = new Vector3f(0.0f, 1.0f, 0.0f);
						Vector3f newNormal = map.getTileNormal(x + 0.5f, y + 0.5f);
						if (!newNormal.epsilonEquals(startNormal, 0.00001f)) {
								Vector3f axis = new Vector3f();
								axis.cross(newNormal, startNormal);
								axis.normalize();
								float angle = -newNormal.angle(startNormal);
								gl.glRotatef(angle * 180.0f / (float) Math.PI, axis.x, axis.y, axis.z);
						}


            {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glDisable(GL2.GL_CULL_FACE);
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glPushMatrix();
                gl.glTranslatef(0.0f, 0.05f, 0.0f);
                gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                if (obj.getTeam() == Team.A) {
                    gl.glColor4f(0.2f, 0.0f, 0.0f, 0.5f);
                } else {
                    gl.glColor4f(0.0f, 0.0f, 0.2f, 0.5f);
                }
                float diskSize = 0.25f;
                glu.gluCylinder(quadric, diskSize, 0.0f, 0.3f, 16, 1);
                glu.gluCylinder(quadric, diskSize + 0.1f, 0.0f, 0.3f, 16, 1);
                gl.glPopMatrix();


                gl.glDisable(GL2.GL_BLEND);
                gl.glEnable(GL2.GL_CULL_FACE);

            }

            // draw energon
			final float efXStart = -0.5f;
			final float efXEnd = 0.5f;
			final float efY = 0.125f;
			final float efZStart = 0.5f;
			final float efZWidth = 0.25f;
            if (RenderConfiguration.showEnergon()) {
                float frac = (float) (obj.getEnergon() / obj.getType().maxEnergon);
                final Color3f max = new Color3f(0.0f, 1.0f, 0.0f);
                final Color3f min = new Color3f(1.0f, 0.0f, 0.0f);
                energonColor.interpolate(min, max, frac);

				gl.glBegin(GL2.GL_QUADS);
				gl.glNormal3f(0.0f, 1.0f, 0.0f);
				gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
				gl.glVertex3f(efXStart + frac, efY, efZStart);
				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth);
				gl.glVertex3f(efXEnd, efY, efZStart + efZWidth);
				gl.glVertex3f(efXEnd, efY, efZStart);

				gl.glColor4f(energonColor.x, energonColor.y, energonColor.z, 1.0f);
				gl.glVertex3f(efXStart, efY, efZStart);
				gl.glVertex3f(efXStart, efY, efZStart + efZWidth);
				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth);
				gl.glVertex3f(efXStart + frac, efY, efZStart);
				gl.glEnd();
            }

			// draw flux
//			if (RenderConfiguration.showFlux() && obj.getType() != RobotType.TOWER) {
//				float frac = (float) (obj.getFlux() / obj.getType().maxFlux);
//				final Color3f max = new Color3f(0.0f, 0.0f, 1.0f);
//				final Color3f min = new Color3f(0.0f, 0.0f, 0.5f);
//				final float z = 0.7f;
//				fluxColor.interpolate(min, max, frac);
//
//				gl.glBegin(GL2.GL_QUADS);
//				gl.glNormal3f(0.0f, 1.0f, 0.0f);
//				gl.glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
//				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth);
//				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth * 2.0f);
//				gl.glVertex3f(efXEnd, efY, efZStart + efZWidth * 2.0f);
//				gl.glVertex3f(efXEnd, efY, efZStart + efZWidth);
//
//				gl.glColor4f(fluxColor.x, fluxColor.y, fluxColor.z, 1.0f);
//				gl.glVertex3f(efXStart, efY, efZStart + efZWidth);
//				gl.glVertex3f(efXStart, efY, efZStart + efZWidth * 2.0f);
//				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth * 2.0f);
//				gl.glVertex3f(efXStart + frac, efY, efZStart + efZWidth);
//				gl.glEnd();
//			}

            // disable lighting in ortho mode
            if (r.getCamera().isOrtho()) {
                gl.glDisable(GL2.GL_LIGHTING);
            } else {
                gl.glEnable(GL2.GL_LIGHT0);
                gl.glEnable(GL2.GL_LIGHTING);
            }

            // rotate to direction
            float angle = (obj.getDirection().ordinal() - 2) * 45.0f;
            gl.glRotatef(angle, 0.0f, -1.0f, 0.0f);
            /*if(obj.getTeam() == Team.A) {
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            } else if(obj.getTeam() == Team.B) {
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            } else */

            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);



//            if (GLGameRenderer.USE_MODELS) {
//
//                switch (obj.getType()) {
//                    case ARCHON:
//                        drawArchon(gl, obj);
//                    case SCORCHER:
//                        drawScorcher(gl, obj);
//                    case DISRUPTER:
//                        drawdisrupter(gl, obj);
//                    case SCOUT:
//                        drawScout(gl, obj);
//                    case SOLDIER:
//                        drawSoldier(gl, obj);
//                    case TOWER:
//                        drawNode(gl, obj);
//                    default:
//                        drawRobot(gl, obj);
//                }
//            } else {
//                drawRobot(gl, obj);
//            }

            gl.glPopMatrix();
        }


        gl.glDisable(GL2.GL_TEXTURE_2D);

        // draw explosions
        gl.glTranslatef(-origin.x, 0.0f, -origin.y);
        for (Map.Entry<Integer, GLDrawObject> entry : getDrawableSet()) {
            int id = entry.getKey();
            GLDrawObject obj = entry.getValue();

            float x = obj.getDrawX() - origin.x;
            float y = obj.getDrawY() - origin.y;

            gl.glPushMatrix();
						gl.glTranslatef(obj.getDrawX(), map.getTerrainHeight(x + 0.5f, y + 0.5f), obj.getDrawY());

            if (obj.getExplosionAnim() != null) {
                obj.getExplosionAnim().draw(gl, glu);
            }

            if (obj.getTeleportAnim() != null) {
                obj.getTeleportAnim().draw(gl, glu);
            }

            gl.glPopMatrix();


            if (obj.getMortarExplosionAnim() != null && obj.getMortarAttackTarget() != null) {
                MapLocation tar = obj.getMortarAttackTarget();
                gl.glPushMatrix();
                float tx = tar.x - origin.x + 0.5f;
                float ty = tar.y - origin.y + 0.5f;
                gl.glTranslatef(tar.x, map.getTerrainHeight(tx, ty), tar.y);
                obj.getMortarExplosionAnim().draw(gl, glu);
                gl.glPopMatrix();
            }

            if (obj.getMortarAttackAnim() != null) {
                obj.getMortarAttackAnim().draw(gl, glu, map, origin);
            }

        }

        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_BLEND);
    }

    public void doSelection(GL2 gl, GLU glu, GLGameRenderer r, GLDrawMap map, MapLocation origin) {
        final GLUquadric quadric = glu.gluNewQuadric();

        Iterable<Map.Entry<Integer, GLDrawObject>> drawableSet = getDrawableSet();
        if (drawableSet == null) {
            return;
        }

        // initialize name stack
        gl.glInitNames();

        // push base name
        gl.glPushName(0);

        for (Map.Entry<Integer, GLDrawObject> entry : drawableSet) {
            int id = entry.getKey();
            GLDrawObject obj = entry.getValue();

            // set the object id as the gl name
            gl.glLoadName(id);

            float x = obj.getDrawX() - origin.x;
            float y = obj.getDrawY() - origin.y;

            gl.glPushMatrix();
            gl.glTranslatef(x + 0.5f, 0.0f, y + 0.5f);
						gl.glTranslatef(0.0f, map.getTerrainHeight(x + 0.5f, y + 0.5f), 0.0f);

            double rad = 0.5f;
            //if (obj.getType() == RobotType.ARCHON)
            //    rad = 0.6f;

            glu.gluSphere(quadric, rad, 5, 5);
            gl.glPopMatrix();
        }

        gl.glPopName();
    }

    private void drawRobot(GL2 gl, GLDrawObject obj) {
        String path = getAvatarPath(new RobotInfo(obj.getType(), obj.getTeam()));
        Texture tex = textureCache.getResource(path, path).tex;

        if (tex != null) {
            tex.bind(gl);
        }

        // draw robot
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-0.5f, 0.1f, -0.5f);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-0.5f, 0.1f, 0.5f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(0.5f, 0.1f, 0.5f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(0.5f, 0.1f, -0.5f);
        gl.glEnd();
    }

    private void drawBox(GL2 gl) {
        // points are
        final float[] points = new float[]{
            // top
            -1, 1, -1,
            1, 1, -1,
            1, 1, 1,
            -1, 1, 1,
            // bottom
            -1, -1, -1,
            1, -1, -1,
            1, -1, 1,
            -1, -1, 1
        };

        // indices for box
        int indices[] = new int[]{
            0, 1, 2, 3, 0, 4, 5, 1, 5, 6, 2, 6, 7, 3, 7, 4
        };

        gl.glBegin(GL2.GL_LINE_STRIP);
        final int VEC_SIZE = 3;
        for (int i = 0; i < indices.length; i++) {
            gl.glVertex3fv(points, indices[i] * VEC_SIZE);
        }
        gl.glEnd();
    }

    public void unloadTextures() {
        // unload all textures
        textureCache.unloadAll();
    }
}
