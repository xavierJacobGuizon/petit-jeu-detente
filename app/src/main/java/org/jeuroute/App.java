package org.jeuroute;

import static org.lwjgl.opengl.GL11.*;

import org.jeuroute.gamecore.GameLoop;
import org.jeuroute.gamecore.GameWindow;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.hud.presentation.HudRenderer;
import org.jeuroute.manager.GameManager;
import org.lwjgl.opengl.GL;

public class App {

	private final GameWindow window = new GameWindow(1280, 720, "Route Runner");
	private final Camera2D camera = new Camera2D(640.0, 360.0);
	private final GameLoop loop = new GameLoop();
	private final GameManager gameManager = new GameManager();

	public App() {
		window.setCamera(camera);
		window.setMouseHandler(gameManager.getMouseHandlerManager().getMouseHandler());
		window.setHud(gameManager.getHud());
		window.setRightClickHandler(gameManager::cancelActiveAction);
	}

	public void run() {
		window.init();
		try {
			GL.createCapabilities();
			configure2DView();
			loop.start(window::shouldClose, window::update, this::update, this::render);
		} finally {
			window.destroy();
		}
	}

	/**
	 * Configures the OpenGL context for 2D rendering, setting up an orthographic
	 * projection that matches the window dimensions.
	 */
	private void configure2DView() {
		glViewport(0, 0, window.getWidth(), window.getHeight());
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
	}

	/**
	 * Updates the game state, including vehicle positions and any other dynamic
	 * elements, based on the elapsed time since the last update.
	 *
	 * @param deltaSeconds
	 */
	private void update(double deltaSeconds) {
		gameManager.update(deltaSeconds);
	}

	/**
	 * Renders the current game state to the window, including roads, intersections,
	 * vehicles, and any drag lines created by the mouse handler.
	 */
	private void render() {
		glClearColor(0.07f, 0.09f, 0.12f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
		camera.apply(window.getWidth(), window.getHeight());

		WorldRenderer.renderRoads(gameManager.getRoadGraph().getRoads(), camera.getZoom());
		WorldRenderer.renderResourceBuildings(
			gameManager.getWorldMap().getResourceBuildingManager().getBuildings(),
			camera.getZoom()
		);
		WorldRenderer.renderLines(gameManager.getLineManager().getLines(), camera.getZoom());
		WorldRenderer.renderIntersections(gameManager.getFixedEntityManager().getIntersections());
		WorldRenderer.renderStations(
			gameManager.getFixedEntityManager().getStations(),
			camera.getZoom()
		);
		WorldRenderer.renderDepots(gameManager.getDepots(), camera.getZoom());
		WorldRenderer.renderVehicles(gameManager.getVehicleManager().getVehicles());
		gameManager.renderDragLine(camera.getZoom());
		glPopMatrix();
		glLoadIdentity();

		HudRenderer.render(gameManager.getHud(), window.getWidth(), window.getHeight());

		window.render();
	}

	public static void main(String[] args) {
		new App().run();
	}
}
