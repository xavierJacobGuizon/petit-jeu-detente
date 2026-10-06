package org.jeuroute.gamecore.controllers;

import org.jeuroute.model.records.window.WindowMetrics;

public interface WindowInputController {
	void onWindowMetricsChanged(WindowMetrics metrics);

	void handleKey(int key, int action);

	void handleCursorPosition(double mouseX, double mouseY);

	void handleScroll(double scrollAmount);

	boolean handleMouseButton(int button, int action);

	void update();
}
