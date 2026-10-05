package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.settlement.PersonMovementSystem;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.utils.PerformanceUtils;

public final class PersonManager {

	static final int MAX_ACTIVE_PLANNING_REQUESTS = 256;
	static final int MAX_PLANNING_REQUESTS_STARTED_PER_TICK = 128;
	static final int MAX_PLANNING_WORK_UNITS_PER_TICK = 16_384;

	private final List<House> houses;
	private final TerrainMap terrain;
	private final JourneyPlanner<House, PersonGoal, PersonRoute> journeyPlanner;
	private final PersonMovementSystem movementSystem = new PersonMovementSystem();
	private final PerformanceProfiler performanceProfiler;
	private final List<Person> people = new ArrayList<>();
	private final List<Person> peopleView = Collections.unmodifiableList(people);
	private final Set<Person> walkingPeople = new LinkedHashSet<>();
	private final PriorityQueue<PersonWakeup> sleepingPeople = new PriorityQueue<>(
		java.util.Comparator.comparingLong((PersonWakeup wakeup) ->
			wakeup.wakeTick()
		).thenComparingLong(wakeup -> wakeup.sequence())
	);
	private final Deque<PlanningRequest<?>> waitingRequests = new ArrayDeque<>();
	private final Deque<PlanningRequest<?>> activeRequests = new ArrayDeque<>();
	private final Map<Person, PlanningRequest<?>> pendingRequestsByPerson = new IdentityHashMap<>();
	private long lastTickNumber;
	private long nextWakeupSequence;

	public PersonManager(TerrainMap terrain, List<House> houses, Random random) {
		this(
			terrain,
			houses,
			new PersonJourneyPlanner(
				terrain,
				new RoadGraph(terrain),
				houses,
				List.of(),
				List.of(),
				random
			),
			null
		);
	}

	public PersonManager(
		TerrainMap terrain,
		List<House> houses,
		Random random,
		PerformanceProfiler performanceProfiler
	) {
		this(
			terrain,
			houses,
			new PersonJourneyPlanner(
				terrain,
				new RoadGraph(terrain),
				houses,
				List.of(),
				List.of(),
				random,
				performanceProfiler
			),
			performanceProfiler
		);
	}

	public PersonManager(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses,
		List<ResourceBuilding> resourceBuildings,
		List<Depot> depots,
		Random random,
		PerformanceProfiler performanceProfiler
	) {
		this(
			terrain,
			houses,
			new PersonJourneyPlanner(
				terrain,
				roadGraph,
				houses,
				resourceBuildings,
				depots,
				Objects.requireNonNull(random),
				performanceProfiler
			),
			performanceProfiler
		);
	}

