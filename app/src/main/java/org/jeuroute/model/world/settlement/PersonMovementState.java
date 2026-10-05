package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.Objects;

public final class PersonMovementState {

	private static final double DISTANCE_EPSILON = 1.0e-9;

	private final double startX;
	private final double startY;
	private final double targetX;
	private final double targetY;
	private final double directionX;
	private final double directionY;
	private final double speed;
	private final double totalDistance;
	private final PersonRoute.Surface surface;
	private double progress;

	PersonMovementState(double startX, double startY, PersonRoute.Segment segment) {
		Point target = Objects.requireNonNull(segment).target();
		this.startX = startX;
		this.startY = startY;
		targetX = target.x;
		targetY = target.y;
		double deltaX = targetX - startX;
		double deltaY = targetY - startY;
		totalDistance = Math.hypot(deltaX, deltaY);
		directionX = totalDistance == 0.0 ? 0.0 : deltaX / totalDistance;
		directionY = totalDistance == 0.0 ? 0.0 : deltaY / totalDistance;
		surface = segment.surface();
		speed = Person.WALK_SPEED_PIXELS_PER_SECOND * surface.speedMultiplier();
	}

	public double directionX() {
		return directionX;
	}

	public double directionY() {
		return directionY;
	}

	public double speed() {
		return speed;
	}

	public double progress() {
		return progress;
	}

	public double totalDistance() {
		return totalDistance;
	}

	public double progressRatio() {
		return totalDistance == 0.0 ? 1.0 : progress / totalDistance;
	}

	public PersonRoute.Surface surface() {
		return surface;
	}

	public boolean isComplete() {
		return totalDistance - progress <= DISTANCE_EPSILON;
	}

	double advance(double elapsedSeconds) {
		if (elapsedSeconds <= 0.0 || isComplete()) {
			return 0.0;
		}
		double consumedSeconds = Math.min(elapsedSeconds, (totalDistance - progress) / speed);
		progress = Math.min(totalDistance, progress + speed * consumedSeconds);
		if (isComplete()) {
			progress = totalDistance;
		}
		return consumedSeconds;
	}

	double x() {
		return isComplete() ? targetX : startX + directionX * progress;
	}

	double y() {
		return isComplete() ? targetY : startY + directionY * progress;
	}
}
