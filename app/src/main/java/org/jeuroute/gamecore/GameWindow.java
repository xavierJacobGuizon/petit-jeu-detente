package org.jeuroute.gamecore;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.hud.Hud;

public class GameWindow {

	private static final double RIGHT_DRAG_THRESHOLD_PIXELS = 5.0;
	private static final long RIGHT_CLICK_MAX_DURATION_NS = 2_000_000_000L;

	private final int width;
	private final int height;
	private final String title;
	private long window;
	private double mouseX;
	private double mouseY;
	private MouseHandler mouseHandler;
	private Hud hud;
	private Camera2D camera;
	private boolean panningWithMouse;
	private boolean rightButtonPressed;
	private double rightButtonPressX;
	private double rightButtonPressY;
	private long rightButtonPressTimeNs;
	private double lastPanMouseX;
	private double lastPanMouseY;
	private Runnable rightClickHandler = () -> {};
	private boolean movingLeft;
	private boolean movingRight;
	private boolean movingUp;
	private boolean movingDown;
	private long lastCameraUpdateNs;

	public GameWindow(int width, int height, String title) {
		this.width = width;
		this.height = height;
		this.title = title;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setMouseHandler(MouseHandler mouseHandler) {
		this.mouseHandler = mouseHandler;
		updateWorldMousePosition();
	}

	public void setHud(Hud hud) {
		this.hud = hud;
	}

	public void setCamera(Camera2D camera) {
		this.camera = camera;
		lastCameraUpdateNs = System.nanoTime();
		updateWorldMousePosition();
	}

	public void setRightClickHandler(Runnable rightClickHandler) {
		this.rightClickHandler = Objects.requireNonNull(rightClickHandler);
	}

	public Point getMousePosition() {
		return new Point((int) Math.round(mouseX), (int) Math.round(mouseY));
	}

	private Point screenToWorld(double screenX, double screenY) {
		return camera == null
			? new Point((int) Math.round(screenX), (int) Math.round(screenY))
			: camera.screenToWorld(screenX, screenY, width, height);
	}

	private void updateWorldMousePosition() {
		if (mouseHandler != null) {
			Point worldPosition = screenToWorld(mouseX, mouseY);
			mouseHandler.onMove(worldPosition.x, worldPosition.y);
		}
	}

	private boolean updateCameraInput(int key, boolean pressed) {
		switch (key) {
			case GLFW_KEY_LEFT, GLFW_KEY_A -> movingLeft = pressed;
			case GLFW_KEY_RIGHT, GLFW_KEY_D -> movingRight = pressed;
			case GLFW_KEY_UP, GLFW_KEY_W -> movingUp = pressed;
			case GLFW_KEY_DOWN, GLFW_KEY_S -> movingDown = pressed;
			default -> {
				return false;
			}
		}
		camera.setMovementInput(
			(movingRight ? 1.0 : 0.0) - (movingLeft ? 1.0 : 0.0),
			(movingDown ? 1.0 : 0.0) - (movingUp ? 1.0 : 0.0)
		);
		return true;
	}

	/**
	 * Initialise la fenêtre GLFW et configure le contexte OpenGL.
	 */
	public void init() {
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		// Configuration de GLFW
		glfwDefaultWindowHints();
		// Configure la fenêtre GLFW avant de la créer
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		// Autoriser la fenêtre à être redimensionnable
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		// Crée la fenêtre GLFW avec les dimensions et le titre spécifiés
		window = glfwCreateWindow(width, height, title != null ? title : "Toto", NULL, NULL);
		if (window == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}

		// Configure les callbacks pour gérer les événements d'entrée
		glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(win, true);
				return;
			}
			if (camera == null) {
				return;
			}
			boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
			if (action != GLFW_RELEASE && !pressed) {
				return;
			}
			if (updateCameraInput(key, pressed)) {
				updateWorldMousePosition();
			}
		});

		// Configure le callback pour suivre la position de la souris
		glfwSetCursorPosCallback(window, (win, xpos, ypos) -> handleCursorPosition(xpos, ypos));

		glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
			if (camera != null) {
				camera.zoomAt(yoffset, mouseX, mouseY, width, height);
				updateWorldMousePosition();
			}
		});

		// Configure le callback pour gérer les boutons de la souris.
		glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
			if (button == GLFW_MOUSE_BUTTON_RIGHT) {
				if (action == GLFW_PRESS) {
					handleRightButtonPress();
				} else if (action == GLFW_RELEASE) {
					handleRightButtonRelease();
				}
				return;
			}
			if (mouseHandler == null) {
				return;
			}

			if (action == GLFW_PRESS) {
				if (
					button == GLFW_MOUSE_BUTTON_LEFT &&
					hud != null &&
					hud.handleClick(mouseX, mouseY, width, height)
				) {
					return;
				}

				if (button == GLFW_MOUSE_BUTTON_LEFT && hud != null && hud.getDialog() != null) {
					hud.handleOutsideClick();
					return;
				}
				Point worldPosition = screenToWorld(mouseX, mouseY);
				mouseHandler.onPress(button, worldPosition.x, worldPosition.y);
			} else if (action == GLFW_RELEASE) {
				Point worldPosition = screenToWorld(mouseX, mouseY);
				mouseHandler.onRelease(button, worldPosition.x, worldPosition.y);
			}
		});

		// Configure le contexte OpenGL pour la fenêtre GLFW
		glfwMakeContextCurrent(window);
		// Active le v-sync
		glfwSwapInterval(1);
		// Affiche la fenêtre GLFW
		glfwShowWindow(window);
	}

	void handleCursorPosition(double xpos, double ypos) {
		if (rightButtonPressed && camera != null) {
			boolean wasPanning = panningWithMouse;
			if (
				!panningWithMouse &&
				Math.hypot(xpos - rightButtonPressX, ypos - rightButtonPressY) >=
					RIGHT_DRAG_THRESHOLD_PIXELS
			) {
				panningWithMouse = true;
			}
			if (panningWithMouse) {
				double previousX = wasPanning ? lastPanMouseX : rightButtonPressX;
				double previousY = wasPanning ? lastPanMouseY : rightButtonPressY;
				camera.panByScreenPixels(previousX - xpos, previousY - ypos);
			}
		}
		mouseX = xpos;
		mouseY = ypos;
		lastPanMouseX = xpos;
		lastPanMouseY = ypos;
		updateWorldMousePosition();
	}

	void handleRightButtonPress() {
		handleRightButtonPress(System.nanoTime());
	}

	void handleRightButtonPress(long pressedAtNs) {
		rightButtonPressed = true;
		panningWithMouse = false;
		rightButtonPressX = mouseX;
		rightButtonPressY = mouseY;
		rightButtonPressTimeNs = pressedAtNs;
		lastPanMouseX = mouseX;
		lastPanMouseY = mouseY;
	}

	void handleRightButtonRelease() {
		handleRightButtonRelease(System.nanoTime());
	}

	void handleRightButtonRelease(long releasedAtNs) {
		if (!rightButtonPressed) {
			return;
		}
		long pressedDurationNs = releasedAtNs - rightButtonPressTimeNs;
		boolean wasClick =
			!panningWithMouse &&
			pressedDurationNs >= 0 &&
			pressedDurationNs <= RIGHT_CLICK_MAX_DURATION_NS;
		rightButtonPressed = false;
		panningWithMouse = false;
		if (wasClick) {
			rightClickHandler.run();
		}
	}

	public void update() {
		glfwPollEvents();
		if (camera != null) {
			long nowNs = System.nanoTime();
			double deltaSeconds = Math.min((nowNs - lastCameraUpdateNs) / 1_000_000_000.0, 0.1);
			lastCameraUpdateNs = nowNs;
			camera.updateMovement(deltaSeconds);
			updateWorldMousePosition();
		}
	}

	public void render() {
		glfwSwapBuffers(window);
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(window);
	}

	public void destroy() {
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
	}
}
