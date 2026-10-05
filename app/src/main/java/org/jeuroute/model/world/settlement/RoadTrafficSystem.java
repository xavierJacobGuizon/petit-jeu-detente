package org.jeuroute.model.world.settlement;

final class RoadTrafficSystem {

	double advance(PersonMovementState state, double elapsedSeconds) {
		return state.advance(elapsedSeconds);
	}
}
