package org.jeuroute.gamecore.hud.presentation;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.awt.Point;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.gamecore.hud.HudWindow;
import org.jeuroute.model.records.hud.HudBounds;
import org.jeuroute.model.records.utils.TextRun;
import org.jeuroute.utils.CharUtils;

public final class DebugProfilerWindowContent implements HudWindow.ContentRenderer {

	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 26;
	private static final int DATA_TOP = 38;
	private static final int MOUSE_COLUMN_WIDTH = 190;
	private static final float[] TEXT_COLOR = { 0.86f, 0.92f, 0.89f };
	private final PerformanceProfiler profiler;
	private final PerformanceProfilerRenderer profilerRenderer = new PerformanceProfilerRenderer();
	private final Supplier<Point> mousePosition;
	private final BooleanSupplier debugEnabled;
	private final Runnable toggleDebug;
	private final TextRun[] textRuns = new TextRun[6];
	private int previousMouseX = Integer.MIN_VALUE;
	private int previousMouseY = Integer.MIN_VALUE;
	private boolean previousDebugEnabled;
	private int previousContentX = Integer.MIN_VALUE;
	private int previousContentY = Integer.MIN_VALUE;

	public DebugProfilerWindowContent(
		PerformanceProfiler profiler,
		Supplier<Point> mousePosition,
		BooleanSupplier debugEnabled,
		Runnable toggleDebug
	) {
		this.profiler = Objects.requireNonNull(profiler);
		this.mousePosition = Objects.requireNonNull(mousePosition);
		this.debugEnabled = Objects.requireNonNull(debugEnabled);
		this.toggleDebug = Objects.requireNonNull(toggleDebug);
	}

	@Override
	public void render(HudBounds contentBounds) {
		Point position = mousePosition.get();
		int mouseX = position == null ? 0 : position.x;
		int mouseY = position == null ? 0 : position.y;
		boolean debugIsEnabled = debugEnabled.getAsBoolean();
		updateTextRuns(contentBounds, mouseX, mouseY, debugIsEnabled);
		renderDebugButton(contentBounds, debugIsEnabled);
		CharUtils.drawTextRuns(textRuns);
		HudBounds profilerBounds = new HudBounds(
			contentBounds.x() + MOUSE_COLUMN_WIDTH,
			contentBounds.y() + DATA_TOP,
			Math.max(0, contentBounds.width() - MOUSE_COLUMN_WIDTH),
			Math.max(0, contentBounds.height() - DATA_TOP)
		);
		profilerRenderer.render(profiler, profilerBounds);
	}

	@Override
	public boolean handleClick(HudBounds contentBounds, double mouseX, double mouseY) {
		HudBounds buttonBounds = debugButtonBounds(contentBounds);
		if (!buttonBounds.contains(mouseX, mouseY)) {
			return false;
		}
		toggleDebug.run();
		return true;
	}

	private void updateTextRuns(
		HudBounds contentBounds,
		int mouseX,
		int mouseY,
		boolean debugIsEnabled
	) {
		if (
			previousContentX == contentBounds.x() &&
			previousContentY == contentBounds.y() &&
			previousMouseX == mouseX &&
			previousMouseY == mouseY &&
			previousDebugEnabled == debugIsEnabled
		) {
			return;
		}
		previousContentX = contentBounds.x();
		previousContentY = contentBounds.y();
		previousMouseX = mouseX;
		previousMouseY = mouseY;
		previousDebugEnabled = debugIsEnabled;
		textRuns[0] = new TextRun(
			debugIsEnabled ? "DEBUG ON" : "DEBUG OFF",
			contentBounds.x() + 10,
			contentBounds.y() + 5,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[1] = new TextRun(
			"MOUSE",
			contentBounds.x() + 4,
			contentBounds.y() + DATA_TOP + 4,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[2] = new TextRun(
			"X",
			contentBounds.x() + 4,
			contentBounds.y() + DATA_TOP + 28,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[3] = new TextRun(
			"Y",
			contentBounds.x() + 92,
			contentBounds.y() + DATA_TOP + 28,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[4] = new TextRun(
			Integer.toString(mouseX),
			contentBounds.x() + 4,
			contentBounds.y() + DATA_TOP + 52,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[5] = new TextRun(
			Integer.toString(mouseY),
			contentBounds.x() + 92,
			contentBounds.y() + DATA_TOP + 52,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
	}

	private static void renderDebugButton(HudBounds contentBounds, boolean debugIsEnabled) {
		HudBounds buttonBounds = debugButtonBounds(contentBounds);
		glColor3f(debugIsEnabled ? 0.10f : 0.07f, debugIsEnabled ? 0.30f : 0.11f, 0.14f);
		glBegin(GL_QUADS);
		fillRect(buttonBounds);
		glEnd();
		glColor3f(0.20f, 0.70f, 0.55f);
		glLineWidth(1.0f);
		glBegin(GL_LINES);
		outlineRect(buttonBounds);
		glEnd();
		glLineWidth(1.0f);
	}

	private static HudBounds debugButtonBounds(HudBounds contentBounds) {
		return new HudBounds(contentBounds.x(), contentBounds.y(), BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	private static void fillRect(HudBounds bounds) {
		glVertex2i(bounds.x(), bounds.y());
		glVertex2i(bounds.x() + bounds.width(), bounds.y());
		glVertex2i(bounds.x() + bounds.width(), bounds.y() + bounds.height());
		glVertex2i(bounds.x(), bounds.y() + bounds.height());
	}

	private static void outlineRect(HudBounds bounds) {
		glVertex2i(bounds.x(), bounds.y());
		glVertex2i(bounds.x() + bounds.width(), bounds.y());
		glVertex2i(bounds.x() + bounds.width(), bounds.y());
		glVertex2i(bounds.x() + bounds.width(), bounds.y() + bounds.height());
		glVertex2i(bounds.x() + bounds.width(), bounds.y() + bounds.height());
		glVertex2i(bounds.x(), bounds.y() + bounds.height());
		glVertex2i(bounds.x(), bounds.y() + bounds.height());
		glVertex2i(bounds.x(), bounds.y());
	}
}
