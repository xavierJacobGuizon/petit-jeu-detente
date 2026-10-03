package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.skin.PersonSkin;

public final class Person {

	public static final double WALK_SPEED_PIXELS_PER_SECOND = 28.0;

	private final PersonSkin skin = new PersonSkin();
	private final House homeHouse;
	private double x;
	private double y;
	private House currentHouse;
	private PersonGoal currentGoal;
	private List<Point> waypoints = List.of();
	private int nextWaypointIndex;
	private long idleTicksRemaining;
	private boolean temporaryDetourApplied;

	public Person(Point position, House homeHouse) {
		Objects.requireNonNull(position, "position cannot be null");
		this.homeHouse = Objects.requireNonNull(homeHouse, "homeHouse cannot be null");
		x = position.x;
		y = position.y;
	}

	public Point getPosition() {
		return new Point((int) Math.round(x), (int) Math.round(y));
	}

	public double getPreciseX() {
		return x;
	}

	public double getPreciseY() {
		return y;
	}

	public House getCurrentHouse() {
		return currentHouse;
	}

	public House getHomeHouse() {
		return homeHouse;
	}

	public House getDestinationHouse() {
		return currentGoal == null ? null : currentGoal.destinationHouse();
	}

	public PersonGoal getCurrentGoal() {
		return currentGoal;
	}

	public boolean isWalking() {
		return currentGoal != null;
	}

	public double getIdleSecondsRemaining() {
		return idleTicksRemaining * SimulationTick.STEP_SECONDS;
	}

	public boolean beginJourney(PersonGoal goal, List<Point> route) {
		currentGoal = Objects.requireNonNull(goal, "goal cannot be null");
		waypoints = route.stream().map(Point::new).toList();
		nextWaypointIndex = 0;
		idleTicksRemaining = 0;
		temporaryDetourApplied = false;
		return waypoints.isEmpty() && arriveAtDestination();
	}

	public boolean canAcceptTemporaryDetour() {
		return isWalking() && !temporaryDetourApplied;
	}

	public boolean replaceRoute(List<Point> route) {
		if (!canAcceptTemporaryDetour() || route.isEmpty()) {
			return false;
		}
		waypoints = route.stream().map(Point::new).toList();
		nextWaypointIndex = 0;
		temporaryDetourApplied = true;
		return true;
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
		Point destination = currentGoal.destination();
		x = destination.x;
		y = destination.y;
		currentHouse = currentGoal.destinationHouse();
		currentGoal = null;
		waypoints = List.of();
		nextWaypointIndex = 0;
		return true;
	}
}
