package org.jeuroute.gamecore.rendering.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import org.jeuroute.model.records.camera.WorldViewBounds;
import org.jeuroute.gamecore.rendering.debug.DebugRouteLineMerger.Line;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.testing.SimulationTestClock;
import org.junit.jupiter.api.Test;

class PersonRouteGeometryCacheTest {

	@Test
	void reusesRouteTargetsAndUpdatesOnlyTheMovingOrigin() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(100, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);
		PersonRouteGeometryCache cache = new PersonRouteGeometryCache();

		List<Line> initialLines = cache.collectVisibleLines(
			List.of(person),
			WorldViewBounds.UNBOUNDED
		);
		assertEquals(1, cache.diagnostics().changedRoutes());
		assertEquals(1, cache.diagnostics().rawSegments());
		assertEquals(1, cache.diagnostics().visibleSegments());
		assertEquals(1, cache.diagnostics().mergedLines());
		new SimulationTestClock().advanceSeconds(0.5, person::advanceMovement);
		List<Line> updatedLines = cache.collectVisibleLines(
			List.of(person),
			WorldViewBounds.UNBOUNDED
		);

		assertEquals(0.0, initialLines.getFirst().startX());
		assertEquals(14.0, updatedLines.getFirst().startX(), 1.0e-9);
		assertEquals(100.0, updatedLines.getFirst().endX());
		assertEquals(0, cache.diagnostics().changedRoutes());
		assertEquals(1, cache.diagnostics().rawSegments());
		assertEquals(1, cache.diagnostics().mergedLines());
	}

	@Test
	void cullsSegmentsOutsideTheViewportBeforeReturningLines() {
		House destination = new House(new Point(100, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(10, 0),
						PersonRoute.Surface.WALKING
					),
					new PersonRoute.Segment(
						new Point(10, 0),
						new Point(100, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);

		List<Line> visibleLines = new PersonRouteGeometryCache().collectVisibleLines(
			List.of(person),
			new WorldViewBounds(90, -5, 110, 5)
		);

		assertEquals(1, visibleLines.size());
		assertEquals(10.0, visibleLines.getFirst().startX());
		assertEquals(100.0, visibleLines.getFirst().endX());
	}

	@Test
	void refreshesCachedTargetsWhenThePersonChangesSegments() {
		House destination = new House(new Point(20, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(10, 0),
						PersonRoute.Surface.WALKING
					),
					new PersonRoute.Segment(
						new Point(10, 0),
						new Point(20, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);
		PersonRouteGeometryCache cache = new PersonRouteGeometryCache();
		cache.collectVisibleLines(List.of(person), WorldViewBounds.UNBOUNDED);

		new SimulationTestClock().advanceSeconds(0.5, person::advanceMovement);
		List<Line> updatedLines = cache.collectVisibleLines(
			List.of(person),
			WorldViewBounds.UNBOUNDED
		);

		assertEquals(1, updatedLines.size());
		assertEquals(14.0, updatedLines.getFirst().startX(), 1.0e-9);
		assertEquals(20.0, updatedLines.getFirst().endX());
		assertEquals(0, cache.diagnostics().changedRoutes());
		assertEquals(1, cache.diagnostics().rawSegments());
		assertEquals(1, cache.diagnostics().segmentTransitions());
	}

	@Test
	void movingHeadDoesNotRebuildStaticTailMerges() {
		House destination = new House(new Point(1_200, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(100, 0),
						PersonRoute.Surface.WALKING
					),
					new PersonRoute.Segment(
						new Point(100, 0),
						new Point(1_200, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);
		PersonRouteGeometryCache cache = new PersonRouteGeometryCache();
		WorldViewBounds view = new WorldViewBounds(0, -10, 200, 10);

		cache.collectVisibleLines(List.of(person), view);
		int initialMergeBuilds = cache.staticMergeBuildCount();
		new SimulationTestClock().advanceSeconds(0.1, person::advanceMovement);
		cache.collectVisibleLines(List.of(person), view);

		assertTrue(initialMergeBuilds > 0);
		assertEquals(initialMergeBuilds, cache.staticMergeBuildCount());
	}

	@Test
	void changingViewportQueriesCachedRouteGeometryWithoutRebuildingPlans() {
		House destination = new House(new Point(1_200, 0));
		Person person = new Person(new Point(0, 0), destination);
		person.beginJourney(
			new PersonGoal(destination, PersonGoal.Reason.RETURN_HOME),
			new PersonRoute(
				List.of(
					new PersonRoute.Segment(
						new Point(0, 0),
						new Point(100, 0),
						PersonRoute.Surface.WALKING
					),
					new PersonRoute.Segment(
						new Point(100, 0),
						new Point(1_200, 0),
						PersonRoute.Surface.WALKING
					)
				)
			)
		);
		PersonRouteGeometryCache cache = new PersonRouteGeometryCache();

		cache.collectVisibleLines(List.of(person), new WorldViewBounds(0, -10, 200, 10));
		cache.collectVisibleLines(List.of(person), new WorldViewBounds(600, -10, 800, 10));

		assertEquals(0, cache.diagnostics().changedRoutes());
		assertTrue(cache.diagnostics().mergedLines() > 0);
	}
}
