package org.jeuroute.model.records.configuration;

import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.configuration.enums.HudAnchor;

public record HudIndicatorSlotConfiguration(HudAnchor anchor, List<String> indicatorIds) {
	public HudIndicatorSlotConfiguration {
		Objects.requireNonNull(anchor, "Slot anchor cannot be null");
		indicatorIds = indicatorIds == null ? List.of() : List.copyOf(indicatorIds);
	}
}
