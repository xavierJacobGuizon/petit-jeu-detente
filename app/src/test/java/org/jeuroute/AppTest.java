package org.jeuroute;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jeuroute.configuration.actions.ActionHandlerRegistry;
import org.jeuroute.configuration.indicators.HudIndicatorConfigurationCache;
import org.jeuroute.configuration.menus.MenuDefinitionCache;
import org.jeuroute.gamecore.GameLoop;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.enums.HudAnchor;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.elements.HudButton;
import org.jeuroute.gamecore.hud.elements.HudIndicator;
import org.jeuroute.gamecore.hud.elements.HudIndicatorSlot;
import org.jeuroute.gamecore.hud.presentation.HudLayout;
import org.jeuroute.manager.FixedEntityManager;
import org.jeuroute.manager.GameManager;
import org.jeuroute.manager.HudManager;
import org.jeuroute.manager.LineManager;
import org.jeuroute.manager.MouseHandlerManager;
import org.jeuroute.manager.VehicleManager;
import org.jeuroute.model.records.configuration.HudIndicatorConfiguration;
import org.jeuroute.model.records.configuration.MenuDefinition;
import org.jeuroute.model.records.preview.LinePreview;
import org.jeuroute.model.records.preview.PlacementPreview;
import org.jeuroute.model.records.preview.enums.LinePreviewStatus;
import org.jeuroute.model.records.preview.enums.PlacementPreviewType;
import org.jeuroute.model.world.network.Intersection;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.network.RoadPath;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;
import org.jeuroute.model.world.transport.TransitLine;
import org.jeuroute.model.world.transport.Vehicle;
import org.junit.jupiter.api.Test;

class AppTest {

	private final RoadGraph graph = new RoadGraph();

	@Test
	void gameLoopShouldSupplyDeltaInSeconds() {
		GameLoop loop = new GameLoop();
		AtomicBoolean rendered = new AtomicBoolean();
		AtomicReference<Double> deltaSeconds = new AtomicReference<>();

		loop.start(rendered::get, () -> LockSupport.parkNanos(20_000_000L), deltaSeconds::set, () ->
			rendered.set(true)
		);

		assertTrue(deltaSeconds.get() >= 0.005);
		assertTrue(deltaSeconds.get() < 1.0);
	}

	@Test
	void cameraZoomKeepsTheCursorWorldPointAnchored() {
		Camera2D camera = new Camera2D(640.0, 360.0);
		Point anchorBeforeZoom = camera.screenToWorld(400, 250, 1280, 720);
		Point otherPointBeforeZoom = camera.screenToWorld(1000, 250, 1280, 720);

		camera.zoomAt(-1.0, 400, 250, 1280, 720);

		assertTrue(camera.getZoom() < 1.0);
		assertEquals(anchorBeforeZoom, camera.screenToWorld(400, 250, 1280, 720));
		assertTrue(camera.screenToWorld(1000, 250, 1280, 720).x > otherPointBeforeZoom.x);

		Point worldAtScreenCenter = camera.screenToWorld(640, 360, 1280, 720);
		camera.panByScreenPixels(-40, 0);
		assertTrue(camera.screenToWorld(640, 360, 1280, 720).x < worldAtScreenCenter.x);
	}

	@Test
	void cameraMovementAcceleratesToMaximumAndSlowsWhenReleased() {
		Camera2D camera = new Camera2D(640.0, 360.0);
		camera.setMovementInput(1.0, 0.0);

		camera.updateMovement(0.1);
		double firstSpeed = camera.getPanSpeed();
		camera.updateMovement(0.1);
		double secondSpeed = camera.getPanSpeed();
		assertTrue(firstSpeed > 0.0);
		assertTrue(secondSpeed > firstSpeed);

		camera.updateMovement(2.0);
		double maximumSpeed = camera.getPanSpeed();
		assertTrue(maximumSpeed > secondSpeed);
		camera.updateMovement(1.0);
		assertEquals(maximumSpeed, camera.getPanSpeed());

		camera.setMovementInput(0.0, 0.0);
		camera.updateMovement(0.2);
		assertTrue(camera.getPanSpeed() < maximumSpeed);
		assertTrue(camera.getPanSpeed() > 0.0);
		camera.updateMovement(2.0);
		assertEquals(0.0, camera.getPanSpeed());
	}

	@Test
	void hudDefaultMenuXmlIsLoadedFromResourceAndCached() {
		MenuDefinitionCache cache = new MenuDefinitionCache();

		MenuDefinition menu = cache.getOrLoad("menus/default-menu.xml");
		MenuDefinition sameMenu = cache.getOrLoad("menus/default-menu.xml");

		assertEquals("MENU", menu.label());
		assertEquals(5, menu.children().size());
		assertEquals("VEHICULE", menu.children().get(0).label());
		assertEquals("PERSONNE", menu.children().get(3).label());
		assertEquals("DEBUG", menu.children().get(4).label());
		assertEquals("toggle-debug", menu.children().get(4).action());
		assertSame(menu, sameMenu);
	}

	@Test
	void routesMenuOffersBidirectionalAndUnimplementedOneWayOptions() {
		GameManager game = new GameManager(new java.util.Random(42));
		Hud hud = game.getHud();
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();

		assertTrue(hud.handleClick(860, 685, 1280, 720));
		assertEquals("ROUTES", hud.getOpenMenu().getLabel());
		assertEquals("DOUBLE SENS", hud.getOpenMenu().getSubmenu().get(0).getLabel());
		assertEquals("SENS UNIQUE", hud.getOpenMenu().getSubmenu().get(1).getLabel());

		assertTrue(hud.handleClick(860, 639, 1280, 720));
		assertTrue(mouseHandler.isRouteCreationEnabled());
		assertTrue(hud.handleClick(860, 685, 1280, 720));
		assertTrue(hud.handleClick(860, 593, 1280, 720));
		assertTrue(mouseHandler.isRouteCreationEnabled());
	}

	@Test
	void hudDefaultIndicatorLayoutXmlIsLoadedAndCached() {
		HudIndicatorConfigurationCache cache = new HudIndicatorConfigurationCache();

		HudIndicatorConfiguration configuration = cache.getOrLoad("hud/default-indicators.xml");
		HudIndicatorConfiguration sameConfiguration = cache.getOrLoad("hud/default-indicators.xml");

		assertEquals(2, configuration.slots().size());
		assertEquals(HudAnchor.TOP_LEFT, configuration.slots().get(0).anchor());
		assertEquals(List.of("fps"), configuration.slots().get(0).indicatorIds());
		assertEquals(HudAnchor.TOP_RIGHT, configuration.slots().get(1).anchor());
		assertEquals(
			List.of("roads", "intersections", "vehicles", "stations", "people"),
			configuration.slots().get(1).indicatorIds()
		);
		assertSame(configuration, sameConfiguration);
	}

	@Test
	void actionHandlersCanBeRegisteredAndRetrieved() {
		ActionHandlerRegistry registry = new ActionHandlerRegistry();
		AtomicBoolean invoked = new AtomicBoolean();
		Runnable handler = () -> invoked.set(true);

		registry.register("custom-action", handler);

		assertSame(handler, registry.get("custom-action"));
		registry.get("custom-action").run();
		assertTrue(invoked.get());
		assertThrows(IllegalArgumentException.class, () -> registry.get("unknown-action"));
	}

	@Test
	void indicatorsCanBeRegisteredRetrievedAndDisplayedById() {
		HudManager hudManager = new GameManager(new java.util.Random(42)).getHudManager();
		AtomicInteger value = new AtomicInteger(4);
		HudIndicator indicator = new HudIndicator(
			"CUSTOM",
			() -> Integer.toString(value.get()),
			0.2f,
			0.4f,
			0.6f
		);

		hudManager.registerIndicator("custom-value", indicator);

		assertSame(indicator, hudManager.getIndicator("custom-value"));
		HudIndicatorSlot slot = hudManager.createIndicatorSlot(HudAnchor.BOTTOM_LEFT);
		slot.add(hudManager.getIndicator("custom-value"));
		assertEquals("4", slot.getIndicators().get(0).getValue());
		value.set(5);
		assertEquals("5", slot.getIndicators().get(0).getValue());
	}

	@Test
	void hudSubmenuOpensAndRunsItsConfiguredButton() {
		AtomicBoolean actionCalled = new AtomicBoolean();
		Hud hud = new Hud().addButton(
			HudButton.submenu(
				"MENU",
				java.util.List.of(new HudButton("ROUTE", () -> actionCalled.set(true)))
			)
		);

		assertTrue(hud.handleClick(1200, 685, 1280, 720));
		assertNotNull(hud.getOpenMenu());
		assertTrue(hud.handleClick(1200, 639, 1280, 720));

		assertTrue(actionCalled.get());
		assertNull(hud.getOpenMenu());
	}

