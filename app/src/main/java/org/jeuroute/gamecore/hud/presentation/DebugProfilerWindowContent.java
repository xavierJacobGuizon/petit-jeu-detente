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
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jeuroute.gamecore.PerformanceCsvRecorder;
import org.jeuroute.gamecore.PerformanceProfiler;
import org.jeuroute.gamecore.hud.HudWindow;
import org.jeuroute.model.records.hud.HudBounds;
import org.jeuroute.model.records.utils.TextRun;
import org.jeuroute.utils.CharUtils;

public final class DebugProfilerWindowContent implements HudWindow.ContentRenderer {

	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 26;
	private static final int BUTTON_GAP = 8;
	private static final int CSV_BUTTON_INDEX = 2;
	private static final int DATA_TOP = 38;
	private static final int MOUSE_COLUMN_WIDTH = 190;
	private static final float[] TEXT_COLOR = { 0.86f, 0.92f, 0.89f };
	private final PerformanceProfiler profiler;
	private final PerformanceProfilerRenderer profilerRenderer = new PerformanceProfilerRenderer();
	private final Supplier<Point> mousePosition;
	private final BooleanSupplier debugEnabled;
	private final Runnable toggleDebug;
	private final BooleanSupplier personRoutesEnabled;
	private final Runnable togglePersonRoutes;
	private final PerformanceCsvRecorder csvRecorder;
	private final LongSupplier currentTick;
	private final TextRun[] textRuns = new TextRun[9];
	private int previousMouseX = Integer.MIN_VALUE;
	private int previousMouseY = Integer.MIN_VALUE;
	private boolean previousDebugEnabled;
	private boolean previousCsvRecording;
	private String previousCsvDisplayPath;
	private int previousContentX = Integer.MIN_VALUE;
	private int previousContentY = Integer.MIN_VALUE;

	public DebugProfilerWindowContent(
		PerformanceProfiler profiler,
		Supplier<Point> mousePosition,
		BooleanSupplier debugEnabled,
		Runnable toggleDebug
	) {
		this(profiler, mousePosition, debugEnabled, toggleDebug, () -> false, () -> {});
	}

	public DebugProfilerWindowContent(
		PerformanceProfiler profiler,
		Supplier<Point> mousePosition,
		BooleanSupplier debugEnabled,
		Runnable toggleDebug,
		BooleanSupplier personRoutesEnabled,
		Runnable togglePersonRoutes
	) {
		this(
			profiler,
			mousePosition,
			debugEnabled,
			toggleDebug,
			personRoutesEnabled,
			togglePersonRoutes,
			null,
			() -> 0L
		);
	}

	public DebugProfilerWindowContent(
		PerformanceProfiler profiler,
		Supplier<Point> mousePosition,
		BooleanSupplier debugEnabled,
		Runnable toggleDebug,
		BooleanSupplier personRoutesEnabled,
		Runnable togglePersonRoutes,
		PerformanceCsvRecorder csvRecorder,
		LongSupplier currentTick
	) {
		this.profiler = Objects.requireNonNull(profiler);
		this.mousePosition = Objects.requireNonNull(mousePosition);
		this.debugEnabled = Objects.requireNonNull(debugEnabled);
		this.toggleDebug = Objects.requireNonNull(toggleDebug);
		this.personRoutesEnabled = Objects.requireNonNull(personRoutesEnabled);
		this.togglePersonRoutes = Objects.requireNonNull(togglePersonRoutes);
		this.csvRecorder = csvRecorder;
		this.currentTick = Objects.requireNonNull(currentTick);
	}

