package org.jeuroute.model.records.preview;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.world.network.Road;

public record RoutePreview(
	Point start,
	Point end,
	boolean valid,
	List<Road> roadSegments,
	List<Point> futureIntersections
) {
	public RoutePreview {
		start = new Point(Objects.requireNonNull(start, "start cannot be null"));
		end = new Point(Objects.requireNonNull(end, "end cannot be null"));
		roadSegments = List.copyOf(
			Objects.requireNonNull(roadSegments, "roadSegments cannot be null")
		);
		futureIntersections = Objects.requireNonNull(
			futureIntersections,
			"futureIntersections cannot be null"
		)
			.stream()
			.map(point -> new Point(Objects.requireNonNull(point, "intersection cannot be null")))
			.toList();
	}

	@Override
	public Point start() {
		return new Point(start);
	}

	@Override
	public Point end() {
		return new Point(end);
	}

	@Override
	public List<Point> futureIntersections() {
		return futureIntersections.stream().map(Point::new).toList();
	}
}
