package org.jeuroute.gamecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.controllers.GameInputController;
import org.junit.jupiter.api.Test;

class GameInputControllerTest {

	private static final WindowMetrics METRICS = new WindowMetrics(1280, 720, 1280, 720);

	@Test
	void rightClickCancelsWithoutPanning() {
		Camera2D camera = new Camera2D(640.0, 360.0);
		AtomicInteger cancellationCount = new AtomicInteger();
		GameInputController input = new GameInputController(
			camera,
			null,
			null,
			null,
			cancellationCount::incrementAndGet,
			METRICS
		);
		input.handleCursorPosition(100, 100);
		Point worldCenterBeforeClick = camera.screenToWorld(640, 360, 1280, 720);

		input.handleRightButtonPress(1_000_000_000L);
		input.handleCursorPosition(103, 103);
		input.handleRightButtonRelease(2_000_000_000L);

		assertEquals(1, cancellationCount.get());
		assertEquals(worldCenterBeforeClick, camera.screenToWorld(640, 360, 1280, 720));
	}

	@Test
	void rightDragPansWithoutCancelling() {
		Camera2D camera = new Camera2D(640.0, 360.0);
		AtomicInteger cancellationCount = new AtomicInteger();
		GameInputController input = new GameInputController(
			camera,
			null,
			null,
			null,
			cancellationCount::incrementAndGet,
			METRICS
		);
		input.handleCursorPosition(100, 100);
		Point worldCenterBeforeDrag = camera.screenToWorld(640, 360, 1280, 720);

		input.handleRightButtonPress(1_000_000_000L);
		input.handleCursorPosition(106, 100);
		input.handleCursorPosition(116, 100);
		input.handleRightButtonRelease(4_000_000_000L);

		assertEquals(0, cancellationCount.get());
		assertTrue(camera.screenToWorld(640, 360, 1280, 720).x < worldCenterBeforeDrag.x);
	}

	@Test
	void rightPressLongerThanTwoSecondsDoesNotCancel() {
		AtomicInteger cancellationCount = new AtomicInteger();
		GameInputController input = controller(cancellationCount);

		input.handleRightButtonPress(1_000_000_000L);
		input.handleRightButtonRelease(3_000_000_001L);

		assertEquals(0, cancellationCount.get());
	}

	@Test
	void rightPressOfExactlyTwoSecondsStillCountsAsClick() {
		AtomicInteger cancellationCount = new AtomicInteger();
		GameInputController input = controller(cancellationCount);

		input.handleRightButtonPress(1_000_000_000L);
		input.handleRightButtonRelease(3_000_000_000L);

		assertEquals(1, cancellationCount.get());
	}

	@Test
	void rightMouseButtonIsConsumedByTheController() {
		GameInputController input = controller(new AtomicInteger());

		assertTrue(
			input.handleMouseButton(GLFW_MOUSE_BUTTON_RIGHT, org.lwjgl.glfw.GLFW.GLFW_PRESS)
		);
		assertTrue(
			input.handleMouseButton(GLFW_MOUSE_BUTTON_RIGHT, org.lwjgl.glfw.GLFW.GLFW_RELEASE)
		);
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

	private static GameInputController controller(AtomicInteger cancellationCount) {
		return new GameInputController(
			new Camera2D(640.0, 360.0),
			null,
			null,
			null,
			cancellationCount::incrementAndGet,
			METRICS
		);
	}
}
