package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Point;
import org.jeuroute.model.world.skin.Skin;

public class RoadSkin implements Skin {

	private float r;
	private float g;
	private float b;

	public RoadSkin(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	@Override
	public void display(Point start, Point end) {
		display(start, end, 1.0);
	}

	@Override
	public void display(Point start, Point end, double scale) {
		glColor3f(this.r, this.g, this.b);
		glLineWidth((float) (10.0 * scale));

		glBegin(GL_LINES);
		glVertex2d(start.x, start.y);
		glVertex2d(end.x, end.y);
		glEnd();

		glLineWidth(1.0f);
	}
}
