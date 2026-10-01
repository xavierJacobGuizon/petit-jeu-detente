package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import org.jeuroute.model.world.enums.ResourceType;

public class VehicleSkin implements Skin {

	private final float r;
	private final float g;
	private final float b;

	public VehicleSkin(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	@Override
	public void display(Point start, Point end) {
		display(start, end, 0, null, 1.0);
	}

	@Override
	public void display(Point start, Point end, double scale) {
		display(start, end, 0, null, scale);
	}

	public void display(
		Point start,
		Point end,
		int cargoUnits,
		ResourceType cargoType,
		double scale
	) {
		glColor3f(r, g, b);
		glBegin(GL_QUADS);
		glVertex2d(start.x, start.y);
		glVertex2d(end.x, start.y);
		glVertex2d(end.x, end.y);
		glVertex2d(start.x, end.y);
		glEnd();

		double width = end.x - start.x;
		double height = end.y - start.y;
		double inset = Math.max(3.0, Math.min(width, height) * 0.16);
		double gap = Math.max(1.0, Math.min(width, height) * 0.06);
		double slotWidth = (width - inset * 2.0 - gap) / 2.0;
		double slotHeight = (height - inset * 2.0 - gap) / 2.0;
		for (int slot = 0; slot < 4; slot++) {
			int column = slot % 2;
			int row = slot / 2;
			double left = start.x + inset + column * (slotWidth + gap);
			double top = start.y + inset + row * (slotHeight + gap);
			boolean occupied = slot < cargoUnits && cargoType != null;

			glColor3f(
				occupied ? cargoType.getRed() : 0.10f,
				occupied ? cargoType.getGreen() : 0.12f,
				occupied ? cargoType.getBlue() : 0.15f
			);
			glBegin(GL_QUADS);
			glVertex2d(left, top);
			glVertex2d(left + slotWidth, top);
			glVertex2d(left + slotWidth, top + slotHeight);
			glVertex2d(left, top + slotHeight);
			glEnd();

			glColor3f(0.92f, 0.92f, 0.86f);
			glLineWidth((float) Math.max(1.0, scale));
			glBegin(GL_LINE_LOOP);
			glVertex2d(left, top);
			glVertex2d(left + slotWidth, top);
			glVertex2d(left + slotWidth, top + slotHeight);
			glVertex2d(left, top + slotHeight);
			glEnd();
		}
		glLineWidth(1.0f);
	}
}
