package org.jeuroute.gamecore.controllers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.elements.HudButton;
import org.jeuroute.manager.GameManager;
import org.jeuroute.manager.input.MouseHandlerManager;
import org.jeuroute.manager.transport.LineManager;
import org.jeuroute.manager.world.FixedEntityManager;
import org.jeuroute.model.records.preview.LinePreview;
import org.jeuroute.model.records.preview.enums.LinePreviewStatus;
import org.jeuroute.model.world.transport.Station;
import org.jeuroute.model.world.transport.TransitLine;

/**
 * Gère le flux de création d'une nouvelle ligne : sélection des stations au
 * clic, aperçu du tracé en cours, validation et annulation. Extrait de
 * {@link GameManager} afin de séparer cette responsabilité du reste de
 * l'orchestration du jeu.
 */
public final class LineCreationController {

	private final FixedEntityManager fixedEntityManager;
	private final LineManager lineManager;
	private final MouseHandlerManager mouseHandlerManager;
	private final Supplier<Hud> hudSupplier;
	private final List<Station> selectedStations = new ArrayList<>();

	public LineCreationController(
		FixedEntityManager fixedEntityManager,
		LineManager lineManager,
		MouseHandlerManager mouseHandlerManager,
		Supplier<Hud> hudSupplier
	) {
		this.fixedEntityManager = fixedEntityManager;
		this.lineManager = lineManager;
		this.mouseHandlerManager = mouseHandlerManager;
		this.hudSupplier = hudSupplier;
	}

	private Hud hud() {
		return hudSupplier.get();
	}

	/** Bascule le mode de création de ligne.
	 * Active ou désactive la sélection des stations et l'affichage du bouton de validation.
	 */
	public void toggle() {
		selectedStations.clear();

		MouseHandler handler = mouseHandlerManager.getMouseHandler();
		handler.setLineCreationEnabled(!handler.isLineCreationEnabled());

		if (handler.isLineCreationEnabled()) {
			showValidationAction();
		} else {
			hud().hideOneShotActionButton();
		}
	}

	public void handleStationSelection(Point clickPosition) {
		if (!mouseHandlerManager.getMouseHandler().isLineCreationEnabled()) {
			return;
		}

		Station selectedStation = fixedEntityManager.findStationNear(clickPosition, 18.0);
		if (selectedStation == null) {
			return;
		}

		if (
			selectedStations
				.stream()
				.anyMatch(station -> station.getPosition().equals(selectedStation.getPosition()))
		) {
			return;
		}

		if (
			!selectedStations.isEmpty() &&
			!lineManager.canConnect(selectedStations.getLast(), selectedStation)
		) {
			hud().showDialog(
				"AUCUN CHEMIN VERS STATION",
				List.of(new HudButton("FERMER", () -> {}))
			);
			return;
		}

		selectedStations.add(selectedStation);
		if (hud().getOneShotActionButton() == null) {
			showValidationAction();
		}
	}

	public LinePreview getPreview() {
		if (selectedStations.isEmpty()) {
			return null;
		}

		MouseHandler mouseHandler = mouseHandlerManager.getMouseHandler();
		if (!mouseHandler.isLineCreationEnabled()) {
			return null;
		}

		Point mousePosition = mouseHandler.getMousePosition();
		if (mousePosition == null) {
			return null;
		}

		LinePreviewStatus status = LinePreviewStatus.NORMAL;
		Station hoveredStation = fixedEntityManager.findStationNear(mousePosition, 18.0);
		if (hoveredStation != null) {
			status = lineManager.canConnect(selectedStations.getLast(), hoveredStation)
				? LinePreviewStatus.CONNECTABLE
				: LinePreviewStatus.DISCONNECTED;
		}

		return new LinePreview(
			this.selectedStations
				.stream()
				.filter(Objects::nonNull)
				.map(Station::getPosition)
				.toList(),
			mousePosition,
			status
		);
	}

	private void showValidationAction() {
		hud().showOneShotActionButton(new HudButton("VALIDER", this::validate), this::cancel);
	}

	public void cancel() {
		selectedStations.clear();
		MouseHandler handler = mouseHandlerManager.getMouseHandler();
		handler.setLineCreationEnabled(false);
		handler.consumeLineStationSelection();
		hud().hideOneShotActionButton();
	}

	private void validate() {
		if (!mouseHandlerManager.getMouseHandler().isLineCreationEnabled()) {
			return;
		}
		if (selectedStations.size() < 2) {
			hud().showDialog("CHOISIR DEUX STATIONS", List.of(new HudButton("FERMER", () -> {})));
			return;
		}

		Optional<TransitLine> line = lineManager.createLine(List.copyOf(selectedStations));
		if (line.isEmpty()) {
			hud().showDialog("TRAJET INVALIDE", List.of(new HudButton("FERMER", () -> {})));
			return;
		}
		cancel();
		hud().hideOneShotActionButton();
	}
}
