package battlecode.client.viewer.render;

import battlecode.client.util.ImageFile;
import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.world.GameMap;
import battlecode.world.signal.IndicatorDotSignal;
import battlecode.world.signal.IndicatorLineSignal;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class DrawState extends AbstractDrawState<DrawObject> {
    private ArrayList<DoodadAnim> doodads;

    protected static final Stroke indicatorLineStroke = new BasicStroke(0.075f);

    private static class Factory implements GameStateFactory<DrawState> {

        public DrawState createState(GameMap map) {
            return new DrawState(map);
        }

        public DrawState cloneState(DrawState state) {
            return new DrawState(state);
        }

        public void copyState(DrawState src, DrawState dst) {
            dst.copyStateFrom(src);
            dst.doodads = src.doodads;
        }
    }

    public static final GameStateFactory<DrawState> FACTORY = new Factory();
    //private Map<Integer, DrawObject> groundUnits;
    //private Map<Integer, DrawObject> airUnits;
    private List<DrawObject> towers;
    // these need to be drawn before all the units,
    // so don't draw them from DrawObjects
    //private ArrayList<TeleportAnim> teleportAnims;
    private MapLocation[][] convexHullsA, convexHullsB;
    // number of blocks in current draw state
    //int[][] blockNumber;

    public DrawState() {
        groundUnits = new LinkedHashMap<Integer, DrawObject>();
        currentRound = -1;
    }

    private DrawState(GameMap map) {
        this();
        this.setGameMap(map);
    }

    private DrawState(DrawState clone) {
        this();
        copyStateFrom(clone);
        this.doodads = clone.doodads;
    }

    public void setGameMap(GameMap map) {
        super.setGameMap(map);
        doodads = new ArrayList<DoodadAnim>();
        int doodadAttemptCount = 0;
        Random r = new Random();
        int w = map.getWidth();
        int h = map.getHeight();

        origin = gameMap.getOrigin();

        rubble = new double[map.getWidth()][map.getHeight()];
        for (int i = 0; i < rubble.length; ++i) {
            for (int j = 0; j < rubble[i].length; ++j) {
                rubble[i][j] = map.initialRubbleAtLocation(i + origin.x, j +
                        origin.y);
            }
        }

        parts = new double[map.getWidth()][map.getHeight()];
        for (int i = 0; i < parts.length; ++i) {
            for (int j = 0; j < parts[i].length; ++j) {
                parts[i][j] = map.initialPartsAtLocation(i + origin.x, j +
                        origin.y);
            }
        }

        boolean[][] alreadyTaken = new boolean[w][h];
        DoodadAnim.DoodadType[] dTypes = DoodadAnim.DoodadType.values();
        for (int d = 0; d < doodadAttemptCount; d++) {
            ArrayList<MapLocation> possibleLocs = new ArrayList<MapLocation>();
            int doodadIndex = r.nextInt(dTypes.length);
            DoodadAnim.DoodadType dType = dTypes[doodadIndex];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    if (canPlace(alreadyTaken, i, j, dType.w, dType.h)) {
                        possibleLocs.add(new MapLocation(i, j));
                    }
                }
            }
            int chosenIndex = r.nextInt(possibleLocs.size());
            MapLocation chosenLoc = possibleLocs.get(chosenIndex);
            doodads.add(new DoodadAnim(new MapLocation(chosenLoc.x, chosenLoc
                    .y),
                    1, dType));
            fillFalse(alreadyTaken, chosenLoc.x, chosenLoc.y, dType.w, dType.h);
        }
    }

    private void fillFalse(boolean alreadyTaken[][],
                           int iStart, int jStart, int fillW, int fillH) {
        for (int i = iStart; i < iStart + fillW; i++) {
            for (int j = jStart; j < jStart + fillH; j++) {
                alreadyTaken[i][j] = false;
            }
        }

    }

    private boolean canPlace(boolean alreadyTaken[][],
                             int iStart, int jStart, int w, int h) {
        boolean allSquares = true;
        for (int i = iStart; i < (iStart + w); i++) {
            if (i >= gameMap.getWidth()) {
                allSquares = false;
                break;
            }
            for (int j = jStart; j < (jStart + h); j++) {
                if (j >= gameMap.getHeight()
                        || alreadyTaken[i][j]) {
                    allSquares = false;
                    break;
                }
            }
            if (!allSquares) {
                break;
            }
        }
        return allSquares;
    }

    protected DrawObject createDrawObject(RobotType type, Team team, int id) {
        return new DrawObject(currentRound, type, team, id, this);
    }

    protected DrawObject createDrawObject(DrawObject o) {
        return new DrawObject(currentRound, o);
    }

    public MapLocation[][] getConvexHullsA() {
        return convexHullsA;
    }

    public MapLocation[][] getConvexHullsB() {
        return convexHullsB;
    }

    public double getTeamResources(Team t) {
        return teamResources[t.ordinal()];
    }

    public double getResearchProgress(Team t, int i) {
        return researchProgress[t.ordinal()][i];
    }

    public DrawObject getDrawObject(int id) {
        try {
            return getRobot(id);
        } catch (AssertionError e) {
            return null;
        }
    }

    private void drawDragged(Graphics2D g2, DebugState debug, DrawObject obj) {
    /*
      MapLocation loc = obj.getLoc();
      float dx = debug.getDX(), dy = debug.getDY();
      g2.setColor(dragShadow);
      g2.fill(new Rectangle2D.Float(Math.round(loc.x + dx),
      Math.round(loc.y + dy), 1, 1));
      AffineTransform pushed = g2.getTransform(); // push
      g2.translate(dx, dy);
      obj.draw(g2, true);
      g2.setTransform(pushed); // pop;
    */
    }

    protected void postUpdateRound() {
        super.postUpdateRound();

        for (DoodadAnim doodad : doodads) {
            doodad.updateRound();
        }
    }

    /**
     * Draws the current game state. This method is always called from the
     * Swing event-dispatch thread, and in particular blocks calls to
     * updateRound.
     *
     * @param g2    The graphics context, transformed to MapLocation-space
     * @param debug The debug state, including MapLocation-space mouse state
     */
    public synchronized void draw(Graphics2D g2, DebugState debug) {
        int dragID = debug.getDragID();
        int focusID = debug.getFocusID();
        int hoverID = -1;
        MapLocation hoverLoc = null;
        long controlBits = 0;
        Iterable<Map.Entry<Integer, DrawObject>> drawableSet = getDrawableSet();


        if (drawableSet == null) {
            return;
        }

        AffineTransform pushed = g2.getTransform();
        g2.translate(gameMap.getOrigin().x,
                gameMap.getOrigin().y);
        for (DoodadAnim doodad : doodads) {
            doodad.draw(g2);
        }
        g2.setTransform(pushed);

        // draw rubble and parts
        for (int i = 0; i < gameMap.getWidth(); ++i) {
            for (int j = 0; j < gameMap.getHeight(); ++j) {
                int x = i + gameMap.getOrigin().x;
                int y = j + gameMap.getOrigin().y;

                // fill a tile with alpha based on how much rubble there is
                float lum = (float) Math.sqrt(Math.min(1.0, rubble[i][j] /
                        1000.0f));
                g2.setColor(new Color(0, 0, 0, lum));

                float size = 1f;
                float offset = ((1.0f - size) / 2);
                g2.fill(new Rectangle2D.Float(x + offset, y + offset,
                        size, size));

                // draw dots equal to number of parts
                g2.setColor(new Color(0.8f, 1.0f, 0.6f, 0.7f));
                for (int r = 0; r < parts[i][j] / 8; ++r) {
                    for (int c = 0; r * 10 + c < parts[i][j] && c < 8; ++c) {
                        g2.fill(new Rectangle2D.Float(x + c * 0.1f + 0.12f, y +
                                r * 0.1f + 0.12f, 0.06f, 0.06f));
                    }
                }
            }
        }

        for (IndicatorDotSignal s : indicatorDots) {
            if (RenderConfiguration.showIndicatorDots(s.team) && (focusID ==
                    -1 || focusID == s.robotID)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.fill(new Ellipse2D.Double(s.location.x + .1, s.location.y
                        + .1, .8, .8));
            }
        }
        g2.setStroke(indicatorLineStroke);
        for (IndicatorLineSignal s : indicatorLines) {
            if (RenderConfiguration.showIndicatorDots(s.team)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.draw(new Line2D.Double(s.loc1.x + .5, s.loc1.y + .5, s
                        .loc2.x + .5, s.loc2.y + .5));
            }
        }
        for (IndicatorDotSignal s : indicatorDots) {
            if (RenderConfiguration.showIndicatorDots(s.team) && (focusID ==
                    -1 || focusID == s.robotID)) {
                g2.setColor(new Color(s.red, s.green, s.blue));
                g2.fill(new Ellipse2D.Double(s.location.x + .1, s.location.y + .1, .8, .8));
            }
        }

        for (int layer = 0; layer < DrawObject.LAYER_COUNT; layer++) {
            for (Map.Entry<Integer, DrawObject> entry : drawableSet) {

                int id = entry.getKey();
                DrawObject obj = entry.getValue();
                if (obj.inTransport()) continue;

                if (id == dragID) {
                    drawDragged(g2, debug, obj);
                } else {
                    if (Math.abs(debug.getX() - obj.getDrawX() - 0.5) < 0.5
                            && Math.abs(debug.getY() - obj.getDrawY() - 0.5) < 0.5) {
                        hoverID = id;
                        hoverLoc = obj.getLocation();
                        controlBits = obj.getControlBits();
                    }
                    obj.draw(g2, id == focusID || id == hoverID,
                            obj.getLocation().y == origin.y + gameMap.getHeight() - 1,
                            layer);
                }
            }
        }

        if (!debug.isDragging()) {
            debug.setTarget(hoverID, hoverLoc, controlBits);
        }

    }
}
