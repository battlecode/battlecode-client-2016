package battlecode.client.viewer.renderer3d;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputListener;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
/**
 * Class describing the camera position in the scene. It allows for operation
 * such as rotation of the camera position.
 */
public class Camera implements MouseMotionListener, MouseInputListener, MouseWheelListener, KeyListener {

	/** Camera position. */
	private Vector3f position;

	/** Camera target. */
	private Vector3f target;

	// default position
	private Vector3f defaultPosition;
	private Vector3f defaultTarget;
	
	/** Camera normal. */
	private Vector3f normal;

	/** Last mouse position. */
	private Point lastPos;
	
	/** Click position. */
	private Point clickPosition = null;

	/** Camera window width and height. */
	private int windowWidth, windowHeight;

	/** Projection parameters. */
	private double fov;

	private double zNear, zFar;

	/** Internal GLU object for transformation. */
	private GLU glu;

	/** Camera projection and transformation matrices and viewport. */
	double proj[] = new double[16];

	double modelView[] = new double[16];

	int viewport[] = new int[4];
	
	// use orthogonal projection
	private boolean isOrthogonal = false;
	
	// pair class for returning pairs
	public class Pair<A, B>
	{
		private A left;
		private B right;
		
		public Pair(A a, B b)
		{
			left = a;
			right = b;
		}
		
		public A a() { return left; }
		public B b() { return right; }
	}
	
	/**
	 * Default constructor.
	 * 
	 * @requires windowHeight != 0
	 */
	public Camera() {
		//this.position = position;
		//this.target = target;
		//this.normal = normal;
		//updateWindowSize(windowWidth, windowHeight);
		windowWidth = 640;
		windowHeight = 480;
		//this.fov = fov;
		//this.zNear = zNear;
		//this.zFar = zFar;
		lastPos = null;

		//setDefaultPosition(position, target);
		
		glu = new GLU();
	}

	public void setParameters(Vector3f position, Vector3f target, Vector3f normal, double fov, double zNear, double zFar) {
		this.position = position;
		this.target = target;
		this.normal = normal;
		this.fov = fov;
		this.zNear = zNear;
		this.zFar = zFar;
		setDefaultPosition(position,target);
	}
	
	/**
	 * Sets the default position and target of the camera.
	 * @param position the new default position
	 * @param target the new default target
	 */
	public void setDefaultPosition(Vector3f position, Vector3f target) {
		this.defaultPosition = new Vector3f(position);
		this.defaultTarget = new Vector3f(target);
	}
	
	/**
	 * Resets the camera to default position;
	 */
	public void reset() {
		this.position = new Vector3f(defaultPosition);
		this.target = new Vector3f(defaultTarget);
	}

	/**
	 * Gets the position of the camera.
	 * 
	 * @return position of the camera.
	 */
	public Vector3f getPosition() {
		return position;
	}
	
	public void setPosition(float x, float y, float z) {
		position.x = x;
		position.y = y;
		position.z = z;
	}

	
	/**
	 * Gets the position of the camera target.
	 * 
	 * @return position of camera target.
	 */
	public Vector3f getTarget() {
		return target;
	}
	
	public void setTarget(float x, float y, float z) {
		target.x = x;
		target.y = y;
		target.z = z;
	}
	
	

	/**
	 * Gets the normal of the camera.
	 * 
	 * @return the normal of the camera.
	 */
	public Vector3f getNormal() {
		return normal;
	}

	/**
	 * Gets the current mouse position.
	 * 
	 * @return the current mouse position.
	 */
	public Point getMousePos() {
		return lastPos;
	}
	
	/**
	 * Get mouse click position.
	 * 
	 * @return position of the mouse click.
	 */
	public Point getMouseClick() {
		return clickPosition;
	}
	
	/**
	 * Gets the current camera viewport.
	 * 
	 * @return the camera viewport
	 */
	public int[] getViewport() {
		return viewport;
	}
	
	/**
	 * Returns true if the camera is in orthogonal projection mode.
	 * 
	 * @return true if the camera is in orthogonal projection mode.
	 */
	public boolean isOrtho() {
		return isOrthogonal;
	}
	
