package org.jeuroute.manager.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import org.jeuroute.manager.GameManager;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;
import org.junit.jupiter.api.Test;

class WorldMapTest {

	@Test
	void defaultLayoutCreatesAndOwnsTheWorldContents() {
		WorldMap worldMap = new WorldMap();

		Depot depot = worldMap.initializeDefaultLayout();

		assertSame(depot, worldMap.getDepots().getFirst());
		assertEquals(
			ResourceBuildingManager.INITIAL_BUILDING_COUNT,
			worldMap.getResourceBuildingManager().getBuildings().size()
		);
		assertEquals(
			HouseManager.INITIAL_HOUSE_COUNT,
			worldMap.getHouseManager().getHouses().size()
		);
		assertTrue(!worldMap.getRoadGraph().getRoads().isEmpty());
		EnumSet<ResourceType> generatedTypes = EnumSet.noneOf(ResourceType.class);
		for (ResourceBuilding building : worldMap.getResourceBuildingManager().getBuildings()) {
			generatedTypes.add(building.getResourceType());
		}
		assertEquals(EnumSet.allOf(ResourceType.class), generatedTypes);
		assertSame(depot, worldMap.initializeDefaultLayout());
		assertEquals(
			ResourceBuildingManager.INITIAL_BUILDING_COUNT,
			worldMap.getResourceBuildingManager().getBuildings().size()
		);
		assertEquals(
			HouseManager.INITIAL_HOUSE_COUNT,
			worldMap.getHouseManager().getHouses().size()
		);
	}

	@Test
	void gameManagerAccessorsDelegateToItsWorldMap() {
		GameManager gameManager = new GameManager(new java.util.Random(42));

		assertSame(gameManager.getWorldMap().getRoadGraph(), gameManager.getRoadGraph());
		assertSame(
			gameManager.getWorldMap().getFixedEntityManager(),
			gameManager.getFixedEntityManager()
		);
		assertSame(
			gameManager.getWorldMap().getResourceBuildingManager(),
			gameManager.getResourceBuildingManager()
		);
	}

	@Test
	void seededIslandHasAConnectedRoadNetworkAndBuildingAccesses() {
		WorldMap firstMap = new WorldMap(new Random(91));
		Depot depot = firstMap.initializeDefaultLayout();
		WorldMap secondMap = new WorldMap(new Random(91));
		secondMap.initializeDefaultLayout();

		List<List<Point>> firstRoads = roadEndpoints(firstMap);
		assertEquals(firstRoads, roadEndpoints(secondMap));
		assertEquals(
			firstMap
				.getResourceBuildingManager()
				.getBuildings()
				.stream()
				.map(ResourceBuilding::getPosition)
				.toList(),
			secondMap
				.getResourceBuildingManager()
				.getBuildings()
				.stream()
				.map(ResourceBuilding::getPosition)
				.toList()
		);

		Point networkStart = depot.getRoadEndPosition();
		for (Road road : firstMap.getRoadGraph().getRoads()) {
			assertTrue(firstMap.getTerrainMap().containsSegment(road.getStart(), road.getEnd()));
			assertTrue(firstMap.getRoadGraph().findPath(networkStart, road.getStart()).isPresent());
		}
		for (ResourceBuilding building : firstMap.getResourceBuildingManager().getBuildings()) {
			assertTrue(
				firstMap
					.getRoadGraph()
					.findPath(networkStart, building.getAccessPosition())
					.isPresent()
			);
		}
		for (House house : firstMap.getHouseManager().getHouses()) {
			assertTrue(firstMap.getTerrainMap().isLand(house.getPosition()));
			assertTrue(
				firstMap.getRoadGraph().findRoadNear(house.getPosition(), House.HALF_SIZE - 1.0) ==
					null
			);
		}
	}

	@Test
	void fixedEntityPlacementPreviewsRejectWater() {
		WorldMap worldMap = new WorldMap(new Random(17));
		worldMap.initializeDefaultLayout();
		Point water = new Point(TerrainMap.ORIGIN_X, TerrainMap.ORIGIN_Y);

		assertFalse(worldMap.getFixedEntityManager().getStationPlacementPreview(water).valid());
		assertFalse(worldMap.getFixedEntityManager().getDepotPlacementPreview(water).valid());
	}

	private static List<List<Point>> roadEndpoints(WorldMap worldMap) {
		return worldMap
			.getRoadGraph()
			.getRoads()
			.stream()
			.map(road -> List.of(road.getStart(), road.getEnd()))
			.toList();
	}
}
