package org.jeuroute.gamecore.loop;

import java.util.OptionalInt;

public final class FrameRateCounter {

	private static final long REPORT_INTERVAL_NANOS = 1_000_000_000L;
	private long windowStartNanos = Long.MIN_VALUE;
	private long previousFrameNanos = Long.MIN_VALUE;
	private long completedFrameIntervals;

	public OptionalInt recordCompletedFrame(long completedAtNanos) {
		if (windowStartNanos == Long.MIN_VALUE) {
			windowStartNanos = completedAtNanos;
			previousFrameNanos = completedAtNanos;
			return OptionalInt.empty();
		}
		if (completedAtNanos < previousFrameNanos) {
			throw new IllegalArgumentException("Frame timestamps must be monotonic");
		}

		previousFrameNanos = completedAtNanos;
		completedFrameIntervals++;
		long elapsedNanos = completedAtNanos - windowStartNanos;
		if (elapsedNanos < REPORT_INTERVAL_NANOS) {
			return OptionalInt.empty();
		}

		int framesPerSecond = (int) Math.round(
			(completedFrameIntervals * (double) REPORT_INTERVAL_NANOS) / elapsedNanos
		);
		windowStartNanos = completedAtNanos;
		completedFrameIntervals = 0;
		return OptionalInt.of(framesPerSecond);
	}
}
