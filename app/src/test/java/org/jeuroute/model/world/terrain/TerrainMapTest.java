package org.jeuroute.model.world.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.Random;
import org.jeuroute.manager.WorldMap;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.terrain.generation.IslandTerrainGenerator;
import org.jeuroute.model.world.terrain.generation.TerrainGenerator;
import org.junit.jupiter.api.Test;

class TerrainMapTest {

	@Test
	void islandUsesTheRoadGridAndKeepsTheStartingAreaOnLand() {
		TerrainMap terrain = new IslandTerrainGenerator().generate(new Random(7));

		assertEquals(25, TerrainMap.CELL_SIZE);
		assertTrue(terrain.isLand(new Point(650, 375)));
		assertTrue(terrain.isLand(new Point(75, 275)));
		assertFalse(terrain.isLand(new Point(TerrainMap.ORIGIN_X, TerrainMap.ORIGIN_Y)));
		assertTrue(terrain.getLandCellCount() > 1_000);
		assertTrue(terrain.getLandCellCount() < TerrainMap.COLUMNS * TerrainMap.ROWS);
	}

	@Test
	void sameRandomSeedProducesTheSameIslandAndWaterSegmentsAreRejected() {
		TerrainMap first = new IslandTerrainGenerator().generate(new Random(42));
		TerrainMap second = new IslandTerrainGenerator().generate(new Random(42));

		for (int row = 0; row < TerrainMap.ROWS; row++) {
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				assertEquals(first.isLandCell(column, row), second.isLandCell(column, row));
			}
		}
		assertFalse(first.containsSegment(new Point(650, 375), new Point(-350, 375)));
		assertTrue(first.containsSegment(new Point(650, 375), new Point(300, 375)));

		RoadGraph graph = new RoadGraph(first);
		assertFalse(graph.canAddRoad(new Point(650, 375), new Point(-350, 375)));
		assertTrue(graph.canAddRoad(new Point(650, 375), new Point(300, 375)));
	}

	@Test
	void alternativeGeneratorCanKeepTheIslandType() {
		boolean[][] alternateIsland = new boolean[TerrainMap.ROWS][TerrainMap.COLUMNS];
		int landCellCount = 0;
		for (int row = 0; row < TerrainMap.ROWS; row++) {
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				double normalizedX = (column - TerrainMap.COLUMNS / 2.0) / 18.0;
				double normalizedY = (row - TerrainMap.ROWS / 2.0) / 14.0;
				alternateIsland[row][column] =
					normalizedX * normalizedX + normalizedY * normalizedY <= 1.0;
				if (alternateIsland[row][column]) {
					landCellCount++;
				}
			}
		}
		TerrainGenerator alternateGenerator = new TerrainGenerator() {
			@Override
			public TerrainType type() {
				return TerrainType.ISLAND;
			}

			@Override
			public TerrainMap generate(Random random) {
				return new TerrainMap(TerrainType.ISLAND, alternateIsland);
			}
		};

		WorldMap worldMap = new WorldMap(alternateGenerator, new Random(3));

		assertEquals(TerrainType.ISLAND, worldMap.getTerrainMap().getType());
		assertEquals(landCellCount, worldMap.getTerrainMap().getLandCellCount());
	}
}
