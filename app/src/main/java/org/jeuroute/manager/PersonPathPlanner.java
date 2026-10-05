package org.jeuroute.manager;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;

final class PersonPathPlanner {

	private static final int WALKING = 0;
	private static final int ON_ROAD = 1;
	private static final int START_SEARCH_RADIUS = 2;
	private static final double ROAD_ACCESS_DISTANCE = TerrainMap.CELL_SIZE * 0.8;
	private static final double MAX_OFFROAD_ACCESS_DISTANCE = RoadGraph.GRID_SIZE * 8.0;
	private static final int HOUSE_ACCESS_OFFSET = House.HALF_SIZE + Person.COLLISION_RADIUS + 1;
	private static final int MAX_CACHED_ROUTE_SEARCHES = 1_024;
	private static final int[][] DIRECTIONS = {
		{ 0, -1 },
		{ 1, 0 },
		{ 0, 1 },
		{ -1, 0 },
		{ 1, -1 },
		{ 1, 1 },
		{ -1, 1 },
		{ -1, -1 },
	};

	private final TerrainMap terrain;
	private final RoadGraph roadGraph;
	private final List<House> houses;
	private final List<ResourceBuilding> resourceBuildings;
	private final List<Depot> depots;
	private final PerformanceProfiler performanceProfiler;
	private final Map<SearchKey, Optional<PersonRoute>> routeCache = new LinkedHashMap<>(
		16,
		0.75f,
		true
	) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<SearchKey, Optional<PersonRoute>> eldest) {
			return size() > MAX_CACHED_ROUTE_SEARCHES;
		}
	};
	private final Map<SearchKey, SearchSession> inFlightSearches = new java.util.HashMap<>();
	private long routeCacheHits;
	private long routeCacheMisses;
	private long coalescedSearches;
	private Road[] roadByCell;
	private Point[] roadPointByCell;
	private int[] roadCells = new int[0];
	private long roadCacheVersion = Long.MIN_VALUE;
	private List<Rectangle2D> obstacleCache;
	private int cachedHouseCount = -1;
	private int cachedResourceBuildingCount = -1;
	private int cachedDepotCount = -1;

	PersonPathPlanner(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses,
		List<ResourceBuilding> resourceBuildings,
		List<Depot> depots
	) {
		this(terrain, roadGraph, houses, resourceBuildings, depots, null);
	}

	PersonPathPlanner(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses,
		List<ResourceBuilding> resourceBuildings,
		List<Depot> depots,
		PerformanceProfiler performanceProfiler
	) {
		this.terrain = Objects.requireNonNull(terrain);
		this.roadGraph = Objects.requireNonNull(roadGraph);
		this.houses = Objects.requireNonNull(houses);
		this.resourceBuildings = Objects.requireNonNull(resourceBuildings);
		this.depots = Objects.requireNonNull(depots);
		this.performanceProfiler = performanceProfiler;
	}

	boolean isWalkablePosition(Point position) {
		if (!terrain.isLand(position)) {
			return false;
		}
		for (Rectangle2D obstacle : buildingObstacles()) {
			if (obstacle.contains(position.x, position.y)) {
				return false;
			}
		}
		return true;
	}

	Optional<PersonRoute> findFastestRoute(Point start, House destinationHouse) {
		Objects.requireNonNull(destinationHouse);
		Point center = destinationHouse.getPosition();
		return findFastestRouteToAnyPoint(
			start,
			List.of(
				new Point(center.x, center.y - HOUSE_ACCESS_OFFSET),
				new Point(center.x + HOUSE_ACCESS_OFFSET, center.y),
				new Point(center.x, center.y + HOUSE_ACCESS_OFFSET),
				new Point(center.x - HOUSE_ACCESS_OFFSET, center.y)
			),
			buildingObstacles()
		);
	}

	Optional<PersonRoute> findFastestRouteToPoint(Point start, Point destination) {
		return findFastestRouteToAnyPoint(start, List.of(destination), buildingObstacles());
	}

	private Optional<PersonRoute> findFastestRouteToAnyPoint(
		Point start,
		List<Point> destinations,
		List<Rectangle2D> obstacles
	) {
		SearchSession session = beginSearch(start, destinations, obstacles);
		while (!session.isComplete()) {
			session.advance(Integer.MAX_VALUE);
		}
		return session.result();
	}

	SearchSession beginFastestRoute(Point start, House destinationHouse) {
		Objects.requireNonNull(destinationHouse);
		Point center = destinationHouse.getPosition();
		return beginSearch(
			start,
			List.of(
				new Point(center.x, center.y - HOUSE_ACCESS_OFFSET),
				new Point(center.x + HOUSE_ACCESS_OFFSET, center.y),
				new Point(center.x, center.y + HOUSE_ACCESS_OFFSET),
				new Point(center.x - HOUSE_ACCESS_OFFSET, center.y)
			),
			buildingObstacles()
		);
	}

	SearchSession beginFastestRouteToPoint(Point start, Point destination) {
		return beginSearch(start, List.of(destination), buildingObstacles());
	}

	private SearchSession beginSearch(
		Point start,
		List<Point> destinations,
		List<Rectangle2D> obstacles
	) {
		Objects.requireNonNull(start);
		List<Point> validDestinations = destinations
			.stream()
			.map(Point::new)
			.filter(destination -> isWalkablePosition(destination, obstacles))
			.toList();
		SearchKey key = searchKey(start, validDestinations, obstacles);
		Optional<PersonRoute> cachedResult = routeCache.get(key);
		if (cachedResult != null) {
			routeCacheHits++;
			recordCacheStatistics();
			return new SearchSession(start, validDestinations, cachedResult);
		}
		SearchSession inFlight = inFlightSearches.get(key);
		if (inFlight != null) {
			coalescedSearches++;
			recordCacheStatistics();
			return inFlight;
		}
		routeCacheMisses++;
		recordCacheStatistics();
		if (!isWalkablePosition(start, obstacles) || validDestinations.isEmpty()) {
			return new SearchSession(start, validDestinations, obstacles, key, Optional.empty());
		}
		if (validDestinations.contains(start)) {
			return new SearchSession(
				start,
				validDestinations,
				obstacles,
				key,
				Optional.of(new PersonRoute(List.of()))
			);
		}
		ensureRoadCache();
		SearchSession session = new SearchSession(
			start,
			validDestinations,
			obstacles,
			key,
			Optional.empty()
		);
		if (!session.isComplete()) {
			inFlightSearches.put(key, session);
		}
		return session;
	}

	long routeCacheHits() {
		return routeCacheHits;
	}

	long routeCacheMisses() {
		return routeCacheMisses;
	}

	long coalescedSearches() {
		return coalescedSearches;
	}

	private void recordCacheStatistics() {
		if (performanceProfiler != null) {
			performanceProfiler.recordPathfindingCacheStatistics(
				new PerformanceProfiler.PathfindingCacheStatistics(
					routeCacheHits,
					routeCacheMisses,
					coalescedSearches
				)
			);
		}
	}

	private SearchKey searchKey(
		Point start,
		List<Point> destinations,
		List<Rectangle2D> obstacles
	) {
		List<PointKey> destinationKeys = destinations
			.stream()
			.map(point -> new PointKey(point.x, point.y))
			.toList();
		List<ObstacleKey> obstacleKeys = obstacles
			.stream()
			.map(obstacle ->
				new ObstacleKey(
					Double.doubleToLongBits(obstacle.getX()),
					Double.doubleToLongBits(obstacle.getY()),
					Double.doubleToLongBits(obstacle.getWidth()),
					Double.doubleToLongBits(obstacle.getHeight())
				)
			)
			.toList();
		return new SearchKey(
			new PointKey(start.x, start.y),
			destinationKeys,
			obstacleKeys,
			roadGraph.getVersion()
		);
	}

	final class SearchSession {

		private final Point start;
		private final List<Point> destinations;
		private final List<Rectangle2D> obstacles;
		private final SearchKey cacheKey;
		private final int stateCount = TerrainMap.COLUMNS * TerrainMap.ROWS * 2;
		private double[] distances;
		private int[] previous;
		private PersonRoute.Surface[] incomingSurface;
		private Point[] roadTransitionByState;
		private boolean[] settled;
		private PriorityQueue<SearchNode> open;
		private int roadSeedIndex;
		private int walkingSeedIndex;
		private int startColumn;
		private int startRow;
		private int minWalkingColumn;
		private int maxWalkingColumn;
		private int minWalkingRow;
		private int maxWalkingRow;
		private boolean startsOnRoad;
		private boolean walkingSeedsEnabled;
		private boolean walkingSeedDecisionMade;
		private boolean seedsComplete;
		private boolean allowFreeWalkingStart;
		private double fastestTime = Double.POSITIVE_INFINITY;
		private int fastestState = -1;
		private Point fastestDestination;
		private boolean complete;
		private Optional<PersonRoute> result = Optional.empty();

		private SearchSession(
			Point start,
			List<Point> destinations,
			List<Rectangle2D> obstacles,
			SearchKey cacheKey,
			Optional<PersonRoute> completedResult
		) {
			this.start = new Point(start);
			this.destinations = List.copyOf(destinations);
			this.obstacles = obstacles;
			this.cacheKey = cacheKey;
			if (
				completedResult.isPresent() ||
				!isWalkablePosition(start, obstacles) ||
				destinations.isEmpty()
			) {
				completeWith(completedResult);
			} else {
				initializePass();
			}
		}

		private SearchSession(
			Point start,
			List<Point> destinations,
			Optional<PersonRoute> completedResult
		) {
			this.start = new Point(start);
			this.destinations = List.copyOf(destinations);
			obstacles = List.of();
			cacheKey = null;
			complete = true;
			result = completedResult;
		}

		int advance(int workBudget) {
			if (workBudget <= 0 || complete) {
				return 0;
			}
			long startedAtNanos = System.nanoTime();
			try {
				return advanceSearch(workBudget);
			} finally {
				if (performanceProfiler != null) {
					performanceProfiler.record(
						PerformanceProfiler.Section.PATHFINDING_SLICE,
						System.nanoTime() - startedAtNanos
					);
				}
			}
		}

		private int advanceSearch(int workBudget) {
			int workDone = 0;
			while (!complete && workDone < workBudget) {
				if (!seedsComplete) {
					seedNextStartState();
					workDone++;
					continue;
				}
				if (open.isEmpty()) {
					finishPass();
					continue;
				}

				SearchNode current = open.remove();
				workDone++;
				if (current.priority() >= fastestTime) {
					finishPass();
					continue;
				}
				if (settled[current.state()] || current.distance() > distances[current.state()]) {
					continue;
				}
				settled[current.state()] = true;
				Point position = positionForState(current.state());
				for (Point destination : destinations) {
					boolean walkingState = current.state() % 2 == WALKING;
					if (
						(walkingState ||
							position.distance(destination) <= MAX_OFFROAD_ACCESS_DISTANCE) &&
						canWalkSegment(position, destination, obstacles)
					) {
						double destinationTime =
							current.distance() +
							position.distance(destination) / Person.WALK_SPEED_PIXELS_PER_SECOND;
						if (destinationTime < fastestTime) {
							fastestTime = destinationTime;
							fastestState = current.state();
							fastestDestination = destination;
						}
					}
				}

				expandState(
					current.state(),
					destinations,
					obstacles,
					distances,
					previous,
					incomingSurface,
					roadTransitionByState,
					settled,
					open
				);
			}
			return workDone;
		}

		boolean isComplete() {
			return complete;
		}

		Optional<PersonRoute> result() {
			if (!complete) {
				throw new IllegalStateException("Path search is not complete");
			}
			return result;
		}

		private void initializePass() {
			distances = new double[stateCount];
			Arrays.fill(distances, Double.POSITIVE_INFINITY);
			previous = new int[stateCount];
			Arrays.fill(previous, -2);
			incomingSurface = new PersonRoute.Surface[stateCount];
			roadTransitionByState = new Point[stateCount];
			settled = new boolean[stateCount];
			open = new PriorityQueue<>((left, right) ->
				Double.compare(left.priority(), right.priority())
			);
			roadSeedIndex = 0;
			walkingSeedIndex = 0;
			startColumn = columnAt(start.x);
			startRow = rowAt(start.y);
			minWalkingColumn = Math.max(0, startColumn - START_SEARCH_RADIUS);
			maxWalkingColumn = Math.min(TerrainMap.COLUMNS - 1, startColumn + START_SEARCH_RADIUS);
			minWalkingRow = Math.max(0, startRow - START_SEARCH_RADIUS);
			maxWalkingRow = Math.min(TerrainMap.ROWS - 1, startRow + START_SEARCH_RADIUS);
			startsOnRoad = false;
			walkingSeedsEnabled = false;
			walkingSeedDecisionMade = false;
			seedsComplete = false;
			fastestTime = Double.POSITIVE_INFINITY;
			fastestState = -1;
			fastestDestination = null;
		}

		private void seedNextStartState() {
			if (roadSeedIndex < roadCells.length) {
				int cell = roadCells[roadSeedIndex++];
				Point roadPoint = roadPointByCell[cell];
				if (
					start.distance(roadPoint) <= MAX_OFFROAD_ACCESS_DISTANCE &&
					isOpenRoadCell(cell, obstacles) &&
					canWalkSegment(start, roadPoint, obstacles)
				) {
					startsOnRoad = true;
					seedState(
						cell * 2 + ON_ROAD,
						start.distance(roadPoint) / Person.WALK_SPEED_PIXELS_PER_SECOND,
						minimumDistance(roadPoint, destinations),
						distances,
						previous,
						open
					);
				}
				return;
			}

			if (!walkingSeedDecisionMade) {
				walkingSeedsEnabled = !startsOnRoad || allowFreeWalkingStart;
				walkingSeedDecisionMade = true;
			}
			int columnCount = maxWalkingColumn - minWalkingColumn + 1;
			int rowCount = maxWalkingRow - minWalkingRow + 1;
			if (walkingSeedsEnabled && walkingSeedIndex < columnCount * rowCount) {
				int column = minWalkingColumn + (walkingSeedIndex % columnCount);
				int row = minWalkingRow + walkingSeedIndex / columnCount;
				walkingSeedIndex++;
				int cell = cellIndex(column, row);
				Point walkingPoint = cellCenter(cell);
				if (
					isOpenWalkingCell(column, row, obstacles) &&
					canWalkSegment(start, walkingPoint, obstacles)
				) {
					seedState(
						cell * 2 + WALKING,
						start.distance(walkingPoint) / Person.WALK_SPEED_PIXELS_PER_SECOND,
						minimumDistance(walkingPoint, destinations),
						distances,
						previous,
						open
					);
				}
				return;
			}
			seedsComplete = true;
		}

		private void finishPass() {
			if (fastestState >= 0) {
				completeWith(
					Optional.of(
						reconstructRoute(
							start,
							fastestDestination,
							fastestState,
							previous,
							incomingSurface,
							roadTransitionByState
						)
					)
				);
			} else if (!allowFreeWalkingStart) {
				allowFreeWalkingStart = true;
				initializePass();
			} else {
				completeWith(Optional.empty());
			}
		}

		private void completeWith(Optional<PersonRoute> completedResult) {
			result = completedResult;
			complete = true;
			if (cacheKey != null) {
				inFlightSearches.remove(cacheKey, this);
				routeCache.put(cacheKey, completedResult);
			}
		}
	}

	private void seedState(
		int state,
		double distance,
		double remainingDistance,
		double[] distances,
		int[] previous,
		PriorityQueue<SearchNode> open
	) {
		if (distance >= distances[state]) {
			return;
		}
		distances[state] = distance;
		previous[state] = -1;
		open.add(
			new SearchNode(
				state,
				distance,
				distance + remainingDistance / (Person.WALK_SPEED_PIXELS_PER_SECOND * 1.3)
			)
		);
	}

	private void expandState(
		int state,
		List<Point> destinations,
		List<Rectangle2D> obstacles,
		double[] distances,
		int[] previous,
		PersonRoute.Surface[] incomingSurface,
		Point[] roadTransitionByState,
		boolean[] settled,
		PriorityQueue<SearchNode> open
	) {
		int cell = state / 2;
		int mode = state % 2;
		int column = cell % TerrainMap.COLUMNS;
		int row = cell / TerrainMap.COLUMNS;
		Point position = positionForState(state);

		if (mode == WALKING && isOpenRoadCell(cell, obstacles)) {
			Point roadPoint = roadPointByCell[cell];
			if (canWalkSegment(position, roadPoint, obstacles)) {
				relax(
					state,
					cell * 2 + ON_ROAD,
					position.distance(roadPoint) / Person.WALK_SPEED_PIXELS_PER_SECOND,
					PersonRoute.Surface.WALKING,
					destinations,
					distances,
					previous,
					incomingSurface,
					roadTransitionByState,
					null,
					settled,
					open
				);
			}
		} else if (mode == ON_ROAD && isOpenWalkingCell(column, row, obstacles)) {
			Point walkingPoint = cellCenter(cell);
			if (
				minimumDistance(walkingPoint, destinations) <= MAX_OFFROAD_ACCESS_DISTANCE &&
				canWalkSegment(position, walkingPoint, obstacles)
			) {
				relax(
					state,
					cell * 2 + WALKING,
					position.distance(walkingPoint) / Person.WALK_SPEED_PIXELS_PER_SECOND,
					PersonRoute.Surface.WALKING,
					destinations,
					distances,
					previous,
					incomingSurface,
					roadTransitionByState,
					null,
					settled,
					open
				);
			}
		}

		for (int[] direction : DIRECTIONS) {
			int nextColumn = column + direction[0];
			int nextRow = row + direction[1];
			if (!terrain.isLandCell(nextColumn, nextRow)) {
				continue;
			}
			int nextCell = cellIndex(nextColumn, nextRow);
			int nextMode = mode;
			PersonRoute.Surface surface =
				mode == ON_ROAD ? PersonRoute.Surface.ROAD : PersonRoute.Surface.WALKING;
			Point roadJunction = null;
			if (mode == WALKING) {
				if (!canStep(column, row, nextColumn, nextRow, obstacles)) {
					continue;
				}
			} else if (!isOpenRoadCell(nextCell, obstacles)) {
				continue;
			} else if (roadByCell[cell] != roadByCell[nextCell]) {
				roadJunction = roadGraph.getSharedRoadConnection(
					roadByCell[cell],
					roadByCell[nextCell]
				);
				Point nextRoadPoint = roadPointByCell[nextCell];
				if (
					roadJunction == null ||
					!roadByCell[cell].hasEndpoint(roadJunction) ||
					!roadByCell[nextCell].hasEndpoint(roadJunction) ||
					!isClearRoadSegment(roadByCell[cell], position, roadJunction, obstacles) ||
					!isClearRoadSegment(
						roadByCell[nextCell],
						roadJunction,
						nextRoadPoint,
						obstacles
					)
				) {
					continue;
				}
			}

			int nextState = nextCell * 2 + nextMode;
			Point nextPosition = positionForState(nextState);
			if (
				roadJunction == null &&
				!isValidMovement(cell, nextCell, position, nextPosition, surface, obstacles)
			) {
				continue;
			}
			double speed = Person.WALK_SPEED_PIXELS_PER_SECOND * surface.speedMultiplier();
			double edgeDistance =
				roadJunction == null
					? position.distance(nextPosition)
					: position.distance(roadJunction) + roadJunction.distance(nextPosition);
			double edgeTime = edgeDistance / speed;
			relax(
				state,
				nextState,
				edgeTime,
				surface,
				destinations,
				distances,
				previous,
				incomingSurface,
				roadTransitionByState,
				roadJunction,
				settled,
				open
			);
		}
	}

	private boolean isValidMovement(
		int startCell,
		int endCell,
		Point start,
		Point end,
		PersonRoute.Surface surface,
		List<Rectangle2D> obstacles
	) {
		if (!terrain.containsSegment(start, end) || intersectsObstacle(start, end, obstacles)) {
			return false;
		}
		if (surface == PersonRoute.Surface.ROAD) {
			Road road = roadByCell[startCell];
			return (
				road != null &&
				road == roadByCell[endCell] &&
				road.containsPoint(start) &&
				road.containsPoint(end)
			);
		}
		return true;
	}

	private boolean isClearRoadSegment(
		Road road,
		Point start,
		Point end,
		List<Rectangle2D> obstacles
	) {
		return (
			road != null &&
			road.containsPoint(start) &&
			road.containsPoint(end) &&
			terrain.containsSegment(start, end) &&
			!intersectsObstacle(start, end, obstacles)
		);
	}

	private void relax(
		int currentState,
		int nextState,
		double edgeTime,
		PersonRoute.Surface surface,
		List<Point> destinations,
		double[] distances,
		int[] previous,
		PersonRoute.Surface[] incomingSurface,
		Point[] roadTransitionByState,
		Point roadJunction,
		boolean[] settled,
		PriorityQueue<SearchNode> open
	) {
		if (settled[nextState]) {
			return;
		}
		double candidateDistance = distances[currentState] + edgeTime;
		if (candidateDistance >= distances[nextState]) {
			return;
		}
		distances[nextState] = candidateDistance;
		previous[nextState] = currentState;
		incomingSurface[nextState] = surface;
		roadTransitionByState[nextState] = roadJunction;
		Point nextPosition = positionForState(nextState);
		double heuristic =
			minimumDistance(nextPosition, destinations) /
			(Person.WALK_SPEED_PIXELS_PER_SECOND * PersonRoute.Surface.ROAD.speedMultiplier());
		open.add(new SearchNode(nextState, candidateDistance, candidateDistance + heuristic));
	}

	private static double minimumDistance(Point position, List<Point> destinations) {
		double minimumDistance = Double.POSITIVE_INFINITY;
		for (Point destination : destinations) {
			minimumDistance = Math.min(minimumDistance, position.distance(destination));
		}
		return minimumDistance;
	}

	private PersonRoute reconstructRoute(
		Point start,
		Point destination,
		int finalState,
		int[] previous,
		PersonRoute.Surface[] incomingSurface,
		Point[] roadTransitionByState
	) {
		List<PersonRoute.Segment> reversedSegments = new ArrayList<>();
		int currentState = finalState;
		while (previous[currentState] >= 0) {
			int previousState = previous[currentState];
			Point previousPoint = positionForState(previousState);
			Point currentPoint = positionForState(currentState);
			Point roadJunction = roadTransitionByState[currentState];
			if (roadJunction != null) {
				reversedSegments.add(
					new PersonRoute.Segment(roadJunction, currentPoint, PersonRoute.Surface.ROAD)
				);
				reversedSegments.add(
					new PersonRoute.Segment(previousPoint, roadJunction, PersonRoute.Surface.ROAD)
				);
			} else if (!previousPoint.equals(currentPoint)) {
				reversedSegments.add(
					new PersonRoute.Segment(
						previousPoint,
						currentPoint,
						incomingSurface[currentState]
					)
				);
			}
			currentState = previousState;
		}
		java.util.Collections.reverse(reversedSegments);

		Point seedPosition = positionForState(currentState);
		List<PersonRoute.Segment> routeSegments = new ArrayList<>();
		if (!start.equals(seedPosition)) {
			routeSegments.add(
				new PersonRoute.Segment(start, seedPosition, PersonRoute.Surface.WALKING)
			);
		}
		routeSegments.addAll(reversedSegments);
		Point routeEnd = routeSegments.isEmpty() ? start : routeSegments.getLast().target();
		if (!routeEnd.equals(destination)) {
			routeSegments.add(
				new PersonRoute.Segment(routeEnd, destination, PersonRoute.Surface.WALKING)
			);
		}
		return new PersonRoute(routeSegments);
	}

	private Point positionForState(int state) {
		int cell = state / 2;
		if (state % 2 == WALKING) {
			return cellCenter(cell);
		}
		return roadPointByCell[cell];
	}

	private boolean isOpenWalkingCell(int column, int row, List<Rectangle2D> obstacles) {
		if (!terrain.isLandCell(column, row)) {
			return false;
		}
		Point position = cellCenter(cellIndex(column, row));
		return !containsObstacle(position, obstacles);
	}

	private boolean isOpenRoadCell(int cell, List<Rectangle2D> obstacles) {
		Point roadPoint = roadPointByCell[cell];
		return (
			roadByCell[cell] != null && roadPoint != null && !containsObstacle(roadPoint, obstacles)
		);
	}

	private void ensureRoadCache() {
		long graphVersion = roadGraph.getVersion();
		if (graphVersion == roadCacheVersion) {
			return;
		}
		int cellCount = TerrainMap.COLUMNS * TerrainMap.ROWS;
		roadByCell = new Road[cellCount];
		roadPointByCell = new Point[cellCount];
		List<Integer> roadCellIndices = new ArrayList<>();
		for (int row = 0; row < TerrainMap.ROWS; row++) {
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				int cell = cellIndex(column, row);
				if (!terrain.isLandCell(column, row)) {
					continue;
				}
				Point center = cellCenter(cell);
				Road road = roadGraph.findRoadNear(center, ROAD_ACCESS_DISTANCE);
				if (road != null) {
					roadByCell[cell] = road;
					roadPointByCell[cell] = roadGraph.closestPointOnRoad(center, road);
					roadCellIndices.add(cell);
				}
			}
		}
		roadCells = new int[roadCellIndices.size()];
		for (int index = 0; index < roadCellIndices.size(); index++) {
			roadCells[index] = roadCellIndices.get(index);
		}
		roadCacheVersion = graphVersion;
	}

	private List<Rectangle2D> buildingObstacles() {
		if (
			obstacleCache != null &&
			cachedHouseCount == houses.size() &&
			cachedResourceBuildingCount == resourceBuildings.size() &&
			cachedDepotCount == depots.size()
		) {
			return obstacleCache;
		}

		List<Rectangle2D> obstacles = new ArrayList<>(
			houses.size() + resourceBuildings.size() + depots.size()
		);
		for (House house : houses) {
			Point position = house.getPosition();
			obstacles.add(obstacle(position, House.HALF_SIZE));
		}
		for (ResourceBuilding building : resourceBuildings) {
			obstacles.add(obstacle(building.getPosition(), ResourceBuilding.HALF_SIZE));
		}
		for (Depot depot : depots) {
			obstacles.add(obstacle(depot.getPosition(), Depot.HALF_SIZE));
		}
		obstacleCache = List.copyOf(obstacles);
		cachedHouseCount = houses.size();
		cachedResourceBuildingCount = resourceBuildings.size();
		cachedDepotCount = depots.size();
		return obstacleCache;
	}

	private static Rectangle2D obstacle(Point center, int halfSize) {
		double extent = halfSize + Person.COLLISION_RADIUS;
		return new Rectangle2D.Double(center.x - extent, center.y - extent, extent * 2, extent * 2);
	}

	private boolean isWalkablePosition(Point position, List<Rectangle2D> obstacles) {
		return terrain.isLand(position) && !containsObstacle(position, obstacles);
	}

	private static boolean containsObstacle(Point point, List<Rectangle2D> obstacles) {
		for (Rectangle2D obstacle : obstacles) {
			if (obstacle.contains(point.x, point.y)) {
				return true;
			}
		}
		return false;
	}

	private boolean canWalkSegment(Point start, Point end, List<Rectangle2D> obstacles) {
		return terrain.containsSegment(start, end) && !intersectsObstacle(start, end, obstacles);
	}

	private static boolean intersectsObstacle(Point start, Point end, List<Rectangle2D> obstacles) {
		for (Rectangle2D obstacle : obstacles) {
			if (
				obstacle.contains(start.x, start.y) ||
				obstacle.contains(end.x, end.y) ||
				obstacle.intersectsLine(start.x, start.y, end.x, end.y)
			) {
				return true;
			}
		}
		return false;
	}

	private boolean canStep(
		int column,
		int row,
		int nextColumn,
		int nextRow,
		List<Rectangle2D> obstacles
	) {
		if (!isOpenWalkingCell(nextColumn, nextRow, obstacles)) {
			return false;
		}
		int deltaColumn = nextColumn - column;
		int deltaRow = nextRow - row;
		if (
			deltaColumn != 0 &&
			deltaRow != 0 &&
			(!isOpenWalkingCell(column + deltaColumn, row, obstacles) ||
				!isOpenWalkingCell(column, row + deltaRow, obstacles))
		) {
			return false;
		}
		return canWalkSegment(
			cellCenter(cellIndex(column, row)),
			cellCenter(cellIndex(nextColumn, nextRow)),
			obstacles
		);
	}

	private static int columnAt(int x) {
		return Math.floorDiv(x - TerrainMap.ORIGIN_X, TerrainMap.CELL_SIZE);
	}

	private static int rowAt(int y) {
		return Math.floorDiv(y - TerrainMap.ORIGIN_Y, TerrainMap.CELL_SIZE);
	}

	private static int cellIndex(int column, int row) {
		return row * TerrainMap.COLUMNS + column;
	}

	private static Point cellCenter(int cell) {
		int column = cell % TerrainMap.COLUMNS;
		int row = cell / TerrainMap.COLUMNS;
		return new Point(
			TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
			TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
		);
	}

	private record SearchNode(int state, double distance, double priority) {}

	private record SearchKey(
		PointKey start,
		List<PointKey> destinations,
		List<ObstacleKey> obstacles,
		long roadGraphVersion
	) {}

	private record PointKey(int x, int y) {}

	private record ObstacleKey(long x, long y, long width, long height) {}
}
