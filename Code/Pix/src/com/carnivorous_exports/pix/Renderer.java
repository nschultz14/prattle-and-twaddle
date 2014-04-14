package com.carnivorous_exports.pix;

import java.awt.AWTException;
import java.awt.Robot;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.opengl.GLWindow;

import static javax.media.opengl.GL.*; // GL constants
import static javax.media.opengl.GL2.*; // GL2 constants
import static javax.media.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_AMBIENT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_COLOR_MATERIAL;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHT1;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_POSITION;

@SuppressWarnings("serial")
public class Renderer implements GLEventListener,
		com.jogamp.newt.event.MouseListener, com.jogamp.newt.event.KeyListener {

	private static GLWindow window;
	private GLU glu; // for the GL Utility
	public boolean audioOn = false;
	private int[] cubeList; // display list for cube
	private Scene terrain = new Scene();
	private boolean initiated = false;
	GLAutoDrawable drawable;

	public static Audio audio = new Audio();
	int walkNum = 0;

	// Prepare light parameters.
	float SHINE_ALL_DIRECTIONS = 1;
	float[] lightPos = { 20, 30, 20, SHINE_ALL_DIRECTIONS };
	float[] lightDif = { 0.6f, 0.6f, 0.6f, 1.0f };
	float[] lightColorAmbient = { 0.2f, 0.2f, 0.2f, 1f };
	float[] lightColorSpecular = { 0.8f, 0.8f, 0.8f, 1f };
	float cameraPos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
	float[] colorWhite = { 1.0f, 1.0f, 1.0f, 1.0f };

	Robot robot;
	int width;
	int height;

	// public boolean initialized;
	private boolean mouseRButtonDown;
	private boolean mouseInMiddle = false;
	private int prevMouseX;
	private int prevMouseY;
	private int mouseX;
	private int mouseY;
	private int mouseXGlobal;
	private int mouseYGlobal;
	public float view_rotx;
	public float view_roty;
	public float view_rotz;
	public float oldRotX;
	public float oldRotY;
	public float oldRotZ;
	public float movex;
	public float movey;
	public float movez;
	private float mouseSensitivity = 0.75f;

	// for movement
	private boolean upPressed;
	private boolean downPressed;
	private boolean rightPressed;
	private boolean leftPressed;

	private boolean flyUpPressed;
	private boolean flyDownPressed;

	private boolean forwardMove;
	private boolean strifeMove;

	private boolean flyUpMove;
	private boolean flyDownMove;

	//private boolean[] collided = {false, false, false};

	int moveDirForward;
	int moveDirStrife;
	float moveSpeed = 2f;

	float selectedObject;

	// picking variables
	int BUFSIZE = 1024;
	IntBuffer selectBuf = Buffers.newDirectIntBuffer(BUFSIZE);
	boolean pick = false;
	public static int hits;
	int textureNum = 0; // picking test variable

	public Renderer(GLWindow window) {

		this.window = window;
		window.addGLEventListener(this);
		window.addMouseListener(this);
		window.addKeyListener(this);
		window.requestFocus();
	}

	// for user movement
	public void checkMoving() {

		if (forwardMove) { // moving forward or back
			movez += Math.cos(180 - view_roty * (Math.PI / 180) + 40) * 0.1
					* -moveDirForward * moveSpeed;
			movex -= Math.sin(180 - view_roty * (Math.PI / 180) + 40) * 0.1
					* -moveDirForward * moveSpeed;

		}

		if (strifeMove) { // moving right or left
			movez -= Math.cos(180 - view_roty * (Math.PI / 180) + 40 + 80.1
					* -moveDirStrife)
					* 0.1 * moveSpeed;
			movex += Math.sin(180 - view_roty * (Math.PI / 180) + 40 + 80.1
					* -moveDirStrife)
					* 0.1 * moveSpeed;
		}

		//moving up or down
		if (flyUpMove)
			movey -= 0.1;
		if (flyDownMove)
			movey += 0.1;
	}

	/**
	 * Get the current mouse position in world coordinates using gluUnProject. "d"
	 * is the distance away from the screen: d == 0.0 is at the screen, d == 1.0
	 * is very far away from the screen
	 * 
	 * @return
	 */
	public double[] getPositionUnProject(GL2 gl, float d) {

		int viewport[] = new int[4];
		double modelview[] = new double[16];
		double projection[] = new double[16];
		float winX, winY, winZ;
		float posX, posY, posZ;
		double mouse3DPos[] = new double[4];

		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

		winX = (float) mouseX;
		winY = (float) viewport[3] - (float) mouseY;

		glu.gluUnProject(winX, winY, d, modelview, 0, projection, 0, viewport,
				0, mouse3DPos, 0);

		return mouse3DPos;
	}

	/**
	 * Get the names of objects that are picked using gluPickMatrix.
	 * Call startPicking() before drawing and stopPicking() after drawing.
	 * Name objects in Scene.java
	 * @return
	 */
	public void startPicking(GL2 gl) {
		IntBuffer viewport = Buffers.newDirectIntBuffer(4);
		float ratio;

		gl.glSelectBuffer(BUFSIZE, selectBuf);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);
		gl.glRenderMode(GL2.GL_SELECT);
		gl.glInitNames();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		GLU glu = new GLU();
		glu.gluPickMatrix(mouseXGlobal, viewport.get(3) - mouseYGlobal, 5, 5,
				viewport);

		ratio = (float) (viewport.get(2) + 0.0f) / (float) viewport.get(3);
		glu.gluPerspective(45, ratio, 0.1, 1000);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	// returns the name of the picked object
	public int[] stopPicking(GL2 gl) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glFlush();

		hits = gl.glRenderMode(GL2.GL_RENDER);
		
		if (hits > 0) {
			pick = false;
			return processHits(hits, selectBuf);
		} else {
			pick = false;
			return new int[0];
		}
	}

	public int[] processHits(int hits, IntBuffer buffer) {

		int[] out = new int[hits];

		int offset = 0;
		int names;
		float z1, z2;
		for (int i = 0; i < hits; i++) {
			names = buffer.get(offset);
			offset++;
			z1 = (float) (buffer.get(offset) & 0xffffffffL) / 0x7fffffff;
			offset++;
			z2 = (float) (buffer.get(offset) & 0xffffffffL) / 0x7fffffff;
			offset++;

			for (int j = 0; j < names; j++) {
				int q = buffer.get(offset);
				out[i] = q;
				offset++;
			}
		}

		return out;
	}

	//draw the scene
	public void draw(GL2 gl, int textureNum) {

		gl.glPushMatrix();

		// rotate around wherever the user points the mouse
		gl.glRotatef(-view_rotx, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(-view_roty, 0.0f, 1.0f, 0.0f);
		gl.glRotatef(-view_rotz, 0.0f, 0.0f, 1.0f);

		gl.glTranslatef(movex, movey, movez);

		// terrain.drawScene(gl);
		terrain.testLightCube(gl, cubeList, lightPos, textureNum);

		gl.glLightfv(GL_LIGHT1, GL_AMBIENT, lightColorAmbient, 0);
		gl.glLightfv(GL_LIGHT1, GL_DIFFUSE, lightDif, 0);
		gl.glLightfv(GL_LIGHT1, GL_SPECULAR, lightColorSpecular, 0);
		gl.glLightfv(GL_LIGHT1, GL_POSITION, lightPos, 0);

		// terrain.testLight(gl, lightPos);

		gl.glPopMatrix();
	}

	public static void initAudio() {

		audio.newFile(1, "SoundEffects/Groups/Humanoids/Walking1.wav", false,
				new float[3], 1.0f, 500);
		audio.newFile(2, "SoundEffects/Groups/Humanoids/Walking2.wav", false,
				new float[3], 1.0f, 500);
		audio.newFile(3, "SoundEffects/Groups/Humanoids/Walking3.wav", false,
				new float[3], 1.0f, 500);
		audio.newFile(4, "SoundEffects/Groups/Humanoids/Walking4.wav", false,
				new float[3], 1.0f, 500);
	}

	/**
	 * Called back immediately after the OpenGL context is initialized. Can be
	 * used to perform one-time initialization. Run only once.
	 */
	@Override
	public void init(GLAutoDrawable drawable) {

		initAudio();

		drawable.getAnimator().setUpdateFPSFrames(3, null); // 3
		drawable.setAutoSwapBufferMode(false);

		width = window.getWidth();
		height = window.getHeight();

		// move the mouse to the center
		try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		robot.mouseMove(window.getWidth() / 2, window.getHeight() / 2);
		mouseInMiddle = true;

		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL graphics context
		glu = new GLU(); // get GL Utilities
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
		gl.glClearDepth(1.0f); // set clear depth value to farthest

		// for anti-aliasing
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL2.GL_CULL_FACE);
		// gl.glEnable(GL_DEPTH_TEST);
		// gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);

		// Enable lighting in GL.
		gl.glEnable(GL_LIGHT1);
		gl.glEnable(GL_LIGHTING);

		gl.glEnable(GL_DEPTH_TEST); // enables depth testing
		gl.glDepthFunc(GL_LEQUAL); // the type of depth test to do
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best
																// perspective
																// correction

		gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out
									// lighting

		// Add colors to texture maps, so that glColor3f(r,g,b) takes effect.
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL.GL_TEXTURE_2D);

		// set the drawing mode
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glRenderMode(GL2.GL_RENDER);

		// We want the best perspective correction to be done
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

		// ----- Your OpenGL initialization code here -----
		cubeList = new int[7];
		cubeList[0] = terrain.getCubeList(gl,
				"terrainTextures/Layer Rock.jpeg", ".jpeg");
		cubeList[1] = terrain.getCubeList(gl,
				"terrainTextures/MMud Texture.jpeg", ".jpeg");
		cubeList[2] = terrain.getCubeList(gl,
				"terrainTextures/Night Sky-.jpeg", ".jpeg");
		cubeList[3] = terrain.getCubeList(gl,
				"terrainTextures/Rock Texture-.jpeg", ".jpeg");
		cubeList[4] = terrain.getCubeList(gl,
				"terrainTextures/Sand Texture 1.jpeg", ".jpeg");
		cubeList[5] = terrain.getCubeList(gl,
				"terrainTextures/Water Texture 1.jpeg", ".jpeg");
		cubeList[6] = terrain.getCubeList(gl,
				"terrainTextures/White Water Texture.jpeg", ".jpeg");

		if (!initiated)
			// terrain.buildScene(drawable, this, gl, cubeList);
			terrain.testLightCube(gl, cubeList, lightPos, 0);
		initiated = true;
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

		long startNanos = System.nanoTime();

		this.drawable = drawable;

		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear color
																// and depth
																// buffers

		int[] pickedObject = null;

		if (pick) {
			startPicking(gl);
			draw(gl, 0);
			pickedObject = stopPicking(gl);

			System.out.print("( ");
			for (int i = 0; i < pickedObject.length; i++) {
				if (pickedObject[i] == 1 && textureNum < 6) {
					textureNum++;
				}
				System.out.print(pickedObject[i] + " , ");
			}
			System.out.println(" )");

			// System.out.println("TEXTURE NUM: " + textureNum);
		}

		draw(gl, textureNum);

		checkKeysPressed();

		float[] in = terrain.checkCollisions(movex, movey, movez);
		
		checkMoving();

		if(in[3] == 1f || in[4] == 1f || in[5] == 1f) {
			movex = in[0];
			movey = in[1];
			movez = in[2];
		}
			
		oldRotX = view_rotx;
		oldRotY = view_roty;
		oldRotZ = view_rotz;

		long drawNanos = System.nanoTime() - startNanos;
		// System.out.println("drawn in " + drawNanos);
		// System.out.println(drawable.getAnimator().getLastFPS());

		drawable.swapBuffers();
		gl.glFlush();
	}

	/**
	 * Called back before the OpenGL context is destroyed. Release resource such
	 * as buffers.
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {

	}

	public void checkKeysPressed() {

		if (initiated) {

			if (upPressed && !downPressed) {
				moveDirForward = -1;
			} else if (downPressed && !upPressed) {
				moveDirForward = +1;
			}

			if (leftPressed && !rightPressed) {
				moveDirStrife = +1;
			} else if (rightPressed && !leftPressed) {
				moveDirStrife = -1;
			}

			if (!leftPressed && !rightPressed) {
				strifeMove = false;
			}

			if (!upPressed && !downPressed) {
				forwardMove = false;
			}

			if (upPressed || downPressed) {
				forwardMove = true;
			}

			if (rightPressed || leftPressed) {
				strifeMove = true;
			}

			if (upPressed && downPressed) {
				forwardMove = false;
			}

			if (rightPressed && leftPressed) {
				strifeMove = false;
			}

			if (flyUpPressed && !flyDownPressed) {
				flyUpMove = true;
			}

			if (flyDownPressed && !flyUpPressed) {
				flyDownMove = true;
			}

			if (!flyUpPressed) {
				flyUpMove = false;
			}

			if (!flyDownPressed) {
				flyDownMove = false;
			}

			if (audioOn) {
				// for walking audio
				if (!forwardMove && !strifeMove) {
					audio.stop(walkNum);
				} else if ((forwardMove || strifeMove)
						&& !audio.isPlaying(walkNum)) {
					walkNum = (int) (Math.random() * 4);
					audio.play(walkNum);
				}
			}
		}
	}

	@Override
	public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
		pick = true;
	}

	@Override
	public void mouseDragged(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
		if (initiated) {
			/**
			 * To prevent the mouse from hitting the edge of the screen we have
			 * to move the mouse to the center of the screen every other
			 * iteration and keep track of the mouse position ourselves
			 * 
			 * mouseInMiddle == false when we are iterating just to keep the
			 * mouse in the middle
			 */

			mouseXGlobal = e.getX();
			mouseYGlobal = e.getY();

			if (!mouseInMiddle)
				return;

			prevMouseY = height / 2; // because the mouse is always in the
										// middle of
			prevMouseX = width / 2; // the screen, the prevMouseX/Y is also
									// always
									// the middle of the screen
			float thetaY;
			float thetaX;

			mouseY = prevMouseY - e.getY();
			mouseX = prevMouseX - e.getX();

			thetaY = 360.0f * ((float) (mouseX) / (float) width);
			thetaX = 360.0f * ((float) (mouseY) / (float) height);

			prevMouseX = mouseX;
			prevMouseY = mouseY;

			// restricting x rotation movement to make physical sense
			// DOESNT WORK
			if (view_rotx + thetaX * mouseSensitivity < 180
					&& view_rotx + thetaX * mouseSensitivity > -180) {
				view_rotx += thetaX * mouseSensitivity;
			}

			view_roty += thetaY * mouseSensitivity;

			view_roty = view_roty % 360;

			mouseInMiddle = false;

			// both robot.mouseMove and window.warpPointer work
			// robot.mouseMove(width / 2, height / 2);
			window.warpPointer(width / 2, height / 2);

			mouseInMiddle = true;
		}
	}

	@Override
	public void mousePressed(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(com.jogamp.newt.event.KeyEvent e) {

		if (initiated) {
			// press esc to quit
			int keyCode = e.getKeyCode();
			switch (keyCode) {
			case com.jogamp.newt.event.KeyEvent.VK_ESCAPE: // quit
				// Use a dedicate thread to run the stop() to ensure that the
				// animator stops before program exits.
				new Thread() {
					@Override
					public void run() {
						GLAnimatorControl animator = window.getAnimator();
						if (animator.isStarted())
							animator.stop();
						System.exit(0);
					}
				}.start();
				break;
			}

			// to move
			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_LEFT
					|| keyCode == com.jogamp.newt.event.KeyEvent.VK_A)
				leftPressed = true;
			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_RIGHT
					|| keyCode == com.jogamp.newt.event.KeyEvent.VK_D)
				rightPressed = true;
			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_UP
					|| keyCode == com.jogamp.newt.event.KeyEvent.VK_W)
				upPressed = true;
			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_DOWN
					|| keyCode == com.jogamp.newt.event.KeyEvent.VK_S)
				downPressed = true;

			// flying up and down (for debugging)
			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_SHIFT) {
				flyUpPressed = true;
			}

			if (keyCode == com.jogamp.newt.event.KeyEvent.VK_CONTROL) {
				flyDownPressed = true;
			}
		}
	}

	@Override
	public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
		if (initiated) {
			if (e.isAutoRepeat())
				return;

			int kc = e.getKeyCode();
			if (kc == com.jogamp.newt.event.KeyEvent.VK_LEFT
					|| kc == com.jogamp.newt.event.KeyEvent.VK_A)
				leftPressed = false;
			else if (kc == com.jogamp.newt.event.KeyEvent.VK_RIGHT
					|| kc == com.jogamp.newt.event.KeyEvent.VK_D)
				rightPressed = false;
			else if (kc == com.jogamp.newt.event.KeyEvent.VK_UP
					|| kc == com.jogamp.newt.event.KeyEvent.VK_W)
				upPressed = false;
			else if (kc == com.jogamp.newt.event.KeyEvent.VK_DOWN
					|| kc == com.jogamp.newt.event.KeyEvent.VK_S)
				downPressed = false;

			// flying up and down (for debugging)
			if (kc == com.jogamp.newt.event.KeyEvent.VK_SHIFT) {
				flyUpPressed = false;
			} else if (kc == com.jogamp.newt.event.KeyEvent.VK_CONTROL) {
				flyDownPressed = false;
			}
		}
	}
}