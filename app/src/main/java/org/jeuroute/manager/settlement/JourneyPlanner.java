package org.jeuroute.manager.settlement;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

public interface JourneyPlanner<Destination, Goal, Route> {
	Optional<Journey<Goal, Route>> planInitialJourney(Point start, List<Destination> destinations);

	default JourneyPlanningTask<Optional<Journey<Goal, Route>>> beginInitialJourneyPlanning(
		Point start,
		List<Destination> destinations
	) {
		Point startSnapshot = new Point(start);
		List<Destination> destinationSnapshot = List.copyOf(destinations);
		return JourneyPlanningTask.deferred(() ->
			planInitialJourney(startSnapshot, destinationSnapshot)
		);
	}

	Optional<Journey<Goal, Route>> planNextJourney(
		Point start,
		Destination currentDestination,
		List<Destination> destinations
	);

	default JourneyPlanningTask<Optional<Journey<Goal, Route>>> beginNextJourneyPlanning(
		Point start,
		Destination currentDestination,
		List<Destination> destinations
	) {
		Point startSnapshot = new Point(start);
		List<Destination> destinationSnapshot = List.copyOf(destinations);
		return JourneyPlanningTask.deferred(() ->
			planNextJourney(startSnapshot, currentDestination, destinationSnapshot)
		);
	}

	Optional<Route> planDetour(Point start, Point detour, Goal finalGoal);

	default JourneyPlanningTask<Optional<Route>> beginDetourPlanning(
		Point start,
		Point detour,
		Goal finalGoal
	) {
		Point startSnapshot = new Point(start);
		Point detourSnapshot = new Point(detour);
		return JourneyPlanningTask.deferred(() ->
			planDetour(startSnapshot, detourSnapshot, finalGoal)
		);
	}

	boolean isWalkablePosition(Point position);

	double nextIdleDurationSeconds();

	record Journey<Goal, Route>(Goal goal, Route route) {}
}
