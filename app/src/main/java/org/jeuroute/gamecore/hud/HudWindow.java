package org.jeuroute.gamecore.hud;

import java.util.Objects;
import org.jeuroute.gamecore.hud.constants.HudWindowMetrics;
import org.jeuroute.model.records.hud.HudBounds;

public final class HudWindow {

	private final String title;
	private final ContentRenderer contentRenderer;
	private final int minimumWidth;
	private final int minimumHeight;
	private final boolean resizable;
	private final boolean closable;
	private final Runnable onClose;
	private final long id;
	private int x;
	private int y;
	private int width;
	private int height;

	HudWindow(long id, HudWindowSpec spec) {
		this.id = id;
		title = spec.title();
		x = spec.x();
		y = spec.y();
		width = spec.width();
		height = spec.height();
		minimumWidth = spec.minimumWidth();
		minimumHeight = spec.minimumHeight();
		resizable = spec.resizable();
		closable = spec.closable();
		contentRenderer = Objects.requireNonNull(spec.contentRenderer());
		onClose = Objects.requireNonNull(spec.onClose());
	}

	public long id() {
		return id;
	}

	public String title() {
		return title;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int minimumWidth() {
		return minimumWidth;
	}

	public int minimumHeight() {
		return minimumHeight;
	}

	public boolean resizable() {
		return resizable;
	}

	public boolean closable() {
		return closable;
	}

	public HudBounds bounds() {
		return new HudBounds(x, y, width, height);
	}

	public HudBounds contentBounds() {
		int padding = HudWindowMetrics.CONTENT_PADDING;
		return new HudBounds(
			x + padding,
			y + HudWindowMetrics.TITLE_BAR_HEIGHT + padding,
			width - padding * 2,
			height - HudWindowMetrics.TITLE_BAR_HEIGHT - padding * 2
		);
	}

	public void renderContent() {
		contentRenderer.render(contentBounds());
	}

	boolean handleContentClick(double mouseX, double mouseY) {
		return contentRenderer.handleClick(contentBounds(), mouseX, mouseY);
	}

	boolean handleContentScroll(double mouseX, double mouseY, double scrollAmount) {
		return contentRenderer.handleScroll(contentBounds(), mouseX, mouseY, scrollAmount);
	}

	void moveTo(int nextX, int nextY) {
		x = nextX;
		y = nextY;
	}

	void resizeTo(int nextWidth, int nextHeight) {
		width = Math.max(minimumWidth, nextWidth);
		height = Math.max(minimumHeight, nextHeight);
	}

	void close() {
		onClose.run();
	}

	@FunctionalInterface
	public interface ContentRenderer {
		void render(HudBounds contentBounds);

		default boolean handleClick(HudBounds contentBounds, double mouseX, double mouseY) {
			return false;
		}

		default boolean handleScroll(
			HudBounds contentBounds,
			double mouseX,
			double mouseY,
			double scrollAmount
		) {
			return false;
		}
	}
}
