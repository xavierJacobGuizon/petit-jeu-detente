package org.jeuroute.model.jouet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import org.junit.jupiter.api.Test;

class ResourceBuildingTest {

	@Test
	void productionIsTimedAndStockNeverExceedsCapacity() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.FOOD);
		double interval = ResourceType.FOOD.getProductionIntervalSeconds();

		building.update(interval - 0.1);
		assertEquals(0, building.getStock());

		building.update(0.1);
		assertEquals(1, building.getStock());

		building.update(interval * 100.0);
		assertEquals(ResourceType.FOOD.getStorageCapacity(), building.getStock());
	}

	@Test
	void takingResourcesReturnsOnlyAvailableStock() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.WOOD);
		building.update(ResourceType.WOOD.getProductionIntervalSeconds() * 3.0);

		assertEquals(3, building.takeResource(8));
		assertEquals(0, building.getStock());
		assertEquals(0, building.takeResource(1));
	}

	@Test
	void consumingReceivedResourcesRenewsDemand() {
		ResourceBuilding building = new ResourceBuilding(new Point(100, 200), ResourceType.WOOD);
		assertEquals(ResourceBuilding.INPUT_STORAGE_CAPACITY, building.receiveResource(9));
		assertEquals(0, building.getOutstandingDemand());

		building.update(ResourceBuilding.INPUT_CONSUMPTION_INTERVAL_SECONDS - 0.1);
		assertEquals(4, building.getReceivedResourceStock());
		assertEquals(0, building.getOutstandingDemand());

		building.update(0.1);
		assertEquals(3, building.getReceivedResourceStock());
		assertEquals(1, building.getOutstandingDemand());

		building.update(ResourceBuilding.INPUT_CONSUMPTION_INTERVAL_SECONDS * 3.0);
		assertEquals(0, building.getReceivedResourceStock());
		assertEquals(ResourceBuilding.INPUT_STORAGE_CAPACITY, building.getOutstandingDemand());
	}
}
