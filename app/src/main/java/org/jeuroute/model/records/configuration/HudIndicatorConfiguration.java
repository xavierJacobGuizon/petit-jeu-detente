package org.jeuroute.model.records.configuration;

import java.util.List;

public record HudIndicatorConfiguration(List<HudIndicatorSlotConfiguration> slots) {
	public HudIndicatorConfiguration {
		slots = slots == null ? List.of() : List.copyOf(slots);
	}
}
