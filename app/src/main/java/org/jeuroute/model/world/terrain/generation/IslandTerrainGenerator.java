package org.jeuroute.model.world.terrain.generation;

import java.util.Objects;
import java.util.Random;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.terrain.TerrainMap;

public final class IslandTerrainGenerator implements TerrainGenerator {

	private static final double ISLAND_RADIUS_X = 850.0;
	private static final double ISLAND_RADIUS_Y = 500.0;
	private static final double FIRST_SHAPE_VARIATION = 0.08;
	private static final double SECOND_SHAPE_VARIATION = 0.045;

	@Override
	public TerrainType type() {
		return TerrainType.ISLAND;
	}

	@Override
	public TerrainMap generate(Random random) {
		Objects.requireNonNull(random, "random cannot be null");
		boolean[][] land = new boolean[TerrainMap.ROWS][TerrainMap.COLUMNS];
		int centerX = TerrainMap.gridX(TerrainMap.COLUMNS / 2);
		int centerY = TerrainMap.gridY(TerrainMap.ROWS / 2);
		double firstPhase = random.nextDouble() * Math.PI * 2.0;
		double secondPhase = random.nextDouble() * Math.PI * 2.0;

		for (int row = 0; row < TerrainMap.ROWS; row++) {
			int y = TerrainMap.gridY(row);
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				int x = TerrainMap.gridX(column);
				double normalizedX = (x - centerX) / ISLAND_RADIUS_X;
				double normalizedY = (y - centerY) / ISLAND_RADIUS_Y;
				double angle = Math.atan2(normalizedY, normalizedX);
				double radius =
					1.0 +
					FIRST_SHAPE_VARIATION * Math.sin(3.0 * angle + firstPhase) +
					SECOND_SHAPE_VARIATION * Math.cos(5.0 * angle + secondPhase);
				land[row][column] =
					normalizedX * normalizedX + normalizedY * normalizedY <= radius * radius;
			}
		}

		return new TerrainMap(TerrainType.ISLAND, land);
	}
}
