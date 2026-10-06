package org.jeuroute;

import org.jeuroute.gamecore.loop.GameLoop;
import org.jeuroute.gamecore.window.GameWindow;
import org.jeuroute.gamecore.scene.GameScene;
import org.jeuroute.gamecore.scene.MainMenuScene;
import org.jeuroute.gamecore.scene.SceneService;
import org.jeuroute.model.records.window.WindowMetrics;
import org.lwjgl.opengl.GL;

public class App {

	private final GameWindow window = new GameWindow(1280, 720, "Route Runner");
	private final GameLoop loop = new GameLoop();
	private final SceneService sceneService = new SceneService();

	public void run() {
		window.init();
		try {
			GL.createCapabilities();
			window.setInputController(sceneService);
			sceneService.start(this::createMainMenuScene);
			loop.start(
				window::shouldClose,
				window::update,
				sceneService::updateScene,
				this::render,
				sceneService::onFramesPerSecondUpdated
			);
		} finally {
			sceneService.close();
			window.destroy();
		}
	}

	private MainMenuScene createMainMenuScene() {
		return new MainMenuScene(
			() -> sceneService.request(this::createGameScene),
			window::requestClose
		);
	}

	private GameScene createGameScene() {
		return new GameScene(window, () -> sceneService.request(this::createMainMenuScene));
	}

	private void render() {
		long frameStartedAtNanos = System.nanoTime();
		WindowMetrics metrics = window.getMetrics();

		// Rend la scène actuelle si la fenêtre a une zone d'affichage définie
		if (metrics.hasArea()) {
			sceneService.render(metrics);
		}

		window.render();
		sceneService.onFramePresented(frameStartedAtNanos);
	}

	public static void main(String[] args) {
		new App().run();
	}
}
