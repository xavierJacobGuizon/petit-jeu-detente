package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.util.List;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.world.settlement.Person;

public final class PersonRenderer {

	private static final int HEAD_SEGMENTS = 12;
	private static final double[] HEAD_COSINES = new double[HEAD_SEGMENTS + 1];
	private static final double[] HEAD_SINES = new double[HEAD_SEGMENTS + 1];

	static {
		for (int segment = 0; segment <= HEAD_SEGMENTS; segment++) {
			double angle = (segment * Math.PI * 2.0) / HEAD_SEGMENTS;
			HEAD_COSINES[segment] = Math.cos(angle);
			HEAD_SINES[segment] = Math.sin(angle);
		}
	}

	private PersonRenderer() {}

	public static void render(List<Person> people, double zoom) {
		render(people, zoom, WorldViewBounds.UNBOUNDED);
	}

	public static void render(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		if (zoom < 0.3) {
			renderLowDetail(people, zoom, viewBounds);
			return;
		}
		renderHeads(people, zoom, viewBounds);
		renderBodies(people, zoom, viewBounds);
		renderLegs(people, zoom, viewBounds);
	}

	private static void renderLowDetail(
		List<Person> people,
		double zoom,
		WorldViewBounds viewBounds
	) {
		glColor3f(0.22f, 0.48f, 0.78f);
		glPointSize((float) Math.max(1.0, 6.0 * zoom));
		glBegin(GL_POINTS);
		for (Person person : people) {
			if (person != null && isVisible(person, zoom, viewBounds)) {
				glVertex2d(person.getPreciseX(), person.getPreciseY());
			}
		}
		glEnd();
		glPointSize(1.0f);
	}

	private static void renderHeads(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		glColor3f(0.92f, 0.72f, 0.38f);
		glBegin(GL_TRIANGLES);
		for (Person person : people) {
			if (person == null || !isVisible(person, zoom, viewBounds)) {
				continue;
			}
			double centerX = person.getPreciseX();
			double centerY = person.getPreciseY() - 5.0;
			double radius = 4.0;
			for (int segment = 0; segment < HEAD_SEGMENTS; segment++) {
				glVertex2d(centerX, centerY);
				glVertex2d(
					centerX + HEAD_COSINES[segment] * radius,
					centerY + HEAD_SINES[segment] * radius
				);
				glVertex2d(
					centerX + HEAD_COSINES[segment + 1] * radius,
					centerY + HEAD_SINES[segment + 1] * radius
				);
			}
		}
		glEnd();
	}

	private static void renderBodies(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		glColor3f(0.22f, 0.48f, 0.78f);
		glBegin(GL_QUADS);
		for (Person person : people) {
			if (person == null || !isVisible(person, zoom, viewBounds)) {
				continue;
			}
			double x = person.getPreciseX();
			double y = person.getPreciseY();
			glVertex2d(x - 3.0, y - 1.0);
			glVertex2d(x + 3.0, y - 1.0);
			glVertex2d(x + 4.0, y + 6.0);
			glVertex2d(x - 4.0, y + 6.0);
		}
		glEnd();
	}

	private static void renderLegs(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		glColor3f(0.16f, 0.18f, 0.2f);
		glBegin(GL_TRIANGLES);
		for (Person person : people) {
			if (person == null || !isVisible(person, zoom, viewBounds)) {
				continue;
			}
			double x = person.getPreciseX();
			double y = person.getPreciseY();
			glVertex2d(x - 2.0, y + 5.0);
			glVertex2d(x, y + 5.0);
			glVertex2d(x - 2.0, y + 10.0);
			glVertex2d(x, y + 5.0);
			glVertex2d(x + 2.0, y + 5.0);
			glVertex2d(x + 2.0, y + 10.0);
		}
		glEnd();
	}

	private static boolean isVisible(Person person, double zoom, WorldViewBounds viewBounds) {
		return viewBounds.contains(person.getPreciseX(), person.getPreciseY(), 10.0);
	}
}
