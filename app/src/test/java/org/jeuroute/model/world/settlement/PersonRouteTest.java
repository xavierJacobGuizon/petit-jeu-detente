package org.jeuroute.model.world.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import org.jeuroute.testing.SimulationTestClock;
import org.junit.jupiter.api.Test;

class PersonRouteTest {

	@Test
	void appliesRoadSpeedOnlyToRoadSegmentsAndRetainsTheRouteEndpoint() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		PersonRoute route = new PersonRoute(
			List.of(
				new PersonRoute.Segment(
					new Point(0, 0),
					new Point(36, 0),
					PersonRoute.Surface.ROAD
				),
				new PersonRoute.Segment(
					new Point(36, 0),
					new Point(100, 0),
					PersonRoute.Surface.WALKING
				)
			)
		);
		person.beginJourney(new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME), route);
		double roadSpeed =
			Person.WALK_SPEED_PIXELS_PER_SECOND * PersonRoute.Surface.ROAD.speedMultiplier();

		new SimulationTestClock().advanceSeconds(1.0, person::advanceMovement);

		assertEquals(
			36.0 + Person.WALK_SPEED_PIXELS_PER_SECOND * (1.0 - 36.0 / roadSpeed),
			person.getPreciseX(),
			0.01
		);
		assertEquals(0.0, person.getPreciseY());
	}

	@Test
	void movementStateTracksDirectionSpeedProgressAndSurfaceTransitions() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(14, 0),
						PersonRoute.Surface.ROAD
					),
					new PersonRoute.Segment(
						new Point(14, 0),
						new Point(100, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);
		PersonMovementState roadState = person.getMovementState();
		double roadSpeed =
			Person.WALK_SPEED_PIXELS_PER_SECOND * PersonRoute.Surface.ROAD.speedMultiplier();

		assertEquals(1.0, roadState.directionX());
		assertEquals(0.0, roadState.directionY());
		assertEquals(roadSpeed, roadState.speed());

		new SimulationTestClock().advanceSeconds(0.5, person::advanceMovement);

		assertTrue(roadState.isComplete());
		assertEquals(PersonRoute.Surface.WALKING, person.getMovementState().surface());
		assertEquals(28.0, person.getMovementState().speed());
		assertEquals(
			Person.WALK_SPEED_PIXELS_PER_SECOND * (0.5 - 14.0 / roadSpeed),
			person.getMovementState().progress(),
			1.0e-9
		);
	}

	@Test
	void personCanWaitForAnInitialRouteBeforeReceivingTheirHome() {
		House home = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0));

		assertTrue(person.isAwaitingInitialJourney());
		assertEquals(null, person.getHomeHouse());
		assertEquals(null, person.getDestinationHouse());

		person.beginInitialJourney(
			new PersonGoal(home, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(85, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);

		assertEquals(home, person.getHomeHouse());
		assertEquals(home, person.getDestinationHouse());
		assertTrue(person.isWalking());
	}

	@Test
	void preparesTheNextMovementStateWhenATickEndsOnASegmentBoundary() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(7, 0),
						PersonRoute.Surface.WALKING
					),
					new PersonRoute.Segment(
						new Point(7, 0),
						new Point(100, 0),
						PersonRoute.Surface.ROAD
					)
				)
			)
		);

		new SimulationTestClock().advanceSeconds(0.25, person::advanceMovement);

		assertEquals(PersonRoute.Surface.ROAD, person.getMovementState().surface());
		assertEquals(0.0, person.getMovementState().progress(), 1.0e-9);
	}

	@Test
	void exposesOnlyTheRemainingRouteSegments() {
		House destination = new House(new Point(20, 0));
		Person person = new Person(new Point(0, 0), destination);
		PersonRoute.Segment firstSegment = new PersonRoute.Segment(
			new Point(0, 0),
			new Point(10, 0),
			PersonRoute.Surface.WALKING
		);
		PersonRoute.Segment finalSegment = new PersonRoute.Segment(
			new Point(10, 0),
			new Point(20, 0),
			PersonRoute.Surface.WALKING
		);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(List.of(firstSegment, finalSegment))
		);
		long versionBeforeSegmentTransition = person.getRouteGeometryVersion();

		new SimulationTestClock().advanceSeconds(0.5, person::advanceMovement);

		assertEquals(List.of(finalSegment), person.getRemainingRouteSegments());
		assertTrue(person.getRouteGeometryVersion() > versionBeforeSegmentTransition);
	}

	@Test
	void reachingAHouseKeepsThePersonAtTheExteriorRouteEndpoint() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(85, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);

		boolean[] arrived = { false };
		new SimulationTestClock().advanceSeconds(10.0, tick -> {
			if (person.advanceMovement(tick)) {
				arrived[0] = true;
			}
		});

		assertTrue(arrived[0]);
		assertEquals(85.0, person.getPreciseX());
		assertEquals(destination, person.getCurrentHouse());
	}
}
