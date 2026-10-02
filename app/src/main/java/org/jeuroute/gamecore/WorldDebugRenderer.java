package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import org.jeuroute.model.records.world.WorldRenderData;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.transport.Vehicle;

public final class WorldDebugRenderer {

	private static final int CIRCLE_SEGMENTS = 24;
	private static final double DESTINATION_RADIUS = 8.0;

	private WorldDebugRenderer() {}

	public static void renderDestinations(WorldRenderData world, double zoom) {
		for (Vehicle vehicle : world.vehicles()) {
			if (vehicle != null) {
				renderDestination(
					vehicle.getPosition(),
					vehicle.getDestination(),
					0.95f,
					0.72f,
					0.2f,
					zoom
				);
			}
		}
		for (Person person : world.people()) {
			if (person == null) {
				continue;
			}
			House destinationHouse = person.getDestinationHouse();
			if (destinationHouse != null) {
				renderDestination(
					person.getPosition(),
					destinationHouse.getPosition(),
					0.25f,
					0.9f,
					0.75f,
					zoom
				);
			}
		}
	}

	private static void renderDestination(
		Point origin,
		Point destination,
		float red,
		float green,
		float blue,
		double zoom
	) {
		if (destination == null) {
			return;
		}

		glColor3f(red, green, blue);
		glLineWidth((float) Math.max(1.0, 1.5 * zoom));
		glBegin(GL_LINES);
		glVertex2i(origin.x, origin.y);
		glVertex2i(destination.x, destination.y);
		glEnd();

		glBegin(GL_LINE_LOOP);
		for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
			double angle = (Math.PI * 2.0 * segment) / CIRCLE_SEGMENTS;
			glVertex2d(
				destination.x + Math.cos(angle) * DESTINATION_RADIUS,
				destination.y + Math.sin(angle) * DESTINATION_RADIUS
			);
		}
		glEnd();
		glLineWidth(1.0f);
	}
}
