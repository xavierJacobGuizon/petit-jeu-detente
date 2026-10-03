package org.jeuroute.gamecore.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudWindowManagerTest {

	@Test
	void outsideClickDoesNotCloseWindow() {
		HudWindowManager manager = new HudWindowManager();
		manager.open(HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {}));

		assertFalse(manager.handleMousePressed(500, 500));
		assertEquals(1, manager.windows().size());
	}

	@Test
	void titleBarDragMovesWindowAndReleaseStopsDragging() {
		HudWindowManager manager = new HudWindowManager();
		HudWindow window = manager.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(manager.handleMousePressed(30, 30));
		manager.handleMouseMoved(80, 90, 800, 600);
		assertEquals(60, window.x());
		assertEquals(80, window.y());

		manager.handleMouseReleased();
		manager.handleMouseMoved(180, 190, 800, 600);
		assertEquals(60, window.x());
		assertEquals(80, window.y());
	}

	@Test
	void closeButtonRemovesOnlyItsWindow() {
		HudWindowManager manager = new HudWindowManager();
		manager.open(HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {}));

		assertTrue(manager.handleMousePressed(284, 31));
		assertTrue(manager.windows().isEmpty());
	}

	@Test
	void contentClickIsForwardedWithoutClosingWindow() {
		HudWindowManager manager = new HudWindowManager();
		int[] clickCount = { 0 };
		HudWindow.ContentRenderer contentRenderer = new HudWindow.ContentRenderer() {
			@Override
			public void render(org.jeuroute.model.records.hud.HudBounds contentBounds) {}

			@Override
			public boolean handleClick(
				org.jeuroute.model.records.hud.HudBounds contentBounds,
				double mouseX,
				double mouseY
			) {
				clickCount[0]++;
				return true;
			}
		};
		HudWindow window = manager.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, contentRenderer)
		);
		var contentBounds = window.contentBounds();

		assertTrue(manager.handleMousePressed(contentBounds.x() + 5, contentBounds.y() + 5));
		assertEquals(1, clickCount[0]);
		assertEquals(1, manager.windows().size());
	}

	@Test
	void rightBorderResizesWidthOnly() {
		HudWindowManager manager = new HudWindowManager();
		HudWindow window = manager.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(manager.handleMousePressed(305, 100));
		manager.handleMouseMoved(355, 130, 800, 600);

		assertEquals(350, window.width());
		assertEquals(200, window.height());
	}

	@Test
	void bottomBorderResizesHeightOnly() {
		HudWindowManager manager = new HudWindowManager();
		HudWindow window = manager.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(manager.handleMousePressed(100, 215));
		manager.handleMouseMoved(140, 265, 800, 600);

		assertEquals(300, window.width());
		assertEquals(250, window.height());
	}

	@Test
	void bottomRightCornerResizesBothDimensionsAndRespectsMinimums() {
		HudWindowManager manager = new HudWindowManager();
		HudWindow window = manager.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(manager.handleMousePressed(305, 215));
		manager.handleMouseMoved(-100, -100, 800, 600);

		assertEquals(HudWindowMetrics.MIN_WINDOW_WIDTH, window.width());
		assertEquals(HudWindowMetrics.MIN_WINDOW_HEIGHT, window.height());
	}

	@Test
	void supportsMultipleWindowsAndRoutesInputToTheFrontmostWindow() {
		HudWindowManager manager = new HudWindowManager();
		int[] lowerWindowClicks = { 0 };
		int[] upperWindowClicks = { 0 };
		HudWindow.ContentRenderer lowerContent = clickCounter(lowerWindowClicks);
		HudWindow.ContentRenderer upperContent = clickCounter(upperWindowClicks);
		HudWindow lowerWindow = manager.open(
			HudWindowSpec.standard("LOWER", 10, 20, 300, 200, lowerContent)
		);
		HudWindow upperWindow = manager.open(
			HudWindowSpec.standard("UPPER", 10, 20, 300, 200, upperContent)
		);

		var contentBounds = upperWindow.contentBounds();
		assertTrue(manager.handleMousePressed(contentBounds.x() + 5, contentBounds.y() + 5));
		assertEquals(0, lowerWindowClicks[0]);
		assertEquals(1, upperWindowClicks[0]);
		assertEquals(upperWindow, manager.windows().getLast());
		assertEquals(2, manager.windows().size());
		assertTrue(manager.handleMouseReleased());
		assertFalse(manager.handleMouseReleased());
		assertTrue(manager.windows().contains(lowerWindow));
	}

	@Test
	void closingWindowByIdRunsItsCloseHandlerOnlyOnce() {
		HudWindowManager manager = new HudWindowManager();
		int[] closeCount = { 0 };
		HudWindowSpec spec = new HudWindowSpec(
			"CLOSABLE",
			0,
			0,
			300,
			200,
			240,
			140,
			true,
			true,
			ignored -> {},
			() -> closeCount[0]++
		);
		HudWindow window = manager.open(spec);

		assertTrue(manager.close(window.id()));
		assertFalse(manager.close(window.id()));
		assertEquals(1, closeCount[0]);
	}

	@Test
	void windowSpecControlsResizeAndCloseBehaviorPerInstance() {
		HudWindowManager manager = new HudWindowManager();
		HudWindow window = manager.open(
			new HudWindowSpec(
				"FIXED",
				10,
				20,
				300,
				200,
				280,
				180,
				false,
				false,
				ignored -> {},
				() -> {}
			)
		);

		assertTrue(window.closable() == false);
		assertTrue(window.resizable() == false);
		assertTrue(manager.handleMousePressed(305, 100));
		manager.handleMouseMoved(400, 100, 800, 600);
		assertEquals(300, window.width());
		assertEquals(200, window.height());
		assertEquals(1, manager.windows().size());
	}

	private static HudWindow.ContentRenderer clickCounter(int[] clickCount) {
		return new HudWindow.ContentRenderer() {
			@Override
			public void render(org.jeuroute.model.records.hud.HudBounds contentBounds) {}

			@Override
			public boolean handleClick(
				org.jeuroute.model.records.hud.HudBounds contentBounds,
				double mouseX,
				double mouseY
			) {
				clickCount[0]++;
				return true;
			}
		};
	}
}
