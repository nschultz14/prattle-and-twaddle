package com.carnivorous_exports.terrain;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.MessageFormat;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.Animator;

import static javax.media.opengl.GL.*; // GL constants
import static javax.media.opengl.GL2.*; // GL2 constants
import static javax.media.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_COLOR_MATERIAL;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHT0;

/**
 * JOGL 2.0 Program Template (GLCanvas) This is a "Component" which can be added
 * into a top-level "Container". It also handles the OpenGL events to render
 * graphics.
 */
@SuppressWarnings("serial")
public class Cubes2Renderer extends GLCanvas implements GLEventListener,
		KeyListener, MouseListener, MouseMotionListener {

	private GLU glu; // for the GL Utility
	private int cubeDList; // display list for cube

	private boolean mouseRButtonDown;
	private int prevMouseX;
	private int prevMouseY;
	private float view_rotx;
	private float view_roty;
	private float view_rotz;
	private float movex;
	private float movey;
	private float movez;

	// for testing rotation
	float tempRotX;

	// for stopping key auto-repeat
	private boolean[] pressedYet = new boolean[5];

	// for running
	String moveAxis;
	int moveDir;

	// final Animator animator = new Animator();

	private static float[][] boxColors = { // Bright: Red, Orange, Yellow,
			// Green, Blue
			{ 1.0f, 0.0f, 0.0f }, { 1.0f, 0.5f, 0.0f }, { 1.0f, 1.0f, 0.0f },
			{ 0.0f, 1.0f, 0.0f }, { 0.0f, 1.0f, 1.0f } };

	/** Constructor to setup the GUI for this Component */
	public Cubes2Renderer() {

		this.addGLEventListener(this);
		this.addKeyListener(this); // for Handling KeyEvents
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.setFocusable(true);
		this.requestFocus();
	}

	public void buildDisplayList(GL2 gl) {
		// Build two lists, and returns handle for the first list
		// create one display list
		// GLuint index = glGenLists(1);

		int base = gl.glGenLists(1);

		// Create a new list for box (with open-top), pre-compile for efficiency
		cubeDList = base;

		gl.glNewList(cubeDList, GL_COMPILE);
		gl.glBegin(GL_QUADS);

		// Top-face
		gl.glColor3f(0.0f, 1.0f, 0.0f); // green
		gl.glVertex3f(1.0f, 1.0f, -1.0f);
		gl.glVertex3f(-1.0f, 1.0f, -1.0f);
		gl.glVertex3f(-1.0f, 1.0f, 1.0f);
		gl.glVertex3f(1.0f, 1.0f, 1.0f);

		// Bottom-face
		gl.glColor3f(1.0f, 0.5f, 0.0f); // orange
		gl.glVertex3f(1.0f, -1.0f, 1.0f);
		gl.glVertex3f(-1.0f, -1.0f, 1.0f);
		gl.glVertex3f(-1.0f, -1.0f, -1.0f);
		gl.glVertex3f(1.0f, -1.0f, -1.0f);

		// Front-face
		gl.glColor3f(1.0f, 0.0f, 0.0f); // red
		gl.glVertex3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(-1.0f, 1.0f, 1.0f);
		gl.glVertex3f(-1.0f, -1.0f, 1.0f);
		gl.glVertex3f(1.0f, -1.0f, 1.0f);

		// Back-face
		gl.glColor3f(1.0f, 1.0f, 0.0f); // yellow
		gl.glVertex3f(1.0f, -1.0f, -1.0f);
		gl.glVertex3f(-1.0f, -1.0f, -1.0f);
		gl.glVertex3f(-1.0f, 1.0f, -1.0f);
		gl.glVertex3f(1.0f, 1.0f, -1.0f);

		// Left-face
		gl.glColor3f(0.0f, 0.0f, 1.0f); // blue
		gl.glVertex3f(-1.0f, 1.0f, 1.0f);
		gl.glVertex3f(-1.0f, 1.0f, -1.0f);
		gl.glVertex3f(-1.0f, -1.0f, -1.0f);
		gl.glVertex3f(-1.0f, -1.0f, 1.0f);

		// Right-face
		gl.glColor3f(1.0f, 0.0f, 1.0f); // magenta
		gl.glVertex3f(1.0f, 1.0f, -1.0f);
		gl.glVertex3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(1.0f, -1.0f, 1.0f);
		gl.glVertex3f(1.0f, -1.0f, -1.0f);

		gl.glEnd();
		gl.glEndList();
	}

	// for user movement
	public void running() {

		
		if(moveAxis == "movez") {
			if(moveDir > 0) {
				//going forwards
				movez += Math.cos(view_roty/360);
				movex += Math.sin(view_roty/360);
			} else if(moveDir < 0) {
				//going backwards
				movez -= Math.cos(view_roty/360);
				movex -= Math.sin(view_roty/360);
			}
		}
		
		/*
		
		if (moveAxis == "movex") {
			// movex += moveDir * (0.1) * (float) Math.cos(view_roty+90);
			// movez += moveDir * (0.1) * (float) Math.sin(view_roty+90);
			movex += moveDir * (0.1);
		} else if (moveAxis == "movez") {
			// movex += moveDir * (0.1) * (float) Math.cos(Math.abs(view_rotx));
			// movez += moveDir * (0.1) * (float) Math.sin(Math.abs(view_rotx));
			movez += moveDir * (0.1);
		}
		
		*/
	}

	// ------ Implement methods declared in GLEventListener ------

	/**
	 * Called back immediately after the OpenGL context is initialized. Can be
	 * used to perform one-time initialization. Run only once.
	 */
	@Override
	public void init(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL graphics context
		glu = new GLU(); // get GL Utilities
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
		gl.glClearDepth(1.0f); // set clear depth value to farthest
		gl.glEnable(GL_DEPTH_TEST); // enables depth testing
		gl.glDepthFunc(GL_LEQUAL); // the type of depth test to do
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best
																// perspective
																// correction
		gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out
									// lighting

		// Enable LIGHT0, which is pre-defined on most video cards.
		gl.glEnable(GL_LIGHT0);
		// gl.glEnable(GL_LIGHTING);

		// Add colors to texture maps, so that glColor3f(r,g,b) takes effect.
		gl.glEnable(GL_COLOR_MATERIAL);

		// We want the best perspective correction to be done
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

		// ----- Your OpenGL initialization code here -----
		buildDisplayList(gl);
	}

	/**
	 * Call-back handler for window re-size event. Also called when the drawable
	 * is first set to visible.
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context

		if (height == 0)
			height = 1; // prevent divide by zero
		float aspect = (float) width / height;

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL_PROJECTION); // choose projection matrix
		gl.glLoadIdentity(); // reset projection matrix
		glu.gluPerspective(45.0, aspect, 0.1, 100.0); // fovy, aspect, zNear,
														// zFar

		// Enable the model-view transform
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity(); // reset
	}

	/**
	 * Called back by the animator to perform rendering.
	 */
	@Override
	public void display(GLAutoDrawable drawable) {
		// System.out.println("Display Method Called!");

		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear color
																// and depth
																// buffers

		gl.glPushMatrix();

		// translate before rotate

		 //gl.glTranslatef(movex, movey, movez);

		// rotate around wherever the user drags the mouse
		gl.glRotatef(-view_rotx, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(-view_roty, 0.0f, 1.0f, 0.0f);
		gl.glRotatef(-view_rotz, 0.0f, 0.0f, 1.0f);

		//gl.glTranslatef(movex, movey, movez);

		// --------- Rendering Code
		for (int i = 0; i < 5; i++) {

			gl.glPushMatrix();

			gl.glTranslatef(-3.0f + i * 3f, 0.0f, -6.0f);

			//rotate the cube to face the camera
				//I don't know if I'm doing this right
				//I need to investigate what view_rotx, y, z ARE
			//translate the cube along its axis
			//rotate the cube back to it's original rotation
				//I don't know how to access/store an object's rotation
			
			
			
			
			gl.glTranslatef(movex, movey, movez);
			
		//	gl.glRotatef(-view_rotx, 1.0f, 0.0f, 0.0f);
		//	gl.glRotatef(-view_roty, 0.0f, 1.0f, 0.0f);
		//	gl.glRotatef(-view_rotz, 0.0f, 0.0f, 1.0f);
			
			
			//gl.glRotatef(1, 1.0f, 0.0f, 0.0f);
			//gl.glRotatef(1, 0.0f, 1.0f, 0.0f);
			//gl.glRotatef(1, 0.0f, 0.0f, 1.0f);

			
			
			
			
			
			
			//gl.glTranslatef(-3.0f + i * 3f, 0.0f, -6.0f);
			
////////////////////////////
		/*	
			gl.glRotatef(-view_rotx, 1.0f, 0.0f, 0.0f);
			//gl.glRotatef(-view_roty, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(-view_rotz, 0.0f, 0.0f, 1.0f);

			gl.glTranslatef(0, movey, 0);

			gl.glRotatef(0, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(0, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(0, 0.0f, 0.0f, 1.0f);
			
///////////////////////////////
			
			//gl.glRotatef(-view_rotx, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(-view_roty, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(-view_rotz, 0.0f, 0.0f, 1.0f);

			gl.glTranslatef(movex, 0, 0);

			gl.glRotatef(0, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(0, 0.0f, 1.0f, 0.0f);
			gl.glRotatef(0, 0.0f, 0.0f, 1.0f);
	*/

			 //gl.glTranslatef(-3.0f + i * 3f, 0.0f, -6.0f);

			gl.glColor3fv(boxColors[2], 0);

			if (i == 1) {
				tempRotX += 2f;
				gl.glRotatef(tempRotX, 1.0f, 0.0f, 0.0f);
			}

			// gl.glTranslatef(-3.0f + i * 3f, 0.0f, -6.0f);

			gl.glCallList(cubeDList); // draw the cube
			gl.glPopMatrix();
		}

		gl.glPopMatrix();
		running();
	}

	/**
	 * Called back before the OpenGL context is destroyed. Release resource such
	 * as buffers.
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void keyPressed(KeyEvent e) {

		// press esc to quit
		int keyCode = e.getKeyCode();
		switch (keyCode) {
		case KeyEvent.VK_ESCAPE: // quit
			// Use a dedicate thread to run the stop() to ensure that the
			// animator stops before program exits.
			new Thread() {
				@Override
				public void run() {
					GLAnimatorControl animator = getAnimator();
					if (animator.isStarted())
						animator.stop();
					System.exit(0);
				}
			}.start();
			break;
		}

		// to move
		// if (0 == (InputEvent.AUTOREPEAT_MASK & e.getModifiers())) {

		if (!pressedYet[1] && keyCode == KeyEvent.VK_LEFT)
			changeMove(e);
		if (!pressedYet[2] && keyCode == KeyEvent.VK_RIGHT)
			changeMove(e);
		if (!pressedYet[3] && keyCode == KeyEvent.VK_UP)
			changeMove(e);
		if (!pressedYet[4] && keyCode == KeyEvent.VK_DOWN)
			changeMove(e);

	}

	public void changeMove(KeyEvent e) {

		int kc = e.getKeyCode();
		if (KeyEvent.VK_LEFT == kc) {
			pressedYet[1] = true;
			moveAxis = "movex";
			moveDir = -1;
		} else if (KeyEvent.VK_RIGHT == kc) {
			pressedYet[2] = true;
			moveAxis = "movex";
			moveDir = +1;
		} else if (KeyEvent.VK_UP == kc) {
			pressedYet[3] = true;
			moveAxis = "movez";
			moveDir = +1;
		} else if (KeyEvent.VK_DOWN == kc) {
			pressedYet[4] = true;
			moveAxis = "movez";
			moveDir = -1;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int kc = e.getKeyCode();
		if (kc == KeyEvent.VK_LEFT)
			pressedYet[1] = false;
		else if (kc == KeyEvent.VK_RIGHT)
			pressedYet[2] = false;
		else if (kc == KeyEvent.VK_UP)
			pressedYet[3] = false;
		else if (kc == KeyEvent.VK_DOWN)
			pressedYet[4] = false;

		// if no arrow keys are pressed, stop
		if (!pressedYet[1] && !pressedYet[2] && !pressedYet[3]
				&& !pressedYet[4])
			moveDir = 0;
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	// class Cubes2MouseAdapter extends MouseAdapter implements MouseListener {

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		prevMouseX = e.getX();
		prevMouseY = e.getY();
		if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
			mouseRButtonDown = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
			mouseRButtonDown = false;
		}
	}

	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		int width = 0, height = 0;
		Object source = e.getSource();
		if (source instanceof Window) {
			Window window = (Window) source;
			width = window.getWidth();
			height = window.getHeight();
		} else if (GLProfile.isAWTAvailable()
				&& source instanceof java.awt.Component) {
			java.awt.Component comp = (java.awt.Component) source;
			width = comp.getWidth();
			height = comp.getHeight();
		} else {
			throw new RuntimeException(
					"Event source neither Window nor Component: " + source);
		}
		float thetaY = 360.0f * ((float) (x - prevMouseX) / (float) width);
		float thetaX = 360.0f * ((float) (prevMouseY - y) / (float) height);

		prevMouseX = x;
		prevMouseY = y;

		view_rotx += thetaX;
		view_roty += thetaY;
	}
}
