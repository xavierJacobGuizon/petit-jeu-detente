package org.jeuroute.model.records.hud;

import java.awt.Color;

public record HudDrawButton(String label, HudBounds bounds, boolean active, Color swatchColor) {}
