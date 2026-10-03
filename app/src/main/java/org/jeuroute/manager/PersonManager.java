package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.terrain.TerrainMap;

public final class PersonManager {

	private final List<House> houses;
	private final TerrainMap terrain;
	private final PersonJourneyPlanner journeyPlanner;
	private final PerformanceProfiler performanceProfiler;
	private final List<Person> people = new ArrayList<>();
	private final List<Person> peopleView = Collections.unmodifiableList(people);

	public PersonManager(TerrainMap terrain, List<House> houses, Random random) {
		this(terrain, houses, random, null);
	}

	public PersonManager(
		TerrainMap terrain,
		List<House> houses,
		Random random,
		PerformanceProfiler performanceProfiler
	) {
		this.houses = Objects.requireNonNull(houses);
		this.terrain = Objects.requireNonNull(terrain);
		this.journeyPlanner = new PersonJourneyPlanner(terrain, Objects.requireNonNull(random));
		this.performanceProfiler = performanceProfiler;
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
		PersonJourneyPlanner.Journey initialJourney = journeyPlanner
			.planInitialJourney(position, houses)
			.orElse(null);
		if (initialJourney == null) {
			return Optional.empty();
		}

		Person person = new Person(position, initialJourney.goal().destinationHouse());
		people.add(person);
		if (person.beginJourney(initialJourney.goal(), initialJourney.waypoints())) {
			person.beginIdle(journeyPlanner.nextIdleDuration());
		}
		return Optional.of(person);
	}

	public void update(double deltaSeconds) {
		long startedAtNanos = System.nanoTime();
		try {
			for (Person person : people) {
				if (person.isWalking()) {
					if (person.advanceMovement(deltaSeconds)) {
						person.beginIdle(journeyPlanner.nextIdleDuration());
					}
				} else if (person.advanceIdle(deltaSeconds)) {
					startNextJourney(person);
				}
			}
		} finally {
			recordUpdateTime(startedAtNanos);
		}
	}

	public void update(SimulationTick tick) {
		Objects.requireNonNull(tick);
		long startedAtNanos = System.nanoTime();
		try {
			for (Person person : people) {
				if (person.isWalking()) {
					if (person.advanceMovement(tick)) {
						person.beginIdle(journeyPlanner.nextIdleDuration());
					}
				} else if (person.advanceIdle(tick)) {
					startNextJourney(person);
				}
			}
		} finally {
			recordUpdateTime(startedAtNanos);
		}
	}

	public boolean requestTemporaryDetour(Person person, Point detourPoint) {
		Objects.requireNonNull(person);
		Objects.requireNonNull(detourPoint);
		if (!people.contains(person) || !person.canAcceptTemporaryDetour()) {
			return false;
		}
		Optional<List<Point>> detour = journeyPlanner.planTemporaryDetour(
			person.getPosition(),
			detourPoint,
			person.getCurrentGoal()
		);
		return detour.filter(person::replaceRoute).isPresent();
	}

	private boolean startNextJourney(Person person) {
		long startedAtNanos = System.nanoTime();
		Optional<PersonJourneyPlanner.Journey> journey;
		try {
			journey = journeyPlanner.planNextHouseJourney(person, houses);
		} finally {
			if (performanceProfiler != null) {
				performanceProfiler.record(
					PerformanceProfiler.Section.ROUTE_PLANNING,
					System.nanoTime() - startedAtNanos
				);
			}
		}
		if (journey.isEmpty()) {
			person.beginIdle(journeyPlanner.nextIdleDuration());
			return false;
		}

		PersonJourneyPlanner.Journey nextJourney = journey.get();
		if (person.beginJourney(nextJourney.goal(), nextJourney.waypoints())) {
			person.beginIdle(journeyPlanner.nextIdleDuration());
		}
		return true;
	}

	private void recordUpdateTime(long startedAtNanos) {
		if (performanceProfiler != null) {
			performanceProfiler.record(
				PerformanceProfiler.Section.PERSON_UPDATE,
				System.nanoTime() - startedAtNanos
			);
		}
	}
}