	@Test
	void hudDialogCanSelectOptionsOnLaterPages() {
		AtomicBoolean selected = new AtomicBoolean();
		List<HudButton> options = new java.util.ArrayList<>();
		for (int index = 0; index < 20; index++) {
			int optionIndex = index;
			options.add(new HudButton("VEHICULE " + index, () -> selected.set(optionIndex == 18)));
		}
		Hud hud = new Hud();
		hud.showDialog("CHOISIR VEHICULE", options);

		assertTrue(hud.handleClick(700, 658, 1280, 720));
		assertTrue(hud.handleClick(640, 448, 1280, 720));

		assertTrue(selected.get());
		assertNull(hud.getDialog());
	}

	@Test
	void hudIndicatorSlotReadsUpdatedValues() {
		AtomicInteger count = new AtomicInteger(3);
		HudIndicatorSlot slot = new HudIndicatorSlot(HudAnchor.TOP_LEFT).add(
			new HudIndicator("V", () -> Integer.toString(count.get()), 0.5f, 0.7f, 0.3f)
		);

		assertEquals("3", slot.getIndicators().get(0).getValue());
		count.set(4);
		assertEquals("4", slot.getIndicators().get(0).getValue());
	}

	@Test
	void hudManagerOwnsDefaultDataAndAllowsAddingSlots() {
		GameManager game = new GameManager(new java.util.Random(42));
		RoadGraph hudGraph = game.getRoadGraph();
		HudManager hudManager = game.getHudManager();
		hudGraph.createRoad(new Point(0, 75), new Point(200, 75));
		game.setFramesPerSecond(60);

		HudIndicatorSlot customSlot = hudManager.createIndicatorSlot(HudAnchor.BOTTOM_LEFT);
		customSlot.add(new HudIndicator("TEST", () -> "OK", 0.4f, 0.8f, 0.6f));

		assertEquals(
			"60",
			hudManager.getHud().getIndicatorSlots().get(0).getIndicators().get(0).getValue()
		);
		assertEquals(
			Integer.toString(hudGraph.getRoads().size()),
			hudManager.getHud().getIndicatorSlots().get(1).getIndicators().get(0).getValue()
		);
		assertEquals(3, hudManager.getHud().getIndicatorSlots().size());
		assertEquals("OK", customSlot.getIndicators().get(0).getValue());
	}

	@Test
	void hudSlotsWithSameAnchorArePlacedWithoutOverlap() {
		HudIndicatorSlot first = new HudIndicatorSlot(HudAnchor.TOP_LEFT).add(
			new HudIndicator("A", () -> "1", 0.4f, 0.8f, 0.6f)
		);
		HudIndicatorSlot second = new HudIndicatorSlot(HudAnchor.TOP_LEFT).add(
			new HudIndicator("B", () -> "2", 0.4f, 0.8f, 0.6f)
		);
		HudIndicatorSlot opposite = new HudIndicatorSlot(HudAnchor.TOP_RIGHT).add(
			new HudIndicator("C", () -> "3", 0.4f, 0.8f, 0.6f)
		);

		var placements = HudLayout.layoutIndicatorSlots(
			java.util.List.of(first, second, opposite),
			640,
			360
		);

		assertEquals(3, placements.size());
		for (int firstIndex = 0; firstIndex < placements.size(); firstIndex++) {
			for (int secondIndex = firstIndex + 1; secondIndex < placements.size(); secondIndex++) {
				assertFalse(
					placements
						.get(firstIndex)
						.bounds()
						.intersects(placements.get(secondIndex).bounds())
				);
			}
		}
		assertTrue(placements.get(0).bounds().x() < placements.get(2).bounds().x());
	}

	@Test
	void hudLayoutRejectsWindowTooSmallForIndicatorSlot() {
		HudIndicatorSlot slot = new HudIndicatorSlot(HudAnchor.TOP_LEFT).add(
			new HudIndicator("A", () -> "1", 0.4f, 0.8f, 0.6f)
		);

		assertThrows(IllegalArgumentException.class, () ->
			HudLayout.layoutIndicatorSlots(java.util.List.of(slot), 100, 50)
		);
	}

	@Test
	void vehiclePlacementAtRoadEndpointChoosesTheOtherEndpoint() {
		VehicleManager vehicleManager = new VehicleManager(graph);
		Road road = graph.createRoad(new Point(0, 75), new Point(200, 75));

		vehicleManager.createVehicleAt(road.getStart());
		vehicleManager.createVehicleAt(road.getEnd());

		assertEquals(2, vehicleManager.getVehicles().size());
		assertEquals(road.getEnd(), vehicleManager.getVehicles().get(0).getTarget());
		assertEquals(road.getStart(), vehicleManager.getVehicles().get(1).getTarget());
	}

	@Test
	void gameManagerInitializesAnIsolatedGameWorld() {
		GameManager firstGame = new GameManager(new java.util.Random(42));
		GameManager secondGame = new GameManager(new java.util.Random(42));

		assertTrue(firstGame.getRoadGraph().getRoads().size() > 6);
		assertEquals(1, firstGame.getDepots().size());
		assertEquals(3, firstGame.getVehicleManager().getVehicles().size());
		assertNotSame(firstGame.getRoadGraph(), secondGame.getRoadGraph());
		assertNotSame(firstGame.getVehicleManager(), secondGame.getVehicleManager());
		assertNotSame(firstGame.getHud(), secondGame.getHud());
	}

	@Test
	void gameManagerVehiclesParkAtTheDepotWhenTheyHaveNoLine() {
		GameManager game = new GameManager(new java.util.Random(42));
		for (Vehicle vehicle : game.getVehicleManager().getVehicles()) {
			if (vehicle.getRoad() != null) {
				assertTrue(
					game
						.getRoadGraph()
						.findPathFromRoadPosition(
							vehicle.getPosition(),
							vehicle.getRoad(),
							game.getDepot().getAccessPosition()
						)
						.isPresent()
				);
			}
		}
		for (
			int frame = 0;
			frame < 30 &&
			game
				.getVehicleManager()
				.getVehicles()
				.stream()
				.anyMatch(vehicle -> !vehicle.isParkedAtDepot());
			frame++
		) {
			game.update(1.0);
		}

		for (Vehicle vehicle : game.getVehicleManager().getVehicles()) {
			assertTrue(vehicle.isParkedAtDepot());
			assertEquals(game.getDepot().getAccessPosition(), vehicle.getPosition());
		}
	}

	@Test
	void selectedRouteHighlightsIntersectionAndRouteEndpointsWithinSnapRange() {
		GameManager game = new GameManager(new java.util.Random(42));
		RoadGraph roadGraph = game.getRoadGraph();
		roadGraph.createRoad(new Point(200, 200), new Point(400, 200));
		roadGraph.createRoad(new Point(300, 100), new Point(300, 300));
		roadGraph.createRoad(new Point(500, 200), new Point(600, 200));
		game.update(0.0);
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		mouseHandler.setRouteCreationEnabled(true);

		mouseHandler.onMove(503, 201);
		assertEquals(new Point(500, 200), game.getMouseHandlerManager().getRouteSnapPoint());
		mouseHandler.onMove(301, 201);
		assertEquals(new Point(300, 200), game.getMouseHandlerManager().getRouteSnapPoint());
		mouseHandler.onMove(326, 201);
		assertNull(game.getMouseHandlerManager().getRouteSnapPoint());

		mouseHandler.onRoutePlacement(225, 250);
		mouseHandler.onMove(308, 201);
		assertEquals(new Point(300, 200), game.getMouseHandlerManager().getRouteSnapPoint());
		mouseHandler.onRelease(0, 308, 201);
		game.update(0.0);
		assertTrue(roadGraph.findPath(new Point(225, 250), new Point(300, 200)).isPresent());

		assertEquals(new Point(300, 200), game.getMouseHandlerManager().getRouteSnapPoint());
		mouseHandler.setRouteCreationEnabled(false);
		assertNull(game.getMouseHandlerManager().getRouteSnapPoint());
	}

