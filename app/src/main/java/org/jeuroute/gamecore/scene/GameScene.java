package org.jeuroute.gamecore.scene;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

import java.util.Objects;
import org.jeuroute.gamecore.window.GameWindow;
import org.jeuroute.gamecore.rendering.world.WorldViewport;
import org.jeuroute.model.records.camera.WorldViewBounds;
import org.jeuroute.gamecore.controllers.GameInputController;
import org.jeuroute.gamecore.hud.HudWindowService;
import org.jeuroute.gamecore.hud.HudWindowSpec;
import org.jeuroute.gamecore.hud.presentation.DebugProfilerWindowContent;
import org.jeuroute.gamecore.hud.presentation.HudRenderer;
import org.jeuroute.gamecore.performance.PerformanceProfiler;
import org.jeuroute.gamecore.rendering.world.PersonMeshRenderer;
import org.jeuroute.gamecore.rendering.preview.WorldPreviewRenderer;
import org.jeuroute.gamecore.rendering.world.WorldRenderer;
import org.jeuroute.gamecore.rendering.debug.WorldDebugOverlay;
import org.jeuroute.manager.GameManager;
import org.jeuroute.model.records.window.WindowMetrics;
import org.jeuroute.model.records.world.WorldRenderData;
import org.lwjgl.opengl.GL;

public final class GameScene implements Scene {

	private final GameWindow window;
	private final Runnable returnToMenu;
	private final GameManager gameManager = new GameManager();
	private final HudWindowService hudWindowService = new HudWindowService();
	private final WorldViewport worldViewport = new WorldViewport();
	private final WorldRenderData worldRenderData;
	private final GameInputController inputController;
	private WorldDebugOverlay worldDebugOverlay;
	private PersonMeshRenderer personMeshRenderer;

	public GameScene(GameWindow window, Runnable returnToMenu) {
		this.window = Objects.requireNonNull(window);
		this.returnToMenu = Objects.requireNonNull(returnToMenu);
		worldRenderData = new WorldRenderData(
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

		inputController = new GameInputController(
			window.getCamera(),
			gameManager.getMouseHandlerManager().getMouseHandler(),
			gameManager.getHud(),
			hudWindowService,
			gameManager::cancelActiveAction,
			window.getMetrics()
		);

		gameManager.setDebugWindowCreationHandler(this::createProfilerWindow);
		gameManager.setPerformanceSampleHandler(
			gameManager.getPerformanceService().getPerformanceCsvRecorder()::recordAtTick
		);
	}

	@Override
	public void enter() {
		worldDebugOverlay = new WorldDebugOverlay(
			gameManager.getPerformanceService().getPerformanceProfiler()
		);

		if (GL.getCapabilities().OpenGL33) {
			personMeshRenderer = new PersonMeshRenderer();
		} else {
			System.err.println(
				"OpenGL 3.3 unavailable; using compatibility renderers for people and debug."
			);
		}

		worldViewport.applyIfChanged(window.getMetrics());
	}

	@Override
	public void update(long elapsedNanoseconds) {
		long updateStartedAtNanos = System.nanoTime();
		try {
			gameManager.advanceFrame(elapsedNanoseconds);
		} finally {
			gameManager
				.getPerformanceService()
				.getPerformanceProfiler()
				.record(
					PerformanceProfiler.Section.UPDATE,
					System.nanoTime() - updateStartedAtNanos
				);
		}
	}

	@Override
	public void render(WindowMetrics metrics) {
		if (!metrics.hasArea()) {
			return;
		}

		worldViewport.applyIfChanged(metrics);
		glClearColor(0.035f, 0.16f, 0.23f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glPushMatrix();

		window.getCamera().apply(metrics.width(), metrics.height());
		WorldViewBounds viewBounds = window
			.getCamera()
			.getVisibleWorldBounds(metrics.width(), metrics.height());

		long worldRenderStartedAtNanos = System.nanoTime();
		WorldRenderer.renderWorld(
			worldRenderData,
			window.getCamera().getZoom(),
			viewBounds,
			personMeshRenderer,
			gameManager.getPerformanceService().getPerformanceProfiler()
		);

		gameManager
			.getPerformanceService()
			.getPerformanceProfiler()
			.record(
				PerformanceProfiler.Section.WORLD_RENDER,
				System.nanoTime() - worldRenderStartedAtNanos
			);

		WorldPreviewRenderer.renderWorldPreviews(
			gameManager.getWorldPreviewData(),
			window.getCamera().getZoom(),
			viewBounds
		);

		worldDebugOverlay.render(
			gameManager.isPersonRouteDisplayEnabled(),
			worldRenderData,
			gameManager.getPersonManager().getWalkingPeople(),
			window.getCamera().getZoom(),
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

		hudWindowService
			.getHudWindowRenderer()
			.render(
				hudWindowService,
				metrics.width(),
				metrics.height(),
				metrics.framebufferWidth(),
				metrics.framebufferHeight()
			);

		gameManager
			.getPerformanceService()
			.getPerformanceProfiler()
			.record(
				PerformanceProfiler.Section.HUD_RENDER,
				System.nanoTime() - hudRenderStartedAtNanos
			);
	}

	@Override
	public void onFramesPerSecondUpdated(int framesPerSecond) {
		gameManager.setFramesPerSecond(framesPerSecond);
	}

	@Override
	public void onFramePresented(long frameStartedAtNanos) {
		gameManager
			.getPerformanceService()
			.getPerformanceProfiler()
			.record(PerformanceProfiler.Section.FRAME, System.nanoTime() - frameStartedAtNanos);
	}

	@Override
	public void onWindowMetricsChanged(WindowMetrics metrics) {
		inputController.onWindowMetricsChanged(metrics);
	}

	@Override
	public void handleKey(int key, int action) {
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
			gameManager.cancelActiveAction();
			returnToMenu.run();
			return;
		}
		inputController.handleKey(key, action);
	}

	@Override
	public void handleCursorPosition(double mouseX, double mouseY) {
		inputController.handleCursorPosition(mouseX, mouseY);
	}

	@Override
	public void handleScroll(double scrollAmount) {
		inputController.handleScroll(scrollAmount);
	}

	@Override
	public boolean handleMouseButton(int button, int action) {
		return inputController.handleMouseButton(button, action);
	}

	@Override
	public void update() {
		inputController.update();
	}

	@Override
	public void exit() {
		if (worldDebugOverlay != null) {
			worldDebugOverlay.close();
			worldDebugOverlay = null;
		}
		if (personMeshRenderer != null) {
			personMeshRenderer.close();
			personMeshRenderer = null;
		}
		gameManager.getPerformanceService().getPerformanceCsvRecorder().close();
	}

	private void createProfilerWindow() {
		hudWindowService.open(
			HudWindowSpec.standard(
				"DEBUG",
				24,
				156,
				1000,
				310,
				new DebugProfilerWindowContent(
					gameManager.getPerformanceService().getPerformanceProfiler(),
					() -> gameManager.getMouseHandlerManager().getMouseHandler().getMousePosition(),
					gameManager::isDebugModeEnabled,
					() -> gameManager.getActionHandlers().get("toggle-debug").run(),
					gameManager::isPersonRouteDisplayEnabled,
					() -> gameManager.getActionHandlers().get("toggle-person-routes").run(),
					gameManager.getPerformanceService().getPerformanceCsvRecorder(),
					gameManager::getCurrentTickNumber
				)
			)
		);
	}
}
