package org.jeuroute.model.world.resources;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.skin.ResourceBuildingSkin;

public final class ResourceBuilding {

	public static final int HALF_SIZE = 24;
	public static final int INPUT_STORAGE_CAPACITY = 4;
	public static final double INPUT_CONSUMPTION_INTERVAL_SECONDS = 10.0;

	private final Point position;
	private final Point accessPosition;
	private final ResourceType resourceType;
	private final ResourceBuildingSkin skin;
	private int stock;
	private int receivedResourceStock;
	private double elapsedSeconds;
	private double inputConsumptionElapsedSeconds;

	public ResourceBuilding(Point position, ResourceType resourceType) {
		this(position, resourceType, position);
	}

	public ResourceBuilding(Point position, ResourceType resourceType, Point accessPosition) {
		this.position = new Point(Objects.requireNonNull(position));
		this.accessPosition = new Point(Objects.requireNonNull(accessPosition));
		this.resourceType = Objects.requireNonNull(resourceType);
		this.skin = new ResourceBuildingSkin(resourceType);
	}

	public Point getPosition() {
		return new Point(position);
	}

	public Point getAccessPosition() {
		return new Point(accessPosition);
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public int getStock() {
		return stock;
	}

	public int getStorageCapacity() {
		return resourceType.getStorageCapacity();
	}

	public ResourceType getRequestedResourceType() {
		return resourceType.getRequiredResourceType();
	}

	public int getReceivedResourceStock() {
		return receivedResourceStock;
	}

	public int getOutstandingDemand() {
		return INPUT_STORAGE_CAPACITY - receivedResourceStock;
	}

	public int receiveResource(int requestedAmount) {
		if (requestedAmount <= 0) {
			return 0;
		}
		int acceptedAmount = Math.min(getOutstandingDemand(), requestedAmount);
		receivedResourceStock += acceptedAmount;
		return acceptedAmount;
	}

	public void update(double deltaSeconds) {
		if (deltaSeconds <= 0.0) {
			return;
		}

		consumeReceivedResources(deltaSeconds);
		if (stock >= getStorageCapacity()) {
			elapsedSeconds = 0.0;
			return;
		}
		elapsedSeconds += deltaSeconds;
		int productionCount = (int) Math.min(
			getStorageCapacity() - stock,
			Math.floor(elapsedSeconds / resourceType.getProductionIntervalSeconds())
		);
		stock += productionCount;
		elapsedSeconds -= productionCount * resourceType.getProductionIntervalSeconds();
		if (stock >= getStorageCapacity()) {
			elapsedSeconds = 0.0;
		}
	}

	private void consumeReceivedResources(double deltaSeconds) {
		if (receivedResourceStock == 0) {
			inputConsumptionElapsedSeconds = 0.0;
			return;
		}

		inputConsumptionElapsedSeconds += deltaSeconds;
		int consumptionCount = (int) Math.min(
			receivedResourceStock,
			Math.floor(inputConsumptionElapsedSeconds / INPUT_CONSUMPTION_INTERVAL_SECONDS)
		);
		receivedResourceStock -= consumptionCount;
		inputConsumptionElapsedSeconds -= consumptionCount * INPUT_CONSUMPTION_INTERVAL_SECONDS;
		if (receivedResourceStock == 0) {
			inputConsumptionElapsedSeconds = 0.0;
		}
	}

	public int takeResource(int requestedAmount) {
		if (requestedAmount <= 0) {
			return 0;
		}
		int takenAmount = Math.min(stock, requestedAmount);
		stock -= takenAmount;
		return takenAmount;
	}

	public void display(double scale) {
		Point start = new Point(position.x - HALF_SIZE, position.y - HALF_SIZE);
		Point end = new Point(position.x + HALF_SIZE, position.y + HALF_SIZE);
		skin.display(start, end, stock, getStorageCapacity(), scale);
	}
}
