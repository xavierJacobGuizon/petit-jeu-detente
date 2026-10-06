package org.jeuroute.manager.hud;

import java.util.List;
import org.jeuroute.configuration.Registry;
import org.jeuroute.configuration.indicators.HudIndicatorConfigurationCache;
import org.jeuroute.configuration.menus.MenuDefinitionCache;
import org.jeuroute.model.records.configuration.enums.HudAnchor;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.elements.HudButton;
import org.jeuroute.gamecore.hud.elements.HudIndicator;
import org.jeuroute.gamecore.hud.elements.HudIndicatorSlot;
import org.jeuroute.model.records.configuration.HudIndicatorConfiguration;
import org.jeuroute.model.records.configuration.HudIndicatorSlotConfiguration;
import org.jeuroute.model.records.configuration.MenuDefinition;

public final class HudManager {

	private final Hud hud = new Hud();
	private final Registry<Runnable> actionHandlers;
	private final Registry<HudIndicator> indicators;

	private final String DEFAULT_MENU_PATH = "menus/default-menu.xml";
	private final String DEFAULT_INDICATOR_CONFIGURATION_PATH = "hud/default-indicators.xml";

	public HudManager(Registry<Runnable> actionHandlers, Registry<HudIndicator> indicators) {
		this.actionHandlers = actionHandlers;
		this.indicators = indicators;

		addConfiguredIndicators();
		addDefaultButtons();
	}

	public Hud getHud() {
		return hud;
	}

	public HudManager addIndicatorSlot(HudIndicatorSlot slot) {
		hud.addIndicatorSlot(slot);
		return this;
	}

	public HudManager registerIndicator(String indicatorId, HudIndicator indicator) {
		indicators.register(indicatorId, indicator);
		return this;
	}

	public HudIndicator getIndicator(String indicatorId) {
		return indicators.get(indicatorId);
	}

	public HudIndicatorSlot createIndicatorSlot(HudAnchor anchor) {
		HudIndicatorSlot slot = new HudIndicatorSlot(anchor);
		hud.addIndicatorSlot(slot);
		return slot;
	}

	public HudManager addButton(HudButton button) {
		hud.addButton(button);
		return this;
	}

	private void addConfiguredIndicators() {
		HudIndicatorConfiguration configuration = new HudIndicatorConfigurationCache().getOrLoad(
			DEFAULT_INDICATOR_CONFIGURATION_PATH
		);
		for (HudIndicatorSlotConfiguration slotConfiguration : configuration.slots()) {
			HudIndicatorSlot slot = new HudIndicatorSlot(slotConfiguration.anchor());
			for (String indicatorId : slotConfiguration.indicatorIds()) {
				slot.add(getIndicator(indicatorId));
			}
			addIndicatorSlot(slot);
		}
	}

	private void addDefaultButtons() {
		MenuDefinition menuDefinition = new MenuDefinitionCache().getOrLoad(this.DEFAULT_MENU_PATH);
		addButton(toHudButton(menuDefinition));
	}

	private HudButton toHudButton(MenuDefinition definition) {
		List<HudButton> children = definition.children().stream().map(this::toHudButton).toList();
		if (!definition.hasChildren()) {
			return new HudButton(definition.label(), actionFor(definition.action()));
		}
		return HudButton.submenu(definition.label(), children);
	}

	private Runnable actionFor(String actionName) {
		if (actionName == null || actionName.isBlank()) {
			return () -> {};
		}
		return () -> actionHandlers.get(actionName).run();
	}
}
