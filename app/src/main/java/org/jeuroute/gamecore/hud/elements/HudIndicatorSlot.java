package org.jeuroute.gamecore.hud.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jeuroute.model.records.configuration.enums.HudAnchor;

public final class HudIndicatorSlot {

	private final HudAnchor anchor;
	private final List<HudIndicator> indicators = new ArrayList<>();
	private final List<HudIndicator> indicatorsView = Collections.unmodifiableList(indicators);

	public HudIndicatorSlot(HudAnchor anchor) {
		this.anchor = java.util.Objects.requireNonNull(anchor);
	}

	public HudIndicatorSlot add(HudIndicator indicator) {
		indicators.add(indicator);
		return this;
	}

	public HudAnchor getAnchor() {
		return anchor;
	}

	public List<HudIndicator> getIndicators() {
		return indicatorsView;
	}
}
