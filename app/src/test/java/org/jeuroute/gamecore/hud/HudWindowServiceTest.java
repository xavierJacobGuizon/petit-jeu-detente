package org.jeuroute.gamecore.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jeuroute.gamecore.hud.constants.HudWindowMetrics;
import org.junit.jupiter.api.Test;

class HudWindowServiceTest {

	@Test
	void outsideClickDoesNotCloseWindow() {
		HudWindowService service = new HudWindowService();
		service.open(HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {}));

		assertFalse(service.handleMousePressed(500, 500));
		assertEquals(1, service.windows().size());
	}

	@Test
	void titleBarDragMovesWindowAndReleaseStopsDragging() {
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(service.handleMousePressed(30, 30));
		service.handleMouseMoved(80, 90, 800, 600);
		assertEquals(60, window.x());
		assertEquals(80, window.y());

		service.handleMouseReleased();
		service.handleMouseMoved(180, 190, 800, 600);
		assertEquals(60, window.x());
		assertEquals(80, window.y());
	}

	@Test
	void closeButtonRemovesOnlyItsWindow() {
		HudWindowService service = new HudWindowService();
		service.open(HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {}));

		assertTrue(service.handleMousePressed(284, 31));
		assertTrue(service.windows().isEmpty());
	}

	@Test
	void contentClickIsForwardedWithoutClosingWindow() {
		HudWindowService service = new HudWindowService();
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
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, contentRenderer)
		);
		var contentBounds = window.contentBounds();

		assertTrue(service.handleMousePressed(contentBounds.x() + 5, contentBounds.y() + 5));
		assertEquals(1, clickCount[0]);
		assertEquals(1, service.windows().size());
	}

	@Test
	void scrollOverWindowContentIsForwardedAndConsumed() {
		int[] scrollCount = { 0 };
		HudWindow.ContentRenderer contentRenderer = new HudWindow.ContentRenderer() {
			@Override
			public void render(org.jeuroute.model.records.hud.HudBounds contentBounds) {}

			@Override
			public boolean handleScroll(
				org.jeuroute.model.records.hud.HudBounds contentBounds,
				double mouseX,
				double mouseY,
				double scrollAmount
			) {
				scrollCount[0]++;
				return true;
			}
		};
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, contentRenderer)
		);
		var contentBounds = window.contentBounds();

		assertTrue(service.handleScroll(contentBounds.x() + 5, contentBounds.y() + 5, -1.0));
		assertEquals(1, scrollCount[0]);
		assertFalse(service.handleScroll(500, 500, -1.0));
	}

	@Test
	void rightBorderResizesWidthOnly() {
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(service.handleMousePressed(305, 100));
		service.handleMouseMoved(355, 130, 800, 600);

		assertEquals(350, window.width());
		assertEquals(200, window.height());
	}

	@Test
	void bottomBorderResizesHeightOnly() {
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(service.handleMousePressed(100, 215));
		service.handleMouseMoved(140, 265, 800, 600);

		assertEquals(300, window.width());
		assertEquals(250, window.height());
	}

	@Test
	void bottomRightCornerResizesBothDimensionsAndRespectsMinimums() {
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
			HudWindowSpec.standard("Profiler", 10, 20, 300, 200, ignored -> {})
		);

		assertTrue(service.handleMousePressed(305, 215));
		service.handleMouseMoved(-100, -100, 800, 600);

		assertEquals(HudWindowMetrics.MIN_WINDOW_WIDTH, window.width());
		assertEquals(HudWindowMetrics.MIN_WINDOW_HEIGHT, window.height());
	}

	@Test
	void supportsMultipleWindowsAndRoutesInputToTheFrontmostWindow() {
		HudWindowService service = new HudWindowService();
		int[] lowerWindowClicks = { 0 };
		int[] upperWindowClicks = { 0 };
		HudWindow.ContentRenderer lowerContent = clickCounter(lowerWindowClicks);
		HudWindow.ContentRenderer upperContent = clickCounter(upperWindowClicks);
		HudWindow lowerWindow = service.open(
			HudWindowSpec.standard("LOWER", 10, 20, 300, 200, lowerContent)
		);
		HudWindow upperWindow = service.open(
			HudWindowSpec.standard("UPPER", 10, 20, 300, 200, upperContent)
		);

		var contentBounds = upperWindow.contentBounds();
		assertTrue(service.handleMousePressed(contentBounds.x() + 5, contentBounds.y() + 5));
		assertEquals(0, lowerWindowClicks[0]);
		assertEquals(1, upperWindowClicks[0]);
		assertEquals(upperWindow, service.windows().getLast());
		assertEquals(2, service.windows().size());
		assertTrue(service.handleMouseReleased());
		assertFalse(service.handleMouseReleased());
		assertTrue(service.windows().contains(lowerWindow));
	}

	@Test
	void closingWindowByIdRunsItsCloseHandlerOnlyOnce() {
		HudWindowService service = new HudWindowService();
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
		HudWindow window = service.open(spec);

		assertTrue(service.close(window.id()));
		assertFalse(service.close(window.id()));
		assertEquals(1, closeCount[0]);
	}

	@Test
	void windowSpecControlsResizeAndCloseBehaviorPerInstance() {
		HudWindowService service = new HudWindowService();
		HudWindow window = service.open(
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
		assertTrue(service.handleMousePressed(305, 100));
		service.handleMouseMoved(400, 100, 800, 600);
		assertEquals(300, window.width());
		assertEquals(200, window.height());
		assertEquals(1, service.windows().size());
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
