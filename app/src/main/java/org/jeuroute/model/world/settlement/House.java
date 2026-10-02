package org.jeuroute.model.world.settlement;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.world.skin.HouseSkin;

public final class House {

	public static final int HALF_SIZE = 12;

	private final Point position;
	private final HouseSkin skin = new HouseSkin();

	public House(Point position) {
		this.position = new Point(Objects.requireNonNull(position, "position cannot be null"));
	}

	public Point getPosition() {
		return new Point(position);
	}

	public void display(double zoom) {
		skin.display(position, zoom);
	}
}
