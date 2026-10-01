package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import org.jeuroute.model.world.transport.Depot;

public final class DepotSkin implements Skin {

	@Override
	public void display(Point position, Point accessPosition) {
		display(position, accessPosition, 1.0);
	}

	@Override
	public void display(Point position, Point accessPosition, double scale) {
		glColor3f(0.58f, 0.30f, 0.78f);
		glBegin(GL_QUADS);
		glVertex2i(position.x - Depot.HALF_SIZE, position.y - Depot.HALF_SIZE);
		glVertex2i(position.x + Depot.HALF_SIZE, position.y - Depot.HALF_SIZE);
		glVertex2i(position.x + Depot.HALF_SIZE, position.y + Depot.HALF_SIZE);
		glVertex2i(position.x - Depot.HALF_SIZE, position.y + Depot.HALF_SIZE);
		glEnd();

		glColor3f(0.20f, 0.12f, 0.27f);
		glBegin(GL_QUADS);
		glVertex2i(position.x - 8, position.y + 10);
		glVertex2i(position.x + 8, position.y + 10);
		glVertex2i(position.x + 8, accessPosition.y);
		glVertex2i(position.x - 8, accessPosition.y);
		glEnd();

		glLineWidth((float) (2.0 * scale));
		glBegin(GL_LINE_LOOP);
		glVertex2i(position.x - Depot.HALF_SIZE, position.y - Depot.HALF_SIZE);
		glVertex2i(position.x + Depot.HALF_SIZE, position.y - Depot.HALF_SIZE);
		glVertex2i(position.x + Depot.HALF_SIZE, position.y + Depot.HALF_SIZE);
		glVertex2i(position.x - Depot.HALF_SIZE, position.y + Depot.HALF_SIZE);
		glEnd();
		glLineWidth(1.0f);
	}
}
