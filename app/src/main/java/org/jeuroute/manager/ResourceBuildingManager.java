package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;

public final class ResourceBuildingManager {

	public static final int INITIAL_BUILDING_COUNT = 12;
	private static final int GRID_SIZE = RoadGraph.GRID_SIZE;
	private static final int MAX_PLACEMENT_ATTEMPTS = 500;
	private static final int EXTRA_BUILDING_GAP = 40;
	private static final double ROAD_CLEARANCE = ResourceBuilding.HALF_SIZE + 12.0;
	private static final double MAX_ROAD_ACCESS_DISTANCE = GRID_SIZE * 8.0;
	private static final double ACCESS_OBSTACLE_CLEARANCE = ResourceBuilding.HALF_SIZE + 4.0;

	private final Random random;
	private final List<ResourceBuilding> buildings = new ArrayList<>();
	private final List<ResourceBuilding> buildingsView = Collections.unmodifiableList(buildings);

	public ResourceBuildingManager() {
		this(new Random());
	}

	public ResourceBuildingManager(Random random) {
		this.random = Objects.requireNonNull(random);
	}

	public List<ResourceBuilding> getBuildings() {
		return buildingsView;
	}

	public boolean addBuilding(ResourceBuilding building) {
		Objects.requireNonNull(building);
		for (ResourceBuilding existing : buildings) {
			if (
				building.getPosition().distance(existing.getPosition()) <
				ResourceBuilding.HALF_SIZE * 2 + EXTRA_BUILDING_GAP
			) {
				return false;
			}
		}
		buildings.add(building);
		return true;
	}

	public void generateInitialBuildings(TerrainMap terrain, RoadGraph graph, List<Depot> depots) {
		Objects.requireNonNull(terrain);
		Objects.requireNonNull(graph);
		Objects.requireNonNull(depots);
		if (!buildings.isEmpty()) {
			return;
		}

		ResourceType[] resourceTypes = ResourceType.values();

		for (int count = 0; count < INITIAL_BUILDING_COUNT; count++) {
			ResourceType resourceType =
				count < resourceTypes.length
					? resourceTypes[count]
					: resourceTypes[random.nextInt(resourceTypes.length)];
			for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
				Point position = new Point(
					TerrainMap.gridX(random.nextInt(TerrainMap.COLUMNS)),
					TerrainMap.gridY(random.nextInt(TerrainMap.ROWS))
				);
				if (!isValidPlacement(position, terrain, graph, depots)) {
					continue;
				}

				Road road = graph.findRoadNear(position, MAX_ROAD_ACCESS_DISTANCE);
				List<Point> accessRoute = createAccessRoute(position, road, graph);
				if (!isValidAccessRoute(accessRoute, terrain, graph, depots)) {
					continue;
				}

				Point accessPosition = accessRoute.getFirst();
				ResourceBuilding building = new ResourceBuilding(
					position,
					resourceType,
					accessPosition
				);
				buildings.add(building);
				for (int routeIndex = 1; routeIndex < accessRoute.size(); routeIndex++) {
					graph.createRoad(accessRoute.get(routeIndex - 1), accessRoute.get(routeIndex));
				}
				break;
			}
		}

