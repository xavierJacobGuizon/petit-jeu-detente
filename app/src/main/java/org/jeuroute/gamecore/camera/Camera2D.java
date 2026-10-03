package org.jeuroute.gamecore.camera;

import static org.lwjgl.opengl.GL11.glScaled;
import static org.lwjgl.opengl.GL11.glTranslated;

import java.awt.Point;

public final class Camera2D {

	private static final double MIN_ZOOM = 0.25;
	private static final double MAX_ZOOM = 4.0;
	private static final double ZOOM_FACTOR = 1.12;
	private static final double PAN_ACCELERATION = 900.0;
	private static final double MAX_PAN_SPEED = 650.0;

	private double centerX;
	private double centerY;
	private double zoom = 1.0;
	private double inputX;
	private double inputY;
	private double velocityX;
	private double velocityY;

	public Camera2D(double centerX, double centerY) {
		this.centerX = centerX;
		this.centerY = centerY;
	}

	public double getZoom() {
		return zoom;
	}

	public double getPanSpeed() {
		return Math.hypot(velocityX, velocityY);
	}

	public Point screenToWorld(
		double screenX,
		double screenY,
		int viewportWidth,
		int viewportHeight
	) {
		return new Point(
			(int) Math.round(centerX + (screenX - viewportWidth / 2.0) / zoom),
			(int) Math.round(centerY + (screenY - viewportHeight / 2.0) / zoom)
		);
	}

	public Point worldToScreen(Point worldPosition, int viewportWidth, int viewportHeight) {
		return new Point(
			(int) Math.round(viewportWidth / 2.0 + (worldPosition.x - centerX) * zoom),
			(int) Math.round(viewportHeight / 2.0 + (worldPosition.y - centerY) * zoom)
		);
	}

	public WorldViewBounds getVisibleWorldBounds(int viewportWidth, int viewportHeight) {
		double halfWidth = viewportWidth / (2.0 * zoom);
		double halfHeight = viewportHeight / (2.0 * zoom);
		return new WorldViewBounds(
			centerX - halfWidth,
			centerY - halfHeight,
			centerX + halfWidth,
			centerY + halfHeight
		);
	}

	public void panByScreenPixels(double horizontalPixels, double verticalPixels) {
		centerX += horizontalPixels / zoom;
		centerY += verticalPixels / zoom;
	}

	public void setMovementInput(double horizontal, double vertical) {
		double inputLength = Math.hypot(horizontal, vertical);
		if (inputLength > 1.0) {
			horizontal /= inputLength;
			vertical /= inputLength;
		}
		inputX = horizontal;
		inputY = vertical;
	}

	public void updateMovement(double deltaSeconds) {
		if (deltaSeconds <= 0.0) {
			return;
		}

		double targetVelocityX = inputX * MAX_PAN_SPEED;
		double targetVelocityY = inputY * MAX_PAN_SPEED;
		double differenceX = targetVelocityX - velocityX;
		double differenceY = targetVelocityY - velocityY;
		double differenceLength = Math.hypot(differenceX, differenceY);
		double maximumChange = PAN_ACCELERATION * deltaSeconds;
		if (differenceLength <= maximumChange) {
			velocityX = targetVelocityX;
			velocityY = targetVelocityY;
		} else {
			velocityX += (differenceX / differenceLength) * maximumChange;
			velocityY += (differenceY / differenceLength) * maximumChange;
		}

		panByScreenPixels(velocityX * deltaSeconds, velocityY * deltaSeconds);
	}

	public void zoomAt(
		double scrollAmount,
		double screenX,
		double screenY,
		int viewportWidth,
		int viewportHeight
	) {
		if (scrollAmount == 0.0) {
			return;
		}
		Point worldAnchor = screenToWorld(screenX, screenY, viewportWidth, viewportHeight);
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * Math.pow(ZOOM_FACTOR, scrollAmount)));
		centerX = worldAnchor.x - (screenX - viewportWidth / 2.0) / zoom;
		centerY = worldAnchor.y - (screenY - viewportHeight / 2.0) / zoom;
	}

	public void apply(int viewportWidth, int viewportHeight) {
		glTranslated(
			viewportWidth / 2.0 - centerX * zoom,
			viewportHeight / 2.0 - centerY * zoom,
			0.0
		);
		glScaled(zoom, zoom, 1.0);
	}
}
