package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.terrain.navigation.TerrainPathfinder;

final class PersonJourneyPlanner {

	private static final double NON_OPTIMAL_ROUTE_CHANCE = 0.2;
	private static final double MAX_DETOUR_RATIO = 1.35;
	private static final double MIN_DETOUR_DISTANCE = TerrainMap.CELL_SIZE * 2.0;
	private static final int DETOUR_ATTEMPTS = 16;
	private static final double MIN_IDLE_SECONDS = 1.5;
	private static final double IDLE_VARIATION_SECONDS = 7.0;

	private final TerrainMap terrain;
	private final TerrainPathfinder pathfinder;
	private final Random random;

	PersonJourneyPlanner(TerrainMap terrain, Random random) {
		this.terrain = terrain;
		this.pathfinder = new TerrainPathfinder(terrain);
		this.random = random;
	}

	Optional<Journey> planInitialJourney(Point start, List<House> houses) {
		List<House> nearestFirst = new ArrayList<>(houses);
		nearestFirst.sort(Comparator.comparingDouble(house -> house.getPosition().distance(start)));
		for (House house : nearestFirst) {
			Optional<List<Point>> route = planRoute(start, house.getPosition());
			if (route.isPresent()) {
				return Optional.of(
					new Journey(new PersonGoal(house, PersonGoal.Reason.RETURN_HOME), route.get())
				);
			}
		}
		return Optional.empty();
	}

	Optional<Journey> planNextHouseJourney(Person person, List<House> houses) {
		List<House> candidates = new ArrayList<>();
		for (House house : houses) {
			if (house != person.getCurrentHouse()) {
				candidates.add(house);
			}
		}
		java.util.Collections.shuffle(candidates, random);
		for (House house : candidates) {
			Optional<List<Point>> route = planRoute(person.getPosition(), house.getPosition());
			if (route.isPresent()) {
				return Optional.of(
					new Journey(new PersonGoal(house, PersonGoal.Reason.LEISURE_VISIT), route.get())
				);
			}
		}
		return Optional.empty();
	}

	Optional<List<Point>> planTemporaryDetour(Point start, Point detour, PersonGoal finalGoal) {
		Point destination = finalGoal.destination();
		Optional<List<Point>> directRoute = findRoute(start, destination);
		Optional<List<Point>> routeToDetour = findRoute(start, detour);
		Optional<List<Point>> routeFromDetour = findRoute(detour, destination);
		if (directRoute.isEmpty() || routeToDetour.isEmpty() || routeFromDetour.isEmpty()) {
			return Optional.empty();
		}

		List<Point> detourRoute = combineRoutes(routeToDetour.get(), routeFromDetour.get());
		double directLength = routeLength(start, directRoute.get());
		double detourLength = routeLength(start, detourRoute);
		if (detourLength <= directLength || detourLength > directLength * MAX_DETOUR_RATIO) {
			return Optional.empty();
		}
		return Optional.of(detourRoute);
	}

	double nextIdleDuration() {
		return MIN_IDLE_SECONDS + random.nextDouble() * IDLE_VARIATION_SECONDS;
	}

	private Optional<List<Point>> planRoute(Point start, Point destination) {
		Optional<List<Point>> directRoute = findRoute(start, destination);
		if (directRoute.isEmpty() || directRoute.get().isEmpty()) {
			return directRoute;
		}
		if (random.nextDouble() >= NON_OPTIMAL_ROUTE_CHANCE) {
			return directRoute;
		}
		return findBoundedRandomDetour(start, destination, directRoute.get()).or(() -> directRoute);
	}

	private Optional<List<Point>> findBoundedRandomDetour(
		Point start,
		Point destination,
		List<Point> directRoute
	) {
		if (directRoute.size() < 3) {
			return Optional.empty();
		}
		double directLength = routeLength(start, directRoute);
		for (int attempt = 0; attempt < DETOUR_ATTEMPTS; attempt++) {
			int detourIndex = 1 + random.nextInt(directRoute.size() - 2);
			Point anchor = directRoute.get(detourIndex);
			int anchorColumn = Math.floorDiv(anchor.x - TerrainMap.ORIGIN_X, TerrainMap.CELL_SIZE);
			int anchorRow = Math.floorDiv(anchor.y - TerrainMap.ORIGIN_Y, TerrainMap.CELL_SIZE);
			int column = anchorColumn + random.nextInt(5) - 2;
			int row = anchorRow + random.nextInt(5) - 2;
			if (column < 0 || column >= TerrainMap.COLUMNS || row < 0 || row >= TerrainMap.ROWS) {
				continue;
			}
			if (!terrain.isLandCell(column, row)) {
				continue;
			}
			Point waypoint = new Point(
				TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
				TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
			);
			if (
				waypoint.equals(anchor) ||
				waypoint.distance(start) < MIN_DETOUR_DISTANCE ||
				waypoint.distance(destination) < MIN_DETOUR_DISTANCE
			) {
				continue;
			}
			Point previous = directRoute.get(detourIndex - 1);
			Point next = directRoute.get(detourIndex + 1);
			if (
				!terrain.containsSegment(previous, waypoint) ||
				!terrain.containsSegment(waypoint, next)
			) {
				continue;
			}

			List<Point> detourRoute = new ArrayList<>(directRoute.size() + 1);
			for (int index = 0; index < detourIndex; index++) {
				detourRoute.add(directRoute.get(index));
			}
			detourRoute.add(waypoint);
			for (int index = detourIndex + 1; index < directRoute.size(); index++) {
				detourRoute.add(directRoute.get(index));
			}
			double detourLength = routeLength(start, detourRoute);
			if (detourLength > directLength && detourLength <= directLength * MAX_DETOUR_RATIO) {
				return Optional.of(List.copyOf(detourRoute));
			}
		}
		return Optional.empty();
	}

	private Optional<List<Point>> findRoute(Point start, Point destination) {
		if (start.equals(destination)) {
			return Optional.of(List.of());
		}
		List<Point> route = pathfinder.findPath(start, destination, random);
		return route.isEmpty() ? Optional.empty() : Optional.of(route);
	}

	private static List<Point> combineRoutes(List<Point> first, List<Point> second) {
		List<Point> combined = new ArrayList<>(first);
		for (Point point : second) {
			if (combined.isEmpty() || !combined.getLast().equals(point)) {
				combined.add(new Point(point));
			}
		}
		return List.copyOf(combined);
	}

	private static double routeLength(Point start, List<Point> route) {
		Point previous = start;
		double length = 0.0;
		for (Point waypoint : route) {
			length += previous.distance(waypoint);
			previous = waypoint;
		}
		return length;
	}

	record Journey(PersonGoal goal, List<Point> waypoints) {
		Journey {
			waypoints = waypoints.stream().map(Point::new).toList();
		}
	}
}
