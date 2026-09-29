package org.jeuroute.gamecore.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
