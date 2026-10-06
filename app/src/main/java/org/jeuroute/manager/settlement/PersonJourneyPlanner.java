package org.jeuroute.manager.settlement;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.jeuroute.gamecore.performance.PerformanceProfiler;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonGoal;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;

final class PersonJourneyPlanner implements JourneyPlanner<House, PersonGoal, PersonRoute> {

	private static final double MAX_DETOUR_RATIO = 1.35;
	private static final double MIN_IDLE_SECONDS = 1.5;
	private static final double IDLE_VARIATION_SECONDS = 7.0;

	private final PersonPathPlanner pathPlanner;
	private final Random random;

	PersonJourneyPlanner(TerrainMap terrain, Random random) {
		this(terrain, new RoadGraph(terrain), List.of(), List.of(), List.of(), random);
	}

	PersonJourneyPlanner(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses,
		List<ResourceBuilding> resourceBuildings,
		List<Depot> depots,
		Random random
	) {
		this(terrain, roadGraph, houses, resourceBuildings, depots, random, null);
	}

	PersonJourneyPlanner(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses,
		List<ResourceBuilding> resourceBuildings,
		List<Depot> depots,
		Random random,
		PerformanceProfiler performanceProfiler
	) {
		pathPlanner = new PersonPathPlanner(
			terrain,
			roadGraph,
			houses,
			resourceBuildings,
			depots,
			performanceProfiler
		);
		this.random = random;
	}

	@Override
	public Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>> planInitialJourney(
		Point start,
		List<House> houses
	) {
		return complete(beginInitialJourneyPlanning(start, houses));
	}

	@Override
	public JourneyPlanningTask<
		Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>>
	> beginInitialJourneyPlanning(Point start, List<House> houses) {
		List<House> nearestFirst = new ArrayList<>(houses);
		nearestFirst.sort(Comparator.comparingDouble(house -> house.getPosition().distance(start)));
		return new HouseJourneyTask(new Point(start), nearestFirst, PersonGoal.Reason.RETURN_HOME);
	}

	@Override
	public Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>> planNextJourney(
		Point start,
		House currentDestination,
		List<House> houses
	) {
		return complete(beginNextJourneyPlanning(start, currentDestination, houses));
	}

	@Override
	public JourneyPlanningTask<
		Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>>
	> beginNextJourneyPlanning(Point start, House currentDestination, List<House> houses) {
		List<House> candidates = new ArrayList<>();
		for (House house : houses) {
			if (house != currentDestination) {
				candidates.add(house);
			}
		}
		Collections.shuffle(candidates, random);
		return new HouseJourneyTask(new Point(start), candidates, PersonGoal.Reason.LEISURE_VISIT);
	}

	@Override
	public Optional<PersonRoute> planDetour(Point start, Point detour, PersonGoal finalGoal) {
		return complete(beginDetourPlanning(start, detour, finalGoal));
	}

	@Override
	public JourneyPlanningTask<Optional<PersonRoute>> beginDetourPlanning(
		Point start,
		Point detour,
		PersonGoal finalGoal
	) {
		return new DetourPlanningTask(new Point(start), new Point(detour), finalGoal);
	}

	@Override
	public boolean isWalkablePosition(Point position) {
		return pathPlanner.isWalkablePosition(position);
	}

	@Override
	public double nextIdleDurationSeconds() {
		return MIN_IDLE_SECONDS + random.nextDouble() * IDLE_VARIATION_SECONDS;
	}

	private static PersonRoute combineRoutes(PersonRoute first, PersonRoute second) {
		List<PersonRoute.Segment> combined = new ArrayList<>(first.segments());
		combined.addAll(second.segments());
		return new PersonRoute(combined);
	}

	private static <Result> Result complete(JourneyPlanningTask<Result> task) {
		while (!task.isComplete()) {
			task.advance(Integer.MAX_VALUE);
		}
		return task.result();
	}

