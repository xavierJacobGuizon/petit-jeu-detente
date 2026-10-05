package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.records.time.SimulationTick;
import org.jeuroute.model.world.skin.PersonSkin;

public final class Person {

	public static final double WALK_SPEED_PIXELS_PER_SECOND = 28.0;
	public static final int COLLISION_RADIUS = 4;

	private final PersonSkin skin = new PersonSkin();
	private House homeHouse;
	private double x;
	private double y;
	private House currentHouse;
	private PersonGoal currentGoal;
	private PersonRoute route = new PersonRoute(List.of());
	private int nextSegmentIndex;
	private long routeGeometryVersion;
	private long routePlanVersion;
	private PersonMovementState movementState;
	private long idleTicksRemaining;
	private boolean temporaryDetourApplied;

	public Person(Point position, House homeHouse) {
		this(position);
		this.homeHouse = Objects.requireNonNull(homeHouse, "homeHouse cannot be null");
	}

	public Person(Point position) {
		Objects.requireNonNull(position, "position cannot be null");
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

	public boolean isAwaitingInitialJourney() {
		return homeHouse == null;
	}

	public PersonMovementState getMovementState() {
		return movementState;
	}

	public List<PersonRoute.Segment> getRemainingRouteSegments() {
		return route.segments().subList(nextSegmentIndex, route.segments().size());
	}

	public long getRouteGeometryVersion() {
		return routeGeometryVersion;
	}

	public long getRoutePlanVersion() {
		return routePlanVersion;
	}

	public int getCurrentSegmentIndex() {
		return nextSegmentIndex;
	}

	public double getIdleSecondsRemaining() {
		return idleTicksRemaining * SimulationTick.STEP_SECONDS;
	}

	public long getIdleTicksRemaining() {
		return idleTicksRemaining;
	}

	public void finishIdlePeriod() {
		idleTicksRemaining = 0;
	}

	public boolean beginJourney(PersonGoal goal, List<Point> route) {
		return beginJourney(
			Objects.requireNonNull(goal),
			PersonRoute.walking(getPosition(), route)
		);
	}

	public boolean beginJourney(PersonGoal goal, PersonRoute route) {
		currentGoal = Objects.requireNonNull(goal, "goal cannot be null");
		this.route = Objects.requireNonNull(route, "route cannot be null");
		nextSegmentIndex = 0;
		routeGeometryVersion++;
		routePlanVersion++;
		idleTicksRemaining = 0;
		temporaryDetourApplied = false;
		if (route.isEmpty()) {
			return arriveAtDestination();
		}
		prepareMovementState();
		return false;
	}

	public boolean beginInitialJourney(PersonGoal goal, PersonRoute route) {
		if (!isAwaitingInitialJourney()) {
			return false;
		}
		PersonGoal initialGoal = Objects.requireNonNull(goal, "goal cannot be null");
		homeHouse = initialGoal.destinationHouse();
		return beginJourney(initialGoal, route);
	}

	public boolean canAcceptTemporaryDetour() {
		return isWalking() && !temporaryDetourApplied;
	}

	public boolean replaceRoute(List<Point> route) {
		return replaceRoute(PersonRoute.walking(getPosition(), route));
	}

	public boolean replaceRoute(PersonRoute route) {
		if (!canAcceptTemporaryDetour() || route.isEmpty()) {
			return false;
		}
		this.route = Objects.requireNonNull(route);
		nextSegmentIndex = 0;
		routeGeometryVersion++;
		routePlanVersion++;
		temporaryDetourApplied = true;
		prepareMovementState();
		return true;
	}

	public boolean advanceMovement(SimulationTick tick) {
		return PersonMovementSystem.advancePerson(this, Objects.requireNonNull(tick));
	}

	PersonMovementState movementState() {
		return movementState;
	}

	void setMovementPosition(double nextX, double nextY) {
		x = nextX;
		y = nextY;
	}

	boolean completeMovementSegment() {
		PersonMovementState completedState = movementState;
		setMovementPosition(completedState.x(), completedState.y());
		movementState = null;
		nextSegmentIndex++;
		routeGeometryVersion++;
		if (nextSegmentIndex >= route.segments().size()) {
			return arriveAtDestination();
		}
		prepareMovementState();
		return false;
	}

	void prepareMovementState() {
		if (nextSegmentIndex >= route.segments().size()) {
			movementState = null;
			return;
		}
		movementState = new PersonMovementState(x, y, route.segments().get(nextSegmentIndex));
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
		currentHouse = currentGoal.destinationHouse();
		currentGoal = null;
		route = new PersonRoute(List.of());
		nextSegmentIndex = 0;
		routeGeometryVersion++;
		routePlanVersion++;
		movementState = null;
		return true;
	}
}
