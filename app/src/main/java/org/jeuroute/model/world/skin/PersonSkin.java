package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Point;

public final class PersonSkin {

	public void display(Point position, double ignoredZoom) {
		double headRadius = 4.0;
		glColor3f(0.92f, 0.72f, 0.38f);
		glBegin(GL_TRIANGLE_FAN);
		glVertex2d(position.x, position.y - 5.0);
		for (int segment = 0; segment <= 12; segment++) {
			double angle = (segment * Math.PI * 2.0) / 12.0;
			glVertex2d(
				position.x + Math.cos(angle) * headRadius,
				position.y - 5.0 + Math.sin(angle) * headRadius
			);
		}
		glEnd();

		glColor3f(0.22f, 0.48f, 0.78f);
		glBegin(GL_QUADS);
		glVertex2d(position.x - 3.0, position.y - 1.0);
		glVertex2d(position.x + 3.0, position.y - 1.0);
		glVertex2d(position.x + 4.0, position.y + 6.0);
		glVertex2d(position.x - 4.0, position.y + 6.0);
		glEnd();

		glColor3f(0.16f, 0.18f, 0.2f);
		glBegin(GL_TRIANGLES);
		glVertex2d(position.x - 2.0, position.y + 5.0);
		glVertex2d(position.x, position.y + 5.0);
		glVertex2d(position.x - 2.0, position.y + 10.0);
		glVertex2d(position.x, position.y + 5.0);
		glVertex2d(position.x + 2.0, position.y + 5.0);
		glVertex2d(position.x + 2.0, position.y + 10.0);
		glEnd();
	}
}
