package battlecode.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.media.opengl.GL;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;
import java.net.URL;

public class OBJFile extends DataFile {
	
	public static void convertToBCMs(){
		OBJFile temp = new OBJFile(" ");
		temp.convertToBCM("art/units/NewArchon/Low_poly_archon.obj","art/units/NewArchon/Low_poly_archon.bcm");
		temp.convertToBCM("art/units/NewChainer/guardbot.obj","art/units/NewChainer/guardbot.bcm");
		temp.convertToBCM("art/units/NewTurret/tank.obj","art/units/NewTurret/tank.bcm");
		temp.convertToBCM("art/units/NewWout/wout.obj","art/units/NewWout/wout.bcm");
		temp.convertToBCM("art/units/NewSoldier/soldier.obj","art/units/NewSoldier/soldier.bcm");

		temp.convertToBCM("art/units/building/building.obj","art/units/building/building.bcm");
		temp.convertToBCM("art/botBuildings/communications.obj","art/botBuildings/communications.bcm");
		temp.convertToBCM("art/botBuildings/processingplant.obj","art/botBuildings/processingplant.bcm");
		temp.convertToBCM("art/botBuildings/tower.obj","art/botBuildings/tower.bcm");
	
	
	}

    private int arraySize;		// number of faces * 3 (index array size)
    private int numElements;	// number of render elements
    private int indexLength;
    // buffer objects
    private FloatBuffer vBuf;
    private FloatBuffer nBuf;
    private FloatBuffer tBuf;
    private IntBuffer iBuf;

    private class Face {

        public int[] vertexIndex = new int[3];
        public int[] texCoordIndex = new int[3];
        public int[] normalIndex = new int[3];

        public Face() {
        }
    };

    // constructor that takes a file location
    public OBJFile(String pathname) {
    	
        super(pathname);
       
    }

    protected void load(URL file) {
    	
        float[] vertices;
        float[] normals;
        float[] texCoords;
        int[] indices;

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(file.openStream()));

            ArrayList<Vector3f> verticesVec = new ArrayList<Vector3f>();
            ArrayList<Vector3f> normalsVec = new ArrayList<Vector3f>();
            ArrayList<Vector2f> texCoordVec = new ArrayList<Vector2f>();
            ArrayList<Face> faceVec = new ArrayList<Face>();

            String s;
            int offset = 1;
            while ((s = br.readLine()) != null) {
                if (s.startsWith("vt")) {
                    // we have a texture coordinate
                    String[] parts = s.split(" +");
                    float u = Float.parseFloat(parts[2-offset]);
                    float v = Float.parseFloat(parts[3-offset]);

                    // compensate for left hand origin
                    v = 1.0f - v;

                    texCoordVec.add(new Vector2f(u, v));
                } else if (s.startsWith("vn")) {
                    // we have a normal
                    String[] parts = s.split(" +");
                    float x = Float.parseFloat(parts[2-offset]);
                    float y = Float.parseFloat(parts[3-offset]);
                    float z = Float.parseFloat(parts[4-offset]);

                    normalsVec.add(new Vector3f(x, y, z));
                } else if (s.startsWith("v")) {
                    // we have a vertex
                    String[] parts = s.split(" +");
                    float x = Float.parseFloat(parts[2-offset]);
                    float y = Float.parseFloat(parts[3-offset]);
                    float z = Float.parseFloat(parts[4-offset]);

                    verticesVec.add(new Vector3f(x, y, z));
                    
                } else if (s.startsWith("f")) {
                    // we have a face
                    String[] parts = s.split(" +");
                    Face f = new Face();
                    for (int i = 0; i < 3; i++) {
                        String[] sub = parts[1 + i].split("/");
                        // make sure elements are indexed from 0
                        f.vertexIndex[i] = Integer.parseInt(sub[0]) - 1;
                        f.texCoordIndex[i] = Integer.parseInt(sub[1]) - 1;
                        f.normalIndex[i] = Integer.parseInt(sub[2]) - 1;
                    }
                    faceVec.add(f);
                }
            }
            
