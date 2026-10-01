package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jeuroute.model.records.manager.VehiclePlacement;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.transport.Vehicle;

public class VehicleManager {

	private final RoadGraph graph;
	private final List<Point> depotAccessPositions = new ArrayList<>();
	private final List<Vehicle> vehicles = new java.util.ArrayList<>();
	private final List<Vehicle> vehiclesView = java.util.Collections.unmodifiableList(vehicles);
	private final Map<Vehicle, Integer> vehicleNumbers = new IdentityHashMap<>();
	private int nextVehicleNumber = 1;

	public VehicleManager(RoadGraph graph) {
		this(graph, null);
	}

	public VehicleManager(RoadGraph graph, Point depotAccessPosition) {
		this.graph = graph;
		if (depotAccessPosition != null) {
			addDepotAccessPosition(depotAccessPosition);
		}
	}

	public void addDepotAccessPosition(Point position) {
		Point copy = new Point(position);
		if (depotAccessPositions.contains(copy)) {
			return;
		}
		depotAccessPositions.add(copy);
		for (Vehicle vehicle : vehicles) {
			vehicle.addDepotAccessPosition(copy);
		}
	}

	public void addVehicle(Vehicle vehicle) {
		vehicles.add(vehicle);
		vehicleNumbers.put(vehicle, nextVehicleNumber++);
	}

	public int getVehicleNumber(Vehicle vehicle) {
		Integer number = vehicleNumbers.get(vehicle);
		if (number == null) {
			throw new IllegalArgumentException("Vehicle is not managed by this manager");
		}
		return number;
	}

	public List<Vehicle> getVehicles() {
		return vehiclesView;
	}

	public List<Vehicle> getUnassignedVehicles() {
		return vehicles
			.stream()
			.filter(vehicle -> !vehicle.isAssignedToLine())
			.toList();
	}

	public void clearVehicle() {
		vehicles.clear();
		vehicleNumbers.clear();
		nextVehicleNumber = 1;
	}

	public Vehicle createVehicle(Road road, double size, double speed, Point start, Point end) {
		return createVehicle(road, size, speed, Vehicle.DEFAULT_POWER_WATTS, start, end);
	}

	public Vehicle createVehicle(
		Road road,
		double size,
		double maxSpeed,
		double powerWatts,
		Point start,
		Point end
	) {
		Vehicle vehicle = new Vehicle(
			graph,
			road,
			size,
			maxSpeed,
			powerWatts,
			start,
			end,
			depotAccessPositions
		);
		addVehicle(vehicle);
		return vehicle;
	}

	public void createVehicleAt(Point position) {
		VehiclePlacement placement = resolvePlacement(position);
		if (placement == null) {
			return;
		}
		addVehicle(
			new Vehicle(
				graph,
				placement.road(),
				24,
				180,
				180_000.0,
				placement.position(),
				placement.target(),
				depotAccessPositions
			)
		);
	}

	public PlacementPreview getPlacementPreview(Point position) {
		VehiclePlacement placement = resolvePlacement(position);
		if (placement == null) {
			return new PlacementPreview(PlacementPreviewType.VEHICLE, position, false);
		}
		return new PlacementPreview(PlacementPreviewType.VEHICLE, placement.position(), true);
	}

	private VehiclePlacement resolvePlacement(Point position) {
		Road road = graph.findRoadNear(position, 12.0);
		if (road == null) {
			return null;
		}
		Point roadPosition = graph.closestPointOnRoad(position, road);
		Point target = road.endpointInDirection(roadPosition, road.getEnd());
		if (roadPosition.equals(target)) {
			target = road.otherEndpoint(target);
		}
		return roadPosition.equals(target)
			? null
			: new VehiclePlacement(road, roadPosition, target);
	}
}
