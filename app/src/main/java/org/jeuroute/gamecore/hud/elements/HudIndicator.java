package org.jeuroute.gamecore.hud.elements;

import java.util.Objects;
import java.util.function.Supplier;

public final class HudIndicator extends HudElement {

	private final Supplier<String> value;
	private final float red;
	private final float green;
	private final float blue;

	public HudIndicator(String label, Supplier<String> value, float red, float green, float blue) {
		super(label);
		this.value = Objects.requireNonNull(value);
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public String getValue() {
		return value.get();
	}

	public float getRed() {
		return red;
	}

	public float getGreen() {
		return green;
	}

	public float getBlue() {
		return blue;
	}
}
