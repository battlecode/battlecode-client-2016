package battlecode.client.viewer.renderer3d;

import java.awt.Color;




import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.IntBuffer;
import java.util.Observable;
import java.util.Observer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.SwingUtilities;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import battlecode.client.util.TextureResource;
import battlecode.client.viewer.AbstractDrawObject;
import battlecode.client.viewer.BufferedMatch;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.GameStateTimeline;
import battlecode.client.viewer.InfoPanel;
import battlecode.client.viewer.MatchListener;
import battlecode.client.viewer.MatchPlayer;
import battlecode.client.viewer.render.BaseRenderer;
import battlecode.client.viewer.render.FramerateTracker;
import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Team;
import battlecode.serial.MatchFooter;
import battlecode.serial.MatchHeader;
import battlecode.server.Config;
import battlecode.world.GameMap;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;

import battlecode.client.util.ImageFile;

public class GLGameRenderer extends BaseRenderer implements GLEventListener {

    private Graphics canvas;
    //private Window fullscreen;
    private BufferedMatch match;
    private GameStateTimeline<GLDrawState> timeline;
    // mini map
    //private MiniMap miniMapWindow;
    // texture cache
    public static TextureResource<String> textureCache = new TextureResource<String>();
    // update thread state
    private boolean rendererDone = false;
    // select buffer stuff
    private final int SELECT_STACK_DEPTH = 1;
    private int selectCapacity = 0;
    private IntBuffer selectBuffer = null;
    private int currentSelectedID = -1;
    // game camera
    private Camera cam = null;
    private static TextRenderer txtRenderer;
    private static FontMetrics metric;
    private static TextRenderer cutsceneFont;
    private static FontMetrics cutsceneMetrics;
    private int roundStringWidth = -1;
    private int pointsAWidth = -1;
    private int pointsBWidth = -1;
    private GLDrawCutScene cutScene;
    private GLDrawMap drawMap;
    public static boolean USE_MODELS = Config.getGlobalConfig().getBoolean("bc.client.use-models");
    private GLDrawState ds;
    private MapLocation origin;
    private int maxRounds = 0;
    private int realmaxrounds = 0;
    private DebugState debugState;
    //private InfoPanel infoPanel = null;
    //private final Color winnerMask = new Color(0, 0, 0, 0.6f);
    private Font debugFont;
    private float spriteSize = RenderConfiguration.getInstance().getSpriteSize();
    private float unitWidth, unitHeight; // size of gc in sprite [grid] units
    private float unitHUDwidth;
    private float unitOffX, unitOffY;
    private Dimension canvasSize;
    private final Rectangle2D.Float clipRect = new Rectangle2D.Float();
    private AffineTransform hudScale;
    private ImageFile teamA, teamB, winnerImage;
    private FramerateTracker fps = new FramerateTracker(30);
    private boolean fastForward = false;
    private int targetID = -1;
    static private boolean loadedPrefsAlready = false;
    // texture cache
    //public static TextureResource<String> textureCache = new TextureResource<String>();
    private final MatchListener ml = new MatchListener() {

        public void headerReceived(BufferedMatch m) {
            processHeader(m.getHeader());
        }

        public void breakReceived(BufferedMatch m) {
            setDebugEnabled(true);
        }

        public void footerReceived(BufferedMatch m) {
            processFooter(m.getFooter());
        }
    };
    // update thread for the renderer
	/*private final Thread updateThread = new Thread() {
    public synchronized void run() {
    while(!rendererDone) {
    try {
    canvas.display();
    }
    catch(Exception e) {}

    try {
    Thread.sleep(30);
    } catch(Exception e) {}
    }
    }
    };*/

    private Runnable matchStarter = null;

    public GLGameRenderer(BufferedMatch match, Graphics canvas) {
        this.match = match;
        this.canvas = canvas;
        debugFont = new Font(null, Font.PLAIN, 2);
        ds = new GLDrawState();
		cam = new Camera();

		//fullscreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getFullScreenWindow();

        // enumerate all graphisc devices
		/*GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        GraphicsDevice dg = devices[1];
        System.out.println(dg.getIDstring());
        System.out.println(dg.getDisplayMode().getWidth() + "x" +
        dg.getDisplayMode().getHeight());
        System.out.println(dg.getDefaultConfiguration().getBounds());
        fullscreen = new Window(new Frame(dg.getDefaultConfiguration()));
         */

        timeline = new GameStateTimeline<GLDrawState>(match, GLDrawState.FACTORY, 10);
        timeline.setTargetState(ds);
        //timeline.addObserver(observer);
        match.addMatchListener(ml);
        match.addMatchPausedListener(ml);

        // GLU and GLUT
        glu = new GLU();
        glut = new GLUT();

        //textureCache = new TextureResource<String>();
        GLDrawState.textureCache = textureCache;

        loadPrefs();
    }

