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
import java.awt.Transparency;
import java.awt.GraphicsEnvironment;
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

  private BufferedImage mapBG;

  // for storing the loaded voids
  //private BufferedImage[] tiles;
  //private byte[][] tileIndices;
  // for the stored roads
  private BufferedImage roadTiles[][];
  private BufferedImage voidTiles[][];
  // 0-2 which image to draw from, 3rd bit for road v void
  private byte[] subtileIndices;

  private final int subtileHeight = 4; // 4 x 4
  private final int roadTileCount = 3; // empty, full, rounded
  private final int roadSubtileWidth = roadTileCount * subtileHeight; // side by side
  private final int innerVoidTileCount = 5; // inCorners, horiz, vert, outCorners, water
  private final int innerVoidSubtileWidth = innerVoidTileCount * subtileHeight; // side by side
  private final byte atlasChoiceBit = 5;
  
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
    gridStroke = new BasicStroke(0.3f / RenderConfiguration.getInstance().getSpriteSize());

  }

  public void prerenderMap(battlecode.world.GameMap m) {
    Graphics2D g2 = prerender.createGraphics();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

    for (int x = 0; x < mapWidth; x += mapBG.getWidth() / locPixelWidth) {
      for (int y = 0; y < mapHeight; y += mapBG.getHeight() / locPixelWidth) {
        g2.drawImage(mapBG, null, x * locPixelWidth, y * locPixelWidth);
      }
    }

    for (int x = 0; x < mapWidth; x++) {
      for (int y = 0; y < mapHeight; y++) {
        for (int sx = 0; sx < subtileHeight; sx++) {
          for (int sy = 0; sy < subtileHeight; sy++) {
            byte index = subtileIndices[mapIndex(x, y, sx, sy)];
            byte atlasSelect = (1 << atlasChoiceBit);
            byte indexMask = (byte)(atlasSelect - (byte)1);
            BufferedImage tileset[][] = ((index & atlasSelect) != 0) ? voidTiles : roadTiles;
            index &= indexMask;
            g2.drawImage(tileset[sx + (subtileHeight * index)][sy], null,
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

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    
    g2.scale(1.0 / locPixelWidth, 1.0 / locPixelWidth);
   
    g2.drawImage(prerender, null, null);
    
    g2.setTransform(pushed);
    if (RenderConfiguration.showGridlines()) {
      g2.setColor(new Color(0.7f, 0.7f, 0.7f, 1.0f));
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

    mapBG = (new ImageFile ("art/map_bg.png")).image;

    // set up the road tiles
    ImageFile roadImg = new ImageFile("art/roads.png"); // actual rendering
    ImageFile voidImg = new ImageFile("art/innervoid.png"); // actual rendering
    BufferedImage roadAtlas = roadImg.image;
    BufferedImage voidAtlas = voidImg.image;
    locPixelWidth = roadAtlas.getWidth() / roadTileCount;
    int subtileYPixels = locPixelWidth / subtileHeight;
      
    assert roadAtlas.getWidth() % roadTileCount == 0; // the road Atlas has 3 images side by side

    roadTiles = new BufferedImage[roadSubtileWidth][subtileHeight];
    voidTiles = new BufferedImage[innerVoidSubtileWidth][subtileHeight];
    
    
    for (int y = 0; y < subtileHeight; y++) for (int x = 0; x < roadSubtileWidth; x++) {
        roadTiles[x][y] = roadAtlas.getSubimage(x * subtileYPixels,
                                                y * subtileYPixels,
                                                subtileYPixels,
                                                subtileYPixels);
              }
    for (int y = 0; y < subtileHeight; y++) for (int x = 0; x < innerVoidSubtileWidth; x++) {
        voidTiles[x][y] = voidAtlas.getSubimage(x * subtileYPixels,
                                                y * subtileYPixels,
                                                subtileYPixels,
                                                subtileYPixels);
      }
    
    prerender = GraphicsEnvironment.getLocalGraphicsEnvironment()
      .getDefaultScreenDevice()
      .getDefaultConfiguration()
      .createCompatibleImage(locPixelWidth * mapWidth, locPixelWidth * mapHeight,
                             Transparency.TRANSLUCENT);
                                                      
    
    subtileIndices = new byte[subtileHeight * mapHeight * subtileHeight * mapWidth];
    for (int i = 0; i < subtileHeight * mapHeight * subtileHeight * mapWidth; i++) {
        subtileIndices[i] = 0; // default value
    }
    for (int x = 0; x < mapWidth; x++) for (int y = 0; y < mapHeight; y++) {
        TerrainTile typeHere = map[x][y];
        if (typeHere != VOID)
        {
          continue;
        }
        // to prevent reading out of array
        boolean topInBounds = (y - 1 >= 0);
        boolean botInBounds = (y + 1 < mapHeight);
        boolean leftInBounds = (x - 1 >= 0);
        boolean rightInBounds = (x + 1 < mapWidth);
        // same in adjacent?
        boolean top =  !topInBounds || (map[x][y - 1] == typeHere); 
        boolean bot =  !botInBounds || (map[x][y + 1] == typeHere);
        boolean left = !leftInBounds || (map[x - 1][y] == typeHere);
        boolean right =  !rightInBounds || (map[x + 1][y] == typeHere);
        // corners
        boolean topLeft = top && left && (!topInBounds || !leftInBounds || map[x - 1][y - 1] == typeHere);
        boolean topRight = top && right && (!topInBounds || !rightInBounds || map[x + 1][y - 1] == typeHere);
        boolean botLeft = bot && left && (!botInBounds || !leftInBounds || map[x - 1][y + 1] == typeHere);
        boolean botRight = bot && right && (!botInBounds || !rightInBounds || map[x + 1][y + 1] == typeHere);
         
        if(typeHere == VOID)
        {
          // inCorners, horiz, vert, outCorners, water
          // bridges
          subtileIndices[mapIndex(x, y, 1, 0)] = subtileIndices[mapIndex(x, y, 2, 0)]
            = (byte)(top ? 4 : 3);
          subtileIndices[mapIndex(x, y, 1, 3)] = subtileIndices[mapIndex(x, y, 2, 3)]
            = (byte)(bot ? 4 : 3);
          subtileIndices[mapIndex(x, y, 0, 1)] = subtileIndices[mapIndex(x, y, 0, 2)]
            = (byte)(left ? 4 : 3);
          subtileIndices[mapIndex(x, y, 3, 1)] = subtileIndices[mapIndex(x, y, 3, 2)]
            = (byte)(right ? 4 : 3);
          // center
          subtileIndices[mapIndex(x, y, 1, 1)]
            = subtileIndices[mapIndex(x, y, 1, 2)]
            = subtileIndices[mapIndex(x, y, 2, 1)]
            = subtileIndices[mapIndex(x, y, 2, 2)]
            = 4; // always water
          
          subtileIndices[mapIndex(x, y, 0, 0)]
            = (byte)(topLeft ? 4 : (left ? (top ? 0 : 1) : (top ? 2 : 3)));
          subtileIndices[mapIndex(x, y, 3, 0)]
            = (byte)(topRight ? 4 : (right ? (top ? 0 : 1) : (top ? 2 : 3)));
          subtileIndices[mapIndex(x, y, 0, 3)] 
            = (byte)(botLeft ? 4 : (left ? (bot ? 0 : 1) : (bot ? 2 : 3)));
          subtileIndices[mapIndex(x, y, 3, 3)]
            = (byte)(botRight ? 4 : (right ? (bot ? 0 : 1) : (bot ? 2 : 3)));
          
          for (int sx = 0; sx < 4; sx++) for (int sy = 0; sy < 4; sy++) {
              subtileIndices[mapIndex(x, y, sx, sy)] |= (1 << atlasChoiceBit);
            }
        }
      }
    roadImg.unload();
  }
}
