package org.jeuroute.gamecore.hud.presentation;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.util.ArrayList;
import java.util.List;
import org.jeuroute.gamecore.hud.HudTable;
import org.jeuroute.model.records.hud.HudBounds;
import org.jeuroute.model.records.utils.TextRun;
import org.jeuroute.utils.CharUtils;

public final class HudTableRenderer {

	private static final int ROW_HEIGHT = 18;
	private static final int CELL_PADDING = 6;
	private static final float TEXT_RED = 0.86f;
	private static final float TEXT_GREEN = 0.92f;
	private static final float TEXT_BLUE = 0.89f;
	private long renderedVersion = Long.MIN_VALUE;
	private int renderedX = Integer.MIN_VALUE;
	private int renderedY = Integer.MIN_VALUE;
	private TextRun[] textRuns = new TextRun[0];
	private int[] columnOffsets = new int[0];

	public void render(HudTable table, HudBounds bounds) {
		if (bounds.width() <= 0 || bounds.height() <= 0) {
			return;
		}
		if (
			renderedVersion != table.version() || renderedX != bounds.x() || renderedY != bounds.y()
		) {
			rebuildTextRuns(table, bounds);
		}
		renderGrid(table, bounds);
		CharUtils.drawTextRuns(textRuns);
	}

	private void rebuildTextRuns(HudTable table, HudBounds bounds) {
		List<TextRun> runs = new ArrayList<>();
		columnOffsets = new int[table.columns().size()];
		int columnX = bounds.x();
		for (int columnIndex = 0; columnIndex < table.columns().size(); columnIndex++) {
			HudTable.Column column = table.columns().get(columnIndex);
			columnOffsets[columnIndex] = columnX - bounds.x();
			runs.add(
				cellText(column.header(), columnX, bounds.y(), column.width(), column.alignment())
			);
			columnX += column.width();
		}

		for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
			HudTable.Row row = table.rows().get(rowIndex);
			columnX = bounds.x();
			for (int columnIndex = 0; columnIndex < table.columns().size(); columnIndex++) {
				HudTable.Column column = table.columns().get(columnIndex);
				runs.add(
					cellText(
						row.cells().get(columnIndex),
						columnX,
						bounds.y() + (rowIndex + 1) * ROW_HEIGHT,
						column.width(),
						column.alignment()
					)
				);
				columnX += column.width();
			}
		}
		textRuns = runs.toArray(TextRun[]::new);
		renderedVersion = table.version();
		renderedX = bounds.x();
		renderedY = bounds.y();
	}

	private static TextRun cellText(
		String value,
		int x,
		int y,
		int width,
		HudTable.Alignment alignment
	) {
		int textX = alignedTextX(value, x, width, alignment);
		return new TextRun(value, textX, y, TEXT_RED, TEXT_GREEN, TEXT_BLUE, TextRun.POLICE_TXT);
	}

	static int alignedTextX(String value, int x, int width, HudTable.Alignment alignment) {
		int textWidth = value.length() * 16;
		return switch (alignment) {
			case LEFT -> x + CELL_PADDING;
			case CENTER -> x + Math.max(CELL_PADDING, (width - textWidth) / 2);
			case RIGHT -> x + Math.max(CELL_PADDING, width - CELL_PADDING - textWidth);
		};
	}

	private void renderGrid(HudTable table, HudBounds bounds) {
		glColor3f(0.20f, 0.34f, 0.34f);
		glLineWidth(1.0f);
		glBegin(GL_LINES);
		int tableBottom = bounds.y() + (table.rows().size() + 1) * ROW_HEIGHT;
		for (int columnOffset : columnOffsets) {
			int x = bounds.x() + columnOffset;
			glVertex2i(x, bounds.y());
			glVertex2i(x, tableBottom);
		}
		glVertex2i(bounds.x() + table.width(), bounds.y());
		glVertex2i(bounds.x() + table.width(), tableBottom);
		for (int rowIndex = 0; rowIndex <= table.rows().size() + 1; rowIndex++) {
			int y = bounds.y() + rowIndex * ROW_HEIGHT;
			glVertex2i(bounds.x(), y);
			glVertex2i(bounds.x() + table.width(), y);
		}
		glEnd();
		glLineWidth(1.0f);
	}
}
