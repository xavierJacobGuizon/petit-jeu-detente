package org.jeuroute.gamecore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jeuroute.gamecore.camera.WorldViewBounds;

final class SpatialRouteLineIndex {

	private static final int TILE_SIZE = 512;

	private final Map<Tile, Bucket> buckets = new LinkedHashMap<>();
	private int mergeBuildCount;

	IndexedLine add(DebugRouteLineMerger.Line line) {
		Map<Tile, Entry> entries = new LinkedHashMap<>();
		for (Tile tile : tilesAlong(line)) {
			DebugRouteLineMerger.Line clipped = clip(line, tile);
			if (clipped == null) {
				continue;
			}
			Bucket bucket = buckets.computeIfAbsent(tile, ignored -> new Bucket());
			DebugRouteLineMerger.IndexedLine indexedLine = bucket.add(clipped);
			if (indexedLine != null) {
				entries.put(tile, new Entry(clipped, indexedLine));
			}
		}
		return entries.isEmpty() ? null : new IndexedLine(entries);
	}

	void remove(IndexedLine line) {
		if (line == null) {
			return;
		}
		for (Map.Entry<Tile, Entry> entry : line.entries().entrySet()) {
			Bucket bucket = buckets.get(entry.getKey());
			if (bucket == null) {
				continue;
			}
			bucket.remove(entry.getValue());
			if (bucket.isEmpty()) {
				buckets.remove(entry.getKey());
			}
		}
	}

	Query query(WorldViewBounds viewBounds, double padding) {
		Set<Tile> visibleTiles = visibleTiles(viewBounds, padding);
		Set<DebugRouteLineMerger.Line> lines = new LinkedHashSet<>();
		int visibleSegments = 0;
		for (Tile tile : visibleTiles) {
			Bucket bucket = buckets.get(tile);
			if (bucket == null) {
				continue;
			}
			visibleSegments += bucket.visibleSegmentCount(viewBounds, padding);
			for (DebugRouteLineMerger.Line line : mergedLines(bucket)) {
				if (
					viewBounds.intersectsSegment(
						line.startX(),
						line.startY(),
						line.endX(),
						line.endY(),
						padding
					)
				) {
					lines.add(line);
				}
			}
		}
		return new Query(List.copyOf(lines), visibleSegments);
	}

	void clear() {
		buckets.clear();
		mergeBuildCount = 0;
	}

	int mergeBuildCount() {
		return mergeBuildCount;
	}

	private List<DebugRouteLineMerger.Line> mergedLines(Bucket bucket) {
		if (bucket.dirty) {
			mergeBuildCount++;
			bucket.dirty = false;
		}
		return bucket.index.mergedLines();
	}

	private Set<Tile> visibleTiles(WorldViewBounds bounds, double padding) {
		if (
			!Double.isFinite(bounds.minX()) ||
			!Double.isFinite(bounds.minY()) ||
			!Double.isFinite(bounds.maxX()) ||
			!Double.isFinite(bounds.maxY())
		) {
			return new LinkedHashSet<>(buckets.keySet());
		}
		int minColumn = tileAt(bounds.minX() - padding);
		int maxColumn = tileAt(bounds.maxX() + padding);
		int minRow = tileAt(bounds.minY() - padding);
		int maxRow = tileAt(bounds.maxY() + padding);
		Set<Tile> tiles = new LinkedHashSet<>();
		for (int row = minRow; row <= maxRow; row++) {
			for (int column = minColumn; column <= maxColumn; column++) {
				Tile tile = new Tile(column, row);
				if (buckets.containsKey(tile)) {
					tiles.add(tile);
				}
			}
		}
		return tiles;
	}

	private static int tileAt(double coordinate) {
		return (int) Math.floor(coordinate / TILE_SIZE);
	}

