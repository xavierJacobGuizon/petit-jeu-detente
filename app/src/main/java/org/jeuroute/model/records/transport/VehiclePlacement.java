package org.jeuroute.model.records.transport;

import java.awt.Point;
import org.jeuroute.model.world.network.Road;

public record VehiclePlacement(Road road, Point position, Point target) {}
