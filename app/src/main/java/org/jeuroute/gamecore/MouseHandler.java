package org.jeuroute.gamecore;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

import java.awt.Point;
import org.jeuroute.gamecore.preview.PlacementPreview;

public class MouseHandler {

	private enum Mode {
		NONE,
		ROUTE,
		VEHICLE,
		STATION,
		DEPOT,
		LINE,
	}

	private Point dragStart;
	private Point currentPosition;
	private Point mousePosition;
	private boolean dragging;
	private Point lastReleasedStart;
	private Point lastReleasedEnd;
	private boolean hasPendingRoad;
	private Mode activeMode = Mode.NONE;
	private Point pendingVehiclePosition;
	private Point pendingStationPosition;
	private Point pendingDepotPosition;
	private Point pendingLineStationSelection;

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
			case VEHICLE -> onVehiclePlacement(x, y);
			case ROUTE -> onRoutePlacement(x, y);
			case NONE -> {
			}
		}
	}

	public void onRoutePlacement(double x, double y) {
		this.dragStart = new Point((int) Math.round(x), (int) Math.round(y));
		this.currentPosition = this.dragStart;
		this.dragging = true;
	}

	public void onVehiclePlacement(double x, double y) {
		if (activeMode == Mode.VEHICLE) {
			pendingVehiclePosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onStationPlacement(double x, double y) {
		if (activeMode == Mode.STATION) {
			pendingStationPosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onDepotPlacement(double x, double y) {
		if (activeMode == Mode.DEPOT) {
			pendingDepotPosition = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void onLineStationSelection(double x, double y) {
		if (activeMode == Mode.LINE) {
			pendingLineStationSelection = new Point((int) Math.round(x), (int) Math.round(y));
		}
	}

	public void setRouteCreationEnabled(boolean enabled) {
		setActiveMode(Mode.ROUTE, enabled);
	}

	public boolean isRouteCreationEnabled() {
		return activeMode == Mode.ROUTE;
	}

	public void setVehicleCreationEnabled(boolean enabled) {
		setActiveMode(Mode.VEHICLE, enabled);
	}

	public void setStationCreationEnabled(boolean enabled) {
		setActiveMode(Mode.STATION, enabled);
	}

	public void setDepotCreationEnabled(boolean enabled) {
		setActiveMode(Mode.DEPOT, enabled);
	}

	public boolean isDepotCreationEnabled() {
		return activeMode == Mode.DEPOT;
	}

	public void setLineCreationEnabled(boolean enabled) {
		setActiveMode(Mode.LINE, enabled);
	}

	public boolean isLineCreationEnabled() {
		return activeMode == Mode.LINE;
	}

	public boolean isStationCreationEnabled() {
		return activeMode == Mode.STATION;
	}

	public void setvehicleCreationEnabled(boolean enabled) {
		setVehicleCreationEnabled(enabled);
	}

	public boolean isVehicleCreationEnabled() {
		return activeMode == Mode.VEHICLE;
	}

	private void setActiveMode(Mode mode, boolean enabled) {
		if (enabled) {
			activeMode = mode;
		} else if (activeMode == mode) {
			activeMode = Mode.NONE;
		}
	}

	public Point consumeVehiclePlacement() {
		Point placement = pendingVehiclePosition;
		pendingVehiclePosition = null;
		return placement;
	}

	public PlacementPreview.Type getPlacementPreviewType() {
		return switch (activeMode) {
			case VEHICLE -> PlacementPreview.Type.VEHICLE;
			case STATION -> PlacementPreview.Type.STATION;
			case DEPOT -> PlacementPreview.Type.DEPOT;
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
