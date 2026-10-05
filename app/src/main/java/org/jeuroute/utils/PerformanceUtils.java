package org.jeuroute.utils;

import java.util.Objects;
import java.util.function.Supplier;
import org.jeuroute.gamecore.PerformanceProfiler;

public final class PerformanceUtils {

	private PerformanceUtils() {}

	public static void measure(
		PerformanceProfiler profiler,
		PerformanceProfiler.Section section,
		Runnable action
	) {
		Objects.requireNonNull(action);
		if (profiler == null) {
			action.run();
			return;
		}

		long startedAtNanos = System.nanoTime();
		try {
			action.run();
		} finally {
			recordElapsed(profiler, section, startedAtNanos);
		}
	}

	public static <T> T measure(
		PerformanceProfiler profiler,
		PerformanceProfiler.Section section,
		Supplier<T> action
	) {
		Objects.requireNonNull(action);
		if (profiler == null) {
			return action.get();
		}

		long startedAtNanos = System.nanoTime();
		try {
			return action.get();
		} finally {
			recordElapsed(profiler, section, startedAtNanos);
		}
	}

	private static void recordElapsed(
		PerformanceProfiler profiler,
		PerformanceProfiler.Section section,
		long startedAtNanos
	) {
		if (profiler == null) {
			return;
		}
		profiler.record(section, System.nanoTime() - startedAtNanos);
	}
}
