package org.jeuroute.model.records.window;

public record WindowMetrics(int width, int height, int framebufferWidth, int framebufferHeight) {
	public boolean hasArea() {
		return width > 0 && height > 0 && framebufferWidth > 0 && framebufferHeight > 0;
	}
}
