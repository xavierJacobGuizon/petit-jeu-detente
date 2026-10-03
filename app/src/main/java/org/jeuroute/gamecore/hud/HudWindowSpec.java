package org.jeuroute.gamecore.hud;

import java.util.Objects;

public record HudWindowSpec(
	String title,
	int x,
	int y,
	int width,
	int height,
	int minimumWidth,
	int minimumHeight,
	boolean resizable,
	boolean closable,
	HudWindow.ContentRenderer contentRenderer,
	Runnable onClose
) {
	public HudWindowSpec {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("Window title cannot be blank");
		}
		if (minimumWidth < 1 || minimumHeight < 1) {
			throw new IllegalArgumentException("Window minimum dimensions must be positive");
		}
		if (width < minimumWidth || height < minimumHeight) {
			throw new IllegalArgumentException(
				"Initial window dimensions are below their minimums"
			);
		}
		Objects.requireNonNull(contentRenderer);
		Objects.requireNonNull(onClose);
	}

	public static HudWindowSpec standard(
		String title,
		int x,
		int y,
		int width,
		int height,
		HudWindow.ContentRenderer contentRenderer
	) {
		return new HudWindowSpec(
			title,
			x,
			y,
			width,
			height,
			HudWindowMetrics.MIN_WINDOW_WIDTH,
			HudWindowMetrics.MIN_WINDOW_HEIGHT,
			true,
			true,
			contentRenderer,
			() -> {}
		);
	}
}
