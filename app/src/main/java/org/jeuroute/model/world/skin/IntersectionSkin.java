package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Point;

public class IntersectionSkin implements Skin {

	private final float r;
	private final float g;
	private final float b;

	public IntersectionSkin(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	@Override
	public void display(Point start, Point end) {
		glColor3f(r, g, b);
		glBegin(GL_QUADS);
		glVertex2d(start.x - 5, start.y - 5);
		glVertex2d(start.x + 5, start.y - 5);
		glVertex2d(start.x + 5, start.y + 5);
		glVertex2d(start.x - 5, start.y + 5);
		glEnd();
	}
}
