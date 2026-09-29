package org.jeuroute.model.interfaces.jouetpeau;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import org.jeuroute.model.interfaces.Peau;

public final class StationPeau implements Peau {

	@Override
	public void display(Point position, Point unused) {
		display(position, unused, 1.0);
	}

	@Override
	public void display(Point position, Point unused, double scale) {
		glColor3f(0.95f, 0.68f, 0.18f);
		glBegin(GL_QUADS);
		glVertex2i(position.x - 9, position.y - 9);
		glVertex2i(position.x + 9, position.y - 9);
		glVertex2i(position.x + 9, position.y + 9);
		glVertex2i(position.x - 9, position.y + 9);
		glEnd();

		glColor3f(0.25f, 0.20f, 0.10f);
		glLineWidth((float) (2.0 * scale));
		glBegin(GL_LINES);
		glVertex2i(position.x - 5, position.y);
		glVertex2i(position.x + 5, position.y);
		glVertex2i(position.x, position.y - 5);
		glVertex2i(position.x, position.y + 5);
		glEnd();
		glLineWidth(1.0f);
	}
}
