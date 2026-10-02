package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.skin.PersonSkin;

public final class Person {

	public static final double WALK_SPEED_PIXELS_PER_SECOND = 28.0;

	private final PersonSkin skin = new PersonSkin();
	private double x;
	private double y;
	private House currentHouse;
	private House destinationHouse;
	private List<Point> waypoints = List.of();
	private int nextWaypointIndex;
	private long idleTicksRemaining;

	public Person(Point position) {
		Objects.requireNonNull(position, "position cannot be null");
		x = position.x;
		y = position.y;
	}

	public Point getPosition() {
		return new Point((int) Math.round(x), (int) Math.round(y));
	}

	public House getCurrentHouse() {
		return currentHouse;
	}

	public House getDestinationHouse() {
		return destinationHouse;
	}

	public boolean isWalking() {
		return destinationHouse != null;
	}

	public double getIdleSecondsRemaining() {
		return idleTicksRemaining * SimulationTick.STEP_SECONDS;
	}

	public boolean walkTo(House house, List<Point> route) {
		destinationHouse = Objects.requireNonNull(house, "house cannot be null");
		waypoints = route.stream().map(Point::new).toList();
		nextWaypointIndex = 0;
		idleTicksRemaining = 0;
		return waypoints.isEmpty() && arriveAtDestination();
	}

	public boolean advanceMovement(double deltaSeconds) {
		if (!isWalking() || deltaSeconds <= 0.0) {
			return false;
		}

		double remainingDistance = WALK_SPEED_PIXELS_PER_SECOND * deltaSeconds;
		while (nextWaypointIndex < waypoints.size()) {
			Point waypoint = waypoints.get(nextWaypointIndex);
			double deltaX = waypoint.x - x;
			double deltaY = waypoint.y - y;
			double distance = Math.hypot(deltaX, deltaY);
			if (distance <= remainingDistance || distance == 0.0) {
				x = waypoint.x;
				y = waypoint.y;
				remainingDistance -= distance;
				nextWaypointIndex++;
				continue;
			}

			double ratio = remainingDistance / distance;
			x += deltaX * ratio;
			y += deltaY * ratio;
			return false;
		}

		return arriveAtDestination();
	}

	public boolean advanceMovement(SimulationTick tick) {
		return advanceMovement(Objects.requireNonNull(tick).deltaSeconds());
	}

	public boolean advanceIdle(double deltaSeconds) {
		return advanceIdleTicks(SimulationTick.ticksForSeconds(Math.max(0.0, deltaSeconds)));
	}

	public boolean advanceIdle(SimulationTick tick) {
		Objects.requireNonNull(tick);
		return advanceIdleTicks(1);
	}

	private boolean advanceIdleTicks(long elapsedTicks) {
		if (idleTicksRemaining <= 0) {
			return true;
		}
		idleTicksRemaining = Math.max(0, idleTicksRemaining - elapsedTicks);
		return idleTicksRemaining == 0;
	}

	public void beginIdle(double seconds) {
		if (!Double.isFinite(seconds) || seconds < 0.0) {
			throw new IllegalArgumentException("Idle duration must be finite and non-negative");
		}
		idleTicksRemaining = SimulationTick.ticksForSeconds(seconds);
	}

	public void display(double zoom) {
		skin.display(getPosition(), zoom);
	}

	private boolean arriveAtDestination() {
		House arrivedAt = destinationHouse;
		x = arrivedAt.getPosition().x;
		y = arrivedAt.getPosition().y;
		currentHouse = arrivedAt;
		destinationHouse = null;
		waypoints = List.of();
		nextWaypointIndex = 0;
		return true;
	}
}
