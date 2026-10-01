package org.jeuroute.model.records.hud;

public record HudBounds(int x, int y, int width, int height) {
	public boolean contains(double pointX, double pointY) {
		return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
	}

	public boolean intersects(HudBounds other) {
		return (
			x < other.x + other.width &&
			x + width > other.x &&
			y < other.y + other.height &&
			y + height > other.y
		);
	}
}
