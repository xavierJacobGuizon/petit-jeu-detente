package org.jeuroute.configuration.indicators;

import java.util.ArrayList;
import java.util.List;
import org.jeuroute.configuration.XmlResourceCache;
import org.jeuroute.gamecore.enums.HudAnchor;
import org.jeuroute.model.records.configuration.HudIndicatorConfiguration;
import org.jeuroute.model.records.configuration.HudIndicatorSlotConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class HudIndicatorConfigurationCache {

	private final XmlResourceCache<HudIndicatorConfiguration> cache = new XmlResourceCache<>(
		HudIndicatorConfigurationCache::parseConfiguration
	);

	public HudIndicatorConfiguration getOrLoad(String resourcePath) {
		return cache.getOrLoad(resourcePath);
	}

	private static HudIndicatorConfiguration parseConfiguration(Element root) {
		if (!"hud".equals(root.getTagName())) {
			throw new IllegalArgumentException("HUD indicator configuration root must be 'hud'");
		}
		List<HudIndicatorSlotConfiguration> slots = new ArrayList<>();
		NodeList children = root.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element slotElement = (Element) child;
			if (!"slot".equals(slotElement.getTagName())) {
				throw new IllegalArgumentException(
					"Unexpected element '" + slotElement.getTagName() + "' inside 'hud'"
				);
			}
			slots.add(parseSlot(slotElement));
		}
		return new HudIndicatorConfiguration(slots);
	}

	private static HudIndicatorSlotConfiguration parseSlot(Element element) {
		HudAnchor anchor;
		try {
			anchor = HudAnchor.valueOf(requiredAttribute(element, "anchor"));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Invalid HUD indicator slot anchor", exception);
		}
		List<String> indicatorIds = new ArrayList<>();
		NodeList children = element.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element indicatorElement = (Element) child;
			if (!"indicator".equals(indicatorElement.getTagName())) {
				throw new IllegalArgumentException(
					"Unexpected element '" + indicatorElement.getTagName() + "' inside 'slot'"
				);
			}
			indicatorIds.add(requiredAttribute(indicatorElement, "ref"));
		}
		return new HudIndicatorSlotConfiguration(anchor, indicatorIds);
	}

	private static String requiredAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(
				"Missing required attribute '" +
					name +
					"' on element '" +
					element.getTagName() +
					"'"
			);
		}
		return value;
	}
}
