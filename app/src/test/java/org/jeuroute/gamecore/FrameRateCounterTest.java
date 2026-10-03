package org.jeuroute.gamecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class FrameRateCounterTest {

	@Test
	void reportsAverageCompletedFramesOverOneSecond() {
		FrameRateCounter counter = new FrameRateCounter();
		OptionalInt reportedFps = OptionalInt.empty();

		for (int frame = 0; frame <= 60; frame++) {
			long timestamp = (frame * 1_000_000_000L) / 60;
			reportedFps = counter.recordCompletedFrame(timestamp);
		}

		assertEquals(60, reportedFps.orElseThrow());
	}

	@Test
	void doesNotReportAnInstantaneousValueBeforeTheWindowCompletes() {
		FrameRateCounter counter = new FrameRateCounter();

		assertFalse(counter.recordCompletedFrame(0).isPresent());
		assertFalse(counter.recordCompletedFrame(500_000_000L).isPresent());
		assertEquals(2, counter.recordCompletedFrame(1_000_000_000L).orElseThrow());
	}
}
