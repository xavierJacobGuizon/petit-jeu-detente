package org.jeuroute.model.records.preview;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;

public record PlacementPreview(PlacementPreviewType type, Point position, boolean valid) {
	public PlacementPreview {
		Objects.requireNonNull(type, "type cannot be null");
		position = new Point(Objects.requireNonNull(position, "position cannot be null"));
	}

	public Point position() {
		return new Point(position);
	}
}
