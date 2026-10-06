package org.jeuroute.gamecore.scene;

import org.jeuroute.gamecore.controllers.WindowInputController;
import org.jeuroute.model.records.window.WindowMetrics;

public interface Scene extends WindowInputController {
	default void enter() {}

	void update(long elapsedNanoseconds);

	void render(WindowMetrics metrics);

	default void onFramesPerSecondUpdated(int framesPerSecond) {}

	default void onFramePresented(long frameStartedAtNanos) {}

	default void exit() {}

	@Override
	default void onWindowMetricsChanged(WindowMetrics metrics) {}

	@Override
	default void handleKey(int key, int action) {}

	@Override
	default void handleCursorPosition(double mouseX, double mouseY) {}

	@Override
	default void handleScroll(double scrollAmount) {}

	@Override
	default boolean handleMouseButton(int button, int action) {
		return false;
	}

	@Override
	default void update() {}
}
