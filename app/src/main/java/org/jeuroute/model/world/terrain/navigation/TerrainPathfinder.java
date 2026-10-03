package org.jeuroute.model.world.terrain.navigation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jeuroute.model.world.terrain.TerrainMap;

public final class TerrainPathfinder {

	private static final int[][] DIRECTIONS = {
		{ 0, -1 },
		{ 1, 0 },
		{ 0, 1 },
		{ -1, 0 },
		{ 1, -1 },
		{ 1, 1 },
		{ -1, 1 },
		{ -1, -1 },
	};

	private final TerrainMap terrain;

	public TerrainPathfinder(TerrainMap terrain) {
		this.terrain = Objects.requireNonNull(terrain, "terrain cannot be null");
	}

	public List<Point> findPath(Point start, Point destination, Random random) {
		Objects.requireNonNull(start, "start cannot be null");
		Objects.requireNonNull(destination, "destination cannot be null");
		Objects.requireNonNull(random, "random cannot be null");
		if (!terrain.isLand(start) || !terrain.isLand(destination)) {
			return List.of();
		}

		int startColumn = columnAt(start.x);
		int startRow = rowAt(start.y);
		int destinationColumn = columnAt(destination.x);
		int destinationRow = rowAt(destination.y);
		int startIndex = cellIndex(startColumn, startRow);
		int destinationIndex = cellIndex(destinationColumn, destinationRow);
		if (startIndex == destinationIndex) {
			return start.equals(destination) ? List.of() : List.of(new Point(destination));
		}

		int[] previous = new int[TerrainMap.COLUMNS * TerrainMap.ROWS];
		java.util.Arrays.fill(previous, -2);
		int[] pending = new int[previous.length];
		int pendingHead = 0;
		int pendingTail = 0;
		previous[startIndex] = -1;
		pending[pendingTail++] = startIndex;
		int[] directionOrder = shuffledDirectionOrder(random);

		while (pendingHead < pendingTail && previous[destinationIndex] == -2) {
			int currentIndex = pending[pendingHead++];
			int currentColumn = currentIndex % TerrainMap.COLUMNS;
			int currentRow = currentIndex / TerrainMap.COLUMNS;
			for (int directionIndex : directionOrder) {
				int nextColumn = currentColumn + DIRECTIONS[directionIndex][0];
				int nextRow = currentRow + DIRECTIONS[directionIndex][1];
				if (!canStep(currentColumn, currentRow, nextColumn, nextRow)) {
					continue;
				}
				int nextIndex = cellIndex(nextColumn, nextRow);
				if (previous[nextIndex] != -2) {
					continue;
				}
				previous[nextIndex] = currentIndex;
				pending[pendingTail++] = nextIndex;
			}
		}

		if (previous[destinationIndex] == -2) {
			return List.of();
		}

		List<Integer> cells = new ArrayList<>();
		for (int index = destinationIndex; index != -1; index = previous[index]) {
			cells.add(index);
		}
		Collections.reverse(cells);

		List<Point> waypoints = new ArrayList<>();
		for (int cell : cells) {
			Point center = cellCenter(cell);
			if (waypoints.isEmpty() && !terrain.containsSegment(start, center)) {
				return List.of();
			}
			if (waypoints.isEmpty() || !waypoints.getLast().equals(center)) {
				waypoints.add(center);
			}
		}
		Point lastPoint = waypoints.getLast();
		if (!lastPoint.equals(destination)) {
			if (!terrain.containsSegment(lastPoint, destination)) {
				return List.of();
			}
			waypoints.add(new Point(destination));
		}
		return List.copyOf(waypoints);
	}

	private boolean canStep(int fromColumn, int fromRow, int toColumn, int toRow) {
		if (!terrain.isLandCell(toColumn, toRow)) {
			return false;
		}
		int deltaColumn = toColumn - fromColumn;
		int deltaRow = toRow - fromRow;
		return (
			deltaColumn == 0 ||
			deltaRow == 0 ||
			(terrain.isLandCell(fromColumn + deltaColumn, fromRow) &&
				terrain.isLandCell(fromColumn, fromRow + deltaRow))
		);
	}

	private static int[] shuffledDirectionOrder(Random random) {
		int[] order = new int[DIRECTIONS.length];
		for (int index = 0; index < order.length; index++) {
			order[index] = index;
		}
		for (int index = order.length - 1; index > 0; index--) {
			int other = random.nextInt(index + 1);
			int value = order[index];
			order[index] = order[other];
			order[other] = value;
		}
		return order;
	}

	private static int columnAt(int x) {
		return Math.floorDiv(x - TerrainMap.ORIGIN_X, TerrainMap.CELL_SIZE);
	}

	private static int rowAt(int y) {
		return Math.floorDiv(y - TerrainMap.ORIGIN_Y, TerrainMap.CELL_SIZE);
	}

	private static int cellIndex(int column, int row) {
		return row * TerrainMap.COLUMNS + column;
	}

	private static Point cellCenter(int cellIndex) {
		int column = cellIndex % TerrainMap.COLUMNS;
		int row = cellIndex / TerrainMap.COLUMNS;
		return new Point(
			TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
			TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
		);
	}
}
