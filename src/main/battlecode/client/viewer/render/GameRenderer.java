package battlecode.client.viewer.render;

import battlecode.client.viewer.*;
import battlecode.common.MapLocation;
import battlecode.common.Team;
import battlecode.world.DominationFactor;
import battlecode.serial.MatchFooter;
import battlecode.serial.MatchHeader;
import battlecode.server.Config;
import battlecode.world.GameMap;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class GameRenderer {

    private BufferedMatch match;
    private GameStateTimeline<DrawState> timeline;
    private DrawCutScene cutScene;
    private DrawHUD sideA, sideB, sideZ, sideN;
    private DrawMap drawMap;
    private DrawState ds;
    private MapLocation origin;
    private int maxRounds = 0;
    private String mapName = "";
    private DebugState debugState;
    private float spriteSize = RenderConfiguration.getInstance()
            .getSpriteSize();
    private float unitWidth, unitHeight; // size of gc in sprite [grid] units
    private float unitHUDwidth;
    private float unitOffX, unitOffY;
    private Dimension canvasSize = null;
    private final Rectangle2D.Float clipRect = new Rectangle2D.Float();
    private AffineTransform hudScale;
    static private boolean loadedPrefsAlready = false;
    private final MatchListener ml = new MatchListener() {

        public void headerReceived(BufferedMatch m) {
            processHeader(m.getHeader());
        }

        public void breakReceived(BufferedMatch m) {
            setDebugEnabled(true);
        }

        public void footerReceived(BufferedMatch m) {
            processDominationFactor(m.getDominationFactor());
            processFooter(m.getFooter());
        }
    };

    public GameRenderer() {
    }

    public GameRenderer(BufferedMatch match) {
        this.match = match;
        ds = new DrawState();

        try {
            sideA = new DrawHUD(ds, Team.A, match);
            sideB = new DrawHUD(ds, Team.B, match);
            sideZ = new DrawHUD(ds, Team.ZOMBIE, match);
            sideN = new DrawHUD(ds, Team.NEUTRAL, match);
        } catch (Error e) {
            e.printStackTrace();
        }

        timeline = new GameStateTimeline<>(match, DrawState.FACTORY,
                10);
        timeline.setTargetState(ds);
        match.addMatchListener(ml);
        match.addMatchPausedListener(ml);
        loadPrefs();
    }

    public DrawState getDrawState() {
        return ds;
    }

    // wins obtained by each side
    int aWins = 0, bWins = 0;
    // methods for win display

    public void resetMatches() {
        aWins = 0;
        bWins = 0;
    }

    public void addWin(Team t) {
        System.out.println("Win added" + t);
        if (t == Team.A) {
            aWins++;
        } else if (t == Team.B) {
            bWins++;
        }
        if (cutScene != null)
            cutScene.setWinner(t);
    }

    public void loadPrefs() {
        if (!loadedPrefsAlready) {
            for (char ch : Config.getGlobalConfig().get("bc.client" +
                    ".renderprefs2d").toCharArray()) {
                handleAction(ch);
            }
            loadedPrefsAlready = true;
        }
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#getTimeline()
     */
    public GameStateTimeline<DrawState> getTimeline() {
        return timeline;
    }

    private void setDebugEnabled(boolean enabled) {
        if (debugState != null) {
            debugState.setEnabled(enabled);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void processHeader(MatchHeader header) {
        GameMap map = header.getMap();
        drawMap = new DrawMap(map);
        origin = map.getOrigin();
        sideA.setFooterText("GAME " + (header.getMatchNumber() + 1));
        maxRounds = map.getRounds();
        mapName = map.getMapName();

        unitHeight = drawMap.getMapHeight();
        unitHUDwidth = sideA.getRatioWidth() * unitHeight;
        unitWidth = drawMap.getMapWidth() + 2 * unitHUDwidth;
        clipRect.width = drawMap.getMapWidth();
        clipRect.height = drawMap.getMapHeight();
        hudScale = AffineTransform.getScaleInstance(unitHeight, unitHeight);

        if (RenderConfiguration.isTournamentMode()) {
            (new Thread() {

                public void run() {
                    while (match.getTeamA() == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                    cutScene = new DrawCutScene(unitWidth, unitHeight,
                            match.getTeamA(), match.getTeamB(), mapName);
                }
            }).start();
        }
    }

    private void processDominationFactor(DominationFactor dom) {
        if (cutScene != null) {
            cutScene.setDominationFactor(dom);
        }
    }

    private void processFooter(MatchFooter footer) {
        if (cutScene != null) {
            cutScene.setWinner(footer.getWinner());
        }
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#setCanvasSize(java
     * .awt.Dimension)
     */
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

    public DrawObject getRobotByID(int id) {
        return ds.getDrawObject(id);
    }

    private void drawHUD(Graphics2D g2) {
        if (hudScale == null) {
            return;
        } // just in case

        // update wins
        sideA.setWins(aWins, bWins);
        sideB.setWins(aWins, bWins);
        //sideA.setPointsText();

        AffineTransform pushed = g2.getTransform();
        {
            g2.transform(hudScale);
            sideA.draw(g2);
        }
        g2.setTransform(pushed);
        {
            g2.translate(unitHUDwidth / 2, 0);
            g2.transform(hudScale);
            sideZ.draw(g2);
        }
        g2.setTransform(pushed);
        {
            g2.translate(unitHUDwidth, 0);
            g2.transform(hudScale);
            sideB.setFooterText(String.format("%04d",
                    Math.max(maxRounds - timeline.getRound(), 0)));
            sideB.draw(g2);
        }
        g2.setTransform(pushed);
        {
            g2.translate(unitHUDwidth * 3 / 2, 0);
            g2.transform(hudScale);
            sideN.draw(g2);
        }
        g2.setTransform(pushed);
    }

    public void setDebugState(DebugState dbg) {
        this.debugState = dbg;
    }

    public DebugState getDebugState() {
        return debugState;
    }

    private void drawState(Graphics2D g2, boolean isGraphicsStable) {
        if (drawMap == null || ds == null) {
            return;
        } // just in case
        AffineTransform pushed = g2.getTransform();
        {
            g2.translate(unitHUDwidth * 2, 0);
            drawMap.draw(g2, ds);
            g2.clip(clipRect);
            g2.translate(-origin.x, -origin.y);
            if (isGraphicsStable) {
                RenderConfiguration.getInstance().updateMapTransform(g2
                        .getTransform());
            }
            ds.draw(g2, debugState);
            g2.setClip(null);
        }
        g2.setTransform(pushed);
    }

    public void draw(Graphics g) {
        // remove if anything breaks

        if (canvasSize == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints
                .VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints
                .VALUE_STROKE_NORMALIZE);


        boolean isGraphicsStable = g2.getTransform().isIdentity();
        g2.setColor(Color.BLACK);
        g2.fill(new Rectangle(0, 0, canvasSize.width, canvasSize.height));
        AffineTransform pushed = g2.getTransform();
        g2.scale(spriteSize, spriteSize);
        g2.translate(unitOffX, unitOffY);
        if (timeline.getRound() >= 0) {
            drawHUD(g2);
            drawState(g2, isGraphicsStable);
        }
        g2.setTransform(pushed);
        //g2.scale(spriteSize, spriteSize);
        //g2.translate(unitOffX, unitOffY);
        if (cutScene != null) {
            g2.scale(spriteSize, spriteSize);
            g2.translate(unitOffX, unitOffY);
            cutScene.draw(g2);
            g2.setTransform(pushed);
        }
        //fps.updateFramerate(); renderFramerate();
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#beginIntroCutScene
     * (long)
     */
    public void beginIntroCutScene(long targetMillis) {
        while (cutScene == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        setCutSceneVisible(true);
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#setCutSceneVisible(boolean)
     */
    public void setCutSceneVisible(boolean visible) {
        if (visible) {
            if (timeline.getRound() == -1) {
                cutScene.step = DrawCutScene.Step.INTRO;
            } else {//if (timeline.getRound() == timeline.getNumRounds()) {
                cutScene.step = DrawCutScene.Step.OUTRO;
            }
        }
        cutScene.setVisible(visible);
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#fadeOutCutScene()
     */
    public void fadeOutCutScene() {
        cutScene.fadeOut();
    }

    public static void preloadGraphics() {
        DrawObject.loadAll();
    }

    public Dimension getPreferredSize(GraphicsDevice gd) {
        if (gd == null) {
            return new Dimension(Math.round(spriteSize * unitWidth), Math.round(spriteSize * unitHeight));
        } else {
            int width = gd.getDisplayMode().getWidth();
            int height = gd.getDisplayMode().getHeight();
            double scale = Math.min(1.0, Math.min(width / (spriteSize *
                    unitWidth), height / (spriteSize * unitHeight)));
            return new Dimension((int) Math.round(scale * spriteSize *
                    unitWidth), (int) Math.round(scale * spriteSize * unitHeight));
        }
    }

    protected void skipRounds(int rounds) {
        GameStateTimeline timeline = getTimeline();
        int newRound = Math.max(Math.min(timeline.getRound() + rounds,
                timeline.getNumRounds()), 0);
        timeline.setRound(newRound);
    }

    protected final void toggleFastForward() {
        MatchPlayer.getCurrent().speedup();
        //MatchPlayer.getCurrent().toggleFastForward();
    }

    protected final void toggleSlowDown() {
        MatchPlayer.getCurrent().slowdown();
    }

    @SuppressWarnings("empty")
    public void handleAction(char actionCommand) {

        actionCommand = Character.toUpperCase(actionCommand);

        switch (actionCommand) {
            case 'A':
                RenderConfiguration.toggleSpawnRadii();
                break;
            case 'B':
                RenderConfiguration.toggleBroadcast();
                break;
            case 'D':
                RenderConfiguration.toggleDiscrete();
                break;
            case 'E':
                RenderConfiguration.toggleEnergon();
                break;
            case 'F':
                toggleFastForward();
                break;
            case 'G':
                RenderConfiguration.toggleGridlines();
                break;
            case 'H':
                RenderConfiguration.toggleActionLines();
                break;
            case 'I':
                skipRounds(-50);
                break;
            case 'J':
                toggleSlowDown();
                break;
            case 'K':
                RenderConfiguration.toggleAttack();
                break;
            case 'M':
                RenderConfiguration.toggleAmbientMusic();
                break;
            case 'L':
                RenderConfiguration.toggleSupplyIndicators();
                break;
            case 'O':
                RenderConfiguration.toggleShowHats();
                break;
            case 'R':
                RenderConfiguration.toggleRangeHatch();
                break;
            case 'S':
                skipRounds(100);
                break;
            case 'T':
                RenderConfiguration.toggleSupplyTransfers();
                break;
            case 'U':
                RenderConfiguration.toggleCows();
                break;
            case 'V':
                RenderConfiguration.toggleIndicatorDots();
                break;
            case 'X':
                RenderConfiguration.toggleExplosions();
                break;
            default:
        }
    }
}
