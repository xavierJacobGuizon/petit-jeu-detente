package org.jeuroute.gamecore.rendering.debug;

import java.util.Collection;
import java.util.List;
import org.jeuroute.gamecore.performance.PerformanceProfiler;
import org.jeuroute.model.records.camera.WorldViewBounds;
import org.jeuroute.model.records.world.WorldRenderData;
import org.jeuroute.model.world.settlement.Person;
import org.lwjgl.opengl.GL;

public final class WorldDebugOverlay implements AutoCloseable {

	private static final long REFRESH_TICKS = 6;
	private static final long VIEW_REFRESH_TICKS = 6;

	private final PerformanceProfiler profiler;
	private final WorldDebugMeshRenderer meshRenderer;
	private final GpuTimerQuery gpuTimerQuery;
	private final PersonRouteGeometryCache routeGeometryCache = new PersonRouteGeometryCache();
	private long lastRefreshTick = -1;
	private WorldViewBounds lastViewBounds;
	private List<DebugRouteLineMerger.Line> preparedPersonRouteLines = List.of();
	private boolean dataReady;

	public WorldDebugOverlay(PerformanceProfiler profiler) {
		this.profiler = profiler;
		meshRenderer = GL.getCapabilities().OpenGL33 ? new WorldDebugMeshRenderer() : null;
		gpuTimerQuery = meshRenderer == null ? null : new GpuTimerQuery();
	}

	public boolean usesInstancedRenderer() {
		return meshRenderer != null;
	}

	public void render(
		boolean routeDisplayEnabled,
		WorldRenderData world,
		Collection<Person> walkingPeople,
		double zoom,
		WorldViewBounds viewBounds,
		long currentTick
	) {
		if (!routeDisplayEnabled) {
			if (dataReady) {
				invalidate();
			}
			return;
		}

		refreshIfNeeded(world, walkingPeople, viewBounds, zoom, currentTick);

		long renderStartedAtNanos = System.nanoTime();
		if (gpuTimerQuery != null) {
			gpuTimerQuery.begin(profiler);
		}
		try {
			if (meshRenderer == null) {
				WorldDebugRenderer.renderDestinations(
					world,
					walkingPeople,
					zoom,
					viewBounds,
					preparedPersonRouteLines
				);
			} else {
				meshRenderer.renderDestinations(zoom);
			}
		} finally {
			if (gpuTimerQuery != null) {
				gpuTimerQuery.end();
			}
		}
		profiler.record(
			PerformanceProfiler.Section.DEBUG_RENDER,
			System.nanoTime() - renderStartedAtNanos
		);
	}

	@Override
	public void close() {
		if (meshRenderer != null) {
			meshRenderer.close();
		}
		if (gpuTimerQuery != null) {
			gpuTimerQuery.close();
		}
	}

	private void refreshIfNeeded(
		WorldRenderData world,
		Collection<Person> walkingPeople,
		WorldViewBounds viewBounds,
		double zoom,
		long currentTick
	) {
		boolean refreshByTick = !dataReady || currentTick - lastRefreshTick >= REFRESH_TICKS;
		boolean refreshByView =
			!viewBounds.equals(lastViewBounds) &&
			currentTick - lastRefreshTick >= VIEW_REFRESH_TICKS;
		if (!refreshByTick && !refreshByView) {
			return;
		}

		long prepareStartedAtNanos = System.nanoTime();
		preparedPersonRouteLines = routeGeometryCache.collectVisibleLines(
			walkingPeople,
			viewBounds
		);
		PersonRouteGeometryCache.Diagnostics diagnostics = routeGeometryCache.diagnostics();
		profiler.recordRouteGeometryStatistics(
			new PerformanceProfiler.RouteGeometryStatistics(
				diagnostics.rawSegments(),
				diagnostics.visibleSegments(),
				diagnostics.mergedLines(),
				diagnostics.changedRoutes(),
				diagnostics.segmentTransitions()
			)
		);
		if (meshRenderer != null) {
			meshRenderer.updateDestinations(
				world,
				walkingPeople,
				viewBounds,
				zoom,
				preparedPersonRouteLines
			);
		}
		profiler.record(
			PerformanceProfiler.Section.DEBUG_PREPARE,
			System.nanoTime() - prepareStartedAtNanos
		);
		lastRefreshTick = currentTick;
		lastViewBounds = viewBounds;
		dataReady = true;
	}

	private void invalidate() {
		dataReady = false;
		lastRefreshTick = -1;
		lastViewBounds = null;
		preparedPersonRouteLines = List.of();
		routeGeometryCache.clear();
		profiler.recordRouteGeometryStatistics(
			new PerformanceProfiler.RouteGeometryStatistics(0, 0, 0, 0, 0)
		);
	}
}
