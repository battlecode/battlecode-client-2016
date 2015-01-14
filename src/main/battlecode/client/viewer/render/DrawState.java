package battlecode.client.viewer.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import battlecode.client.util.ImageFile;
import battlecode.client.viewer.AbstractDrawState;
import battlecode.client.viewer.DebugState;
import battlecode.client.viewer.FluxDepositState;
import battlecode.client.viewer.GameStateFactory;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.GameConstants;
import battlecode.common.TerrainTile;
import battlecode.serial.RoundStats;
import battlecode.world.GameMap;
import battlecode.world.signal.IndicatorDotSignal;
import battlecode.world.signal.IndicatorLineSignal;

public class DrawState extends AbstractDrawState<DrawObject> {
    private ArrayList<DoodadAnim> doodads;
	
  private static final ImageFile rNuker = new ImageFile("art/nuker1.png");
  private static final ImageFile rNuker2 = new ImageFile("art/nuker2.png");
  private static final ImageFile rNukeb = new ImageFile("art/nukeb1.png");
  private static final ImageFile rNukeb2 = new ImageFile("art/nukeb2.png");
  private static final ImageFile rexplode = new ImageFile("art/nukeexplode.png");
	
  protected static final Color dragShadow = new Color(0.5f, 0.5f, 0.5f, 0.5f);
  protected static final Color linkNone = new Color(0.f,0.f,0.f);
  protected static final Color linkA = new Color(1.f,0.f,0.f);
  protected static final Color linkB = new Color(0.f,0.f,1.f);
  protected static final Color linkBoth = new Color(.75f,0.f,.75f);
  protected static final ImageFile encampment = new ImageFile("art/encampment.png");
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
    airUnits = new LinkedHashMap<Integer, DrawObject>();
    encampments = new HashSet<MapLocation>();
    towers = new LinkedList<DrawObject>();
    fluxDeposits = new LinkedHashMap<Integer, FluxDepositState>();
    currentRound = -1;
    convexHullsA = new MapLocation[0][];
    convexHullsB = new MapLocation[0][];
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
	boolean[][] alreadyTaken = new boolean[w][h];
	DoodadAnim.DoodadType[] dTypes = DoodadAnim.DoodadType .values();
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
	    doodads.add(new DoodadAnim(new MapLocation(chosenLoc.x,						       chosenLoc.y),
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
	TerrainTile[][] mapTiles = gameMap.getTerrainMatrix();
	boolean allSquares = true;
	for (int i = iStart; i < (iStart + w); i++) {
	    if (i >= gameMap.getWidth()) {
		allSquares = false;
		break;
	    }
	    for (int j = jStart; j < (jStart + h); j++) {
		if (j >= gameMap.getHeight()
		    || mapTiles[i][j].isTraversable()
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

  public synchronized void apply(RoundStats stats) {
      this.stats = stats;
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
      MapLocation loc = obj.getLocation();
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

	for(DoodadAnim doodad : doodads) {
	    doodad.updateRound();
	}
    }

  /**
   * Draws the current game state. This method is always called from the
   * Swing event-dispatch thread, and in particular blocks calls to
   * updateRound.
   * @param g2 The graphics context, transformed to MapLocation-space
   * @param debug The debug state, including MapLocation-space mouse state
   */
  public synchronized void draw(Graphics2D g2, DebugState debug) {
      if (RenderConfiguration.showSpawnRadii()) {
        /*
          for (DrawObject tower : towers) {
          tower.drawSpawnRadius(g2);
          }
        */
      }
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
      g2.translate(gameMap.getMapOrigin().x,
		   gameMap.getMapOrigin().y);
      for (DoodadAnim doodad : doodads) {
	  doodad.draw(g2);
      }
      g2.setTransform(pushed);
      
      // ore densities
      double maxDensity = 0.0;
      for (int i = 0; i < gameMap.getWidth() && RenderConfiguration.showCows(); i++) {
	  for (int j = 0; j < gameMap.getHeight(); j++) {
	      int x = i + gameMap.getMapOrigin().x;
	      int y = j + gameMap.getMapOrigin().y;
	      double density =  gameMap.getInitialOre(new MapLocation(x, y))
		- getOreAtLocation(new MapLocation(x, y));
		    
	      maxDensity = Math.max(maxDensity, density);
        }
      }
      double thresholdDensity = Math.max(5, .1 * maxDensity);
      if (RenderConfiguration.threshCows()) {
        maxDensity -= thresholdDensity;
      }
      for (int i = 0; i < gameMap.getWidth() && RenderConfiguration.showCows(); i++) {
	  for (int j = 0; j < gameMap.getHeight(); j++) {
	      int x = i + gameMap.getMapOrigin().x;
	      int y = j + gameMap.getMapOrigin().y;
	      
	      double density =  gameMap.getInitialOre(new MapLocation(x, y))
		  - getOreAtLocation(new MapLocation(x, y));
		    
	      if(RenderConfiguration.threshCows()) {
		  if(density < thresholdDensity) continue;
		  else density -= thresholdDensity;
	      }
	      //I'm leaving the different coloring code intact in case we
	      // think of something cool
	      float lum = (float)(.5 * density / maxDensity + .25);
	      lum = Math.max(Math.min(lum, 1.0f), 0.0f);
	      lum = .75f;
	      float r = lum;
	      float b = lum;
	      float g = lum;
	      g2.setColor(new Color(r, g, b, .5f));
	      // cap at the max possible size
	      float maxSize = .45f;
	      float maxPossible = gameMap.getMaxInitialOre();
	      float size = (float) Math.min(Math.sqrt(density / maxPossible), 1.0f);
	      size *= maxSize;
	      // make appear at the center
	      float offset = ((1.0f - size) / 2);
	      g2.fill(new Rectangle2D.Float(x + offset, y + offset, size, size));
	  }
      }
      for(IndicatorDotSignal s : indicatorDots) {
        if(RenderConfiguration.showIndicatorDots(s.team)&&(focusID==-1||focusID==s.robotID)) {
            g2.setColor(new Color(s.red,s.green,s.blue));
            g2.fill(new Ellipse2D.Double(s.location.x+.1,s.location.y+.1,.8,.8));
        }
      }
      g2.setStroke(indicatorLineStroke);
      for(IndicatorLineSignal s : indicatorLines) {
        if(RenderConfiguration.showIndicatorDots(s.team)) {
            g2.setColor(new Color(s.red,s.green,s.blue));
            g2.draw(new Line2D.Double(s.loc1.x+.5,s.loc1.y+.5,s.loc2.x+.5,s.loc2.y+.5));
        }
      }
      for(IndicatorDotSignal s : indicatorDots) {
        if(RenderConfiguration.showIndicatorDots(s.team)&&(focusID==-1||focusID==s.robotID)) {
            g2.setColor(new Color(s.red,s.green,s.blue));
            g2.fill(new Ellipse2D.Double(s.location.x+.1,s.location.y+.1,.8,.8));
        }
      }

      for (int layer = 0; layer < DrawObject.LAYER_COUNT; layer++) {
	  for (Map.Entry<Integer, DrawObject> entry : drawableSet) {

	      int id = entry.getKey();
	      DrawObject obj = entry.getValue();
	      if(obj.inTransport()) continue;

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
			   obj.getLocation().y==origin.y+gameMap.getHeight()-1,
			   layer);
	      }
	  }
      }

        /*
      AffineTransform pushed = g2.getTransform();
      for (Team t : new Team[]{Team.A, Team.B})
      {
        g2.setTransform(pushed);
        int research = (int)(1.000001*getResearchProgress(t, Upgrade.NUKE.ordinal())*Upgrade.NUKE.numRounds);
        if (research > 350 && research<370)
        {
          DrawObject o = getHQ(t);
          g2.translate(o.getDrawX(), o.getDrawY());
          g2.translate(0.0, -(research-350)*0.45);
          g2.translate(0.0, -1);
          BufferedImage bi = t==Team.A ? rNuker.image : rNukeb.image;
          AffineTransform trans = AffineTransform.getScaleInstance((1.0 / bi.getWidth()), (1.0 / bi.getWidth()));
          g2.drawImage(bi, trans, null);
        } else if (research > 375)
        {
          DrawObject o = getHQ(t.opponent());
          g2.translate(o.getDrawX(), o.getDrawY());
          g2.translate(0.0, -(404-research)*0.45);
          g2.translate(0.0, -1);
          BufferedImage bi = t==Team.A ? rNuker2.image : rNukeb2.image;
          AffineTransform trans = AffineTransform.getScaleInstance((1.0 / bi.getWidth()), (1.0 / bi.getWidth()));
          g2.drawImage(bi, trans, null);
          if (research == 404)
          {
            double scale = 6.0;
            BufferedImage ei = rexplode.image;
            trans = AffineTransform.getScaleInstance((scale / ei.getWidth()), (scale / ei.getWidth()));
            g2.translate(-(scale-1.0)/2, -(scale-1.0)/2-2);
            g2.drawImage(ei, trans, null);
          }
        }
        	
        	
      }
        */
        
      if (!debug.isDragging()) {
        debug.setTarget(hoverID, hoverLoc, controlBits);
      }

    }
}