		if (buildings.size() != INITIAL_BUILDING_COUNT) {
			throw new IllegalStateException("Unable to place all initial resource buildings");
		}
	}

	public void update(double deltaSeconds, List<Station> stations) {
		for (ResourceBuilding building : buildings) {
			building.update(deltaSeconds);
		}
		for (Station station : stations) {
			List<ResourceBuilding> capturedBuildings = buildings
				.stream()
				.filter(station::canAccess)
				.toList();
			station.synchronizeCapturedBuildings(capturedBuildings);
		}
	}

	private boolean isValidPlacement(
		Point position,
		TerrainMap terrain,
		RoadGraph graph,
		List<Depot> depots
	) {
		if (
			!terrain.isLand(position) ||
			graph.findRoadNear(position, ROAD_CLEARANCE) != null ||
			graph.findRoadNear(position, MAX_ROAD_ACCESS_DISTANCE) == null
		) {
			return false;
		}
		for (Depot depot : depots) {
			if (
				position.distance(depot.getPosition()) <
				ResourceBuilding.HALF_SIZE + Depot.HALF_SIZE + EXTRA_BUILDING_GAP
			) {
				return false;
			}
		}
		for (ResourceBuilding existing : buildings) {
			if (
				position.distance(existing.getPosition()) <
				ResourceBuilding.HALF_SIZE * 2 + EXTRA_BUILDING_GAP
			) {
				return false;
			}
		}
		return true;
	}

	private List<Point> createAccessRoute(Point buildingPosition, Road road, RoadGraph graph) {
		if (road == null) {
			return List.of();
		}

		Point roadPosition = RoadGraph.snapPoint(graph.closestPointOnRoad(buildingPosition, road));
		int directionX = Integer.compare(roadPosition.x, buildingPosition.x);
		int directionY = Integer.compare(roadPosition.y, buildingPosition.y);
		boolean horizontalRoad =
			Math.abs(road.getEnd().x - road.getStart().x) >=
			Math.abs(road.getEnd().y - road.getStart().y);
		Point accessPosition;
		Point bend;

		if (horizontalRoad && directionY != 0) {
			accessPosition = new Point(
				buildingPosition.x,
				buildingPosition.y + directionY * (ResourceBuilding.HALF_SIZE + 1)
			);
			bend = new Point(accessPosition.x, roadPosition.y);
		} else if (directionX != 0) {
			accessPosition = new Point(
				buildingPosition.x + directionX * (ResourceBuilding.HALF_SIZE + 1),
				buildingPosition.y
			);
			bend = new Point(roadPosition.x, accessPosition.y);
		} else if (directionY != 0) {
			accessPosition = new Point(
				buildingPosition.x,
				buildingPosition.y + directionY * (ResourceBuilding.HALF_SIZE + 1)
			);
			bend = new Point(accessPosition.x, roadPosition.y);
		} else {
			return List.of();
		}

		List<Point> route = new ArrayList<>();
		route.add(accessPosition);
		if (!bend.equals(accessPosition)) {
			route.add(bend);
		}
		if (!roadPosition.equals(route.getLast())) {
			route.add(roadPosition);
		}
		return route.size() > 1 ? List.copyOf(route) : List.of();
	}

	private boolean isValidAccessRoute(
		List<Point> route,
		TerrainMap terrain,
		RoadGraph graph,
		List<Depot> depots
	) {
		if (route.size() < 2) {
			return false;
		}
		for (int routeIndex = 1; routeIndex < route.size(); routeIndex++) {
			Point start = route.get(routeIndex - 1);
			Point end = route.get(routeIndex);
			if (!terrain.containsSegment(start, end) || !graph.isLandSegment(start, end)) {
				return false;
			}
			for (ResourceBuilding existing : buildings) {
				if (
					distanceToSegment(existing.getPosition(), start, end) <
					ACCESS_OBSTACLE_CLEARANCE
				) {
					return false;
				}
			}
			for (Depot depot : depots) {
				if (distanceToSegment(depot.getPosition(), start, end) < Depot.HALF_SIZE + 4.0) {
					return false;
				}
			}
		}
		return true;
	}

	private static double distanceToSegment(Point point, Point start, Point end) {
		double directionX = end.x - start.x;
		double directionY = end.y - start.y;
		double lengthSquared = directionX * directionX + directionY * directionY;
		if (lengthSquared == 0.0) {
			return point.distance(start);
		}
		double projection =
			((point.x - start.x) * directionX + (point.y - start.y) * directionY) / lengthSquared;
		projection = Math.max(0.0, Math.min(1.0, projection));
		double nearestX = start.x + projection * directionX;
		double nearestY = start.y + projection * directionY;
		return Point.distance(point.x, point.y, nearestX, nearestY);
	}
}
