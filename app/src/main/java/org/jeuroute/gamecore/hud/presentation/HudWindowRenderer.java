package org.jeuroute.gamecore.hud.presentation;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import org.jeuroute.gamecore.hud.HudWindow;
import org.jeuroute.gamecore.hud.HudWindowService;
import org.jeuroute.gamecore.hud.constants.HudWindowMetrics;
import org.jeuroute.model.records.presentation.TextRun;
import org.jeuroute.gamecore.rendering.text.CharUtils;

public final class HudWindowRenderer {

	private final Map<HudWindow, WindowText> windowTexts = new IdentityHashMap<>();

	public void render(
		HudWindowService service,
		int windowWidth,
		int windowHeight,
		int framebufferWidth,
		int framebufferHeight
	) {
		windowTexts.keySet().removeIf(window -> !service.windows().contains(window));
		for (HudWindow window : service.windows()) {
			WindowText text = windowTexts.computeIfAbsent(window, ignored -> new WindowText());
			text.update(window);
			renderBackground(window);
			renderFrame(window);
			CharUtils.drawTextRuns(text.runs);
			if (
				!enableContentScissor(
					window,
					windowWidth,
					windowHeight,
					framebufferWidth,
					framebufferHeight
				)
			) {
				continue;
			}
			window.renderContent();
			glDisable(GL_SCISSOR_TEST);
		}
	}

	private static boolean enableContentScissor(
		HudWindow window,
		int windowWidth,
		int windowHeight,
		int framebufferWidth,
		int framebufferHeight
	) {
		if (
			windowWidth <= 0 || windowHeight <= 0 || framebufferWidth <= 0 || framebufferHeight <= 0
		) {
			return false;
		}
		int left = Math.max(0, window.contentBounds().x());
		int top = Math.max(0, window.contentBounds().y());
		int right = Math.min(
			windowWidth,
			window.contentBounds().x() + window.contentBounds().width()
		);
		int bottom = Math.min(
			windowHeight,
			window.contentBounds().y() + window.contentBounds().height()
		);
		if (right <= left || bottom <= top) {
			return false;
		}
		double scaleX = framebufferWidth / (double) windowWidth;
		double scaleY = framebufferHeight / (double) windowHeight;
		int scissorLeft = (int) Math.floor(left * scaleX);
		int scissorBottom = (int) Math.floor(framebufferHeight - bottom * scaleY);
		int scissorRight = (int) Math.ceil(right * scaleX);
		int scissorTop = (int) Math.ceil(framebufferHeight - top * scaleY);
		glEnable(GL_SCISSOR_TEST);
		glScissor(
			scissorLeft,
			scissorBottom,
			scissorRight - scissorLeft,
			scissorTop - scissorBottom
		);
		return true;
	}

	private static void renderBackground(HudWindow window) {
		glBegin(GL_QUADS);
		glColor3f(0.035f, 0.045f, 0.055f);
		fillRect(window.x(), window.y(), window.width(), window.height());
		glColor3f(0.09f, 0.13f, 0.14f);
		fillRect(window.x(), window.y(), window.width(), HudWindowMetrics.TITLE_BAR_HEIGHT);
		glEnd();
	}

	private static void renderFrame(HudWindow window) {
		glColor3f(0.20f, 0.70f, 0.55f);
		glLineWidth(1.0f);
		glBegin(GL_LINES);
		outlineRect(window.x(), window.y(), window.width(), window.height());
		glVertex2i(window.x(), window.y() + HudWindowMetrics.TITLE_BAR_HEIGHT);
		glVertex2i(window.x() + window.width(), window.y() + HudWindowMetrics.TITLE_BAR_HEIGHT);
		int closeX = window.x() + window.width() - HudWindowMetrics.CLOSE_BUTTON_SIZE - 4;
		int closeY = window.y() + 4;
		glVertex2i(closeX + 6, closeY + 5);
		glVertex2i(closeX + 16, closeY + 15);
		glVertex2i(closeX + 16, closeY + 5);
		glVertex2i(closeX + 6, closeY + 15);
		int resizeX = window.x() + window.width() - 14;
		int resizeY = window.y() + window.height() - 14;
		glVertex2i(resizeX + 3, resizeY + 12);
		glVertex2i(resizeX + 12, resizeY + 3);
		glVertex2i(resizeX + 8, resizeY + 12);
		glVertex2i(resizeX + 12, resizeY + 8);
		glEnd();
		glLineWidth(1.0f);
	}

	private static void fillRect(int x, int y, int width, int height) {
		glVertex2i(x, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y + height);
		glVertex2i(x, y + height);
	}

	private static void outlineRect(int x, int y, int width, int height) {
		glVertex2i(x, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y);
		glVertex2i(x + width, y + height);
		glVertex2i(x + width, y + height);
		glVertex2i(x, y + height);
		glVertex2i(x, y + height);
		glVertex2i(x, y);
	}

	private static final class WindowText {

		private final TextRun[] runs = new TextRun[2];
		private int x = Integer.MIN_VALUE;
		private int y = Integer.MIN_VALUE;
		private int width = Integer.MIN_VALUE;

		private void update(HudWindow window) {
			if (x == window.x() && y == window.y() && width == window.width()) {
				return;
			}
			x = window.x();
			y = window.y();
			width = window.width();
			runs[0] = new TextRun(
				window.title().toUpperCase(Locale.ROOT),
				x + HudWindowMetrics.CONTENT_PADDING,
				y + 6,
				0.9f,
				0.94f,
				0.91f,
				TextRun.POLICE_TXT
			);
			runs[1] = new TextRun(
				"X",
				x + window.width() - HudWindowMetrics.CLOSE_BUTTON_SIZE - 4,
				y + 7,
				0.9f,
				0.94f,
				0.91f,
				TextRun.POLICE_TXT
			);
		}
	}
}
