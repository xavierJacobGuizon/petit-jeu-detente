package org.jeuroute.model.world.skin;

import java.awt.Point;

public interface Skin {
	public void display(Point start, Point end);

	public default void display(Point start, Point end, double scale) {
		display(start, end);
	}
}
