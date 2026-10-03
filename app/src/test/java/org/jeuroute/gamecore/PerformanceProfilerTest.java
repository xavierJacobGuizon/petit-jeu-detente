package org.jeuroute.gamecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerformanceProfilerTest {

	@Test
	void calculatesAverageP95AndMaximumForTheRollingWindow() {
		PerformanceProfiler profiler = new PerformanceProfiler();
		for (int duration = 1; duration <= 100; duration++) {
			profiler.recordAt(PerformanceProfiler.Section.FRAME, duration, duration * 1_000L);
		}

		assertTrue(profiler.refreshSnapshot(100));
		PerformanceProfiler.Statistics statistics = profiler.statistics(
			PerformanceProfiler.Section.FRAME
		);
		assertEquals(100, statistics.sampleCount());
		assertEquals(50_500.0, statistics.averageNanos());
		assertEquals(95_000L, statistics.p95Nanos());
		assertEquals(100_000L, statistics.maxNanos());
	}

	@Test
	void expiresSamplesOlderThanTheFiveSecondWindow() {
		PerformanceProfiler profiler = new PerformanceProfiler();
		profiler.recordAt(PerformanceProfiler.Section.UPDATE, 1L, 10L);
		profiler.recordAt(PerformanceProfiler.Section.UPDATE, 2L, 20L);

		profiler.refreshSnapshot(5_000_000_001L);

		PerformanceProfiler.Statistics statistics = profiler.statistics(
			PerformanceProfiler.Section.UPDATE
		);
		assertEquals(1, statistics.sampleCount());
		assertEquals(20.0, statistics.averageNanos());
	}

	@Test
	void refreshesDisplayAtMostOncePerSecond() {
		PerformanceProfiler profiler = new PerformanceProfiler();

		assertTrue(profiler.refreshSnapshot(0L));
		assertFalse(profiler.refreshSnapshot(999_999_999L));
		assertTrue(profiler.refreshSnapshot(1_000_000_000L));
	}

	@Test
	void alignsMetricColumnsWhenMaximumHasMoreDigits() {
		PerformanceProfiler profiler = new PerformanceProfiler();
		profiler.recordAt(PerformanceProfiler.Section.FRAME, 1L, 1_000_000L);
		profiler.recordAt(PerformanceProfiler.Section.FRAME, 2L, 20_000_000L);
		profiler.recordAt(PerformanceProfiler.Section.FRAME, 3L, 1_000_000_000L);
		profiler.refreshSnapshot(3L);

		String line = profiler.displayLines()[PerformanceProfiler.Section.FRAME.ordinal() + 1];
		int averageDecimal = line.indexOf('.');
		int percentileDecimal = line.indexOf('.', averageDecimal + 1);
		int maximumDecimal = line.indexOf('.', percentileDecimal + 1);

		assertEquals(averageDecimal, percentileDecimal);
		assertEquals(percentileDecimal, maximumDecimal);
	}
}
