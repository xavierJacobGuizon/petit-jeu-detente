package org.jeuroute.gamecore.performance;

public class PerformanceService {

	private final PerformanceProfiler performanceProfiler = new PerformanceProfiler();
	private final PerformanceCsvRecorder performanceCsvRecorder = new PerformanceCsvRecorder(
		this.performanceProfiler
	);

	public PerformanceCsvRecorder getPerformanceCsvRecorder() {
		return performanceCsvRecorder;
	}

	public PerformanceProfiler getPerformanceProfiler() {
		return performanceProfiler;
	}
}
