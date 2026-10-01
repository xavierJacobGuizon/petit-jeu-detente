package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.jeuroute.model.world.Depot;
import org.jeuroute.model.world.ResourceBuilding;
import org.jeuroute.model.world.ResourceType;
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
	}

	@Test
	void gameManagerAccessorsDelegateToItsWorldMap() {
		GameManager gameManager = new GameManager();

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
}
