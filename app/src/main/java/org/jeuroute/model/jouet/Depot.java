package org.jeuroute.model.jouet;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.interfaces.Peau;
import org.jeuroute.model.interfaces.jouetpeau.DepotPeau;

public final class Depot {

	public static final int HALF_SIZE = 24;
	public static final int ACCESS_OFFSET = HALF_SIZE + 1;
	public static final int ROAD_STUB_LENGTH = 75;

	private final Point position;
	private final Point accessPosition;
	private final Peau peau = new DepotPeau();

	public Depot(Point position) {
		this(position, new Point(position.x, position.y + ACCESS_OFFSET));
	}

	public Depot(Point position, Point accessPosition) {
		this.position = new Point(Objects.requireNonNull(position));
		this.accessPosition = new Point(Objects.requireNonNull(accessPosition));
	}

	public Point getPosition() {
		return new Point(position);
	}

	public Point getAccessPosition() {
		return new Point(accessPosition);
	}

	public Point getRoadEndPosition() {
		return new Point(accessPosition.x, accessPosition.y + ROAD_STUB_LENGTH);
	}

	public void display() {
		peau.display(position, accessPosition);
	}

	public void display(double scale) {
		peau.display(position, accessPosition, scale);
	}
}
