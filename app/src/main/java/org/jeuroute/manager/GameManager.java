package org.jeuroute.manager;

import java.awt.Point;
import java.util.List;
import org.jeuroute.configuration.actions.ActionHandlerRegistry;
import org.jeuroute.configuration.indicators.IndicatorRegistry;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.WorldRenderer;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.HudButton;
import org.jeuroute.gamecore.preview.LinePreview;
import org.jeuroute.manager.lignes.LineCreationController;
import org.jeuroute.model.jouet.Depot;
import org.jeuroute.model.jouet.Road;
import org.jeuroute.model.jouet.RoadGraph;
import org.jeuroute.model.jouet.Station;
import org.jeuroute.model.jouet.TransitLine;
import org.jeuroute.model.jouet.Vehicle;

/**
 * Orchestre les collaborateurs principaux du jeu (graphe de routes,
 * véhicules, lignes, entités fixes, HUD). Les flux plus spécifiques
 * (création de ligne, boîtes de dialogue de gestion) sont délégués à
 * {@link LineCreationController} et {@link LineVehicleDialogController} afin
 * de garder cette classe centrée sur le câblage et la boucle de jeu.
 */
public final class GameManager {

	private final FixedEntityManager fixedEntityManager = new FixedEntityManager();
	private final RoadGraph roadGraph = new RoadGraph();
	private final VehicleManager vehicleManager = new VehicleManager(roadGraph);
	private final LineManager lineManager = new LineManager(roadGraph);

	private final ActionHandlerRegistry actionHandlers = new ActionHandlerRegistry();
	private final IndicatorRegistry indicatorRegistry = new IndicatorRegistry();

	private long synchronizedFixedEntityGraphVersion = -1;
	private int fps;
	private final MouseHandlerManager mouseHandlerManager = new MouseHandlerManager(
		roadGraph,
		new MouseHandler(),
		vehicleManager,
		fixedEntityManager,
		this::handleLineStationSelection
	);

	private final LineCreationController lineCreationController = new LineCreationController(
		fixedEntityManager,
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
					new HudButton("AJOUTER ROUTE", actionHandlers.get("toggle-route")),
					new HudButton("ROUTE SENS UNIQUE", () -> {})
				)
			)
		);
		initializeWorld();
	}

	public RoadGraph getRoadGraph() {
		return roadGraph;
	}

	public VehicleManager getVehicleManager() {
		return vehicleManager;
	}

	public LineManager getLineManager() {
		return lineManager;
	}

	public List<Station> getStations() {
		return fixedEntityManager.getStations();
	}

	public Depot getDepot() {
		return fixedEntityManager.getDepots().getFirst();
	}

	public List<Depot> getDepots() {
		return fixedEntityManager.getDepots();
	}

	public ActionHandlerRegistry getActionHandlers() {
		return actionHandlers;
	}

	public IndicatorRegistry getIndicatorHandlers() {
		return indicatorRegistry;
	}

	public FixedEntityManager getFixedEntityManager() {
		return fixedEntityManager;
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
		roadGraph.createRoad(new Point(50, 360), new Point(123, 360));
		roadGraph.createRoad(new Point(50, 520), new Point(123, 520));
		roadGraph.createRoad(new Point(125, 350), new Point(125, 525));
		Depot depot = fixedEntityManager.createDepot(new Point(75, 275));
		roadGraph.createRoad(depot.getAccessPosition(), depot.getRoadEndPosition());
		vehicleManager.addDepotAccessPosition(depot.getAccessPosition());

		Road firstRoad = roadGraph.findRoadNear(new Point(60, 350), 1.0);
		Road secondRoad = roadGraph.findRoadNear(new Point(60, 525), 1.0);
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
		if (!fixedEntityManager.getDepotPlacementPreview(position).valid()) {
			return;
		}
		Depot depot = fixedEntityManager.createDepot(position);
		Point accessPosition = depot.getAccessPosition();
		roadGraph.createRoad(accessPosition, depot.getRoadEndPosition());
		vehicleManager.addDepotAccessPosition(accessPosition);
	}

	private void toggleDepotCreation() {
		MouseHandler handler = mouseHandlerManager.getMouseHandler();
		handler.setDepotCreationEnabled(!handler.isDepotCreationEnabled());
	}

	private void synchronizeFixedEntities() {
		long graphVersion = roadGraph.getVersion();
		if (synchronizedFixedEntityGraphVersion == graphVersion) {
			return;
		}
		fixedEntityManager.synchronizeIntersections(
			roadGraph.getIntersectionPositions(),
			roadGraph
		);
		fixedEntityManager.synchronizeStations(roadGraph);
		List<TransitLine> removedLines = lineManager.refreshPaths();
		for (Vehicle vehicle : vehicleManager.getVehicles()) {
			if (
				vehicle.getAssignedLine() != null &&
				removedLines.contains(vehicle.getAssignedLine())
			) {
				vehicle.unassignLine();
			}
		}
		synchronizedFixedEntityGraphVersion = graphVersion;
	}

	private void initializeHud() {
		actionHandlers.registerDefaults(
			this.mouseHandlerManager.getMouseHandler(),
			lineCreationController::toggle,
			this::toggleDepotCreation
		);
		indicatorRegistry.registerDefaults(
			() -> Integer.toString(fps),
			() -> Integer.toString(roadGraph.getRoads().size()),
			() -> Integer.toString(fixedEntityManager.getIntersections().size()),
			() -> Integer.toString(vehicleManager.getVehicles().size()),
			() -> Integer.toString(fixedEntityManager.getStations().size())
		);
	}
}