	private final class HouseJourneyTask
		implements JourneyPlanningTask<Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>>>
	{

		private final Point start;
		private final List<House> candidates;
		private final PersonGoal.Reason reason;
		private int candidateIndex;
		private PersonPathPlanner.SearchSession search;
		private Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>> result = Optional.empty();
		private boolean complete;

		private HouseJourneyTask(Point start, List<House> candidates, PersonGoal.Reason reason) {
			this.start = start;
			this.candidates = List.copyOf(candidates);
			this.reason = reason;
		}

		@Override
		public int advance(int workBudget) {
			if (complete || workBudget <= 0) {
				return 0;
			}
			int workDone = 0;
			while (!complete && workDone < workBudget) {
				if (candidateIndex >= candidates.size()) {
					complete = true;
					break;
				}
				if (search == null) {
					search = pathPlanner.beginFastestRoute(start, candidates.get(candidateIndex));
				}
				int usedWork = search.advance(workBudget - workDone);
				workDone += Math.max(1, usedWork);
				if (!search.isComplete()) {
					break;
				}
				Optional<PersonRoute> route = search.result();
				search = null;
				if (route.isPresent()) {
					House destination = candidates.get(candidateIndex);
					result = Optional.of(
						new JourneyPlanner.Journey<>(
							new PersonGoal(destination, reason),
							route.get()
						)
					);
					complete = true;
				} else {
					candidateIndex++;
				}
			}
			return workDone;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public Optional<JourneyPlanner.Journey<PersonGoal, PersonRoute>> result() {
			if (!complete) {
				throw new IllegalStateException("Journey planning is not complete");
			}
			return result;
		}
	}

	private final class DetourPlanningTask implements JourneyPlanningTask<Optional<PersonRoute>> {

		private final Point start;
		private final Point detour;
		private final PersonGoal finalGoal;
		private int phase;
		private PersonPathPlanner.SearchSession search;
		private PersonRoute directRoute;
		private PersonRoute routeToDetour;
		private PersonRoute routeFromDetour;
		private Optional<PersonRoute> result = Optional.empty();
		private boolean complete;

		private DetourPlanningTask(Point start, Point detour, PersonGoal finalGoal) {
			this.start = start;
			this.detour = detour;
			this.finalGoal = finalGoal;
		}

		@Override
		public int advance(int workBudget) {
			if (complete || workBudget <= 0) {
				return 0;
			}
			int workDone = 0;
			while (!complete && workDone < workBudget) {
				if (search == null) {
					search = switch (phase) {
						case 0 -> pathPlanner.beginFastestRoute(
							start,
							finalGoal.destinationHouse()
						);
						case 1 -> pathPlanner.beginFastestRouteToPoint(start, detour);
						case 2 -> pathPlanner.beginFastestRoute(
							detour,
							finalGoal.destinationHouse()
						);
						default -> null;
					};
					if (search == null) {
						completeDetour();
						break;
					}
				}
				int usedWork = search.advance(workBudget - workDone);
				workDone += Math.max(1, usedWork);
				if (!search.isComplete()) {
					break;
				}
				Optional<PersonRoute> route = search.result();
				search = null;
				if (route.isEmpty()) {
					complete = true;
					break;
				}
				switch (phase++) {
					case 0 -> directRoute = route.get();
					case 1 -> routeToDetour = route.get();
					case 2 -> routeFromDetour = route.get();
					default -> throw new IllegalStateException("Unexpected detour search phase");
				}
				if (phase == 3) {
					completeDetour();
				}
			}
			return workDone;
		}

		private void completeDetour() {
			if (directRoute != null && routeToDetour != null && routeFromDetour != null) {
				PersonRoute detourRoute = combineRoutes(routeToDetour, routeFromDetour);
				double directTime = directRoute.travelTimeSeconds(
					Person.WALK_SPEED_PIXELS_PER_SECOND
				);
				double detourTime = detourRoute.travelTimeSeconds(
					Person.WALK_SPEED_PIXELS_PER_SECOND
				);
				if (detourTime > directTime && detourTime <= directTime * MAX_DETOUR_RATIO) {
					result = Optional.of(detourRoute);
				}
			}
			complete = true;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public Optional<PersonRoute> result() {
			if (!complete) {
				throw new IllegalStateException("Detour planning is not complete");
			}
			return result;
		}
	}
}
