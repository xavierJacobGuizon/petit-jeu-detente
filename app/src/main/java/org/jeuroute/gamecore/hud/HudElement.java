package org.jeuroute.gamecore.hud;

import java.util.Objects;

public abstract class HudElement {

	private final String label;

	public HudElement(String label) {
		this.label = Objects.requireNonNull(label);
	}

	public String getLabel() {
		return label;
	}
}
