package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.List;
import org.jeuroute.model.records.preview.LinePreview;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.RoutePreview;
import org.jeuroute.model.records.preview.WorldPreviewData;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.skin.StationSkin;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;

public final class WorldPreviewRenderer {

	private WorldPreviewRenderer() {}

	public static void renderWorldPreviews(WorldPreviewData previews, double zoom) {
		renderRoadSnapIndicator(previews.routeSnapPoint(), zoom);
		renderRoutePreview(previews.route(), zoom);
		boolean placementValid = previews.placement() != null && previews.placement().valid();
		renderStationRoadPreview(previews.stationRoadSegments(), placementValid, zoom);
		renderPlacementPreview(previews.placement(), zoom);
		renderLinePreview(previews.line(), zoom);
	}

	private static void renderRoutePreview(RoutePreview preview, double zoom) {
		if (preview == null) {
			return;
		}
		if (preview.valid()) {
			renderRoutePlacementPreview(preview.roadSegments(), zoom);
		} else {
			renderInvalidRoutePreview(preview.start(), preview.end(), zoom);
		}
		renderFutureIntersections(preview.futureIntersections(), zoom);
	}

	private static void renderInvalidRoutePreview(Point start, Point end, double zoom) {
		glColor3f(1.0f, 0.12f, 0.2f);
		glLineWidth((float) (10.0 * zoom));
		glBegin(GL_LINES);
		glVertex2d(start.x, start.y);
		glVertex2d(end.x, end.y);
		glEnd();
		glLineWidth(1.0f);
	}

	public static void renderPlacementPreview(PlacementPreview preview) {
		renderPlacementPreview(preview, 1.0);
	}

	public static void renderPlacementPreview(PlacementPreview preview, double zoom) {
		if (preview == null) {
			return;
		}

		Point position = preview.position();
		renderTypeSpecificPlacementPreview(preview, position, zoom);
		renderPlacementMarker(position, markerHalfSize(preview.type()), preview.valid(), zoom);
	}

	private static void renderTypeSpecificPlacementPreview(
		PlacementPreview preview,
		Point position,
		double zoom
	) {
		switch (preview.type()) {
			case STATION -> StationSkin.displayCaptureRadiusPreview(
				position,
				Station.CAPTURE_RADIUS,
				zoom,
				preview.valid()
			);
			case DEPOT -> renderDepotAccessPreview(position, preview.valid(), zoom);
			case VEHICLE, PERSON -> {
			}
		}
	}

	private static void renderDepotAccessPreview(Point position, boolean valid, double zoom) {
		setPreviewColor(valid);
		beginDashedLines((float) (3.0 * zoom));
		glBegin(GL_LINES);
		glVertex2i(position.x, position.y + Depot.ACCESS_OFFSET);
		glVertex2i(position.x, position.y + Depot.ACCESS_OFFSET + Depot.ROAD_STUB_LENGTH);
		glEnd();
		endDashedLines();
	}

	private static int markerHalfSize(PlacementPreviewType type) {
		return switch (type) {
			case VEHICLE -> 12;
			case STATION -> 9;
			case DEPOT -> Depot.HALF_SIZE;
			case PERSON -> 6;
		};
	}

	private static void renderPlacementMarker(
		Point position,
		int halfSize,
		boolean valid,
		double zoom
	) {
		setPreviewColor(valid);
		beginDashedLines((float) (2.0 * zoom));
		glBegin(GL_LINE_LOOP);
		glVertex2i(position.x - halfSize, position.y - halfSize);
		glVertex2i(position.x + halfSize, position.y - halfSize);
		glVertex2i(position.x + halfSize, position.y + halfSize);
		glVertex2i(position.x - halfSize, position.y + halfSize);
		glEnd();
		endDashedLines();
	}

	public static void renderStationRoadPreview(List<Road> roads, boolean valid) {
		renderStationRoadPreview(roads, valid, 1.0);
	}

