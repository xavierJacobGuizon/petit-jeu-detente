package org.jeuroute.gamecore.hud.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jeuroute.gamecore.hud.HudTable.Alignment;
import org.junit.jupiter.api.Test;

class HudTableRendererTest {

	@Test
	void rightAlignedValuesKeepTheirDecimalPointsInTheSameColumn() {
		int shortValueX = HudTableRenderer.alignedTextX("1.0", 200, 132, Alignment.RIGHT);
		int longValueX = HudTableRenderer.alignedTextX("1000.0", 200, 132, Alignment.RIGHT);

		assertEquals(shortValueX + 16, longValueX + 64);
	}
}