    // quietly dont support win counting
    public void resetMatches() {
    }

    public void addWin(Team t) {
    }

    public void loadPrefs() {
        if (!loadedPrefsAlready) {
            for (char ch : Config.getGlobalConfig().get("bc.client.renderprefs3d").toCharArray()) {
                handleAction(ch);
            }
            loadedPrefsAlready = true;
        }
    }

    public boolean isFastForwarding() {
        return fastForward;
    }

    public GameStateTimeline getTimeline() {
        return timeline;
    }

    public void setMatchStarter(Runnable starter) {
        if (cutScene != null && cutScene.step == GLDrawCutScene.Step.GAME) {
            starter.run();
        } else {
            matchStarter = starter;
        }
    }

    private void setDebugEnabled(boolean enabled) {
        if (debugState != null) {
            debugState.setEnabled(enabled);
        }
    }

	protected void toggleFastForward() {
		fastForward = !fastForward;
	}

    protected boolean trySkipRounds(int rounds) {
        int targetRound = timeline.getRound() + rounds;
        if (targetRound < timeline.getNumRounds()) {
            timeline.setRound(targetRound);
            return true;
        }
        return false;
    }
    public static int BRACKET_INDEX = 0;

	@SuppressWarnings("unchecked")
    private void processHeader(MatchHeader header) {
        System.out.println("PROCESS HEADER: " + header);
        GameMap map = new GameMap((GameMap)header.getMap());
        drawMap = new GLDrawMap(map);
        //ds.clearBlockNumberDeltas();
        origin = map.getMapOrigin();
        //sideA.setFooterText("GAME " + (header.getMatchNumber() + 1));
        maxRounds = map.getMaxRounds();
        realmaxrounds = map.getStraightMaxRounds();
        ds.setGameMap(map);

        // set default playback speed
        MatchPlayer mp = MatchPlayer.getCurrent();
        if (mp != null) {
            mp.setTimeDelta(MatchPlayer.DEFAULT_TIME_DELTA * 3);
        }

        // create minimap
        //miniMapWindow = new MiniMap(map);
        //miniMapWindow.setVisible(true);

        unitHeight = drawMap.getMapHeight();
        //unitHUDwidth = sideA.getRatioWidth() * unitHeight;
        unitWidth = drawMap.getMapWidth() + 2 * unitHUDwidth;
        clipRect.width = drawMap.getMapWidth();
        clipRect.height = drawMap.getMapHeight();
	
        Vector3f eye = new Vector3f(drawMap.getMapWidth() / 2, 50.0f, drawMap.getMapHeight());
        Vector3f target = new Vector3f(drawMap.getMapWidth() / 2, 0.0f, drawMap.getMapHeight() / 2);
        eye.sub(eye, target);

        float slope = 0.015f;
        float zoom = 0.3f + slope * Math.max(drawMap.getMapWidth(), drawMap.getMapHeight());

        eye.scale(zoom); // bring us to 75% of the distance
        eye.add(eye, target);
        cam.setParameters(eye,target,new Vector3f(0.0f, 1.0f, 0.0f), 45.0, 0.1, 100.0);

        // select buffer size (2 * mapWidth * mapHeight) squares
        //selectCapacity = 2 * drawMap.getMapHeight() * drawMap.getMapWidth() * SELECT_STACK_DEPTH * 4;
        selectCapacity = 64 * SELECT_STACK_DEPTH * 4;
        selectBuffer = BufferUtil.newIntBuffer(selectCapacity);
        selectBuffer.rewind();

        File f = new File("art/computerfont.ttf");
        try {
            Font fnt = Font.createFont(Font.TRUETYPE_FONT, f);
            boolean isTournament = RenderConfiguration.isTournamentMode();
            Font newFnt = fnt.deriveFont(Font.PLAIN, (isTournament ? 36 : 24));
            if (txtRenderer != null)
                txtRenderer.dispose();
            metric = canvas.getFontMetrics(newFnt);
            txtRenderer = new TextRenderer(newFnt);
            newFnt = fnt.deriveFont(Font.PLAIN, 60);
            cutsceneMetrics = canvas.getFontMetrics(newFnt);
            if (cutsceneFont != null)
                cutsceneFont.dispose();
            cutsceneFont = new TextRenderer(newFnt);
        } catch (Exception ex) {
            txtRenderer = null;
        }

        //init(canvas.getGL());
        if (RenderConfiguration.getInstance().isTournamentMode()) {
            (new Thread() {

                public void run() {
                    while (match.getTeamA() == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                    cutScene = new GLDrawCutScene(unitWidth, unitHeight,
                            match.getTeamA(), match.getTeamB());

                    try {
                        String path = "./art/background/" + match.getMapNames()[match.getHeader().getMatchNumber()] + ".xml.png";
                        String cratePath = "./art/walls/" + match.getMapNames()[match.getHeader().getMatchNumber()] + ".xml.png";
                        drawMap.setTexture(path);
                        drawMap.setCrateTexture(cratePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*try {
                    String path = "map-backgrounds/" + match.getMapNames()[match.getHeader().getMatchNumber()] + ".xml.png";
                    System.out.println("loading " + path);
                    ImageFile imgFile = new ImageFile(path);
                    drawMap.prerenderMap(imgFile.image);
                    imgFile.unload();
                    }
                    catch (NullPointerException e) {
                    e.printStackTrace();
                    //drawMap.prerenderMap();
                    }*/
                }
            }).start();
        }
    }

	public void setDebugState(battlecode.client.viewer.DebugState dbg) {
		this.debugState = dbg;
	}

	private void processFooter(MatchFooter footer) {
        if (cutScene != null) {
            cutScene.setWinner(footer.getWinner());
        }
    }

    public synchronized void setCanvasSize(Dimension dim) {
        canvasSize = new Dimension(dim);
        if (drawMap != null) {
            setCanvasSize();
        }
    }

    private void setCanvasSize() {
        if (canvasSize.width * unitHeight > canvasSize.height * unitWidth) {
            spriteSize = canvasSize.height / unitHeight;
        } else {
            spriteSize = canvasSize.width / unitWidth;
        }
        unitOffX = 0.5f * (canvasSize.width / spriteSize - unitWidth);
        unitOffY = 0.5f * (canvasSize.height / spriteSize - unitHeight);
        RenderConfiguration.getInstance().setSpriteSize(spriteSize);
    }

	public AbstractDrawObject getRobotByID(int id) {
		return ds.getDrawObject(id);
	}

    /*private void drawHUD(Graphics2D g2) {
    AffineTransform pushed = g2.getTransform(); {
    g2.transform(hudScale);
    //sideA.draw(g2);
    } g2.setTransform(pushed);
    {
    g2.translate(unitWidth - unitHUDwidth, 0);
    g2.transform(hudScale);
    //sideB.setFooterText(String.format("%04d",
    //                                  maxRounds - timeline.getRound()));
    //sideB.draw(g2);
    } g2.setTransform(pushed);
    }

    private void drawState(Graphics2D g2, boolean isGraphicsStable) {
    AffineTransform pushed = g2.getTransform(); {
    g2.translate(unitHUDwidth, 0);
    //drawMap.draw(g2);
    g2.clip(clipRect);
    g2.translate(-origin.getX(), -origin.getY());
    if (isGraphicsStable) {
    RenderConfiguration.getInstance()
    .updateMapTransform(g2.getTransform());
    }
    //ds.draw(g2, debugState);
    g2.setClip(null);
    } g2.setTransform(pushed);
    }*/

    /*public void draw(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    updateInfoPanel();
    boolean isGraphicsStable = g2.getTransform().isIdentity();
    g2.setColor(Color.BLACK);
    g2.fill(new Rectangle(0, 0, canvasSize.width, canvasSize.height));
    g2.scale(spriteSize, spriteSize);
    g2.translate(unitOffX, unitOffY);
    if (timeline.getRound() >= 0) {
    drawHUD(g2);
    drawState(g2, isGraphicsStable);
    }
    if (cutScene != null) {
    cutScene.draw(g2);
    }
    //fps.updateFramerate(); renderFramerate();
    if (fastForward) {
    timeline.setRound(timeline.getRound() + 1);
    }
    }*/
    public void beginIntroCutScene(long targetMillis) {
        if (cutScene != null)
            cutScene.setTargetEnd(targetMillis);
        setCutSceneVisible(true);
    }

    public void setCutSceneVisible(boolean visible) {
        if (cutScene == null)
            return;

        if (visible) {
            if (timeline.getRound() == -1) {
                cutScene.step = GLDrawCutScene.Step.INTRO;
            } else if (timeline.getRound() == timeline.getNumRounds()) {
                cutScene.step = GLDrawCutScene.Step.OUTRO;
            }
        }
        cutScene.setVisible(visible);
    }

    public void fadeOutCutScene() {
        if (cutScene != null)
            cutScene.fadeOut();
    }

    public void doRepaint() {
        //System.out.println("DO REPAINT");
        //if (fullscreen != null) {
        //	draw(canvas.getGraphics());
        //}
        //else {
        //	canvas.display();
        //}
    }

    private void renderFramerate(Graphics2D g2) {
        g2.setTransform(new AffineTransform());
        g2.setColor(Color.BLACK);
        g2.setFont(debugFont);
        g2.drawString("Framerate: " + fps.getFramerate(), 20, 30);
    }

    public static void preloadGraphics() {
        //GLDrawObject.loadAll();
    }

    public IntBuffer getSelectBuffer() {
        return selectBuffer;
    }

    public Camera getCamera() {
        return cam;
    }

	public void setupProjection(GL gl, GLU glu) {
		if (cam.isOrtho()) {
			gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
			
			float heightScale = (float) canvasSize.height / (float) drawMap.getMapHeight();
			float widthScale = (float) canvasSize.width / (float) drawMap.getMapWidth();
			float scale = 0.0f;
			
			scale = Math.min(heightScale, widthScale);
			float zScale = scale;
			float xScale = zScale;
			
			float xTrans = (canvasSize.width - drawMap.getMapWidth() * xScale) / 2.0f;
			float zTrans = (canvasSize.height - drawMap.getMapHeight() * zScale) / 2.0f;
			
			//gl.glTranslatef(0.0f, 0.0f, canvasSize.height);
			gl.glTranslatef(xTrans, 0.0f, zTrans);
			gl.glTranslatef(0.0f, 0.0f, -(canvasSize.height - drawMap.getMapHeight() * zScale));
			gl.glScalef(xScale, 0.0f, zScale);
			gl.glTranslatef(0.0f, 0.0f, -drawMap.getMapHeight());
		} else {
			gl.glScalef(0.1f, 0.1f, 0.1f);
			cam.setTransform(gl, glu);
		}
	}

	final battlecode.client.util.SettableObservable displayObservable = new battlecode.client.util.SettableObservable();

    /**
     * This method is called to render the contents of each frame.
     * @param ad is a wrapper for the OpenGL context.
     */
    public void display(GLAutoDrawable ad) {

        // HACK: remove this whenever a better system is devised
        if (txtRenderer == null) {
            File f = new File("art/computerfont.ttf");
            try {
                Font fnt = Font.createFont(Font.TRUETYPE_FONT, f);
                boolean isTournament = RenderConfiguration.isTournamentMode();
                Font newFnt = fnt.deriveFont(Font.PLAIN, (isTournament ? 36 : 24));
                metric = canvas.getFontMetrics(newFnt);
                txtRenderer = new TextRenderer(newFnt);
                newFnt = fnt.deriveFont(Font.PLAIN, 60);
                cutsceneMetrics = canvas.getFontMetrics(newFnt);
                cutsceneFont = new TextRenderer(newFnt);
            } catch (Exception ex) {
                txtRenderer = null;
            }
        }



        if (drawMap == null || ds.getRoundStats() == null || glu == null) {
            GL gl = ad.getGL();

            /*gl.glClearDepth(1.0f);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glEnable(GL.GL_NORMALIZE);

            // TODO: maybe put skybox?
            //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClearColor(0.35f, 0.61f, 0.9f, 0.0f);

            gl.glColor3f(0.0f, 0.0f, 0.0f);
            gl.glShadeModel(GL.GL_SMOOTH);
            gl.glEnable(GL.GL_LIGHTING);

            gl.glFrontFace(GL.GL_CCW);
            gl.glEnable(GL.GL_CULL_FACE);

            gl.glEnable(GL.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);

            gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
             */
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            // draw cutscene
            if (cutScene != null)
                cutScene.draw(gl, ad.getWidth(), ad.getHeight(), cutsceneFont, cutsceneMetrics);

            return;
        }

        // look at awesome point
        Vector2f awesomePoint = ds.getAwesomePoint();
        if (/*timeline.getRound() % 10 == 0 &&*/awesomePoint != null) {
            float ax = awesomePoint.x - origin.getX() + 0.5f;
            float ay = awesomePoint.y - origin.getY() + 0.5f;
			cam.setAutocamTarget(ax, 3.2f, ay, ds.getAwesomeRadius());
			// using getTerrainHeight causes excessive jitter
            //cam.setAutocamTarget(ax, drawMap.getTerrainHeight(ax, ay), ay, ds.getAwesomeRadius());
        }

        // perform autocam operations
        cam.doAutocam();

        // update minimap
        //miniMapWindow.update(ds, origin);

        // TODO draw the scene in here
        GL gl = ad.getGL();
		gl.glLoadIdentity();

        // make nice blue color
        gl.glClearColor(0.35f * 0.5f, 0.61f * 0.5f, 0.9f * 0.5f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        //Draw background (space)
        Texture backgroundTexture = null;
        final String path = "art/backgrounds/space-the-final-frontier.jpg";

        backgroundTexture = GLGameRenderer.textureCache.getResource(path, path).tex;
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);


        float boxWidth = 1.0f;
        float texSize = 1.5f;


        Vector3f position = cam.getPosition();
        Vector3f target = cam.getTarget();
        float pi = (float) Math.PI;
        float tx = target.x;
        float ty = target.y;
        float tz = target.z;

        float px = position.x;
        float py = position.y;
        float pz = position.z;

        float tMag = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        tx /= tMag;
        ty /= tMag;
        tz /= tMag;

        float pMag = (float) Math.sqrt(px * px + py * py + pz * pz);
        px /= pMag;
        py /= pMag;
        pz /= pMag;

        float deltaY = ty = py;
        float deltaZ = tz - pz;
        float deltaX = tx - px;
        float multiplier = 3.0f;
//		System.out.println(deltaY+ " " + deltaZ + " " + deltaX);
        float theta = (float) Math.atan2(deltaX, deltaY);
        theta = (theta + pi / 2) / pi;

        float phi = (float) Math.atan2(deltaX, deltaZ);
        phi = (phi + pi / 2) / pi;

        float texXStart = theta;
        float texXEnd = texXStart + texSize;
        float texYStart = phi;
        float texYEnd = texYStart + texSize;

        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        if (backgroundTexture != null) {
            backgroundTexture.bind();

            gl.glPushMatrix();
//			gl.glDisable(GL.GL_COLOR_MATERIAL);
            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_BLEND);

            //gl.glScalef(200.0f, 75.0f, 200.0f);
            gl.glBegin(GL.GL_QUADS);
            //Back face
            gl.glTexCoord2f(texXStart, texYStart);
            gl.glVertex3f(-boxWidth, -boxWidth, boxWidth);
            gl.glTexCoord2f(texXEnd, texYStart);
            gl.glVertex3f(boxWidth, -boxWidth, boxWidth);
            gl.glTexCoord2f(texXEnd, texYEnd);
            gl.glVertex3f(boxWidth, boxWidth, boxWidth);
            gl.glTexCoord2f(texXStart, texYEnd);
            gl.glVertex3f(-boxWidth, boxWidth, boxWidth);
            gl.glEnd();

            texXStart *= multiplier;
            texXEnd = texXStart + texSize;
            texYStart *= multiplier;
            texYEnd = texYStart + texSize;

            gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);

            gl.glBegin(GL.GL_QUADS);
            //Back face
            gl.glTexCoord2f(texXStart, texYStart);
            gl.glVertex3f(-boxWidth, -boxWidth, boxWidth);
            gl.glTexCoord2f(texXEnd, texYStart);
            gl.glVertex3f(boxWidth, -boxWidth, boxWidth);
            gl.glTexCoord2f(texXEnd, texYEnd);
            gl.glVertex3f(boxWidth, boxWidth, boxWidth);
            gl.glTexCoord2f(texXStart, texYEnd);
            gl.glVertex3f(-boxWidth, boxWidth, boxWidth);
            gl.glEnd();

            gl.glDisable(GL.GL_BLEND);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDisable(GL.GL_TEXTURE_2D);
            //	gl.glEnable(GL.GL_COLOR_MATERIAL);

            gl.glPopMatrix();
        }
        //End draw background


        if (!gl.glIsEnabled(GL.GL_COLOR_MATERIAL)) {
            gl.glEnable(GL.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);
        }

        if (!RenderConfiguration.isTournamentMode()) {
            if (cam != null && selectBuffer != null && cam.getMouseClick() != null && cam.getMousePos() != null) {
                int[] viewport = cam.getViewport();

                gl.glSelectBuffer(getSelectBuffer().capacity(), getSelectBuffer());
                gl.glRenderMode(GL.GL_SELECT);

                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity();

                Point mousePoint = cam.getMousePos();

                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity();
                glu.gluPickMatrix(mousePoint.x, viewport[3] - mousePoint.y, 1.0, 1.0, cam.getViewport(), 0);

                cam.setProjection(gl, glu);

                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glLoadIdentity();
				setupProjection(gl,glu);

                // this is where we do selection
                ds.doSelection(gl, glu, this, drawMap, origin);

                // draw the scene
                int numSelections = gl.glRenderMode(GL.GL_RENDER);

                if (numSelections > 0) {
                    // selectBuffer is:
                    // 1st int: depth of name stack
                    // 2nd int: minimum depth value
                    // 3rd int: maximum depth value
                    // x ints: list of names
                    currentSelectedID = selectBuffer.get(3);
                }
            }

            // update the info
            debugState.setFocusID(currentSelectedID);
            // update the info panel
            //updateInfoPanel();
        }

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();

        if (cam != null) cam.setProjection(gl, glu);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();



        if (cam != null) {
			setupProjection(gl,glu);
        }

        gl.glEnable(GL.GL_LIGHTING);
        //gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[] {drawMap.getMapWidth() / 2, 15.0f, drawMap.getMapHeight() / 2, 1.0f}, 0);
        if (!cam.isOrtho())
            gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[]{0.0f, 7.5f, 0.0f, 1.0f}, 0);
        else
            gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[]{0.0f, drawMap.getMapHeight(), 7.5f, 1.0f}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[]{0.3f, 0.3f, 0.3f, 1.0f}, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[]{0.8f, 0.8f, 0.8f, 1.0f}, 0);
        gl.glEnable(GL.GL_LIGHT0);