            // now we close stream
            br.close();

            // number of vertices in total
            arraySize = faceVec.size() * 3;

            // number of elements is the size of the largest set
            numElements = Math.max(Math.max(verticesVec.size(), normalsVec.size()), texCoordVec.size());

            int index = -1;
            if (normalsVec.size() >= verticesVec.size() && normalsVec.size() >= texCoordVec.size()) {
                index = 0; // by normals
            } else if (verticesVec.size() >= normalsVec.size() && verticesVec.size() >= texCoordVec.size()) {
                index = 1; // by vertices
            } else {
                index = 2; // by texture coordinates
            }

            // storage
            indices = new int[arraySize];

            vertices = new float[numElements * 3];
            normals = new float[numElements * 3];
            texCoords = new float[numElements * 2];

            // unroll faces
            int[] newIDs = {0, 0, 0};
            int j = 0;
            for (Face f : faceVec) {
                switch (index) {
                    case 0: // by normals
                        newIDs[0] = f.normalIndex[0];
                        newIDs[1] = f.normalIndex[1];
                        newIDs[2] = f.normalIndex[2];
                        break;
                    case 1: // by vertices
                        newIDs[0] = f.vertexIndex[0];
                        newIDs[1] = f.vertexIndex[1];
                        newIDs[2] = f.vertexIndex[2];
                        break;
                    case 2: // by tex coords
                        newIDs[0] = f.texCoordIndex[0];
                        newIDs[1] = f.texCoordIndex[1];
                        newIDs[2] = f.texCoordIndex[2];
                        break;
                    default:
                        throw new Exception("LOADING MODEL FORMAT ERROR: " + file.getFile());
                }

                // now that we have the new index, fill in the arrays
                for (int i = 0; i < 3; i++) {
                    vertices[newIDs[i] * 3] = verticesVec.get(f.vertexIndex[i]).x;
                    vertices[newIDs[i] * 3 + 1] = verticesVec.get(f.vertexIndex[i]).y;
                    vertices[newIDs[i] * 3 + 2] = verticesVec.get(f.vertexIndex[i]).z;

                    normals[newIDs[i] * 3] = normalsVec.get(f.normalIndex[i]).x;
                    normals[newIDs[i] * 3 + 1] = normalsVec.get(f.normalIndex[i]).y;
                    normals[newIDs[i] * 3 + 2] = normalsVec.get(f.normalIndex[i]).z;

                    texCoords[newIDs[i] * 2] = texCoordVec.get(f.texCoordIndex[i]).x;
                    texCoords[newIDs[i] * 2 + 1] = texCoordVec.get(f.texCoordIndex[i]).y;

                    indices[j++] = newIDs[i];
                }

                // make buffers
                vBuf = BufferUtil.newFloatBuffer(vertices.length);
                nBuf = BufferUtil.newFloatBuffer(normals.length);
                tBuf = BufferUtil.newFloatBuffer(texCoords.length);
                iBuf = BufferUtil.newIntBuffer(indices.length);
                indexLength = indices.length;

                // copy into buffer
                for (int i = 0; i < vertices.length; i++)
                    vBuf.put(vertices[i]);
                vBuf.rewind();
                for (int i = 0; i < normals.length; i++)
                    nBuf.put(normals[i]);
                nBuf.rewind();
                for (int i = 0; i < texCoords.length; i++)
                    tBuf.put(texCoords[i]);
                tBuf.rewind();
                for (int i = 0; i < indices.length; i++)
                    iBuf.put(indices[i]);
                iBuf.rewind();

            }
        } catch (FileNotFoundException ex) {
            arraySize = numElements = 0;
        } catch (IOException ex) {
            arraySize = numElements = 0;
        } catch (Exception e) {
            arraySize = numElements = 0;
            e.printStackTrace();
        }

    }

	public void convertToBCM(String objfile, String bcmfile) {

		System.out.println("Load Started "+objfile);
        float[] vertices;
        float[] normals;
        float[] texCoords;
        int[] indices;
		int arraySize;
		int numElements;
		int indexLength;

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(objfile)));

            ArrayList<Vector3f> verticesVec = new ArrayList<Vector3f>();
            ArrayList<Vector3f> normalsVec = new ArrayList<Vector3f>();
            ArrayList<Vector2f> texCoordVec = new ArrayList<Vector2f>();
            ArrayList<Face> faceVec = new ArrayList<Face>();

            String s;
            int offset = 1;
            int faceInt = 0;
            while ((s = br.readLine()) != null) {
            	
                if (s.startsWith("vt")) {
                    // we have a texture coordinate
                    String[] parts = s.split(" +");
                    float u = Float.parseFloat(parts[2-offset]);
                    float v = Float.parseFloat(parts[3-offset]);

                    // compensate for left hand origin
                    v = 1.0f - v;

                    texCoordVec.add(new Vector2f(u, v));
                } else if (s.startsWith("vn")) {
                    // we have a normal
                    String[] parts = s.split(" +");
                    float x = Float.parseFloat(parts[2-offset]);
                    float y = Float.parseFloat(parts[3-offset]);
                    float z = Float.parseFloat(parts[4-offset]);

                    normalsVec.add(new Vector3f(x, y, z));
                } else if (s.startsWith("v")) {
                    // we have a vertex
                    String[] parts = s.split(" +");
                    float x = Float.parseFloat(parts[2-offset]);
                    float y = Float.parseFloat(parts[3-offset]);
                    float z = Float.parseFloat(parts[4-offset]);

                    verticesVec.add(new Vector3f(x, y, z));
                } else if (s.startsWith("f")) {

                    // we have a face
                    String[] parts = s.split(" +");
                  
                    int partsLen = parts.length;
                    int numTriangles = partsLen-3;
                    
                    for(int t = 0; t < numTriangles; t++){
                    	Face f = new Face();
                    	String[] sub = parts[1].split("/");
                    	f.vertexIndex[0] = Integer.parseInt(sub[0]) - 1;
                		f.texCoordIndex[0] = Integer.parseInt(sub[1]) - 1;
                		f.normalIndex[0] = Integer.parseInt(sub[2]) - 1;
                    	for (int i = t+1; i < t+3; i++) {
                    		String[] suba = parts[1 + i].split("/");

                    		// make sure elements are indexed from 0
                    		f.vertexIndex[i-t] = Integer.parseInt(suba[0]) - 1;
                    		f.texCoordIndex[i-t] = Integer.parseInt(suba[1]) - 1;
                    		f.normalIndex[i-t] = Integer.parseInt(suba[2]) - 1;

                    	}
                    	faceVec.add(f);
                    }
                    
                    
                }
            }

            // now we close stream
            br.close();

            // number of vertices in total
            arraySize = faceVec.size() * 3;

            // number of elements is the size of the largest set
            numElements = Math.max(Math.max(verticesVec.size(), normalsVec.size()), texCoordVec.size());

            int index = -1;
            if (normalsVec.size() >= verticesVec.size() && normalsVec.size() >= texCoordVec.size()) {
                index = 0; // by normals
            } else if (verticesVec.size() >= normalsVec.size() && verticesVec.size() >= texCoordVec.size()) {
                index = 1; // by vertices
            } else {
                index = 2; // by texture coordinates
            }

            // storage
            indices = new int[arraySize];

            vertices = new float[numElements * 3];
            normals = new float[numElements * 3];
            texCoords = new float[numElements * 2];