	public static void renderStationRoadPreview(List<Road> roads, boolean valid, double zoom) {
		if (roads.isEmpty()) {
			return;
		}
		setPreviewColor(valid);
		beginDashedLines((float) (3.0 * zoom));
		glBegin(GL_LINES);
		for (Road road : roads) {
			Point start = road.getStart();
			Point end = road.getEnd();
			glVertex2i(start.x, start.y);
			glVertex2i(end.x, end.y);
		}
		glEnd();
		endDashedLines();
	}

	public static void renderLinePreview(LinePreview preview) {
		renderLinePreview(preview, 1.0);
	}

	public static void renderLinePreview(LinePreview preview, double zoom) {
		if (preview == null) {
			return;
		}
		switch (preview.status()) {
			case NORMAL -> glColor3f(0.2f, 0.9f, 0.75f);
			case CONNECTABLE -> glColor3f(0.15f, 0.9f, 0.2f);
			case DISCONNECTED -> glColor3f(1.0f, 0.12f, 0.2f);
		}
		beginDashedLines((float) (2.0 * zoom));
		List<Point> stations = preview.stations();
		Point cursor = preview.cursor();
		glBegin(GL_LINES);
		for (int index = 1; index < stations.size(); index++) {
			Point start = stations.get(index - 1);
			Point end = stations.get(index);
			glVertex2i(start.x, start.y);
			glVertex2i(end.x, end.y);
		}
		Point lastStation = stations.getLast();
		glVertex2i(lastStation.x, lastStation.y);
		glVertex2i(cursor.x, cursor.y);
		glEnd();
		endDashedLines();
	}

	public static void renderRoadSnapIndicator(Point center) {
		renderRoadSnapIndicator(center, 1.0);
	}

	public static void renderRoadSnapIndicator(Point center, double zoom) {
		if (center == null) {
			return;
		}

		double pulse = (Math.sin((System.nanoTime() / 1_000_000_000.0) * 3.5) + 1.0) / 2.0;
		double radius = 12.0 + pulse * 7.0;
		glColor3f(1.0f, 0.9f, 0.28f);
		glLineWidth((float) (2.5 * zoom));
		glBegin(GL_LINE_LOOP);
		for (int segment = 0; segment < 48; segment++) {
			double angle = (Math.PI * 2.0 * segment) / 48.0;
			glVertex2d(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius);
		}
		glEnd();
		glLineWidth(1.0f);
	}

	public static void renderFutureIntersections(List<Point> intersections) {
		renderFutureIntersections(intersections, 1.0);
	}

	public static void renderFutureIntersections(List<Point> intersections, double zoom) {
		if (intersections.isEmpty()) {
			return;
		}
		glColor3f(0.0f, 0.0f, 1.0f);
		glBegin(GL_QUADS);
		for (Point point : intersections) {
			glVertex2i(point.x - 5, point.y - 5);
			glVertex2i(point.x + 5, point.y - 5);
			glVertex2i(point.x + 5, point.y + 5);
			glVertex2i(point.x - 5, point.y + 5);
		}
		glEnd();
	}

	public static void renderRoutePlacementPreview(List<Road> segments) {
		renderRoutePlacementPreview(segments, 1.0);
	}

	public static void renderRoutePlacementPreview(List<Road> segments, double zoom) {
		if (segments.isEmpty()) {
			return;
		}
		glColor3f(1.0f, 0.5f, 0.3f);
		beginDashedLines((float) (5.0 * zoom));
		glBegin(GL_LINES);
		for (Road segment : segments) {
			Point start = segment.getStart();
			Point end = segment.getEnd();
			glVertex2i(start.x, start.y);
			glVertex2i(end.x, end.y);
		}
		glEnd();
		endDashedLines();
	}

	private static void setPreviewColor(boolean valid) {
		glColor3f(valid ? 0.15f : 1.0f, valid ? 0.9f : 0.12f, 0.2f);
	}

	private static void beginDashedLines(float width) {
		glLineWidth(width);
		glLineStipple(1, (short) 0x00FF);
		glEnable(GL_LINE_STIPPLE);
	}

	private static void endDashedLines() {
		glDisable(GL_LINE_STIPPLE);
		glLineWidth(1.0f);
	}
}
