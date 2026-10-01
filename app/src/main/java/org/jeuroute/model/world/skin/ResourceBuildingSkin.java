package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import org.jeuroute.model.world.skin.Skin;
import org.jeuroute.model.world.ResourceType;
import org.jeuroute.utils.CharUtils;

public final class ResourceBuildingSkin implements Skin {

	private final ResourceType resourceType;

	public ResourceBuildingSkin(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	@Override
	public void display(Point start, Point end) {
		display(start, end, 0, resourceType.getStorageCapacity(), 1.0);
	}

	@Override
	public void display(Point start, Point end, double scale) {
		display(start, end, 0, resourceType.getStorageCapacity(), scale);
	}

	public void display(Point start, Point end, int stock, int capacity, double scale) {
		glColor3f(resourceType.getRed(), resourceType.getGreen(), resourceType.getBlue());
		glBegin(GL_QUADS);
		glVertex2i(start.x, start.y);
		glVertex2i(end.x, start.y);
		glVertex2i(end.x, end.y);
		glVertex2i(start.x, end.y);
		glEnd();

		glColor3f(0.12f, 0.14f, 0.16f);
		glBegin(GL_QUADS);
		glVertex2i(start.x + 5, start.y + 5);
		glVertex2i(end.x - 5, start.y + 5);
		glVertex2i(end.x - 5, end.y - 5);
		glVertex2i(start.x + 5, end.y - 5);
		glEnd();

		glColor3f(resourceType.getRed(), resourceType.getGreen(), resourceType.getBlue());
		glBegin(GL_QUADS);
		glVertex2i(start.x + 9, start.y + 9);
		glVertex2i(end.x - 9, start.y + 9);
		glVertex2i(end.x - 9, end.y - 9);
		glVertex2i(start.x + 9, end.y - 9);
		glEnd();

		glColor3f(0.08f, 0.10f, 0.12f);
		glBegin(GL_QUADS);
		glVertex2i(start.x + 5, end.y - 8);
		glVertex2i(end.x - 5, end.y - 8);
		glVertex2i(end.x - 5, end.y - 4);
		glVertex2i(start.x + 5, end.y - 4);
		glEnd();

		int barWidth = Math.max(0, end.x - start.x - 10);
		int filledWidth =
			capacity <= 0 ? 0 : (int) ((barWidth * Math.min(stock, capacity)) / (double) capacity);
		glColor3f(0.92f, 0.93f, 0.88f);
		glBegin(GL_QUADS);
		glVertex2i(start.x + 5, end.y - 8);
		glVertex2i(start.x + 5 + filledWidth, end.y - 8);
		glVertex2i(start.x + 5 + filledWidth, end.y - 4);
		glVertex2i(start.x + 5, end.y - 4);
		glEnd();
		glLineWidth((float) scale);
		glColor3f(0.08f, 0.10f, 0.12f);
		glBegin(GL_LINE_LOOP);
		glVertex2i(start.x, start.y);
		glVertex2i(end.x, start.y);
		glVertex2i(end.x, end.y);
		glVertex2i(start.x, end.y);
		glEnd();
		glLineWidth(1.0f);

		int centerX = (start.x + end.x) / 2;
		int centerY = (start.y + end.y) / 2;
		CharUtils.drawGlyph(
			resourceType.getSymbol(),
			centerX - 15,
			centerY - 16,
			0.08f,
			0.10f,
			0.12f
		);
		ResourceType requestedResourceType = resourceType.getRequiredResourceType();
		CharUtils.drawGlyph(
			requestedResourceType.getSymbol(),
			centerX + 3,
			centerY - 16,
			requestedResourceType.getRed(),
			requestedResourceType.getGreen(),
			requestedResourceType.getBlue()
		);
		CharUtils.drawNumber(stock, centerX - 8, centerY + 1, 0.95f, 0.93f, 0.86f);
	}
}
