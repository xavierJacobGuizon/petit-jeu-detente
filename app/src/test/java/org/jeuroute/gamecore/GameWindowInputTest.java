package org.jeuroute.gamecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import org.jeuroute.gamecore.camera.Camera2D;
import org.junit.jupiter.api.Test;

class GameWindowInputTest {

	@Test
	void rightClickCancelsWithoutPanning() {
		GameWindow window = new GameWindow(1280, 720, "test");
		Camera2D camera = new Camera2D(640.0, 360.0);
		AtomicInteger cancellationCount = new AtomicInteger();
		window.setCamera(camera);
		window.setRightClickHandler(cancellationCount::incrementAndGet);
		window.handleCursorPosition(100, 100);
		Point worldCenterBeforeClick = camera.screenToWorld(640, 360, 1280, 720);

		window.handleRightButtonPress();
		window.handleCursorPosition(103, 103);
		window.handleRightButtonRelease();

		assertEquals(1, cancellationCount.get());
		assertEquals(worldCenterBeforeClick, camera.screenToWorld(640, 360, 1280, 720));
	}

	@Test
	void rightDragPansWithoutCancelling() {
		GameWindow window = new GameWindow(1280, 720, "test");
		Camera2D camera = new Camera2D(640.0, 360.0);
		AtomicInteger cancellationCount = new AtomicInteger();
		window.setCamera(camera);
		window.setRightClickHandler(cancellationCount::incrementAndGet);
		window.handleCursorPosition(100, 100);
		Point worldCenterBeforeDrag = camera.screenToWorld(640, 360, 1280, 720);

		window.handleRightButtonPress(1_000_000_000L);
		window.handleCursorPosition(106, 100);
		window.handleCursorPosition(116, 100);
		window.handleRightButtonRelease(4_000_000_000L);

		assertEquals(0, cancellationCount.get());
		assertTrue(camera.screenToWorld(640, 360, 1280, 720).x < worldCenterBeforeDrag.x);
	}

	@Test
	void rightPressLongerThanTwoSecondsDoesNotCancel() {
		GameWindow window = new GameWindow(1280, 720, "test");
		AtomicInteger cancellationCount = new AtomicInteger();
		window.setRightClickHandler(cancellationCount::incrementAndGet);
		window.handleRightButtonPress(1_000_000_000L);
		window.handleRightButtonRelease(3_000_000_001L);

		assertEquals(0, cancellationCount.get());
	}

	@Test
	void rightPressOfExactlyTwoSecondsStillCountsAsClick() {
		GameWindow window = new GameWindow(1280, 720, "test");
		AtomicInteger cancellationCount = new AtomicInteger();
		window.setRightClickHandler(cancellationCount::incrementAndGet);
		window.handleRightButtonPress(1_000_000_000L);
		window.handleRightButtonRelease(3_000_000_000L);

		assertEquals(1, cancellationCount.get());
	}

	@Test
	void cancellingMouseActionClearsModeAndPendingPlacement() {
		MouseHandler mouseHandler = new MouseHandler();
		mouseHandler.setStationCreationEnabled(true);
		mouseHandler.onStationPlacement(100, 200);

		mouseHandler.cancelCurrentAction();

		assertFalse(mouseHandler.isStationCreationEnabled());
		assertNull(mouseHandler.consumeStationPlacement());
		assertNull(mouseHandler.getPlacementPreviewType());
	}
}
