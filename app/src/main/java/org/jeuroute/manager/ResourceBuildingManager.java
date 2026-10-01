package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jeuroute.model.jouet.Depot;
import org.jeuroute.model.jouet.ResourceBuilding;
import org.jeuroute.model.jouet.ResourceType;
import org.jeuroute.model.jouet.RoadGraph;
import org.jeuroute.model.jouet.Station;

public final class ResourceBuildingManager {

	public static final int INITIAL_BUILDING_COUNT = 12;
	public static final int SPAWN_AREA_WIDTH = 1920;
	public static final int SPAWN_AREA_HEIGHT = 1080;
	public static final int INITIAL_VIEW_CENTER_X = 640;
	public static final int INITIAL_VIEW_CENTER_Y = 360;

	private static final int GRID_SIZE = RoadGraph.GRID_SIZE;
	private static final int MAX_PLACEMENT_ATTEMPTS = 500;
	private static final int EXTRA_BUILDING_GAP = 40;
	private static final double ROAD_CLEARANCE = ResourceBuilding.HALF_SIZE + 12.0;

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

	public void generateInitialBuildings(RoadGraph graph, List<Depot> depots) {
		Objects.requireNonNull(graph);
		Objects.requireNonNull(depots);
		if (!buildings.isEmpty()) {
			return;
		}

		int halfWidth = SPAWN_AREA_WIDTH / 2;
		int halfHeight = SPAWN_AREA_HEIGHT / 2;
		int minimumX = roundUpToGrid(
			INITIAL_VIEW_CENTER_X - halfWidth + ResourceBuilding.HALF_SIZE
		);
		int maximumX = roundDownToGrid(
			INITIAL_VIEW_CENTER_X + halfWidth - ResourceBuilding.HALF_SIZE
		);
		int minimumY = roundUpToGrid(
			INITIAL_VIEW_CENTER_Y - halfHeight + ResourceBuilding.HALF_SIZE
		);
		int maximumY = roundDownToGrid(
			INITIAL_VIEW_CENTER_Y + halfHeight - ResourceBuilding.HALF_SIZE
		);
		ResourceType[] resourceTypes = ResourceType.values();

		for (int count = 0; count < INITIAL_BUILDING_COUNT; count++) {
			ResourceType resourceType =
				count < resourceTypes.length
					? resourceTypes[count]
					: resourceTypes[random.nextInt(resourceTypes.length)];
			for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
				Point position = new Point(
					randomGridPosition(minimumX, maximumX),
					randomGridPosition(minimumY, maximumY)
				);
				if (!isValidPlacement(position, graph, depots)) {
					continue;
				}
				buildings.add(new ResourceBuilding(position, resourceType));
				break;
			}
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

	private boolean isValidPlacement(Point position, RoadGraph graph, List<Depot> depots) {
		if (graph.findRoadNear(position, ROAD_CLEARANCE) != null) {
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

	private int randomGridPosition(int minimum, int maximum) {
		int gridCount = (maximum - minimum) / GRID_SIZE + 1;
		return minimum + random.nextInt(gridCount) * GRID_SIZE;
	}

	private static int roundUpToGrid(int coordinate) {
		return (int) Math.ceil(coordinate / (double) GRID_SIZE) * GRID_SIZE;
	}

	private static int roundDownToGrid(int coordinate) {
		return (int) Math.floor(coordinate / (double) GRID_SIZE) * GRID_SIZE;
	}
}
