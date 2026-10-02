package org.jeuroute.model.records.time;

public record SimulationTick(long number) {
	public static final int TICKS_PER_SECOND = 60;
	public static final double STEP_SECONDS = 1.0 / TICKS_PER_SECOND;

	public SimulationTick {
		if (number <= 0) {
			throw new IllegalArgumentException("Tick number must be positive");
		}
	}

	public double elapsedSeconds() {
		return number * STEP_SECONDS;
	}

	public double deltaSeconds() {
		return STEP_SECONDS;
	}

	public static long ticksForSeconds(double seconds) {
		if (!Double.isFinite(seconds) || seconds < 0.0) {
			throw new IllegalArgumentException("Duration must be finite and non-negative");
		}
		if (seconds > Long.MAX_VALUE / (double) TICKS_PER_SECOND) {
			throw new IllegalArgumentException("Duration is too large");
		}
		return Math.round(seconds * TICKS_PER_SECOND);
	}
}
