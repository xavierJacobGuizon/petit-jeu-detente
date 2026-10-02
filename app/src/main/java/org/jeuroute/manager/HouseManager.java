package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;

public final class HouseManager {

	public static final int INITIAL_HOUSE_COUNT = 12;

	private static final int MAX_PLACEMENT_ATTEMPTS = 2_000;
	private static final double ROAD_CLEARANCE = House.HALF_SIZE + 5.0;
	private static final double RESOURCE_BUILDING_CLEARANCE =
		House.HALF_SIZE + ResourceBuilding.HALF_SIZE + 35.0;
	private static final double HOUSE_CLEARANCE = House.HALF_SIZE * 2 + 35.0;
	private static final double DEPOT_CLEARANCE = House.HALF_SIZE + Depot.HALF_SIZE + 35.0;
	private static final double MAX_ROAD_DISTANCE = RoadGraph.GRID_SIZE * 8.0;

	private final Random random;
	private final List<House> houses = new ArrayList<>();
	private final List<House> housesView = Collections.unmodifiableList(houses);

	public HouseManager(Random random) {
		this.random = Objects.requireNonNull(random);
	}

	public List<House> getHouses() {
		return housesView;
	}

	public void generateInitialHouses(
		TerrainMap terrain,
		RoadGraph graph,
		List<Depot> depots,
		List<ResourceBuilding> resourceBuildings
	) {
		Objects.requireNonNull(terrain);
		Objects.requireNonNull(graph);
		Objects.requireNonNull(depots);
		Objects.requireNonNull(resourceBuildings);
		if (!houses.isEmpty()) {
			return;
		}

		for (int houseIndex = 0; houseIndex < INITIAL_HOUSE_COUNT; houseIndex++) {
			boolean placed = false;
			for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
				Point position = new Point(
					TerrainMap.gridX(random.nextInt(TerrainMap.COLUMNS)) + TerrainMap.CELL_SIZE / 2,
					TerrainMap.gridY(random.nextInt(TerrainMap.ROWS)) + TerrainMap.CELL_SIZE / 2
				);
				if (!isValidPlacement(position, terrain, graph, depots, resourceBuildings)) {
					continue;
				}
				houses.add(new House(position));
				placed = true;
				break;
			}
			if (!placed) {
				throw new IllegalStateException("Unable to place all initial houses");
			}
		}
	}

	private boolean isValidPlacement(
		Point position,
		TerrainMap terrain,
		RoadGraph graph,
		List<Depot> depots,
		List<ResourceBuilding> resourceBuildings
	) {
		if (
			!terrain.isLand(position) ||
			graph.findRoadNear(position, ROAD_CLEARANCE) != null ||
			graph.findRoadNear(position, MAX_ROAD_DISTANCE) == null
		) {
			return false;
		}
		for (Depot depot : depots) {
			if (position.distance(depot.getPosition()) < DEPOT_CLEARANCE) {
				return false;
			}
		}
		for (ResourceBuilding building : resourceBuildings) {
			if (position.distance(building.getPosition()) < RESOURCE_BUILDING_CLEARANCE) {
				return false;
			}
		}
		for (House house : houses) {
			if (position.distance(house.getPosition()) < HOUSE_CLEARANCE) {
				return false;
			}
		}
		return true;
	}
}
