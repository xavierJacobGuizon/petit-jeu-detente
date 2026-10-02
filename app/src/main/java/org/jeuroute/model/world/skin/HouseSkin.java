package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.awt.Point;
import org.jeuroute.model.world.settlement.House;

public final class HouseSkin {

	public void display(Point position, double zoom) {
		int halfWidth = (int) Math.round(House.HALF_SIZE * zoom);
		int halfWallHeight = (int) Math.round(8.0 * zoom);
		int roofHeight = (int) Math.round(10.0 * zoom);
		int x = position.x;
		int y = position.y;

		glColor3f(0.72f, 0.57f, 0.37f);
		glBegin(GL_QUADS);
		glVertex2i(x - halfWidth, y - halfWallHeight);
		glVertex2i(x + halfWidth, y - halfWallHeight);
		glVertex2i(x + halfWidth, y + halfWallHeight);
		glVertex2i(x - halfWidth, y + halfWallHeight);
		glEnd();

		glColor3f(0.45f, 0.20f, 0.16f);
		glBegin(GL_TRIANGLES);
		glVertex2i(x - halfWidth - 2, y - halfWallHeight);
		glVertex2i(x + halfWidth + 2, y - halfWallHeight);
		glVertex2i(x, y - halfWallHeight - roofHeight);
		glEnd();

		glColor3f(0.24f, 0.29f, 0.31f);
		glBegin(GL_QUADS);
		glVertex2i(x - 3, y + halfWallHeight);
		glVertex2i(x + 3, y + halfWallHeight);
		glVertex2i(x + 3, y + 2);
		glVertex2i(x - 3, y + 2);
		glEnd();
	}
}
