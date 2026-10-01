package org.jeuroute.gamecore.hud.elements;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public final class HudButton extends HudElement {

	private final Runnable action;
	private final List<HudButton> submenu;
	private final Color swatchColor;

	public HudButton(String label, Runnable action) {
		this(label, Objects.requireNonNull(action), List.of(), null);
	}

	public HudButton(String label, Runnable action, Color swatchColor) {
		this(label, Objects.requireNonNull(action), List.of(), swatchColor);
	}

	private HudButton(String label, Runnable action, List<HudButton> submenu, Color swatchColor) {
		super(label);
		this.action = action;
		this.submenu = List.copyOf(submenu);
		this.swatchColor = swatchColor;
	}

	public static HudButton submenu(String label, List<HudButton> buttons) {
		if (buttons.isEmpty()) {
			throw new IllegalArgumentException("A submenu must contain at least one button");
		}
		return new HudButton(label, null, buttons, null);
	}

	public boolean hasSubmenu() {
		return !submenu.isEmpty();
	}

	public List<HudButton> getSubmenu() {
		return submenu;
	}

	public Color getSwatchColor() {
		return swatchColor;
	}

	public void activate() {
		if (action == null) {
			return;
		}
		action.run();
	}
}
