package org.jeuroute.model.world.network;

import java.util.List;
import org.jeuroute.model.records.world.RoadLeg;

public final class RoadPath {

	private final List<RoadLeg> legs;
	private final double length;

	public RoadPath(List<RoadLeg> legs) {
		this.legs = List.copyOf(legs);
		this.length = legs
			.stream()
			.mapToDouble(leg -> leg.start().distance(leg.target()))
			.sum();
	}

	public List<RoadLeg> getLegs() {
		return legs;
	}

	public double getLength() {
		return length;
	}

	public boolean isEmpty() {
		return legs.isEmpty();
	}
}
