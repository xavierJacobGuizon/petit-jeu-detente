package org.jeuroute.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jeuroute.gamecore.PerformanceProfiler;
import org.junit.jupiter.api.Test;

class PerformanceUtilsTest {

	@Test
	void measureReturnsSupplierValueAndRecordsDuration() {
		PerformanceProfiler profiler = new PerformanceProfiler();

		String result = PerformanceUtils.measure(
			profiler,
			PerformanceProfiler.Section.ROUTE_PLANNING,
			() -> "planned"
		);
		profiler.refreshSnapshot(System.nanoTime());

		assertEquals("planned", result);
		assertEquals(
			1,
			profiler.statistics(PerformanceProfiler.Section.ROUTE_PLANNING).sampleCount()
		);
	}

	@Test
	void measureRecordsDurationWhenActionThrows() {
		PerformanceProfiler profiler = new PerformanceProfiler();
		Runnable failingAction = () -> {
			throw new IllegalStateException("route planning failed");
		};

		assertThrows(IllegalStateException.class, () ->
			PerformanceUtils.measure(
				profiler,
				PerformanceProfiler.Section.ROUTE_PLANNING,
				failingAction
			)
		);
		profiler.refreshSnapshot(System.nanoTime());

		assertEquals(
			1,
			profiler.statistics(PerformanceProfiler.Section.ROUTE_PLANNING).sampleCount()
		);
	}
}
