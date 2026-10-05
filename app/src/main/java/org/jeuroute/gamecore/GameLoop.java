package org.jeuroute.gamecore;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class GameLoop {

	private static final long MAX_FRAME_DELTA_NANOS = 100_000_000L;

	public void start(
		BooleanSupplier shouldClose,
		Runnable pollEvents,
		LongConsumer update,
		Runnable render
	) {
		start(shouldClose, pollEvents, update, render, ignored -> {});
	}

	public void start(
		BooleanSupplier shouldClose,
		Runnable pollEvents,
		LongConsumer update,
		Runnable render,
		IntConsumer framesPerSecondUpdated
	) {
		Objects.requireNonNull(framesPerSecondUpdated);
		FrameRateCounter frameRateCounter = new FrameRateCounter();
		long previousFrameNs = System.nanoTime();

		while (!shouldClose.getAsBoolean()) {
			pollEvents.run();

			if (shouldClose.getAsBoolean()) {
				break;
			}

			long nowNs = System.nanoTime();
			long elapsedNanoseconds = nowNs - previousFrameNs;
			previousFrameNs = nowNs;

			update.accept(capFrameDelta(elapsedNanoseconds));
			render.run();
			frameRateCounter
				.recordCompletedFrame(System.nanoTime())
				.ifPresent(framesPerSecondUpdated);
		}
	}

	static long capFrameDelta(long elapsedNanoseconds) {
		return Math.min(elapsedNanoseconds, MAX_FRAME_DELTA_NANOS);
	}
}
