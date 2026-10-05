package org.jeuroute.testing;

import java.util.Objects;
import java.util.function.Consumer;
import org.jeuroute.gamecore.time.SimulationClock;
import org.jeuroute.model.records.time.SimulationTick;

public final class SimulationTestClock {

	private final SimulationClock clock = new SimulationClock();

	public void advanceSeconds(double elapsedSeconds, Consumer<SimulationTick> update) {
		clock.advanceSeconds(elapsedSeconds, Objects.requireNonNull(update));
	}

	public void step(Consumer<SimulationTick> update) {
		advanceSeconds(SimulationTick.STEP_SECONDS, update);
	}
}
