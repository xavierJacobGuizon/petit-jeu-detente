package org.jeuroute.gamecore.scene;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glVertex2f;

import java.awt.Rectangle;
import java.util.Objects;
import org.jeuroute.gamecore.rendering.world.WorldViewport;
import org.jeuroute.model.records.window.WindowMetrics;
import org.jeuroute.gamecore.rendering.text.CharUtils;

public final class MainMenuScene implements Scene {

	private static final int BUTTON_WIDTH = 280;
	private static final int BUTTON_HEIGHT = 58;

	private final Runnable startGame;
	private final Runnable quitGame;
	private final WorldViewport viewport = new WorldViewport();
	private WindowMetrics metrics = new WindowMetrics(1280, 720, 1280, 720);
	private double mouseX;
	private double mouseY;

	public MainMenuScene(Runnable startGame, Runnable quitGame) {
		this.startGame = Objects.requireNonNull(startGame);
		this.quitGame = Objects.requireNonNull(quitGame);
	}

	@Override
	public void update(long elapsedNanoseconds) {}

	@Override
	public void render(WindowMetrics metrics) {
		if (!metrics.hasArea()) {
			return;
		}
		this.metrics = metrics;
		viewport.applyIfChanged(metrics);
		glClearColor(0.035f, 0.10f, 0.12f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		int centerX = metrics.width() / 2;
		int centerY = metrics.height() / 2;
		drawBackdrop(centerX, centerY);
		CharUtils.drawText("ROUTE RUNNER", centerX - 96, centerY - 132, 0.92f, 0.96f, 0.88f);
		CharUtils.drawText("BUILD YOUR NETWORK", centerX - 144, centerY - 78, 0.36f, 0.82f, 0.68f);
		drawButton(startBounds(), "START GAME", isHovered(startBounds()));
		drawButton(quitBounds(), "QUIT", isHovered(quitBounds()));
	}

	@Override
	public void handleKey(int key, int action) {
		if (action != GLFW_RELEASE) {
			return;
		}
		if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_SPACE) {
			startGame.run();
		} else if (key == GLFW_KEY_ESCAPE) {
			quitGame.run();
		}
	}

	@Override
	public void handleCursorPosition(double mouseX, double mouseY) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
	}

	@Override
	public boolean handleMouseButton(int button, int action) {
		if (button != GLFW_MOUSE_BUTTON_LEFT || action != GLFW_PRESS) {
			return false;
		}
		if (startBounds().contains(mouseX, mouseY)) {
			startGame.run();
			return true;
		}
		if (quitBounds().contains(mouseX, mouseY)) {
			quitGame.run();
			return true;
		}
		return false;
	}

	@Override
	public void onWindowMetricsChanged(WindowMetrics metrics) {
		this.metrics = metrics;
	}

	private void drawBackdrop(int centerX, int centerY) {
		glColor3f(0.12f, 0.31f, 0.29f);
		glBegin(GL_LINES);
		glVertex2f(centerX - 360, centerY + 8);
		glVertex2f(centerX - 190, centerY + 8);
		glVertex2f(centerX + 190, centerY + 8);
		glVertex2f(centerX + 360, centerY + 8);
		glVertex2f(centerX, centerY - 30);
		glVertex2f(centerX - 110, centerY + 8);
		glVertex2f(centerX, centerY - 30);
		glVertex2f(centerX + 110, centerY + 8);
		glEnd();
	}

	private void drawButton(Rectangle bounds, String label, boolean hovered) {
		glColor3f(hovered ? 0.20f : 0.10f, hovered ? 0.58f : 0.28f, hovered ? 0.48f : 0.29f);
		glBegin(GL_QUADS);
		glVertex2f(bounds.x, bounds.y);
		glVertex2f(bounds.x + bounds.width, bounds.y);
		glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
		glVertex2f(bounds.x, bounds.y + bounds.height);
		glEnd();
		CharUtils.drawText(
			label,
			bounds.x + (bounds.width - label.length() * 16) / 2,
			bounds.y + 21,
			0.95f,
			0.97f,
			0.91f
		);
	}

	private Rectangle startBounds() {
		return new Rectangle(
			metrics.width() / 2 - BUTTON_WIDTH / 2,
			metrics.height() / 2 + 34,
			BUTTON_WIDTH,
			BUTTON_HEIGHT
		);
	}

	private Rectangle quitBounds() {
		return new Rectangle(
			metrics.width() / 2 - BUTTON_WIDTH / 2,
			metrics.height() / 2 + 108,
			BUTTON_WIDTH,
			BUTTON_HEIGHT
		);
	}

	private boolean isHovered(Rectangle bounds) {
		return bounds.contains(mouseX, mouseY);
	}
}
