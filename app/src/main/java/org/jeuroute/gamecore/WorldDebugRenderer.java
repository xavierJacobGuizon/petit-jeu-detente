package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.records.world.WorldRenderData;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.transport.Vehicle;

public final class WorldDebugRenderer {

	private static final int MAX_CIRCLE_SEGMENTS = 16;
	private static final double DESTINATION_RADIUS = 8.0;
	private static final double[] CIRCLE_COSINES = new double[MAX_CIRCLE_SEGMENTS];
	private static final double[] CIRCLE_SINES = new double[MAX_CIRCLE_SEGMENTS];

	static {
		for (int segment = 0; segment < MAX_CIRCLE_SEGMENTS; segment++) {
			double angle = (Math.PI * 2.0 * segment) / MAX_CIRCLE_SEGMENTS;
			CIRCLE_COSINES[segment] = Math.cos(angle);
			CIRCLE_SINES[segment] = Math.sin(angle);
		}
	}

	private WorldDebugRenderer() {}

	public static void renderDestinations(WorldRenderData world, double zoom) {
		renderDestinations(world, zoom, WorldViewBounds.UNBOUNDED);
	}

	public static void renderDestinations(
		WorldRenderData world,
		double zoom,
		WorldViewBounds viewBounds
	) {
		int circleSegments = circleSegmentsForZoom(zoom);
		glLineWidth((float) Math.max(1.0, 1.5 * zoom));
		glColor3f(0.95f, 0.72f, 0.2f);
		glBegin(GL_LINES);
		for (Vehicle vehicle : world.vehicles()) {
			if (vehicle == null) {
				continue;
			}
			Point destination = vehicle.getDestination();
			if (destination != null) {
				drawVisibleDestination(
					vehicle.getPositionX(),
					vehicle.getPositionY(),
					destination.x,
					destination.y,
					circleSegments,
					zoom,
					viewBounds
				);
			}
		}
		glEnd();

		glColor3f(0.25f, 0.9f, 0.75f);
		Set<House> destinationHouses = Collections.newSetFromMap(new IdentityHashMap<>());
		glBegin(GL_LINES);
		for (Person person : world.people()) {
			if (person == null) {
				continue;
			}
			House destinationHouse = person.getDestinationHouse();
			if (
				destinationHouse != null &&
				viewBounds.intersectsSegment(
					person.getPreciseX(),
					person.getPreciseY(),
					destinationHouse.getPositionX(),
					destinationHouse.getPositionY(),
					0.75
				)
			) {
				drawDestinationLine(
					person.getPreciseX(),
					person.getPreciseY(),
					destinationHouse.getPositionX(),
					destinationHouse.getPositionY()
				);
				if (
					viewBounds.contains(
						destinationHouse.getPositionX(),
						destinationHouse.getPositionY(),
						DESTINATION_RADIUS
					)
				) {
					destinationHouses.add(destinationHouse);
				}
			}
		}
		glEnd();

		glBegin(GL_LINES);
		for (House destinationHouse : destinationHouses) {
			drawDestinationCircle(
				destinationHouse.getPositionX(),
				destinationHouse.getPositionY(),
				circleSegments
			);
		}
		glEnd();
		glLineWidth(1.0f);
	}

	private static int circleSegmentsForZoom(double zoom) {
		if (zoom < 0.5) {
			return 6;
		}
		if (zoom < 1.5) {
			return 10;
		}
		return MAX_CIRCLE_SEGMENTS;
	}

	private static void drawVisibleDestination(
		double originX,
		double originY,
		double destinationX,
		double destinationY,
		int circleSegments,
		double zoom,
		WorldViewBounds viewBounds
	) {
		if (viewBounds.intersectsSegment(originX, originY, destinationX, destinationY, 0.75)) {
			drawDestinationLine(originX, originY, destinationX, destinationY);
		}
		if (viewBounds.contains(destinationX, destinationY, DESTINATION_RADIUS)) {
			drawDestinationCircle(destinationX, destinationY, circleSegments);
		}
	}

	private static void drawDestinationLine(
		double originX,
		double originY,
		double destinationX,
		double destinationY
	) {
		glVertex2d(originX, originY);
		glVertex2d(destinationX, destinationY);
	}

	private static void drawDestinationCircle(
		double destinationX,
		double destinationY,
		int circleSegments
	) {
		for (int segment = 0; segment < circleSegments; segment++) {
			int nextSegment = (segment + 1) % circleSegments;
			glVertex2d(
				destinationX + CIRCLE_COSINES[segment] * DESTINATION_RADIUS,
				destinationY + CIRCLE_SINES[segment] * DESTINATION_RADIUS
			);
			glVertex2d(
				destinationX + CIRCLE_COSINES[nextSegment] * DESTINATION_RADIUS,
				destinationY + CIRCLE_SINES[nextSegment] * DESTINATION_RADIUS
			);
		}
	}
}