	// autocam stuff
	private Vector3f autocamTarget = null;
	private Vector3f autocamPosition = null;
	private boolean autocam = true;
	
	
	public static final float AUTOCAM_VELOCITY = 4.0f; // units per second
	private long time = 0;
	
	public void setAutocamTarget(float x, float y, float z, float radius) {
		if(autocamTarget == null || autocamPosition == null) {
			autocamTarget = new Vector3f(target);
			autocamPosition = new Vector3f(position);
			return;
		}
		
		Vector3f delta = new Vector3f();
		delta.x = position.x - x;
		delta.x = Math.max(Math.abs(delta.x), 3.0f * Math.signum(delta.x));
		delta.y = Math.max(6.0f, position.y - y);
		delta.z = position.z - z;
		delta.z = Math.max(Math.abs(delta.z), 3.0f * Math.signum(delta.z));
		
		delta.normalize();
		delta.scale(Math.max(9.0f, radius * 2.5f));
		
		autocamTarget.x = x;
		autocamTarget.y = y;
		autocamTarget.z = z;
		
		autocamPosition.x = delta.x + autocamTarget.x;
		autocamPosition.y = delta.y + autocamTarget.y;
		autocamPosition.z = delta.z + autocamTarget.z;
	}
	
	private static float sigmoid(float x) {
		return 1.0f / (1.0f + (float)Math.exp(-x));
	}
	
	// this method approaches 1.0f as distance -> 1.0f
	private static float slowdownFactor(float distance) {
		return (sigmoid(distance * 4.0f) - 0.5f) * 2.0f;
		//return sigmoid(distance * 4.0f - 1.0f);
	}
	
	public void doAutocam() {
		// update current time
		long currentTime = System.currentTimeMillis();
		float delta = (currentTime - time) / 1000.0f;
		time = currentTime;
		
		if(autocamTarget == null || autocamPosition == null)
			return;
		
		if(!autocam || time == 0)
			return;
		
		Vector3f temp = new Vector3f();
	
		
		temp.sub(autocamTarget, target);
		float length = temp.length();
		if(length > 0.00001f) {
			temp.normalize();
			temp.scale(AUTOCAM_VELOCITY * delta * slowdownFactor(length));
			target.add(target, temp);
		}
		
		temp.sub(autocamPosition, position);
		length = temp.length();
		if(length > 0.00001f) {
			temp.normalize();
			temp.scale(AUTOCAM_VELOCITY * delta * slowdownFactor(length));
			position.add(position, temp);
		}
		//target.set(autocamTarget);
		//position.set(autocamPosition);
	}
	
	/**
	 * Updates the window width and height for proper projection.
	 * 
	 * @param width
	 *            width of the window.
	 * @param height
	 *            of the window.
	 */
	public void updateWindowSize(int width, int height) {
		this.windowWidth = width;
		this.windowHeight = height;
	}

