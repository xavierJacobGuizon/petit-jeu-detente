package org.jeuroute.model.world.network;

import java.awt.Point;
import org.jeuroute.model.world.skin.Skin;
import org.jeuroute.model.world.skin.RoadSkin;

public class Road {

	private final Skin roadSkin;

	private final Point start;
	private final Point end;

	public Road(Point start, Point end) {
		this(start, end, true);
	}

	private Road(Point start, Point end, boolean snapToGrid) {
		this.start = snapToGrid ? RoadGraph.snapPoint(start) : new Point(start);
		this.end = snapToGrid ? RoadGraph.snapPoint(end) : new Point(end);

		roadSkin = new RoadSkin(1.0f, 0.5f, 0.3f);
	}

	public static Road createSegment(Point start, Point end) {
		return new Road(start, end, false);
	}

	public Point getStart() {
		return new Point(start);
	}

	public Point getEnd() {
		return new Point(end);
	}

	public boolean hasEndpoint(Point point) {
		return start.equals(point) || end.equals(point);
	}

	/**
	 * Vérifie si un point donné se trouve sur la route, en considérant la route
	 * comme un segment de ligne entre les points de départ et d'arrivée.
	 *
	 * @param point Point à vérifier
	 * @return true si le point se trouve sur la route, false sinon
	 */
	public boolean containsPoint(Point point) {
		if (point == null) {
			return false;
		}

		double directionX = end.x - start.x;
		double directionY = end.y - start.y;
		double lengthSquared = directionX * directionX + directionY * directionY;
		if (lengthSquared == 0.0) {
			return point.equals(start);
		}

		double projection =
			((point.x - start.x) * directionX + (point.y - start.y) * directionY) / lengthSquared;
		if (projection < 0.0 || projection > 1.0) {
			return false;
		}

		double closestX = start.x + projection * directionX;
		double closestY = start.y + projection * directionY;
		return Math.hypot(point.x - closestX, point.y - closestY) <= 1.5;
	}

	public Point otherEndpoint(Point endpoint) {
		if (start.equals(endpoint)) {
			return new Point(end);
		}
		if (end.equals(endpoint)) {
			return new Point(start);
		}
		throw new IllegalArgumentException("Point is not an endpoint of this road");
	}

	public Point endpointInDirection(Point position, Point destination) {
		Point first = getStart();
		Point second = getEnd();
		if (position.equals(first)) {
			return second;
		}
		if (position.equals(second)) {
			return first;
		}

		if (destination == null) {
			return second;
		}

		double directionX = destination.x - position.x;
		double directionY = destination.y - position.y;
		double firstDot = (first.x - position.x) * directionX + (first.y - position.y) * directionY;
		double secondDot =
			(second.x - position.x) * directionX + (second.y - position.y) * directionY;
		return secondDot >= firstDot ? second : first;
	}

	public double getLength() {
		double dx = end.x - start.x;
		double dy = end.y - start.y;
		return Math.hypot(dx, dy);
	}

	public Point pointAt(double distanceFromStart) {
		double dx = end.x - start.x;
		double dy = end.y - start.y;
		double length = getLength();
		double ratio = length == 0 ? 0 : distanceFromStart / length;

		int x = (int) Math.round(start.x + dx * ratio);
		int y = (int) Math.round(start.y + dy * ratio);
		return new Point(x, y);
	}

	public void display() {
		roadSkin.display(this.getStart(), this.getEnd());
	}

	public void display(double scale) {
		roadSkin.display(this.getStart(), this.getEnd(), scale);
	}
}
