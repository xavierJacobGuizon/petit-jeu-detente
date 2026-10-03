package org.jeuroute.gamecore.hud;

import java.util.List;
import java.util.Objects;

public final class HudTable {

	private final List<Column> columns;
	private final int width;
	private List<Row> rows = List.of();
	private long version;

	public HudTable(List<Column> columns) {
		this.columns = List.copyOf(columns);
		if (this.columns.isEmpty()) {
			throw new IllegalArgumentException("A table must have at least one column");
		}
		int totalWidth = 0;
		for (Column column : this.columns) {
			totalWidth += column.width();
		}
		width = totalWidth;
	}

	public List<Column> columns() {
		return columns;
	}

	public List<Row> rows() {
		return rows;
	}

	public long version() {
		return version;
	}

	public int width() {
		return width;
	}

	public void setRows(List<Row> rows) {
		List<Row> nextRows = List.copyOf(rows);
		for (Row row : nextRows) {
			if (row.cells().size() != columns.size()) {
				throw new IllegalArgumentException("Every table row must match the column count");
			}
		}
		this.rows = nextRows;
		version++;
	}

	public record Column(String header, int width, Alignment alignment) {
		public Column {
			if (header == null || header.isBlank()) {
				throw new IllegalArgumentException("Column header cannot be blank");
			}
			if (width < 1) {
				throw new IllegalArgumentException("Column width must be positive");
			}
			Objects.requireNonNull(alignment);
		}
	}

	public record Row(List<String> cells) {
		public Row {
			cells = List.copyOf(cells);
		}

		public static Row of(String... cells) {
			return new Row(List.of(cells));
		}
	}

	public enum Alignment {
		LEFT,
		CENTER,
		RIGHT,
	}
}
