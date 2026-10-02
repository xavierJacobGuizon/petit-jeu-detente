package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.terrain.navigation.TerrainPathfinder;

public final class PersonManager {

	private static final double MIN_IDLE_SECONDS = 1.5;
	private static final double IDLE_VARIATION_SECONDS = 7.0;
	private static final int MAX_WANDER_STOPS = 3;
	private static final int WANDER_STOP_ATTEMPTS = 8;

	private final TerrainMap terrain;
	private final List<House> houses;
	private final TerrainPathfinder pathfinder;
	private final Random random;
	private final List<Person> people = new ArrayList<>();
	private final List<Person> peopleView = Collections.unmodifiableList(people);

	public PersonManager(TerrainMap terrain, List<House> houses, Random random) {
		this.terrain = Objects.requireNonNull(terrain);
		this.houses = Objects.requireNonNull(houses);
		this.pathfinder = new TerrainPathfinder(terrain);
		this.random = Objects.requireNonNull(random);
	}

	public List<Person> getPeople() {
		return peopleView;
	}

	public PlacementPreview getPlacementPreview(Point position) {
		Objects.requireNonNull(position);
		return new PlacementPreview(
			PlacementPreviewType.PERSON,
			position,
			terrain.isLand(position) && !houses.isEmpty()
		);
	}

	public Optional<Person> addPerson(Point position) {
		Objects.requireNonNull(position);
		if (!getPlacementPreview(position).valid()) {
			return Optional.empty();
		}
		Person person = new Person(position);
		people.add(person);
		if (!startWalkToAnotherHouse(person)) {
			people.remove(person);
			return Optional.empty();
		}
		return Optional.of(person);
	}

	public void update(double deltaSeconds) {
		for (Person person : people) {
			if (person.isWalking()) {
				if (person.advanceMovement(deltaSeconds)) {
					person.beginIdle(nextIdleDuration());
				}
			} else if (person.advanceIdle(deltaSeconds)) {
				if (!startWalkToAnotherHouse(person)) {
					person.beginIdle(nextIdleDuration());
				}
			}
		}
	}

	private boolean startWalkToAnotherHouse(Person person) {
		List<House> candidates = new ArrayList<>();
		for (House house : houses) {
			if (house != person.getCurrentHouse()) {
				candidates.add(house);
			}
		}
		Collections.shuffle(candidates, random);
		for (House house : candidates) {
			Point position = person.getPosition();
			Point housePosition = house.getPosition();
			List<Point> route = createWanderingRoute(position, housePosition);
			if (route.isEmpty() && !position.equals(housePosition)) {
				continue;
			}
			if (person.walkTo(house, route)) {
				person.beginIdle(nextIdleDuration());
			}
			return true;
		}
		return false;
	}

	private List<Point> createWanderingRoute(Point start, Point destination) {
		List<Point> route = new ArrayList<>();
		Point segmentStart = start;
		int wanderStops = 1 + random.nextInt(MAX_WANDER_STOPS);

		for (int stopIndex = 0; stopIndex < wanderStops; stopIndex++) {
			boolean foundStop = false;
			for (int attempt = 0; attempt < WANDER_STOP_ATTEMPTS; attempt++) {
				int column = random.nextInt(TerrainMap.COLUMNS);
				int row = random.nextInt(TerrainMap.ROWS);
				if (!terrain.isLandCell(column, row)) {
					continue;
				}
				Point stop = new Point(
					TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
					TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
				);
				if (stop.distance(segmentStart) < TerrainMap.CELL_SIZE * 2.0) {
					continue;
				}
				List<Point> segment = pathfinder.findPath(segmentStart, stop, random);
				if (segment.isEmpty()) {
					continue;
				}
				appendDistinct(route, segment);
				segmentStart = stop;
				foundStop = true;
				break;
			}
			if (!foundStop) {
				break;
			}
		}

		List<Point> finalSegment = pathfinder.findPath(segmentStart, destination, random);
		if (finalSegment.isEmpty()) {
			return List.of();
		}
		appendDistinct(route, finalSegment);
		return List.copyOf(route);
	}

	private static void appendDistinct(List<Point> route, List<Point> segment) {
		for (Point waypoint : segment) {
			if (route.isEmpty() || !route.getLast().equals(waypoint)) {
				route.add(waypoint);
			}
		}
	}

	private double nextIdleDuration() {
		return MIN_IDLE_SECONDS + random.nextDouble() * IDLE_VARIATION_SECONDS;
	}
}
