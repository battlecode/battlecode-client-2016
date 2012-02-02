package battlecode.client.viewer.renderer3d;

import java.lang.Math;



import java.util.ArrayList;

import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;


import com.sun.opengl.util.texture.Texture;

public class GLDrawMap {

    final static float MAP_SCALE = .20f;
    private int mapWidth;
    private int mapHeight;
    private float scaleSize;
    // points that define the terrain
    private Vector3f[][] points;
    private Vector3f[][] norms;
    private Color4f[][] cols;
    // normal array
    private float[] normals;
    // vertex array
    private float[] vertices;
    // index array
    private int[] indices;
    // color array
    private float[] colors;
    private float[] colorNorm;
    // tex coord array
    private float[] texCoords;
    // do we have a texture
    private Texture tex = null;
    private String texturePath = null;
    private Texture crateTexture = null;
    private String crateTexturePath = null;
    // wall tile locations
    private Vector3f[] walls;
    // internal map for terrain heights
    private battlecode.world.GameMap map;
    // initial state of map blocks
    //private int[][] blockMat;
    private final int DENSITY = 3; //Density of triangle,
    //this should be 2 unless otherwise specified

    public GLDrawMap(battlecode.world.GameMap map) {
        mapWidth = map.getWidth() * DENSITY;
        mapHeight = map.getHeight() * DENSITY;

        this.map = map;
        // make map into point set
        points = prerenderMap(map.getTerrainMatrix());

        // compute normals
        norms = new Vector3f[mapHeight][mapWidth];
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Vector3f v3 = new Vector3f();
        Vector3f v4 = new Vector3f();

        Vector3f temp = new Vector3f();

        Vector3f total;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                int numNorms = 0;
                total = new Vector3f(0, 0, 0);
                norms[j][i] = new Vector3f();
                if (j > 0) {
                    v3.sub(points[j - 1][i], points[j][i]);
                    v3.normalize();

                } else if (j < mapHeight - 1) {
                    v1.sub(points[j + 1][i], points[j][i]);
                    v1.normalize();
                }
                if (i > 0) {
                    v4.sub(points[j][i - 1], points[j][i]);
                    v4.normalize();
                } else if (i < mapWidth - 1) {
                    v2.sub(points[j][i + 1], points[j][i]);
                    v2.normalize();
                }


                if (j > 0) {
                    if (i < mapWidth - 1) {
                        temp.cross(v2, v3);
                        temp.normalize();
                        total.add(temp);
                        numNorms++;
                    }
                    if (i > 0) {
                        temp.cross(v3, v4);
                        temp.normalize();
                        total.add(temp);
                        numNorms++;
                    }

                }
                if (j < mapHeight - 1) {
                    if (i < mapWidth - 1) {
                        temp.cross(v1, v2);
                        temp.normalize();
                        total.add(temp);
                        numNorms++;
                    }
                    if (i > 0) {
                        temp.cross(v4, v1);
                        temp.normalize();
                        total.add(temp);
                        numNorms++;
                    }

                }






                total.scale(1.0f / (float) numNorms);
                norms[j][i].set(total);
                norms[j][i].normalize();
            }
        }

        // compute colors
        //final Color3f groundColor = new Color3f(0.4f, 0.4f, 0.4f);
        //final Color3f obstacleColor = new Color3f(1.0f, 0.5f, 0.0f);
        final Color4f groundColor = new Color4f(243.0f / 255.0f, 221.0f / 255.0f, 92.0f / 255.0f, 1.0f);
        //final Color3f obstacleColor = new Color3f(47.0f / 255.0f, 23.0f / 255.0f, 0.0f);
        final Color4f obstacleColor = new Color4f(0.0f, 0.0f, 0.0f, 0.0f);

        cols = new Color4f[mapHeight][mapWidth];
        TerrainTile[][] tiles = map.getTerrainMatrix();
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                cols[j][i] = groundColor;
            }
        }
        for (int j = 1; j < mapHeight; j++) {
            for (int i = 1; i < mapWidth; i++) {


                int numLand = 0;
                if (tiles[i / DENSITY][j / DENSITY] == TerrainTile.LAND) {
                    numLand++;
                }
                if (tiles[i / DENSITY][j / DENSITY] == TerrainTile.LAND) {
                    numLand++;
                }
                if (tiles[i / DENSITY][j / DENSITY] == TerrainTile.LAND) {
                    numLand++;
                }
                if (tiles[i / DENSITY][j / DENSITY] == TerrainTile.LAND) {
                    numLand++;
                }

                float landFrac = (float) numLand / 4.0f;
                Color4f color = new Color4f();
                color.interpolate(obstacleColor, groundColor, landFrac);



                cols[j][i] = color;



                //cols[j][i] = new Color3f(0.0f, 0.4f * landFrac, 0.4f * waterFrac);
            }
        }

        // make a list of all wall positions
		/*
        ArrayList<Vector3f> wallPositions = new ArrayList<Vector3f>();

        for(int j = 0; j < mapHeight; j++) {
        for(int i = 0; i < mapWidth; i++) {


        if(tiles[i/DENSITY][j/DENSITY] == TerrainType.VOID) {
        Vector3f wall = new Vector3f();
        wall.x = i + 0.5f;
        wall.z = j + 0.5f;
        //wall.y = getMapHeight(wall.x, wall.z, null);
        wall.y = getMapHeight(wall.x, wall.z);
        wallPositions.add(wall);
        }
        }
        }
         *//*

        // make walls
        walls = new Vector3f [wallPositions.size()];
        //wallPositions.toArray(walls);
         */
        // make vertex array
        vertices = new float[(mapWidth) * (mapHeight) * 3];
        int k = 0;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {

                vertices[k] = points[j][i].x;
                vertices[k + 1] = points[j][i].y;
                vertices[k + 2] = points[j][i].z;
                k += 3;
            }
        }

        // make normal array
        normals = new float[(mapWidth) * (mapHeight) * 3];
        k = 0;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                normals[k] = norms[j][i].x;
                normals[k + 1] = norms[j][i].y;
                normals[k + 2] = norms[j][i].z;
                k += 3;
            }
        }

        // make color array
        colors = new float[(mapWidth) * (mapHeight) * 4];
        colorNorm = new float[(mapWidth) * (mapHeight) * 4];
        k = 0;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                colors[k] = cols[j][i].x;
                colors[k + 1] = cols[j][i].y;
                colors[k + 2] = cols[j][i].z;
                colors[k + 3] = cols[j][i].w;

                float norm = (float) Math.sqrt(cols[j][i].x * cols[j][i].x
                        + cols[j][i].y * cols[j][i].y
                        + cols[j][i].z * cols[j][i].z);
                float val = (norm < 0.3f) ? 0.0f : 0.9f;
                colorNorm[k] = val;
                colorNorm[k + 1] = val;
                colorNorm[k + 2] = val;
                colorNorm[k + 3] = 1.0f;
                k += 4;
            }
        }

        Vector2f[][] txc = new Vector2f[mapHeight][mapWidth];
        int maxWidth = Math.max(mapWidth, mapHeight);
        for (int j = 0; j < mapHeight; j++) {
            /*if(j % 2 == 1) {
            for(int i = 0; i < mapWidth + 1; i++) {
            l.add((j - 1) * (mapWidth + 1) + i);
            l.add(j * (mapWidth + 1) + i);
            }
            } else {
            for(int i = mapWidth; i >= 0; i--) {
            l.add(j * (mapWidth + 1) + i);
            l.add((j - 1) * (mapWidth + 1) + i);
            }
            }*/

            //if(j % 2 == 1) {
            for (int i = 0; i < mapWidth; i++) {
                txc[j][i] = new Vector2f((float) i / (float) maxWidth, (float) j / (float) maxWidth);
            }
            /*} else {
            for(int i = mapWidth; i >= 0; i--) {
            txc[j][i] = new Vector2f((float)i / (float)maxWidth, 1.0f - (float)j / (float)maxWidth);
            }
            }*/
        }



        // make texcoord array
