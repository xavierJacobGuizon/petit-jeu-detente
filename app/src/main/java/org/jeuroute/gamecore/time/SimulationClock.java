package org.jeuroute.gamecore.time;

import java.util.Objects;
import java.util.function.Consumer;
import org.jeuroute.model.records.time.SimulationTick;

public final class SimulationClock {

	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private long fractionalTickUnits;
	private long pendingTicks;
	private long currentTickNumber;

	public void advanceSeconds(double elapsedSeconds, Consumer<SimulationTick> tickHandler) {
		if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0.0) {
			throw new IllegalArgumentException("Elapsed time must be finite and non-negative");
		}
		if (elapsedSeconds > Long.MAX_VALUE / (double) NANOS_PER_SECOND) {
			throw new IllegalArgumentException("Elapsed time is too large");
		}
		advance(Math.round(elapsedSeconds * NANOS_PER_SECOND), tickHandler);
	}

	public void advance(long elapsedNanoseconds, Consumer<SimulationTick> tickHandler) {
		if (elapsedNanoseconds < 0) {
			throw new IllegalArgumentException("Elapsed time cannot be negative");
		}
		Objects.requireNonNull(tickHandler, "tickHandler cannot be null");

		long wholeSeconds = elapsedNanoseconds / NANOS_PER_SECOND;
		long remainingNanoseconds = elapsedNanoseconds % NANOS_PER_SECOND;
		long elapsedTicks = Math.multiplyExact(wholeSeconds, SimulationTick.TICKS_PER_SECOND);
		long fractionalUnits =
			remainingNanoseconds * SimulationTick.TICKS_PER_SECOND + fractionalTickUnits;
		elapsedTicks = Math.addExact(elapsedTicks, fractionalUnits / NANOS_PER_SECOND);
		fractionalTickUnits = fractionalUnits % NANOS_PER_SECOND;
		pendingTicks = Math.addExact(pendingTicks, elapsedTicks);

		while (pendingTicks > 0) {
			long nextTickNumber = Math.incrementExact(currentTickNumber);
			tickHandler.accept(new SimulationTick(nextTickNumber));
			currentTickNumber = nextTickNumber;
			pendingTicks--;
		}
	}

	public long currentTickNumber() {
		return currentTickNumber;
	}

	public long pendingTickCount() {
		return pendingTicks;
	}
}
