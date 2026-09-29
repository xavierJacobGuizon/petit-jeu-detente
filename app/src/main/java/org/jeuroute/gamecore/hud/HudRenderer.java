package org.jeuroute.gamecore.hud;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.jeuroute.utils.CharUtils;
import org.jeuroute.utils.CharUtils.TextRun;

public final class HudRenderer {

	private static final float BACKGROUND_R = 0.06f;
	private static final float BACKGROUND_G = 0.08f;
	private static final float BACKGROUND_B = 0.11f;
	private static final float ACCENT_R = 0.20f;
	private static final float ACCENT_G = 0.70f;
	private static final float ACCENT_B = 0.55f;

	private HudRenderer() {}

	/**
	 * Rend le HUD à l'écran.
	 * @param hud Le HUD à rendre
	 * @param windowWidth Largeur de la fenêtre
	 * @param windowHeight Hauteur de la fenêtre
	 */
	public static void render(Hud hud, int windowWidth, int windowHeight) {
		List<DrawButton> buttons = collectButtons(hud, windowWidth, windowHeight);
		List<HudLayout.SlotPlacement> slots = HudLayout.layoutIndicatorSlots(
			hud.getIndicatorSlots(),
			windowWidth,
			windowHeight
		);
		renderIndicatorSlots(slots);
		renderButtons(buttons);
		renderText(slots, buttons);
		if (hud.getDialog() != null) {
			renderDialog(hud.getDialog(), windowWidth, windowHeight);
		}
	}

	/**
	 * Collecte les boutons à rendre à l'écran.
	 * @param hud Le HUD à rendre
	 * @param windowWidth Largeur de la fenêtre
	 * @param windowHeight Hauteur de la fenêtre
	 * @return La liste des boutons à rendre
	 */
	private static List<DrawButton> collectButtons(Hud hud, int windowWidth, int windowHeight) {
		List<DrawButton> buttons = new ArrayList<>();
		List<HudButton> rootButtons = hud.getButtons();

		// Parcourt les boutons racine et collecte leurs informations pour le rendu.
		for (int index = 0; index < rootButtons.size(); index++) {
			HudButton button = rootButtons.get(index);
			HudLayout.Bounds bounds = HudLayout.rootButtonBounds(index, windowWidth, windowHeight);
			buttons.add(
				new DrawButton(
					button.getLabel(),
					bounds,
					hud.getOpenMenu() == button,
					button.getSwatchColor()
				)
			);

			if (hud.getOpenMenu() == button) {
				List<HudButton> submenu = button.getSubmenu();
				for (int submenuIndex = 0; submenuIndex < submenu.size(); submenuIndex++) {
					HudButton submenuButton = submenu.get(submenuIndex);
					HudLayout.Bounds submenuBounds = HudLayout.submenuButtonBounds(
						index,
						submenuIndex,
						windowWidth,
						windowHeight
					);
					buttons.add(
						new DrawButton(
							submenuButton.getLabel(),
							submenuBounds,
							false,
							submenuButton.getSwatchColor()
						)
					);
				}
			}
		}

		HudButton actionButton = hud.getOneShotActionButton();
		if (actionButton != null) {
			HudLayout.Bounds bounds = HudLayout.rootButtonBounds(
				rootButtons.size(),
				windowWidth,
				windowHeight
			);
			buttons.add(
				new DrawButton(
					actionButton.getLabel(),
					bounds,
					false,
					actionButton.getSwatchColor()
				)
			);
		}
		return buttons;
	}

