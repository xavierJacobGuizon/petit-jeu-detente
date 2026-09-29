package org.jeuroute.configuration.menus;

import java.util.List;
import org.jeuroute.configuration.XmlResourceCache;
import org.jeuroute.model.configuration.MenuDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class MenuDefinitionCache {

	private final XmlResourceCache<MenuDefinition> cache = new XmlResourceCache<>(
		MenuDefinitionCache::elementToMenu
	);

	public MenuDefinition getOrLoad(String resourcePath) {
		return cache.getOrLoad(resourcePath);
	}

	private static MenuDefinition elementToMenu(Element element) {
		String id = getAttribute(element, "id");
		String label = getAttribute(element, "label");
		String action = getAttributeOrDefault(element, "action", "");
		List<MenuDefinition> children = parseChildren(element);
		return new MenuDefinition(id, label, action, children);
	}

	private static List<MenuDefinition> parseChildren(Element element) {
		NodeList children = element.getChildNodes();
		java.util.List<MenuDefinition> result = new java.util.ArrayList<>();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element childElement = (Element) child;
			if (
				!"button".equals(childElement.getTagName()) &&
				!"menu".equals(childElement.getTagName())
			) {
				continue;
			}
			result.add(elementToMenu(childElement));
		}
		return result;
	}

	private static String getAttribute(Element element, String name) {
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

	private static String getAttributeOrDefault(Element element, String name, String defaultValue) {
		String value = element.getAttribute(name);
		return value == null || value.isBlank() ? defaultValue : value;
	}
}
