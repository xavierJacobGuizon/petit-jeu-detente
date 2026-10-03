package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.Objects;

public record PersonGoal(House destinationHouse, Reason reason) {
	public PersonGoal {
		Objects.requireNonNull(destinationHouse, "destinationHouse cannot be null");
		Objects.requireNonNull(reason, "reason cannot be null");
	}

	public Point destination() {
		return destinationHouse.getPosition();
	}

	public enum Reason {
		RETURN_HOME,
		LEISURE_VISIT,
	}
}
