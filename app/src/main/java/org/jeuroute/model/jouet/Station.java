package org.jeuroute.model.jouet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jeuroute.model.interfaces.Peau;
import org.jeuroute.model.interfaces.jouetpeau.StationPeau;

public final class Station {

	private final Point position;
	private final Peau peau = new StationPeau();
	private final List<Road> roads = new ArrayList<>();
	private final List<Road> roadsView = Collections.unmodifiableList(roads);

	public Station(Point position) {
		this.position = new Point(position);
	}

	public Point getPosition() {
		return new Point(position);
	}

	public List<Road> getRoads() {
		return roadsView;
	}

	public void setRoads(List<Road> connectedRoads) {
		roads.clear();
		roads.addAll(connectedRoads);
	}

	public Road firstRight(Point arrivalPoint) {
		return Intersection.firstRight(position, roads, arrivalPoint);
	}

	public void display() {
		peau.display(position, null);
	}

	public void display(double scale) {
		peau.display(position, null, scale);
	}
}
