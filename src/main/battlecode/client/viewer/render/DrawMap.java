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
    private BufferedImage prerender;
    private Stroke gridStroke;
    private float scaleSize;
    // number of blocks at each square
    // origin of the map
    MapLocation origin;
    private int imgSize = 32;

    public DrawMap(battlecode.world.GameMap map) {
        mapWidth = map.getWidth();
        mapHeight = map.getHeight();
        origin = map.getMapOrigin();

        //FIXME: commented out for now
//		if (!RenderConfiguration.getInstance().isTournamentMode()) {
        prerenderMap(map);
//		}
        gridStroke = new BasicStroke(0.1f / RenderConfiguration.getInstance().getSpriteSize());

    }

    public void prerenderMap(battlecode.world.GameMap m) {
        TerrainTile[][] map = m.getTerrainMatrix();

        byte[][] indices = new byte[mapWidth + 1][mapHeight + 1]; // init indices
        for (int j = 0; j <= mapHeight; j++) for (int i = 0; i <= mapWidth; i++) {
                int top = (j == 0 ? 0x03 : (indices[i][j - 1] & 0x03));
                int bottom = (j == mapHeight ? 0x03 : (i == 0 || map[i - 1][j] == VOID ? 0x02 : 0x00)
                        | (i == mapWidth || map[i][j] == VOID ? 0x01 : 0x00));
                indices[i][j] = (byte) (top << 2 | bottom);
            }
        ImageFile terrainImg = new ImageFile("art/terrain.png"); // actual rendering
        BufferedImage image = terrainImg.image;
        assert image.getWidth() == image.getHeight();
        imgSize = image.getWidth() / 4;
        BufferedImage[] tiles = new BufferedImage[16];
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


        Graphics2D g2 = prerender.createGraphics();
        for (int i = 0; i <= mapWidth; i++) for (int j = 0; j <= mapHeight; j++) {
                byte index = indices[i][j];
                assert 0 <= index && index < 16;
                //g2.drawImage(tiles[index], null, imgSize * i - imgSize / 2, imgSize * j - imgSize / 2);
            }
        // find max height

        for (int i = 0; i < mapWidth; i++) for (int j = 0; j < mapHeight; j++) {
                if (map[i][j].getType() == TerrainTile.VOID)
                    continue;

                float height = 0;
                g2.setColor(new Color(height, height, height, 0.5f));
                g2.fill(new Rectangle(imgSize * i, imgSize * j, imgSize, imgSize));
            }


        // prerender the flux deposits
        // TODO: GET RID OF THIS
		/*InternalFluxDeposit deposits[] = m.getDeposits();
        //MapLocation deposits[] = m.getDepositLocations();
        MapLocation origin = m.getMapOrigin();
        for(int i = 0; i < deposits.length; ++i) {
        MapLocation l = deposits[i].getLocation();
        int x = l.getX() - origin.getX();
        int y = l.getY() - origin.getY();
        g2.setColor(new Color(0.f, 1.0f, 0.f, 1.0f));
        g2.fillOval(x * imgSize, y * imgSize, imgSize, imgSize);
        }*/

        g2.dispose();
        terrainImg.unload();

        scaleSize = imgSize;
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

        
        byte[][] flux = ds.getFlux();
        for (int xi = 0; xi < mapWidth; xi++) {
            for (int yi = 0; yi < mapHeight; yi++) {
                float fFlux = flux[yi][xi]/64.f;
				float fHeight = ds.getGameMap().getTerrainMatrix()[xi][yi].getHeight()/32.f;
				float green = fHeight==0.f?0.f:.25f+.75f*fHeight;
				//lighter version that I didn't like as much
				//float green = 1.f-(1.f-fHeight)*(1.f-fFlux*fFlux/2.f);
				float red = green * (1.f - fFlux);
				//Flash client uses these colors
				//float green = fHeight * (1.f-fFlux/2.f) + fFlux/2.f;
				//float red = fHeight * (1.f-fFlux/2.f) + (1.f-fFlux) * fFlux/2.f;
                g2.setColor(new Color(red, green, red, 1.0f));
                //g2.setStroke(noStroke);
                int shift = 0;
                g2.fill(new Rectangle(imgSize * xi + shift / 2, imgSize * yi + shift / 2, imgSize - shift, imgSize - shift));
            }
        }
        /*
        // display block heights if needed
        if (RenderConfiguration.showBlocks() && blockNumberDeltas != null) {
        renderHeights(g2, blockNumberDeltas);
        }
         */






        //g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) 0.8));
        //g2.drawImage(prerender, null, null);
        Polygon hullPoly = new Polygon();
        MapLocation[][] hulls = ds.getConvexHullsA();
        g2.setStroke(new BasicStroke(5.0f));
        for (int i = 0; i < hulls.length; i++) {
        	hullPoly.reset();
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.getX() - ds.getGameMap().getMapOrigin().getX(),
                        sy = start.getY() - ds.getGameMap().getMapOrigin().getY(),
                        ex = end.getX() - ds.getGameMap().getMapOrigin().getX(),
                        ey = end.getY() - ds.getGameMap().getMapOrigin().getY();
                hullPoly.addPoint(sx* 32 + 16, sy* 32 + 16);
                
       //         g2.setColor(new java.awt.Color(100, 0, 0));
         //       g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }
            g2.setColor(new java.awt.Color(255,150,0, 180));
            g2.fill(hullPoly);
            
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.getX() - ds.getGameMap().getMapOrigin().getX(),
                        sy = start.getY() - ds.getGameMap().getMapOrigin().getY(),
                        ex = end.getX() - ds.getGameMap().getMapOrigin().getX(),
                       ey = end.getY() - ds.getGameMap().getMapOrigin().getY();
              //  hullPoly.addPoint(sx* 32 + 16, sy* 32 + 16);
                
                g2.setColor(new java.awt.Color(100, 0, 0));
                g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }

        }
        
        
        
        hulls = ds.getConvexHullsB();
        for (int i = 0; i < hulls.length; i++) {
        	hullPoly.reset();
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.getX() - ds.getGameMap().getMapOrigin().getX(),
                        sy = start.getY() - ds.getGameMap().getMapOrigin().getY(),
                        ex = end.getX() - ds.getGameMap().getMapOrigin().getX(),
                        ey = end.getY() - ds.getGameMap().getMapOrigin().getY();
                hullPoly.addPoint(sx* 32 + 16, sy* 32 + 16);
                
            //    g2.setColor(new java.awt.Color(0, 0, 100));
              //  g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }
            g2.setColor(new java.awt.Color(0,150,255, 180));
            g2.fill(hullPoly);
            
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.getX() - ds.getGameMap().getMapOrigin().getX(),
                        sy = start.getY() - ds.getGameMap().getMapOrigin().getY(),
                        ex = end.getX() - ds.getGameMap().getMapOrigin().getX(),
                        ey = end.getY() - ds.getGameMap().getMapOrigin().getY();
                hullPoly.addPoint(sx* 32 + 16, sy* 32 + 16);
                
                g2.setColor(new java.awt.Color(0, 0, 100));
                g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }

        }






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
}
