package org.jeuroute.gamecore.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameLoopTest {

	@Test
	void keepsNormalFrameDeltaUnchanged() {
		assertEquals(16_666_667L, GameLoop.capFrameDelta(16_666_667L));
	}

	@Test
	void capsLongFrameDeltaAtOneHundredMilliseconds() {
		assertEquals(100_000_000L, GameLoop.capFrameDelta(3_600_000_000_000L));
	}
}
