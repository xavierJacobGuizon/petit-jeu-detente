package org.jeuroute.model.world.transport;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.records.world.RoadLeg;
import org.jeuroute.model.records.world.VehicleMotionStep;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.network.RoadPath;
import org.jeuroute.model.world.network.RoadPosition;
import org.jeuroute.model.world.skin.VehicleSkin;
import org.jeuroute.utils.GeometryUtils;

public class Vehicle {

	public static final double REFERENCE_MASS_KG = 1_000.0;
	public static final double METERS_PER_PIXEL = 0.1;
	public static final double DEFAULT_POWER_WATTS = 900_000.0;
	public static final int MAX_CARGO_UNITS = 4;
	public static final double RESOURCE_TRANSFER_SECONDS_PER_UNIT = 0.5;
	private static final long RESOURCE_TRANSFER_TICKS = SimulationTick.ticksForSeconds(
		RESOURCE_TRANSFER_SECONDS_PER_UNIT
	);
	private static final double DISTANCE_EPSILON = 0.000001;

	private final VehicleSkin vehicleSkin;

	private final double size;
	public final double halfSize;
	private final double maxSpeed;
	private final double powerWatts;
	private final RoadGraph roadGraph;
	private final List<Point> depotAccessPositions = new ArrayList<>();
	private double currentSpeed;

	private Road road;

	// L'objet RoadPosition encapsule la logique de positionnement sur une route, y
	// compris le point de départ, le point cible et la distance parcourue.
	private RoadPosition roadPosition;

	private Point position;
	private long synchronizedGraphVersion;
	private TransitLine assignedLine;
	private RoadPath activePath;
	private int pathLegIndex;
	private int lineSegmentIndex;
	private boolean approachingLine;
	private boolean returningToStart;
	private boolean replanningCurrentLeg;
	private boolean parkedAtStation;
	private boolean parkedAtDepot;
	private boolean parkingPathAttempted;
	private ResourceType cargoType;
	private int cargoAmount;
	private Station loadingStation;
	private ResourceType loadingResourceType;
	private long resourceTransferElapsedTicks;
	private boolean unloadingResources;

	public Vehicle(
		RoadGraph roadGraph,
		Road road,
		double size,
		double speed,
		Point start,
		Point end
	) {
		this(roadGraph, road, size, speed, DEFAULT_POWER_WATTS, start, end, List.of());
	}

	public Vehicle(
		RoadGraph roadGraph,
		Road road,
		double size,
		double speed,
		Point start,
		Point end,
		List<Point> depotAccessPositions
	) {
		this(roadGraph, road, size, speed, DEFAULT_POWER_WATTS, start, end, depotAccessPositions);
	}

	public Vehicle(
		RoadGraph roadGraph,
		Road road,
		double size,
		double maxSpeed,
		double powerWatts,
		Point start,
		Point end
	) {
		this(roadGraph, road, size, maxSpeed, powerWatts, start, end, List.of());
	}

	public Vehicle(
		RoadGraph roadGraph,
		Road road,
		double size,
		double maxSpeed,
		double powerWatts,
		Point start,
		Point end,
		List<Point> depotAccessPositions
	) {
		if (!Double.isFinite(maxSpeed) || maxSpeed <= 0.0) {
			throw new IllegalArgumentException("Maximum speed must be positive and finite");
		}
		if (!Double.isFinite(powerWatts) || powerWatts <= 0.0) {
			throw new IllegalArgumentException("Power must be positive and finite");
		}
		this.roadGraph = roadGraph;
		this.road = road;
		this.maxSpeed = maxSpeed;
		this.powerWatts = powerWatts;
		for (Point depotAccessPosition : depotAccessPositions) {
			this.depotAccessPositions.add(new Point(depotAccessPosition));
		}
		this.size = size;
		this.halfSize = size / 2.0;
		this.setPosition(road, start, end);

		this.synchronizedGraphVersion = roadGraph.getVersion();

		vehicleSkin = new VehicleSkin(1.0f, 0.35f, 0.15f);
		// 0.75f, 0.30f, 0.85f
	}

	public Point getPosition() {
		return new Point(position);
	}

	public int getPositionX() {
		return position.x;
	}

	public int getPositionY() {
		return position.y;
	}

	public Road getRoad() {
		return road;
	}

	public double getSize() {
		return size;
	}

