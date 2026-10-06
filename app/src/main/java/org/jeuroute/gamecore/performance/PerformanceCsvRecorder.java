package org.jeuroute.gamecore.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PerformanceCsvRecorder implements AutoCloseable {

	public static final int SAMPLE_INTERVAL_TICKS = 5;

	private static final String[] METRIC_SUFFIXES = { "avg", "p95", "max" };
	private static final String ROUTE_DIAGNOSTICS_HEADER =
		"tick;timestamp_utc;raw_segments;visible_segments;merged_lines;routes_changed;segments_advanced;gpu_avg_ms;gpu_p95_ms;gpu_max_ms;queue_depth;requests_started;requests_completed;requests_deferred;work_units;max_wait_ticks;route_cache_hits;route_cache_misses;coalesced_searches\n";
	private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern(
		"yyyyMMdd-HHmmss-SSS"
	);

	private final PerformanceProfiler profiler;
	private final Path outputDirectory;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "performance-csv-writer");
		thread.setDaemon(true);
		return thread;
	});
	private volatile String lastErrorMessage;
	private OutputFiles outputFiles;
	private long lastRecordedTick = Long.MIN_VALUE;
	private boolean recording;
	private boolean closed;

	public PerformanceCsvRecorder(PerformanceProfiler profiler) {
		this(profiler, Path.of("build", "performance"));
	}

	PerformanceCsvRecorder(PerformanceProfiler profiler, Path outputDirectory) {
		this.profiler = Objects.requireNonNull(profiler);
		this.outputDirectory = Objects.requireNonNull(outputDirectory);
	}

	public synchronized boolean startRecording(long currentTick) {
		if (closed || recording) {
			return false;
		}
		try {
			Files.createDirectories(outputDirectory);
			outputFiles = createOutputFiles();
			lastRecordedTick = currentTick;
			recording = true;
			lastErrorMessage = null;
			return true;
		} catch (IOException exception) {
			lastErrorMessage = exception.getMessage();
			return false;
		}
	}

	public synchronized void stopRecording() {
		recording = false;
	}

	public synchronized boolean isRecording() {
		return recording;
	}

	public synchronized Optional<OutputFiles> getOutputFiles() {
		return Optional.ofNullable(outputFiles);
	}

	public synchronized String getDisplayPath() {
		if (lastErrorMessage != null) {
			return "CSV ERROR";
		}
		return outputFiles == null
			? outputDirectory.toString()
			: outputFiles
					.average()
					.getFileName()
					.toString()
					.replace("-avg.csv", "-{avg,p95,max,routes}.csv");
	}

	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	public synchronized void recordAtTick(long tickNumber) {
		if (
			!recording || tickNumber <= lastRecordedTick || tickNumber % SAMPLE_INTERVAL_TICKS != 0
		) {
			return;
		}

		long nowNanos = System.nanoTime();
		profiler.refreshSnapshot(nowNanos, 0L);
		String timestamp = Instant.now().toString();
		String[] csvBatches = createCsvBatches(tickNumber, timestamp);
		OutputFiles targets = outputFiles;
		lastRecordedTick = tickNumber;
		try {
			writer.execute(() -> appendBatch(targets, csvBatches));
		} catch (RuntimeException exception) {
			lastErrorMessage = exception.getMessage();
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			recording = false;
			closed = true;
		}
		writer.shutdown();
		try {
			if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
				writer.shutdownNow();
			}
		} catch (InterruptedException exception) {
			writer.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private OutputFiles createOutputFiles() throws IOException {
		String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
		for (int sequence = 0; sequence < 100; sequence++) {
			String suffix = sequence == 0 ? "" : "-" + sequence;
			String prefix = "profiler-" + timestamp + suffix;
			OutputFiles files = new OutputFiles(
				outputDirectory.resolve(prefix + "-avg.csv"),
				outputDirectory.resolve(prefix + "-p95.csv"),
				outputDirectory.resolve(prefix + "-max.csv"),
				outputDirectory.resolve(prefix + "-routes.csv")
			);
			List<Path> createdFiles = new ArrayList<>(METRIC_SUFFIXES.length);
			try {
				List<Path> allFiles = files.all();
				for (int index = 0; index < allFiles.size(); index++) {
					Path file = allFiles.get(index);
					Files.writeString(
						file,
						index < METRIC_SUFFIXES.length
							? createCsvHeader()
							: ROUTE_DIAGNOSTICS_HEADER,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE_NEW,
						StandardOpenOption.WRITE
					);
					createdFiles.add(file);
				}
				return files;
			} catch (FileAlreadyExistsException exception) {
				deleteFiles(createdFiles);
				continue;
			} catch (IOException exception) {
				deleteFiles(createdFiles);
				throw exception;
			}
		}
		throw new IOException("Unable to create unique profiler CSV files");
	}

	private static String createCsvHeader() {
		StringBuilder csv = new StringBuilder(PerformanceProfiler.Section.values().length * 16);
		csv.append("tick;timestamp_utc");
		for (PerformanceProfiler.Section section : PerformanceProfiler.Section.values()) {
			csv.append(';').append(section.displayLabel());
		}
		return csv.append('\n').toString();
	}

	private String[] createCsvBatches(long tickNumber, String timestamp) {
		String[] batches = new String[METRIC_SUFFIXES.length + 1];
		StringBuilder[] rows = new StringBuilder[METRIC_SUFFIXES.length];
		for (int index = 0; index < rows.length; index++) {
			rows[index] = new StringBuilder(PerformanceProfiler.Section.values().length * 16)
				.append(tickNumber)
				.append(';')
				.append(timestamp);
		}

		for (PerformanceProfiler.Section section : PerformanceProfiler.Section.values()) {
			PerformanceProfiler.Statistics statistics = profiler.statistics(section);
			long[] values = {
				Math.round(statistics.averageNanos()),
				statistics.p95Nanos(),
				statistics.maxNanos(),
			};
			for (int index = 0; index < rows.length; index++) {
				rows[index].append(';');
				appendMilliseconds(rows[index], statistics.sampleCount() > 0 ? values[index] : 0L);
			}
		}
		for (int index = 0; index < rows.length; index++) {
			batches[index] = rows[index].append('\n').toString();
		}
		batches[METRIC_SUFFIXES.length] = createRouteDiagnosticsBatch(tickNumber, timestamp);
		return batches;
	}

	private String createRouteDiagnosticsBatch(long tickNumber, String timestamp) {
		PerformanceProfiler.RouteGeometryStatistics routes = profiler.routeGeometryStatistics();
		PerformanceProfiler.Statistics gpu = profiler.statistics(
			PerformanceProfiler.Section.DEBUG_GPU
		);
		PerformanceProfiler.PathfindingQueueStatistics queue =
			profiler.pathfindingQueueStatistics();
		PerformanceProfiler.PathfindingCacheStatistics cache =
			profiler.pathfindingCacheStatistics();
		StringBuilder csv = new StringBuilder(128)
			.append(tickNumber)
			.append(';')
			.append(timestamp)
			.append(';')
			.append(routes.rawSegments())
			.append(';')
			.append(routes.visibleSegments())
			.append(';')
			.append(routes.mergedLines())
			.append(';')
			.append(routes.changedRoutes())
			.append(';')
			.append(routes.segmentTransitions())
			.append(';');
		appendMilliseconds(csv, gpu.sampleCount() == 0 ? 0L : Math.round(gpu.averageNanos()));
		csv.append(';');
		appendMilliseconds(csv, gpu.sampleCount() == 0 ? 0L : gpu.p95Nanos());
		csv.append(';');
		appendMilliseconds(csv, gpu.sampleCount() == 0 ? 0L : gpu.maxNanos());
		csv.append(';')
			.append(queue.queueDepth())
			.append(';')
			.append(queue.requestsStarted())
			.append(';')
			.append(queue.requestsCompleted())
			.append(';')
			.append(queue.requestsDeferred())
			.append(';')
			.append(queue.workUnits())
			.append(';')
			.append(queue.maxWaitTicks())
			.append(';')
			.append(cache.routeHits())
			.append(';')
			.append(cache.routeMisses())
			.append(';')
			.append(cache.coalescedSearches());
		return csv.append('\n').toString();
	}

	private static void appendMilliseconds(StringBuilder output, long nanoseconds) {
		long thousandthsOfMillisecond = Math.round(nanoseconds / 1_000.0);
		output.append(thousandthsOfMillisecond / 1_000).append(',');
		long fractional = thousandthsOfMillisecond % 1_000;
		if (fractional < 100) {
			output.append('0');
		}
		if (fractional < 10) {
			output.append('0');
		}
		output.append(fractional);
	}

	private static void deleteFiles(List<Path> files) throws IOException {
		IOException deletionFailure = null;
		for (Path file : files) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException exception) {
				if (deletionFailure == null) {
					deletionFailure = exception;
				} else {
					deletionFailure.addSuppressed(exception);
				}
			}
		}
		if (deletionFailure != null) {
			throw deletionFailure;
		}
	}

	private void appendBatch(OutputFiles targets, String[] csvBatches) {
		List<Path> files = targets.all();
		for (int index = 0; index < files.size(); index++) {
			appendBatch(files.get(index), csvBatches[index]);
		}
	}

	private void appendBatch(Path target, String csvBatch) {
		try {
			Files.writeString(
				target,
				csvBatch,
				StandardCharsets.UTF_8,
				StandardOpenOption.APPEND,
				StandardOpenOption.WRITE
			);
		} catch (IOException exception) {
			lastErrorMessage = exception.getMessage();
		}
	}

	public record OutputFiles(Path average, Path p95, Path maximum, Path routeDiagnostics) {
		public List<Path> all() {
			return List.of(average, p95, maximum, routeDiagnostics);
		}
	}
}
