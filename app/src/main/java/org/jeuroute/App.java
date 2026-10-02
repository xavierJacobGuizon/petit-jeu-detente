package org.jeuroute;

import static org.lwjgl.opengl.GL11.*;

import org.jeuroute.gamecore.GameLoop;
import org.jeuroute.gamecore.GameWindow;
import org.jeuroute.gamecore.WorldPreviewRenderer;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.hud.presentation.HudRenderer;
import org.jeuroute.manager.GameManager;
import org.jeuroute.model.records.world.WorldRenderData;
import org.lwjgl.opengl.GL;

public class App {

	private final GameWindow window = new GameWindow(1280, 720, "Route Runner");
	private final Camera2D camera = new Camera2D(640.0, 360.0);
	private final GameLoop loop = new GameLoop();
	private final GameManager gameManager = new GameManager();
	private final WorldRenderData worldRenderData = new WorldRenderData(
		gameManager.getWorldMap().getTerrainMap(),
		gameManager.getRoadGraph().getRoads(),
		gameManager.getWorldMap().getResourceBuildingManager().getBuildings(),
		gameManager.getWorldMap().getHouseManager().getHouses(),
		gameManager.getLineManager().getLines(),
		gameManager.getFixedEntityManager().getIntersections(),
		gameManager.getFixedEntityManager().getStations(),
		gameManager.getDepots(),
		gameManager.getVehicleManager().getVehicles(),
		gameManager.getPersonManager().getPeople()
	);

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
		glClearColor(0.035f, 0.16f, 0.23f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
		camera.apply(window.getWidth(), window.getHeight());

		WorldRenderer.renderWorld(worldRenderData, camera.getZoom());
		WorldPreviewRenderer.renderWorldPreviews(
			gameManager.getWorldPreviewData(),
			camera.getZoom()
		);
		glPopMatrix();
		glLoadIdentity();

		HudRenderer.render(gameManager.getHud(), window.getWidth(), window.getHeight());

		window.render();
	}

	public static void main(String[] args) {
		new App().run();
	}
}
