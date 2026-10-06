package org.jeuroute.gamecore.scene;

import java.util.Objects;
import java.util.function.Supplier;
import org.jeuroute.gamecore.controllers.WindowInputController;
import org.jeuroute.model.records.window.WindowMetrics;

public final class SceneService implements WindowInputController, AutoCloseable {

	private Scene activeScene;
	private Supplier<? extends Scene> pendingScene;

	public void start(Supplier<? extends Scene> initialScene) {
		if (activeScene != null) {
			throw new IllegalStateException("A scene is already active");
		}
		activate(Objects.requireNonNull(initialScene).get());
	}

	public void request(Supplier<? extends Scene> nextScene) {
		pendingScene = Objects.requireNonNull(nextScene);
	}

	public void updateScene(long elapsedNanoseconds) {
		if (pendingScene != null) {
			Supplier<? extends Scene> requestedScene = pendingScene;
			pendingScene = null;
			Scene nextScene = Objects.requireNonNull(requestedScene.get());
			if (activeScene != null) {
				activeScene.exit();
			}
			activate(nextScene);
		}
		if (activeScene != null) {
			activeScene.update(elapsedNanoseconds);
		}
	}

	public void render(WindowMetrics metrics) {
		if (activeScene == null) {
			return;
		}
		activeScene.render(metrics);
	}

	public void onFramesPerSecondUpdated(int framesPerSecond) {
		if (activeScene == null) {
			return;
		}
		activeScene.onFramesPerSecondUpdated(framesPerSecond);
	}

	public void onFramePresented(long frameStartedAtNanos) {
		if (activeScene == null) {
			return;
		}
		activeScene.onFramePresented(frameStartedAtNanos);
	}

	@Override
	public void onWindowMetricsChanged(WindowMetrics metrics) {
		if (activeScene == null) {
			return;
		}
		activeScene.onWindowMetricsChanged(metrics);
	}

	@Override
	public void handleKey(int key, int action) {
		if (activeScene == null) {
			return;
		}
		activeScene.handleKey(key, action);
	}

	@Override
	public void handleCursorPosition(double mouseX, double mouseY) {
		if (activeScene == null) {
			return;
		}
		activeScene.handleCursorPosition(mouseX, mouseY);
	}

	@Override
	public void handleScroll(double scrollAmount) {
		if (activeScene == null) {
			return;
		}
		activeScene.handleScroll(scrollAmount);
	}

	@Override
	public boolean handleMouseButton(int button, int action) {
		return activeScene != null && activeScene.handleMouseButton(button, action);
	}

	@Override
	public void update() {
		if (activeScene == null) {
			return;
		}
		activeScene.update();
	}

	@Override
	public void close() {
		if (activeScene == null) {
			pendingScene = null;
			return;
		}
		activeScene.exit();
		activeScene = null;
	}

	private void activate(Scene scene) {
		activeScene = scene;
		activeScene.enter();
	}
}
