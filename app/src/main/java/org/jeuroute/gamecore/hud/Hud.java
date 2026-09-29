package org.jeuroute.gamecore.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Hud {

	private final List<HudIndicatorSlot> indicatorSlots = new ArrayList<>();
	private final List<HudButton> buttons = new ArrayList<>();
	private final List<HudIndicatorSlot> indicatorSlotsView = Collections.unmodifiableList(
		indicatorSlots
	);
	private final List<HudButton> buttonsView = Collections.unmodifiableList(buttons);
	private HudButton openMenu;
	private int openMenuIndex = -1;
	private HudDialog dialog;
	private HudButton oneShotActionButton;
	private Runnable oneShotActionCancellation = () -> {};

	public Hud addIndicatorSlot(HudIndicatorSlot slot) {
		indicatorSlots.add(slot);
		return this;
	}

	public Hud addButton(HudButton button) {
		buttons.add(button);
		return this;
	}

	public List<HudIndicatorSlot> getIndicatorSlots() {
		return indicatorSlotsView;
	}

	public List<HudButton> getButtons() {
		return buttonsView;
	}

	public HudButton getOpenMenu() {
		return openMenu;
	}

	public HudButton getOneShotActionButton() {
		return oneShotActionButton;
	}

	public Hud showOneShotActionButton(HudButton button) {
		return showOneShotActionButton(button, () -> {});
	}

	public Hud showOneShotActionButton(HudButton button, Runnable cancellation) {
		oneShotActionButton = Objects.requireNonNull(button);
		oneShotActionCancellation = Objects.requireNonNull(cancellation);
		return this;
	}

	public Hud hideOneShotActionButton() {
		oneShotActionButton = null;
		oneShotActionCancellation = () -> {};
		return this;
	}

	public Hud cancelOneShotActionButton() {
		if (oneShotActionButton == null) {
			return this;
		}
		Runnable cancellation = oneShotActionCancellation;
		hideOneShotActionButton();
		cancellation.run();
		return this;
	}

	public int getOpenMenuIndex() {
		return openMenuIndex;
	}

	public HudDialog getDialog() {
		return dialog;
	}

	public void showDialog(String title, List<HudButton> options) {
		dialog = new HudDialog(title, options);
	}

	public void closeDialog() {
		dialog = null;
	}

	public boolean handleClick(double mouseX, double mouseY, int windowWidth, int windowHeight) {
		if (dialog != null) {
			HudDialog activeDialog = dialog;
			HudLayout.DialogPlacement placement = HudLayout.dialogPlacement(
				activeDialog.getOptions().size(),
				activeDialog.getPage(),
				windowWidth,
				windowHeight
			);
			if (placement.previous() != null && placement.previous().contains(mouseX, mouseY)) {
				activeDialog.previousPage();
				return true;
			}
			if (placement.next() != null && placement.next().contains(mouseX, mouseY)) {
				activeDialog.nextPage(windowHeight);
				return true;
			}
			for (int index = 0; index < placement.options().size(); index++) {
				if (placement.options().get(index).contains(mouseX, mouseY)) {
					closeDialog();
					activeDialog.getVisibleOptions(windowHeight).get(index).activate();
					return true;
				}
			}
			closeDialog();
			return true;
		}

		for (int index = 0; index < buttons.size(); index++) {
			HudButton button = buttons.get(index);
			if (
				!HudLayout.rootButtonBounds(index, windowWidth, windowHeight).contains(
					mouseX,
					mouseY
				)
			) {
				continue;
			}

			if (button.hasSubmenu()) {
				if (openMenu == button) {
					closeMenu();
				} else {
					openMenu = button;
					openMenuIndex = index;
				}
			} else {
				cancelOneShotActionButton();
				button.activate();
				closeMenu();
			}
			return true;
		}
		if (
			oneShotActionButton != null &&
			HudLayout.rootButtonBounds(buttons.size(), windowWidth, windowHeight).contains(
				mouseX,
				mouseY
			)
		) {
			HudButton actionButton = oneShotActionButton;
			hideOneShotActionButton();
			closeMenu();
			actionButton.activate();
			return true;
		}

		if (openMenu != null) {
			List<HudButton> submenu = openMenu.getSubmenu();
			for (int index = 0; index < submenu.size(); index++) {
				if (
					HudLayout.submenuButtonBounds(
						openMenuIndex,
						index,
						windowWidth,
						windowHeight
					).contains(mouseX, mouseY)
				) {
					cancelOneShotActionButton();
					submenu.get(index).activate();
					closeMenu();
					return true;
				}
			}
			closeMenu();
			return true;
		}

		return false;
	}

	public void handleOutsideClick() {
		if (dialog != null) {
			closeDialog();
		}
	}

	private void closeMenu() {
		openMenu = null;
		openMenuIndex = -1;
	}
}
