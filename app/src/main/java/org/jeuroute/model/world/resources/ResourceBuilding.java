package org.jeuroute.model.world.resources;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.records.time.SimulationTick;
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
	private long productionElapsedTicks;
	private long inputConsumptionElapsedTicks;

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

	public void update(SimulationTick tick) {
		Objects.requireNonNull(tick, "tick cannot be null");
		advanceTicks(1);
	}

	private void advanceTicks(long elapsedTicks) {
		if (elapsedTicks <= 0) {
			return;
		}

		consumeReceivedResources(elapsedTicks);
		if (stock >= getStorageCapacity()) {
			productionElapsedTicks = 0;
			return;
		}
		productionElapsedTicks += elapsedTicks;
		long productionIntervalTicks = SimulationTick.ticksForSeconds(
			resourceType.getProductionIntervalSeconds()
		);
		int productionCount = (int) Math.min(
			getStorageCapacity() - stock,
			productionElapsedTicks / productionIntervalTicks
		);
		stock += productionCount;
		productionElapsedTicks -= productionCount * productionIntervalTicks;
		if (stock >= getStorageCapacity()) {
			productionElapsedTicks = 0;
		}
	}

	private void consumeReceivedResources(long elapsedTicks) {
		if (receivedResourceStock == 0) {
			inputConsumptionElapsedTicks = 0;
			return;
		}

		inputConsumptionElapsedTicks += elapsedTicks;
		long consumptionIntervalTicks = SimulationTick.ticksForSeconds(
			INPUT_CONSUMPTION_INTERVAL_SECONDS
		);
		int consumptionCount = (int) Math.min(
			receivedResourceStock,
			inputConsumptionElapsedTicks / consumptionIntervalTicks
		);
		receivedResourceStock -= consumptionCount;
		inputConsumptionElapsedTicks -= consumptionCount * consumptionIntervalTicks;
		if (receivedResourceStock == 0) {
			inputConsumptionElapsedTicks = 0;
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
