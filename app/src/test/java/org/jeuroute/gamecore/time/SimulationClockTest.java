package org.jeuroute.gamecore.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.jeuroute.model.records.time.SimulationTick;
import org.junit.jupiter.api.Test;

class SimulationClockTest {

	@Test
	void emitsEveryTickForElapsedTimeWithoutSkipping() {
		SimulationClock clock = new SimulationClock();
		List<SimulationTick> ticks = new ArrayList<>();

		clock.advance(10_000_000_000L, ticks::add);

		assertEquals(600, ticks.size());
		assertEquals(600, clock.currentTickNumber());
		assertEquals(0, clock.pendingTickCount());
		assertEquals(1, ticks.getFirst().number());
		assertEquals(600, ticks.getLast().number());
	}

	@Test
	void slicingElapsedTimeProducesTheSameTickSequence() {
		SimulationClock wholeAdvanceClock = new SimulationClock();
		SimulationClock slicedAdvanceClock = new SimulationClock();
		List<SimulationTick> wholeAdvanceTicks = new ArrayList<>();
		List<SimulationTick> slicedAdvanceTicks = new ArrayList<>();

		wholeAdvanceClock.advance(1_000_000_000L, wholeAdvanceTicks::add);
		for (int slice = 0; slice < 8; slice++) {
			slicedAdvanceClock.advance(125_000_000L, slicedAdvanceTicks::add);
		}

		assertEquals(wholeAdvanceTicks, slicedAdvanceTicks);
		assertEquals(60, slicedAdvanceClock.currentTickNumber());
	}

	@Test
	void doesNotConsumeATickUntilItsHandlerCompletes() {
		SimulationClock clock = new SimulationClock();

		assertThrows(IllegalStateException.class, () ->
			clock.advance(1_000_000_000L, tick -> {
				throw new IllegalStateException("update failed");
			})
		);
		assertEquals(0, clock.currentTickNumber());
		assertEquals(60, clock.pendingTickCount());

		clock.advance(0, tick -> {});

		assertEquals(60, clock.currentTickNumber());
		assertEquals(0, clock.pendingTickCount());
	}

	@Test
	void rejectsNegativeElapsedTime() {
		SimulationClock clock = new SimulationClock();

		assertThrows(IllegalArgumentException.class, () -> clock.advance(-1, tick -> {}));
	}
}
