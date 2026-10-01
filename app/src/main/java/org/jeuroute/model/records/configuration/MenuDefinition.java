package org.jeuroute.model.records.configuration;

import java.util.List;
import java.util.Objects;

public record MenuDefinition(
	String id,
	String label,
	String action,
	List<MenuDefinition> children
) {
	public MenuDefinition {
		Objects.requireNonNull(id, "Menu id cannot be null");
		Objects.requireNonNull(label, "Menu label cannot be null");
		children = children == null ? List.of() : List.copyOf(children);
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}
}
