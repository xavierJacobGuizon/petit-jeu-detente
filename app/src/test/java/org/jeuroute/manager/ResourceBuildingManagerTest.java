package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;
import org.junit.jupiter.api.Test;

class ResourceBuildingManagerTest {

	@Test
	void generatedBuildingsStayOnLandAndConnectToTheRoadNetwork() {
		WorldMap worldMap = new WorldMap(new Random(42));
		Depot depot = worldMap.initializeDefaultLayout();
		ResourceBuildingManager manager = worldMap.getResourceBuildingManager();
		RoadGraph graph = worldMap.getRoadGraph();
		TerrainMap terrain = worldMap.getTerrainMap();

		assertEquals(ResourceBuildingManager.INITIAL_BUILDING_COUNT, manager.getBuildings().size());
		EnumSet<ResourceType> generatedResourceTypes = EnumSet.noneOf(ResourceType.class);
		for (int first = 0; first < manager.getBuildings().size(); first++) {
			ResourceBuilding building = manager.getBuildings().get(first);
			generatedResourceTypes.add(building.getResourceType());
			Point position = building.getPosition();
			assertTrue(terrain.isLand(position));
			assertTrue(graph.findRoadNear(position, ResourceBuilding.HALF_SIZE - 1.0) == null);
			assertTrue(terrain.isLand(building.getAccessPosition()));
			assertTrue(
				graph.findPath(building.getAccessPosition(), depot.getRoadEndPosition()).isPresent()
			);
			assertTrue(
				position.distance(depot.getPosition()) >=
					ResourceBuilding.HALF_SIZE + Depot.HALF_SIZE + 40
			);
			for (int second = first + 1; second < manager.getBuildings().size(); second++) {
				assertNotEquals(position, manager.getBuildings().get(second).getPosition());
				assertTrue(
					position.distance(manager.getBuildings().get(second).getPosition()) >=
						ResourceBuilding.HALF_SIZE * 2 + 40
				);
			}
		}
		assertEquals(EnumSet.allOf(ResourceType.class), generatedResourceTypes);
	}

	@Test
	void gameManagerProducesResourcesForItsInitialBuildings() {
		GameManager gameManager = new GameManager(new java.util.Random(42));
		List<ResourceBuilding> buildings = gameManager.getResourceBuildingManager().getBuildings();
		assertEquals(ResourceBuildingManager.INITIAL_BUILDING_COUNT, buildings.size());

		gameManager.update(ResourceType.FOOD.getProductionIntervalSeconds());

		for (ResourceBuilding building : buildings) {
			assertEquals(
				building.getResourceType() == ResourceType.FOOD ? 1 : 0,
				building.getStock()
			);
		}
	}

	@Test
	void stationsCanReadAndWithdrawStockFromCapturedBuildings() {
		ResourceBuildingManager manager = new ResourceBuildingManager(new Random(1));
		ResourceBuilding building = new ResourceBuilding(new Point(100, 100), ResourceType.FOOD);
		Station station = new Station(new Point(100 + (int) Station.CAPTURE_RADIUS, 100));
		assertTrue(manager.addBuilding(building));

		manager.update(ResourceType.FOOD.getProductionIntervalSeconds(), List.of(station));

		assertEquals(List.of(building), station.getCapturedBuildings());
		assertEquals(1, station.getAccessibleResourceStock(ResourceType.FOOD));
		assertEquals(Map.of(ResourceType.FOOD, 1), station.getCapturedResourceStocks());
		assertEquals(1, station.takeAccessibleResources(ResourceType.FOOD, 4));
		assertEquals(0, station.getAccessibleResourceStock(ResourceType.FOOD));
		assertEquals(Map.of(ResourceType.FOOD, 0), station.getCapturedResourceStocks());
	}

	@Test
	void buildingsOutsideCaptureRadiusAreNotAccessible() {
		ResourceBuildingManager manager = new ResourceBuildingManager(new Random(1));
		ResourceBuilding building = new ResourceBuilding(new Point(0, 0), ResourceType.METAL);
		Station station = new Station(new Point((int) Station.CAPTURE_RADIUS + 1, 0));
		assertTrue(manager.addBuilding(building));

		manager.update(ResourceType.METAL.getProductionIntervalSeconds(), List.of(station));

		assertTrue(station.getCapturedBuildings().isEmpty());
		assertEquals(0, station.getAccessibleResourceStock(ResourceType.METAL));
	}

	@Test
	void resourceTypesFormTheRequestedCircularDependency() {
		assertEquals(ResourceType.METAL, ResourceType.FOOD.getRequiredResourceType());
		assertEquals(ResourceType.FOOD, ResourceType.WOOD.getRequiredResourceType());
		assertEquals(ResourceType.WOOD, ResourceType.METAL.getRequiredResourceType());
	}

	@Test
	void stationDistributesOnlyResourcesRequestedByCapturedBuildings() {
		ResourceBuilding woodBuilding = new ResourceBuilding(
			new Point(100, 100),
			ResourceType.WOOD
		);
		Station station = new Station(new Point(100, 100));
		station.synchronizeCapturedBuildings(List.of(woodBuilding));

		assertEquals(Map.of(ResourceType.FOOD, 4), station.getCapturedResourceDemand());
		assertEquals(0, station.deliverResources(ResourceType.METAL, 2));
		assertEquals(0, woodBuilding.getReceivedResourceStock());
		assertEquals(4, station.deliverResources(ResourceType.FOOD, 6));
		assertEquals(4, woodBuilding.getReceivedResourceStock());
		assertEquals(0, station.getAccessibleResourceDemand(ResourceType.FOOD));
		assertTrue(station.getCapturedResourceDemand().isEmpty());
	}

	@Test
	void capturedStationSeesDemandRenewAfterBuildingConsumesInput() {
		ResourceBuilding woodBuilding = new ResourceBuilding(
			new Point(100, 100),
			ResourceType.WOOD
		);
		Station station = new Station(new Point(100, 100));
		station.synchronizeCapturedBuildings(List.of(woodBuilding));
		assertEquals(4, station.deliverResources(ResourceType.FOOD, 4));
		assertEquals(0, station.getAccessibleResourceDemand(ResourceType.FOOD));

		woodBuilding.update(ResourceBuilding.INPUT_CONSUMPTION_INTERVAL_SECONDS);

		assertEquals(1, station.getAccessibleResourceDemand(ResourceType.FOOD));
		assertEquals(Map.of(ResourceType.FOOD, 1), station.getCapturedResourceDemand());
	}
}
