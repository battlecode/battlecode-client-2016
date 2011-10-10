package battlecode.client.viewer.renderer3d;

import java.awt.Color;





import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
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

import com.sun.opengl.util.texture.Texture;
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
    //private Map<Integer, DrawUpgrade> upgrades;
    //private Map<Integer, GLDrawObject> groundUnits;
    //private Map<Integer, GLDrawObject> airUnits;
    //private List<GLDrawObject> archonsA;
    //private List<GLDrawObject> archonsB;
    private MapLocation[][] convexHullsA, convexHullsB;
    // lists to help separate units
	/*private List<GLDrawObject> archons;
    private List<GLDrawObject> snipers;
    private List<GLDrawObject> mortars;
    private List<GLDrawObject> soldiers;
    private List<GLDrawObject> scouts;
    private List<GLDrawObject> bombers;*/
    // texture paths
    // archon
    private static final String archonBodyTexPathR = "art/units/NewArchon/Red/villain_1.jpg";
    private static final String archonBodyTexPathWingR = "art/units/NewArchon/Red/villainWing.jpg";
    private static final String archonBodyTexPathB = "art/units/NewArchon/Blue/villain_1.jpg";
    private static final String archonBodyTexPathWingB = "art/units/NewArchon/Blue/villainWing.jpg";
    // chainer
    private static final String chainerBodyTexPathR = "art/units/NewChainer/Red/guardbot_MeshShape.jpg";
    private static final String chainerBodyTexPathB = "art/units/NewChainer/Blue/guardbot_MeshShape.jpg";
    //  private static final String channelerWingTexPathB = "art/units/channeler/channeler_wing_b.png";
    // private static final String channelerBodyTexPathR = "art/units/channeler/channeler_body_r.png";
    // private static final String channelerBodyTexPathB = "art/units/channeler/channeler_body_b.png";
    // turret
    private static final String sniperTurretTexPathR = "art/units/NewTurret/Red/tank.jpg";
    private static final String sniperTurretTexPathB = "art/units/NewTurret/Blue/tank.jpg";
    //   private static final String sniperTurretTexPathB = "art/units/sniper/sniper_turret_b.png";
    // private static final String sniperBodyTexPathR = "art/units/sniper/sniper_body_r.png";
    //private static final String sniperBodyTexPathB = "art/units/sniper/sniper_body_b.png";
    // wout
    private static final String woutBodyTexPathR = "art/units/NewWout/Red/wout_Color.jpg";
    private static final String woutBodyTexPathB = "art/units/NewWout/Blue/wout_Color.jpg";
    // private static final String scoutBodyTexPathB = "art/units/scout/scout_b.png";
    // soldier
    private static final String soldierBodyTexPathR = "art/units/NewSoldier/Red/soldior.jpg";//sic
    private static final String soldierBodyTexPathB = "art/units/NewSoldier/Blue/soldior.jpg";//sic
    //   private static final String soldierBodyTexPathB = "art/units/soldier/soldier_body_b.png";
    // worker
    //   private static final String workerBodyTexPathR = "art/units/worker/worker_body_r.png";
    // private static final String workerBodyTexPathB = "art/units/worker/worker_body_b.png";
    /*private static final String workerDroneTexPathR = "art/units/worker/soldier_body_r.png";
    private static final String workerDroneTexPathB = "art/units/worker/soldier_body_b.png";*/
    private static final String buildingBodyTexPathR = "art/botBuildings/Red/scifiBuildings.jpg";
    private static final String buildingBodyTexPathB = "art/botBuildings/Blue/scifiBuildings.jpg";
    // texture files for the objects
	/*private TextureFile towerN, towerR, towerB;
    private TextureFile archonTurretR, archonTurretB, archonBodyR, archonBodyB;
    private TextureFile mortarTurretR, mortarTurretB, mortarBodyR, mortarBodyB;
    private TextureFile sniperTurretR, sniperTurretB, sniperBodyR, sinperBodyB;
    private TextureFile scoutR, scoutB;
    private TextureFile bomberR, bomberB;
    private TextureFile soldierR, soldierB;*/
    // texture cache
    public static TextureResource<String> textureCache = GLGameRenderer.textureCache; //new TextureResource<String>();
    // fast forwarding
    private boolean fastforward = false;
    // map origin and block deltas

    public GLDrawState() {

        groundUnits = new LinkedHashMap<Integer, GLDrawObject>();
        airUnits = new LinkedHashMap<Integer, GLDrawObject>();
        //archonsA = new ArrayList<GLDrawObject>(4);
        //archonsB = new ArrayList<GLDrawObject>(4);
        //upgrades = new HashMap<Integer, DrawUpgrade>();
        fluxDeposits = new LinkedHashMap<Integer, FluxDepositState>();
        currentRound = -1;
        // HACK: from DrawMap update hack
        /*blockNumber = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++)
        blockNumber[i][j] = 0;
        }*/
        convexHullsA = new MapLocation[0][];
        convexHullsB = new MapLocation[0][];
        /*archons = new LinkedList<GLDrawObject>();
        snipers = new LinkedList<GLDrawObject>();
        mortars = new LinkedList<GLDrawObject>();
        soldiers = new LinkedList<GLDrawObject>();
        scouts = new LinkedList<GLDrawObject>();
        bombers = new LinkedList<GLDrawObject>();*/
    }

    private GLDrawState(GameMap map) {

        this();
        this.setGameMap(map);
        //this.mapMinPoints = map.getMinPoints();
    }

    private GLDrawState(GLDrawState clone) {
        this();
        copyStateFrom(clone);
    }

    protected GLDrawObject createDrawObject(RobotType type, Team team) {
        return new GLDrawObject(type, team);
    }

    public Vector2f getAwesomePoint() {
        return awesomePoint;
    }

    public MapLocation[][] getConvexHullsA() {
        return convexHullsA;
    }

    public MapLocation[][] getConvexHullsB() {
        return convexHullsB;
    }

    public float getAwesomeRadius() {
        return radius;
    }

    private void clearAccelerators() {
        /*	snipers.clear();
        mortars.clear();
        soldiers.clear();
        scouts.clear();
        bombers.clear();
        archons.clear();*/
    }

    private void addAcceleratorUnit(GLDrawObject o) {
        /*if(o.getType() == RobotType.SNIPER) {
        snipers.add(o);
        } else if(o.getType() == RobotType.SOLDIER) {
        soldiers.add(o);
        } else if(o.getType() == RobotType.MORTAR) {
        mortars.add(o);
        } else if(o.getType() == RobotType.SCOUT) {
        scouts.add(o);
        } else if(o.getType() == RobotType.BOMBER) {
        bombers.add(o);
        } else if(o.getType() == RobotType.ARCHON) {
        archons.add(o);
        }*/
    }

    private synchronized void copyStateFrom(GLDrawState src) {
        groundUnits.clear();
        //archonsA.clear();
        //archonsB.clear();
        clearAccelerators();
        for (Map.Entry<Integer, GLDrawObject> entry : src.groundUnits.entrySet()) {
            GLDrawObject copy = new GLDrawObject(entry.getValue());
            groundUnits.put(entry.getKey(), copy);
            addAcceleratorUnit(copy);
        }
        airUnits.clear();
        for (Map.Entry<Integer, GLDrawObject> entry : src.airUnits.entrySet()) {
            GLDrawObject copy = new GLDrawObject(entry.getValue());
            airUnits.put(entry.getKey(), copy);
            tryAddArchon(copy);
            addAcceleratorUnit(copy);
        }
        fluxDeposits.clear();
        for (Map.Entry<Integer, FluxDepositState> entry : src.fluxDeposits.entrySet()) {
            fluxDeposits.put(entry.getKey(), new FluxDepositState(entry.getValue()));
        }

        stats = src.stats;

        if (src.awesomePoint != null) {
            this.awesomePoint = new Vector2f(src.awesomePoint);
            this.radius = src.radius;
        }

        // HACK: from DrawMap update hack
        /*blockNumber = new int[100][100];
        for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
        blockNumber[i][j] = src.blockNumber[i][j];
        }
        }*/

        // mapMinPoints = src.mapMinPoints;

        if (src.gameMap != null) {
            gameMap = src.gameMap;
        }
        currentRound = src.currentRound;
        convexHullsA = new MapLocation[src.convexHullsA.length][];
        if (convexHullsA.length > 0) {
            for (int i = 0; i < convexHullsA.length; i++) {
                convexHullsA[i] = new MapLocation[src.convexHullsA[i].length];
                System.arraycopy(src.convexHullsA[i], 0, convexHullsA[i], 0, convexHullsA[i].length);
            }
        }

        convexHullsB = new MapLocation[src.convexHullsB.length][];
        if (convexHullsB.length > 0) {
            for (int i = 0; i < convexHullsB.length; i++) {
                convexHullsB[i] = new MapLocation[src.convexHullsB[i].length];
                System.arraycopy(src.convexHullsB[i], 0, convexHullsB[i], 0, convexHullsB[i].length);
            }
        }
    }

    public Void visitAwesomenessSignal(AwesomenessSignal s) {
        MatchPlayer mp = MatchPlayer.getCurrent();

        // get awesomeness point
        awesomePoint = new Vector2f(s.centerX, s.centerY);
        radius = s.radius;

        if (mp != null) {
            final int defaultDelta = 20000000;
            //final int range = defaultDelta * 2 - defaultDelta / 2;
            //int delta = defaultDelta / 2 + (int)(range * Math.log(s.relativeAwesomeness + 1));

            float stretchedAwesomeness = (s.relativeAwesomeness - 0.5f) * 4;
            int delta = (int) (defaultDelta * Math.pow(2, stretchedAwesomeness));
            if (!fastforward) {
                mp.setTimeDelta(delta);
            }
            /*System.out.println("awesomeness - rel:" + s.relativeAwesomeness +
            " abs: " + s.totalAwesomeness);*/
        }
        return null;
    }

    public synchronized void apply(RoundStats stats) {
        this.stats = stats;
    }

    protected void mineFlux(GLDrawObject obj) {
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

    //public int getMapMinPoints() {
    //     return mapMinPoints;
    // }
    private String getAvatarPath(RobotInfo ri) {
        String type = ri.type.toString().toLowerCase();
        return "art/" + type + (ri.team == Team.NEUTRAL ? "0" : (ri.team == Team.A ? "1" : "2")) + ".png";
    }
    // archon
    private static final OBJFile archonBody = new OBJFile("art/units/NewArchon/Low_poly_archon.bcm");//NewArchon/archon.obj");
    //private static final OBJFile archonFluxBurn = new OBJFile("art/units/archon/flux_burn.obj");
    // chainer
    private static final OBJFile chainerBody = new OBJFile("art/units/NewChainer/guardbot.bcm");
    // private static final OBJFile channelerWing = new OBJFile("art/units/channeler/channeler_wing.obj");
    // turret
    private static final OBJFile turretBody = new OBJFile("art/units/NewTurret/tank.bcm");
    // private static final OBJFile sniperTurret = new OBJFile("art/units/sniper/sniper_turret.obj");
    // wout
    private static final OBJFile woutBody = new OBJFile("art/units/NewWout/wout.bcm");
    //private static final OBJFile scoutRotor = new OBJFile("art/units/scout/scout_rotor.bcm");
    // soldier
    private static final OBJFile soldierBody = new OBJFile("art/units/NewSoldier/soldier.bcm");
    // wout?
//    private static final OBJFile workerBody = new OBJFile("art/units/worker/worker_body.obj");
    //  private static final OBJFile workerDrone = new OBJFile("art/units/worker/worker_drone.obj");
    private static final OBJFile towerBody = new OBJFile("art/botBuildings/tower.bcm");
    private static final OBJFile communicationsBody = new OBJFile("art/botBuildings/communications.bcm");
    private static final OBJFile processingplantBody = new OBJFile("art/botBuildings/processingplant.bcm");
    private static final float MODEL_SCALE = 0.1f;
    private static final float ARCHON_MODEL_SCALE = 0.07f;

    public void drawArchon(GL gl, GLDrawObject obj) {
        String archonBodyTexPath = (obj.getTeam() == Team.A) ? archonBodyTexPathR : archonBodyTexPathB;
        Texture bodyTex = textureCache.getResource(archonBodyTexPath, archonBodyTexPath).tex;

        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL.GL_BLEND);
        // scale the model
        gl.glScalef(ARCHON_MODEL_SCALE, ARCHON_MODEL_SCALE, ARCHON_MODEL_SCALE);
        // gl.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind();
        }
        archonBody.draw(gl);

        gl.glEnable(GL.GL_BLEND);
    }

    public void drawChainer(GL gl, GLDrawObject obj) {

        // String channelerWingTexPath = (obj.getTeam() == Team.A) ? channelerWingTexPathR : channelerWingTexPathB;
        String chainerBodyTexPath = (obj.getTeam() == Team.A) ? chainerBodyTexPathR : chainerBodyTexPathB;

        Texture bodyTex = textureCache.getResource(chainerBodyTexPath, chainerBodyTexPath).tex;
        // Texture wingTex = textureCache.getResource(channelerWingTexPath, channelerWingTexPath).tex;

        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL.GL_BLEND);
        // scale the model
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        // gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind();
        }
        chainerBody.draw(gl);
        /*
        gl.glTranslatef(5000.0f * MODEL_SCALE, 2500.0f * MODEL_SCALE, -7800.0f * MODEL_SCALE);
        if (wingTex != null)
        wingTex.bind();
        channelerWing.draw(gl);

        gl.glTranslatef(-10000.0f * MODEL_SCALE, 0.0f, 0.0f);
        gl.glScalef(-1.0f, 1.0f, 1.0f);
        gl.glFrontFace(GL.GL_CW);
        channelerWing.draw(gl);
        gl.glFrontFace(GL.GL_CCW);
         */

        gl.glEnable(GL.GL_BLEND);

    }

    public void drawTurret(GL gl, GLDrawObject obj) {
        String sniperTurretTexPath = (obj.getTeam() == Team.A) ? sniperTurretTexPathR : sniperTurretTexPathB;
        //  String sniperBodyTexPath = (obj.getTeam() == Team.A) ? sniperBodyTexPathR : sniperBodyTexPathB;

        Texture bodyTex = textureCache.getResource(sniperTurretTexPath, sniperTurretTexPath).tex;
        //   Texture turretTex = textureCache.getResource(sniperTurretTexPath, sniperTurretTexPath).tex;

        // translate up to normal level
        gl.glTranslatef(0.0f, 0.1f, 0.0f);
        gl.glDisable(GL.GL_BLEND);
        // scale the model
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        //  gl.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f);
        if (bodyTex != null) {
            bodyTex.bind();
        }
        turretBody.draw(gl);
        /*
        gl.glTranslatef(0.0f, 10000.0f * MODEL_SCALE, 10000.0f * MODEL_SCALE);
        if (turretTex != null)
        turretTex.bind();
        sniperTurret.draw(gl);
        gl.glEnable(GL.GL_BLEND);
         */
        /*} else if (obj.getType() == RobotType.WOUT2XXX) {

        String scoutBodyTexPath = (obj.getTeam() == Team.A) ? scoutBodyTexPathR : scoutBodyTexPathB;

        Texture bodyTex = textureCache.getResource(scoutBodyTexPath, scoutBodyTexPath).tex;

        if (bodyTex != null)
        bodyTex.bind();
        gl.glScalef(2.0f * MODEL_SCALE, 2.0f * MODEL_SCALE, 2.0f * MODEL_SCALE);
        gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
        scoutBody.draw(gl);

        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glTranslatef(0.0f, 5500.0f * MODEL_SCALE, 0.0f);
        float time = (System.currentTimeMillis() % 1000) / 1000.0f * 360.0f;
        gl.glRotatef(time, 0.0f, 1.0f, 0.0f);
        gl.glScalef(0.5f, 0.5f, 0.5f);
        scoutRotor.draw(gl);*/
    }

    public void drawSoldier(GL gl, GLDrawObject obj) {
        String soldierBodyTexPath = (obj.getTeam() == Team.A) ? soldierBodyTexPathR : soldierBodyTexPathB;

        Texture bodyTex = textureCache.getResource(soldierBodyTexPath, soldierBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind();
        }
        gl.glTranslatef(-0.1f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        //   gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);

        soldierBody.draw(gl);
    }

    public void drawWout(GL gl, GLDrawObject obj) {

        // now draw the worker
        String woutBodyTexPath = (obj.getTeam() == Team.A) ? woutBodyTexPathR : woutBodyTexPathB;

        Texture bodyTex = textureCache.getResource(woutBodyTexPath, woutBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind();
        }
        gl.glTranslatef(0.f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE * 1.f, MODEL_SCALE * 1.f, MODEL_SCALE * 1.f);
        gl.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        woutBody.draw(gl);
    }

    public void scaleBuilding(GL gl, GLDrawObject obj) {
        String buildingBodyTexPath = (obj.getTeam() == Team.A) ? buildingBodyTexPathR : buildingBodyTexPathB;

        Texture bodyTex = textureCache.getResource(buildingBodyTexPath, buildingBodyTexPath).tex;

        if (bodyTex != null) {
            bodyTex.bind();
        }
        //  gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);

        gl.glDisable(GL.GL_BLEND);
        // this model uses different a different scale because it's from 2008
        gl.glTranslatef(0.f, 0.1f, 0.0f);
        gl.glScalef(MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f, MODEL_SCALE * 1.2f);
        //gl.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
    }

    public void drawTeleporter(GL gl, GLDrawObject obj) {
        scaleBuilding(gl, obj);
        processingplantBody.draw(gl);
    }

    public void drawAura(GL gl, GLDrawObject obj) {
        scaleBuilding(gl, obj);
        towerBody.draw(gl);
        /*
        if(obj.getCurrentAura() != null){
        gl.glPushMatrix();
        gl.glDisable(GL.GL_TEXTURE_2D);
        switch(obj.getCurrentAura()){
        case OFF:
        gl.glColor4f(1.0f, 0.7f, 0.24f, 0.5f);
        break;
        case DEF:
        gl.glColor4f(0.0f, 0.24f, 0.9f, 0.5f);
        break;
        case MOV:
        gl.glColor4f(0.0f, 0.9f, 0.24f, 0.5f);
        break;
        default:
        gl.glColor4f(0.9f, 0.75f, 0.0f, 0.5f);
        break;
        }
        gl.glEnable(GL.GL_BLEND);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
        gl.glScalef(1.0f/(MODEL_SCALE * 1.2f), 1.0f/(MODEL_SCALE * 1.2f), 1.0f/(MODEL_SCALE * 1.2f));
        float diskSize = (float)Math.sqrt(obj.getType().sensorRadiusSquared());

        glu.gluCylinder(quadric, diskSize, 0.0f, 1.0f, 16, 1);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glPopMatrix();

        }
         */
    }

    public void drawComm(GL gl, GLDrawObject obj) {
        scaleBuilding(gl, obj);
        communicationsBody.draw(gl);
    }

    /**
     * Draws the current game state. This method is always called from the
     * Swing event-dispatch thread, and in particular blocks calls to
     * updateRound.
     * @param g2 The graphics context, transformed to MapLocation-space
     * @param debug The debug state, including MapLocation-space mouse state
     */
    public synchronized void draw(GL gl, GLU glu, GLGameRenderer r, DebugState debug, GLDrawMap map, MapLocation origin) {
        final GLUquadric quadric = glu.gluNewQuadric();

        // are we fast forwarding
        fastforward = r.isFastForwarding();

        // draw the flux deposits
        gl.glDisable(GL.GL_TEXTURE_2D);
        for (FluxDepositState entry : fluxDeposits.values()) {
            float fx = entry.getLocation().getX() - origin.getX() + 0.5f;
            float fy = entry.getLocation().getY() - origin.getY() + 0.5f;
            float scalar = 0;
            if (entry.getRoundsAvailable() >= 0) {
                scalar = (float) Math.sqrt(Math.max(0, (float) entry.getRoundsAvailable()) / GameConstants.MINE_ROUNDS);
                gl.glColor3f((1.f - scalar), scalar, 0.f);
            } else {
                scalar = (float) Math.max(GameConstants.MINE_DEPLETED_RESOURCES / GameConstants.MINE_RESOURCES,
                        (GameConstants.MINE_RESOURCES + (float) entry.getRoundsAvailable() * 0.01 / GameConstants.MINE_DEPLETION_RATE) / GameConstants.MINE_RESOURCES);
                gl.glColor3f(scalar, 0.f, 0.f);
            }
//            if (entry.getTeam() == Team.A) {
//                gl.glColor3f(scalar, 0.f, 0.f);
//            } else if (entry.getTeam() == Team.B) {
//                gl.glColor3f(0.f, 0.f, scalar);
//            } else {
//            gl.glColor3f((1.f - scalar) * 0.5f, scalar, 0.f);
//            }

            gl.glPushMatrix();
            gl.glTranslatef(fx, map.getTerrainHeight(fx, fy), fy);
            glu.gluSphere(quadric, 0.5, 5, 5);
            gl.glPopMatrix();
        }

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw the units
        Color3f energonColor = new Color3f();
        gl.glLineWidth(2.0f);

        Iterable<Map.Entry<Integer, GLDrawObject>> drawableSet = getDrawableSet();
        if (drawableSet == null) {
            return;
        }

        for (Map.Entry<Integer, GLDrawObject> entry : drawableSet) {
            int id = entry.getKey();
            GLDrawObject obj = entry.getValue();

            float oriX = origin.getX();
            float oriY = origin.getY();

            float x = obj.getDrawX() - oriX;
            float y = obj.getDrawY() - oriY;

            gl.glPushMatrix();
            // first translate to position
            gl.glTranslatef(x + 0.5f, 0.0f, y + 0.5f);

            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

            gl.glLineWidth(4.0f);

            float extraDist = 0.1f;
            float maxHeight = GLDrawMap.MAP_SCALE * 32;
            // are we flying or not
            if (obj.getType().isAirborne()) {

                gl.glTranslatef(0.0f, maxHeight, 0.0f);
            } //                gl.glTranslatef(0.0f, map.getTerrainHeight(x + 0.5f, y + 0.5f) + 5.0f, 0.0f);
            else {
                if (obj.getMovementAction() == ActionType.MOVING) {
                    float delay = obj.getDirection().isDiagonal() ? obj.getType().moveDelayDiagonal : obj.getType().moveDelayOrthogonal;
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

                    float destZ = map.getTerrainHeight((frontSquare.getX() - oriX), (frontSquare.getY() - oriY));
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
                if (!obj.getType().isAirborne()) {
                    //If we're not flying
                    float pitch = -(float) (Math.atan2(xPosZ - xNegZ, 2) * 180 / 3.14159);//sqrt2 because it's diagonal

                    float roll = -(float) (Math.atan2(yPosZ - yNegZ, 2) * 180 / 3.14159);
                    //gl.glRotatef(pitch, 0.0f, 0.0f, 1.0f);
                    //gl.glRotatef(roll, 1.0f, 0.0f, 0.0f);
                }


                //gl.glTranslatef(0.0f, map.getTerrainHeight(actualX, actualY), 0.0f);
                //          	float roll = (float)(Math.atan2(Math.abs(sideDeltaZ), Math.sqrt(2))*180/3.14159);
                //  gl.glRotatef(roll, 1.0f, 0.0f, 0.0f);
//            	float pitch = (float)(Math.atan2(Math.abs(deltaZ), Math.sqrt(2))*180/3.14159);//sqrt2 because it's diagonal

                //   gl.glRotatef(-pitch, 0.0f, 0.0f, 1.0f);



                /*
                // compute actual jumping height if moving between locations
                MapLocation dstSquare = obj.getLocation().add(obj.getDirection());
                //float distToTarget = (float)Math.sqrt(obj.getLocation().distanceSquaredTo(dstSquare));

                // direction delta
                float squareDeltaX = dstSquare.getX() - obj.getLocation().getX();
                //squareDeltaX /= 4;
                float squareDeltaY = dstSquare.getY() - obj.getLocation().getY();
                //squareDeltaY /= 4;

                
                float srcHeight = map.getMapHeight(x + 0.5f, y + 0.5f);
                float dstHeight = map.getMapHeight(x + 0.5f + squareDeltaX, y + 0.5f + squareDeltaY);


                //    if(srcHeight != dstHeight) {
                float heightDelta = Math.abs(srcHeight - dstHeight);

                //float frac = (float)Math.sqrt(xDelta * xDelta + yDelta * yDelta) / distToTarget;


                float delay = obj.getDirection().isDiagonal() ? obj.getType().moveDelayDiagonal() : obj.getType().moveDelayOrthogonal();

                float frac = (float)obj.timeUntilIdle() / delay;

                if(frac > 1.0f) frac = 1.0f;
                else if(frac < 0.0f) frac = 0.0f;

                // magic (completely unnecessary)
                if(srcHeight > dstHeight) {
                float temp = srcHeight;
                srcHeight = dstHeight;
                dstHeight = temp;
                }

                //   System.out.println(frac);
                float h = 0.0f;
                float m = Math.max(srcHeight, dstHeight) + 0.6f;

                h = frac*srcHeight + (1-frac)*dstHeight;



                if (srcHeight != dstHeight) {
                gl.glTranslatef(0.0f, h+.4f, 0.0f);

                //float m = Math.max(srcHeight, dstHeight) + 0.3f;
                //float a = 2 * dstHeight + 2 * srcHeight - 2 * m;
                //float b = 2 * m - dstHeight - 3 * srcHeight;
                //float c = srcHeight;
                //gl.glTranslatef(0.0f, frac * frac * a + frac * b + c, 0.0f);

                } else {


                gl.glTranslatef(0.0f, map.getMapHeight(x + 0.5f, y + 0.5f)+.4f, 0.0f);
                }


                gl.glRotatef(pitch, 0.0f, 0.0f, 1.0f);
                 */
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
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDisable(GL.GL_LIGHTING);
            /*
            if (obj.getAttackAction() == ActionType.DRAINING) {
            gl.glPushMatrix();
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            if (obj.getTeam() == Team.A)
            gl.glColor4f(0.75f, 0.0f, 0.0f, 0.2f);
            else
            gl.glColor4f(0.0f, 0.0f, 0.75f, 0.2f);
            glu.gluDisk(quadric, 0.0f, Math.sqrt(RobotType.CHAINER.attackRadiusMaxSquared()), 16, 1);
            gl.glPopMatrix();
            }
             */

            // draw message broadcast
            if (obj.getBroadcast() != 0x00 && RenderConfiguration.showBroadcast()) {

                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
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

                gl.glDisable(GL.GL_BLEND);
            }
            gl.glEnable(GL.GL_LIGHTING);
            gl.glEnable(GL.GL_DEPTH_TEST);

            gl.glLineWidth(4.0f);
            // draw energon transfer animation
            GLEnergonTransferAnim transferAnim = obj.getEnergonTransferAnim();
            if (transferAnim != null) {
                transferAnim.draw(gl, map, origin);
            }

            gl.glDisable(GL.GL_LIGHTING);
            // draw crosshair if shooting
            if (obj.getAttackAction() == ActionType.ATTACKING /*&& obj.getType() != RobotType.CHAINER*/) {
                /*  final String crosshairRed = "art/crosshair.png";
                final String crosshairBlue = "art/crosshair2.png";
                String crosshair = (obj.getTeam() == Team.A) ? crosshairRed : crosshairBlue;
                Texture tex = textureCache.getResource(crosshair, crosshair).tex;
                if (tex != null) {*/
                boolean drawArch = true;
                MapLocation target = obj.getTargetLoc();
                float tx = target.getX() - origin.getX();
                float ty = target.getY() - origin.getY();
                float tz = (obj.getTargetHeight() == RobotLevel.IN_AIR) ? maxHeight : map.getTerrainHeight(tx + 0.5f, ty + 0.5f);
                float deltax = tx - x;
                float deltay = ty - y;
                float deltaz = tz;

                if (obj.getType().isAirborne()) {
                    deltaz -= maxHeight;
                    drawArch = false;
                } else {
                    deltaz -= map.getTerrainHeight(x, y);
                }

                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glPushMatrix();
                gl.glTranslatef(deltax, deltaz, deltay);
                if (obj.getTeam() == Team.A) {
                    gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
                } else {
                    gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
                }
                gl.glLineWidth(3.0f);
                float angleDelta = (float) 3.14159 / 8;
                float circleScale = .5f;
                gl.glBegin(GL.GL_LINE_LOOP);
                for (float angle = 0; angle < 3.14159 * 2; angle += angleDelta) {
                    gl.glVertex3f((float) Math.cos(angle) * circleScale, 0.01f, (float) Math.sin(angle) * circleScale);
                }
                gl.glEnd();

                gl.glBegin(GL.GL_LINES);
                angleDelta = (float) 3.14159 / 2;
                for (float angle = 0; angle < 3.14159 * 2; angle += angleDelta) {
                    gl.glVertex3f((float) (Math.cos(angle) * circleScale * 1.5), 0.01f, (float) (Math.sin(angle) * circleScale * 1.5));
                    gl.glVertex3f((float) (Math.cos(angle) * circleScale / 2), 0.01f, (float) (Math.sin(angle) * circleScale / 2));
                }
                gl.glEnd();

                gl.glLineWidth(1.0f);

                /*
                tex.bind();
                gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                gl.glBegin(GL.GL_TRIANGLE_FAN);
                gl.glTexCoord2f(0.0f, 1.0f);
                gl.glVertex3f(-0.5f, 0.01f, -0.5f);
                gl.glTexCoord2f(0.0f, 0.0f);
                gl.glVertex3f(-0.5f, 0.01f, 0.5f);
                gl.glTexCoord2f(1.0f, 0.0f);
                gl.glVertex3f(0.5f, 0.01f, 0.5f);
                gl.glTexCoord2f(1.0f, 1.0f);
                gl.glVertex3f(0.5f, 0.01f, -0.5f);
                gl.glEnd();
                 */
                gl.glPopMatrix();
                gl.glDisable(GL.GL_BLEND);

                gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
                if (obj.getTeam() == Team.A) {
                    gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
                } else {
                    gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
                }

                gl.glLineWidth(4.0f);
                gl.glBegin(GL.GL_LINES);
                //if (obj.getType() == RobotType.WOUT2XXX)
                //    gl.glVertex3f(0.0f, 0.0f, 0.0f);
                //else
                gl.glVertex3f(0.0f, 0.5f, 0.0f);
                gl.glVertex3f(deltax, deltaz, deltay);
                gl.glEnd();
                gl.glLineWidth(1.0f);
            }
            //}


            if (!obj.getType().isAirborne()) {
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
            }

            // draw archon circle
            if (obj.getType().isAirborne()) {
                gl.glDisable(GL.GL_LIGHTING);
                gl.glDisable(GL.GL_CULL_FACE);
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glPushMatrix();
                float mapheight = map.getTerrainHeight(x, y);
                gl.glTranslatef(0.0f, -maxHeight + mapheight, 0.0f);
                gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                if (obj.getTeam() == Team.A) {
                    gl.glColor4f(0.5f, 0.0f, 0.0f, 0.5f);
                } else {
                    gl.glColor4f(0.0f, 0.0f, 0.5f, 0.5f);
                }
                float diskSize = 0.5f;
                glu.gluCylinder(quadric, diskSize, 0.0f, 1.0f, 16, 1);
                gl.glPopMatrix();


                gl.glDisable(GL.GL_BLEND);
                gl.glEnable(GL.GL_CULL_FACE);
            } else {
                gl.glDisable(GL.GL_LIGHTING);
                gl.glDisable(GL.GL_CULL_FACE);
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
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


                gl.glDisable(GL.GL_BLEND);
                gl.glEnable(GL.GL_CULL_FACE);

            }

            // draw energon
            if (RenderConfiguration.showEnergon()) {
                float frac = (float) (obj.getEnergon() / obj.getType().maxEnergon);
                final Color3f max = new Color3f(0.0f, 1.0f, 0.0f);
                final Color3f min = new Color3f(1.0f, 0.0f, 0.0f);
                energonColor.interpolate(min, max, frac);

                gl.glLineWidth(4.0f);
                gl.glBegin(GL.GL_LINES);
                gl.glNormal3f(0.0f, 1.0f, 0.0f);
                gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
                gl.glVertex3f(-0.5f + frac, 0.125f, 0.5f);
                gl.glVertex3f(0.5f, 0.125f, 0.5f);

                gl.glColor4f(energonColor.x, energonColor.y, energonColor.z, 1.0f);
                gl.glVertex3f(-0.5f, 0.125f, 0.5f);
                gl.glVertex3f(-0.5f + frac, 0.125f, 0.5f);
                gl.glEnd();
                gl.glLineWidth(1.0f);

            }

            // disable lighting in ortho mode
            if (r.getCamera().isOrtho()) {
                gl.glDisable(GL.GL_LIGHTING);
            } else {
                gl.glEnable(GL.GL_LIGHT0);
                gl.glEnable(GL.GL_LIGHTING);
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



            if (GLGameRenderer.USE_MODELS) {

                switch (obj.getType()) {
                    case ARCHON:
                        drawArchon(gl, obj);
                        drawChainer(gl, obj);
                    case SOLDIER:
                        drawSoldier(gl, obj);
                    default:
                        drawRobot(gl, obj);
                }
            } else {
                drawRobot(gl, obj);
            }

            gl.glPopMatrix();
        }

        // draw awesomeness
		/*if(awesomePoint != null) {
        gl.glPushMatrix();
        gl.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        gl.glTranslatef(0.0f, 2.0f, 0.0f);
        gl.glTranslatef(awesomePoint.x - origin.getX(), 0.0f, awesomePoint.y - origin.getY());
        gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
        glu.gluDisk(quadric, 0, this.radius, 16, 1);
        gl.glPopMatrix();
        }*/

        gl.glDisable(GL.GL_TEXTURE_2D);

        // draw explosions
        gl.glTranslatef(-origin.getX(), 0.0f, -origin.getY());
        for (Map.Entry<Integer, GLDrawObject> entry : getDrawableSet()) {
            int id = entry.getKey();
            GLDrawObject obj = entry.getValue();

            float x = obj.getDrawX() - origin.getX();
            float y = obj.getDrawY() - origin.getY();

            gl.glPushMatrix();
            // are we flying or not
            if (obj.getType().isAirborne()) {
                gl.glTranslatef(obj.getDrawX(), map.getTerrainHeight(x + 0.5f, y + 0.5f) + 5.0f, obj.getDrawY());
            } else {
                gl.glTranslatef(obj.getDrawX(), map.getTerrainHeight(x + 0.5f, y + 0.5f), obj.getDrawY());
            }

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
                float tx = tar.getX() - origin.getX() + 0.5f;
                float ty = tar.getY() - origin.getY() + 0.5f;
                gl.glTranslatef(tar.getX(), map.getTerrainHeight(tx, ty), tar.getY());
                obj.getMortarExplosionAnim().draw(gl, glu);
                gl.glPopMatrix();
            }

            if (obj.getMortarAttackAnim() != null) {
                obj.getMortarAttackAnim().draw(gl, glu, map, origin);
            }

        }

        gl.glLineWidth(1.0f);
        gl.glDisable(GL.GL_BLEND);
    }

    public void doSelection(GL gl, GLU glu, GLGameRenderer r, GLDrawMap map, MapLocation origin) {
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

            float x = obj.getDrawX() - origin.getX();
            float y = obj.getDrawY() - origin.getY();

            gl.glPushMatrix();
            gl.glTranslatef(x + 0.5f, 0.0f, y + 0.5f);
            // are we flying or not
            if (obj.getType().isAirborne()) {
                gl.glTranslatef(0.0f, map.getTerrainHeight(x + 0.5f, y + 0.5f) + 5.0f, 0.0f);
            } else {
                gl.glTranslatef(0.0f, map.getTerrainHeight(x + 0.5f, y + 0.5f), 0.0f);
            }

            double rad = 0.5f;
            //if (obj.getType() == RobotType.ARCHON)
            //    rad = 0.6f;

            glu.gluSphere(quadric, rad, 5, 5);
            gl.glPopMatrix();
        }

        gl.glPopName();
    }

    private void drawRobot(GL gl, GLDrawObject obj) {
        String path = getAvatarPath(new RobotInfo(obj.getType(), obj.getTeam()));
        Texture tex = textureCache.getResource(path, path).tex;

        if (tex != null) {
            tex.bind();
        }

        // draw robot
        gl.glBegin(GL.GL_TRIANGLE_FAN);
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

    private void drawBox(GL gl) {
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

        gl.glBegin(GL.GL_LINE_STRIP);
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
