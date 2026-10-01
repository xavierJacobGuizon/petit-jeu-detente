package org.jeuroute.model.jouet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import org.jeuroute.manager.LineManager;
import org.junit.jupiter.api.Test;

class VehicleResourceLoadingTest {

	@Test
	void vehicleLoadsKeepsUndemandedCargoAndDeliversOnlyAtMatchingStation() {
		RoadGraph graph = new RoadGraph();
		Station start = new Station(new Point(50, 100));
		Station middle = new Station(new Point(450, 100));
		Station end = new Station(new Point(850, 100));
		graph.addStationNode(start.getPosition());
		graph.addStationNode(middle.getPosition());
		graph.addStationNode(end.getPosition());
		Road road = graph.createRoad(start.getPosition(), middle.getPosition());
		graph.createRoad(middle.getPosition(), end.getPosition());
		ResourceBuilding woodBuilding = new ResourceBuilding(
			start.getPosition(),
			ResourceType.WOOD
		);
		ResourceBuilding foodBuilding = new ResourceBuilding(
			middle.getPosition(),
			ResourceType.FOOD
		);
		ResourceBuilding metalBuilding = new ResourceBuilding(
			end.getPosition(),
			ResourceType.METAL
		);
		woodBuilding.update(ResourceType.WOOD.getProductionIntervalSeconds() * 6.0);
		foodBuilding.update(ResourceType.FOOD.getProductionIntervalSeconds() * 3.0);
		start.synchronizeCapturedBuildings(List.of(woodBuilding));
		middle.synchronizeCapturedBuildings(List.of(foodBuilding));
		end.synchronizeCapturedBuildings(List.of(metalBuilding));
		TransitLine line = new LineManager(graph)
			.createLine(List.of(start, middle, end))
			.orElseThrow();
		Vehicle vehicle = new Vehicle(
			graph,
			road,
			10,
			100.0,
			start.getPosition(),
			middle.getPosition()
		);
		assertTrue(vehicle.assignLine(line));
		vehicle.update(0.0);
		assertTrue(vehicle.isWaitingForResources());
		assertEquals(0, vehicle.getCargoAmount());

		vehicle.update(0.49);
		assertEquals(0, vehicle.getCargoAmount());
		vehicle.update(0.02);
		assertEquals(1, vehicle.getCargoAmount());
		assertEquals(ResourceType.WOOD, vehicle.getCargoType());

		vehicle.update(1.5);
		assertEquals(Vehicle.MAX_CARGO_UNITS, vehicle.getCargoAmount());
		assertEquals(2, woodBuilding.getStock());
		assertFalse(vehicle.isWaitingForResources());

		for (
			int frame = 0;
			frame < 3600 && !middle.getPosition().equals(vehicle.getPosition());
			frame++
		) {
			vehicle.update(1.0 / 60.0);
		}
		assertEquals(middle.getPosition(), vehicle.getPosition());
		assertEquals(4, vehicle.getCargoAmount());
		assertEquals(0, foodBuilding.getReceivedResourceStock());
		assertFalse(vehicle.isWaitingForResources());

		for (
			int frame = 0;
			frame < 3600 && !end.getPosition().equals(vehicle.getPosition());
			frame++
		) {
			vehicle.update(1.0 / 60.0);
		}
		assertEquals(end.getPosition(), vehicle.getPosition());
		assertTrue(vehicle.isWaitingForResources());
		assertEquals(4, vehicle.getCargoAmount());

		vehicle.update(0.47);
		assertEquals(4, vehicle.getCargoAmount());
		vehicle.update(0.04);
		assertEquals(3, vehicle.getCargoAmount());
		assertEquals(ResourceType.WOOD, vehicle.getCargoType());
		assertEquals(1, metalBuilding.getReceivedResourceStock());

		vehicle.update(1.5);
		assertEquals(0, vehicle.getCargoAmount());
		assertEquals(4, metalBuilding.getReceivedResourceStock());
		assertFalse(vehicle.isWaitingForResources());
	}
}
