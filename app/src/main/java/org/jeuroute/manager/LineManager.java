package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jeuroute.model.world.LineColor;
import org.jeuroute.model.world.RoadGraph;
import org.jeuroute.model.world.RoadPath;
import org.jeuroute.model.world.Station;
import org.jeuroute.model.world.TransitLine;
import org.jeuroute.model.world.Vehicle;

public final class LineManager {

	private final RoadGraph roadGraph;
	private final List<TransitLine> lines = new ArrayList<>();
	private final List<TransitLine> linesView = Collections.unmodifiableList(lines);

	public LineManager(RoadGraph roadGraph) {
		this.roadGraph = roadGraph;
	}

	public List<TransitLine> getLines() {
		return linesView;
	}

	public boolean canConnect(Station startStation, Station endStation) {
		return findSegmentPath(startStation, endStation).isPresent();
	}

	public Optional<TransitLine> createLine(Station startStation, Station endStation) {
		if (startStation == null || endStation == null || startStation == endStation) {
			return Optional.empty();
		}
		return createLine(List.of(startStation, endStation));
	}

	public Optional<TransitLine> createLine(List<Station> stations) {
		if (stations == null || stations.size() < 2 || hasRepeatedStations(stations)) {
			return Optional.empty();
		}

		Optional<List<RoadPath>> segmentPaths = findSegmentPaths(stations);
		Optional<RoadPath> returnPath = findReturnPath(stations);
		if (segmentPaths.isEmpty() || returnPath.isEmpty()) {
			return Optional.empty();
		}

		LineColor color = LineColor.values()[lines.size() % LineColor.values().length];
		TransitLine line = new TransitLine(stations, segmentPaths.get(), returnPath.get(), color);
		lines.add(line);
		return Optional.of(line);
	}

	public boolean assignVehicle(Vehicle vehicle, TransitLine line) {
		return vehicle != null && line != null && vehicle.assignLine(line);
	}

	public List<TransitLine> refreshPaths() {
		List<TransitLine> removedLines = new ArrayList<>();
		for (TransitLine line : new ArrayList<>(lines)) {
			Optional<List<RoadPath>> segmentPaths = findSegmentPaths(line.getStations());
			Optional<RoadPath> returnPath = findReturnPath(line.getStations());
			if (segmentPaths.isEmpty() || returnPath.isEmpty()) {
				lines.remove(line);
				removedLines.add(line);
			} else {
				line.setSegmentPaths(segmentPaths.get());
				line.setReturnPath(returnPath.get());
			}
		}
		return List.copyOf(removedLines);
	}

	private Optional<List<RoadPath>> findSegmentPaths(List<Station> stations) {
		List<RoadPath> paths = new ArrayList<>();
		for (int index = 1; index < stations.size(); index++) {
			Optional<RoadPath> path = findSegmentPath(stations.get(index - 1), stations.get(index));
			if (path.isEmpty()) {
				return Optional.empty();
			}
			paths.add(path.get());
		}
		return Optional.of(List.copyOf(paths));
	}

	private Optional<RoadPath> findSegmentPath(Station startStation, Station endStation) {
		if (startStation == null || endStation == null) {
			return Optional.empty();
		}
		return roadGraph
			.findPath(startStation.getPosition(), endStation.getPosition())
			.filter(path -> !path.isEmpty());
	}

	private Optional<RoadPath> findReturnPath(List<Station> stations) {
		Point end = stations.getLast().getPosition();
		Point start = stations.getFirst().getPosition();
		return roadGraph.findPath(end, start).filter(path -> !path.isEmpty());
	}

	private boolean hasRepeatedStations(List<Station> stations) {
		Set<Point> positions = new HashSet<>();
		for (Station station : stations) {
			if (station == null || !positions.add(station.getPosition())) {
				return true;
			}
		}
		return false;
	}
}
