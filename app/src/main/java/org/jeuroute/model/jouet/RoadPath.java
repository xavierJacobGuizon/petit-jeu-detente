package org.jeuroute.model.jouet;

import java.awt.Point;
import java.util.List;

public final class RoadPath {

	public record Leg(Road road, Point start, Point target) {
		public Leg {
			start = new Point(start);
			target = new Point(target);
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

	private final List<Leg> legs;
	private final double length;

	public RoadPath(List<Leg> legs) {
		this.legs = List.copyOf(legs);
		this.length = legs
			.stream()
			.mapToDouble(leg -> leg.start().distance(leg.target()))
			.sum();
	}

	public List<Leg> getLegs() {
		return legs;
	}

	public double getLength() {
		return length;
	}

	public boolean isEmpty() {
		return legs.isEmpty();
	}
}
