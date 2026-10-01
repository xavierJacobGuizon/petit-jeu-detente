package org.jeuroute.model.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TransitLine {

	private final List<Station> stations;
	private List<RoadPath> segmentPaths;
	private RoadPath returnPath;
	private LineColor color;

	public TransitLine(Station startStation, Station endStation, RoadPath path) {
		this(List.of(startStation, endStation), List.of(path), LineColor.TURQUOISE);
	}

	public TransitLine(List<Station> stations, List<RoadPath> segmentPaths, LineColor color) {
		this(stations, segmentPaths, reversePath(segmentPaths), color);
	}

	public TransitLine(
		List<Station> stations,
		List<RoadPath> segmentPaths,
		RoadPath returnPath,
		LineColor color
	) {
		this.stations = validateStations(stations);
		this.color = Objects.requireNonNull(color);
		setSegmentPaths(segmentPaths);
		setReturnPath(returnPath);
	}

	private static List<Station> validateStations(List<Station> stations) {
		List<Station> copy = List.copyOf(stations);
		if (copy.size() < 2) {
			throw new IllegalArgumentException("A line needs at least two stations");
		}
		Set<java.awt.Point> positions = new HashSet<>();
		for (Station station : copy) {
			if (!positions.add(station.getPosition())) {
				throw new IllegalArgumentException("A line cannot visit a station more than once");
			}
		}
		return copy;
	}

	public List<Station> getStations() {
		return stations;
	}

	public Station getStartStation() {
		return stations.getFirst();
	}

	public Station getEndStation() {
		return stations.getLast();
	}

	public RoadPath getPath() {
		List<RoadPath.Leg> legs = new ArrayList<>();
		for (RoadPath segmentPath : segmentPaths) {
			legs.addAll(segmentPath.getLegs());
		}
		return new RoadPath(legs);
	}

	public RoadPath getReturnPath() {
		return returnPath;
	}

	private static RoadPath reversePath(List<RoadPath> paths) {
		List<RoadPath.Leg> legs = new ArrayList<>();
		for (RoadPath path : paths) {
			legs.addAll(path.getLegs());
		}
		List<RoadPath.Leg> reversed = new ArrayList<>(legs.size());
		for (int index = legs.size() - 1; index >= 0; index--) {
			RoadPath.Leg leg = legs.get(index);
			reversed.add(new RoadPath.Leg(leg.road(), leg.target(), leg.start()));
		}
		return new RoadPath(reversed);
	}

	public List<RoadPath> getSegmentPaths() {
		return segmentPaths;
	}

	public void setSegmentPaths(List<RoadPath> segmentPaths) {
		List<RoadPath> copy = List.copyOf(segmentPaths);
		if (copy.size() != stations.size() - 1) {
			throw new IllegalArgumentException(
				"A line needs one non-empty path per station segment"
			);
		}
		for (RoadPath path : copy) {
			if (path.isEmpty()) {
				throw new IllegalArgumentException(
					"A line needs one non-empty path per station segment"
				);
			}
		}
		this.segmentPaths = copy;
	}

	public void setReturnPath(RoadPath returnPath) {
		RoadPath path = Objects.requireNonNull(returnPath);
		if (path.isEmpty()) {
			throw new IllegalArgumentException("A line needs a non-empty return path");
		}
		this.returnPath = path;
	}

	public LineColor getColor() {
		return color;
	}

	public void setColor(LineColor color) {
		this.color = Objects.requireNonNull(color);
	}
}
