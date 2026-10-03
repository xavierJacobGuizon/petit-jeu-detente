package org.jeuroute;

import static org.lwjgl.opengl.GL11.*;

import org.jeuroute.gamecore.GameLoop;
import org.jeuroute.gamecore.GameWindow;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.gamecore.PersonMeshRenderer;
import org.jeuroute.gamecore.WorldDebugMeshRenderer;
import org.jeuroute.gamecore.WorldDebugRenderer;
import org.jeuroute.gamecore.WorldPreviewRenderer;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.gamecore.hud.presentation.HudRenderer;
import org.jeuroute.gamecore.hud.presentation.PerformanceProfilerRenderer;
import org.jeuroute.manager.GameManager;
import org.jeuroute.model.records.world.WorldRenderData;
import org.lwjgl.opengl.GL;

public class App {

	private static final long DEBUG_REFRESH_TICKS = 60;
	private static final long DEBUG_VIEW_REFRESH_TICKS = 30;

	private final GameWindow window = new GameWindow(1280, 720, "Route Runner");
	private final Camera2D camera = new Camera2D(640.0, 360.0);
	private final GameLoop loop = new GameLoop();
	private final GameManager gameManager = new GameManager();
	private final PerformanceProfilerRenderer performanceProfilerRenderer =
		new PerformanceProfilerRenderer();
	private long frameStartedAtNanos;
	private PersonMeshRenderer personMeshRenderer;
	private WorldDebugMeshRenderer worldDebugMeshRenderer;
	private long lastDebugRefreshTick = -1;
	private WorldViewBounds lastDebugViewBounds;
	private boolean debugDataReady;
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
			if (GL.getCapabilities().OpenGL33) {
				personMeshRenderer = new PersonMeshRenderer();
				worldDebugMeshRenderer = new WorldDebugMeshRenderer();
			} else {
				System.err.println(
					"OpenGL 3.3 unavailable; using compatibility renderers for people and debug."
				);
			}
			configure2DView();
			loop.start(
				window::shouldClose,
				window::update,
				this::update,
				this::render,
				gameManager::setFramesPerSecond
			);
		} finally {
			if (worldDebugMeshRenderer != null) {
				worldDebugMeshRenderer.close();
			}
			if (personMeshRenderer != null) {
				personMeshRenderer.close();
			}
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
		frameStartedAtNanos = System.nanoTime();
		long updateStartedAtNanos = frameStartedAtNanos;
		try {
			gameManager.update(deltaSeconds);
		} finally {
			gameManager
				.getPerformanceProfiler()
				.record(
					PerformanceProfiler.Section.UPDATE,
					System.nanoTime() - updateStartedAtNanos
				);
		}
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
		WorldViewBounds viewBounds = camera.getVisibleWorldBounds(
			window.getWidth(),
			window.getHeight()
		);
		boolean debugModeEnabled = gameManager.isDebugModeEnabled();
		if (!debugModeEnabled) {
			debugDataReady = false;
			lastDebugRefreshTick = -1;
			lastDebugViewBounds = null;
		} else if (worldDebugMeshRenderer != null) {
			long currentTick = gameManager.getCurrentTickNumber();
			boolean refreshByTick =
				!debugDataReady || currentTick - lastDebugRefreshTick >= DEBUG_REFRESH_TICKS;
			boolean refreshByView =
				!viewBounds.equals(lastDebugViewBounds) &&
				currentTick - lastDebugRefreshTick >= DEBUG_VIEW_REFRESH_TICKS;
			if (refreshByTick || refreshByView) {
				long debugPrepareStartedAtNanos = System.nanoTime();
				worldDebugMeshRenderer.updateDestinations(
					worldRenderData,
					viewBounds,
					camera.getZoom()
				);
				gameManager
					.getPerformanceProfiler()
					.record(
						PerformanceProfiler.Section.DEBUG_PREPARE,
						System.nanoTime() - debugPrepareStartedAtNanos
					);
				lastDebugRefreshTick = currentTick;
				lastDebugViewBounds = viewBounds;
				debugDataReady = true;
			}
		}

		long worldRenderStartedAtNanos = System.nanoTime();
		WorldRenderer.renderWorld(
			worldRenderData,
			camera.getZoom(),
			viewBounds,
			personMeshRenderer,
			gameManager.getPerformanceProfiler()
		);
		gameManager
			.getPerformanceProfiler()
			.record(
				PerformanceProfiler.Section.WORLD_RENDER,
				System.nanoTime() - worldRenderStartedAtNanos
			);
		WorldPreviewRenderer.renderWorldPreviews(
			gameManager.getWorldPreviewData(),
			camera.getZoom(),
			viewBounds
		);
		if (gameManager.isDebugModeEnabled()) {
			long debugRenderStartedAtNanos = System.nanoTime();
			if (worldDebugMeshRenderer == null) {
				WorldDebugRenderer.renderDestinations(
					worldRenderData,
					camera.getZoom(),
					viewBounds
				);
			} else {
				worldDebugMeshRenderer.renderDestinations(camera.getZoom());
			}
			gameManager
				.getPerformanceProfiler()
				.record(
					PerformanceProfiler.Section.DEBUG_RENDER,
					System.nanoTime() - debugRenderStartedAtNanos
				);
		}
		glPopMatrix();
		glLoadIdentity();

		long hudRenderStartedAtNanos = System.nanoTime();
		HudRenderer.render(
			gameManager.getHud(),
			window.getWidth(),
			window.getHeight(),
			gameManager.getDebugMousePosition()
		);
		if (debugModeEnabled) {
			performanceProfilerRenderer.render(
				gameManager.getPerformanceProfiler(),
				window.getWidth()
			);
		}
		PerformanceProfiler performanceProfiler = gameManager.getPerformanceProfiler();
		performanceProfiler.record(
			PerformanceProfiler.Section.HUD_RENDER,
			System.nanoTime() - hudRenderStartedAtNanos
		);
		window.render();
		performanceProfiler.record(
			PerformanceProfiler.Section.FRAME,
			System.nanoTime() - frameStartedAtNanos
		);
	}

	public static void main(String[] args) {
		new App().run();
	}
}
