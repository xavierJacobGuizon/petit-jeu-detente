package org.jeuroute.model.world.terrain;

import java.awt.Point;
import java.util.Objects;
import org.jeuroute.model.world.enums.TerrainType;

public final class TerrainMap {

	public static final int CELL_SIZE = 25;
	public static final int COLUMNS = 80;
	public static final int ROWS = 48;
	public static final int ORIGIN_X = -350;
	public static final int ORIGIN_Y = -225;

	private final TerrainType type;
	private final boolean[][] land;
	private final int landCellCount;

	public TerrainMap(TerrainType type, boolean[][] land) {
		this.type = Objects.requireNonNull(type, "type cannot be null");
		Objects.requireNonNull(land, "land cannot be null");
		if (land.length != ROWS) {
			throw new IllegalArgumentException("Terrain must have exactly " + ROWS + " rows");
		}
		this.land = new boolean[ROWS][COLUMNS];
		int cellCount = 0;
		for (int row = 0; row < ROWS; row++) {
			if (land[row] == null || land[row].length != COLUMNS) {
				throw new IllegalArgumentException(
					"Every terrain row must have exactly " + COLUMNS + " cells"
				);
			}
			this.land[row] = land[row].clone();
			for (boolean isLand : this.land[row]) {
				if (isLand) {
					cellCount++;
				}
			}
		}
		if (cellCount == 0) {
			throw new IllegalArgumentException("Terrain must contain at least one land cell");
		}
		landCellCount = cellCount;
	}

	public TerrainType getType() {
		return type;
	}

	public boolean isLand(Point position) {
		Objects.requireNonNull(position, "position cannot be null");
		return isLand(position.x, position.y);
	}

	public boolean isLand(int x, int y) {
		int column = Math.floorDiv(x - ORIGIN_X, CELL_SIZE);
		int row = Math.floorDiv(y - ORIGIN_Y, CELL_SIZE);
		return isLandCell(column, row);
	}

	public boolean containsSegment(Point start, Point end) {
		Objects.requireNonNull(start, "start cannot be null");
		Objects.requireNonNull(end, "end cannot be null");
		int sampleCount = Math.max(1, (int) Math.ceil(start.distance(end) / (CELL_SIZE / 2.0)));
		for (int sampleIndex = 0; sampleIndex <= sampleCount; sampleIndex++) {
			double ratio = sampleIndex / (double) sampleCount;
			int x = (int) Math.round(start.x + (end.x - start.x) * ratio);
			int y = (int) Math.round(start.y + (end.y - start.y) * ratio);
			if (!isLand(x, y)) {
				return false;
			}
		}
		return true;
	}

	public boolean isLandCell(int column, int row) {
		return column >= 0 && column < COLUMNS && row >= 0 && row < ROWS && land[row][column];
	}

	public int getFirstLandColumn(int row) {
		for (int column = 0; column < COLUMNS; column++) {
			if (isLandCell(column, row)) {
				return column;
			}
		}
		return -1;
	}

	public int getLastLandColumn(int row) {
		for (int column = COLUMNS - 1; column >= 0; column--) {
			if (isLandCell(column, row)) {
				return column;
			}
		}
		return -1;
	}

	public int getFirstLandRow(int column) {
		for (int row = 0; row < ROWS; row++) {
			if (isLandCell(column, row)) {
				return row;
			}
		}
		return -1;
	}

	public int getLastLandRow(int column) {
		for (int row = ROWS - 1; row >= 0; row--) {
			if (isLandCell(column, row)) {
				return row;
			}
		}
		return -1;
	}

	public int getLandCellCount() {
		return landCellCount;
	}

	public static int gridX(int column) {
		return ORIGIN_X + column * CELL_SIZE;
	}

	public static int gridY(int row) {
		return ORIGIN_Y + row * CELL_SIZE;
	}
}
