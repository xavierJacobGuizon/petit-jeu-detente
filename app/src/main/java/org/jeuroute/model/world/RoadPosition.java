package org.jeuroute.model.world;

import java.awt.Point;

public class RoadPosition {
    private final Road road;
    private final Point start;
    private final Point target;
    private final double length;
    private double travelledDistance;

    public RoadPosition(Road road, Point start, Point target) {
        if (road == null || start == null || target == null) {
            throw new IllegalArgumentException("Road and points cannot be null");
        }

        if (!road.containsPoint(start) || !road.hasEndpoint(target) || start.equals(target)) {
            throw new IllegalArgumentException("Road position must use a point on the road and a different endpoint");
        }

        this.road = road;
        this.start = new Point(start);
        this.target = new Point(target);
        this.length = this.start.distance(this.target);
    }

    public Road getRoad() {
        return road;
    }

    // Attention à la création de Point à chaque frame ..
    public Point getStart() {
        return new Point(start);
    }

    public Point getTarget() {
        return new Point(target);
    }

    public Point getPoint() {
        double ratio = length == 0.0 ? 1.0 : travelledDistance / length;
        return new Point(
                (int) Math.round(start.x + (target.x - start.x) * ratio),
                (int) Math.round(start.y + (target.y - start.y) * ratio));
    }

    public double getRemainingDistance() {
        return length - travelledDistance;
    }

    public boolean isAtTarget() {
        return travelledDistance >= length;
    }

    public double advance(double distance) {
        double distanceToTarget = getRemainingDistance();
        double distanceUsed = Math.min(Math.max(distance, 0.0), distanceToTarget);
        travelledDistance += distanceUsed;
        return distance - distanceUsed;
    }
}