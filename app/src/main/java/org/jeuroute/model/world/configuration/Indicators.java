package org.jeuroute.model.world.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jeuroute.configuration.indicators.IndicatorRegistry;
import org.jeuroute.gamecore.hud.elements.HudIndicator;

public final class Indicators {

	private static final IndicatorDefinition[] DEFAULT_INDICATORS = {
		new IndicatorDefinition("fps", "FPS", 0.45f, 0.85f, 0.75f),
		new IndicatorDefinition("roads", "R", 0.12f, 0.35f, 0.42f),
		new IndicatorDefinition("intersections", "I", 0.20f, 0.42f, 0.30f),
		new IndicatorDefinition("vehicles", "V", 0.55f, 0.30f, 0.18f),
		new IndicatorDefinition("stations", "D", 0.85f, 0.68f, 0.24f),
		new IndicatorDefinition("people", "P", 0.24f, 0.72f, 0.86f),
	};

	private Indicators() {
		// Private constructor to prevent instantiation
	}

	private record IndicatorDefinition(
		String id,
		String label,
		float red,
		float green,
		float blue
	) {}

	public static final class Builder {

		private final Map<String, Supplier<String>> values = new LinkedHashMap<>();

		public Builder() {}

		public Builder fps(Supplier<String> value) {
			return setValue("fps", value);
		}

		public Builder roads(Supplier<String> value) {
			return setValue("roads", value);
		}

		public Builder intersections(Supplier<String> value) {
			return setValue("intersections", value);
		}

		public Builder vehicles(Supplier<String> value) {
			return setValue("vehicles", value);
		}

		public Builder stations(Supplier<String> value) {
			return setValue("stations", value);
		}

		public Builder people(Supplier<String> value) {
			return setValue("people", value);
		}

		public IndicatorRegistry build() {
			IndicatorRegistry registry = new IndicatorRegistry();
			for (IndicatorDefinition definition : DEFAULT_INDICATORS) {
				Supplier<String> value = values.get(definition.id());
				if (value == null) {
					continue;
				}

				registry.register(
					definition.id(),
					new HudIndicator(
						definition.label(),
						value,
						definition.red(),
						definition.green(),
						definition.blue()
					)
				);
			}
			return registry;
		}

		private Builder setValue(String indicatorId, Supplier<String> value) {
			values.put(
				indicatorId,
				Objects.requireNonNull(value, "indicator supplier cannot be null")
			);
			return this;
		}
	}
}
