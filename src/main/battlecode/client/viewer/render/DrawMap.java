package battlecode.client.viewer.render;

import java.awt.BasicStroke;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;

import battlecode.client.util.ImageFile;
import battlecode.client.viewer.FluxDepositState;
import battlecode.common.MapLocation;
import battlecode.common.Team;
import battlecode.common.TerrainTile;
import java.awt.AlphaComposite;

import static battlecode.common.TerrainTile.*;

public class DrawMap {

  private int mapWidth;
  private int mapHeight;
  private Stroke gridStroke;
  // number of blocks at each square
  // origin of the map
  MapLocation origin;
  private int locPixelWidth = 32;

  // prerendered images
  private BufferedImage prerender;
  private BufferedImage roadPrerender;

  // for storing the loaded walls
  //private BufferedImage[] tiles;
  //private byte[][] tileIndices;
  // for the stored roads
  private BufferedImage roadTiles[][];
  // 0-2 which image to draw from, 3rd bit for road v wall
  private byte[] subtileIndices;

  private final int subtileRows = 4; // 4 x 4
  private final int tileCount = 3; // empty, full, rounded
  private final int subtileCols = tileCount * subtileRows; // side by side

  
  public battlecode.world.GameMap m;

  public DrawMap(battlecode.world.GameMap map) {
    mapWidth = map.getWidth();
    mapHeight = map.getHeight();
    origin = map.getMapOrigin();

    this.m = map;
    
    loadMapArt();

    //FIXME: commented out for now
//    if (!RenderConfiguration.getInstance().isTournamentMode()) {
    prerenderMap(map);
//    }
    gridStroke = new BasicStroke(0.1f / RenderConfiguration.getInstance().getSpriteSize());

  }

  public void redraw() {
    prerenderMap(this.m);
  }

  public void prerenderMap(battlecode.world.GameMap m) {
    Graphics2D g2 = prerender.createGraphics();
    
    for (int r = 0; r < mapHeight; r++) {
      for (int c = 0; c < mapWidth; c++) {
        for (int sr = 0; sr < subtileRows; sr++) {
          for (int sc = 0; sc < subtileRows; sc++) {
            byte index = subtileIndices[mapIndex(r, c, sr, sc)];
            // why row and col don't need to be converted is still under investigation
            g2.drawImage(roadTiles[sr][sc + (subtileRows * index)], null,
                         (subtileRows * r + sr) * locPixelWidth / subtileRows,
                         (subtileRows * c + sc) * locPixelWidth / subtileRows);
          }
        }
      }
    }
   
    g2.dispose();
  }

  public void prerenderMap(BufferedImage bg) {
    prerender = bg;
  }

  public int getMapWidth() {
    return mapWidth;
  }

  public int getMapHeight() {
    return mapHeight;
  }

  public void draw(Graphics2D g2, DrawState ds) {
    AffineTransform pushed = g2.getTransform();
    
    g2.scale(1.0 / locPixelWidth, 1.0 / locPixelWidth);

    g2.drawImage(prerender, null, null);
        
    g2.setTransform(pushed);
    if (RenderConfiguration.showGridlines()) {
      g2.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));
      g2.setStroke(gridStroke);
      Line2D.Float gridline = new Line2D.Float(0, 0, 0, mapHeight);
      for (int i = 1; i < mapWidth; i += 1) {
        gridline.x1 = gridline.x2 = i;
        g2.draw(gridline);
      }
      gridline.x1 = 0;
      gridline.x2 = mapWidth;
      for (int i = 1; i < mapHeight; i += 1) {
        gridline.y1 = gridline.y2 = i;
        g2.draw(gridline);
      }
    }
  }


  private int mapIndex(int r, int c, int sr, int sc) {
    int index = (r * subtileRows + sr) * mapWidth * subtileRows
      + (c * subtileRows + sr);
    return index;
  }

  public void loadMapArt()  {
    TerrainTile[][] map = m.getTerrainMatrix();

    // set up the road tiles
    ImageFile roadImg = new ImageFile("art/roads.png"); // actual rendering
    BufferedImage roadAtlas = roadImg.image;
    locPixelWidth = roadAtlas.getWidth() / tileCount;
    int subtileWidth = locPixelWidth / subtileRows;
      
    assert roadAtlas.getWidth() % tileCount == 0; // the road Atlas has 3 images side by side

    roadTiles = new BufferedImage[subtileRows][subtileCols];
    
    for (int row = 0; row < subtileRows; row++) for (int col = 0; col < subtileCols; col++) {
        roadTiles[row][col] = roadAtlas.getSubimage(col * subtileWidth,
                                                    row * subtileWidth,
                                                    subtileWidth,
                                                    subtileWidth);
      }
    
    prerender = RenderConfiguration.createCompatibleImage(locPixelWidth * mapWidth,
                                                          locPixelWidth * mapHeight);
    
    subtileIndices = new byte[subtileRows * mapHeight * subtileRows * mapWidth];
    for (int i = 0; i < subtileRows * mapHeight * subtileRows * mapWidth; i++) {
        subtileIndices[i] = 0; // default value
    }
    for (int r = 0; r < mapHeight; r++) for (int c = 0; c < mapWidth; c++) {
        TerrainTile typeHere = map[r][c];
        if (typeHere != ROAD)
        {
          continue;
        }
        boolean top = (r - 1 >= 0) && (map[r - 1][c] == typeHere);
        boolean bot = (r + 1 < mapHeight) && (map[r + 1][c] == typeHere);
        boolean left = (c - 1 >= 0) && (map[r][c - 1] == typeHere);
        boolean right = (c + 1 < mapWidth) && (map[r][c + 1] == typeHere);
        subtileIndices[mapIndex(r, c, 0, 1)] = subtileIndices[mapIndex(r, c, 0, 2)] = (byte)(top ? 1 : 0);
        subtileIndices[mapIndex(r, c, 3, 1)] = subtileIndices[mapIndex(r, c, 3, 2)] = (byte)(bot ? 1 : 0);
        subtileIndices[mapIndex(r, c, 1, 0)] = subtileIndices[mapIndex(r, c, 2, 0)] = (byte)(left ? 1 : 0);
        subtileIndices[mapIndex(r, c, 1, 3)] = subtileIndices[mapIndex(r, c, 2, 3)] = (byte)(right ? 1 : 0);
        subtileIndices[mapIndex(r, c, 1, 1)] = (byte)((!top && !left) ? 2 : 1);
        subtileIndices[mapIndex(r, c, 1, 2)] = (byte)((!top && !right) ? 2 : 1);
        subtileIndices[mapIndex(r, c, 2, 1)] = (byte)((!bot && !left) ? 2 : 1);
        subtileIndices[mapIndex(r, c, 2, 2)] = (byte)((!bot && !right) ? 2 : 1);
      }
    
    roadImg.unload();
  }
}
