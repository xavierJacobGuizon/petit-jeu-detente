package org.jeuroute.manager;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.transport.Station;

/** Owns the persistent world state and its graph-derived entities. */
public final class WorldMap {

	private final RoadGraph roadGraph = new RoadGraph();
	private final FixedEntityManager fixedEntityManager = new FixedEntityManager();
	private final ResourceBuildingManager resourceBuildingManager = new ResourceBuildingManager();
	private long synchronizedGraphVersion = -1;
	private boolean defaultLayoutInitialized;

	public RoadGraph getRoadGraph() {
		return roadGraph;
	}

	public FixedEntityManager getFixedEntityManager() {
		return fixedEntityManager;
	}

	public ResourceBuildingManager getResourceBuildingManager() {
		return resourceBuildingManager;
	}

	public Depot initializeDefaultLayout() {
		if (defaultLayoutInitialized) {
			return fixedEntityManager.getDepots().getFirst();
		}

		roadGraph.createRoad(new Point(50, 360), new Point(123, 360));
		roadGraph.createRoad(new Point(50, 520), new Point(123, 520));
		roadGraph.createRoad(new Point(125, 350), new Point(125, 525));
		Depot depot = fixedEntityManager.createDepot(new Point(75, 275));
		roadGraph.createRoad(depot.getAccessPosition(), depot.getRoadEndPosition());
		resourceBuildingManager.generateInitialBuildings(roadGraph, fixedEntityManager.getDepots());
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
}