	@Test
	void routePreviewShowsEveryIntersectionItWillCreate() {
		RoadGraph roadGraph = new RoadGraph();
		MouseHandler mouseHandler = new MouseHandler();
		MouseHandlerManager mouseHandlerManager = new MouseHandlerManager(
			roadGraph,
			mouseHandler,
			new VehicleManager(roadGraph),
			new FixedEntityManager(),
			station -> {}
		);
		roadGraph.createRoad(new Point(150, 100), new Point(150, 300));
		roadGraph.createRoad(new Point(250, 100), new Point(250, 300));
		mouseHandler.setRouteCreationEnabled(true);
		mouseHandler.onRoutePlacement(100, 200);
		mouseHandler.onMove(300, 200);

		List<Point> futureIntersections = mouseHandlerManager.getFutureRouteIntersections();
		assertEquals(2, futureIntersections.size());
		assertTrue(futureIntersections.contains(new Point(150, 200)));
		assertTrue(futureIntersections.contains(new Point(250, 200)));
		assertEquals(
			List.of(
				List.of(new Point(100, 200), new Point(150, 200)),
				List.of(new Point(150, 200), new Point(250, 200)),
				List.of(new Point(250, 200), new Point(300, 200))
			),
			roadEndpoints(
				roadGraph.getRoadSegmentsAfterAddingRoad(new Point(100, 200), new Point(300, 200))
			)
		);
	}

	@Test
	void depotCanBePlacedFromHudAndUsedByUnassignedVehicles() {
		GameManager game = new GameManager(new java.util.Random(42));
		Hud hud = game.getHud();
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();

		assertTrue(hud.handleClick(1200, 685, 1280, 720));
		assertTrue(hud.handleClick(1200, 547, 1280, 720));
		assertTrue(mouseHandler.isDepotCreationEnabled());
		mouseHandler.onMove(100, 100);
		PlacementPreview preview = game.getMouseHandlerManager().getPlacementPreview();
		assertEquals(PlacementPreviewType.DEPOT, preview.type());
		assertTrue(preview.valid());

		mouseHandler.onDepotPlacement(100, 100);
		game.update(0.0);

		assertEquals(2, game.getDepots().size());
		var placedDepot = game.getDepots().getLast();
		assertEquals(new Point(100, 100), placedDepot.getPosition());
		assertEquals(new Point(100, 125), placedDepot.getAccessPosition());
		assertEquals(new Point(100, 200), placedDepot.getRoadEndPosition());
		assertTrue(
			game
				.getRoadGraph()
				.findPath(placedDepot.getAccessPosition(), placedDepot.getRoadEndPosition())
				.isPresent()
		);
		assertEquals(
			Depot.ROAD_STUB_LENGTH,
			game
				.getRoadGraph()
				.findPath(placedDepot.getAccessPosition(), placedDepot.getRoadEndPosition())
				.orElseThrow()
				.getLength(),
			0.001
		);

		Road accessRoad = game.getRoadGraph().findRoadNear(placedDepot.getAccessPosition(), 1.0);
		Vehicle vehicle = game
			.getVehicleManager()
			.createVehicle(
				accessRoad,
				10,
				100.0,
				placedDepot.getAccessPosition(),
				accessRoad.getEnd()
			);
		vehicle.update(0.1);
		assertEquals(placedDepot.getAccessPosition(), vehicle.getPosition());
		assertTrue(vehicle.isParkedAtDepot());
	}

	@Test
	void gameManagerOwnsDepositsPlacedDuringGameUpdates() {
		GameManager game = new GameManager(new java.util.Random(42));
		game.getMouseHandlerManager().getMouseHandler().setStationCreationEnabled(true);
		game.getMouseHandlerManager().getMouseHandler().onStationPlacement(401, 299);

		game.update(0.0);

		assertEquals(1, game.getStations().size());
		assertEquals(new Point(400, 300), game.getStations().get(0).getPosition());
		assertTrue(game.getRoadGraph().isStationNodeAt(new Point(400, 300)));
		assertThrows(UnsupportedOperationException.class, () ->
			game.getStations().add(new Station(new Point(500, 300)))
		);
	}

	@Test
	void roadGraphCreatesUniqueStationAndConnectsRoads() {
		RoadGraph stationGraph = new RoadGraph();
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		stationGraph.createRoad(new Point(0, 100), new Point(200, 100));

		Station station = fixedEntityManager.createStation(new Point(101, 101));
		stationGraph.addStationNode(station.getPosition());
		fixedEntityManager.synchronizeStations(stationGraph);

		assertEquals(new Point(100, 100), station.getPosition());
		assertEquals(1, fixedEntityManager.getStations().size());
		assertEquals(2, station.getRoads().size());
		assertSame(station, fixedEntityManager.createStation(new Point(100, 100)));
		assertEquals(1, fixedEntityManager.getStations().size());

		stationGraph.addRoad(new Road(new Point(100, 100), new Point(100, 200)));
		fixedEntityManager.synchronizeStations(stationGraph);

		assertEquals(3, station.getRoads().size());
		assertTrue(
			station
				.getRoads()
				.stream()
				.allMatch(road -> road.hasEndpoint(station.getPosition()))
		);
	}

	@Test
	void stationReplacesIntersectionAtSameGridPosition() {
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		RoadGraph stationGraph = new RoadGraph();
		stationGraph.addRoad(new Road(new Point(0, 100), new Point(200, 100)));
		stationGraph.addRoad(new Road(new Point(100, 0), new Point(100, 200)));
		Point crossing = new Point(100, 100);
		fixedEntityManager.synchronizeIntersections(
			stationGraph.getIntersectionPositions(),
			stationGraph
		);
		assertNotNull(fixedEntityManager.getIntersectionAt(crossing));
		assertEquals(1, fixedEntityManager.getIntersections().size());

		Station station = fixedEntityManager.createStation(crossing);
		stationGraph.addStationNode(station.getPosition());
		fixedEntityManager.synchronizeStations(stationGraph);
		fixedEntityManager.synchronizeIntersections(
			stationGraph.getIntersectionPositions(),
			stationGraph
		);

		assertFalse(stationGraph.isIntersectionAt(crossing));
		assertTrue(stationGraph.isStationNodeAt(crossing));
		assertNull(fixedEntityManager.getIntersectionAt(crossing));
		assertSame(station, fixedEntityManager.getStations().get(0));
		assertEquals(4, station.getRoads().size());
	}

	@Test
	void stationCanChooseConnectedRoadLikeAnIntersection() {
		RoadGraph stationGraph = new RoadGraph();
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		Station station = fixedEntityManager.createStation(new Point(100, 100));
		stationGraph.addStationNode(station.getPosition());
		stationGraph.addRoad(new Road(new Point(0, 100), new Point(100, 100)));
		stationGraph.addRoad(new Road(new Point(100, 100), new Point(100, 200)));

		Road selectedRoad = stationGraph.firstRightRoadAt(station.getPosition(), new Point(0, 100));

		assertNotNull(selectedRoad);
		assertEquals(new Point(100, 200), selectedRoad.otherEndpoint(station.getPosition()));
	}

	@Test
	void stationPlacementModeIsExclusiveAndQueuesPlacement() {
		MouseHandler mouseHandler = new MouseHandler();
		mouseHandler.setRouteCreationEnabled(true);
		mouseHandler.setStationCreationEnabled(true);

		assertFalse(mouseHandler.isRouteCreationEnabled());
		assertTrue(mouseHandler.isStationCreationEnabled());
		mouseHandler.onPress(0, 74.6, 125.2);
		assertEquals(new Point(75, 125), mouseHandler.consumeStationPlacement());
		assertNull(mouseHandler.consumeStationPlacement());
	}

	@Test
	void personPlacementDragQueuesSpacedInterpolatedPositions() {
		MouseHandler mouseHandler = new MouseHandler();
		mouseHandler.setPersonCreationEnabled(true);
		mouseHandler.onPress(0, 100, 100);
		mouseHandler.onMove(110, 100);
		mouseHandler.onMove(151, 100);
		mouseHandler.onRelease(0, 160, 100);
		mouseHandler.onMove(200, 100);

		assertEquals(
			List.of(
				new Point(100, 100),
				new Point(120, 100),
				new Point(140, 100),
				new Point(160, 100)
			),
			mouseHandler.consumePersonPlacements()
		);
		assertTrue(mouseHandler.consumePersonPlacements().isEmpty());
	}

	@Test
	void vehiclePlacementPreviewUsesTheSameRoadSnapAsVehicleCreation() {
		RoadGraph previewGraph = new RoadGraph();
		previewGraph.createRoad(new Point(0, 100), new Point(200, 100));
		VehicleManager vehicleManager = new VehicleManager(previewGraph);

		PlacementPreview validPreview = vehicleManager.getPlacementPreview(new Point(50, 108));
		PlacementPreview invalidPreview = vehicleManager.getPlacementPreview(new Point(50, 140));

		assertEquals(PlacementPreviewType.VEHICLE, validPreview.type());
		assertEquals(new Point(50, 100), validPreview.position());
		assertTrue(validPreview.valid());
		assertEquals(new Point(50, 140), invalidPreview.position());
		assertFalse(invalidPreview.valid());
	}