	public PersonManager(
		TerrainMap terrain,
		List<House> houses,
		JourneyPlanner<House, PersonGoal, PersonRoute> journeyPlanner,
		PerformanceProfiler performanceProfiler
	) {
		this.houses = Objects.requireNonNull(houses);
		this.terrain = Objects.requireNonNull(terrain);
		this.journeyPlanner = Objects.requireNonNull(journeyPlanner);
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
			terrain.isLand(position) &&
				!houses.isEmpty() &&
				journeyPlanner.isWalkablePosition(position)
		);
	}

	public Optional<Person> addPerson(Point position) {
		Objects.requireNonNull(position);
		if (!getPlacementPreview(position).valid()) {
			return Optional.empty();
		}

		Person person = new Person(position);
		people.add(person);
		Point start = new Point(position);
		enqueuePlanningRequest(
			person,
			PlanningRequestType.INITIAL,
			lastTickNumber,
			() -> journeyPlanner.beginInitialJourneyPlanning(start, houses),
			journey -> {
				if (journey.isEmpty()) {
					people.remove(person);
					return;
				}
				JourneyPlanner.Journey<PersonGoal, PersonRoute> initialJourney = journey.get();
				person.beginInitialJourney(initialJourney.goal(), initialJourney.route());
				trackAfterPlanning(person, lastTickNumber);
			}
		);
		return Optional.of(person);
	}

	public void update(SimulationTick tick) {
		Objects.requireNonNull(tick);
		lastTickNumber = tick.number();
		PerformanceUtils.measure(
			performanceProfiler,
			PerformanceProfiler.Section.PERSON_UPDATE,
			() -> {
				PerformanceUtils.measure(
					performanceProfiler,
					PerformanceProfiler.Section.PERSON_MOVEMENT,
					() -> advanceWalkingPeople(tick)
				);
				PerformanceUtils.measure(
					performanceProfiler,
					PerformanceProfiler.Section.PERSON_IDLE_WAKE,
					() -> wakeSleepingPeople(tick.number())
				);
				advancePlanningRequests(tick.number());
			}
		);
	}

	private void advanceWalkingPeople(SimulationTick tick) {
		Iterator<Person> walkers = walkingPeople.iterator();
		while (walkers.hasNext()) {
			Person person = walkers.next();
			PlanningRequest<?> pendingRequest = pendingRequestsByPerson.get(person);
			if (pendingRequest != null && pendingRequest.type == PlanningRequestType.DETOUR) {
				continue;
			}
			if (movementSystem.advance(person, tick)) {
				walkers.remove();
				beginIdlePeriod(person, tick.number());
			}
		}
	}

	public boolean requestTemporaryDetour(Person person, Point detourPoint) {
		Objects.requireNonNull(person);
		Objects.requireNonNull(detourPoint);
		if (
			!people.contains(person) ||
			!person.canAcceptTemporaryDetour() ||
			pendingRequestsByPerson.containsKey(person)
		) {
			return false;
		}

		Point start = person.getPosition();
		Point detour = new Point(detourPoint);
		PersonGoal finalGoal = person.getCurrentGoal();
		boolean enqueued = enqueuePlanningRequest(
			person,
			PlanningRequestType.DETOUR,
			lastTickNumber,
			() -> journeyPlanner.beginDetourPlanning(start, detour, finalGoal),
			route -> {
				if (route.isPresent() && person.canAcceptTemporaryDetour()) {
					person.replaceRoute(route.get());
				}
				if (person.isWalking()) {
					walkingPeople.add(person);
				}
			}
		);
		if (enqueued) {
			walkingPeople.remove(person);
		}
		return enqueued;
	}

	private void enqueueNextJourney(Person person, long tickNumber) {
		Point start = person.getPosition();
		House currentHouse = person.getCurrentHouse();
		enqueuePlanningRequest(
			person,
			PlanningRequestType.NEXT,
			tickNumber,
			() -> journeyPlanner.beginNextJourneyPlanning(start, currentHouse, houses),
			journey -> {
				if (journey.isEmpty()) {
					beginIdlePeriod(person, lastTickNumber);
					return;
				}
				JourneyPlanner.Journey<PersonGoal, PersonRoute> nextJourney = journey.get();
				person.beginJourney(nextJourney.goal(), nextJourney.route());
				trackAfterPlanning(person, lastTickNumber);
			}
		);
	}

	private void trackAfterPlanning(Person person, long tickNumber) {
		if (person.isWalking()) {
			walkingPeople.add(person);
		} else if (person.getCurrentHouse() != null) {
			beginIdlePeriod(person, tickNumber);
		}
	}

	private void beginIdlePeriod(Person person, long tickNumber) {
		person.beginIdle(journeyPlanner.nextIdleDurationSeconds());
		sleepingPeople.add(
			new PersonWakeup(
				tickNumber + Math.max(1L, person.getIdleTicksRemaining()),
				nextWakeupSequence++,
				person
			)
		);
	}

	private void wakeSleepingPeople(long tickNumber) {
		while (!sleepingPeople.isEmpty() && sleepingPeople.peek().wakeTick() <= tickNumber) {
			PersonWakeup wakeup = sleepingPeople.remove();
			Person person = wakeup.person();
			if (
				person.isWalking() ||
				person.isAwaitingInitialJourney() ||
				pendingRequestsByPerson.containsKey(person)
			) {
				continue;
			}
			person.finishIdlePeriod();
			enqueueNextJourney(person, tickNumber);
		}
	}

	private <Result> boolean enqueuePlanningRequest(
		Person person,
		PlanningRequestType type,
		long tickNumber,
		Supplier<JourneyPlanningTask<Result>> taskFactory,
		Consumer<Result> completion
	) {
		if (pendingRequestsByPerson.containsKey(person)) {
			return false;
		}
		PlanningRequest<Result> request = new PlanningRequest<>(
			person,
			type,
			tickNumber,
			taskFactory,
			completion
		);
		pendingRequestsByPerson.put(person, request);
		waitingRequests.addLast(request);
		return true;
	}

	private void advancePlanningRequests(long tickNumber) {
		int requestsStarted = 0;
		while (
			activeRequests.size() < MAX_ACTIVE_PLANNING_REQUESTS &&
			requestsStarted < MAX_PLANNING_REQUESTS_STARTED_PER_TICK &&
			!waitingRequests.isEmpty()
		) {
			PlanningRequest<?> request = waitingRequests.removeFirst();
			long activationStartedAtNanos = System.nanoTime();
			try {
				request.activate();
			} finally {
				if (performanceProfiler != null) {
					performanceProfiler.record(
						PerformanceProfiler.Section.PLANNING_START,
						System.nanoTime() - activationStartedAtNanos
					);
				}
			}
			activeRequests.addLast(request);
			requestsStarted++;
		}

		int workRemaining = MAX_PLANNING_WORK_UNITS_PER_TICK;
		int totalWorkUnits = 0;
		int requestsCompleted = 0;
		int requestCount = activeRequests.size();
		long maxWaitTicks = oldestRequestTick()
			.map(enqueuedTick -> Math.max(0L, tickNumber - enqueuedTick))
			.orElse(0L);

		for (int index = 0; index < requestCount && workRemaining > 0; index++) {
			PlanningRequest<?> request = activeRequests.removeFirst();
			int requestsRemaining = requestCount - index;
			int workBudget = Math.max(1, workRemaining / requestsRemaining);
			long planningStartedAtNanos = System.nanoTime();
			int workDone;
			try {
				workDone = request.advance(workBudget);
			} finally {
				if (performanceProfiler != null) {
					performanceProfiler.record(
						PerformanceProfiler.Section.ROUTE_PLANNING,
						System.nanoTime() - planningStartedAtNanos
					);
				}
			}
			workDone = Math.min(workDone, workBudget);
			workRemaining -= workDone;
			totalWorkUnits += workDone;
			maxWaitTicks = Math.max(maxWaitTicks, tickNumber - request.enqueuedTick);
			if (request.isComplete()) {
				pendingRequestsByPerson.remove(request.person);
				request.complete();
				requestsCompleted++;
			} else {
				activeRequests.addLast(request);
			}
		}

		int queueDepth = pendingRequestsByPerson.size();
		if (performanceProfiler != null) {
			performanceProfiler.recordPathfindingQueueStatistics(
				new PerformanceProfiler.PathfindingQueueStatistics(
					queueDepth,
					requestsStarted,
					requestsCompleted,
					Math.max(0, queueDepth - activeRequests.size()),
					totalWorkUnits,
					maxWaitTicks
				)
			);
		}
	}

	private Optional<Long> oldestRequestTick() {
		long oldest = Long.MAX_VALUE;
		PlanningRequest<?> waiting = waitingRequests.peekFirst();
		if (waiting != null) {
			oldest = waiting.enqueuedTick;
		}
		for (PlanningRequest<?> active : activeRequests) {
			oldest = Math.min(oldest, active.enqueuedTick);
		}
		return oldest == Long.MAX_VALUE ? Optional.empty() : Optional.of(oldest);
	}

	private enum PlanningRequestType {
		INITIAL,
		NEXT,
		DETOUR,
	}

	private record PersonWakeup(long wakeTick, long sequence, Person person) {}

	private static final class PlanningRequest<Result> {

		private final Person person;
		private final PlanningRequestType type;
		private final long enqueuedTick;
		private final Supplier<JourneyPlanningTask<Result>> taskFactory;
		private final Consumer<Result> completion;
		private JourneyPlanningTask<Result> task;

		private PlanningRequest(
			Person person,
			PlanningRequestType type,
			long enqueuedTick,
			Supplier<JourneyPlanningTask<Result>> taskFactory,
			Consumer<Result> completion
		) {
			this.person = person;
			this.type = type;
			this.enqueuedTick = enqueuedTick;
			this.taskFactory = taskFactory;
			this.completion = completion;
		}

		private void activate() {
			task = taskFactory.get();
		}

		private int advance(int workBudget) {
			return task.advance(workBudget);
		}

		private boolean isComplete() {
			return task.isComplete();
		}

		private void complete() {
			completion.accept(task.result());
		}
	}
}
