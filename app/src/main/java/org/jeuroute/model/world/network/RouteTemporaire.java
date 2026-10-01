package org.jeuroute.model.world.network;

import java.awt.Point;
import org.jeuroute.model.world.skin.Skin;
import org.jeuroute.model.world.skin.RoadSkin;

public class RouteTemporaire {

	private final Skin roadSkin;
	private final Skin invalidRoadSkin;
	private final Point start;
	private Point end;

	public RouteTemporaire(Point start, Point end) {
		this.start = RoadGraph.snapPoint(start);
		this.end = RoadGraph.snapPoint(end);
		this.roadSkin = new RoadSkin(1.0f, 0.5f, 0.3f);
		this.invalidRoadSkin = new RoadSkin(1.0f, 0.12f, 0.2f);
	}

	public Point getStart() {
		return new Point(start);
	}

	public Point getEnd() {
		return new Point(end);
	}

	public void updateEnd(Point end) {
		Point snappedEnd = RoadGraph.snapPoint(end);
		if (snappedEnd != null) {
			this.end = snappedEnd;
		}
	}

	public double getLength() {
		return start.distance(end);
	}

	public Road toRoad() {
		return new Road(start, end);
	}

	public void display() {
		display(true, 1.0);
	}

	public void display(boolean valid) {
		display(valid, 1.0);
	}

	public void display(boolean valid, double scale) {
		(valid ? roadSkin : invalidRoadSkin).display(start, end, scale);
	}
}
