package org.jeuroute.manager;

import java.awt.Point;
import java.util.List;
import org.jeuroute.configuration.actions.ActionHandlerRegistry;
import org.jeuroute.configuration.indicators.IndicatorRegistry;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.elements.HudButton;
import org.jeuroute.model.records.preview.LinePreview;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;
import org.jeuroute.model.world.transport.TransitLine;
import org.jeuroute.model.world.transport.Vehicle;

/**
 * Orchestre les collaborateurs principaux du jeu (graphe de routes,
 * véhicules, lignes, entités fixes, HUD). Les flux plus spécifiques
 * (création de ligne, boîtes de dialogue de gestion) sont délégués à
 * {@link LineCreationController} et {@link LineVehicleDialogController} afin
 * de garder cette classe centrée sur le câblage et la boucle de jeu.
 */
public final class GameManager {

	private final WorldMap worldMap = new WorldMap();
	private final VehicleManager vehicleManager = new VehicleManager(worldMap.getRoadGraph());
	private final LineManager lineManager = new LineManager(worldMap.getRoadGraph());

	private final ActionHandlerRegistry actionHandlers = new ActionHandlerRegistry();
	private final IndicatorRegistry indicatorRegistry = new IndicatorRegistry();

	private int fps;
	private final MouseHandlerManager mouseHandlerManager = new MouseHandlerManager(
		worldMap.getRoadGraph(),
		new MouseHandler(),
		vehicleManager,
		worldMap.getFixedEntityManager(),
		this::handleLineStationSelection
	);

	private final LineCreationController lineCreationController = new LineCreationController(
		worldMap.getFixedEntityManager(),
		lineManager,
		mouseHandlerManager,
		this::getHud
	);

	private final LineVehicleDialogController lineVehicleDialogController =
		new LineVehicleDialogController(lineManager, vehicleManager, this::getHud);

	private final HudManager hudManager;

	public GameManager() {
		initializeHud();
		hudManager = new HudManager(actionHandlers, indicatorRegistry);
		hudManager.addButton(
			HudButton.submenu(
				"LIGNES",
				List.of(
					new HudButton("CREER LIGNE", lineCreationController::toggle),
					new HudButton("VEHICULES", lineVehicleDialogController::openVehicleListDialog),
					new HudButton(
						"GERER LIGNES",
						lineVehicleDialogController::openLineManagementDialog
					)
				)
			)
		);
		hudManager.addButton(
			HudButton.submenu(
				"ROUTES",
				List.of(
					new HudButton("DOUBLE SENS", actionHandlers.get("toggle-route")),
					new HudButton("SENS UNIQUE", () -> {})
				)
			)
		);
		initializeWorld();
	}

	public RoadGraph getRoadGraph() {
		return worldMap.getRoadGraph();
	}

	public WorldMap getWorldMap() {
		return worldMap;
	}

	public VehicleManager getVehicleManager() {
		return vehicleManager;
	}

	public LineManager getLineManager() {
		return lineManager;
	}

	public ResourceBuildingManager getResourceBuildingManager() {
		return worldMap.getResourceBuildingManager();
	}

	public List<Station> getStations() {
		return worldMap.getStations();
	}

	public Depot getDepot() {
		return worldMap.getDepots().getFirst();
	}

	public List<Depot> getDepots() {
		return worldMap.getDepots();
	}

	public ActionHandlerRegistry getActionHandlers() {
		return actionHandlers;
	}

	public IndicatorRegistry getIndicatorHandlers() {
		return indicatorRegistry;
	}

	public FixedEntityManager getFixedEntityManager() {
		return worldMap.getFixedEntityManager();
	}

	public MouseHandlerManager getMouseHandlerManager() {
		return mouseHandlerManager;
	}

	public Hud getHud() {
		return hudManager.getHud();
	}

	public HudManager getHudManager() {
		return hudManager;
	}

