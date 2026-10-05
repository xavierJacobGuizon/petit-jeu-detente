package org.jeuroute.model.world.settlement;

import java.util.Objects;
import org.jeuroute.model.records.time.SimulationTick;

public final class PersonMovementSystem {

	private static final PersonMovementSystem DEFAULT = new PersonMovementSystem();

	private final RoadTrafficSystem roadTrafficSystem = new RoadTrafficSystem();

	public boolean advance(Person person, SimulationTick tick) {
		Objects.requireNonNull(person);
		double remainingSeconds = Objects.requireNonNull(tick).deltaSeconds();
		if (!person.isWalking() || remainingSeconds <= 0.0) {
			return false;
		}

		while (person.isWalking()) {
			PersonMovementState state = person.movementState();
			if (state == null) {
				person.prepareMovementState();
				state = person.movementState();
			}

			double consumedSeconds =
				state.surface() == PersonRoute.Surface.ROAD
					? roadTrafficSystem.advance(state, remainingSeconds)
					: state.advance(remainingSeconds);
			person.setMovementPosition(state.x(), state.y());
			if (!state.isComplete()) {
				return false;
			}
			if (person.completeMovementSegment()) {
				return true;
			}

			remainingSeconds = Math.max(0.0, remainingSeconds - consumedSeconds);
			if (remainingSeconds == 0.0) {
				return false;
			}
		}
		return false;
	}

	static boolean advancePerson(Person person, SimulationTick tick) {
		return DEFAULT.advance(person, tick);
	}
}
