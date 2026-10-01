package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jeuroute.model.jouet.Depot;
import org.jeuroute.model.jouet.ResourceBuilding;
import org.jeuroute.model.jouet.ResourceType;
import org.jeuroute.model.jouet.RoadGraph;
import org.jeuroute.model.jouet.Station;
import org.junit.jupiter.api.Test;

class ResourceBuildingManagerTest {

	@Test
	void initialBuildingsAreUniqueInsideTheLargerSpawnArea() {
		ResourceBuildingManager manager = new ResourceBuildingManager(new Random(42));
		RoadGraph graph = new RoadGraph();
		graph.createRoad(new Point(50, 360), new Point(123, 360));
		graph.createRoad(new Point(50, 520), new Point(123, 520));
		graph.createRoad(new Point(125, 350), new Point(125, 525));
		Depot depot = new Depot(new Point(75, 275));
		graph.createRoad(depot.getAccessPosition(), depot.getRoadEndPosition());
		manager.generateInitialBuildings(graph, List.of(depot));

		assertEquals(ResourceBuildingManager.INITIAL_BUILDING_COUNT, manager.getBuildings().size());
		EnumSet<ResourceType> generatedResourceTypes = EnumSet.noneOf(ResourceType.class);
		for (int first = 0; first < manager.getBuildings().size(); first++) {
			generatedResourceTypes.add(manager.getBuildings().get(first).getResourceType());
			Point position = manager.getBuildings().get(first).getPosition();
			assertTrue(
				position.x >=
					ResourceBuildingManager.INITIAL_VIEW_CENTER_X -
						ResourceBuildingManager.SPAWN_AREA_WIDTH / 2 +
						ResourceBuilding.HALF_SIZE
			);
			assertTrue(
				position.x <=
					ResourceBuildingManager.INITIAL_VIEW_CENTER_X +
						ResourceBuildingManager.SPAWN_AREA_WIDTH / 2 -
						ResourceBuilding.HALF_SIZE
			);
			assertTrue(position.y >= ResourceBuildingManager.INITIAL_VIEW_CENTER_Y - 516);
			assertTrue(position.y <= ResourceBuildingManager.INITIAL_VIEW_CENTER_Y + 516);
			assertTrue(graph.findRoadNear(position, ResourceBuilding.HALF_SIZE + 12.0) == null);
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
		GameManager gameManager = new GameManager();
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
