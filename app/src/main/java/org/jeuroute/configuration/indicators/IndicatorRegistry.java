package org.jeuroute.configuration.indicators;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jeuroute.configuration.Registry;
import org.jeuroute.gamecore.hud.elements.HudIndicator;
import org.jeuroute.model.world.configuration.Indicators;

public final class IndicatorRegistry implements Registry<HudIndicator> {

	private final Map<String, HudIndicator> indicators = new HashMap<>();

	public static Indicators.Builder builder() {
		return new Indicators.Builder();
	}

	public IndicatorRegistry registerAll(IndicatorRegistry other) {
		Objects.requireNonNull(other, "other registry cannot be null");
		indicators.putAll(other.indicators);
		return this;
	}

	public IndicatorRegistry register(String indicatorId, HudIndicator indicator) {
		if (indicatorId == null || indicatorId.isBlank()) {
			throw new IllegalArgumentException("Indicator id cannot be blank");
		}
		indicators.put(indicatorId, Objects.requireNonNull(indicator, "indicator cannot be null"));
		return this;
	}

	public HudIndicator get(String indicatorId) {
		HudIndicator indicator = indicators.get(indicatorId);
		if (indicator == null) {
			throw new IllegalArgumentException("No indicator registered for id: " + indicatorId);
		}
		return indicator;
	}
}
