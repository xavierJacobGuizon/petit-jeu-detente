package org.jeuroute.model.interfaces;

import java.awt.Point;

public interface Peau {
	public void display(Point start, Point end);

	public default void display(Point start, Point end, double scale) {
		display(start, end);
	}
}
