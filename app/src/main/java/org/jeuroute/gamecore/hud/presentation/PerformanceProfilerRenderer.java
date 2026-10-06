package org.jeuroute.gamecore.hud.presentation;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jeuroute.gamecore.hud.HudTable;
import org.jeuroute.gamecore.hud.HudTable.Alignment;
import org.jeuroute.gamecore.hud.HudTable.Column;
import org.jeuroute.gamecore.hud.HudTable.Row;
import org.jeuroute.gamecore.performance.PerformanceProfiler;
import org.jeuroute.model.records.hud.HudBounds;

public final class PerformanceProfilerRenderer {

	private static final int ROW_HEIGHT = 18;
	private static final int ROWS_PER_SCROLL = 3;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 18;
	private static final List<Column> COLUMNS = List.of(
		new Column("SECTION", 200, Alignment.LEFT),
		new Column("AVG MS", 132, Alignment.RIGHT),
		new Column("P95 MS", 132, Alignment.RIGHT),
		new Column("MAX MS", 132, Alignment.RIGHT)
	);
	private final HudTable table = new HudTable(COLUMNS);
	private final HudTableRenderer tableRenderer = new HudTableRenderer();
	private long renderedSnapshotVersion = Long.MIN_VALUE;
	private List<Row> rows = List.of();
	private int firstVisibleRow;
	private int renderedFirstVisibleRow = -1;
	private int renderedLastVisibleRow = -1;
	private long renderedRowsVersion = Long.MIN_VALUE;

	public void render(PerformanceProfiler profiler, HudBounds bounds) {
		profiler.refreshSnapshot(System.nanoTime());
		if (renderedSnapshotVersion != profiler.snapshotVersion()) {
			updateRows(profiler);
			renderedSnapshotVersion = profiler.snapshotVersion();
		}
		int visibleRowCount = visibleRowCount(bounds);
		int maxFirstVisibleRow = Math.max(0, rows.size() - visibleRowCount);
		firstVisibleRow = Math.min(firstVisibleRow, maxFirstVisibleRow);
		int lastVisibleRow = Math.min(rows.size(), firstVisibleRow + visibleRowCount);
		if (
			renderedFirstVisibleRow != firstVisibleRow ||
			renderedLastVisibleRow != lastVisibleRow ||
			renderedRowsVersion != renderedSnapshotVersion
		) {
			table.setRows(rows.subList(firstVisibleRow, lastVisibleRow));
			renderedFirstVisibleRow = firstVisibleRow;
			renderedLastVisibleRow = lastVisibleRow;
			renderedRowsVersion = renderedSnapshotVersion;
		}
		tableRenderer.render(table, bounds);
		renderScrollbar(bounds, visibleRowCount);
	}

	public boolean handleScroll(double scrollAmount, HudBounds bounds) {
		int visibleRowCount = visibleRowCount(bounds);
		int maxFirstVisibleRow = Math.max(
			0,
			PerformanceProfiler.Section.values().length - visibleRowCount
		);
		firstVisibleRow = Math.max(
			0,
			Math.min(
				maxFirstVisibleRow,
				firstVisibleRow - (int) Math.round(scrollAmount * ROWS_PER_SCROLL)
			)
		);
		return maxFirstVisibleRow > 0;
	}

	private void updateRows(PerformanceProfiler profiler) {
		List<Row> rows = new ArrayList<>(PerformanceProfiler.Section.values().length);
		for (PerformanceProfiler.Section section : PerformanceProfiler.Section.values()) {
			PerformanceProfiler.Statistics statistics = profiler.statistics(section);
			if (statistics.sampleCount() == 0) {
				rows.add(Row.of(section.displayLabel(), "--", "--", "--"));
				continue;
			}
			rows.add(
				Row.of(
					section.displayLabel(),
					formatMilliseconds(statistics.averageNanos()),
					formatMilliseconds(statistics.p95Nanos()),
					formatMilliseconds(statistics.maxNanos())
				)
			);
		}
		this.rows = List.copyOf(rows);
	}

	private static int visibleRowCount(HudBounds bounds) {
		return Math.max(0, bounds.height() / ROW_HEIGHT - 1);
	}

	private void renderScrollbar(HudBounds bounds, int visibleRowCount) {
		int maxFirstVisibleRow = rows.size() - visibleRowCount;
		int trackHeight = bounds.height() - ROW_HEIGHT;
		if (maxFirstVisibleRow <= 0 || trackHeight <= 0) {
			return;
		}

		int trackX = Math.max(
			bounds.x(),
			Math.min(bounds.x() + table.width() + 12, bounds.x() + bounds.width() - SCROLLBAR_WIDTH)
		);
		int trackY = bounds.y() + ROW_HEIGHT;
		int thumbHeight = Math.min(
			trackHeight,
			Math.max(
				SCROLLBAR_MIN_THUMB_HEIGHT,
				(int) Math.round((trackHeight * visibleRowCount) / (double) rows.size())
			)
		);
		int thumbY =
			trackY +
			(int) Math.round(
				((trackHeight - thumbHeight) * firstVisibleRow) / (double) maxFirstVisibleRow
			);

		glBegin(GL_QUADS);
		glColor3f(0.08f, 0.13f, 0.14f);
		fillRect(trackX, trackY, SCROLLBAR_WIDTH, trackHeight);
		glColor3f(0.20f, 0.70f, 0.55f);
		fillRect(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight);
		glEnd();
	}

	private static void fillRect(int x, int y, int width, int height) {
		glVertex2i(x, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y + height);
		glVertex2i(x, y + height);
	}

	private static String formatMilliseconds(double nanoseconds) {
		return String.format(Locale.ROOT, "%.1f", nanoseconds / 1_000_000.0);
	}
}
