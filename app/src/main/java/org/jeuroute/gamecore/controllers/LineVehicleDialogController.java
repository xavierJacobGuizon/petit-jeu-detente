package org.jeuroute.gamecore.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.elements.HudButton;
import org.jeuroute.manager.GameManager;
import org.jeuroute.manager.LineManager;
import org.jeuroute.manager.VehicleManager;
import org.jeuroute.model.world.enums.LineColor;
import org.jeuroute.model.world.transport.TransitLine;
import org.jeuroute.model.world.transport.Vehicle;

/**
 * Gère les boîtes de dialogue HUD relatives aux lignes (liste, options,
 * couleur, affectation de véhicules) et aux véhicules (liste, retrait de
 * ligne). Extrait de {@link GameManager} afin de séparer cette responsabilité
 * du reste de l'orchestration du jeu.
 */
final class LineVehicleDialogController {

	private final LineManager lineManager;
	private final VehicleManager vehicleManager;
	private final Supplier<Hud> hudSupplier;

	LineVehicleDialogController(
		LineManager lineManager,
		VehicleManager vehicleManager,
		Supplier<Hud> hudSupplier
	) {
		this.lineManager = lineManager;
		this.vehicleManager = vehicleManager;
		this.hudSupplier = hudSupplier;
	}

	private Hud hud() {
		return hudSupplier.get();
	}

	void openLineManagementDialog() {
		List<TransitLine> lines = lineManager.getLines();
		if (lines.isEmpty()) {
			List<HudButton> options = new ArrayList<>();
			options.add(new HudButton("FERMER", () -> {}));
			hud().showDialog("AUCUNE LIGNE", options);
			return;
		}

		List<HudButton> options = new ArrayList<>();
		for (int index = 0; index < lines.size(); index++) {
			TransitLine line = lines.get(index);
			int lineNumber = index + 1;
			String vehicles = assignedVehicleNumbers(line);
			String label = vehicles.isEmpty()
				? "LIGNE " + lineNumber + " | SANS VEHICULE"
				: "LIGNE " + lineNumber + " | VEH " + vehicles;
			options.add(
				new HudButton(
					label,
					() -> openLineOptionsDialog(line, lineNumber),
					line.getColor().toAwtColor()
				)
			);
		}
		options.add(new HudButton("FERMER", () -> {}));
		hud().showDialog("LIGNES ET VEHICULES", options);
	}

	private String assignedVehicleNumbers(TransitLine line) {
		List<Vehicle> vehicles = vehicleManager.getVehicles();
		List<String> numbers = new ArrayList<>();
		for (Vehicle vehicle : vehicles) {
			if (vehicle.getAssignedLine() == line) {
				numbers.add(Integer.toString(vehicleManager.getVehicleNumber(vehicle)));
			}
		}
		return String.join(",", numbers);
	}

	private void openLineOptionsDialog(TransitLine line, int lineNumber) {
		hud().showDialog(
			"LIGNE " + lineNumber,
			List.of(
				new HudButton("COULEUR", () -> openLineColorDialog(line, lineNumber)),
				new HudButton("AJOUTER VEHICULE", () ->
					openAvailableVehicleDialog(line, lineNumber)
				),
				new HudButton("FERMER", () -> {})
			)
		);
	}

	private void openLineColorDialog(TransitLine line, int lineNumber) {
		List<HudButton> options = new ArrayList<>();
		for (LineColor color : LineColor.values()) {
			options.add(
				new HudButton(color.getLabel(), () -> line.setColor(color), color.toAwtColor())
			);
		}
		options.add(new HudButton("ANNULER", () -> {}));
		hud().showDialog("COULEUR LIGNE " + lineNumber, options);
	}

	private void openAvailableVehicleDialog(TransitLine line, int lineNumber) {
		List<Vehicle> unassignedVehicles = vehicleManager.getUnassignedVehicles();
		List<HudButton> options = new ArrayList<>();
		for (Vehicle vehicle : unassignedVehicles) {
			int vehicleNumber = vehicleManager.getVehicleNumber(vehicle);
			options.add(
				new HudButton("VEHICULE " + vehicleNumber + " | LIBRE", () -> {
					if (lineManager.assignVehicle(vehicle, line)) {
						openAvailableVehicleDialog(line, lineNumber);
					}
				})
			);
		}
		options.add(new HudButton("RETOUR", () -> openLineOptionsDialog(line, lineNumber)));
		hud().showDialog("AJOUTER DES VEHICULES", options);
	}

	void openVehicleListDialog() {
		if (vehicleManager.getVehicles().isEmpty()) {
			List<HudButton> options = new ArrayList<>();
			options.add(new HudButton("FERMER", () -> {}));
			hud().showDialog("AUCUN VEHICULE", options);
			return;
		}

		List<HudButton> options = new ArrayList<>();
		for (Vehicle vehicle : vehicleManager.getVehicles()) {
			int vehicleNumber = vehicleManager.getVehicleNumber(vehicle);
			TransitLine assignedLine = vehicle.getAssignedLine();
			if (assignedLine == null) {
				options.add(new HudButton("VEHICULE " + vehicleNumber + " | LIBRE", () -> {}));
				continue;
			}

			int lineNumber = lineManager.getLines().indexOf(assignedLine) + 1;
			options.add(
				new HudButton("VEHICULE " + vehicleNumber + " | LIGNE " + lineNumber, () ->
					openVehicleRemovalDialog(vehicle, vehicleNumber, lineNumber)
				)
			);
		}
		options.add(new HudButton("FERMER", () -> {}));
		hud().showDialog("VEHICULES", options);
	}

	private void openVehicleRemovalDialog(Vehicle vehicle, int vehicleNumber, int lineNumber) {
		hud().showDialog(
			"VEHICULE " + vehicleNumber + " | LIGNE " + lineNumber,
			List.of(
				new HudButton("RETIRER DE LA LIGNE", () -> {
					vehicle.unassignLine();
					List<HudButton> options = new ArrayList<>();
					options.add(new HudButton("FERMER", () -> {}));
					hud().showDialog("VEHICULE " + vehicleNumber + " LIBRE", options);
				}),
				new HudButton("ANNULER", () -> {})
			)
		);
	}
}
