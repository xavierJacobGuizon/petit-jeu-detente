package org.jeuroute.gamecore.controllers;

import static org.lwjgl.glfw.GLFW.*;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.HudWindowService;
import org.jeuroute.model.records.window.WindowMetrics;

public final class GameInputController implements WindowInputController {

	private static final double RIGHT_DRAG_THRESHOLD_PIXELS = 5.0;
	private static final long RIGHT_CLICK_MAX_DURATION_NS = 2_000_000_000L;

	private final Camera2D camera;
	private final MouseHandler mouseHandler;
	private final Hud hud;
	private final HudWindowService hudWindowService;
	private final Runnable cancelActiveAction;
	private WindowMetrics metrics;
	private double mouseX;
	private double mouseY;
	private boolean panningWithMouse;
	private boolean rightButtonPressed;
	private double rightButtonPressX;
	private double rightButtonPressY;
	private long rightButtonPressTimeNs;
	private double lastPanMouseX;
	private double lastPanMouseY;
	private boolean movingLeft;
	private boolean movingRight;
	private boolean movingUp;
	private boolean movingDown;
	private long lastCameraUpdateNs;

	public GameInputController(
		Camera2D camera,
		MouseHandler mouseHandler,
		Hud hud,
		HudWindowService hudWindowService,
		Runnable cancelActiveAction,
		WindowMetrics metrics
	) {
		this.camera = Objects.requireNonNull(camera);
		this.mouseHandler = mouseHandler;
		this.hud = hud;
		this.hudWindowService = hudWindowService;
		this.cancelActiveAction = Objects.requireNonNull(cancelActiveAction);
		this.metrics = Objects.requireNonNull(metrics);
		lastCameraUpdateNs = System.nanoTime();
	}

	public Point getMousePosition() {
		return new Point((int) Math.round(mouseX), (int) Math.round(mouseY));
	}

	public void onWindowMetricsChanged(WindowMetrics metrics) {
		this.metrics = Objects.requireNonNull(metrics);
		updateWorldMousePosition();
	}

	public void handleKey(int key, int action) {
		boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
		if (action != GLFW_RELEASE && !pressed) {
			return;
		}
		if (updateCameraInput(key, pressed)) {
			updateWorldMousePosition();
		}
	}

	public void handleCursorPosition(double nextMouseX, double nextMouseY) {
		if (rightButtonPressed) {
			boolean wasPanning = panningWithMouse;
			if (
				!panningWithMouse &&
				Math.hypot(nextMouseX - rightButtonPressX, nextMouseY - rightButtonPressY) >=
					RIGHT_DRAG_THRESHOLD_PIXELS
			) {
				panningWithMouse = true;
			}
			if (panningWithMouse) {
				double previousX = wasPanning ? lastPanMouseX : rightButtonPressX;
				double previousY = wasPanning ? lastPanMouseY : rightButtonPressY;
				camera.panByScreenPixels(previousX - nextMouseX, previousY - nextMouseY);
			}
		}
		mouseX = nextMouseX;
		mouseY = nextMouseY;
		lastPanMouseX = nextMouseX;
		lastPanMouseY = nextMouseY;
		if (hudWindowService != null) {
			hudWindowService.handleMouseMoved(mouseX, mouseY, metrics.width(), metrics.height());
		}
		updateWorldMousePosition();
	}

	public void handleScroll(double scrollAmount) {
		if (
			hudWindowService == null || !hudWindowService.handleScroll(mouseX, mouseY, scrollAmount)
		) {
			camera.zoomAt(scrollAmount, mouseX, mouseY, metrics.width(), metrics.height());
		}
		updateWorldMousePosition();
	}

	public boolean handleMouseButton(int button, int action) {
		if (button == GLFW_MOUSE_BUTTON_RIGHT) {
			if (action == GLFW_PRESS) {
				handleRightButtonPress(System.nanoTime());
			} else if (action == GLFW_RELEASE) {
				handleRightButtonRelease(System.nanoTime());
			}
			return true;
		}

		if (action == GLFW_PRESS) {
			if (
				button == GLFW_MOUSE_BUTTON_LEFT &&
				hudWindowService != null &&
				hudWindowService.handleMousePressed(mouseX, mouseY)
			) {
				return true;
			}
			if (mouseHandler == null) {
				return false;
			}
			if (
				button == GLFW_MOUSE_BUTTON_LEFT &&
				hud != null &&
				hud.handleClick(mouseX, mouseY, metrics.width(), metrics.height())
			) {
				return true;
			}
			if (button == GLFW_MOUSE_BUTTON_LEFT && hud != null && hud.getDialog() != null) {
				hud.handleOutsideClick();
				return true;
			}
			Point worldPosition = screenToWorld(mouseX, mouseY);
			mouseHandler.onPress(button, worldPosition.x, worldPosition.y);
			return true;
		}

		if (action == GLFW_RELEASE) {
			if (
				button == GLFW_MOUSE_BUTTON_LEFT &&
				hudWindowService != null &&
				hudWindowService.handleMouseReleased()
			) {
				return true;
			}
			if (mouseHandler == null) {
				return false;
			}
			Point worldPosition = screenToWorld(mouseX, mouseY);
			mouseHandler.onRelease(button, worldPosition.x, worldPosition.y);
			return true;
		}
		return false;
	}

	public void update() {
		long nowNs = System.nanoTime();
		double deltaSeconds = Math.min((nowNs - lastCameraUpdateNs) / 1_000_000_000.0, 0.1);
		lastCameraUpdateNs = nowNs;
		camera.updateMovement(deltaSeconds);
		updateWorldMousePosition();
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
			cancelActiveAction.run();
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

	private Point screenToWorld(double screenX, double screenY) {
		return camera.screenToWorld(screenX, screenY, metrics.width(), metrics.height());
	}

	private void updateWorldMousePosition() {
		if (mouseHandler != null) {
			Point worldPosition = screenToWorld(mouseX, mouseY);
			mouseHandler.onMove(worldPosition.x, worldPosition.y);
		}
	}
}