        // draw map
        drawMap.draw(gl, glu, ds);
        // draw the game state

        if (cam.isOrtho())
            gl.glDisable(GL.GL_LIGHTING);

        //gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] {0.6f, 0.6f, 0.6f, 1.0f}, 0);
        ds.draw(gl, glu, this, debugState, drawMap, origin);

        gl.glDisable(GL.GL_LIGHTING);

        // draw the text
        if (txtRenderer != null) {
            // get points of the teams
            int minPoints = ds.getMapMinPoints();
            int aPoints = (int) Math.round(ds.getRoundStats().getPoints(Team.A));
            int bPoints = (int) Math.round(ds.getRoundStats().getPoints(Team.B));

            // clamp the names to 12 characters
            int nameLength = Math.max(match.getTeamA().length(), match.getTeamB().length());
            if (nameLength > 12)
                nameLength = 12;

            String teamANameShort = "RED TEAM";
            if (match.getTeamA() != null)
                teamANameShort = match.getTeamA().substring(0, Math.min(nameLength, match.getTeamA().length()));
            String teamBNameShort = "BLUE TEAM";
            if (match.getTeamB() != null)
                teamBNameShort = match.getTeamB().substring(0, Math.min(nameLength, match.getTeamB().length()));

            if (pointsAWidth == -1)
                pointsAWidth = metric.stringWidth(teamANameShort + " - 00000");
            if (pointsBWidth == -1)
                pointsBWidth = metric.stringWidth("00000 - " + teamBNameShort);

            // use a magic number for padding
            final int PADDING_MAGIC = 30;
            int barHalfWidth = (ad.getWidth() - pointsAWidth - pointsBWidth) / 2 - PADDING_MAGIC;

            // draw ortho stuff
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            if (canvas != null)
                gl.glOrtho(0, canvasSize.width, 0, canvasSize.height, -100, 100);
            else
                gl.glOrtho(0, 1280, 0, 1024, -100, 100);
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glLoadIdentity();

            // draw winner bar here
            gl.glLineWidth(16.0f);
            float barDir = 0;
            //if (aPoints > minPoints || bPoints > minPoints)
            barDir = (bPoints - aPoints) / (minPoints / 1.0f);
            if (barDir < -1.0f) barDir = -1.0f;
            if (barDir > 1.0f) barDir = 1.0f;
            // if barDir > 0 then B is winning, < 0 then A is winning, else tie

            gl.glBegin(GL.GL_LINES);
            // draw background for relative victory line
            gl.glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
            gl.glVertex3i(ad.getWidth() / 2 - barHalfWidth, ad.getHeight() - 15, 1);
            gl.glVertex3i(ad.getWidth() / 2 + barHalfWidth, ad.getHeight() - 15, 1);

            //System.out.println("tr " + (timeline.getRound() - this.realmaxrounds) + " " + ((timeline.getRound()) / 10));
            if (timeline.getRound() - this.realmaxrounds > -100 && ((timeline.getRound()) / 10) % 2 == 0) {
                gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                if (barDir > 0)
                    gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
                else if (barDir < 0)
                    gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            }

            // draw the relative victory line
            gl.glVertex3i(ad.getWidth() / 2, ad.getHeight() - 15, 2);
            gl.glVertex3i(ad.getWidth() / 2 + (int) (barDir * barHalfWidth), ad.getHeight() - 15, 2);
            gl.glEnd();


            // draw the absolute length to minPoints
            gl.glLineWidth(4.0f);
            gl.glBegin(GL.GL_LINES);

            // for the blue guy
            gl.glColor4f(0.7f, 0.95f, 1.0f, 1.0f);
            gl.glVertex3i(ad.getWidth() / 2, ad.getHeight() - 20, 3);
            float bPercent = (float) Math.min(1, (1 - (timeline.getRound() - realmaxrounds + 1) * GameConstants.POINTS_DECREASE_PER_ROUND_FACTOR));
            gl.glVertex3i((int) (ad.getWidth() / 2 + bPercent * barHalfWidth), ad.getHeight() - 20, 3);

            // for the red guy
            gl.glColor4f(1.0f, 0.7f, 0.95f, 1.0f);
            gl.glVertex3i(ad.getWidth() / 2, ad.getHeight() - 20, 3);
            float aPercent = (float) Math.min(1, (1 - (timeline.getRound() - realmaxrounds + 1) * GameConstants.POINTS_DECREASE_PER_ROUND_FACTOR));
            gl.glVertex3i((int) (ad.getWidth() / 2 - aPercent * barHalfWidth), ad.getHeight() - 20, 3);
            gl.glEnd();


            gl.glBegin(GL.GL_LINES);
            gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
            // now draw vertical center bar
            gl.glVertex3i(ad.getWidth() / 2, ad.getHeight() - 6, 4);
            gl.glVertex3i(ad.getWidth() / 2, ad.getHeight() - 24, 4);
            gl.glEnd();
            gl.glLineWidth(1.0f);

            String s = teamANameShort + " - " + aPoints;
            // do text rendering
            txtRenderer.beginRendering(ad.getWidth(), ad.getHeight());
            int numArchons = ds.getArchons(Team.A).size();
            String archons = numArchons + ((numArchons == 1) ? " Archon" : " Archons");
            txtRenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
            //txtRenderer.draw("RED TEAM - " + aPoints, 2 + 4, ad.getHeight() - metric.getAscent() + 2);
            txtRenderer.draw(s, 2 + 4, ad.getHeight() - metric.getAscent() + 2);
            txtRenderer.draw(archons, 2 + 4, ad.getHeight() - metric.getAscent() * 2 + 2);
            txtRenderer.setColor(1.0f, 0.0f, 0.0f, 1.0f);
            //txtRenderer.draw("RED TEAM - " + aPoints, 0 + 4, ad.getHeight() - metric.getAscent());
            txtRenderer.draw(s, 0 + 4, ad.getHeight() - metric.getAscent());
            txtRenderer.draw(archons, 0 + 4, ad.getHeight() - metric.getAscent() * 2);


            //String s = bPoints + " - BLUE TEAM";
            s = bPoints + " - " + teamBNameShort;
            numArchons = ds.getArchons(Team.B).size();
            archons = numArchons + ((numArchons == 1) ? " Archon" : " Archons");

            txtRenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
            txtRenderer.draw(s, ad.getWidth() - metric.stringWidth(s) + 2 - 2 - 4, ad.getHeight() - metric.getAscent() + 2);
            txtRenderer.draw(archons, ad.getWidth() - metric.stringWidth(archons) + 2 - 2 - 4, ad.getHeight() - metric.getAscent() * 2 + 2);
            //txtRenderer.setColor(0.0f, 0.0f, 1.0f, 1.0f);
            txtRenderer.setColor(0.7f, 0.95f, 1.0f, 1.0f);
            txtRenderer.draw(s, ad.getWidth() - metric.stringWidth(s) - 2 - 4, ad.getHeight() - metric.getAscent());
            txtRenderer.draw(archons, ad.getWidth() - metric.stringWidth(archons) - 2 - 4, ad.getHeight() - metric.getAscent() * 2);

            String round = "MIN: " + minPoints + "    RND: " + String.valueOf(maxRounds - timeline.getRound());
            if (roundStringWidth == -1)
                roundStringWidth = metric.stringWidth(round);

            txtRenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
            txtRenderer.draw(round, ad.getWidth() / 2 - roundStringWidth / 2 + 2, ad.getHeight() - metric.getHeight() - 20 + 2);
            txtRenderer.setColor(0.0f, 1.0f, 0.0f, 1.0f);
            txtRenderer.draw(round, ad.getWidth() / 2 - roundStringWidth / 2, ad.getHeight() - metric.getHeight() - 20);

            txtRenderer.endRendering();
        }

