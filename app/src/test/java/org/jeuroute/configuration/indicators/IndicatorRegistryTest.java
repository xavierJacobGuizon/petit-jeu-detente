package org.jeuroute.configuration.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IndicatorRegistryTest {

	@Test
	void builderRegistersAllDefaultIndicatorsByName() {
		IndicatorRegistry registry = IndicatorRegistry.builder()
			.fps(() -> "60")
			.roads(() -> "12")
			.intersections(() -> "7")
			.vehicles(() -> "4")
			.stations(() -> "2")
			.people(() -> "25")
			.build();

		assertEquals("60", registry.get("fps").getValue());
		assertEquals("12", registry.get("roads").getValue());
		assertEquals("7", registry.get("intersections").getValue());
		assertEquals("4", registry.get("vehicles").getValue());
		assertEquals("2", registry.get("stations").getValue());
		assertEquals("25", registry.get("people").getValue());
	}

	@Test
	void builderOnlyRegistersConfiguredIndicators() {
		IndicatorRegistry registry = IndicatorRegistry.builder()
			.fps(() -> "60")
			.build();

		assertEquals("60", registry.get("fps").getValue());
		assertThrows(IllegalArgumentException.class, () -> registry.get("roads"));
	}

	@Test
	void registerAllKeepsTheTargetRegistryInstance() {
		IndicatorRegistry target = new IndicatorRegistry();
		IndicatorRegistry partial = IndicatorRegistry.builder()
			.fps(() -> "60")
			.build();

		assertEquals(target, target.registerAll(partial));
		assertEquals("60", target.get("fps").getValue());
	}
}
