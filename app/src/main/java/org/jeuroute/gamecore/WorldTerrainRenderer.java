package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import org.jeuroute.model.world.terrain.TerrainMap;

public final class WorldTerrainRenderer {

	private WorldTerrainRenderer() {}

	public static void render(TerrainMap terrain, double zoom) {
		renderLandCells(terrain);
		renderTerrainEdges(terrain, zoom);
	}

	private static void renderLandCells(TerrainMap terrain) {
		glBegin(GL_QUADS);
		for (int row = 0; row < TerrainMap.ROWS; row++) {
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				if (!terrain.isLandCell(column, row)) {
					continue;
				}
				float shade = ((row * 17 + column * 31) % 5) * 0.006f;
				glColor3f(0.13f + shade, 0.27f + shade, 0.17f + shade);
				int x = TerrainMap.gridX(column);
				int y = TerrainMap.gridY(row);
				glVertex2i(x, y);
				glVertex2i(x + TerrainMap.CELL_SIZE, y);
				glVertex2i(x + TerrainMap.CELL_SIZE, y + TerrainMap.CELL_SIZE);
				glVertex2i(x, y + TerrainMap.CELL_SIZE);
			}
		}
		glEnd();
	}

	private static void renderTerrainEdges(TerrainMap terrain, double zoom) {
		glColor3f(0.09f, 0.19f, 0.13f);
		glLineWidth(1.0f);
		glBegin(GL_LINES);
		int gridStep = zoom >= 0.75 ? 1 : 4;
		for (int row = 0; row < TerrainMap.ROWS; row++) {
			for (int column = 0; column < TerrainMap.COLUMNS; column++) {
				if (!terrain.isLandCell(column, row)) {
					continue;
				}
				int x = TerrainMap.gridX(column);
				int y = TerrainMap.gridY(row);
				boolean northIsWater = !terrain.isLandCell(column, row - 1);
				boolean westIsWater = !terrain.isLandCell(column - 1, row);
				boolean eastIsWater = !terrain.isLandCell(column + 1, row);
				boolean southIsWater = !terrain.isLandCell(column, row + 1);
				if (northIsWater || row % gridStep == 0) {
					drawTerrainEdge(x, y, x + TerrainMap.CELL_SIZE, y, northIsWater);
				}
				if (westIsWater || column % gridStep == 0) {
					drawTerrainEdge(x, y, x, y + TerrainMap.CELL_SIZE, westIsWater);
				}
				if (eastIsWater) {
					drawTerrainEdge(
						x + TerrainMap.CELL_SIZE,
						y,
						x + TerrainMap.CELL_SIZE,
						y + TerrainMap.CELL_SIZE,
						true
					);
				}
				if (southIsWater) {
					drawTerrainEdge(
						x,
						y + TerrainMap.CELL_SIZE,
						x + TerrainMap.CELL_SIZE,
						y + TerrainMap.CELL_SIZE,
						true
					);
				}
			}
		}
		glEnd();
		glLineWidth(1.0f);
	}

	private static void drawTerrainEdge(int startX, int startY, int endX, int endY, boolean coast) {
		if (coast) {
			glColor3f(0.28f, 0.46f, 0.38f);
		} else {
			glColor3f(0.09f, 0.19f, 0.13f);
		}
		glVertex2i(startX, startY);
		glVertex2i(endX, endY);
	}
}
