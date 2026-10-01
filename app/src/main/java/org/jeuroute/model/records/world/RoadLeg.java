package org.jeuroute.model.records.world;

import java.awt.Point;
import org.jeuroute.model.world.network.Road;

public record RoadLeg(Road road, Point start, Point target) {
	public RoadLeg {
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
