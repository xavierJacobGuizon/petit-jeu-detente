package org.jeuroute.gamecore.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jeuroute.gamecore.hud.presentation.HudWindowRenderer;
import org.jeuroute.model.records.hud.HudBounds;

public final class HudWindowManager {

	private final HudWindowRenderer hudWindowRenderer = new HudWindowRenderer();

	private final List<HudWindow> windows = new ArrayList<>();
	private final List<HudWindow> windowsView = Collections.unmodifiableList(windows);
	private long nextWindowId;
	private HudWindow activeWindow;

	private PointerInteraction pointerInteraction = PointerInteraction.NONE;
	private boolean pointerCaptured;
	private int dragOffsetX;
	private int dragOffsetY;
	private int resizeStartX;
	private int resizeStartY;
	private int resizeStartWidth;
	private int resizeStartHeight;

	public HudWindowRenderer getHudWindowRenderer() {
		return this.hudWindowRenderer;
	}

	public HudWindow open(HudWindowSpec spec) {
		HudWindow window = new HudWindow(nextWindowId++, spec);
		windows.add(window);
		return window;
	}

	public boolean close(long windowId) {
		for (int index = 0; index < windows.size(); index++) {
			if (windows.get(index).id() == windowId) {
				closeAt(index);
				return true;
			}
		}
		return false;
	}

	public void closeAll() {
		while (!windows.isEmpty()) {
			closeAt(windows.size() - 1);
		}
	}

	public List<HudWindow> windows() {
		return windowsView;
	}

	public boolean handleMousePressed(double mouseX, double mouseY) {
		pointerCaptured = false;
		pointerInteraction = PointerInteraction.NONE;
		activeWindow = null;
		for (int index = windows.size() - 1; index >= 0; index--) {
			HudWindow window = windows.get(index);
			if (window.closable() && closeButtonBounds(window).contains(mouseX, mouseY)) {
				pointerCaptured = true;
				closeAt(index);
				return true;
			}
			if (!window.bounds().contains(mouseX, mouseY)) {
				continue;
			}

			bringToFront(index);
			activeWindow = window;
			pointerCaptured = true;
			boolean onRightBorder =
				window.resizable() &&
				mouseX >= window.x() + window.width() - HudWindowMetrics.RESIZE_BORDER_SIZE;
			boolean onBottomBorder =
				window.resizable() &&
				mouseY >= window.y() + window.height() - HudWindowMetrics.RESIZE_BORDER_SIZE;
			if (onRightBorder || onBottomBorder) {
				pointerInteraction =
					onRightBorder && onBottomBorder
						? PointerInteraction.RESIZE_BOTH
						: onRightBorder
							? PointerInteraction.RESIZE_WIDTH
							: PointerInteraction.RESIZE_HEIGHT;
				resizeStartX = (int) Math.round(mouseX);
				resizeStartY = (int) Math.round(mouseY);
				resizeStartWidth = window.width();
				resizeStartHeight = window.height();
			} else if (mouseY < window.y() + HudWindowMetrics.TITLE_BAR_HEIGHT) {
				pointerInteraction = PointerInteraction.MOVE;
				dragOffsetX = (int) Math.round(mouseX) - window.x();
				dragOffsetY = (int) Math.round(mouseY) - window.y();
			} else if (window.contentBounds().contains(mouseX, mouseY)) {
				window.handleContentClick(mouseX, mouseY);
			}
			return true;
		}
		return false;
	}

	public void handleMouseMoved(
		double mouseX,
		double mouseY,
		int viewportWidth,
		int viewportHeight
	) {
		if (activeWindow == null) {
			return;
		}
		switch (pointerInteraction) {
			case MOVE -> moveWindow(mouseX, mouseY, viewportWidth, viewportHeight);
			case RESIZE_WIDTH -> resizeWindow(mouseX, mouseY, true, false);
			case RESIZE_HEIGHT -> resizeWindow(mouseX, mouseY, false, true);
			case RESIZE_BOTH -> resizeWindow(mouseX, mouseY, true, true);
			case NONE -> {
			}
		}
	}

	public boolean handleMouseReleased() {
		boolean wasCaptured = pointerCaptured;
		pointerCaptured = false;
		activeWindow = null;
		pointerInteraction = PointerInteraction.NONE;
		return wasCaptured;
	}

	private void bringToFront(int index) {
		if (index == windows.size() - 1) {
			return;
		}
		HudWindow window = windows.remove(index);
		windows.add(window);
	}

	private void closeAt(int index) {
		HudWindow window = windows.remove(index);
		if (activeWindow == window) {
			activeWindow = null;
			pointerInteraction = PointerInteraction.NONE;
		}
		window.close();
	}

	private void moveWindow(double mouseX, double mouseY, int viewportWidth, int viewportHeight) {
		int minimumX = Math.min(0, viewportWidth - HudWindowMetrics.MIN_VISIBLE_TITLE_WIDTH);
		int maximumX = Math.max(0, viewportWidth - HudWindowMetrics.MIN_VISIBLE_TITLE_WIDTH);
		int maximumY = Math.max(0, viewportHeight - HudWindowMetrics.TITLE_BAR_HEIGHT);
		int nextX = (int) Math.round(mouseX) - dragOffsetX;
		int nextY = (int) Math.round(mouseY) - dragOffsetY;
		activeWindow.moveTo(
			Math.max(minimumX, Math.min(nextX, maximumX)),
			Math.max(0, Math.min(nextY, maximumY))
		);
	}

	private void resizeWindow(double mouseX, double mouseY, boolean width, boolean height) {
		int nextWidth = width
			? resizeStartWidth + (int) Math.round(mouseX) - resizeStartX
			: activeWindow.width();
		int nextHeight = height
			? resizeStartHeight + (int) Math.round(mouseY) - resizeStartY
			: activeWindow.height();
		activeWindow.resizeTo(nextWidth, nextHeight);
	}

	private static HudBounds closeButtonBounds(HudWindow window) {
		return new HudBounds(
			window.x() + window.width() - HudWindowMetrics.CLOSE_BUTTON_SIZE - 4,
			window.y() + 4,
			HudWindowMetrics.CLOSE_BUTTON_SIZE,
			HudWindowMetrics.TITLE_BAR_HEIGHT - 8
		);
	}

	private enum PointerInteraction {
		NONE,
		MOVE,
		RESIZE_WIDTH,
		RESIZE_HEIGHT,
		RESIZE_BOTH,
	}
}
