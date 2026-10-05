package org.jeuroute;

import static org.lwjgl.opengl.GL11.*;

import org.jeuroute.gamecore.GameLoop;
import org.jeuroute.gamecore.GameWindow;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.gamecore.PersonMeshRenderer;
import org.jeuroute.gamecore.WindowMetrics;
import org.jeuroute.gamecore.WorldDebugOverlay;
import org.jeuroute.gamecore.WorldPreviewRenderer;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.WorldViewport;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.gamecore.controllers.GameInputController;
import org.jeuroute.gamecore.hud.HudWindowManager;
import org.jeuroute.gamecore.hud.HudWindowSpec;
import org.jeuroute.gamecore.hud.presentation.DebugProfilerWindowContent;
import org.jeuroute.gamecore.hud.presentation.HudRenderer;
import org.jeuroute.manager.GameManager;
import org.jeuroute.model.records.world.WorldRenderData;
import org.lwjgl.opengl.GL;

public class App {

	private final GameWindow window = new GameWindow(1280, 720, "Route Runner");
	private final Camera2D camera = new Camera2D(640.0, 360.0);
	private final GameLoop loop = new GameLoop();

	private final HudWindowManager hudWindowManager = new HudWindowManager();
	private final GameManager gameManager = new GameManager();

	private final WorldViewport worldViewport = new WorldViewport();
	private WorldDebugOverlay worldDebugOverlay;
	private long frameStartedAtNanos;
	private PersonMeshRenderer personMeshRenderer;

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
		window.setInputController(
			new GameInputController(
				camera,
				gameManager.getMouseHandlerManager().getMouseHandler(),
				gameManager.getHud(),
				hudWindowManager,
				gameManager::cancelActiveAction,
				window.getMetrics()
			)
		);
		gameManager.setDebugWindowCreationHandler(this::createProfilerWindow);
		gameManager.setPerformanceSampleHandler(
			this.gameManager.getPerformanceManager().getPerformanceCsvRecorder()::recordAtTick
		);
	}

	private void createProfilerWindow() {
		hudWindowManager.open(
			HudWindowSpec.standard(
				"DEBUG",
				24,
				156,
				1000,
				310,
				new DebugProfilerWindowContent(
					gameManager.getPerformanceManager().getPerformanceProfiler(),
					() -> gameManager.getMouseHandlerManager().getMouseHandler().getMousePosition(),
					gameManager::isDebugModeEnabled,
					() -> gameManager.getActionHandlers().get("toggle-debug").run(),
					gameManager::isPersonRouteDisplayEnabled,
					() -> gameManager.getActionHandlers().get("toggle-person-routes").run(),
					this.gameManager.getPerformanceManager().getPerformanceCsvRecorder(),
					gameManager::getCurrentTickNumber
				)
			)
		);
	}

	public void run() {
		window.init();
		try {
			GL.createCapabilities();
			worldDebugOverlay = new WorldDebugOverlay(
				gameManager.getPerformanceManager().getPerformanceProfiler()
			);
			if (GL.getCapabilities().OpenGL33) {
				personMeshRenderer = new PersonMeshRenderer();
			} else {
				System.err.println(
					"OpenGL 3.3 unavailable; using compatibility renderers for people and debug."
				);
			}
			worldViewport.applyIfChanged(window.getMetrics());
			loop.start(
				window::shouldClose,
				window::update,
				this::update,
				this::render,
				gameManager::setFramesPerSecond
			);
		} finally {
			if (worldDebugOverlay != null) {
				worldDebugOverlay.close();
			}
			if (personMeshRenderer != null) {
				personMeshRenderer.close();
			}
			this.gameManager.getPerformanceManager().getPerformanceCsvRecorder().close();
			window.destroy();
		}
	}

	/**
	 * Updates the game state, including vehicle positions and any other dynamic
	 * elements, based on the elapsed time since the last update.
	 *
	 * @param deltaSeconds
	 */
	private void update(long elapsedNanoseconds) {
		frameStartedAtNanos = System.nanoTime();
		long updateStartedAtNanos = frameStartedAtNanos;
		try {
			gameManager.advanceFrame(elapsedNanoseconds);
		} finally {
			gameManager
				.getPerformanceManager()
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
		WindowMetrics metrics = window.getMetrics();
		if (!metrics.hasArea()) {
			return;
		}
		worldViewport.applyIfChanged(metrics);
		glClearColor(0.035f, 0.16f, 0.23f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();
		camera.apply(metrics.width(), metrics.height());
		WorldViewBounds viewBounds = camera.getVisibleWorldBounds(
			metrics.width(),
			metrics.height()
		);
		long worldRenderStartedAtNanos = System.nanoTime();
		WorldRenderer.renderWorld(
			worldRenderData,
			camera.getZoom(),
			viewBounds,
			personMeshRenderer,
			gameManager.getPerformanceManager().getPerformanceProfiler()
		);
		gameManager
			.getPerformanceManager()
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
		worldDebugOverlay.render(
			gameManager.isPersonRouteDisplayEnabled(),
			worldRenderData,
			camera.getZoom(),
			viewBounds,
			gameManager.getCurrentTickNumber()
		);
		glPopMatrix();
		glLoadIdentity();

		long hudRenderStartedAtNanos = System.nanoTime();
		HudRenderer.render(
			gameManager.getHud(),
			metrics.width(),
			metrics.height(),
			gameManager.getDebugMousePosition()
		);
		hudWindowManager
			.getHudWindowRenderer()
			.render(
				hudWindowManager,
				metrics.width(),
				metrics.height(),
				metrics.framebufferWidth(),
				metrics.framebufferHeight()
			);
		PerformanceProfiler performanceProfiler = gameManager
			.getPerformanceManager()
			.getPerformanceProfiler();
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
