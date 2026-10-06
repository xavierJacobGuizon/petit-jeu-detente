package org.jeuroute.gamecore.rendering.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DebugRouteLineMerger {

	private static final double DIRECTION_SCALE = 100_000_000.0;
	private static final double OFFSET_SCALE = 1_000.0;
	private static final double MERGE_EPSILON = 1.0e-6;

	private DebugRouteLineMerger() {}

	static List<Line> merge(List<Line> lines) {
		Index index = new Index();
		for (Line line : lines) {
			index.add(line);
		}
		return index.mergedLines();
	}

	static final class Index {

		private final Map<LineKey, LineGroup> groups = new LinkedHashMap<>();
		private List<Line> cachedLines = List.of();
		private boolean dirty;

		IndexedLine add(Line line) {
			double deltaX = line.endX() - line.startX();
			double deltaY = line.endY() - line.startY();
			double length = Math.hypot(deltaX, deltaY);
			if (length <= MERGE_EPSILON) {
				return null;
			}

			double directionX = deltaX / length;
			double directionY = deltaY / length;
			double offset = -directionY * line.startX() + directionX * line.startY();
			LineKey key = new LineKey(
				Math.round(directionX * DIRECTION_SCALE),
				Math.round(directionY * DIRECTION_SCALE),
				Math.round(offset * OFFSET_SCALE)
			);
			LineGroup group = groups.computeIfAbsent(key, ignored ->
				new LineGroup(directionX, directionY, offset)
			);
			double start = directionX * line.startX() + directionY * line.startY();
			double end = directionX * line.endX() + directionY * line.endY();
			Interval interval = new Interval(Math.min(start, end), Math.max(start, end));
			group.add(interval);
			dirty = true;
			return new IndexedLine(key, interval);
		}

		void remove(IndexedLine line) {
			if (line == null) {
				return;
			}
			LineGroup group = groups.get(line.key());
			if (group == null) {
				return;
			}
			group.remove(line.interval());
			if (group.isEmpty()) {
				groups.remove(line.key());
			}
			dirty = true;
		}

		List<Line> mergedLines() {
			if (!dirty) {
				return cachedLines;
			}
			List<Line> merged = new ArrayList<>();
			for (LineGroup group : groups.values()) {
				merged.addAll(group.mergedLines());
			}
			cachedLines = List.copyOf(merged);
			dirty = false;
			return cachedLines;
		}

		void clear() {
			groups.clear();
			cachedLines = List.of();
			dirty = false;
		}
	}

	static final class IndexedLine {

		private final LineKey key;
		private final Interval interval;

		private IndexedLine(LineKey key, Interval interval) {
			this.key = key;
			this.interval = interval;
		}

		private LineKey key() {
			return key;
		}

		private Interval interval() {
			return interval;
		}
	}

	record Line(double startX, double startY, double endX, double endY) {}

	private record LineKey(long directionX, long directionY, long offset) {}

	private record Interval(double start, double end) {}

	private static final class LineGroup {

		private final double directionX;
		private final double directionY;
		private final double offset;
		private final Map<Interval, Integer> intervals = new LinkedHashMap<>();
		private List<Line> cachedLines = List.of();
		private boolean dirty;

		private LineGroup(double directionX, double directionY, double offset) {
			this.directionX = directionX;
			this.directionY = directionY;
			this.offset = offset;
		}

		private void add(Interval interval) {
			intervals.put(interval, intervals.getOrDefault(interval, 0) + 1);
			dirty = true;
		}

		private void remove(Interval interval) {
			Integer count = intervals.get(interval);
			if (count == null) {
				return;
			}
			if (count == 1) {
				intervals.remove(interval);
			} else {
				intervals.put(interval, count - 1);
			}
			dirty = true;
		}

		private boolean isEmpty() {
			return intervals.isEmpty();
		}

		private List<Line> mergedLines() {
			if (!dirty) {
				return cachedLines;
			}
			List<Interval> sortedIntervals = new ArrayList<>(intervals.keySet());
			sortedIntervals.sort((left, right) -> Double.compare(left.start(), right.start()));
			List<Line> merged = new ArrayList<>();
			if (sortedIntervals.isEmpty()) {
				cachedLines = List.of();
				dirty = false;
				return cachedLines;
			}
			double start = sortedIntervals.getFirst().start();
			double end = sortedIntervals.getFirst().end();
			for (int index = 1; index < sortedIntervals.size(); index++) {
				Interval interval = sortedIntervals.get(index);
				if (interval.start() <= end + MERGE_EPSILON) {
					end = Math.max(end, interval.end());
					continue;
				}
				merged.add(line(start, end));
				start = interval.start();
				end = interval.end();
			}
			merged.add(line(start, end));
			cachedLines = List.copyOf(merged);
			dirty = false;
			return cachedLines;
		}

		private Line line(double start, double end) {
			return new Line(
				directionX * start - directionY * offset,
				directionY * start + directionX * offset,
				directionX * end - directionY * offset,
				directionY * end + directionX * offset
			);
		}
	}
}