	/**
	 * Applies the camera transform to a OpenGL context.
	 * 
	 * @param gl
	 *            the context to which the transform is applied.
	 */
	public void setTransform(GL gl, GLU glu) {
		glu.gluLookAt(position.x, position.y, position.z, target.x,
				target.y, target.z, normal.x, normal.y, normal.z);

		gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, modelView, 0);
	}

	/**
	 * Applies the projection transformation from the screen to the world
	 * coordinates.
	 * 
	 * @param glu
	 *            the context to which to apply the transformation.
	 */
	public void setProjection(GL gl, GLU glu) {
		double aspect = 0.0;
		if(windowHeight == 0)
			aspect = windowWidth;
		else
			aspect = (double)windowWidth / (double)windowHeight;
		
		if(!this.isOrthogonal) {
			// perspective projection
			glu.gluPerspective(fov, aspect, zNear, zFar);
		} else {
			// orthogonal projection
			gl.glOrtho(0, windowWidth, 0, windowHeight, -100, 100);
			//gl.glScalef(1, -1, 1);
			//gl.glTranslatef(0.0f, -windowHeight, 0.0f);
		}
		
		gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, proj, 0);
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
	}

	/**
	 * Un-projects a mouse coordinate into world space.
	 * 
	 * @param x
	 *            the horizontal coordinate of the mouse.
	 * @param y
	 *            the vertical coordinate of the mouse.
	 * 
	 * @return a pair of vectors with the first being the origin of the ray and
	 *         the second being the normalized direction of the ray.
	 */
	public Pair<Vector3f, Vector3f> unproject(int x, int y) {
		// set space for unprojected point and viewport size
		double unprojectedPoint[] = new double[3];

		// unproject into worldspace
		glu.gluUnProject((double) x, (double) (viewport[3] - y), 0.0,
				modelView, 0, proj, 0, viewport, 0, unprojectedPoint, 0);
		Vector3f v1 = new Vector3f((float)unprojectedPoint[0], (float)-unprojectedPoint[1],
				(float)-unprojectedPoint[2]);
		glu.gluUnProject((double) x, (double) (viewport[3] - y), 1.0,
				modelView, 0, proj, 0, viewport, 0, unprojectedPoint, 0);
		Vector3f v2 = new Vector3f((float)-unprojectedPoint[0], (float)unprojectedPoint[1],
				(float)unprojectedPoint[2]);

		v2.sub(v1, v2);
		v2.normalize();
		
		return new Pair<Vector3f, Vector3f>(v1, v2);
	}

	/**
	 * Rotates the point specified by pt around an axis by a certain angle.
	 * @param pt the point to rotate.
	 * @param axis the axis about which we will rotate the point.
	 * @param angle the angle by which to rotate, in radians
	 * @return the position of the rotated point
	 */
	public static Vector3f rotateAroundAxis(Vector3f pt, Vector3f axis, Float angle)
	{
		// make a quaternion
		float sin = (float)Math.sin(angle.doubleValue() / 2.0);
		float b = axis.x * sin;
		float c = axis.y * sin;
		float d = axis.z * sin;
		float a = (float)Math.sqrt(1.0 - sin * sin);
		float invLength = 1.0f / (float)Math.sqrt(a * a + b * b + c * c + d * d);
		/*a *= invLength;
		b *= invLength;
		c *= invLength;
		d *= invLength;*/
		
		// make a rotation matrix
		Matrix3f rot = new Matrix3f(a * a + b * b - c * c - d * d, 2.0f * b * c - 2.0f * a * d, 2.0f * a * c + 2.0f * b * d,
									2.0f * a * d + 2.0f * b * c, a * a - b * b + c * c - d * d, 2.0f * c * d - 2.0f * a * b,
									2.0f * b * d - 2.0f * a * c, 2.0f * a * b + 2.0f * c * d, a * a - b * b - c * c + d * d);
		
		Vector3f vec = new Vector3f(pt);
		rot.transform(vec);
		
		return vec;
	}
	
	/**
	 * Handles the mouse motion event for when the mouse is clicked and dragged,
	 * that is when the view is needed to rotate.
	 */
	public void mouseDragged(MouseEvent me) {
		// no movement in orthogonal mode
		if(isOrthogonal)
			return;
		
		Pair<Vector3f, Float> rot = arcballRotate(lastPos, me.getPoint());

		if((me.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 &&
				(me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
			// rotate on left click
			try {
				Vector3f posTemp = new Vector3f();
				posTemp.sub(position, target);
				rot.a().normalize();
				
				// TODO: add awesome rotation code
				posTemp = rotateAroundAxis(posTemp, rot.a(), rot.b());
	
				position.add(posTemp, target);
			} catch (Throwable b) {
				//System.out.println("OOPS");
			}
		} else if((((me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) &&
				(me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) ||
			(((me.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) &&
				((me.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0))){
			// zoom on right click (and modifiers)
			Vector3f delta = new Vector3f();
			delta.sub(position, target);
			
			// if we move down zoom out
			if(me.getY() > lastPos.y)
				delta.scale(1.05f);
			else if(me.getY() < lastPos.y)
				delta.scale(0.95f);
			position.add(delta, target);
		} else if(((me.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) ||
				(((me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) &&
						(me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)) {
			// strafe
			Pair<Vector3f, Vector3f> pNew = unproject(me.getX(), me.getY());
			Pair<Vector3f, Vector3f> pOld = unproject(lastPos.x, lastPos.y);
			
			Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f);
			float denNew = normal.dot(pNew.b());
			float denOld = normal.dot(pOld.b());
			normal.negate();
			Vector3f temp = new Vector3f(0.0f, 0.0f, 0.0f);
			temp.sub(pNew.a(), temp);
			float numNew = normal.dot(temp);
			temp.set(0.0f, 0.0f, 0.0f);
			temp.sub(pOld.a(), temp);
			float numOld = normal.dot(temp);
			
			if(denNew == 0 || denOld == 0) { // we are parallel
			}
			else if(numNew == 0 || numOld == 0) { // we start in plane
			}
			else { // we are good
				float tNew = numNew / denNew;
				float tOld = numOld / denOld;
				
				Vector3f targetPoint = new Vector3f();
				Vector3f sourcePoint = new Vector3f();
				pNew.b().scale(tNew);
				pOld.b().scale(tOld);
				targetPoint.add(pNew.a(), pNew.b());
				sourcePoint.add(pOld.a(), pOld.b());
				
				// direction
				temp.sub(targetPoint, sourcePoint);
				temp.x = -temp.x;
				position.add(temp);
				target.add(temp);
			}
		}

		lastPos = me.getPoint();
	}

	public void mouseMoved(MouseEvent me) {
		// update the last point before dragging starts
		lastPos = me.getPoint();
	}

	/**
	 * Computes the angles of rotation to perform an arc-ball rotation for motion
	 * starting with point s and ending at point e.
	 * 
	 * @param s
	 *            start point of rotation motion.
	 * @param e
	 *            end point of rotation motion.
	 * @return a pair containing a vector representing the axis of rotation and
	 *         an angle rotation about that axis.
	 */
	private Pair<Vector3f, Float> arcballRotate(Point s, Point e) {
		// compute offsets from center of drawing area
		double sx = s.getX() - windowWidth / 2.0;
		double sy = s.getY() - windowHeight / 2.0;
		double ex = e.getX() - windowWidth / 2.0;
		double ey = e.getY() - windowHeight / 2.0;

		// y axis should point the other way
		sy = -sy;
		ey = -ey;

		// compute scale to normalise to window size
		double scale = 1.0 / (double) Math.min(windowWidth, windowHeight);

		// scale components
		sx *= scale;
		sy *= scale;
		ex *= scale;
		ey *= scale;

		// project components onto circle
		double sl = Math.sqrt(sx * sx + sy * sy);
		double el = Math.sqrt(ex * ex + ey * ey);

		// normalize components once more
		if (sl > 1.0) {
			sx /= sl;
			sy /= sl;
			sl = 1.0;
		}
		if (el > 1.0) {
			ex /= el;
			ey /= el;
			el = 1.0;
		}

		// project onto sphere
		double sz = Math.sqrt(1.0 - sl * sl);
		double ez = Math.sqrt(1.0 - el * el);

		double dot = sx * ex + sy * ey + sz * ez;

		return new Pair<Vector3f, Float>(new Vector3f(new Vector3d(sy * ez - ey * sz, sz * ex - ez
				* sx, sx * ey - ex * sy)), new Float(-2.0f * Math.acos(dot)));
	}

	public void keyPressed(KeyEvent ke) {
		if(ke.getKeyCode() == KeyEvent.VK_R && ((ke.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
			if(!isOrthogonal)
				reset();
		} else if(ke.getKeyCode() == KeyEvent.VK_O) {
			isOrthogonal = !isOrthogonal;
		} else if(ke.getKeyCode() == KeyEvent.VK_C) {
			autocam = !autocam;
		}
	}

	public void keyReleased(KeyEvent ke) {
	}

	public void keyTyped(KeyEvent ke) {
	}

	public void mouseWheelMoved(MouseWheelEvent mwe) {
		// zoom on mouse scroll as well
		Vector3f delta = new Vector3f();
		delta.sub(position, target);
		
		// if we move down zoom out
		if(mwe.getWheelRotation() < 0)
			delta.scale(0.95f);
		else if(mwe.getWheelRotation() > 0)
			delta.scale(1.05f);

		position.add(delta, target);
		
	}

	public void mouseClicked(MouseEvent me) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent me) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent me) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent me) {
		if(me.getButton() == MouseEvent.BUTTON1)
			clickPosition = me.getPoint();
	}

	public void mouseReleased(MouseEvent me) {
		if(me.getButton() == MouseEvent.BUTTON1)
			clickPosition = null;
	}
}
