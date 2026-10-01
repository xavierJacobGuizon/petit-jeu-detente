package org.jeuroute.manager;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.preview.PlacementPreview;
import org.jeuroute.model.jouet.RoadGraph;
import org.jeuroute.model.jouet.RouteTemporaire;
import org.jeuroute.model.jouet.Station;

public class MouseHandlerManager {

	private final RoadGraph graph;
	private final MouseHandler mouseHandler;
	private final VehicleManager vehicleManager;
	private final FixedEntityManager fixedEntityManager;
	private final Consumer<Point> lineStationSelectionHandler;
	private RouteTemporaire routeTemporaire;

	public MouseHandlerManager(
		RoadGraph graph,
		MouseHandler mouseHandler,
		VehicleManager vehicleManager,
		FixedEntityManager fixedEntityManager,
		Consumer<Point> lineStationSelectionHandler
	) {
		this.graph = graph;
		this.mouseHandler = mouseHandler;
		this.vehicleManager = vehicleManager;
		this.fixedEntityManager = fixedEntityManager;
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
		clearRouteTemporaire();
	}

	public RouteTemporaire getRouteTemporaire() {
		return routeTemporaire;
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

	public void setRouteTemporaire(RouteTemporaire routeTemporaire) {
		this.routeTemporaire = routeTemporaire;
	}

	public void clearRouteTemporaire() {
		this.routeTemporaire = null;
	}

	/**
	 * Traite les entrées en attente du gestionnaire de souris,
	 * y compris la création de routes, de véhicules, de stations et la sélection de stations pour les lignes.
	 */
	public void processPendingInput() {
		createRoadAfterDrag();
		createVehicleAfterClick();
		createStationAfterClick();
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

		if (routeTemporaire == null) {
			routeTemporaire = new RouteTemporaire(start, end);
		} else {
			routeTemporaire.updateEnd(end);
		}

		if (canPlaceRoute(start, end)) {
			graph.addRoad(routeTemporaire.toRoad());
		}
		clearRouteTemporaire();
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
		Station station = fixedEntityManager.createStation(position);
		graph.addStationNode(station.getPosition());
	}

	private void selectLineStationAfterClick() {
		Point position = mouseHandler.consumeLineStationSelection();
		if (position != null) {
			lineStationSelectionHandler.accept(position);
		}
	}

	public void renderDragLine() {
		renderDragLine(1.0);
	}

	public void renderDragLine(double zoom) {
		WorldRenderer.renderRoadSnapIndicator(getRouteSnapPoint(), zoom);
		if (!mouseHandler.isDragging() || mouseHandler.getDragStart() == null) {
			return;
		}

		Point start = graph.snapRoutePoint(mouseHandler.getDragStart());
		Point end = graph.snapRoutePoint(mouseHandler.getCurrentDragEnd());
		if (start != null && end != null) {
			if (routeTemporaire == null) {
				routeTemporaire = new RouteTemporaire(start, end);
			} else {
				routeTemporaire.updateEnd(end);
			}
			boolean valid = canPlaceRoute(start, end);
			if (valid) {
				WorldRenderer.renderRoutePlacementPreview(
					graph.getRoadSegmentsAfterAddingRoad(start, end),
					zoom
				);
			} else {
				routeTemporaire.display(false, zoom);
			}
			WorldRenderer.renderRoadSnapIndicator(getRouteSnapPoint(), zoom);
			WorldRenderer.renderFutureIntersections(getFutureRouteIntersections());
		}
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
		PlacementPreview.Type type = mouseHandler.getPlacementPreviewType();
		if (type == PlacementPreview.Type.VEHICLE) {
			return vehicleManager.getPlacementPreview(mousePosition);
		}
		if (type == PlacementPreview.Type.STATION) {
			return fixedEntityManager.getStationPlacementPreview(mousePosition);
		}
		if (type == PlacementPreview.Type.DEPOT) {
			return fixedEntityManager.getDepotPlacementPreview(mousePosition);
		}
		return null;
	}

	public void renderPlacementPreview() {
		renderPlacementPreview(1.0);
	}

	public void renderPlacementPreview(double zoom) {
		PlacementPreview preview = getPlacementPreview();
		if (preview != null && preview.type() == PlacementPreview.Type.STATION) {
			WorldRenderer.renderStationRoadPreview(
				graph.getRoadSegmentsAfterAddingStation(preview.position()),
				preview.valid(),
				zoom
			);
		}
		WorldRenderer.renderPlacementPreview(preview, zoom);
	}
}
