package org.jeuroute.gamecore.preview;

import java.awt.Point;
import java.util.Objects;

public record PlacementPreview(Type type, Point position, boolean valid) {
	public enum Type {
		VEHICLE,
		STATION,
		DEPOT,
	}

	public PlacementPreview {
		Objects.requireNonNull(type, "type cannot be null");
		position = new Point(Objects.requireNonNull(position, "position cannot be null"));
	}

	public Point position() {
		return new Point(position);
	}
}
