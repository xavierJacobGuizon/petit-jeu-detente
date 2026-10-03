package org.jeuroute.gamecore;

import java.util.Arrays;
import java.util.Locale;

public final class PerformanceProfiler {

	private static final int SAMPLE_CAPACITY = 512;
	private static final long WINDOW_NANOS = 5_000_000_000L;
	private static final long SNAPSHOT_INTERVAL_NANOS = 1_000_000_000L;
	private static final String HEADER = formatHeader();

	public enum Section {
		FRAME("FRAME"),
		UPDATE("UPDATE"),
		SIMULATION("SIM"),
		PERSON_UPDATE("PEOPLE"),
		ROUTE_PLANNING("ROUTE"),
		WORLD_RENDER("WORLD"),
		PERSON_RENDER("PERSON DRAW"),
		DEBUG_PREPARE("DEBUG PREP"),
		DEBUG_RENDER("DEBUG DRAW"),
		HUD_RENDER("HUD");

		private final String label;

		Section(String label) {
			this.label = label;
		}
	}

	public record Statistics(int sampleCount, double averageNanos, long p95Nanos, long maxNanos) {}

	private final long[][] durations = new long[Section.values().length][SAMPLE_CAPACITY];
	private final long[][] timestamps = new long[Section.values().length][SAMPLE_CAPACITY];
	private final int[] cursors = new int[Section.values().length];
	private final int[] counts = new int[Section.values().length];
	private final Statistics[] statistics = new Statistics[Section.values().length];
	private final String[] displayLines = new String[Section.values().length + 1];
	private final long[] sortBuffer = new long[SAMPLE_CAPACITY];
	private long lastSnapshotNanos = Long.MIN_VALUE;
	private long snapshotVersion;

	public PerformanceProfiler() {
		displayLines[0] = HEADER;
		for (Section section : Section.values()) {
			statistics[section.ordinal()] = new Statistics(0, 0.0, 0L, 0L);
			displayLines[section.ordinal() + 1] = section.label + "  --";
		}
	}

	public void record(Section section, long durationNanos) {
		recordAt(section, System.nanoTime(), durationNanos);
	}

	public boolean refreshSnapshot(long nowNanos) {
		if (
			lastSnapshotNanos != Long.MIN_VALUE &&
			nowNanos - lastSnapshotNanos < SNAPSHOT_INTERVAL_NANOS
		) {
			return false;
		}

		for (Section section : Section.values()) {
			int sectionIndex = section.ordinal();
			pruneExpiredSamples(sectionIndex, nowNanos);
			Statistics sectionStatistics = calculateStatistics(sectionIndex);
			statistics[sectionIndex] = sectionStatistics;
			displayLines[sectionIndex + 1] = formatLine(section, sectionStatistics);
		}
		lastSnapshotNanos = nowNanos;
		snapshotVersion++;
		return true;
	}

	public Statistics statistics(Section section) {
		return statistics[section.ordinal()];
	}

	public String[] displayLines() {
		return displayLines;
	}

	public long snapshotVersion() {
		return snapshotVersion;
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

	private static String formatLine(Section section, Statistics sectionStatistics) {
		if (sectionStatistics.sampleCount() == 0) {
			return String.format(Locale.ROOT, "%-12s %8s %8s %8s", section.label, "--", "--", "--");
		}
		return String.format(
			Locale.ROOT,
			"%-12s %8.1f %8.1f %8.1f",
			section.label,
			sectionStatistics.averageNanos() / 1_000_000.0,
			sectionStatistics.p95Nanos() / 1_000_000.0,
			sectionStatistics.maxNanos() / 1_000_000.0
		);
	}

	private static String formatHeader() {
		return String.format(
			Locale.ROOT,
			"%-12s %8s %8s %8s",
			"SECTION",
			"AVG MS",
			"P95 MS",
			"MAX MS"
		);
	}
}
