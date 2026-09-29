package org.jeuroute.model.jouet;

import java.awt.Color;

public enum LineColor {
	TURQUOISE("TURQUOISE", 0.20f, 0.82f, 0.88f),
	CORAL("CORAIL", 1.0f, 0.50f, 0.30f),
	YELLOW("JAUNE", 0.95f, 0.78f, 0.20f),
	GREEN("VERT", 0.30f, 0.75f, 0.40f),
	MAGENTA("MAGENTA", 0.86f, 0.36f, 0.72f),
	BLUE("BLEU", 0.35f, 0.55f, 1.0f),
	WHITE("BLANC", 0.92f, 0.92f, 0.86f);

	private final String label;
	private final float red;
	private final float green;
	private final float blue;

	LineColor(String label, float red, float green, float blue) {
		this.label = label;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public String getLabel() {
		return label;
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

	public Color toAwtColor() {
		return new Color(red, green, blue);
	}
}
