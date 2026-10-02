package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.junit.jupiter.api.Test;

class PersonManagerTest {

	@Test
	void personWalksAtConstantSpeedIdlesAndThenChoosesAnotherHouse() {
		TerrainMap terrain = rectangularTerrain();
		House firstHouse = new House(gridCenter(20, 24));
		House secondHouse = new House(gridCenter(50, 24));
		PersonManager manager = new PersonManager(
			terrain,
			List.of(firstHouse, secondHouse),
			new Random(8)
		);
		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();
		Point initialPosition = person.getPosition();

		manager.update(0.5);
		double distanceMoved = initialPosition.distance(person.getPosition());
		assertTrue(distanceMoved > 0.0);
		assertEquals(Person.WALK_SPEED_PIXELS_PER_SECOND * 0.5, distanceMoved, 1.0);
		assertNotEquals(initialPosition.y, person.getPosition().y);
		assertTrue(terrain.isLand(person.getPosition()));

		for (int frame = 0; frame < 1_000 && person.isWalking(); frame++) {
			manager.update(0.25);
			assertTrue(terrain.isLand(person.getPosition()));
		}

		assertFalse(person.isWalking());
		House firstDestination = person.getCurrentHouse();
		assertNotNull(firstDestination);
		assertTrue(person.getIdleSecondsRemaining() >= 1.5);
		assertTrue(person.getIdleSecondsRemaining() < 8.5);

		manager.update(person.getIdleSecondsRemaining() + 0.01);

		assertTrue(person.isWalking());
		assertNotEquals(firstDestination, person.getDestinationHouse());
	}

	private static TerrainMap rectangularTerrain() {
		boolean[][] land = new boolean[TerrainMap.ROWS][TerrainMap.COLUMNS];
		for (int row = 2; row < TerrainMap.ROWS - 2; row++) {
			for (int column = 2; column < TerrainMap.COLUMNS - 2; column++) {
				land[row][column] = true;
			}
		}
		return new TerrainMap(TerrainType.ISLAND, land);
	}

	private static Point gridCenter(int column, int row) {
		return new Point(
			TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
			TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
		);
	}
}
