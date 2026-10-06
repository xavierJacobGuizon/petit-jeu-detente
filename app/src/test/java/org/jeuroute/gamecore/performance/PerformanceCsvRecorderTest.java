package org.jeuroute.gamecore.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jeuroute.gamecore.hud.presentation.DebugProfilerWindowContent;
import org.jeuroute.model.records.hud.HudBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerformanceCsvRecorderTest {

	@Test
	void writesProfilerSectionSnapshotsEveryFiveTicks(@TempDir Path outputDirectory)
		throws IOException {
		PerformanceProfiler profiler = new PerformanceProfiler();
		for (int durationMillis = 1; durationMillis <= 20; durationMillis++) {
			profiler.record(PerformanceProfiler.Section.FRAME, durationMillis * 1_000_000L);
		}
		profiler.record(PerformanceProfiler.Section.DEBUG_GPU, 2_000_000L);
		profiler.recordRouteGeometryStatistics(
			new PerformanceProfiler.RouteGeometryStatistics(100, 35, 20, 7, 3)
		);
		profiler.recordPathfindingQueueStatistics(
			new PerformanceProfiler.PathfindingQueueStatistics(12, 4, 2, 10, 16_384, 24)
		);
		profiler.recordPathfindingCacheStatistics(
			new PerformanceProfiler.PathfindingCacheStatistics(3, 5, 2)
		);
		PerformanceCsvRecorder recorder = new PerformanceCsvRecorder(profiler, outputDirectory);
		assertTrue(recorder.startRecording(0));
		PerformanceCsvRecorder.OutputFiles outputFiles = recorder.getOutputFiles().orElseThrow();
		assertTrue(outputFiles.average().getFileName().toString().endsWith("-avg.csv"));
		assertTrue(outputFiles.p95().getFileName().toString().endsWith("-p95.csv"));
		assertTrue(outputFiles.maximum().getFileName().toString().endsWith("-max.csv"));
		assertTrue(outputFiles.routeDiagnostics().getFileName().toString().endsWith("-routes.csv"));
		for (Path outputFile : outputFiles.all()) {
			assertTrue(Files.exists(outputFile));
			assertEquals(1, Files.readAllLines(outputFile).size());
		}

		recorder.recordAtTick(4);
		assertEquals(1, Files.readAllLines(outputFiles.average()).size());

		recorder.recordAtTick(5);
		recorder.recordAtTick(5);
		recorder.stopRecording();
		recorder.recordAtTick(10);
		recorder.close();

		assertMetricValue(outputFiles.average(), "10,500");
		assertMetricValue(outputFiles.p95(), "19,000");
		assertMetricValue(outputFiles.maximum(), "20,000");
		var routeLines = Files.readAllLines(outputFiles.routeDiagnostics());
		assertEquals(2, routeLines.size());
		assertEquals(
			"tick;timestamp_utc;raw_segments;visible_segments;merged_lines;routes_changed;segments_advanced;gpu_avg_ms;gpu_p95_ms;gpu_max_ms;queue_depth;requests_started;requests_completed;requests_deferred;work_units;max_wait_ticks;route_cache_hits;route_cache_misses;coalesced_searches",
			routeLines.getFirst()
		);
		String[] routeValues = routeLines.get(1).split(";");
		assertEquals("5", routeValues[0]);
		assertEquals("100", routeValues[2]);
		assertEquals("35", routeValues[3]);
		assertEquals("20", routeValues[4]);
		assertEquals("7", routeValues[5]);
		assertEquals("3", routeValues[6]);
		assertEquals("2,000", routeValues[7]);
		assertEquals("2,000", routeValues[8]);
		assertEquals("2,000", routeValues[9]);
		assertEquals("12", routeValues[10]);
		assertEquals("4", routeValues[11]);
		assertEquals("2", routeValues[12]);
		assertEquals("10", routeValues[13]);
		assertEquals("16384", routeValues[14]);
		assertEquals("24", routeValues[15]);
		assertEquals("3", routeValues[16]);
		assertEquals("5", routeValues[17]);
		assertEquals("2", routeValues[18]);
		assertFalse(recorder.isRecording());
	}

	@Test
	void debugWindowButtonStartsAndStopsCsvRecording(@TempDir Path outputDirectory) {
		PerformanceCsvRecorder recorder = new PerformanceCsvRecorder(
			new PerformanceProfiler(),
			outputDirectory
		);
		DebugProfilerWindowContent content = new DebugProfilerWindowContent(
			new PerformanceProfiler(),
			() -> new Point(0, 0),
			() -> false,
			() -> {},
			() -> false,
			() -> {},
			recorder,
			() -> 12L
		);
		HudBounds contentBounds = new HudBounds(20, 30, 800, 200);

		assertTrue(content.handleClick(contentBounds, 345, 40));
		assertTrue(recorder.isRecording());
		assertTrue(
			recorder
				.getOutputFiles()
				.orElseThrow()
				.all()
				.stream()
				.allMatch(file -> file.startsWith(outputDirectory))
		);

		assertTrue(content.handleClick(contentBounds, 345, 40));
		assertFalse(recorder.isRecording());
		recorder.close();
	}

	private static void assertMetricValue(Path outputFile, String expectedFrameMilliseconds)
		throws IOException {
		var csvLines = Files.readAllLines(outputFile);
		assertEquals(2, csvLines.size());
		assertTrue(csvLines.getFirst().startsWith("tick;timestamp_utc;FRAME;UPDATE;"));
		assertTrue(csvLines.get(1).startsWith("5;"));
		assertEquals(
			PerformanceProfiler.Section.values().length + 2,
			csvLines.getFirst().split(";").length
		);
		String[] values = csvLines.get(1).split(";");
		assertEquals(expectedFrameMilliseconds, values[2]);
		assertEquals("0,000", values[3]);
	}
}
