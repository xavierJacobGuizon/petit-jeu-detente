package org.jeuroute.gamecore.camera;

public record WorldViewBounds(double minX, double minY, double maxX, double maxY) {
	public static final WorldViewBounds UNBOUNDED = new WorldViewBounds(
		Double.NEGATIVE_INFINITY,
		Double.NEGATIVE_INFINITY,
		Double.POSITIVE_INFINITY,
		Double.POSITIVE_INFINITY
	);

	public WorldViewBounds {
		if (minX > maxX || minY > maxY) {
			throw new IllegalArgumentException("View bounds minimums must not exceed maximums");
		}
	}

	public boolean contains(double x, double y, double padding) {
		return (
			x + padding >= minX && x - padding <= maxX && y + padding >= minY && y - padding <= maxY
		);
	}

	public boolean intersectsRectangle(
		double centerX,
		double centerY,
		double halfWidth,
		double halfHeight
	) {
		return (
			centerX + halfWidth >= minX &&
			centerX - halfWidth <= maxX &&
			centerY + halfHeight >= minY &&
			centerY - halfHeight <= maxY
		);
	}

	public boolean intersectsSegment(
		double startX,
		double startY,
		double endX,
		double endY,
		double padding
	) {
		return (
			Math.max(startX, endX) + padding >= minX &&
			Math.min(startX, endX) - padding <= maxX &&
			Math.max(startY, endY) + padding >= minY &&
			Math.min(startY, endY) - padding <= maxY
		);
	}
}
