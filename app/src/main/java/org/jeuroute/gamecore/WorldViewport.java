package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glViewport;

public final class WorldViewport {

	private WindowMetrics appliedMetrics;

	public boolean applyIfChanged(WindowMetrics metrics) {
		if (!metrics.hasArea() || metrics.equals(appliedMetrics)) {
			return false;
		}
		glViewport(0, 0, metrics.framebufferWidth(), metrics.framebufferHeight());
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, metrics.width(), metrics.height(), 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		appliedMetrics = metrics;
		return true;
	}

	public WindowMetrics appliedMetrics() {
		return appliedMetrics;
	}
}
