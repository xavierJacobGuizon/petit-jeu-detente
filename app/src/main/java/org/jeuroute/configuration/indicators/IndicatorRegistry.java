package org.jeuroute.configuration.indicators;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jeuroute.configuration.Registry;
import org.jeuroute.gamecore.hud.elements.HudIndicator;

public final class IndicatorRegistry implements Registry<HudIndicator> {

	private final Map<String, HudIndicator> indicators = new HashMap<>();

	public IndicatorRegistry registerDefaults(
		Supplier<String> fpsValue,
		Supplier<String> roadCount,
		Supplier<String> intersectionCount,
		Supplier<String> vehicleCount,
		Supplier<String> stationCount,
		Supplier<String> personCount
	) {
		return register("fps", new HudIndicator("FPS", fpsValue, 0.45f, 0.85f, 0.75f))
			.register("roads", new HudIndicator("R", roadCount, 0.12f, 0.35f, 0.42f))
			.register(
				"intersections",
				new HudIndicator("I", intersectionCount, 0.20f, 0.42f, 0.30f)
			)
			.register("vehicles", new HudIndicator("V", vehicleCount, 0.55f, 0.30f, 0.18f))
			.register("stations", new HudIndicator("D", stationCount, 0.85f, 0.68f, 0.24f))
			.register("people", new HudIndicator("P", personCount, 0.24f, 0.72f, 0.86f));
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