	@Test
	void stationPlacementPreviewSnapsToGridAndRejectsOccupiedCell() {
		FixedEntityManager fixedEntityManager = new FixedEntityManager();

		PlacementPreview availablePreview = fixedEntityManager.getStationPlacementPreview(
			new Point(101, 101)
		);
		fixedEntityManager.createStation(new Point(100, 100));
		PlacementPreview occupiedPreview = fixedEntityManager.getStationPlacementPreview(
			new Point(104, 104)
		);

		assertEquals(PlacementPreviewType.STATION, availablePreview.type());
		assertEquals(new Point(100, 100), availablePreview.position());
		assertTrue(availablePreview.valid());
		assertEquals(new Point(100, 100), occupiedPreview.position());
		assertFalse(occupiedPreview.valid());
	}

	@Test
	void activeMousePlacementModeTracksCursorWithSharedPreview() {
		RoadGraph previewGraph = new RoadGraph();
		previewGraph.createRoad(new Point(0, 100), new Point(200, 100));
		MouseHandler mouseHandler = new MouseHandler();
		MouseHandlerManager mouseHandlerManager = new MouseHandlerManager(
			previewGraph,
			mouseHandler,
			new VehicleManager(previewGraph),
			new FixedEntityManager(),
			selection -> {}
		);

		mouseHandler.setVehicleCreationEnabled(true);
		mouseHandler.onMove(50, 108);
		PlacementPreview vehiclePreview = mouseHandlerManager.getPlacementPreview();
		mouseHandler.setStationCreationEnabled(true);
		mouseHandler.onMove(101, 101);
		PlacementPreview stationPreview = mouseHandlerManager.getPlacementPreview();

		assertEquals(PlacementPreviewType.VEHICLE, vehiclePreview.type());
		assertEquals(new Point(50, 100), vehiclePreview.position());
		assertEquals(PlacementPreviewType.STATION, stationPreview.type());
		assertEquals(new Point(100, 100), stationPreview.position());
	}

	@Test
	void lineCreationPreviewConnectsSelectedStationToMouse() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station startStation = game.getFixedEntityManager().createStation(new Point(100, 100));
		Station middleStation = game.getFixedEntityManager().createStation(new Point(200, 100));
		game.getRoadGraph().createRoad(startStation.getPosition(), middleStation.getPosition());
		game.getRoadGraph().addStationNode(startStation.getPosition());
		game.getRoadGraph().addStationNode(middleStation.getPosition());
		game.update(0.0);

		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		mouseHandler.setLineCreationEnabled(true);
		mouseHandler.onLineStationSelection(100, 100);
		game.update(0.0);
		mouseHandler.onLineStationSelection(200, 100);
		game.update(0.0);
		mouseHandler.onMove(240, 160);

		LinePreview preview = game.getLinePreview();

