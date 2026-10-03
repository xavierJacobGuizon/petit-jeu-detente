package org.jeuroute.gamecore;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class GameLoop {

	public void start(
		BooleanSupplier shouldClose,
		Runnable pollEvents,
		DoubleConsumer update,
		Runnable render
	) {
		start(shouldClose, pollEvents, update, render, ignored -> {});
	}

	public void start(
		BooleanSupplier shouldClose,
		Runnable pollEvents,
		DoubleConsumer update,
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
			double deltaSeconds = (nowNs - previousFrameNs) / 1_000_000_000.0;
			previousFrameNs = nowNs;

			update.accept(deltaSeconds);
			render.run();
			frameRateCounter
				.recordCompletedFrame(System.nanoTime())
				.ifPresent(framesPerSecondUpdated);
		}
	}
}
