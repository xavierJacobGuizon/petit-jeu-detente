package org.jeuroute.model.records.preview;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.preview.enums.LinePreviewStatus;

public record LinePreview(List<Point> stations, Point cursor, LinePreviewStatus status) {
	public LinePreview {
		stations = stations
			.stream()
			.map(point -> new Point(Objects.requireNonNull(point, "station cannot be null")))
			.toList();
		if (stations.isEmpty()) {
			throw new IllegalArgumentException("A line preview needs at least one station");
		}
		cursor = new Point(Objects.requireNonNull(cursor, "cursor cannot be null"));
		Objects.requireNonNull(status, "status cannot be null");
	}

	public LinePreview(Point start, Point end) {
		this(List.of(start), end, LinePreviewStatus.NORMAL);
	}

	@Override
	public List<Point> stations() {
		return stations.stream().map(Point::new).toList();
	}

	@Override
	public Point cursor() {
		return new Point(cursor);
	}

	public Point start() {
		return new Point(stations.getLast());
	}

	public Point end() {
		return cursor();
	}
}