		assertEquals(List.of(new Point(100, 100), new Point(200, 100)), preview.stations());
		assertEquals(new Point(200, 100), preview.start());
		assertEquals(new Point(240, 160), preview.end());
	}

	@Test
	void linePreviewColorStateReflectsHoveredStationConnectivity() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station start = game.getFixedEntityManager().createStation(new Point(100, 100));
		Station reachable = game.getFixedEntityManager().createStation(new Point(200, 100));
		Station isolated = game.getFixedEntityManager().createStation(new Point(100, 300));
		game.getRoadGraph().createRoad(start.getPosition(), reachable.getPosition());
		game.getRoadGraph().addStationNode(start.getPosition());
		game.getRoadGraph().addStationNode(reachable.getPosition());
		game.getRoadGraph().addStationNode(isolated.getPosition());
		game.update(0.0);

		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		mouseHandler.setLineCreationEnabled(true);
		mouseHandler.onLineStationSelection(start.getPosition().x, start.getPosition().y);
		game.update(0.0);

		mouseHandler.onMove(reachable.getPosition().x, reachable.getPosition().y);
		assertEquals(LinePreviewStatus.CONNECTABLE, game.getLinePreview().status());
		mouseHandler.onMove(isolated.getPosition().x, isolated.getPosition().y);
		assertEquals(LinePreviewStatus.DISCONNECTED, game.getLinePreview().status());
		mouseHandler.onMove(250, 250);
		assertEquals(LinePreviewStatus.NORMAL, game.getLinePreview().status());
	}

	@Test
	void hudOneShotActionButtonHidesBeforeRunningAndOnlyRunsOnce() {
		Hud hud = new Hud();
		AtomicBoolean hiddenDuringAction = new AtomicBoolean();
		AtomicInteger actionCount = new AtomicInteger();
		hud.showOneShotActionButton(
			new HudButton("CONTINUER", () -> {
				hiddenDuringAction.set(hud.getOneShotActionButton() == null);
				actionCount.incrementAndGet();
			})
		);

		assertTrue(hud.handleClick(1200, 685, 1280, 720));
		assertTrue(hiddenDuringAction.get());
		assertEquals(1, actionCount.get());
		assertFalse(hud.handleClick(1200, 685, 1280, 720));
	}

	@Test
	void choosingAnotherHudActionCancelsTheOneShotAction() {
		Hud hud = new Hud();
		AtomicBoolean cancelled = new AtomicBoolean();
		AtomicBoolean otherActionRan = new AtomicBoolean();
		hud.addButton(new HudButton("AUTRE", () -> otherActionRan.set(true)));
		hud.showOneShotActionButton(new HudButton("VALIDER", () -> {}), () -> cancelled.set(true));

		assertTrue(hud.handleClick(1200, 685, 1280, 720));

		assertTrue(cancelled.get());
		assertTrue(otherActionRan.get());
		assertNull(hud.getOneShotActionButton());
	}

	@Test
	void choosingAnotherModeCancelsLineCreationAndClearsItsPreview() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station station = game.getFixedEntityManager().createStation(new Point(100, 100));
		game.getRoadGraph().addStationNode(station.getPosition());
		game.update(0.0);
		Hud hud = game.getHud();
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();

		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));
		mouseHandler.onLineStationSelection(100, 100);
		game.update(0.0);
		mouseHandler.onMove(180, 140);
		assertNotNull(game.getLinePreview());

		assertTrue(hud.handleClick(860, 685, 1280, 720));
		assertTrue(hud.handleClick(860, 639, 1280, 720));

		assertNull(hud.getOneShotActionButton());
		assertFalse(mouseHandler.isLineCreationEnabled());
		assertTrue(mouseHandler.isRouteCreationEnabled());
		assertNull(game.getLinePreview());
	}

	@Test
	void pendingLineStationClickIsIgnoredAfterSwitchingModes() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station station = game.getFixedEntityManager().createStation(new Point(100, 100));
		game.getRoadGraph().addStationNode(station.getPosition());
		game.update(0.0);
		Hud hud = game.getHud();
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();

		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));
		mouseHandler.onLineStationSelection(100, 100);
		assertTrue(hud.handleClick(860, 685, 1280, 720));
		assertTrue(hud.handleClick(860, 639, 1280, 720));

		game.update(0.0);

		assertFalse(mouseHandler.isLineCreationEnabled());
		assertTrue(mouseHandler.isRouteCreationEnabled());
		assertNull(hud.getOneShotActionButton());
		assertNull(game.getLinePreview());
	}

	@Test
	void stationRoadPreviewMatchesRoadGraphAfterPlacement() {
		RoadGraph previewGraph = new RoadGraph();
		previewGraph.addRoad(new Road(new Point(0, 0), new Point(200, 50)));
		Point stationPosition = new Point(50, 0);

		List<Road> proposedSegments = previewGraph.getRoadSegmentsAfterAddingStation(
			stationPosition
		);
		previewGraph.addStationNode(stationPosition);

		assertEquals(2, proposedSegments.size());
		assertEquals(roadEndpoints(proposedSegments), roadEndpoints(previewGraph.getRoads()));
	}

	private static List<List<Point>> roadEndpoints(List<Road> roads) {
		return roads
			.stream()
			.map(road -> List.of(road.getStart(), road.getEnd()))
			.toList();
	}

	private static void advanceVehicleUntil(Vehicle vehicle, Point target) {
		for (int frame = 0; frame < 3600 && !target.equals(vehicle.getPosition()); frame++) {
			vehicle.update(1.0 / 60.0);
		}
		assertEquals(target, vehicle.getPosition());
	}

	private static void advanceGameVehicleUntil(GameManager game, Vehicle vehicle, Point target) {
		for (int frame = 0; frame < 3600 && !target.equals(vehicle.getPosition()); frame++) {
			game.update(1.0 / 60.0);
		}
		assertEquals(target, vehicle.getPosition());
	}

	@Test
	void roadGraphFindsShortestRouteAndReportsDisconnectedDestination() {
		RoadGraph pathGraph = new RoadGraph();
		pathGraph.addRoad(new Road(new Point(0, 0), new Point(100, 0)));
		pathGraph.addRoad(new Road(new Point(100, 0), new Point(100, 100)));
		pathGraph.addRoad(new Road(new Point(0, 0), new Point(200, 100)));

		RoadPath shortestPath = pathGraph
			.findPath(new Point(0, 0), new Point(100, 100))
			.orElseThrow();

		assertEquals(2, shortestPath.getLegs().size());
		assertEquals(50.0 + Math.hypot(100.0, 50.0), shortestPath.getLength(), 0.001);
		assertTrue(pathGraph.findPath(new Point(0, 0), new Point(300, 300)).isEmpty());
	}

	@Test
	void routeDragHighlightsAnExistingIntersectionWhenItsSnapPointMatches() {
		GameManager game = new GameManager(new java.util.Random(42));
		RoadGraph roadGraph = game.getRoadGraph();
		roadGraph.createRoad(new Point(200, 200), new Point(400, 200));
		roadGraph.createRoad(new Point(300, 100), new Point(300, 300));
		game.update(0.0);
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		mouseHandler.setRouteCreationEnabled(true);
		mouseHandler.onRoutePlacement(200, 200);
		mouseHandler.onMove(301, 201);

		assertEquals(new Point(300, 200), game.getMouseHandlerManager().getRouteSnapPoint());

		mouseHandler.onMove(326, 201);
		assertNull(game.getMouseHandlerManager().getRouteSnapPoint());
		mouseHandler.onRelease(0, 326, 201);
		assertNull(game.getMouseHandlerManager().getRouteSnapPoint());
	}

	@Test
	void collinearOverlappingAndAdjacentRoadsMergeIntoOneSegment() {
		RoadGraph previewGraph = new RoadGraph();
		previewGraph.createRoad(new Point(100, 100), new Point(200, 100));
		assertEquals(
			List.of(List.of(new Point(100, 100), new Point(250, 100))),
			roadEndpoints(
				previewGraph.getRoadSegmentsAfterAddingRoad(
					new Point(150, 100),
					new Point(250, 100)
				)
			)
		);

		RoadGraph roadGraph = new RoadGraph();
		roadGraph.createRoad(new Point(100, 100), new Point(200, 100));
		roadGraph.createRoad(new Point(150, 100), new Point(250, 100));
		roadGraph.createRoad(new Point(250, 100), new Point(300, 100));

		assertEquals(1, roadGraph.getRoads().size());
		assertEquals(new Point(100, 100), roadGraph.getRoads().getFirst().getStart());
		assertEquals(new Point(300, 100), roadGraph.getRoads().getFirst().getEnd());
	}

	@Test
	void fullyOverlappingRouteIsInvalidAndNotAdded() {
		RoadGraph roadGraph = new RoadGraph();
		roadGraph.createRoad(new Point(100, 100), new Point(200, 100));
		MouseHandler mouseHandler = new MouseHandler();
		mouseHandler.setRouteCreationEnabled(true);
		mouseHandler.onRoutePlacement(125, 100);
		mouseHandler.onMove(175, 100);
		MouseHandlerManager mouseHandlerManager = new MouseHandlerManager(
			roadGraph,
			mouseHandler,
			new VehicleManager(roadGraph),
			new FixedEntityManager(),
			point -> {}
		);

		assertFalse(mouseHandlerManager.canPlaceCurrentRoute());
		mouseHandler.onRelease(0, 175, 100);
		mouseHandlerManager.processPendingInput();

		assertEquals(1, roadGraph.getRoads().size());
	}

	@Test
	void lineIsCreatedOnlyWhenStationsAreConnected() {
		RoadGraph pathGraph = new RoadGraph();
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		LineManager lineManager = new LineManager(pathGraph);
		Station start = fixedEntityManager.createStation(new Point(0, 100));
		Station end = fixedEntityManager.createStation(new Point(200, 100));
		pathGraph.addStationNode(start.getPosition());
		pathGraph.addStationNode(end.getPosition());

		assertTrue(lineManager.createLine(start, end).isEmpty());
		assertTrue(lineManager.getLines().isEmpty());

		pathGraph.addRoad(new Road(new Point(0, 100), new Point(100, 100)));
		pathGraph.addRoad(new Road(new Point(100, 100), new Point(200, 100)));

		TransitLine line = lineManager.createLine(start, end).orElseThrow();
		assertEquals(1, line.getPath().getLegs().size());
		assertEquals(200.0, line.getPath().getLength(), 0.001);
	}

	@Test
	void lineCreationRejectsUnreachableStationAndKeepsTheLastReachableStation() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station first = game.getFixedEntityManager().createStation(new Point(100, 100));
		Station reachable = game.getFixedEntityManager().createStation(new Point(200, 100));
		Station unreachable = game.getFixedEntityManager().createStation(new Point(100, 300));
		game.getRoadGraph().createRoad(first.getPosition(), reachable.getPosition());
		game.getRoadGraph().addStationNode(first.getPosition());
		game.getRoadGraph().addStationNode(reachable.getPosition());
		game.getRoadGraph().addStationNode(unreachable.getPosition());
		game.update(0.0);
		Hud hud = game.getHud();
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));

		mouseHandler.onLineStationSelection(100, 100);
		game.update(0.0);
		mouseHandler.onMove(100, 100);
		mouseHandler.onLineStationSelection(100, 300);
		mouseHandler.onMove(100, 300);
		game.update(0.0);
		assertEquals("AUCUN CHEMIN VERS STATION", hud.getDialog().getTitle());
		assertEquals(List.of(first.getPosition()), game.getLinePreview().stations());
		assertTrue(hud.handleClick(640, 385, 1280, 720));

		mouseHandler.onLineStationSelection(200, 100);
		mouseHandler.onMove(200, 100);
		game.update(0.0);
		assertEquals(
			List.of(first.getPosition(), reachable.getPosition()),
			game.getLinePreview().stations()
		);
		assertTrue(hud.handleClick(700, 685, 1280, 720));

		assertEquals(
			List.of(first, reachable),
			game.getLineManager().getLines().get(0).getStations()
		);
	}

	@Test
	void lineStoresOrderedStationsAndAPathForEachSegment() {
		RoadGraph pathGraph = new RoadGraph();
		Station first = new Station(new Point(0, 100));
		Station middle = new Station(new Point(100, 100));
		Station last = new Station(new Point(200, 100));
		pathGraph.addStationNode(first.getPosition());
		pathGraph.addStationNode(middle.getPosition());
		pathGraph.addStationNode(last.getPosition());
		pathGraph.addRoad(new Road(first.getPosition(), middle.getPosition()));
		pathGraph.addRoad(new Road(middle.getPosition(), last.getPosition()));
		LineManager lineManager = new LineManager(pathGraph);

		TransitLine line = lineManager.createLine(List.of(first, middle, last)).orElseThrow();

		assertEquals(List.of(first, middle, last), line.getStations());
		assertEquals(2, line.getSegmentPaths().size());
		assertEquals(2, line.getPath().getLegs().size());
	}

	@Test
	void addingRoadRecalculatesEverySegmentOfAnExistingLine() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station first = game.getFixedEntityManager().createStation(new Point(100, 100));
		Station middle = game.getFixedEntityManager().createStation(new Point(200, 100));
		Station last = game.getFixedEntityManager().createStation(new Point(300, 100));
		RoadGraph roadGraph = game.getRoadGraph();
		roadGraph.createRoad(new Point(100, 100), new Point(100, 200));
		roadGraph.createRoad(new Point(100, 200), new Point(200, 100));
		roadGraph.createRoad(new Point(200, 100), new Point(300, 200));
		roadGraph.createRoad(new Point(300, 200), new Point(300, 100));
		roadGraph.addStationNode(first.getPosition());
		roadGraph.addStationNode(middle.getPosition());
		roadGraph.addStationNode(last.getPosition());
		game.update(0.0);
		TransitLine line = game
			.getLineManager()
			.createLine(List.of(first, middle, last))
			.orElseThrow();
		assertTrue(line.getSegmentPaths().get(0).getLength() > 100.0);
		assertTrue(line.getSegmentPaths().get(1).getLength() > 100.0);

		roadGraph.createRoad(first.getPosition(), last.getPosition());
		game.update(0.0);

		assertEquals(100.0, line.getSegmentPaths().get(0).getLength(), 0.001);
		assertEquals(100.0, line.getSegmentPaths().get(1).getLength(), 0.001);
	}

	@Test
	void assignedVehicleKeepsItsCurrentStationTargetAfterRoadChanges() {
		RoadGraph routeGraph = new RoadGraph();
		Station start = new Station(new Point(0, 100));
		Station next = new Station(new Point(200, 100));
		Station end = new Station(new Point(300, 100));
		Road firstRoad = routeGraph.createRoad(start.getPosition(), new Point(0, 200));
		routeGraph.createRoad(new Point(0, 200), new Point(200, 200));
		routeGraph.createRoad(new Point(200, 200), next.getPosition());
		routeGraph.createRoad(next.getPosition(), end.getPosition());
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(List.of(start, next, end)).orElseThrow();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			firstRoad,
			10,
			100.0,
			start.getPosition(),
			firstRoad.getEnd()
		);
		assertTrue(lineManager.assignVehicle(vehicle, line));

		vehicle.update(0.0);
		advanceVehicleUntil(vehicle, new Point(0, 200));

		routeGraph.createRoad(new Point(0, 200), next.getPosition());
		vehicle.update(0.0);

		assertEquals(next.getPosition(), vehicle.getTarget());
		advanceVehicleUntil(vehicle, next.getPosition());
	}

	@Test
	void lineReturnFindsShortestPathInsteadOfReversingTheOutboundSegments() {
		RoadGraph routeGraph = new RoadGraph();
		Station start = new Station(new Point(0, 0));
		Station middle = new Station(new Point(100, 0));
		Station end = new Station(new Point(200, 0));
		routeGraph.createRoad(new Point(0, 0), new Point(0, 100));
		routeGraph.createRoad(new Point(0, 100), middle.getPosition());
		routeGraph.createRoad(middle.getPosition(), new Point(200, 100));
		routeGraph.createRoad(new Point(200, 100), end.getPosition());
		routeGraph.createRoad(end.getPosition(), new Point(100, -50));
		routeGraph.createRoad(new Point(100, -50), start.getPosition());
		routeGraph.addStationNode(start.getPosition());
		routeGraph.addStationNode(middle.getPosition());
		routeGraph.addStationNode(end.getPosition());
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(List.of(start, middle, end)).orElseThrow();

		assertTrue(line.getReturnPath().getLength() < line.getPath().getLength());
		assertEquals(2, line.getReturnPath().getLegs().size());

		Road firstOutboundRoad = line.getSegmentPaths().getFirst().getLegs().getFirst().road();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			firstOutboundRoad,
			10,
			100.0,
			start.getPosition(),
			firstOutboundRoad.getEnd()
		);
		assertTrue(lineManager.assignVehicle(vehicle, line));
		vehicle.update(0.0);
		advanceVehicleUntil(vehicle, end.getPosition());
		vehicle.update(0.1);
		assertEquals(new Point(100, -50), vehicle.getTarget());

		routeGraph.createRoad(end.getPosition(), start.getPosition());
		lineManager.refreshPaths();
		assertEquals(200.0, line.getReturnPath().getLength(), 0.001);
	}

	@Test
	void assignedVehicleApproachesLineAndTravelsBackAndForth() {
		RoadGraph routeGraph = new RoadGraph();
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		Station startStation = fixedEntityManager.createStation(new Point(0, 100));
		Station endStation = fixedEntityManager.createStation(new Point(200, 200));
		routeGraph.addStationNode(startStation.getPosition());
		routeGraph.addStationNode(endStation.getPosition());
		Road firstRoad = routeGraph.createRoad(new Point(0, 100), new Point(100, 100));
		routeGraph.createRoad(new Point(100, 100), new Point(100, 200));
		routeGraph.createRoad(new Point(100, 200), new Point(200, 200));
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(startStation, endStation).orElseThrow();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			firstRoad,
			10,
			100.0,
			new Point(50, 100),
			firstRoad.getEnd()
		);

		assertTrue(lineManager.assignVehicle(vehicle, line));
		assertFalse(lineManager.assignVehicle(vehicle, line));
		vehicle.update(0.5);
		advanceVehicleUntil(vehicle, startStation.getPosition());
		advanceVehicleUntil(vehicle, endStation.getPosition());
		advanceVehicleUntil(vehicle, startStation.getPosition());
	}

	@Test
	void assignedVehicleVisitsEveryStationBeforeReturningFromTheEnd() {
		RoadGraph routeGraph = new RoadGraph();
		Station start = new Station(new Point(0, 100));
		Station middle = new Station(new Point(100, 100));
		Station end = new Station(new Point(200, 100));
		routeGraph.addStationNode(start.getPosition());
		routeGraph.addStationNode(middle.getPosition());
		routeGraph.addStationNode(end.getPosition());
		Road firstRoad = routeGraph.createRoad(start.getPosition(), middle.getPosition());
		routeGraph.createRoad(middle.getPosition(), end.getPosition());
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(List.of(start, middle, end)).orElseThrow();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			firstRoad,
			10,
			100.0,
			new Point(50, 100),
			firstRoad.getEnd()
		);
		assertTrue(lineManager.assignVehicle(vehicle, line));

		advanceVehicleUntil(vehicle, start.getPosition());
		advanceVehicleUntil(vehicle, middle.getPosition());
		advanceVehicleUntil(vehicle, end.getPosition());
		advanceVehicleUntil(vehicle, start.getPosition());
		advanceVehicleUntil(vehicle, middle.getPosition());
		advanceVehicleUntil(vehicle, end.getPosition());
	}

	@Test
	void assignedVehicleStartsAtDeclaredBeginningEvenWhenCloserToTheEnd() {
		RoadGraph routeGraph = new RoadGraph();
		Station start = new Station(new Point(0, 100));
		Station middle = new Station(new Point(100, 100));
		Station end = new Station(new Point(200, 100));
		routeGraph.addStationNode(start.getPosition());
		routeGraph.addStationNode(middle.getPosition());
		routeGraph.addStationNode(end.getPosition());
		routeGraph.createRoad(start.getPosition(), middle.getPosition());
		Road lastRoad = routeGraph.createRoad(middle.getPosition(), end.getPosition());
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(List.of(start, middle, end)).orElseThrow();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			lastRoad,
			10,
			100.0,
			new Point(180, 100),
			end.getPosition()
		);
		assertTrue(lineManager.assignVehicle(vehicle, line));

		advanceVehicleUntil(vehicle, start.getPosition());
		advanceVehicleUntil(vehicle, middle.getPosition());
		advanceVehicleUntil(vehicle, end.getPosition());
	}

	@Test
	void hudCanCreateLineAssignVehicleAndRunItBackAndForth() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station startStation = game.getFixedEntityManager().createStation(new Point(50, 375));
		Station endStation = game.getFixedEntityManager().createStation(new Point(125, 375));
		game.getRoadGraph().addStationNode(startStation.getPosition());
		game.getRoadGraph().addStationNode(endStation.getPosition());
		game.update(0.0);

		Hud hud = game.getHud();
		assertNull(hud.getOneShotActionButton());
		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));
		assertEquals("VALIDER", hud.getOneShotActionButton().getLabel());
		game.getMouseHandlerManager().getMouseHandler().onLineStationSelection(50, 375);
		game.update(0.0);
		game.getMouseHandlerManager().getMouseHandler().onLineStationSelection(125, 375);
		game.update(0.0);
		assertEquals(0, game.getLineManager().getLines().size());
		assertTrue(hud.handleClick(700, 685, 1280, 720));
		assertEquals(1, game.getLineManager().getLines().size());
		assertNull(hud.getOneShotActionButton());

		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 547, 1280, 720));
		assertNotNull(hud.getDialog());
		assertTrue(hud.handleClick(640, 364, 1280, 720));
		assertNotNull(hud.getDialog());
		assertTrue(hud.handleClick(640, 385, 1280, 720));
		assertEquals("AJOUTER DES VEHICULES", hud.getDialog().getTitle());
		assertTrue(hud.getDialog().getOptions().get(0).getLabel().contains("VEHICULE 1"));
		assertTrue(hud.handleClick(640, 322, 1280, 720));
		assertEquals("AJOUTER DES VEHICULES", hud.getDialog().getTitle());
		assertTrue(hud.getDialog().getOptions().get(0).getLabel().contains("VEHICULE 2"));
		assertTrue(hud.handleClick(640, 343, 1280, 720));
		assertEquals("AJOUTER DES VEHICULES", hud.getDialog().getTitle());

		Vehicle vehicle = game.getVehicleManager().getVehicles().get(0);
		Vehicle secondVehicle = game.getVehicleManager().getVehicles().get(1);
		assertSame(game.getLineManager().getLines().get(0), vehicle.getAssignedLine());
		assertSame(game.getLineManager().getLines().get(0), secondVehicle.getAssignedLine());
		assertTrue(hud.handleClick(640, 406, 1280, 720));
		assertEquals("LIGNE 1", hud.getDialog().getTitle());
		game.update(0.0);
		advanceGameVehicleUntil(game, vehicle, endStation.getPosition());
		advanceGameVehicleUntil(game, vehicle, startStation.getPosition());
	}

	@Test
	void hudValidatesLineAfterSelectingSeveralStations() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station start = game.getFixedEntityManager().createStation(new Point(50, 350));
		Station middle = game.getFixedEntityManager().createStation(new Point(100, 350));
		Station end = game.getFixedEntityManager().createStation(new Point(150, 350));
		game.getRoadGraph().createRoad(start.getPosition(), middle.getPosition());
		game.getRoadGraph().createRoad(middle.getPosition(), end.getPosition());
		game.getRoadGraph().addStationNode(start.getPosition());
		game.getRoadGraph().addStationNode(middle.getPosition());
		game.getRoadGraph().addStationNode(end.getPosition());
		game.update(0.0);

		Hud hud = game.getHud();
		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));
		MouseHandler mouseHandler = game.getMouseHandlerManager().getMouseHandler();
		mouseHandler.onLineStationSelection(50, 350);
		game.update(0.0);
		mouseHandler.onLineStationSelection(100, 350);
		game.update(0.0);
		mouseHandler.onLineStationSelection(150, 350);
		game.update(0.0);
		assertTrue(game.getLineManager().getLines().isEmpty());

		assertTrue(hud.handleClick(700, 685, 1280, 720));

		assertEquals(
			List.of(start, middle, end),
			game.getLineManager().getLines().get(0).getStations()
		);
		assertFalse(mouseHandler.isLineCreationEnabled());
	}

	@Test
	void vehicleListShowsStableNumbersAllowsRemovalAndLineColorChanges() {
		GameManager game = new GameManager(new java.util.Random(42));
		Station start = game.getFixedEntityManager().createStation(new Point(50, 350));
		Station end = game.getFixedEntityManager().createStation(new Point(150, 350));
		game.getRoadGraph().createRoad(start.getPosition(), end.getPosition());
		game.getRoadGraph().addStationNode(start.getPosition());
		game.getRoadGraph().addStationNode(end.getPosition());
		game.update(0.0);
		TransitLine line = game.getLineManager().createLine(start, end).orElseThrow();
		Vehicle vehicle = game.getVehicleManager().getVehicles().get(0);
		assertTrue(game.getLineManager().assignVehicle(vehicle, line));
		assertEquals(1, game.getVehicleManager().getVehicleNumber(vehicle));

		Hud hud = game.getHud();
		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 593, 1280, 720));
		assertTrue(hud.getDialog().getOptions().get(0).getLabel().contains("VEHICULE 1 | LIGNE 1"));
		assertTrue(hud.handleClick(640, 322, 1280, 720));
		assertEquals("VEHICULE 1 | LIGNE 1", hud.getDialog().getTitle());
		assertTrue(hud.handleClick(640, 364, 1280, 720));
		assertTrue(hud.getDialog().getTitle().contains("VEHICULE 1 LIBRE"));
		assertNull(vehicle.getAssignedLine());
		assertEquals(1, game.getVehicleManager().getVehicleNumber(vehicle));
		assertTrue(hud.handleClick(640, 385, 1280, 720));

		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 593, 1280, 720));
		assertTrue(hud.getDialog().getOptions().get(0).getLabel().contains("VEHICULE 1 | LIBRE"));
		assertTrue(hud.handleClick(640, 322, 1280, 720));

		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 547, 1280, 720));
		assertTrue(hud.handleClick(640, 364, 1280, 720));
		assertEquals("LIGNE 1", hud.getDialog().getTitle());
		assertTrue(hud.handleClick(640, 343, 1280, 720));
		assertEquals("COULEUR LIGNE 1", hud.getDialog().getTitle());
		assertTrue(hud.handleClick(640, 280, 1280, 720));

		assertEquals(org.jeuroute.model.world.enums.LineColor.CORAL, line.getColor());
	}

	@Test
	void vehicleWithoutReachableStationDoesNotMove() {
		Road road = graph.createRoad(new Point(0, 75), new Point(200, 75));
		Vehicle vehicle = new Vehicle(graph, road, 10, 2.0, road.getStart(), road.getEnd());
		Point initialPosition = new Point(vehicle.getPosition());

		vehicle.update(10.0);

		assertEquals(initialPosition, vehicle.getPosition());
		assertNotNull(vehicle.getRoadPosition());
	}

	@Test
	void vehicleAccelerationFallsWithSpeedAndBrakesAtTheLineEnd() {
		RoadGraph routeGraph = new RoadGraph();
		Station start = new Station(new Point(0, 100));
		Station end = new Station(new Point(100, 100));
		Road road = routeGraph.createRoad(start.getPosition(), end.getPosition());
		LineManager lineManager = new LineManager(routeGraph);
		TransitLine line = lineManager.createLine(List.of(start, end)).orElseThrow();
		Vehicle vehicle = new Vehicle(
			routeGraph,
			road,
			10,
			50.0,
			5_000.0,
			start.getPosition(),
			end.getPosition()
		);
		assertTrue(lineManager.assignVehicle(vehicle, line));

		vehicle.update(0.0);
		vehicle.update(0.1);
		double firstSpeed = vehicle.getSpeed();
		vehicle.update(0.1);
		double secondSpeed = vehicle.getSpeed();

		assertTrue(firstSpeed > 0.0);
		assertTrue(secondSpeed > firstSpeed);
		assertTrue(secondSpeed - firstSpeed < firstSpeed);
		assertTrue(vehicle.getSpeed() <= vehicle.getMaxSpeed());
		assertEquals(5_000.0, vehicle.getPowerWatts());

		for (
			int frame = 0;
			frame < 100 && !end.getPosition().equals(vehicle.getPosition());
			frame++
		) {
			vehicle.update(0.1);
		}

		assertEquals(end.getPosition(), vehicle.getPosition());
		assertEquals(0.0, vehicle.getSpeed());
	}

	@Test
	void unassignedVehicleParksAtNearestStationByRoadDistance() {
		RoadGraph parkingGraph = new RoadGraph();
		Road road = parkingGraph.createRoad(new Point(0, 100), new Point(300, 100));
		parkingGraph.addStationNode(new Point(100, 100));
		parkingGraph.addStationNode(new Point(300, 100));
		Vehicle vehicle = new Vehicle(
			parkingGraph,
			road,
			10,
			100.0,
			road.getStart(),
			road.getEnd()
		);

		vehicle.update(1.0);
		vehicle.update(1.0);

		assertEquals(new Point(100, 100), vehicle.getPosition());
		assertTrue(vehicle.isParkedAtStation());
	}

	@Test
	void crossingRoadsAreSplitIntoFourSegments() {
		graph.addRoad(new Road(new Point(0, 75), new Point(200, 75)));
		graph.addRoad(new Road(new Point(75, 0), new Point(75, 200)));

		assertEquals(4, graph.getRoads().size());
		assertEquals(List.of(new Point(75, 75)), graph.getIntersectionPositions());
		assertEquals(4, graph.getConnectedRoadsAt(new Point(75, 75)).size());
	}

	@Test
	void intersectionChoosesTheFirstRoadToTheRight() {
		Road horizontal = new Road(new Point(0, 75), new Point(200, 75));
		Road vertical = new Road(new Point(75, 0), new Point(75, 200));
		graph.addRoad(horizontal);
		graph.addRoad(vertical);

		Road rightRoad = graph.firstRightRoadAt(new Point(75, 75), new Point(0, 75));

		assertEquals(new Point(75, 200), rightRoad.otherEndpoint(new Point(75, 75)));
	}

	@Test
	void intersectionChoosesFirstExitWhenRotatingRight() {
		graph.addRoad(new Road(new Point(0, 100), new Point(200, 100)));
		graph.addRoad(new Road(new Point(100, 0), new Point(100, 200)));
		graph.addRoad(new Road(new Point(100, 100), new Point(200, 200)));

		Road selected = graph.firstRightRoadAt(new Point(100, 100), new Point(0, 100));

		assertEquals(new Point(100, 200), selected.otherEndpoint(new Point(100, 100)));
	}

	@Test
	void intersectionChoosesOneDegreeRightBeforeNinetyDegrees() {
		Intersection intersection = new Intersection(new Point(0, 0));
		Road incoming = Road.createSegment(new Point(-100, 0), new Point(0, 0));
		Road oneDegreeRight = Road.createSegment(new Point(0, 0), new Point(-100, 2));
		Road ninetyDegreesRight = Road.createSegment(new Point(0, 0), new Point(0, 100));
		Road straight = Road.createSegment(new Point(0, 0), new Point(100, 0));
		intersection.setRoads(
			java.util.List.of(incoming, oneDegreeRight, ninetyDegreesRight, straight)
		);

		Road selected = intersection.firstRight(incoming, new Point(-100, 0));

		assertSame(oneDegreeRight, selected);
	}

	@Test
	void vehicleTakesTheFirstRightRoadAtAnIntersection() {
		graph.addRoad(new Road(new Point(0, 75), new Point(200, 75)));
		graph.addRoad(new Road(new Point(75, 0), new Point(75, 200)));
		graph.addStationNode(new Point(75, 200));

		Road incoming = graph
			.getRoads()
			.stream()
			.filter(road -> road.getStart().equals(new Point(0, 75)))
			.findFirst()
			.orElseThrow();
		Vehicle vehicle = new Vehicle(
			graph,
			incoming,
			10,
			100.0,
			incoming.getStart(),
			incoming.getEnd()
		);

		vehicle.update(1.12);

		assertEquals(new Point(75, 110), vehicle.getPosition());
		assertEquals(new Point(75, 200), vehicle.getTarget());
	}

	@Test
	void vehicleCreatedBeforeCrossingRoadIsReattachedToNewSegment() {
		Road initialRoad = graph.createRoad(new Point(0, 100), new Point(200, 100));
		graph.addStationNode(new Point(200, 100));
		Vehicle vehicle = new Vehicle(
			graph,
			initialRoad,
			10,
			100.0,
			initialRoad.getStart(),
			initialRoad.getEnd()
		);

		graph.addRoad(new Road(new Point(100, 0), new Point(100, 200)));

		vehicle.update(1.12);

		assertEquals(new Point(110, 100), vehicle.getPosition());
		assertEquals(new Point(200, 100), vehicle.getTarget());
	}

	@Test
	void independentRoadGraphUpdateResynchronizesItsVehicle() {
		RoadGraph graph = new RoadGraph();
		RoadGraph unrelatedGraph = new RoadGraph();
		Road initialRoad = graph.createRoad(new Point(0, 100), new Point(200, 100));
		graph.addStationNode(new Point(200, 100));
		Vehicle vehicle = new Vehicle(
			graph,
			initialRoad,
			10,
			100.0,
			initialRoad.getStart(),
			initialRoad.getEnd()
		);
		long initialVersion = graph.getVersion();

		graph.addRoad(new Road(new Point(100, 0), new Point(100, 200)));

		assertTrue(graph.getVersion() > initialVersion);
		assertEquals(0, unrelatedGraph.getVersion());
		vehicle.update(1.12);

		assertEquals(new Point(110, 100), vehicle.getPosition());
		assertEquals(new Point(200, 100), vehicle.getTarget());
	}

	@Test
	void vehicleKeepsMovingAfterIntersectionIsAddedToDiagonalRoad() {
		RoadGraph graph = new RoadGraph();
		Road diagonalRoad = graph.createRoad(new Point(0, 0), new Point(400, 250));
		graph.addStationNode(new Point(400, 250));
		Vehicle vehicle = new Vehicle(
			graph,
			diagonalRoad,
			10,
			100.0,
			diagonalRoad.getStart(),
			diagonalRoad.getEnd()
		);
		vehicle.update(1.0);
		Point positionBeforeRoadChange = new Point(vehicle.getPosition());

		graph.addRoad(new Road(new Point(200, 0), new Point(200, 250)));
		vehicle.update(0.15);

		assertNotNull(vehicle.getRoadPosition());
		assertNotEquals(positionBeforeRoadChange, vehicle.getPosition());
	}

	@Test
	void vehicleReattachesToRoadShiftedByIntersectionGridSnap() {
		Road diagonalRoad = graph.createRoad(new Point(0, 0), new Point(400, 250));
		graph.addStationNode(new Point(400, 250));
		Vehicle vehicle = new Vehicle(
			graph,
			diagonalRoad,
			10,
			100.0,
			diagonalRoad.getStart(),
			diagonalRoad.getEnd()
		);
		vehicle.update(1.65);
		Point positionBeforeRoadChange = new Point(vehicle.getPosition());

		graph.addRoad(new Road(new Point(150, 0), new Point(150, 200)));
		assertEquals(new Point(150, 100), graph.getIntersectionPositions().get(0));
		assertFalse(
			graph
				.getRoads()
				.stream()
				.anyMatch(road -> road.containsPoint(positionBeforeRoadChange))
		);
		vehicle.update(0.1);

		assertNotNull(vehicle.getRoadPosition());
		assertNotEquals(positionBeforeRoadChange, vehicle.getPosition());
		assertTrue(vehicle.getRoad().containsPoint(vehicle.getPosition()));
	}

	@Test
	void vehicleAtNewIntersectionResynchronizesInItsOriginalDirection() {
		Road initialRoad = graph.createRoad(new Point(0, 100), new Point(200, 100));
		graph.addStationNode(new Point(200, 100));
		Vehicle vehicle = new Vehicle(
			graph,
			initialRoad,
			10,
			100.0,
			initialRoad.getStart(),
			initialRoad.getEnd()
		);
		vehicle.update(1.0);

		graph.addRoad(new Road(new Point(100, 0), new Point(100, 200)));
		vehicle.update(0.12);

		assertEquals(new Point(110, 100), vehicle.getPosition());
	}

	@Test
	void vehiclesContinueWhenOneNewRoadCrossesBothInitialRoads() {
		Road firstRoad = graph.createRoad(new Point(50, 360), new Point(1230, 360));
		Road secondRoad = graph.createRoad(new Point(50, 520), new Point(1230, 520));
		graph.addStationNode(new Point(1230, 360));
		graph.addStationNode(new Point(1230, 520));
		Vehicle firstVehicle = new Vehicle(
			graph,
			firstRoad,
			24,
			180.0,
			firstRoad.getStart(),
			firstRoad.getEnd()
		);
		Vehicle secondVehicle = new Vehicle(
			graph,
			secondRoad,
			30,
			240.0,
			secondRoad.getStart(),
			secondRoad.getEnd()
		);

		graph.addRoad(new Road(new Point(640, 0), new Point(640, 720)));

		for (int frame = 0; frame < 100; frame++) {
			firstVehicle.update(0.1);
			secondVehicle.update(0.1);
		}

		assertTrue(firstVehicle.getPosition().x > 640 || firstVehicle.getPosition().y != 360);
		assertTrue(secondVehicle.getPosition().x > 640 || secondVehicle.getPosition().y != 520);
	}

	@Test
	void diagonalCrossingUsesTheSameGridPointForRoadsAndIntersection() {
		Road initialRoad = graph.createRoad(new Point(0, 100), new Point(400, 100));
		graph.addStationNode(new Point(400, 100));
		Vehicle vehicle = new Vehicle(
			graph,
			initialRoad,
			10,
			100.0,
			initialRoad.getStart(),
			initialRoad.getEnd()
		);
		graph.addRoad(new Road(new Point(0, 0), new Point(400, 300)));

		assertEquals(new Point(125, 100), graph.getIntersectionPositions().get(0));

		vehicle.update(1.5);

		assertNotEquals(new Point(125, 100), vehicle.getPosition());
		assertNotEquals(initialRoad, vehicle.getRoad());
	}

	@Test
	void sameIntersectionPositionIsStoredOnlyOnce() {
		FixedEntityManager fixedEntityManager = new FixedEntityManager();
		RoadGraph localGraph = new RoadGraph();
		localGraph.addRoad(new Road(new Point(0, 75), new Point(200, 75)));
		localGraph.addRoad(new Road(new Point(75, 0), new Point(75, 200)));
		fixedEntityManager.synchronizeIntersections(
			localGraph.getIntersectionPositions(),
			localGraph
		);
		Intersection firstIntersection = fixedEntityManager.getIntersectionAt(new Point(75, 75));

		localGraph.addRoad(new Road(new Point(75, 0), new Point(75, 200)));
		fixedEntityManager.synchronizeIntersections(
			localGraph.getIntersectionPositions(),
			localGraph
		);

		assertEquals(
			1,
			fixedEntityManager
				.getIntersections()
				.stream()
				.filter(intersection -> intersection.getPosition().equals(new Point(75, 75)))
				.count()
		);
		assertSame(firstIntersection, fixedEntityManager.getIntersectionAt(new Point(75, 75)));
	}
}
