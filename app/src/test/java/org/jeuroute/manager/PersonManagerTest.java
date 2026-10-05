package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import java.util.Random;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.testing.SimulationTestClock;
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
		SimulationTestClock clock = new SimulationTestClock();
		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();
		Point initialPosition = person.getPosition();
		assertEquals(initialPosition, gridCenter(10, 24));
		assertTrue(person.isAwaitingInitialJourney());
		advanceUntilInitialJourney(manager, person, clock);
		assertEquals(firstHouse, person.getHomeHouse());
		assertEquals(firstHouse, person.getDestinationHouse());
		assertEquals(PersonGoal.Reason.RETURN_HOME, person.getCurrentGoal().reason());

		Point movementStart = person.getPosition();
		clock.advanceSeconds(0.5, manager::update);
		double distanceMoved = movementStart.distance(person.getPosition());
		assertTrue(distanceMoved > 0.0);
		assertEquals(Person.WALK_SPEED_PIXELS_PER_SECOND * 0.5, distanceMoved, 1.0);
		assertTrue(
			person.getPosition().distance(firstHouse.getPosition()) <
				movementStart.distance(firstHouse.getPosition())
		);
		assertTrue(terrain.isLand(person.getPosition()));

		for (int frame = 0; frame < 1_000 && person.isWalking(); frame++) {
			clock.advanceSeconds(0.25, manager::update);
			assertTrue(terrain.isLand(person.getPosition()));
		}

		assertFalse(person.isWalking());
		House firstDestination = person.getCurrentHouse();
		assertNotNull(firstDestination);
		assertTrue(person.getIdleSecondsRemaining() >= 1.5);
		assertTrue(person.getIdleSecondsRemaining() < 8.5);

		clock.advanceSeconds(person.getIdleSecondsRemaining() + 0.01, manager::update);

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
		SimulationTestClock clock = new SimulationTestClock();
		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();
		advanceUntilInitialJourney(manager, person, clock);
		for (int tick = 0; tick < 10_000 && person.isWalking(); tick++) {
			clock.step(manager::update);
		}
		assertEquals(home, person.getCurrentHouse());
		clock.advanceSeconds(person.getIdleSecondsRemaining() + 0.1, manager::update);
		for (int tick = 0; tick < 10_000 && !person.isWalking(); tick++) {
			clock.step(manager::update);
		}
		assertEquals(destination, person.getDestinationHouse());
		assertTrue(person.canAcceptTemporaryDetour());
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
		SimulationTestClock clock = new SimulationTestClock();
		advanceUntilInitialJourney(manager, person, clock);

		assertEquals(home, person.getHomeHouse());
		assertEquals(home, person.getDestinationHouse());
	}

	@Test
	void managerAcceptsAnAlternativeJourneyPlannerImplementation() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		JourneyPlanner<House, PersonGoal, PersonRoute> planner = new JourneyPlanner<>() {
			@Override
			public java.util.Optional<Journey<PersonGoal, PersonRoute>> planInitialJourney(
				Point start,
				List<House> destinations
			) {
				return java.util.Optional.of(
					new Journey<>(
						new PersonGoal(home, PersonGoal.Reason.RETURN_HOME),
						new PersonRoute(List.of())
					)
				);
			}

			@Override
			public java.util.Optional<Journey<PersonGoal, PersonRoute>> planNextJourney(
				Point start,
				House currentDestination,
				List<House> destinations
			) {
				return java.util.Optional.empty();
			}

			@Override
			public java.util.Optional<PersonRoute> planDetour(
				Point start,
				Point detour,
				PersonGoal finalGoal
			) {
				return java.util.Optional.empty();
			}

			@Override
			public boolean isWalkablePosition(Point position) {
				return true;
			}

			@Override
			public double nextIdleDurationSeconds() {
				return 2.0;
			}
		};
		PersonManager manager = new PersonManager(terrain, List.of(home), planner, null);

		Person person = manager.addPerson(gridCenter(10, 24)).orElseThrow();
		new SimulationTestClock().step(manager::update);

		assertEquals(home, person.getCurrentHouse());
		assertEquals(2.0, person.getIdleSecondsRemaining());
	}

	@Test
	void pathfindingSchedulerCapsTotalWorkPerTickForLongRequests() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		PerformanceProfiler profiler = new PerformanceProfiler();
		PersonManager manager = new PersonManager(
			terrain,
			List.of(home),
			fixedWorkPlanner(home, 100_000),
			profiler
		);
		for (int index = 0; index < 10; index++) {
			manager.addPerson(gridCenter(10 + index, 24)).orElseThrow();
		}

		manager.update(new SimulationTick(1));

		PerformanceProfiler.PathfindingQueueStatistics statistics =
			profiler.pathfindingQueueStatistics();
		assertEquals(10, statistics.requestsStarted());
		assertEquals(PersonManager.MAX_PLANNING_WORK_UNITS_PER_TICK, statistics.workUnits());
		assertEquals(10, statistics.queueDepth());
		assertTrue(
			manager
				.getPeople()
				.stream()
				.allMatch(person -> person.isAwaitingInitialJourney())
		);
	}

	@Test
	void pathfindingQueueAcceptsFiveThousandInitialRequests() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		PerformanceProfiler profiler = new PerformanceProfiler();
		PersonManager manager = new PersonManager(
			terrain,
			List.of(home),
			fixedWorkPlanner(home, 100_000),
			profiler
		);
		for (int index = 0; index < 5_000; index++) {
			manager.addPerson(gridCenter(10 + (index % 20), 24)).orElseThrow();
		}
		assertEquals(5_000, manager.getPeople().size());

		manager.update(new SimulationTick(1));

		assertEquals(5_000, profiler.pathfindingQueueStatistics().queueDepth());
		assertEquals(
			PersonManager.MAX_PLANNING_REQUESTS_STARTED_PER_TICK,
			profiler.pathfindingQueueStatistics().requestsStarted()
		);
		assertEquals(
			PersonManager.MAX_PLANNING_WORK_UNITS_PER_TICK,
			profiler.pathfindingQueueStatistics().workUnits()
		);
		assertEquals(
			5_000,
			manager
				.getPeople()
				.stream()
				.filter(person -> person.isAwaitingInitialJourney())
				.count()
		);
	}

	@Test
	void profilerSeparatesMovementIdleWakeTaskStartAndAStarSlices() {
		TerrainMap terrain = rectangularTerrain();
		House home = new House(gridCenter(20, 24));
		PerformanceProfiler profiler = new PerformanceProfiler();
		PersonManager manager = new PersonManager(terrain, List.of(home), new Random(8), profiler);
		manager.addPerson(gridCenter(10, 24)).orElseThrow();

		manager.update(new SimulationTick(1));
		profiler.refreshSnapshot(System.nanoTime(), 0L);

		assertTrue(
			profiler.statistics(PerformanceProfiler.Section.PERSON_MOVEMENT).sampleCount() > 0
		);
		assertTrue(
			profiler.statistics(PerformanceProfiler.Section.PERSON_IDLE_WAKE).sampleCount() > 0
		);
		assertTrue(
			profiler.statistics(PerformanceProfiler.Section.PLANNING_START).sampleCount() > 0
		);
		assertTrue(
			profiler.statistics(PerformanceProfiler.Section.PATHFINDING_SLICE).sampleCount() > 0
		);
	}

	private static JourneyPlanner<House, PersonGoal, PersonRoute> fixedWorkPlanner(
		House home,
		int workRequired
	) {
		return new JourneyPlanner<>() {
			@Override
			public java.util.Optional<Journey<PersonGoal, PersonRoute>> planInitialJourney(
				Point start,
				List<House> destinations
			) {
				return java.util.Optional.empty();
			}

			@Override
			public JourneyPlanningTask<
				java.util.Optional<Journey<PersonGoal, PersonRoute>>
			> beginInitialJourneyPlanning(Point start, List<House> destinations) {
				return new FixedWorkJourneyTask(home, workRequired);
			}

			@Override
			public java.util.Optional<Journey<PersonGoal, PersonRoute>> planNextJourney(
				Point start,
				House currentDestination,
				List<House> destinations
			) {
				return java.util.Optional.empty();
			}

			@Override
			public java.util.Optional<PersonRoute> planDetour(
				Point start,
				Point detour,
				PersonGoal finalGoal
			) {
				return java.util.Optional.empty();
			}

			@Override
			public boolean isWalkablePosition(Point position) {
				return true;
			}

			@Override
			public double nextIdleDurationSeconds() {
				return 2.0;
			}
		};
	}

	private static final class FixedWorkJourneyTask
		implements
			JourneyPlanningTask<java.util.Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>>>
	{

		private final House destination;
		private int remainingWork;
		private boolean complete;

		private FixedWorkJourneyTask(House destination, int remainingWork) {
			this.destination = destination;
			this.remainingWork = remainingWork;
		}

		@Override
		public int advance(int workBudget) {
			int workDone = Math.min(workBudget, remainingWork);
			remainingWork -= workDone;
			complete = remainingWork == 0;
			return workDone;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public java.util.Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>> result() {
			if (!complete) {
				throw new IllegalStateException("Test journey is still pending");
			}
			return java.util.Optional.of(
				new JourneyPlanner.Journey<>(
					new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
					new PersonRoute(List.of())
				)
			);
		}
	}

	private static void advanceUntilInitialJourney(
		PersonManager manager,
		Person person,
		SimulationTestClock clock
	) {
		for (int tick = 0; tick < 10_000 && person.isAwaitingInitialJourney(); tick++) {
			clock.step(manager::update);
		}
		assertFalse(person.isAwaitingInitialJourney());
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
