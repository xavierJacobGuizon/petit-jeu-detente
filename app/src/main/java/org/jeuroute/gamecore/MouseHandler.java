package org.jeuroute.gamecore;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

import java.awt.Point;
import org.jeuroute.gamecore.enums.MouseMode;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;

public class MouseHandler {

	private Point dragStart;
	private Point currentPosition;
	private Point mousePosition;
	private boolean dragging;
	private Point lastReleasedStart;
	private Point lastReleasedEnd;
	private boolean hasPendingRoad;
	private MouseMode activeMode = MouseMode.NONE;
	private Point pendingVehiclePosition;
	private Point pendingStationPosition;
	private Point pendingDepotPosition;
	private Point pendingLineStationSelection;
	private Point pendingPersonPosition;

	public void onMove(double x, double y) {
		mousePosition = new Point((int) Math.round(x), (int) Math.round(y));
		if (dragging) {
			currentPosition = new Point(mousePosition);
		}
	}

	public void onPress(int button, double x, double y) {
		if (button != GLFW_MOUSE_BUTTON_LEFT) {
			return;
		}

		switch (activeMode) {
			case STATION -> onStationPlacement(x, y);
			case DEPOT -> onDepotPlacement(x, y);
			case LINE -> onLineStationSelection(x, y);
			case PERSON -> onPersonPlacement(x, y);
			case VEHICLE -> onVehiclePlacement(x, y);
			case ROUTE -> onRoutePlacement(x, y);
			case NONE -> {
			}
		}
	}

	public void onPersonPlacement(double x, double y) {
		if (activeMode == MouseMode.PERSON) {
			pendingPersonPosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onRoutePlacement(double x, double y) {
		this.dragStart = new Point((int) Math.round(x), (int) Math.round(y));
		this.currentPosition = this.dragStart;
		this.dragging = true;
	}

	public void onVehiclePlacement(double x, double y) {
		if (activeMode == MouseMode.VEHICLE) {
			pendingVehiclePosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onStationPlacement(double x, double y) {
		if (activeMode == MouseMode.STATION) {
			pendingStationPosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onDepotPlacement(double x, double y) {
		if (activeMode == MouseMode.DEPOT) {
			pendingDepotPosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onLineStationSelection(double x, double y) {
		if (activeMode == MouseMode.LINE) {
			pendingLineStationSelection = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void setRouteCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.ROUTE, enabled);
	}

	public boolean isRouteCreationEnabled() {
		return activeMode == MouseMode.ROUTE;
	}

	public void setVehicleCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.VEHICLE, enabled);
	}

	public void setStationCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.STATION, enabled);
	}

	public void setDepotCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.DEPOT, enabled);
	}

	public boolean isDepotCreationEnabled() {
		return activeMode == MouseMode.DEPOT;
	}

	public void setLineCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.LINE, enabled);
	}

	public boolean isLineCreationEnabled() {
		return activeMode == MouseMode.LINE;
	}

	public void setPersonCreationEnabled(boolean enabled) {
		setActiveMode(MouseMode.PERSON, enabled);
	}

	public boolean isPersonCreationEnabled() {
		return activeMode == MouseMode.PERSON;
	}

	public boolean isStationCreationEnabled() {
		return activeMode == MouseMode.STATION;
	}

	public void setvehicleCreationEnabled(boolean enabled) {
		setVehicleCreationEnabled(enabled);
	}

	public boolean isVehicleCreationEnabled() {
		return activeMode == MouseMode.VEHICLE;
	}

	public void cancelCurrentAction() {
		activeMode = MouseMode.NONE;
		dragStart = null;
		currentPosition = null;
		dragging = false;
		lastReleasedStart = null;
		lastReleasedEnd = null;
		hasPendingRoad = false;
		pendingVehiclePosition = null;
		pendingStationPosition = null;
		pendingDepotPosition = null;
		pendingLineStationSelection = null;
		pendingPersonPosition = null;
	}

	private void setActiveMode(MouseMode mode, boolean enabled) {
		if (enabled) {
			activeMode = mode;
		} else if (activeMode == mode) {
			activeMode = MouseMode.NONE;
		}
	}

	public Point consumeVehiclePlacement() {
		Point placement = pendingVehiclePosition;
		pendingVehiclePosition = null;
		return placement;
	}

	public PlacementPreviewType getPlacementPreviewType() {
		return switch (activeMode) {
			case VEHICLE -> PlacementPreviewType.VEHICLE;
			case STATION -> PlacementPreviewType.STATION;
			case DEPOT -> PlacementPreviewType.DEPOT;
			case PERSON -> PlacementPreviewType.PERSON;
			default -> null;
		};
	}

	public Point getMousePosition() {
		return mousePosition == null ? null : new Point(mousePosition);
	}

	public Point consumeStationPlacement() {
		Point placement = pendingStationPosition;
		pendingStationPosition = null;
		return placement;
	}

	public Point consumeDepotPlacement() {
		Point placement = pendingDepotPosition;
		pendingDepotPosition = null;
		return placement;
	}

	public Point consumeLineStationSelection() {
		Point selection = pendingLineStationSelection;
		pendingLineStationSelection = null;
		return selection;
	}

	public Point consumePersonPlacement() {
		Point placement = pendingPersonPosition;
		pendingPersonPosition = null;
		return placement;
	}

	public void onRelease(int button, double x, double y) {
		mousePosition = new Point((int) Math.round(x), (int) Math.round(y));
		if (button != GLFW_MOUSE_BUTTON_LEFT || dragStart == null) {
			return;
		}

		this.currentPosition = new Point((int) Math.round(x), (int) Math.round(y));
		this.lastReleasedStart = this.dragStart;
		this.lastReleasedEnd = this.currentPosition;
		this.hasPendingRoad = true;
		this.dragging = false;
		this.dragStart = null;
		this.currentPosition = null;
	}

	public boolean isDragging() {
		return dragging;
	}

	public Point getDragStart() {
		return dragStart;
	}

	public Point getCurrentPosition() {
		return currentPosition;
	}

	public Point getCurrentDragEnd() {
		return currentPosition != null ? currentPosition : dragStart;
	}

	public boolean consumePendingRoad() {
		if (!hasPendingRoad) {
			return false;
		}

		hasPendingRoad = false;
		return true;
	}

	public Point getLastReleasedStart() {
		return lastReleasedStart;
	}

	public Point getLastReleasedEnd() {
		return lastReleasedEnd;
	}
}
