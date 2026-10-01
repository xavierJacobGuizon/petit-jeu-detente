package org.jeuroute.configuration.model;

import java.util.List;
import java.util.Objects;
import org.jeuroute.gamecore.hud.HudAnchor;

public record HudIndicatorConfiguration(List<Slot> slots) {
	public HudIndicatorConfiguration {
		slots = slots == null ? List.of() : List.copyOf(slots);
	}

	public record Slot(HudAnchor anchor, List<String> indicatorIds) {
		public Slot {
			Objects.requireNonNull(anchor, "Slot anchor cannot be null");
			indicatorIds = indicatorIds == null ? List.of() : List.copyOf(indicatorIds);
		}
	}
}
