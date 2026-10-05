package org.jeuroute.model.world.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;
import org.jeuroute.manager.LineManager;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.testing.SimulationTestClock;
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
		SimulationTestClock buildingClock = new SimulationTestClock();
		buildingClock.advanceSeconds(
			ResourceType.WOOD.getProductionIntervalSeconds() * 6.0,
			woodBuilding::update
		);
		buildingClock.advanceSeconds(
			ResourceType.FOOD.getProductionIntervalSeconds() * 3.0,
			foodBuilding::update
		);
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
		SimulationTestClock vehicleClock = new SimulationTestClock();
		vehicleClock.step(vehicle::update);
		assertTrue(vehicle.isWaitingForResources());
		assertEquals(0, vehicle.getCargoAmount());

		vehicleClock.advanceSeconds(0.49, vehicle::update);
		assertEquals(0, vehicle.getCargoAmount());
		vehicleClock.advanceSeconds(0.02, vehicle::update);
		assertEquals(1, vehicle.getCargoAmount());
		assertEquals(ResourceType.WOOD, vehicle.getCargoType());

		vehicleClock.advanceSeconds(1.5, vehicle::update);
		assertEquals(Vehicle.MAX_CARGO_UNITS, vehicle.getCargoAmount());
		assertEquals(2, woodBuilding.getStock());
		assertFalse(vehicle.isWaitingForResources());

		for (
			int frame = 0;
			frame < 3600 && !middle.getPosition().equals(vehicle.getPosition());
			frame++
		) {
			vehicleClock.step(vehicle::update);
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
			vehicleClock.step(vehicle::update);
		}
		assertEquals(end.getPosition(), vehicle.getPosition());
		assertTrue(vehicle.isWaitingForResources());
		assertEquals(4, vehicle.getCargoAmount());

		vehicleClock.advanceSeconds(0.47, vehicle::update);
		assertEquals(4, vehicle.getCargoAmount());
		vehicleClock.advanceSeconds(0.04, vehicle::update);
		assertEquals(3, vehicle.getCargoAmount());
		assertEquals(ResourceType.WOOD, vehicle.getCargoType());
		assertEquals(1, metalBuilding.getReceivedResourceStock());

		vehicleClock.advanceSeconds(1.5, vehicle::update);
		assertEquals(0, vehicle.getCargoAmount());
		assertEquals(4, metalBuilding.getReceivedResourceStock());
		assertFalse(vehicle.isWaitingForResources());
	}
}
