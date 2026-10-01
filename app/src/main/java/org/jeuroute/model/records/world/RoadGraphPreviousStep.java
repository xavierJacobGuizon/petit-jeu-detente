package org.jeuroute.model.records.world;

import java.awt.Point;
import org.jeuroute.model.world.network.Road;

public record RoadGraphPreviousStep(Point previous, Road road) {}