	private static List<Tile> tilesAlong(DebugRouteLineMerger.Line line) {
		double deltaX = line.endX() - line.startX();
		double deltaY = line.endY() - line.startY();
		int column = tileAt(line.startX());
		int row = tileAt(line.startY());
		int endColumn = tileAt(line.endX());
		int endRow = tileAt(line.endY());
		int stepX = Double.compare(deltaX, 0.0);
		int stepY = Double.compare(deltaY, 0.0);
		double deltaTileX = stepX == 0 ? Double.POSITIVE_INFINITY : TILE_SIZE / Math.abs(deltaX);
		double deltaTileY = stepY == 0 ? Double.POSITIVE_INFINITY : TILE_SIZE / Math.abs(deltaY);
		double boundaryX = stepX > 0 ? (column + 1.0) * TILE_SIZE : column * (double) TILE_SIZE;
		double boundaryY = stepY > 0 ? (row + 1.0) * TILE_SIZE : row * (double) TILE_SIZE;
		double nextTileX =
			stepX == 0 ? Double.POSITIVE_INFINITY : (boundaryX - line.startX()) / deltaX;
		double nextTileY =
			stepY == 0 ? Double.POSITIVE_INFINITY : (boundaryY - line.startY()) / deltaY;
		List<Tile> tiles = new ArrayList<>();
		int maxSteps = Math.abs(endColumn - column) + Math.abs(endRow - row) + 3;
		for (int step = 0; step < maxSteps; step++) {
			tiles.add(new Tile(column, row));
			if (column == endColumn && row == endRow) {
				break;
			}
			if (nextTileX < nextTileY) {
				column += stepX;
				nextTileX += deltaTileX;
			} else if (nextTileY < nextTileX) {
				row += stepY;
				nextTileY += deltaTileY;
			} else {
				column += stepX;
				row += stepY;
				nextTileX += deltaTileX;
				nextTileY += deltaTileY;
			}
		}
		return tiles;
	}

	private static DebugRouteLineMerger.Line clip(DebugRouteLineMerger.Line line, Tile tile) {
		double minX = (double) tile.column() * TILE_SIZE;
		double minY = (double) tile.row() * TILE_SIZE;
		double maxX = minX + TILE_SIZE;
		double maxY = minY + TILE_SIZE;
		double deltaX = line.endX() - line.startX();
		double deltaY = line.endY() - line.startY();
		double[] interval = { 0.0, 1.0 };
		if (
			!clipTest(-deltaX, line.startX() - minX, interval) ||
			!clipTest(deltaX, maxX - line.startX(), interval) ||
			!clipTest(-deltaY, line.startY() - minY, interval) ||
			!clipTest(deltaY, maxY - line.startY(), interval)
		) {
			return null;
		}
		return new DebugRouteLineMerger.Line(
			line.startX() + interval[0] * deltaX,
			line.startY() + interval[0] * deltaY,
			line.startX() + interval[1] * deltaX,
			line.startY() + interval[1] * deltaY
		);
	}

	private static boolean clipTest(double direction, double distance, double[] interval) {
		if (direction == 0.0) {
			return distance >= 0.0;
		}
		double ratio = distance / direction;
		if (direction < 0.0) {
			if (ratio > interval[1]) {
				return false;
			}
			interval[0] = Math.max(interval[0], ratio);
		} else {
			if (ratio < interval[0]) {
				return false;
			}
			interval[1] = Math.min(interval[1], ratio);
		}
		return true;
	}

	final class IndexedLine {

		private final Map<Tile, Entry> entries;

		private IndexedLine(Map<Tile, Entry> entries) {
			this.entries = Map.copyOf(entries);
		}

		private Map<Tile, Entry> entries() {
			return entries;
		}
	}

	record Query(List<DebugRouteLineMerger.Line> lines, int visibleSegments) {}

	private record Tile(int column, int row) {}

	private record Entry(
		DebugRouteLineMerger.Line line,
		DebugRouteLineMerger.IndexedLine indexedLine
	) {}

	private static final class Bucket {

		private final DebugRouteLineMerger.Index index = new DebugRouteLineMerger.Index();
		private final Map<DebugRouteLineMerger.Line, Integer> sourceLines = new LinkedHashMap<>();
		private boolean dirty;

		private DebugRouteLineMerger.IndexedLine add(DebugRouteLineMerger.Line line) {
			DebugRouteLineMerger.IndexedLine indexedLine = index.add(line);
			if (indexedLine != null) {
				sourceLines.put(line, sourceLines.getOrDefault(line, 0) + 1);
				dirty = true;
			}
			return indexedLine;
		}

		private void remove(Entry entry) {
			index.remove(entry.indexedLine());
			dirty = true;
			sourceLines.computeIfPresent(entry.line(), (line, count) ->
				count == 1 ? null : count - 1
			);
		}

		private int visibleSegmentCount(WorldViewBounds viewBounds, double padding) {
			int count = 0;
			for (Map.Entry<DebugRouteLineMerger.Line, Integer> entry : sourceLines.entrySet()) {
				DebugRouteLineMerger.Line line = entry.getKey();
				if (
					viewBounds.intersectsSegment(
						line.startX(),
						line.startY(),
						line.endX(),
						line.endY(),
						padding
					)
				) {
					count += entry.getValue();
				}
			}
			return count;
		}

		private boolean isEmpty() {
			return sourceLines.isEmpty();
		}
	}
}