	private static void renderIndicatorSlots(List<HudLayout.SlotPlacement> slots) {
		glColor3f(BACKGROUND_R, BACKGROUND_G, BACKGROUND_B);
		glBegin(GL_QUADS);
		for (HudLayout.SlotPlacement placement : slots) {
			HudIndicatorSlot slot = placement.slot();
			int slotX = placement.bounds().x();
			int slotY = placement.bounds().y();
			for (int index = 0; index < slot.getIndicators().size(); index++) {
				fillRect(
					slotX + index * HudLayout.SLOT_ITEM_WIDTH,
					slotY,
					HudLayout.SLOT_ITEM_WIDTH,
					HudLayout.SLOT_HEIGHT
				);
			}
		}
		glEnd();

		glLineWidth(2.0f);
		glBegin(GL_LINES);
		for (HudLayout.SlotPlacement placement : slots) {
			HudIndicatorSlot slot = placement.slot();
			int slotX = placement.bounds().x();
			int slotY = placement.bounds().y();
			for (int index = 0; index < slot.getIndicators().size(); index++) {
				HudIndicator indicator = slot.getIndicators().get(index);
				int x = slotX + index * HudLayout.SLOT_ITEM_WIDTH;
				glColor3f(indicator.getRed(), indicator.getGreen(), indicator.getBlue());
				outlineRect(x, slotY, HudLayout.SLOT_ITEM_WIDTH, HudLayout.SLOT_HEIGHT);
			}
		}
		glEnd();
		glLineWidth(1.0f);
	}

	private static void renderButtons(List<DrawButton> buttons) {
		glColor3f(BACKGROUND_R, BACKGROUND_G, BACKGROUND_B);
		glBegin(GL_QUADS);
		for (DrawButton button : buttons) {
			HudLayout.Bounds bounds = button.bounds();
			fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
		}
		glEnd();
		glBegin(GL_QUADS);
		for (DrawButton button : buttons) {
			if (button.swatchColor() != null) {
				setColor(button.swatchColor());
				fillRect(button.bounds().x() + 8, button.bounds().y() + 9, 20, 20);
			}
		}
		glEnd();

		glLineWidth(2.0f);
		glBegin(GL_LINES);
		for (DrawButton button : buttons) {
			HudLayout.Bounds bounds = button.bounds();
			glColor3f(
				button.active() ? ACCENT_R : 0.32f,
				button.active() ? ACCENT_G : 0.40f,
				button.active() ? ACCENT_B : 0.42f
			);
			outlineRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
		}
		glEnd();
		glLineWidth(1.0f);
	}

