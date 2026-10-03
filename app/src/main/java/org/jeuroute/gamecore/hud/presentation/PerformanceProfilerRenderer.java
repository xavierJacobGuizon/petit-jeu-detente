package org.jeuroute.gamecore.hud.presentation;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2i;

import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.model.records.utils.TextRun;
import org.jeuroute.utils.CharUtils;

public final class PerformanceProfilerRenderer {

	private static final int PANEL_X = 16;
	private static final int PANEL_Y = 156;
	private static final int PANEL_WIDTH = 660;
	private static final int ROW_HEIGHT = 20;
	private static final int PANEL_PADDING = 10;
	private static final float[] TEXT_COLOR = { 0.86f, 0.92f, 0.89f };
	private final TextRun[] textRuns = new TextRun[PerformanceProfiler.Section.values().length + 1];
	private long renderedSnapshotVersion = Long.MIN_VALUE;
	private int renderedWindowWidth = -1;

	public void render(PerformanceProfiler profiler, int windowWidth) {
		profiler.refreshSnapshot(System.nanoTime());
		if (
			renderedSnapshotVersion != profiler.snapshotVersion() ||
			renderedWindowWidth != windowWidth
		) {
			updateTextRuns(profiler.displayLines(), windowWidth);
			renderedSnapshotVersion = profiler.snapshotVersion();
			renderedWindowWidth = windowWidth;
		}

		int panelX = Math.max(PANEL_X, windowWidth - PANEL_WIDTH - PANEL_X);
		int panelHeight = PANEL_PADDING * 2 + textRuns.length * ROW_HEIGHT;
		glColor3f(0.035f, 0.045f, 0.055f);
		glBegin(GL_QUADS);
		fillRect(panelX, PANEL_Y, PANEL_WIDTH, panelHeight);
		glEnd();

		glColor3f(0.20f, 0.70f, 0.55f);
		glLineWidth(1.0f);
		glBegin(GL_LINES);
		outlineRect(panelX, PANEL_Y, PANEL_WIDTH, panelHeight);
		glEnd();
		glLineWidth(1.0f);
		CharUtils.drawTextRuns(textRuns);
	}

	private void updateTextRuns(String[] lines, int windowWidth) {
		int panelX = Math.max(PANEL_X, windowWidth - PANEL_WIDTH - PANEL_X);
		for (int row = 0; row < textRuns.length; row++) {
			textRuns[row] = new TextRun(
				lines[row],
				panelX + PANEL_PADDING,
				PANEL_Y + PANEL_PADDING + row * ROW_HEIGHT,
				TEXT_COLOR[0],
				TEXT_COLOR[1],
				TEXT_COLOR[2],
				TextRun.POLICE_TXT
			);
		}
	}

	private static void fillRect(int x, int y, int width, int height) {
		glVertex2i(x, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y + height);
		glVertex2i(x, y + height);
	}

	private static void outlineRect(int x, int y, int width, int height) {
		glVertex2i(x, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y + height);
		glVertex2i(x + width, y + height);
		glVertex2i(x, y + height);
		glVertex2i(x, y + height);
		glVertex2i(x, y);
	}
}
