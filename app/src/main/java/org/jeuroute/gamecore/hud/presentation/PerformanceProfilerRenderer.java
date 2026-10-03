package org.jeuroute.gamecore.hud.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.gamecore.hud.HudTable;
import org.jeuroute.gamecore.hud.HudTable.Alignment;
import org.jeuroute.gamecore.hud.HudTable.Column;
import org.jeuroute.gamecore.hud.HudTable.Row;
import org.jeuroute.model.records.hud.HudBounds;

public final class PerformanceProfilerRenderer {

	private static final List<Column> COLUMNS = List.of(
		new Column("SECTION", 200, Alignment.LEFT),
		new Column("AVG MS", 132, Alignment.RIGHT),
		new Column("P95 MS", 132, Alignment.RIGHT),
		new Column("MAX MS", 132, Alignment.RIGHT)
	);
	private final HudTable table = new HudTable(COLUMNS);
	private final HudTableRenderer tableRenderer = new HudTableRenderer();
	private long renderedSnapshotVersion = Long.MIN_VALUE;

	public void render(PerformanceProfiler profiler, HudBounds bounds) {
		profiler.refreshSnapshot(System.nanoTime());
		if (renderedSnapshotVersion != profiler.snapshotVersion()) {
			updateRows(profiler);
			renderedSnapshotVersion = profiler.snapshotVersion();
		}
		tableRenderer.render(table, bounds);
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
		table.setRows(rows);
	}

	private static String formatMilliseconds(double nanoseconds) {
		return String.format(Locale.ROOT, "%.1f", nanoseconds / 1_000_000.0);
	}
}
