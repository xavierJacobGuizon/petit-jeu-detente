package org.jeuroute.model.world.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.testing.SimulationTestClock;
import org.junit.jupiter.api.Test;

class ResourceBuildingTest {

	@Test
	void productionIsTimedAndStockNeverExceedsCapacity() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.FOOD);
		SimulationTestClock clock = new SimulationTestClock();
		double interval = ResourceType.FOOD.getProductionIntervalSeconds();

		clock.advanceSeconds(interval - 0.1, building::update);
		assertEquals(0, building.getStock());

		clock.advanceSeconds(0.1, building::update);
		assertEquals(1, building.getStock());

		clock.advanceSeconds(interval * 100.0, building::update);
		assertEquals(ResourceType.FOOD.getStorageCapacity(), building.getStock());
	}

	@Test
	void takingResourcesReturnsOnlyAvailableStock() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.WOOD);
		new SimulationTestClock().advanceSeconds(
			ResourceType.WOOD.getProductionIntervalSeconds() * 3.0,
			building::update
		);

		assertEquals(3, building.takeResource(8));
		assertEquals(0, building.getStock());
		assertEquals(0, building.takeResource(1));
	}

	@Test
	void consumingReceivedResourcesRenewsDemand() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.WOOD);
		SimulationTestClock clock = new SimulationTestClock();
		assertEquals(ResourceBuilding.INPUT_STORAGE_CAPACITY, building.receiveResource(9));
		assertEquals(0, building.getOutstandingDemand());

		clock.advanceSeconds(
			ResourceBuilding.INPUT_CONSUMPTION_INTERVAL_SECONDS - 0.1,
			building::update
		);
		assertEquals(4, building.getReceivedResourceStock());
		assertEquals(0, building.getOutstandingDemand());

		clock.advanceSeconds(0.1, building::update);
		assertEquals(3, building.getReceivedResourceStock());
		assertEquals(1, building.getOutstandingDemand());

		clock.advanceSeconds(
			ResourceBuilding.INPUT_CONSUMPTION_INTERVAL_SECONDS * 3.0,
			building::update
		);
		assertEquals(0, building.getReceivedResourceStock());
		assertEquals(ResourceBuilding.INPUT_STORAGE_CAPACITY, building.getOutstandingDemand());
	}
}