faceInt = 0;
            
            // unroll faces
            int[] newIDs = {0, 0, 0};
            int j = 0;
            for (Face f : faceVec) {
                switch (index) {
                    case 0: // by normals
                        newIDs[0] = f.normalIndex[0];
                        newIDs[1] = f.normalIndex[1];
                        newIDs[2] = f.normalIndex[2];
                        break;
                    case 1: // by vertices
                        newIDs[0] = f.vertexIndex[0];
                        newIDs[1] = f.vertexIndex[1];
                        newIDs[2] = f.vertexIndex[2];
                        break;
                    case 2: // by tex coords
                        newIDs[0] = f.texCoordIndex[0];
                        newIDs[1] = f.texCoordIndex[1];
                        newIDs[2] = f.texCoordIndex[2];
                        break;
                    default:
                        throw new Exception("LOADING MODEL FORMAT ERROR: " + objfile);
                }

                // now that we have the new index, fill in the arrays
                for (int i = 0; i < 3; i++) {
                    vertices[newIDs[i] * 3] = verticesVec.get(f.vertexIndex[i]).x;
                    vertices[newIDs[i] * 3 + 1] = verticesVec.get(f.vertexIndex[i]).y;
                    vertices[newIDs[i] * 3 + 2] = verticesVec.get(f.vertexIndex[i]).z;

                    normals[newIDs[i] * 3] = normalsVec.get(f.normalIndex[i]).x;
                    normals[newIDs[i] * 3 + 1] = normalsVec.get(f.normalIndex[i]).y;
                    normals[newIDs[i] * 3 + 2] = normalsVec.get(f.normalIndex[i]).z;

                    texCoords[newIDs[i] * 2] = texCoordVec.get(f.texCoordIndex[i]).x;
                    texCoords[newIDs[i] * 2 + 1] = texCoordVec.get(f.texCoordIndex[i]).y;

                    indices[j++] = newIDs[i];
                }

				OBJStruct b = new OBJStruct();
				b.v = vertices;
				b.n = normals;
				b.t = texCoords;
				b.i = indices;

				ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(bcmfile));
				os.writeObject(b);
				os.close();

            }
        } catch (FileNotFoundException ex) {
            arraySize = numElements = 0;
        } catch (IOException ex) {
            arraySize = numElements = 0;
        } catch (Exception e) {
            arraySize = numElements = 0;
            e.printStackTrace();
        }

	}

    // loads the obj model from a file
    protected void load(File file) {
		//System.out.println("Load Started "+file);
		OBJStruct b = null;
		try {
			ObjectInputStream os = new ObjectInputStream(new FileInputStream(file.toString()));
			b = (OBJStruct) os.readObject();
		} catch(Exception e) {
			e.printStackTrace();
		}

		// make buffers
		vBuf = BufferUtil.newFloatBuffer(b.v.length);
		nBuf = BufferUtil.newFloatBuffer(b.n.length);
		tBuf = BufferUtil.newFloatBuffer(b.t.length);
		iBuf = BufferUtil.newIntBuffer(b.i.length);
		indexLength = b.i.length;
		vBuf.put(b.v);
		vBuf.rewind();
		nBuf.put(b.n);
		nBuf.rewind();
		tBuf.put(b.t);
		tBuf.rewind();
		iBuf.put(b.i);
		iBuf.rewind();
		
       // System.out.println("Done loading");

    }

    protected void reload(File file) {
        load(file);

    }

    protected void reload(URL url) {
        load(url);
    }

    // draws the obj model the the GL device
    public void draw(GL gl) {
    	 /*gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        for(int i = 0; i < indices.length; i++) {
        gl.glTexCoord2f(texCoords[2 * indices[i]], texCoords[2 * indices[i] + 1]);
        gl.glNormal3f(normals[3 * indices[i]], normals[3 * indices[i] + 1], normals[3 * indices[i] + 2]);
        gl.glVertex3f(vertices[3 * indices[i]], vertices[3 * indices[i] + 1], vertices[3 * indices[i] + 2]);
        }
        gl.glEnd();*/
    	//gl.glDisable(GL.GL_CULL_FACE);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
        gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL.GL_FLOAT, 0, vBuf);
        gl.glNormalPointer(GL.GL_FLOAT, 0, nBuf);
        gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, tBuf);

        gl.glDrawElements(GL.GL_TRIANGLES, indexLength, GL.GL_UNSIGNED_INT, iBuf);
       
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        //gl.glEnable(GL.GL_CULL_FACE);
    }
}
