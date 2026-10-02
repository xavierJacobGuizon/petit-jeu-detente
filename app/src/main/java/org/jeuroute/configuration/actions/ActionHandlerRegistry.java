package org.jeuroute.configuration.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jeuroute.configuration.Registry;
import org.jeuroute.gamecore.MouseHandler;

public final class ActionHandlerRegistry implements Registry<Runnable> {

	private final Map<String, Runnable> handlers = new HashMap<>();

	public ActionHandlerRegistry registerDefaults(
		MouseHandler mouseHandler,
		Runnable toggleLineCreation,
		Runnable toggleDepotCreation
	) {
		Objects.requireNonNull(mouseHandler, "mouseHandler cannot be null");
		return register("toggle-route", () ->
			mouseHandler.setRouteCreationEnabled(!mouseHandler.isRouteCreationEnabled())
		)
			.register("toggle-vehicle", () ->
				mouseHandler.setVehicleCreationEnabled(!mouseHandler.isVehicleCreationEnabled())
			)
			.register("toggle-station", () ->
				mouseHandler.setStationCreationEnabled(!mouseHandler.isStationCreationEnabled())
			)
			.register("toggle-line", toggleLineCreation)
			.register("toggle-depot", toggleDepotCreation)
			.register("toggle-person", () ->
				mouseHandler.setPersonCreationEnabled(!mouseHandler.isPersonCreationEnabled())
			);
	}

	public ActionHandlerRegistry register(String actionId, Runnable handler) {
		if (actionId == null || actionId.isBlank()) {
			throw new IllegalArgumentException("Action id cannot be blank");
		}
		handlers.put(actionId, Objects.requireNonNull(handler, "handler cannot be null"));
		return this;
	}

	public Runnable get(String actionId) {
		Runnable handler = handlers.get(actionId);
		if (handler == null) {
			throw new IllegalArgumentException("No handler registered for action: " + actionId);
		}
		return handler;
	}
}
