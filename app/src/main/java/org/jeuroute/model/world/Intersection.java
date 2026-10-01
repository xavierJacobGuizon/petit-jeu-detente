package org.jeuroute.model.world;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jeuroute.model.world.skin.Skin;
import org.jeuroute.model.world.skin.IntersectionSkin;

public class Intersection {

	private static final double EPSILON = 0.000001;
	private static final double FULL_TURN_DEGREES = 360.0;

	private final Skin intersectionSkin;

	private final Point position;
	private final List<Road> roads = new ArrayList<>();

	public Intersection(Point position) {
		this.position = new Point(position);
		this.intersectionSkin = new IntersectionSkin(0.0f, 0.0f, 1.0f);
	}

	public Point getPosition() {
		return new Point(position);
	}

	public List<Road> getRoads() {
		return Collections.unmodifiableList(roads);
	}

	public void setRoads(List<Road> roads) {
		this.roads.clear();
		this.roads.addAll(roads);
	}

	public Road firstRight(Road incomingRoad, Point arrivalPoint) {
		return firstRight(position, roads, arrivalPoint);
	}

	static Road firstRight(Point position, List<Road> roads, Point arrivalPoint) {
		double arrivalX = arrivalPoint.x - position.x;
		double arrivalY = arrivalPoint.y - position.y;
		double arrivalLength = Math.hypot(arrivalX, arrivalY);
		if (arrivalLength <= EPSILON) {
			return null;
		}

		arrivalX /= arrivalLength;
		arrivalY /= arrivalLength;
		Road selectedRoad = null;
		double smallestClockwiseTurn = FULL_TURN_DEGREES;

		for (Road candidate : roads) {
			if (!candidate.hasEndpoint(position)) {
				continue;
			}

			Point exit = candidate.otherEndpoint(position);
			if (exit.equals(arrivalPoint)) {
				continue;
			}

			double exitX = exit.x - position.x;
			double exitY = exit.y - position.y;
			double exitLength = Math.hypot(exitX, exitY);
			if (exitLength <= EPSILON) {
				continue;
			}
			exitX /= exitLength;
			exitY /= exitLength;

			double clockwiseTurn = clockwiseTurnDegrees(arrivalX, arrivalY, exitX, exitY);
			if (clockwiseTurn < smallestClockwiseTurn) {
				smallestClockwiseTurn = clockwiseTurn;
				selectedRoad = candidate;
			}
		}

		return selectedRoad;
	}

	private static double clockwiseTurnDegrees(
		double arrivalX,
		double arrivalY,
		double exitX,
		double exitY
	) {
		double radians = Math.atan2(
			arrivalY * exitX - arrivalX * exitY,
			arrivalX * exitX + arrivalY * exitY
		);
		double degrees = Math.toDegrees(radians);
		if (degrees <= EPSILON) {
			degrees += FULL_TURN_DEGREES;
		}
		return degrees;
	}

	public void display() {
		intersectionSkin.display(this.getPosition(), null);
	}

	public void display(double scale) {
		intersectionSkin.display(this.getPosition(), null, scale);
	}
}