	private static void renderDialog(HudDialog dialog, int windowWidth, int windowHeight) {
		HudLayout.DialogPlacement placement = HudLayout.dialogPlacement(
			dialog.getOptions().size(),
			dialog.getPage(),
			windowWidth,
			windowHeight
		);
		List<HudButton> visibleOptions = dialog.getVisibleOptions(windowHeight);
		HudLayout.Bounds panel = placement.panel();

		glColor3f(BACKGROUND_R, BACKGROUND_G, BACKGROUND_B);
		glBegin(GL_QUADS);
		fillRect(panel.x(), panel.y(), panel.width(), panel.height());
		for (HudLayout.Bounds option : placement.options()) {
			fillRect(option.x(), option.y(), option.width(), option.height());
		}
		if (placement.previous() != null) {
			fillRect(
				placement.previous().x(),
				placement.previous().y(),
				placement.previous().width(),
				placement.previous().height()
			);
			fillRect(
				placement.next().x(),
				placement.next().y(),
				placement.next().width(),
				placement.next().height()
			);
		}
		glEnd();

		glBegin(GL_QUADS);
		for (int index = 0; index < placement.options().size(); index++) {
			Color swatchColor = visibleOptions.get(index).getSwatchColor();
			if (swatchColor != null) {
				HudLayout.Bounds option = placement.options().get(index);
				setColor(swatchColor);
				fillRect(option.x() + 8, option.y() + 8, 20, 20);
			}
		}
		glEnd();

		glColor3f(ACCENT_R, ACCENT_G, ACCENT_B);
		glLineWidth(2.0f);
		glBegin(GL_LINES);
		outlineRect(panel.x(), panel.y(), panel.width(), panel.height());
		for (HudLayout.Bounds option : placement.options()) {
			outlineRect(option.x(), option.y(), option.width(), option.height());
		}
		if (placement.previous() != null) {
			outlineRect(
				placement.previous().x(),
				placement.previous().y(),
				placement.previous().width(),
				placement.previous().height()
			);
			outlineRect(
				placement.next().x(),
				placement.next().y(),
				placement.next().width(),
				placement.next().height()
			);
		}
		glEnd();
		glLineWidth(1.0f);

		List<TextRun> textRuns = new ArrayList<>();
		textRuns.add(
			new TextRun(
				dialog.getTitle(),
				panel.x() + HudLayout.DIALOG_PADDING,
				panel.y() + HudLayout.DIALOG_PADDING + 8,
				1.0f,
				1.0f,
				1.0f,
				TextRun.POLICE_TXT
			)
		);
		for (int index = 0; index < placement.options().size(); index++) {
			HudLayout.Bounds option = placement.options().get(index);
			textRuns.add(
				new TextRun(
					visibleOptions.get(index).getLabel(),
					option.x() + (visibleOptions.get(index).getSwatchColor() == null ? 8 : 36),
					option.y() + 8,
					0.85f,
					0.88f,
					0.86f,
					TextRun.POLICE_TXT
				)
			);
		}
		if (placement.previous() != null) {
			textRuns.add(
				new TextRun(
					"PRECEDENT",
					placement.previous().x() + 8,
					placement.previous().y() + 8,
					0.85f,
					0.88f,
					0.86f,
					TextRun.POLICE_TXT
				)
			);
			textRuns.add(
				new TextRun(
					"SUIVANT",
					placement.next().x() + 8,
					placement.next().y() + 8,
					0.85f,
					0.88f,
					0.86f,
					TextRun.POLICE_TXT
				)
			);
		}
		CharUtils.drawTextRuns(textRuns.toArray(TextRun[]::new));
	}

	private static void renderText(List<HudLayout.SlotPlacement> slots, List<DrawButton> buttons) {
		List<TextRun> textRuns = new ArrayList<>();
		for (HudLayout.SlotPlacement placement : slots) {
			HudIndicatorSlot slot = placement.slot();
			int slotX = placement.bounds().x();
			int slotY = placement.bounds().y();
			for (int index = 0; index < slot.getIndicators().size(); index++) {
				HudIndicator indicator = slot.getIndicators().get(index);
				int x = slotX + index * HudLayout.SLOT_ITEM_WIDTH;
				textRuns.add(
					new TextRun(
						indicator.getLabel(),
						x + 12,
						slotY + 16,
						1.0f,
						1.0f,
						1.0f,
						TextRun.POLICE_TXT
					)
				);
				int valueX = Math.max(x + 48, x + 12 + indicator.getLabel().length() * 16 + 8);
				textRuns.add(
					new TextRun(
						indicator.getValue(),
						valueX,
						slotY + 18,
						indicator.getRed(),
						indicator.getGreen(),
						indicator.getBlue(),
						TextRun.POLICE_TXT
					)
				);
			}
		}

		for (DrawButton button : buttons) {
			HudLayout.Bounds bounds = button.bounds();
			textRuns.add(
				new TextRun(
					button.label(),
					bounds.x() + (button.swatchColor() == null ? 12 : 36),
					bounds.y() + 12,
					button.active() ? ACCENT_R : 0.75f,
					button.active() ? ACCENT_G : 0.82f,
					button.active() ? ACCENT_B : 0.78f,
					TextRun.POLICE_TXT
				)
			);
		}
		CharUtils.drawTextRuns(textRuns.toArray(TextRun[]::new));
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

	private static void setColor(Color color) {
		glColor3f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
	}

	private record DrawButton(
		String label,
		HudLayout.Bounds bounds,
		boolean active,
		Color swatchColor
	) {}
}
