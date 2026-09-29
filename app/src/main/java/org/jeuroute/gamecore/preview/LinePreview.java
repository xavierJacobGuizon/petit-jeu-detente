package org.jeuroute.gamecore.preview;

import java.awt.Point;
import java.util.List;
import java.util.Objects;

public record LinePreview(List<Point> stations, Point cursor, Status status) {
	public enum Status {
		NORMAL,
		CONNECTABLE,
		DISCONNECTED,
	}

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
		this(List.of(start), end, Status.NORMAL);
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
