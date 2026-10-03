package org.jeuroute.gamecore;

import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.records.world.WorldRenderData;
import org.lwjgl.opengl.GL;

public final class WorldDebugOverlay implements AutoCloseable {

	private static final long REFRESH_TICKS = 60;
	private static final long VIEW_REFRESH_TICKS = 30;

	private final PerformanceProfiler profiler;
	private final WorldDebugMeshRenderer meshRenderer;
	private long lastRefreshTick = -1;
	private WorldViewBounds lastViewBounds;
	private boolean dataReady;

	public WorldDebugOverlay(PerformanceProfiler profiler) {
		this.profiler = profiler;
		meshRenderer = GL.getCapabilities().OpenGL33 ? new WorldDebugMeshRenderer() : null;
	}

	public boolean usesInstancedRenderer() {
		return meshRenderer != null;
	}

	public void render(
		boolean enabled,
		WorldRenderData world,
		double zoom,
		WorldViewBounds viewBounds,
		long currentTick
	) {
		if (!enabled) {
			invalidate();
			return;
		}

		if (meshRenderer != null) {
			refreshIfNeeded(world, viewBounds, zoom, currentTick);
		}

		long renderStartedAtNanos = System.nanoTime();
		if (meshRenderer == null) {
			WorldDebugRenderer.renderDestinations(world, zoom, viewBounds);
		} else {
			meshRenderer.renderDestinations(zoom);
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
	}

	private void refreshIfNeeded(
		WorldRenderData world,
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
		meshRenderer.updateDestinations(world, viewBounds, zoom);
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
	}
}
