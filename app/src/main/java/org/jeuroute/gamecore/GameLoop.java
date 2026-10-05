package org.jeuroute.gamecore;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class GameLoop {

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

			update.accept(elapsedNanoseconds);
			render.run();
			frameRateCounter
				.recordCompletedFrame(System.nanoTime())
				.ifPresent(framesPerSecondUpdated);
		}
	}
}
