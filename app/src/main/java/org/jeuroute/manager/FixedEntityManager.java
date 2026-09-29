package org.jeuroute.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jeuroute.gamecore.preview.PlacementPreview;
import org.jeuroute.model.jouet.Depot;
import org.jeuroute.model.jouet.Intersection;
import org.jeuroute.model.jouet.RoadGraph;
import org.jeuroute.model.jouet.Station;

public final class FixedEntityManager {

	private final List<Station> stations = new ArrayList<>();
	private final List<Depot> depots = new ArrayList<>();
	private final List<Intersection> intersections = new ArrayList<>();
	private final List<Station> stationsView = Collections.unmodifiableList(stations);
	private final List<Depot> depotsView = Collections.unmodifiableList(depots);
	private final List<Intersection> intersectionsView = Collections.unmodifiableList(
		intersections
	);

	public FixedEntityManager() {}

	public Station createStation(Point position) {
		Point snappedPosition = RoadGraph.snapPoint(position);
		Station existingStation = getStationAt(snappedPosition);
		if (existingStation != null) {
			return existingStation;
		}

		Station station = new Station(snappedPosition);
		stations.add(station);
		return station;
	}

	public PlacementPreview getStationPlacementPreview(Point position) {
		Point snappedPosition = RoadGraph.snapPoint(position);
		return new PlacementPreview(
			PlacementPreview.Type.STATION,
			snappedPosition,
			getStationAt(snappedPosition) == null
		);
	}

	public Depot createDepot(Point position) {
		Point snappedPosition = RoadGraph.snapPoint(position);
		Depot existingDepot = getDepotAt(snappedPosition);
		if (existingDepot != null) {
			return existingDepot;
		}
		Depot depot = new Depot(snappedPosition);
		depots.add(depot);
		return depot;
	}

	public PlacementPreview getDepotPlacementPreview(Point position) {
		Point snappedPosition = RoadGraph.snapPoint(position);
		boolean inBounds =
			snappedPosition.x >= Depot.HALF_SIZE &&
			snappedPosition.x <= 1280 - Depot.HALF_SIZE &&
			snappedPosition.y >= Depot.HALF_SIZE &&
			snappedPosition.y <= 720 - Depot.ACCESS_OFFSET - Depot.ROAD_STUB_LENGTH;
		boolean unoccupied = depots
			.stream()
			.noneMatch(
				depot -> depot.getPosition().distance(snappedPosition) < Depot.HALF_SIZE * 2
			);
		return new PlacementPreview(
			PlacementPreview.Type.DEPOT,
			snappedPosition,
			inBounds && unoccupied
		);
	}

	public List<Station> getStations() {
		return stationsView;
	}

	public List<Depot> getDepots() {
		return depotsView;
	}

	public Depot getDepotAt(Point position) {
		return depots
			.stream()
			.filter(depot -> depot.getPosition().equals(position))
			.findFirst()
			.orElse(null);
	}

	public List<Intersection> getIntersections() {
		return intersectionsView;
	}

	public Intersection getIntersectionAt(Point position) {
		return intersections
			.stream()
			.filter(intersection -> intersection.getPosition().equals(position))
			.findFirst()
			.orElse(null);
	}

	public Station getStationAt(Point position) {
		return stations
			.stream()
			.filter(station -> station.getPosition().equals(position))
			.findFirst()
			.orElse(null);
	}

	public Station findStationNear(Point position, double maxDistance) {
		Station nearestStation = null;
		double nearestDistance = maxDistance;
		for (Station station : stations) {
			double distance = position.distance(station.getPosition());
			if (distance <= nearestDistance) {
				nearestStation = station;
				nearestDistance = distance;
			}
		}
		return nearestStation;
	}

	public void synchronizeIntersections(List<Point> intersectionPositions, RoadGraph graph) {
		Map<Point, Intersection> existingIntersections = new LinkedHashMap<>();
		for (Intersection intersection : intersections) {
			existingIntersections.put(intersection.getPosition(), intersection);
		}

		List<Intersection> updatedIntersections = new ArrayList<>();
		for (Point position : intersectionPositions) {
			if (getStationAt(position) != null) {
				continue;
			}

			Intersection intersection = existingIntersections.get(position);
			if (intersection == null) {
				intersection = new Intersection(position);
			}
			intersection.setRoads(graph.getConnectedRoadsAt(position));
			updatedIntersections.add(intersection);
		}

		intersections.clear();
		intersections.addAll(updatedIntersections);
	}

	public void synchronizeStations(RoadGraph graph) {
		for (Station station : stations) {
			station.setRoads(graph.getConnectedRoadsAt(station.getPosition()));
		}
	}

	public void clear() {
		stations.clear();
		depots.clear();
		intersections.clear();
	}
}
