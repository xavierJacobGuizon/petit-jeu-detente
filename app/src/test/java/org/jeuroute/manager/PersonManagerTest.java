package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import java.util.Random;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.terrain.navigation.TerrainPathfinder;
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
		assertEquals(initialPosition, gridCenter(10, 24));
		assertEquals(firstHouse, person.getHomeHouse());
		assertEquals(firstHouse, person.getDestinationHouse());
		assertEquals(PersonGoal.Reason.RETURN_HOME, person.getCurrentGoal().reason());

		manager.update(0.5);
		double distanceMoved = initialPosition.distance(person.getPosition());
		assertTrue(distanceMoved > 0.0);
		assertEquals(Person.WALK_SPEED_PIXELS_PER_SECOND * 0.5, distanceMoved, 1.0);
		assertTrue(
			person.getPosition().distance(firstHouse.getPosition()) <
				initialPosition.distance(firstHouse.getPosition())
		);
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
		assertEquals(PersonGoal.Reason.LEISURE_VISIT, person.getCurrentGoal().reason());
	}

	@Test
	void temporaryDetourKeepsTheOriginalHouseDestinationAndIsAcceptedOnlyOnce() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		House destination = new House(gridCenter(50, 24));
		PersonManager manager = new PersonManager(
			terrain,
			List.of(home, destination),
			new Random(8)
		);
		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();
		for (int tick = 0; tick < 10_000 && person.isWalking(); tick++) {
			manager.update(1.0 / 60.0);
		}
		assertEquals(home, person.getCurrentHouse());
		manager.update(person.getIdleSecondsRemaining() + 0.1);
		assertEquals(destination, person.getDestinationHouse());
		PersonGoal originalGoal = person.getCurrentGoal();

		Point acceptedDetour = null;
		for (int column = 24; column <= 46 && acceptedDetour == null; column += 2) {
			for (int row = 16; row <= 32; row += 2) {
				Point candidate = gridCenter(column, row);
				if (manager.requestTemporaryDetour(person, candidate)) {
					acceptedDetour = candidate;
					break;
				}
			}
		}

		assertNotNull(acceptedDetour);
		assertEquals(destination, person.getDestinationHouse());
		assertEquals(originalGoal, person.getCurrentGoal());
		assertFalse(manager.requestTemporaryDetour(person, gridCenter(35, 30)));
		assertEquals(destination, person.getDestinationHouse());
	}

	@Test
	void personCanBeCreatedWhenTheirHomeIsTheOnlyHouse() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		PersonManager manager = new PersonManager(terrain, List.of(home), new Random(8));

		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();

		assertEquals(home, person.getHomeHouse());
		assertEquals(home, person.getDestinationHouse());
	}

	@Test
	void routeMistakesAreOccasionalAndStayWithinTheDetourBudget() {
		TerrainMap terrain = rectangularTerrain();
		Point start = gridCenter(10, 24);
		House home = new House(gridCenter(50, 24));
		boolean foundDirectRoute = false;
		boolean foundSuboptimalRoute = false;

		for (int seed = 0; seed < 100; seed++) {
			List<Point> directRoute = new TerrainPathfinder(terrain).findPath(
				start,
				home.getPosition(),
				new Random(seed)
			);
			double directDistance = routeDistance(start, directRoute);
			PersonJourneyPlanner planner = new PersonJourneyPlanner(terrain, new Random(seed));
			PersonJourneyPlanner.Journey journey = planner
				.planInitialJourney(start, List.of(home))
				.orElseThrow();
			double routeDistance = routeDistance(start, journey.waypoints());
			assertTrue(routeDistance <= directDistance * 1.35);
			if (routeDistance == directDistance) {
				foundDirectRoute = true;
			} else {
				foundSuboptimalRoute = true;
			}
		}

		assertTrue(foundDirectRoute, "Expected some journeys to use direct routes");
		assertTrue(foundSuboptimalRoute, "Expected some journeys to use bounded route errors");
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

	private static double routeDistance(Point start, List<Point> waypoints) {
		Point previous = start;
		double distance = 0.0;
		for (Point waypoint : waypoints) {
			distance += previous.distance(waypoint);
			previous = waypoint;
		}
		return distance;
	}
}
