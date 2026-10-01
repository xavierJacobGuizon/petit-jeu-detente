package org.jeuroute.manager;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.terrain.generation.IslandTerrainGenerator;
import org.jeuroute.model.world.terrain.generation.TerrainGenerator;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;

/** Owns the persistent world state and its graph-derived entities. */
public final class WorldMap {

	private final Random random;
	private final TerrainMap terrainMap;
	private final RoadGraph roadGraph;
	private final FixedEntityManager fixedEntityManager;
	private final ResourceBuildingManager resourceBuildingManager;
	private long synchronizedGraphVersion = -1;
	private boolean defaultLayoutInitialized;

	public WorldMap() {
		this(new IslandTerrainGenerator(), new Random());
	}

	public WorldMap(Random random) {
		this(new IslandTerrainGenerator(), random);
	}

	public WorldMap(TerrainGenerator terrainGenerator) {
		this(terrainGenerator, new Random());
	}

	public WorldMap(TerrainGenerator terrainGenerator, Random random) {
		this.random = Objects.requireNonNull(random);
		TerrainGenerator generator = Objects.requireNonNull(terrainGenerator);
		TerrainMap generatedTerrain = Objects.requireNonNull(
			generator.generate(random),
			"terrain generator cannot return null"
		);
		if (generatedTerrain.getType() != generator.type()) {
			throw new IllegalArgumentException(
				"Terrain generator returned a map with the wrong type"
			);
		}
		terrainMap = generatedTerrain;
		roadGraph = new RoadGraph(terrainMap);
		fixedEntityManager = new FixedEntityManager(roadGraph);
		resourceBuildingManager = new ResourceBuildingManager(random);
	}

	public RoadGraph getRoadGraph() {
		return roadGraph;
	}

	public FixedEntityManager getFixedEntityManager() {
		return fixedEntityManager;
	}

	public ResourceBuildingManager getResourceBuildingManager() {
		return resourceBuildingManager;
	}

	public TerrainMap getTerrainMap() {
		return terrainMap;
	}

	public Depot initializeDefaultLayout() {
		if (defaultLayoutInitialized) {
			return fixedEntityManager.getDepots().getFirst();
		}

		generateInitialRoadNetwork();
		Depot depot = fixedEntityManager.createDepot(new Point(75, 275));
		roadGraph.createRoad(depot.getAccessPosition(), depot.getRoadEndPosition());
		resourceBuildingManager.generateInitialBuildings(
			terrainMap,
			roadGraph,
			fixedEntityManager.getDepots()
		);
		synchronizeGraphEntities();
		defaultLayoutInitialized = true;
		return depot;
	}

	public Optional<Depot> createDepot(Point position) {
		Objects.requireNonNull(position);
		if (!fixedEntityManager.getDepotPlacementPreview(position).valid()) {
			return Optional.empty();
		}
		Depot depot = fixedEntityManager.createDepot(position);
		roadGraph.createRoad(depot.getAccessPosition(), depot.getRoadEndPosition());
		return Optional.of(depot);
	}

	public boolean synchronizeGraphEntities() {
		long graphVersion = roadGraph.getVersion();
		if (synchronizedGraphVersion == graphVersion) {
			return false;
		}
		fixedEntityManager.synchronizeIntersections(
			roadGraph.getIntersectionPositions(),
			roadGraph
		);
		fixedEntityManager.synchronizeStations(roadGraph);
		synchronizedGraphVersion = graphVersion;
		return true;
	}

	public void update(double deltaSeconds) {
		resourceBuildingManager.update(deltaSeconds, fixedEntityManager.getStations());
	}

	public List<Station> getStations() {
		return fixedEntityManager.getStations();
	}

	public List<Depot> getDepots() {
		return fixedEntityManager.getDepots();
	}

	private void generateInitialRoadNetwork() {
		for (int row : spacedGridIndices(TerrainMap.ROWS / 2, TerrainMap.ROWS)) {
			int firstColumn = terrainMap.getFirstLandColumn(row);
			int lastColumn = terrainMap.getLastLandColumn(row);
			if (lastColumn - firstColumn >= 8) {
				roadGraph.createRoad(
					new Point(TerrainMap.gridX(firstColumn), TerrainMap.gridY(row)),
					new Point(TerrainMap.gridX(lastColumn), TerrainMap.gridY(row))
				);
			}
		}

		for (int column : spacedGridIndices(TerrainMap.COLUMNS / 2, TerrainMap.COLUMNS)) {
			int firstRow = terrainMap.getFirstLandRow(column);
			int lastRow = terrainMap.getLastLandRow(column);
			if (lastRow - firstRow >= 8) {
				roadGraph.createRoad(
					new Point(TerrainMap.gridX(column), TerrainMap.gridY(firstRow)),
					new Point(TerrainMap.gridX(column), TerrainMap.gridY(lastRow))
				);
			}
		}
	}

	private List<Integer> spacedGridIndices(int center, int limit) {
		LinkedHashSet<Integer> indices = new LinkedHashSet<>();
		indices.add(center);
		for (int spacing = 8; spacing <= 16; spacing += 8) {
			for (int direction : new int[] { -1, 1 }) {
				int jitter = random.nextInt(3) - 1;
				int index = center + direction * spacing + jitter;
				if (index >= 0 && index < limit) {
					indices.add(index);
				}
			}
		}
		return indices.stream().sorted().toList();
	}
}