	@Override
	public void render(HudBounds contentBounds) {
		Point position = mousePosition.get();
		int mouseX = position == null ? 0 : position.x;
		int mouseY = position == null ? 0 : position.y;
		boolean debugIsEnabled = debugEnabled.getAsBoolean();
		boolean csvIsRecording = csvRecorder != null && csvRecorder.isRecording();
		String csvDisplayPath = csvRecorder == null ? "" : csvRecorder.getDisplayPath();
		updateTextRuns(
			contentBounds,
			mouseX,
			mouseY,
			debugIsEnabled,
			csvIsRecording,
			csvDisplayPath
		);
		renderDebugButton(contentBounds, debugIsEnabled);
		renderPersonRoutesButton(contentBounds, personRoutesEnabled.getAsBoolean());
		if (csvRecorder != null) {
			renderCsvButton(contentBounds, csvIsRecording);
		}
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
		if (debugButtonBounds(contentBounds).contains(mouseX, mouseY)) {
			toggleDebug.run();
			return true;
		}
		if (personRoutesButtonBounds(contentBounds).contains(mouseX, mouseY)) {
			togglePersonRoutes.run();
			return true;
		}
		if (csvRecorder != null && csvButtonBounds(contentBounds).contains(mouseX, mouseY)) {
			if (csvRecorder.isRecording()) {
				csvRecorder.stopRecording();
			} else {
				csvRecorder.startRecording(currentTick.getAsLong());
			}
			return true;
		}
		return false;
	}

	private void updateTextRuns(
		HudBounds contentBounds,
		int mouseX,
		int mouseY,
		boolean debugIsEnabled,
		boolean csvIsRecording,
		String csvDisplayPath
	) {
		if (
			previousContentX == contentBounds.x() &&
			previousContentY == contentBounds.y() &&
			previousMouseX == mouseX &&
			previousMouseY == mouseY &&
			previousDebugEnabled == debugIsEnabled &&
			previousCsvRecording == csvIsRecording &&
			Objects.equals(previousCsvDisplayPath, csvDisplayPath)
		) {
			return;
		}
		previousContentX = contentBounds.x();
		previousContentY = contentBounds.y();
		previousMouseX = mouseX;
		previousMouseY = mouseY;
		previousDebugEnabled = debugIsEnabled;
		previousCsvRecording = csvIsRecording;
		previousCsvDisplayPath = csvDisplayPath;
		textRuns[0] = new TextRun(
			debugIsEnabled ? "DEBUG ON" : "DEBUG OFF",
			contentBounds.x() + 10,
			contentBounds.y() + 5,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[6] = new TextRun(
			"AFFICHER TRAJET",
			contentBounds.x() + BUTTON_WIDTH + BUTTON_GAP + 8,
			contentBounds.y() + 5,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[7] = new TextRun(
			csvDisplayPath,
			contentBounds.x() +
				CSV_BUTTON_INDEX * (BUTTON_WIDTH + BUTTON_GAP) +
				BUTTON_WIDTH +
				BUTTON_GAP,
			contentBounds.y() + 5,
			TEXT_COLOR[0],
			TEXT_COLOR[1],
			TEXT_COLOR[2],
			TextRun.POLICE_TXT
		);
		textRuns[8] = new TextRun(
			csvRecorder == null ? "" : csvIsRecording ? "ARRETER CSV" : "ENREGISTRER CSV",
			contentBounds.x() + CSV_BUTTON_INDEX * (BUTTON_WIDTH + BUTTON_GAP) + 8,
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

	private static void renderPersonRoutesButton(
		HudBounds contentBounds,
		boolean personRoutesAreEnabled
	) {
		HudBounds buttonBounds = personRoutesButtonBounds(contentBounds);
		glColor3f(
			personRoutesAreEnabled ? 0.10f : 0.07f,
			personRoutesAreEnabled ? 0.30f : 0.11f,
			0.14f
		);
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

	private static HudBounds personRoutesButtonBounds(HudBounds contentBounds) {
		return new HudBounds(
			contentBounds.x() + BUTTON_WIDTH + BUTTON_GAP,
			contentBounds.y(),
			BUTTON_WIDTH,
			BUTTON_HEIGHT
		);
	}

	private static void renderCsvButton(HudBounds contentBounds, boolean recording) {
		HudBounds buttonBounds = csvButtonBounds(contentBounds);
		glColor3f(recording ? 0.10f : 0.07f, recording ? 0.30f : 0.11f, 0.14f);
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

	private static HudBounds csvButtonBounds(HudBounds contentBounds) {
		return new HudBounds(
			contentBounds.x() + CSV_BUTTON_INDEX * (BUTTON_WIDTH + BUTTON_GAP),
			contentBounds.y(),
			BUTTON_WIDTH,
			BUTTON_HEIGHT
		);
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
