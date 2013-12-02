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
  private float scaleSize;
  // number of blocks at each square
  // origin of the map
  MapLocation origin;
  private int imgSize = 32;

  // prerendered images
  private BufferedImage prerender;
  private BufferedImage roadPrerender;

  // for storing the loaded walls
  private BufferedImage[] tiles;
  private byte[][] tileIndices;
  // for the stored roads
  private BufferedImage roadImages[][];
  
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
    
    for (int i = 0; i < mapWidth; i++) {
      for (int j = 0; j < mapHeight; j++) {
        byte index = tileIndices[i][j];
        assert 0 <= index && index < 16;
        g2.drawImage(tiles[index], null, imgSize * i, imgSize * j);
      }
    }
   
    g2.dispose();
  }

  public void prerenderMap(BufferedImage bg) {
    scaleSize = (float) bg.getWidth() / mapWidth;
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
    g2.scale(1.0 / scaleSize, 1.0 / scaleSize);

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


  public void loadMapArt()  {
    TerrainTile[][] map = m.getTerrainMatrix();
    // this code calculates which tile to use to represent the wall edges
    tileIndices = new byte[mapWidth + 1][mapHeight + 1]; // init tileIndices
    for (int j = 0; j <= mapHeight; j++) for (int i = 0; i <= mapWidth; i++) {
        int top = (j == 0 ? 0x03 : (tileIndices[i][j - 1] & 0x03));
        int bottom = (j == mapHeight ? 0x03 : (i == 0 || map[i - 1][j] == VOID ? 0x02 : 0x00)
                      | (i == mapWidth || map[i][j] == VOID ? 0x01 : 0x00));
        tileIndices[i][j] = (byte) (top << 2 | bottom);
      }
    // this image has the tiles for all possible wall types
    ImageFile terrainImg = new ImageFile("art/terrain.png");
    BufferedImage image = terrainImg.image;
    assert image.getWidth() == image.getHeight();
    imgSize = image.getWidth() / 4;
    tiles = new BufferedImage[16];

    byte[][] imgToMap = {
      {8, 1, 7, 14},
      {0, 5, 15, 10},
      {2, 4, 13, 11},
      {9, 3, 6, 12}};
    for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) {
        tiles[imgToMap[row][col]] =
          image.getSubimage(col * imgSize, row * imgSize, imgSize, imgSize);
      }

    prerender = RenderConfiguration.createCompatibleImage(imgSize * mapWidth,
                                                          imgSize * mapHeight);


    terrainImg.unload();

    scaleSize = imgSize;

    // set up the road tiles
    ImageFile roadImg = new ImageFile("art/roads.png"); // actual rendering
    BufferedImage roadAtlas = roadImg.image;
    int roadPixelSize = roadAtlas.getWidth() / 2;
    roadImages = new BufferedImage[2][2];
    for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++) {
        roadImages[row][col] = roadAtlas.getSubimage(col * roadPixelSize,
                                                     row * roadPixelSize,
                                                     roadPixelSize,
                                                     roadPixelSize);
      }

    roadImg.unload();
  }
}
