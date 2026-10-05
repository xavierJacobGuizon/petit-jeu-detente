package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PersonRoute {

	private final List<Segment> segments;

	public PersonRoute(List<Segment> segments) {
		this.segments = List.copyOf(segments);
		for (int index = 1; index < this.segments.size(); index++) {
			if (
				!this.segments
					.get(index - 1)
					.target()
					.equals(this.segments.get(index).start())
			) {
				throw new IllegalArgumentException("Person route segments must be contiguous");
			}
		}
	}

	public static PersonRoute walking(Point start, List<Point> waypoints) {
		Point previous = new Point(Objects.requireNonNull(start));
		List<Segment> segments = new ArrayList<>(waypoints.size());
		for (Point waypoint : waypoints) {
			Point target = new Point(waypoint);
			segments.add(new Segment(previous, target, Surface.WALKING));
			previous = target;
		}
		return new PersonRoute(segments);
	}

	public List<Segment> segments() {
		return segments;
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

	public Point destination() {
		return segments.isEmpty() ? null : segments.getLast().target();
	}

	public double travelTimeSeconds(double walkingSpeed) {
		if (!Double.isFinite(walkingSpeed) || walkingSpeed <= 0.0) {
			throw new IllegalArgumentException("Walking speed must be finite and positive");
		}
		double totalSeconds = 0.0;
		for (Segment segment : segments) {
			totalSeconds +=
				segment.start().distance(segment.target()) /
				(walkingSpeed * segment.surface().speedMultiplier());
		}
		return totalSeconds;
	}

	public enum Surface {
		WALKING(1.0),
		ROAD(1.6);

		private final double speedMultiplier;

		Surface(double speedMultiplier) {
			this.speedMultiplier = speedMultiplier;
		}

		public double speedMultiplier() {
			return speedMultiplier;
		}
	}

	public record Segment(Point start, Point target, Surface surface) {
		public Segment {
			start = new Point(Objects.requireNonNull(start));
			target = new Point(Objects.requireNonNull(target));
			Objects.requireNonNull(surface);
		}

		@Override
		public Point start() {
			return new Point(start);
		}

		@Override
		public Point target() {
			return new Point(target);
		}
	}
}
