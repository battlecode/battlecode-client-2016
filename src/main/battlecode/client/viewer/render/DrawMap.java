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

  private final int subtileHeight = 4; // 4 x 4
  private final int tileCount = 3; // empty, full, rounded
  private final int subtileWidth = tileCount * subtileHeight; // side by side

  
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
    
    for (int x = 0; x < mapWidth; x++) {
      for (int y = 0; y < mapHeight; y++) {
        for (int sx = 0; sx < subtileHeight; sx++) {
          for (int sy = 0; sy < subtileHeight; sy++) {
            byte index = subtileIndices[mapIndex(x, y, sx, sy)];
            g2.drawImage(roadTiles[sx + (subtileHeight * index)][sy], null,
                         (subtileHeight * x + sx) * locPixelWidth / subtileHeight,
                         (subtileHeight * y + sy) * locPixelWidth / subtileHeight);
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


  private int mapIndex(int x, int y, int sx, int sy) {
    int index = (x * subtileHeight + sx) * mapHeight * subtileHeight
      + (y * subtileHeight + sy);
    return index;
  }

  public void loadMapArt()  {
    TerrainTile[][] map = m.getTerrainMatrix();

    // set up the road tiles
    ImageFile roadImg = new ImageFile("art/roads.png"); // actual rendering
    BufferedImage roadAtlas = roadImg.image;
    locPixelWidth = roadAtlas.getWidth() / tileCount;
    int subtileYPixels = locPixelWidth / subtileHeight;
      
    assert roadAtlas.getWidth() % tileCount == 0; // the road Atlas has 3 images side by side

    roadTiles = new BufferedImage[subtileWidth][subtileHeight];
    
    for (int y = 0; y < subtileHeight; y++) for (int x = 0; x < subtileWidth; x++) {
        roadTiles[x][y] = roadAtlas.getSubimage(x * subtileYPixels,
                                                y * subtileYPixels,
                                                subtileYPixels,
                                                subtileYPixels);
      }
    
    prerender = RenderConfiguration.createCompatibleImage(locPixelWidth * mapWidth,
                                                          locPixelWidth * mapHeight);
    
    subtileIndices = new byte[subtileHeight * mapHeight * subtileHeight * mapWidth];
    for (int i = 0; i < subtileHeight * mapHeight * subtileHeight * mapWidth; i++) {
        subtileIndices[i] = 0; // default value
    }
    for (int x = 0; x < mapWidth; x++) for (int y = 0; y < mapHeight; y++) {
        TerrainTile typeHere = map[x][y];
        if (typeHere != ROAD)
        {
          continue;
        }
        boolean top = (y - 1 >= 0) && (map[x][y - 1] == typeHere);
        boolean bot = (y + 1 < mapHeight) && (map[x][y + 1] == typeHere);
        boolean left = (x - 1 >= 0) && (map[x - 1][y] == typeHere);
        boolean right = (x + 1 < mapWidth) && (map[x + 1][y] == typeHere);
        subtileIndices[mapIndex(x, y, 1, 0)] = subtileIndices[mapIndex(x, y, 2, 0)] = (byte)(top ? 1 : 0);
        subtileIndices[mapIndex(x, y, 1, 3)] = subtileIndices[mapIndex(x, y, 2, 3)] = (byte)(bot ? 1 : 0);
        subtileIndices[mapIndex(x, y, 0, 1)] = subtileIndices[mapIndex(x, y, 0, 2)] = (byte)(left ? 1 : 0);
        subtileIndices[mapIndex(x, y, 3, 1)] = subtileIndices[mapIndex(x, y, 3, 2)] = (byte)(right ? 1 : 0);
        subtileIndices[mapIndex(x, y, 1, 1)] = (byte)((!top && !left) ? 2 : 1);
        subtileIndices[mapIndex(x, y, 1, 2)] = (byte)((!bot && !left) ? 2 : 1);
        subtileIndices[mapIndex(x, y, 2, 1)] = (byte)((!top && !right) ? 2 : 1);
        subtileIndices[mapIndex(x, y, 2, 2)] = (byte)((!bot && !right) ? 2 : 1);
      }
    
    roadImg.unload();
  }
}
