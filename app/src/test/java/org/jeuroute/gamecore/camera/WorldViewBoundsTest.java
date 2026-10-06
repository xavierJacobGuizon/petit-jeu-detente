package org.jeuroute.gamecore.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jeuroute.model.records.camera.WorldViewBounds;
import org.junit.jupiter.api.Test;

class WorldViewBoundsTest {

	private static final WorldViewBounds VIEW = new WorldViewBounds(0.0, 0.0, 100.0, 80.0);

	@Test
	void cameraCalculatesVisibleWorldBoundsFromViewportAndZoom() {
		Camera2D camera = new Camera2D(50.0, 40.0);

		assertEquals(
			new WorldViewBounds(0.0, 0.0, 100.0, 80.0),
			camera.getVisibleWorldBounds(100, 80)
		);

		camera.zoomAt(1.0, 50, 40, 100, 80);

		assertEquals(
			new WorldViewBounds(
				50.0 - 50.0 / camera.getZoom(),
				40.0 - 40.0 / camera.getZoom(),
				50.0 + 50.0 / camera.getZoom(),
				40.0 + 40.0 / camera.getZoom()
			),
			camera.getVisibleWorldBounds(100, 80)
		);
	}

	@Test
	void containsVisiblePointsAndIntersectsPartiallyVisibleCircles() {
		assertTrue(VIEW.contains(50.0, 40.0, 0.0));
		assertTrue(VIEW.contains(-5.0, 20.0, 5.0));
		assertFalse(VIEW.contains(-6.0, 20.0, 5.0));
	}

	@Test
	void testsRectanglesAgainstTheirVisibleExtent() {
		assertTrue(VIEW.intersectsRectangle(-4.0, 40.0, 5.0, 5.0));
		assertFalse(VIEW.intersectsRectangle(-6.0, 40.0, 5.0, 5.0));
	}

	@Test
	void keepsSegmentsThatCrossTheViewEvenWhenBothEndsAreOutside() {
		assertTrue(VIEW.intersectsSegment(-50.0, 40.0, 150.0, 40.0, 0.0));
		assertFalse(VIEW.intersectsSegment(-50.0, -30.0, -10.0, -10.0, 0.0));
	}
}