	public double getSpeed() {
		return currentSpeed;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public double getPowerWatts() {
		return powerWatts;
	}

	public RoadPosition getRoadPosition() {
		return roadPosition;
	}

	public Point getTarget() {
		return roadPosition == null ? null : roadPosition.getTarget();
	}

	public Point getDestination() {
		if (activePath == null || activePath.isEmpty()) {
			return null;
		}
		return activePath.getLegs().getLast().target();
	}

	public TransitLine getAssignedLine() {
		return assignedLine;
	}

	public boolean isAssignedToLine() {
		return assignedLine != null;
	}

	public boolean isParkedAtStation() {
		return parkedAtStation;
	}

	public boolean isParkedAtDepot() {
		return parkedAtDepot;
	}

	public ResourceType getCargoType() {
		return cargoType;
	}

	public int getCargoAmount() {
		return cargoAmount;
	}

	public boolean isWaitingForResources() {
		return loadingStation != null;
	}

	public void addDepotAccessPosition(Point accessPosition) {
		Point copy = new Point(accessPosition);
		if (depotAccessPositions.contains(copy)) {
			return;
		}
		depotAccessPositions.add(copy);
		if (assignedLine == null) {
			activePath = null;
			parkingPathAttempted = false;
			parkedAtStation = false;
			parkedAtDepot = false;
		}
	}

	public boolean assignLine(TransitLine line) {
		if (assignedLine != null) {
			return false;
		}
		assignedLine = line;
		parkedAtStation = false;
		parkedAtDepot = false;
		activePath = null;
		pathLegIndex = 0;
		lineSegmentIndex = 0;
		approachingLine = true;
		returningToStart = false;
		replanningCurrentLeg = false;
		return true;
	}

	public void unassignLine() {
		assignedLine = null;
		activePath = null;
		pathLegIndex = 0;
		lineSegmentIndex = 0;
		returningToStart = false;
		replanningCurrentLeg = false;
		parkedAtStation = false;
		parkedAtDepot = false;
		parkingPathAttempted = false;
		clearResourceTransfer();
	}

	public void update(SimulationTick tick) {
		synchronizeGraphVersion();
		updateMovement(tick.deltaSeconds());
	}

	private void synchronizeGraphVersion() {
		if (roadGraph.getVersion() != synchronizedGraphVersion) {
			Point oldTarget = roadPosition == null ? null : roadPosition.getTarget();
			Point oldDirectionStart = roadPosition == null ? position : roadPosition.getStart();
			RoadPosition updatedRoadPosition = roadGraph.findClosestRoadPosition(
				position,
				oldTarget,
				oldDirectionStart
			);
			if (updatedRoadPosition != null) {
				roadPosition = updatedRoadPosition;
				road = roadPosition.getRoad();
				position.setLocation(roadPosition.getPoint());
			} else {
				roadPosition = null;
				road = null;
			}
			activePath = null;
			pathLegIndex = 0;
			if (assignedLine == null) {
				parkedAtStation = false;
				parkedAtDepot = false;
				parkingPathAttempted = false;
			} else {
				replanningCurrentLeg = true;
			}
			synchronizedGraphVersion = roadGraph.getVersion();
		}
	}

	private void updateMovement(double deltaSeconds) {
		if (assignedLine == null) {
			updateParking(deltaSeconds);
		} else {
			updateLine(deltaSeconds);
		}
	}

	public void display() {
		Point start = GeometryUtils.rectangleStart(this, this.position);
		Point end = GeometryUtils.rectangleEnd(this, this.position);
		vehicleSkin.display(start, end, cargoAmount, cargoType, 1.0);
	}

	public void display(double scale) {
		Point start = GeometryUtils.rectangleStart(this, this.position);
		Point end = GeometryUtils.rectangleEnd(this, this.position);
		vehicleSkin.display(start, end, cargoAmount, cargoType, scale);
	}

	/**
	 * Met à jour la position du véhicule sur la route spécifiée, en utilisant les
	 * points de départ et d'arrivée donnés.
	 *
	 * @param road  La route sur laquelle le véhicule se trouve
	 * @param start Le point de départ sur la route
	 * @param end   Le point d'arrivée sur la route
	 */
	private void setPosition(Road road, Point start, Point end) {
		if (road == null || start == null || end == null) {
			this.roadPosition = null;
			this.position = new Point(start);
			return;
		}

		this.roadPosition = new RoadPosition(road, start, end);
		this.position = roadPosition.getPoint();
	}

	private void updateParking(double deltaSeconds) {
		if (parkedAtStation || parkedAtDepot) {
			return;
		}
		if (activePath == null) {
			if (parkingPathAttempted) {
				return;
			}
			parkingPathAttempted = true;
			activePath = depotAccessPositions.isEmpty()
				? roadGraph.findNearestStationPath(position, road).orElse(null)
				: roadGraph.findNearestPathToAny(position, road, depotAccessPositions).orElse(null);
			pathLegIndex = 0;
			if (activePath == null) {
				return;
			}
			if (activePath.isEmpty()) {
				parkAtCurrentPosition();
				return;
			}
			beginCurrentLeg();
		}

		if (advanceAlongPath(deltaSeconds)) {
			parkAtCurrentPosition();
		}
	}

	private void updateLine(double deltaSeconds) {
		if (loadingStation != null) {
			updateResourceTransfer(deltaSeconds);
			return;
		}
		if (activePath == null) {
			if (replanningCurrentLeg) {
				activePath = findPathTo(currentLineDestination()).orElse(null);
				if (activePath == null) {
					return;
				}
				pathLegIndex = 0;
				if (activePath.isEmpty()) {
					replanningCurrentLeg = false;
					finishPath();
					return;
				}
				replanningCurrentLeg = false;
			} else if (approachingLine) {
				planApproachToLine();
			} else if (returningToStart) {
				activePath = assignedLine.getReturnPath();
				pathLegIndex = 0;
			} else {
				activePath = assignedLine.getSegmentPaths().get(lineSegmentIndex);
				pathLegIndex = 0;
			}
			if (activePath == null) {
				return;
			}
			if (activePath.isEmpty()) {
				finishPath();
				return;
			}
			beginCurrentLeg();
		}

		if (advanceAlongPath(deltaSeconds)) {
			finishPath();
		}
	}

	private Point currentLineDestination() {
		if (approachingLine || returningToStart) {
			return assignedLine.getStartStation().getPosition();
		}
		return assignedLine
			.getStations()
			.get(lineSegmentIndex + 1)
			.getPosition();
	}

	private void planApproachToLine() {
		Point startStationPosition = assignedLine.getStartStation().getPosition();
		activePath = findPathTo(startStationPosition).orElse(null);
		pathLegIndex = 0;
	}

	private java.util.Optional<RoadPath> findPathTo(Point destination) {
		if (road != null) {
			return roadGraph.findPathFromRoadPosition(position, road, destination);
		}
		return roadGraph.findPath(position, destination);
	}

	private void beginCurrentLeg() {
		RoadLeg leg = activePath.getLegs().get(pathLegIndex);
		road = leg.road();
		roadPosition = new RoadPosition(road, leg.start(), leg.target());
		position.setLocation(roadPosition.getPoint());
	}

	private boolean advanceAlongPath(double deltaSeconds) {
		if (deltaSeconds <= 0.0 || activePath == null) {
			return false;
		}

		double remainingPathDistanceMeters = getRemainingPathDistance() * METERS_PER_PIXEL;
		double currentSpeedMetersPerSecond = currentSpeed * METERS_PER_PIXEL;
		double specificPower = powerWatts / REFERENCE_MASS_KG;
		double brakingDistanceMeters =
			(currentSpeedMetersPerSecond *
				currentSpeedMetersPerSecond *
				currentSpeedMetersPerSecond) /
			(3.0 * specificPower);
		boolean braking = remainingPathDistanceMeters <= brakingDistanceMeters;
		VehicleMotionStep motionStep = calculateMotionStep(
			currentSpeedMetersPerSecond,
			maxSpeed * METERS_PER_PIXEL,
			specificPower,
			deltaSeconds,
			braking
		);
		currentSpeed = motionStep.speedMetersPerSecond() / METERS_PER_PIXEL;

		double remainingMovement = motionStep.distanceMeters() / METERS_PER_PIXEL;
		while (remainingMovement > DISTANCE_EPSILON && activePath != null) {
			remainingMovement = roadPosition.advance(remainingMovement);
			position.setLocation(roadPosition.getPoint());
			if (!roadPosition.isAtTarget()) {
				return false;
			}

			pathLegIndex++;
			if (pathLegIndex >= activePath.getLegs().size()) {
				return true;
			}
			beginCurrentLeg();
		}
		return false;
	}

	private double getRemainingPathDistance() {
		double remainingDistance = roadPosition.getRemainingDistance();
		List<RoadLeg> legs = activePath.getLegs();
		for (int index = pathLegIndex + 1; index < legs.size(); index++) {
			RoadLeg leg = legs.get(index);
			remainingDistance += leg.start().distance(leg.target());
		}
		return remainingDistance;
	}

	private static VehicleMotionStep calculateMotionStep(
		double currentSpeed,
		double maxSpeed,
		double specificPower,
		double deltaSeconds,
		boolean braking
	) {
		if (braking) {
			double speedSquared = currentSpeed * currentSpeed - 2.0 * specificPower * deltaSeconds;
			if (speedSquared <= 0.0) {
				return new VehicleMotionStep(
					0.0,
					(currentSpeed * currentSpeed * currentSpeed) / (3.0 * specificPower)
				);
			}
			double nextSpeed = Math.sqrt(speedSquared);
			double traveledDistance =
				(currentSpeed * currentSpeed * currentSpeed - nextSpeed * nextSpeed * nextSpeed) /
				(3.0 * specificPower);
			return new VehicleMotionStep(nextSpeed, traveledDistance);
		}

		double speedSquared = currentSpeed * currentSpeed + 2.0 * specificPower * deltaSeconds;
		if (speedSquared <= maxSpeed * maxSpeed) {
			double nextSpeed = Math.sqrt(speedSquared);
			double traveledDistance =
				(nextSpeed * nextSpeed * nextSpeed - currentSpeed * currentSpeed * currentSpeed) /
				(3.0 * specificPower);
			return new VehicleMotionStep(nextSpeed, traveledDistance);
		}

		double timeToMaxSpeed =
			(maxSpeed * maxSpeed - currentSpeed * currentSpeed) / (2.0 * specificPower);
		double accelerationDistance =
			(maxSpeed * maxSpeed * maxSpeed - currentSpeed * currentSpeed * currentSpeed) /
			(3.0 * specificPower);
		double cruiseDistance = maxSpeed * (deltaSeconds - timeToMaxSpeed);
		return new VehicleMotionStep(maxSpeed, accelerationDistance + cruiseDistance);
	}

	private void finishPath() {
		activePath = null;
		roadPosition = null;
		road = null;
		currentSpeed = 0.0;
		if (assignedLine == null) {
			parkAtCurrentPosition();
			return;
		}
		Station arrivedStation = getArrivedStation();
		if (approachingLine) {
			approachingLine = false;
			lineSegmentIndex = 0;
			returningToStart = false;
		} else if (returningToStart) {
			returningToStart = false;
			lineSegmentIndex = 0;
		} else if (lineSegmentIndex == assignedLine.getSegmentPaths().size() - 1) {
			returningToStart = true;
		} else {
			lineSegmentIndex++;
		}
		beginResourceTransfer(arrivedStation);
	}

	private Station getArrivedStation() {
		if (approachingLine || returningToStart) {
			return assignedLine.getStartStation();
		}
		return assignedLine.getStations().get(lineSegmentIndex + 1);
	}

	private void beginResourceTransfer(Station station) {
		loadingStation = station;
		resourceTransferElapsedTicks = 0;
		if (cargoAmount > 0 && station.getAccessibleResourceDemand(cargoType) > 0) {
			loadingResourceType = cargoType;
			unloadingResources = true;
			return;
		}
		if (!beginLoadingResources(station)) {
			clearResourceTransfer();
		}
	}

	private void updateResourceTransfer(double deltaSeconds) {
		if (deltaSeconds <= 0.0) {
			return;
		}
		resourceTransferElapsedTicks++;
		while (resourceTransferElapsedTicks >= RESOURCE_TRANSFER_TICKS) {
			if (unloadingResources) {
				if (loadingStation.deliverResources(loadingResourceType, 1) == 0) {
					clearResourceTransfer();
					return;
				}
				cargoAmount--;
				if (cargoAmount == 0) {
					cargoType = null;
				}
			} else {
				if (
					cargoAmount >= MAX_CARGO_UNITS ||
					loadingStation.takeAccessibleResources(loadingResourceType, 1) == 0
				) {
					clearResourceTransfer();
					return;
				}
				cargoType = loadingResourceType;
				cargoAmount++;
			}
			resourceTransferElapsedTicks -= RESOURCE_TRANSFER_TICKS;

			if (
				unloadingResources &&
				(cargoAmount == 0 ||
					loadingStation.getAccessibleResourceDemand(loadingResourceType) == 0)
			) {
				Station station = loadingStation;
				if (cargoAmount != 0 || !beginLoadingResources(station)) {
					clearResourceTransfer();
					return;
				}
			}
			if (
				!unloadingResources &&
				(cargoAmount >= MAX_CARGO_UNITS ||
					loadingStation.getAccessibleResourceStock(loadingResourceType) == 0)
			) {
				clearResourceTransfer();
				return;
			}
		}
	}

	private boolean beginLoadingResources(Station station) {
		if (cargoAmount >= MAX_CARGO_UNITS) {
			return false;
		}
		ResourceType selectedType = cargoType;
		if (selectedType == null) {
			int largestAvailableStock = 0;
			for (ResourceType resourceType : ResourceType.values()) {
				int availableStock = station.getAccessibleResourceStock(resourceType);
				if (availableStock > largestAvailableStock) {
					largestAvailableStock = availableStock;
					selectedType = resourceType;
				}
			}
		}
		if (selectedType == null || station.getAccessibleResourceStock(selectedType) == 0) {
			return false;
		}
		loadingResourceType = selectedType;
		unloadingResources = false;
		return true;
	}

	private void clearResourceTransfer() {
		loadingStation = null;
		loadingResourceType = null;
		resourceTransferElapsedTicks = 0;
		unloadingResources = false;
	}

	private void parkAtCurrentPosition() {
		activePath = null;
		roadPosition = null;
		road = null;
		currentSpeed = 0.0;
		parkedAtDepot = !depotAccessPositions.isEmpty();
		parkedAtStation = depotAccessPositions.isEmpty();
	}
}
