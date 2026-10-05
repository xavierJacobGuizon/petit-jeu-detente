package org.jeuroute.gamecore;

import java.util.Arrays;

public final class PerformanceProfiler {

	private static final int SAMPLE_CAPACITY = 512;
	private static final long WINDOW_NANOS = 5_000_000_000L;
	private static final long SNAPSHOT_INTERVAL_NANOS = 1_000_000_000L;

	public enum Section {
		FRAME("FRAME"),
		UPDATE("UPDATE"),
		SIMULATION("SIM"),
		PERSON_UPDATE("PEOPLE"),
		PERSON_MOVEMENT("MOVE"),
		PERSON_IDLE_WAKE("IDLE WAKE"),
		ROUTE_PLANNING("ROUTE"),
		PLANNING_START("PLAN START"),
		PATHFINDING_SLICE("A* SLICE"),
		WORLD_RENDER("WORLD"),
		PERSON_RENDER("PERSON DRAW"),
		DEBUG_PREPARE("DBG PREP"),
		DEBUG_RENDER("DBG DRAW"),
		DEBUG_GPU("DBG GPU"),
		HUD_RENDER("HUD");

		private final String label;

		Section(String label) {
			this.label = label;
		}

		public String displayLabel() {
			return label;
		}
	}

	public record Statistics(int sampleCount, double averageNanos, long p95Nanos, long maxNanos) {}

	public record RouteGeometryStatistics(
		int rawSegments,
		int visibleSegments,
		int mergedLines,
		int changedRoutes,
		int segmentTransitions
	) {}

	public record PathfindingQueueStatistics(
		int queueDepth,
		int requestsStarted,
		int requestsCompleted,
		int requestsDeferred,
		int workUnits,
		long maxWaitTicks
	) {}

	public record PathfindingCacheStatistics(
		long routeHits,
		long routeMisses,
		long coalescedSearches
	) {}

	private final long[][] durations = new long[Section.values().length][SAMPLE_CAPACITY];
	private final long[][] timestamps = new long[Section.values().length][SAMPLE_CAPACITY];
	private final int[] cursors = new int[Section.values().length];
	private final int[] counts = new int[Section.values().length];
	private final Statistics[] statistics = new Statistics[Section.values().length];
	private final long[] sortBuffer = new long[SAMPLE_CAPACITY];
	private volatile RouteGeometryStatistics routeGeometryStatistics = new RouteGeometryStatistics(
		0,
		0,
		0,
		0,
		0
	);
	private volatile PathfindingQueueStatistics pathfindingQueueStatistics =
		new PathfindingQueueStatistics(0, 0, 0, 0, 0, 0L);
	private volatile PathfindingCacheStatistics pathfindingCacheStatistics =
		new PathfindingCacheStatistics(0L, 0L, 0L);
	private long lastSnapshotNanos = Long.MIN_VALUE;
	private long snapshotVersion;

	public PerformanceProfiler() {
		for (Section section : Section.values()) {
			statistics[section.ordinal()] = new Statistics(0, 0.0, 0L, 0L);
		}
	}

	public void record(Section section, long durationNanos) {
		recordAt(section, System.nanoTime(), durationNanos);
	}

	public boolean refreshSnapshot(long nowNanos) {
		return refreshSnapshot(nowNanos, SNAPSHOT_INTERVAL_NANOS);
	}

	public boolean refreshSnapshot(long nowNanos, long minimumIntervalNanos) {
		if (minimumIntervalNanos < 0) {
			throw new IllegalArgumentException("Snapshot interval cannot be negative");
		}
		if (
			lastSnapshotNanos != Long.MIN_VALUE &&
			nowNanos - lastSnapshotNanos < minimumIntervalNanos
		) {
			return false;
		}

		for (Section section : Section.values()) {
			int sectionIndex = section.ordinal();
			pruneExpiredSamples(sectionIndex, nowNanos);
			statistics[sectionIndex] = calculateStatistics(sectionIndex);
		}
		lastSnapshotNanos = nowNanos;
		snapshotVersion++;
		return true;
	}

	public Statistics statistics(Section section) {
		return statistics[section.ordinal()];
	}

	public long snapshotVersion() {
		return snapshotVersion;
	}

	public void recordRouteGeometryStatistics(RouteGeometryStatistics statistics) {
		routeGeometryStatistics = java.util.Objects.requireNonNull(statistics);
	}

	public RouteGeometryStatistics routeGeometryStatistics() {
		return routeGeometryStatistics;
	}

	public void recordPathfindingQueueStatistics(PathfindingQueueStatistics statistics) {
		pathfindingQueueStatistics = java.util.Objects.requireNonNull(statistics);
	}

	public PathfindingQueueStatistics pathfindingQueueStatistics() {
		return pathfindingQueueStatistics;
	}

	public void recordPathfindingCacheStatistics(PathfindingCacheStatistics statistics) {
		pathfindingCacheStatistics = java.util.Objects.requireNonNull(statistics);
	}

	public PathfindingCacheStatistics pathfindingCacheStatistics() {
		return pathfindingCacheStatistics;
	}

	void recordAt(Section section, long timestampNanos, long durationNanos) {
		int sectionIndex = section.ordinal();
		int cursor = cursors[sectionIndex];
		durations[sectionIndex][cursor] = Math.max(0L, durationNanos);
		timestamps[sectionIndex][cursor] = timestampNanos;
		cursors[sectionIndex] = (cursor + 1) % SAMPLE_CAPACITY;
		counts[sectionIndex] = Math.min(SAMPLE_CAPACITY, counts[sectionIndex] + 1);
	}

	private void pruneExpiredSamples(int sectionIndex, long nowNanos) {
		int count = counts[sectionIndex];
		int oldest = Math.floorMod(cursors[sectionIndex] - count, SAMPLE_CAPACITY);
		while (count > 0 && nowNanos - timestamps[sectionIndex][oldest] >= WINDOW_NANOS) {
			oldest = (oldest + 1) % SAMPLE_CAPACITY;
			count--;
		}
		counts[sectionIndex] = count;
	}

	private Statistics calculateStatistics(int sectionIndex) {
		int count = counts[sectionIndex];
		if (count == 0) {
			return new Statistics(0, 0.0, 0L, 0L);
		}

		int oldest = Math.floorMod(cursors[sectionIndex] - count, SAMPLE_CAPACITY);
		double totalNanos = 0.0;
		long maximumNanos = 0L;
		for (int index = 0; index < count; index++) {
			long duration = durations[sectionIndex][(oldest + index) % SAMPLE_CAPACITY];
			sortBuffer[index] = duration;
			totalNanos += duration;
			maximumNanos = Math.max(maximumNanos, duration);
		}
		Arrays.sort(sortBuffer, 0, count);
		int percentileIndex = (int) Math.ceil(count * 0.95) - 1;
		return new Statistics(count, totalNanos / count, sortBuffer[percentileIndex], maximumNanos);
	}
}
