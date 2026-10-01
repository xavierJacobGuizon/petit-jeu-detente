package org.jeuroute.model.world;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jeuroute.model.world.skin.StationSkin;

public final class Station {

	public static final double CAPTURE_RADIUS = 180.0;

	private final Point position;
	private final StationSkin skin = new StationSkin();
	private final List<Road> roads = new ArrayList<>();
	private final List<Road> roadsView = Collections.unmodifiableList(roads);
	private final List<ResourceBuilding> capturedBuildings = new ArrayList<>();
	private final List<ResourceBuilding> capturedBuildingsView = Collections.unmodifiableList(
		capturedBuildings
	);

	public Station(Point position) {
		this.position = new Point(position);
	}

	public Point getPosition() {
		return new Point(position);
	}

	public List<Road> getRoads() {
		return roadsView;
	}

	public List<ResourceBuilding> getCapturedBuildings() {
		return capturedBuildingsView;
	}

	public int getAccessibleResourceStock(ResourceType resourceType) {
		return capturedBuildings
			.stream()
			.filter(building -> Objects.requireNonNull(building).getResourceType() == resourceType)
			.mapToInt(building -> Objects.requireNonNull(building).getStock())
			.sum();
	}

	public Map<ResourceType, Integer> getCapturedResourceStocks() {
		Map<ResourceType, Integer> resourceStocks = new EnumMap<>(ResourceType.class);
		for (ResourceBuilding building : capturedBuildings) {
			ResourceType resourceType = building.getResourceType();
			Integer previousStock = resourceStocks.get(resourceType);
			int totalStock = (previousStock == null ? 0 : previousStock) + building.getStock();
			resourceStocks.put(resourceType, totalStock);
		}
		return Collections.unmodifiableMap(resourceStocks);
	}

	public int getAccessibleResourceDemand(ResourceType resourceType) {
		int outstandingDemand = 0;
		for (ResourceBuilding building : capturedBuildings) {
			if (building.getRequestedResourceType() == resourceType) {
				outstandingDemand += building.getOutstandingDemand();
			}
		}
		return outstandingDemand;
	}

	public Map<ResourceType, Integer> getCapturedResourceDemand() {
		Map<ResourceType, Integer> resourceDemand = new EnumMap<>(ResourceType.class);
		for (ResourceBuilding building : capturedBuildings) {
			ResourceType resourceType = building.getRequestedResourceType();
			int outstandingDemand = building.getOutstandingDemand();
			if (outstandingDemand > 0) {
				Integer previousDemand = resourceDemand.get(resourceType);
				resourceDemand.put(
					resourceType,
					(previousDemand == null ? 0 : previousDemand) + outstandingDemand
				);
			}
		}
		return Collections.unmodifiableMap(resourceDemand);
	}

	public int deliverResources(ResourceType resourceType, int requestedAmount) {
		if (requestedAmount <= 0) {
			return 0;
		}
		int remainingAmount = requestedAmount;
		for (ResourceBuilding building : capturedBuildings) {
			if (building.getRequestedResourceType() != resourceType) {
				continue;
			}
			remainingAmount -= building.receiveResource(remainingAmount);
			if (remainingAmount == 0) {
				break;
			}
		}
		return requestedAmount - remainingAmount;
	}

	public int takeAccessibleResources(ResourceType resourceType, int requestedAmount) {
		if (requestedAmount <= 0) {
			return 0;
		}
		int remaining = requestedAmount;
		for (ResourceBuilding building : capturedBuildings) {
			if (building.getResourceType() == resourceType) {
				remaining -= building.takeResource(remaining);
				if (remaining == 0) {
					break;
				}
			}
		}
		return requestedAmount - remaining;
	}

	public boolean canAccess(ResourceBuilding building) {
		return position.distance(building.getPosition()) <= CAPTURE_RADIUS;
	}

	public void synchronizeCapturedBuildings(List<ResourceBuilding> buildings) {
		capturedBuildings.clear();
		capturedBuildings.addAll(buildings);
	}

	public void setRoads(List<Road> connectedRoads) {
		roads.clear();
		roads.addAll(connectedRoads);
	}

	public Road firstRight(Point arrivalPoint) {
		return Intersection.firstRight(position, roads, arrivalPoint);
	}

	public void display() {
		skin.displayCaptureRadius(position, CAPTURE_RADIUS, 1.0);
		skin.display(position, null);
		skin.displayCapturedResources(
			position,
			getCapturedResourceStocks(),
			getCapturedResourceDemand()
		);
	}

	public void display(double scale) {
		skin.displayCaptureRadius(position, CAPTURE_RADIUS, scale);
		skin.display(position, null, scale);
		skin.displayCapturedResources(
			position,
			getCapturedResourceStocks(),
			getCapturedResourceDemand()
		);
	}
}