	public void update(double deltaSeconds) {
		fps = deltaSeconds > 0.0 ? (int) Math.round(1.0 / deltaSeconds) : 0;

		for (Vehicle vehicle : vehicleManager.getVehicles()) {
			vehicle.update(deltaSeconds);
		}

		mouseHandlerManager.processPendingInput();
		Point depotPosition = mouseHandlerManager.getMouseHandler().consumeDepotPlacement();
		if (depotPosition != null) {
			createDepotAt(depotPosition);
		}
		synchronizeFixedEntities();
		worldMap.update(deltaSeconds);
	}

	public void cancelActiveAction() {
		boolean lineCreationEnabled = mouseHandlerManager.getMouseHandler().isLineCreationEnabled();
		mouseHandlerManager.cancelCurrentAction();
		if (lineCreationEnabled) {
			lineCreationController.cancel();
		}
	}

	public void renderDragLine() {
		renderDragLine(1.0);
	}

	public void renderDragLine(double zoom) {
		mouseHandlerManager.renderDragLine(zoom);
		mouseHandlerManager.renderPlacementPreview(zoom);
		WorldRenderer.renderLinePreview(getLinePreview(), zoom);
	}

	public LinePreview getLinePreview() {
		return lineCreationController.getPreview();
	}

	private void handleLineStationSelection(Point clickPosition) {
		lineCreationController.handleStationSelection(clickPosition);
	}

	private void initializeWorld() {
		Depot depot = worldMap.initializeDefaultLayout();
		vehicleManager.addDepotAccessPosition(depot.getAccessPosition());

		Road firstRoad = worldMap.getRoadGraph().findRoadNear(new Point(60, 350), 1.0);
		Road secondRoad = worldMap.getRoadGraph().findRoadNear(new Point(60, 525), 1.0);
		if (firstRoad == null || secondRoad == null) {
			throw new IllegalStateException(
				"The depot roads did not connect to the starting streets"
			);
		}

		vehicleManager.createVehicle(
			firstRoad,
			24,
			180,
			180_000.0,
			firstRoad.getStart(),
			firstRoad.getEnd()
		);
		vehicleManager.createVehicle(
			secondRoad,
			30,
			240,
			220_000.0,
			secondRoad.getStart(),
			secondRoad.getEnd()
		);
		vehicleManager.createVehicle(
			null,
			30,
			180,
			180_000.0,
			depot.getAccessPosition(),
			depot.getAccessPosition()
		);
		synchronizeFixedEntities();
	}

	private void createDepotAt(Point position) {
		worldMap
			.createDepot(position)
			.ifPresent(depot -> vehicleManager.addDepotAccessPosition(depot.getAccessPosition()));
	}

	private void toggleDepotCreation() {
		MouseHandler handler = mouseHandlerManager.getMouseHandler();
		handler.setDepotCreationEnabled(!handler.isDepotCreationEnabled());
	}

	private void synchronizeFixedEntities() {
		if (!worldMap.synchronizeGraphEntities()) {
			return;
		}
		List<TransitLine> removedLines = lineManager.refreshPaths();
		for (Vehicle vehicle : vehicleManager.getVehicles()) {
			if (
				vehicle.getAssignedLine() != null &&
				removedLines.contains(vehicle.getAssignedLine())
			) {
				vehicle.unassignLine();
			}
		}
	}

	private void initializeHud() {
		actionHandlers.registerDefaults(
			this.mouseHandlerManager.getMouseHandler(),
			lineCreationController::toggle,
			this::toggleDepotCreation
		);
		indicatorRegistry.registerDefaults(
			() -> Integer.toString(fps),
			() -> Integer.toString(worldMap.getRoadGraph().getRoads().size()),
			() -> Integer.toString(worldMap.getFixedEntityManager().getIntersections().size()),
			() -> Integer.toString(vehicleManager.getVehicles().size()),
			() -> Integer.toString(worldMap.getStations().size())
		);
	}
}
