package org.jeuroute.gamecore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.util.List;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.junit.jupiter.api.Test;

class PersonRenderBatchTest {

	@Test
	void collectsVisiblePositionsAndDebugGeometryInOnePass() {
		House destination = new House(new Point(60, 40));
		Person first = new Person(new Point(20, 30), destination);
		first.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			List.of(new Point(60, 40))
		);
		Person second = new Person(new Point(30, 35), destination);
		second.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			List.of(new Point(60, 40))
		);
		PersonRenderBatch batch = new PersonRenderBatch();

		try {
			batch.collect(List.of(first, second), new WorldViewBounds(0, 0, 100, 80), true);

			assertEquals(2, batch.visiblePersonCount());
			assertEquals(2, batch.destinationLineCount());
			assertEquals(1, batch.destinationCircleCount());
			assertEquals(4, batch.visiblePositions().remaining());
			assertEquals(8, batch.destinationLines().remaining());
			assertEquals(4, batch.destinationCircles().remaining());
		} finally {
			batch.close();
		}
	}

	@Test
	void skipsDebugPreparationWhenItIsDisabled() {
		House destination = new House(new Point(60, 40));
		Person person = new Person(new Point(20, 30), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			List.of(new Point(60, 40))
		);
		PersonRenderBatch batch = new PersonRenderBatch();

		try {
			batch.collect(List.of(person), new WorldViewBounds(0, 0, 100, 80), false);

			assertEquals(1, batch.visiblePersonCount());
			assertEquals(0, batch.destinationLineCount());
			assertEquals(0, batch.destinationCircleCount());
		} finally {
			batch.close();
		}
	}
}