package org.jeuroute.manager.input;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jeuroute.gamecore.controllers.MouseHandler;
import org.jeuroute.manager.settlement.PersonManager;
import org.jeuroute.manager.transport.VehicleManager;
import org.jeuroute.manager.world.FixedEntityManager;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.RoutePreview;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.transport.Station;

public class MouseHandlerManager {

	private final RoadGraph graph;
	private final MouseHandler mouseHandler;
	private final VehicleManager vehicleManager;
	private final FixedEntityManager fixedEntityManager;
	private final Consumer<Point> lineStationSelectionHandler;
	private final PersonManager personManager;

	public MouseHandlerManager(
		RoadGraph graph,
		MouseHandler mouseHandler,
		VehicleManager vehicleManager,
		FixedEntityManager fixedEntityManager,
		Consumer<Point> lineStationSelectionHandler
	) {
		this(
			graph,
			mouseHandler,
			vehicleManager,
			fixedEntityManager,
			lineStationSelectionHandler,
			null
		);
	}

	public MouseHandlerManager(
		RoadGraph graph,
		MouseHandler mouseHandler,
		VehicleManager vehicleManager,
		FixedEntityManager fixedEntityManager,
		Consumer<Point> lineStationSelectionHandler,
		PersonManager personManager
	) {
		this.graph = graph;
		this.mouseHandler = mouseHandler;
		this.vehicleManager = vehicleManager;
		this.fixedEntityManager = fixedEntityManager;
		this.personManager = personManager;
		this.lineStationSelectionHandler = Objects.requireNonNull(
			lineStationSelectionHandler,
			"lineStationSelectionHandler cannot be null"
		);
	}

	public MouseHandler getMouseHandler() {
		return mouseHandler;
	}

	public void cancelCurrentAction() {
		mouseHandler.cancelCurrentAction();
	}

	public Point getRouteSnapPoint() {
		if (!mouseHandler.isRouteCreationEnabled()) {
			return null;
		}
		Point mousePosition = mouseHandler.getMousePosition();
		return graph.getRouteSnapPoint(mousePosition);
	}

	public boolean canPlaceCurrentRoute() {
		return (
			mouseHandler.isDragging() &&
			canPlaceRoute(mouseHandler.getDragStart(), mouseHandler.getCurrentDragEnd())
		);
	}

	public List<Point> getFutureRouteIntersections() {
		if (!mouseHandler.isDragging() || mouseHandler.getDragStart() == null) {
			return List.of();
		}

		Point start = graph.snapRoutePoint(mouseHandler.getDragStart());
		Point end = graph.snapRoutePoint(mouseHandler.getCurrentDragEnd());
		if (!canPlaceRoute(start, end)) {
			return List.of();
		}

		return graph.getIntersectionsAfterAddingRoad(start, end);
	}

	/**
	 * Traite les entrées en attente du gestionnaire de souris,
	 * y compris la création de routes, de véhicules, de stations et la sélection de stations pour les lignes.
	 */
	public void processPendingInput() {
		createRoadAfterDrag();
		createVehicleAfterClick();
		createStationAfterClick();
		createPersonAfterClick();
		selectLineStationAfterClick();
	}

	/**
	 * Crée une route après un glissement de souris si une route est en attente.
	 */
	private void createRoadAfterDrag() {
		if (!mouseHandler.consumePendingRoad()) {
			return;
		}

		Point start = graph.snapRoutePoint(mouseHandler.getLastReleasedStart());
		Point end = graph.snapRoutePoint(mouseHandler.getLastReleasedEnd());

		if (start == null || end == null) {
			return;
		}

		if (canPlaceRoute(start, end)) {
			graph.addRoad(new Road(start, end));
		}
	}

	private void createVehicleAfterClick() {
		Point position = mouseHandler.consumeVehiclePlacement();
		if (position != null) {
			vehicleManager.createVehicleAt(position);
		}
	}

	private void createStationAfterClick() {
		Point position = mouseHandler.consumeStationPlacement();
		if (position == null) {
			return;
		}
		if (!fixedEntityManager.getStationPlacementPreview(position).valid()) {
			return;
		}
		Station station = fixedEntityManager.createStation(position);
		graph.addStationNode(station.getPosition());
	}

	private void createPersonAfterClick() {
		List<Point> placements = mouseHandler.consumePersonPlacements();
		if (personManager == null) {
			return;
		}
		for (Point position : placements) {
			personManager.addPerson(position);
		}
	}

	private void selectLineStationAfterClick() {
		Point position = mouseHandler.consumeLineStationSelection();
		if (position != null) {
			lineStationSelectionHandler.accept(position);
		}
	}

	public RoutePreview getRoutePreview() {
		if (!mouseHandler.isDragging() || mouseHandler.getDragStart() == null) {
			return null;
		}

		Point start = graph.snapRoutePoint(mouseHandler.getDragStart());
		Point end = graph.snapRoutePoint(mouseHandler.getCurrentDragEnd());
		if (start == null || end == null) {
			return null;
		}

		boolean valid = canPlaceRoute(start, end);
		List<Road> roadSegments = valid
			? graph.getRoadSegmentsAfterAddingRoad(start, end)
			: List.of();
		List<Point> futureIntersections = valid
			? graph.getIntersectionsAfterAddingRoad(start, end)
			: List.of();
		return new RoutePreview(start, end, valid, roadSegments, futureIntersections);
	}

	private boolean canPlaceRoute(Point start, Point end) {
		if (start == null || end == null) {
			return false;
		}
		Point snappedStart = graph.snapRoutePoint(start);
		Point snappedEnd = graph.snapRoutePoint(end);
		return (
			snappedStart.distance(snappedEnd) > 10.0 && graph.canAddRoad(snappedStart, snappedEnd)
		);
	}

	public PlacementPreview getPlacementPreview() {
		Point mousePosition = mouseHandler.getMousePosition();
		if (mousePosition == null) {
			return null;
		}
		PlacementPreviewType type = mouseHandler.getPlacementPreviewType();
		if (type == PlacementPreviewType.VEHICLE) {
			return vehicleManager.getPlacementPreview(mousePosition);
		}
		if (type == PlacementPreviewType.STATION) {
			return fixedEntityManager.getStationPlacementPreview(mousePosition);
		}
		if (type == PlacementPreviewType.DEPOT) {
			return fixedEntityManager.getDepotPlacementPreview(mousePosition);
		}
		if (type == PlacementPreviewType.PERSON && personManager != null) {
			return personManager.getPlacementPreview(mousePosition);
		}
		return null;
	}

	public List<Road> getStationRoadPreview(PlacementPreview preview) {
		if (preview == null || preview.type() != PlacementPreviewType.STATION) {
			return List.of();
		}
		return graph.getRoadSegmentsAfterAddingStation(preview.position());
	}
}
