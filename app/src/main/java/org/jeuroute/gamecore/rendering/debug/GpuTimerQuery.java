package org.jeuroute.gamecore.rendering.debug;

import org.jeuroute.gamecore.performance.PerformanceProfiler;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

final class GpuTimerQuery implements AutoCloseable {

	private static final int QUERY_COUNT = 3;

	private final int[] queryIds = new int[QUERY_COUNT];
	private final boolean[] pending = new boolean[QUERY_COUNT];
	private int activeIndex = -1;
	private int nextIndex;

	GpuTimerQuery() {
		for (int index = 0; index < queryIds.length; index++) {
			queryIds[index] = GL15.glGenQueries();
		}
	}

	void begin(PerformanceProfiler profiler) {
		pollCompleted(profiler);
		if (activeIndex >= 0) {
			return;
		}
		for (int offset = 0; offset < QUERY_COUNT; offset++) {
			int index = (nextIndex + offset) % QUERY_COUNT;
			if (pending[index]) {
				continue;
			}
			GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queryIds[index]);
			activeIndex = index;
			return;
		}
	}

	void end() {
		if (activeIndex < 0) {
			return;
		}
		GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
		pending[activeIndex] = true;
		nextIndex = (activeIndex + 1) % QUERY_COUNT;
		activeIndex = -1;
	}

	@Override
	public void close() {
		if (activeIndex >= 0) {
			GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
			activeIndex = -1;
		}
		for (int queryId : queryIds) {
			if (queryId != 0) {
				GL15.glDeleteQueries(queryId);
			}
		}
	}

	private void pollCompleted(PerformanceProfiler profiler) {
		for (int index = 0; index < QUERY_COUNT; index++) {
			if (!pending[index]) {
				continue;
			}
			if (GL15.glGetQueryObjecti(queryIds[index], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
				continue;
			}
			long elapsedNanos = Integer.toUnsignedLong(
				GL15.glGetQueryObjecti(queryIds[index], GL15.GL_QUERY_RESULT)
			);
			profiler.record(PerformanceProfiler.Section.DEBUG_GPU, elapsedNanos);
			pending[index] = false;
		}
	}
}
