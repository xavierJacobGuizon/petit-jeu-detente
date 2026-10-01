package org.jeuroute.model.world.skin;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.Map;
import org.jeuroute.model.world.skin.Skin;
import org.jeuroute.model.world.ResourceType;
import org.jeuroute.utils.CharUtils;

public final class StationSkin implements Skin {

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

	public void displayCaptureRadius(Point position, double radius, double scale) {
		displayCaptureRadius(position, radius, scale, 0.20f, 0.72f, 0.64f);
	}

	public static void displayCaptureRadiusPreview(
		Point position,
		double radius,
		double scale,
		boolean valid
	) {
		displayCaptureRadius(
			position,
			radius,
			scale,
			valid ? 0.15f : 1.0f,
			valid ? 0.9f : 0.12f,
			0.2f
		);
	}

	private static void displayCaptureRadius(
		Point position,
		double radius,
		double scale,
		float red,
		float green,
		float blue
	) {
		glColor3f(red, green, blue);
		glLineWidth((float) scale);
		glBegin(GL_LINE_LOOP);
		for (int segment = 0; segment < 64; segment++) {
			double angle = (Math.PI * 2.0 * segment) / 64.0;
			glVertex2d(
				position.x + Math.cos(angle) * radius,
				position.y + Math.sin(angle) * radius
			);
		}
		glEnd();
		glLineWidth(1.0f);
	}

	public void displayCapturedResources(
		Point position,
		Map<ResourceType, Integer> stocks,
		Map<ResourceType, Integer> demands
	) {
		int markerIndex = 0;
		for (ResourceType resourceType : ResourceType.values()) {
			Integer stock = stocks.get(resourceType);
			Integer demand = demands.get(resourceType);
			if (stock == null && demand == null) {
				continue;
			}
			int markerY = position.y - 24 + markerIndex * 34;
			if (stock != null) {
				CharUtils.drawGlyph(
					resourceType.getSymbol(),
					position.x + 14,
					markerY,
					0.95f,
					0.93f,
					0.86f
				);
				CharUtils.drawNumber(stock, position.x + 28, markerY, 0.95f, 0.93f, 0.86f);
			}
			if (demand != null) {
				int demandY = markerY + (stock == null ? 0 : 16);
				CharUtils.drawGlyph(
					resourceType.getSymbol(),
					position.x + 14,
					demandY,
					resourceType.getRed(),
					resourceType.getGreen(),
					resourceType.getBlue()
				);
				CharUtils.drawNumber(
					demand,
					position.x + 28,
					demandY,
					resourceType.getRed(),
					resourceType.getGreen(),
					resourceType.getBlue()
				);
			}
			markerIndex++;
		}
	}
}
