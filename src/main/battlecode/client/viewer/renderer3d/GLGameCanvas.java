package battlecode.client.viewer.renderer3d;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLProfile;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputListener;

import battlecode.client.viewer.render.BaseCanvas;
import battlecode.client.viewer.render.BaseRenderer;
import battlecode.client.viewer.render.RenderConfiguration;

import com.jogamp.opengl.util.FPSAnimator;

public final class GLGameCanvas extends BaseCanvas {

	private static final long serialVersionUID = 0; // don't serialize

	private GLGameRenderer renderer = null;
	private GLCanvas canvas = null;
	private FPSAnimator animator = null;

	static {
		System.setProperty("jogamp.gluegen.UseTempJarCache","false");
	}

	public GLGameCanvas() {
		super();
		
		// add the canvas for painting, then start its update thread
		canvas = new GLCanvas();
		this.setLayout(new GridLayout(1,1));
		add(canvas);
		canvas.setVisible(true);
		
		animator = new FPSAnimator(canvas, 60);
		animator.start();
	}

	protected BaseRenderer getRenderer() { return renderer; }

	public void setRenderer(BaseRenderer r) {
		GLGameRenderer renderer = (GLGameRenderer)r;
		/*
		for (MouseListener ml: canvas.getMouseListeners()) {
			removeMouseListener(ml);
		}
		for (MouseMotionListener mml: canvas.getMouseMotionListeners()) {
			removeMouseMotionListener(mml);
		}
		*/
		
		animator.stop();
		
		// only allow one renderer to be set to the current canvas
		if(this.renderer != null) {
			canvas.removeGLEventListener(this.renderer);
			this.renderer.removeCanvasReference();
			this.renderer = null;
		}

		this.renderer = renderer;
		
		// this forces an init call on the game renderer to reinitialize
		// everything that is necessary (like the GLU (damn GLU!))
		// specifically this forces the context for OpenGL to be recreated
		/*remove(canvas);
		add(canvas);*/

		// this adds a drawing handler to the canvas (THIS IS ONLY TO KEEP OLD CODE STRUCTURE)
		// optimally we would probably need to split these up since Renderer doesn't need to
		// know about the canvas
		canvas.addGLEventListener(renderer);
		Camera cam = renderer.getCamera();
		addMouseMotionListener(cam);
        addMouseInputListener(cam);
        addKeyListener(cam);
		addMouseWheelListener(cam);

		animator.start();
		
		setVisible(true);
		renderer.setCanvasSize(getSize());
		/*
		if (isDisplayable() && isVisible()) {
			renderer.setCanvasSize(getSize());
		}
		*/
	}

	public void setTournamentMode() {
		RenderConfiguration.setTournamentMode(true);
		setVisible(true);
	}
	
	public void display() {
		canvas.display();
	}
	
	public GL getGL() {
		return canvas.getGL();
	}
	
	public int getWidth() {
		return canvas.getWidth();
	}
	
	public int getHeight() {
		return canvas.getHeight();
	}
	
	public void addMouseListener(MouseListener m){
		canvas.addMouseListener(m);
	}
	
	public void addMouseMotionListener(MouseMotionListener m) {
		canvas.addMouseMotionListener(m);
	}
	
	public void addMouseInputListener(MouseInputListener m) {
		canvas.addMouseListener(m);
	}
	
	public void addKeyListener(KeyListener k) {
		canvas.addKeyListener(k);
	}
	
	public void addMouseWheelListener(MouseWheelListener mwe) {
		canvas.addMouseWheelListener(mwe);
	}
	
	public void removeMouseMotionListener(MouseMotionListener m) {
		canvas.removeMouseMotionListener(m);
	}
	
	public void removeMouseInputListener(MouseInputListener m) {
		canvas.removeMouseListener(m);
	}
	
	public void removeKeyListener(KeyListener k) {
		canvas.removeKeyListener(k);
	}
	
	public void removeMouseWheelListener(MouseWheelListener m) {
		canvas.removeMouseWheelListener(m);
	}

	public void addPaintObserver(Observer o) {
		renderer.displayObservable.addObserver(o);
	}

}