        // draw cutscene
        if (cutScene != null)
            cutScene.draw(gl, ad.getWidth(), ad.getHeight(), cutsceneFont, cutsceneMetrics);


        //fps.updateFramerate(); renderFramerate();
        if (fastForward) {
            timeline.setRound(timeline.getRound() + 1);
        }

        // draw light
		/*gl.glPointSize(8.0f);
        gl.glBegin(GL.GL_POINTS);
        gl.glColor3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3fv(new float[] {drawMap.getMapWidth() / 2, 15.0f, drawMap.getMapHeight() / 2, 1.0f}, 0);
        gl.glEnd();
        gl.glPointSize(1.0f);*/

        // draw triad
		/*gl.glBegin(GL.GL_LINES);
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(1.0f, 0.0f, 0.0f);
        gl.glColor3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 1.0f, 0.0f);
        gl.glColor3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 1.0f);
        gl.glEnd();
         */
        // draw fonts
		/*gl.glMatrixMode(GL.GL_PROJECTION);
        glu.gluOrtho2D(0.0f, canvasSize.width, 0.0f, canvasSize.height);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -1.0f);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glRasterPos2f(20.0f, 15.0f);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "HELLLLLOOOOO, WOOOORLD");

        gl.glDisable(GL.GL_TEXTURE_2D);*/
		displayObservable.setChanged();
		displayObservable.notifyObservers();
    }

    /**
     * This method is called when the display mode or device is changed.
     * @param ad is a wrapper for the OpenGL context.
     * @param modeChanged specifies whether the display mode is changed.
     * @param deviceChanged specifies whether or not the device has been changed.
     */
    public void displayChanged(GLAutoDrawable ad, boolean modeChanged, boolean deviceChanged) {
        // TODO handle reloading here as well
        //System.out.println("GLGameRenderer.displayChanged(modeChanged = " + modeChanged + ", deviceChanged = " + deviceChanged + ")");
    }
    // some GLU
    private GLU glu = null;
    // some glut
    private GLUT glut = null;

    /**
     * This method is called during the initialization of the renderer.  This will
     * also be called when the display is changed or there is a resize.  So we
     * should probably reload all of the OpenGL resources into memory here.
     * @param ad is a wrapper for the OpenGL context.
     */
    public void init(GLAutoDrawable ad) {
        // TODO NO SIDE-EFFECTS IN HERE
        //initGLParams(ad.getGL());
        init(ad.getGL());
    }

    public void init(GL gl) {
        // TODO NO SIDE-EFFECTS IN HERE
        //System.out.println("GLGameRenderer.init()");
        initGLParams(gl);

        //updateThread.start();
    }

    private void initGLParams(GL gl) {
        gl.glClearDepth(1.0f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glEnable(GL.GL_NORMALIZE);

        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

        // TODO: maybe put skybox?
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClearColor(0.35f, 0.61f, 0.9f, 0.0f);

        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glShadeModel(GL.GL_SMOOTH);
        gl.glEnable(GL.GL_LIGHTING);

        gl.glFrontFace(GL.GL_CCW);
        gl.glEnable(GL.GL_CULL_FACE);

        gl.glEnable(GL.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);

        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
    }

    /**
     * This method is called after a component resize.
     * @param ad is a wrapper for the OpenGL context.
     * @param x the x coordinate of the client area.
     * @param y the y coordinate of the client area.
     * @param width the width of the client area.
     * @param height the height of the client area.
     */
    public void reshape(GLAutoDrawable ad, int x, int y, int width,
            int height) {
        // TODO call glViewport here
		/*System.out.println("GLGameRenderer.reshape(x = " + x + ", y = " + y +
        ", width = " + width + ", height = " + height +")");*/

        GL gl = ad.getGL();

		canvasSize = new Dimension(width,height);
        if (cam != null) cam.updateWindowSize(width, height);
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        if (cam != null) cam.setProjection(gl, glu);
    }

    public void removeCanvasReference() {
        this.canvas = null;
        //debugState.deleteObserver(observer);
    }
}