//		int rowWidth = mapWidth ;
        texCoords = new float[(mapWidth) * (mapHeight) * 2];
        k = 0;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                texCoords[k] = txc[j][i].x;
                texCoords[k + 1] = txc[j][i].y;
                k += 2;
            }
        }/*
        for(int j = 0; j < mapHeight + 1; j++) {
        for(int i = 0; i < mapWidth + 1; i++) {
        texCoords[2 * i + j * rowWidth * 2] = i / (float)(maxWidth);
        texCoords[2 * i + 1 + j * rowWidth * 2] = 1.0f - j / (float)(maxWidth);
        }
        }*/

        // compute triangle indices
        LinkedList<Integer> l = new LinkedList<Integer>();

        // generate triangle list indices
		/*for(int j = 0; j < mapHeight; j++) {
        for(int i = 0; i < mapWidth; i++) {
        int ul = j * (mapWidth + 1) + i;
        l.add(ul);
        l.add(ul + (mapWidth + 1));
        l.add(ul + 1);

        l.add(ul + 1);
        l.add(ul + (mapWidth + 1));
        l.add(ul + (mapWidth + 1) + 1);
        }
        }*/

        // generate triangle strip indices
        for (int j = 1; j < mapHeight; j++) {
            if (j % 2 == 1) {
                for (int i = 0; i < mapWidth; i++) {
                    l.add((j - 1) * (mapWidth) + i);
                    l.add(j * (mapWidth) + i);
                }
            } else {
                for (int i = mapWidth - 1; i >= 0; i--) {
                    l.add(j * (mapWidth) + i);
                    l.add((j - 1) * (mapWidth) + i);


                }
            }
        }

        // generate triangle list indices
		/*for(int j = 0; j < mapHeight; j++) {
        for(int i = 0; i < mapWidth; i++) {
        int index = i + j * (mapWidth + 1);
        l.add(index);
        l.add(index + mapWidth + 1 + 1);
        l.add(index + 1);

        l.add(index);
        l.add(index + mapWidth + 1);
        l.add(index + mapWidth + 1 + 1);

        }
        }*/
        indices = new int[l.size()];
        for (int i = 0; i < l.size(); i++) {
            indices[i] = l.get(i).intValue();
        }
    }

    private Vector3f[][] prerenderMap(TerrainTile[][] map) {
        Vector3f[][] pts = new Vector3f[mapHeight][mapWidth];


        // do center
        float pointHeight;
        for (int i = 0; i < mapHeight; i++) {
            for (int j = 0; j < mapWidth; j++) {
                pointHeight = getMapHeight(j, i) * MAP_SCALE;
                if (map[j / DENSITY][i / DENSITY] == TerrainTile.VOID) {
                }
                pts[i][j] = new Vector3f(((float) j + .5f) / DENSITY, pointHeight,
                        ((float) i + .5f) / DENSITY);

            }
        }

        return pts;
    }

    public int getMapWidth() {
        return mapWidth / DENSITY;
    }

    public int getMapHeight() {
        return mapHeight / DENSITY;
    }

    private float getMapHeight(int x, int y) {
        return getMapHeight((float) x, (float) y);
    }

    private float getMapHeight(float x, float y) {
        // first find what rect we are in
        int ix = (int) x / DENSITY;
        int iy = (int) y / DENSITY;

        // clip the coordinates
        if (ix < 0 || ix >= mapWidth / DENSITY) {
            return 0.0f;
        } else if (iy < 0 || iy >= mapHeight / DENSITY) {
            return 0.0f;
        }

        // we are in the center
		/*Vector3f v1 = new Vector3f(), v2 = new Vector3f();
        v1.sub(points[iy][ix + 1], points[iy][ix]);
        v2.sub(points[iy + 1][ix], points[iy][ix]);
        float dx = x - ix;
        float dy = y - iy;
        v1.scale(dx);
        v2.scale(dy);
        v1.add(v1, v2);
        v1.add(points[iy][ix], v1);
        return v1.y;*/

        // return height of cell + height number of blocks / 3


        //return (float)map.getTerrainMatrix()[ix][iy].getHeight();
        return 0;
    }

    //These x y values correspond to the x y values of the corners of tiles
    // get the interpolated height on the map
    public float getTerrainHeight(float x, float y) {
        // first find what corner it is
        int ix = Math.round(x * DENSITY);
        int iy = Math.round(y * DENSITY);

        // clip the coordinates
        if (ix < 0 || ix >= mapWidth) {
            return 0.0f;
        } else if (iy < 0 || iy >= mapHeight) {
            return 0.0f;
        }

        // we are in the center
		/*Vector3f v1 = new Vector3f(), v2 = new Vector3f();
        v1.sub(points[iy][ix + 1], points[iy][ix]);
        v2.sub(points[iy + 1][ix], points[iy][ix]);
        float dx = x - ix;
        float dy = y - iy;
        v1.scale(dx);
        v2.scale(dy);
        v1.add(v1, v2);
        v1.add(points[iy][ix], v1);
        return v1.y;*/

        float terrainHeight = vertices[(iy * (mapWidth) + ix) * 3 + 1];//1 for y

        return terrainHeight;
    }

    public Vector3f getTileNormal(float x, float y) {
        final Vector3f norm = new Vector3f(0.0f, 1.0f, 0.0f);
        return norm;

        // upper left hand corner
		/*int ix = (int)x;
        int iy = (int)y;

        final Vector3f norm = new Vector3f(0.0f, 1.0f, 0.0f);

        // clip the coordinates
        if(ix < 0 || ix >= mapWidth)
        return norm;
        else if(iy < 0 || iy >= mapHeight)
        return norm;*/

        // we are in the center
		/*Vector3f v1 = new Vector3f(), v2 = new Vector3f();
        v1.sub(points[iy][ix + 1], points[iy][ix]);
        v2.sub(points[iy + 1][ix], points[iy][ix]);
        float dx = x - ix;
        float dy = y - iy;

        Vector3f nHat = new Vector3f();
        Vector3f temp = new Vector3f();

        float dul = (float)Math.sqrt(dx * dx + dy * dy);
        float dlr = (float)Math.sqrt((1 - dx) * (1 - dx) + (1 - dy) * (1 - dy));
        float dur = (float)Math.sqrt((1 - dx) * (1 - dx) + dy * dy);
        float dll = (float)Math.sqrt(dx * dx + (1 - dy) * (1 - dy));

        nHat.cross(v2, v1);
        temp.set(norms[iy][ix]);
        temp.scale(dul);
        nHat.add(temp);
        temp.set(norms[iy + 1][ix]);
        temp.scale(dll);
        nHat.add(temp);
        temp.set(norms[iy + 1][ix + 1]);
        temp.scale(dlr);
        nHat.add(temp);
        temp.set(norms[iy][ix + 1]);
        temp.scale(dur);
        nHat.add(temp);*/
        /*nHat.add(norms[iy][ix], norms[iy + 1][ix]);
        nHat.add(norms[iy + 1][ix + 1]);
        nHat.add(norms[iy][ix + 1]);*/
        //nHat.normalize();
		/*Vector3f ul = points[iy][ix];
        Vector3f ur = points[iy][ix + 1];
        Vector3f bl = points[iy + 1][ix];

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        u.sub(ur, ul);
        v.sub(bl, ul);

        Vector3f nHat = new Vector3f();
        nHat.cross(v, u);
        nHat.normalize();
         */
        //return norm;
    }

    public void setTexture(String tex) {
        this.texturePath = tex;
    }

    public void setCrateTexture(String tex) {
        this.crateTexturePath = tex;
    }

    public void draw(GL2 gl, GLU glu, GLDrawState ds) {
        final Color4f highColor = new Color4f(0.0f, 1.0f, 0.0f, 1.0f);
        final Color4f lowColor = new Color4f(1.0f, 1.0f, 1.0f, 1.0f);


        Color4f[][] trueCols = new Color4f[mapHeight][mapWidth];
//		InternalTerrainTile tiles[][] = ds.getGameMap().getTerrainMatrix();
        //Update colors

        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {

                //float fluxAmount = getMapHeight(i, j)/64.f;
                //	System.out.println(flux[j/DENSITY][i/DENSITY] + " " + fluxAmount);

                //float fluxAmount = (float)tiles[i/DENSITY][j/DENSITY].getFlux();

                Color4f tintColor = new Color4f();
                Color4f trueColor = new Color4f();

                tintColor.interpolate(lowColor, highColor, 0);
                //System.out.println(actualHeight/maxHeight);


                trueColor.interpolate(cols[j][i], tintColor, 1.0f);
                trueColor.w = cols[j][i].w;
                trueCols[j][i] = trueColor;


                //If void, see through to the background.
				/*if(tiles[i/DENSITY][j/DENSITY] == TerrainType.VOID){
                trueCols[j][i].w = 0.0f;
                }*/

            }
        }
        //System.out.println(map.getTerrainMatrix()[0][0].getFlux() + " " + MAX_FLUX_PER_TILE + " " + trueCols[0][0].x);

        int k = 0;
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                colors[k] = trueCols[j][i].x;
                colors[k + 1] = trueCols[j][i].y;
                colors[k + 2] = trueCols[j][i].z;
                colors[k + 3] = trueCols[j][i].w;

                float norm = (float) Math.sqrt(trueCols[j][i].x * trueCols[j][i].x
                        + trueCols[j][i].y * trueCols[j][i].y
                        + trueCols[j][i].z * trueCols[j][i].z);
                float val = (norm < 0.3f) ? 0.0f : 0.9f;
                colorNorm[k] = val;
                colorNorm[k + 1] = val;
                colorNorm[k + 2] = val;
                colorNorm[k + 3] = 1.0f;
                k += 4;
            }
        }


        if (tex == null && texturePath != null) {
            tex = GLGameRenderer.textureCache.getResource(texturePath, texturePath).tex;
            texturePath = null;
        }
        gl.glPushMatrix();
        //	gl.glTranslatef(1.5f, 0.0f, 1.5f);
        if (tex != null) {
            System.out.println("Using texture");
            gl.glEnable(GL2.GL_TEXTURE_2D);
            tex.bind();
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            for (int i = 0; i < indices.length; i++) {
                gl.glTexCoord2f(texCoords[indices[i] * 2], texCoords[indices[i] * 2 + 1]);
                gl.glColor4f(colorNorm[indices[i] * 4], colorNorm[indices[i] * 4 + 1], colorNorm[indices[i] * 4 + 2], colorNorm[indices[i] * 4 + 3]);
                gl.glNormal3f(normals[indices[i] * 3], normals[indices[i] * 3 + 1], normals[indices[i] * 3 + 2]);
                gl.glVertex3f(vertices[indices[i] * 3], vertices[indices[i] * 3 + 1], vertices[indices[i] * 3 + 2]);

            }
            gl.glEnd();
        } else {
            gl.glEnable(gl.GL_BLEND);
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            for (int i = 0; i < indices.length; i++) {
                gl.glColor4f(colors[indices[i] * 4], colors[indices[i] * 4 + 1], colors[indices[i] * 4 + 2], colors[indices[i] * 4 + 3]);
                gl.glNormal3f(normals[indices[i] * 3], normals[indices[i] * 3 + 1], normals[indices[i] * 3 + 2]);
                gl.glVertex3f(vertices[indices[i] * 3], vertices[indices[i] * 3 + 1], vertices[indices[i] * 3 + 2]);
            }
            gl.glEnd();
            gl.glDisable(gl.GL_BLEND);
        }
        gl.glPopMatrix();



        boolean showGrid = RenderConfiguration.showGridlines();
        if (showGrid) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glColor3f(0.1f, 0.1f, 0.1f);
            gl.glNormal3f(0.0f, 1.0f, 0.0f);


            // do the vertical gridlines
            for (int j = 0; j < mapHeight; j++) {
                if ((j + 1) % DENSITY != 0) {
                    continue;
                }
                if ((j + 1) % (DENSITY * 2) == 0) {
                    for (int i = 0; i < mapWidth; i++) {
                        Vector3f p;
                        Vector3f pOne;
                        Vector3f pTwo;


                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j < mapHeight - 1 && i > 0) {

                            pTwo = points[j + 1][i - 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }


                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);



                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j < mapHeight - 1) {
                            pTwo = points[j + 1][i];
                            p.add(pTwo);
                            p.scale(.5f);
                        }

                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);

                    }
                } else {
                    for (int i = mapWidth - 1; i >= 0; i--) {
                        Vector3f p;
                        Vector3f pOne;
                        Vector3f pTwo;

                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j < mapHeight - 1) {
                            pTwo = points[j + 1][i];
                            p.add(pTwo);
                            p.scale(.5f);
                        }

                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);
                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j < mapHeight - 1 && i > 0) {

                            pTwo = points[j + 1][i - 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }


                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);
                    }
                }
            }

            gl.glEnd();

            // do the horizontal gridlines
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < mapWidth; i++) {
                if ((i + 1) % DENSITY != 0) {
                    continue;
                }
                if ((i + 1) % (DENSITY * 2) == 0) {
                    for (int j = 0; j < mapHeight; j++) {
                        Vector3f p;
                        Vector3f pOne;
                        Vector3f pTwo;



                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j > 0 && i < mapWidth - 1) {

                            pTwo = points[j - 1][i + 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }


                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);

                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (i < mapWidth - 1) {
                            pTwo = points[j][i + 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }

                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);




                    }
                } else {
                    for (int j = mapHeight - 1; j >= 0; j--) {
                        Vector3f p;
                        Vector3f pOne;
                        Vector3f pTwo;


                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (i < mapWidth - 1) {
                            pTwo = points[j][i + 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }

                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);


                        pOne = points[j][i];
                        p = new Vector3f(0.0f, 0.0f, 0.0f);
                        p.add(pOne);
                        if (j > 0 && i < mapWidth - 1) {

                            pTwo = points[j - 1][i + 1];
                            p.add(pTwo);
                            p.scale(.5f);
                        }


                        gl.glVertex3f(p.x, p.y + 0.01f, p.z);

                    }
                }
            }

            gl.glEnd();
        }

        gl.glEnable(GL2.GL_BLEND);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        MapLocation[][] hulls = ds.getConvexHullsA();
        for (int i = 0; i < hulls.length; i++) {

            gl.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
            gl.glBegin(GL2.GL_POLYGON);
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.x - ds.getGameMap().getMapOrigin().x,
                        sy = start.y - ds.getGameMap().getMapOrigin().y,
                        ex = end.x - ds.getGameMap().getMapOrigin().x,
                        ey = end.y - ds.getGameMap().getMapOrigin().y;

                gl.glVertex3f(sx + .5f, getMapHeight(sx, sy) * MAP_SCALE + 1.0f, sy + .5f);
                //  g2.setColor(new java.awt.Color(255, 150, 0));
                //g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }
            gl.glEnd();

        }
        hulls = ds.getConvexHullsB();
        for (int i = 0; i < hulls.length; i++) {

            gl.glColor4f(0.0f, 0.0f, 1.0f, 0.5f);
            gl.glBegin(GL2.GL_POLYGON);
            for (int j = 0; j < hulls[i].length; j++) {
                MapLocation start = hulls[i][j];
                MapLocation end = hulls[i][(j + 1) % hulls[i].length];
                int sx = start.x - ds.getGameMap().getMapOrigin().x,
                        sy = start.y - ds.getGameMap().getMapOrigin().y,
                        ex = end.x - ds.getGameMap().getMapOrigin().x,
                        ey = end.y - ds.getGameMap().getMapOrigin().y;

                gl.glVertex3f(sx + .5f, getMapHeight(sx, sy) * MAP_SCALE + 1.0f, sy + .5f);
                //  g2.setColor(new java.awt.Color(255, 150, 0));
                //g2.drawLine(sx * 32 + 16, sy * 32 + 16, ex * 32 + 16, ey * 32 + 16);
                //System.out.println("Drawing hull line " + sx + " " + sy + " "
                //        + ex + " " + ey);
            }
            gl.glEnd();

        }
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        /*
        // draw the boundaries
        final String path = "./art/walls/wall.png";
        if(crateTexture == null && crateTexturePath != null) {
        crateTexture = GLGameRenderer.textureCache.getResource(crateTexturePath, crateTexturePath).tex;
        crateTexturePath = null;
        } else if(crateTexture == null) {
        crateTexture = GLGameRenderer.textureCache.getResource(path, path).tex;
        }*/

    }

    private void drawBox(GL2 gl) {
        // points are
        final float[] points = new float[]{
            // top
            -0.5f, 0.5f, -0.5f, // 0
            0.5f, 0.5f, -0.5f, // 1
            0.5f, 0.5f, 0.5f, // 2
            -0.5f, 0.5f, 0.5f, // 3

            // bottom
            -0.5f, -0.5f, -0.5f,// 4
            0.5f, -0.5f, -0.5f, // 5
            0.5f, -0.5f, 0.5f, // 6
            -0.5f, -0.5f, 0.5f // 7
        };

        // indices for box
        final float[] texCoords = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        final float[] normals = new float[]{
            0.0f, 1.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f
        };

        final int indices[] = new int[]{
            0, 3, 2, 0, 2, 1, 0, 4, 7, 0, 7, 3,
            3, 7, 6, 3, 6, 2, 2, 6, 5, 2, 5, 1,
            1, 5, 4, 1, 4, 0
        };

        final int texIndices[] = new int[]{
            0, 3, 2, 0, 2, 1, 0, 3, 2, 0, 2, 1,
            0, 3, 2, 0, 2, 1, 0, 3, 2, 0, 2, 1,
            0, 3, 2, 0, 2, 1
        };

        gl.glBegin(GL2.GL_TRIANGLES);
        final int VEC_SIZE = 3;
        final int TEX_SIZE = 2;
        for (int i = 0; i < indices.length; i++) {
            gl.glNormal3fv(normals, (i / 6) * VEC_SIZE);
            gl.glTexCoord2fv(texCoords, texIndices[i] * TEX_SIZE);
            gl.glVertex3fv(points, indices[i] * VEC_SIZE);
        }
        gl.glEnd();


    }
}
