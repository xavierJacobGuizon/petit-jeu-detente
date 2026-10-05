package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.presentation.DebugProfilerWindowContent;
import org.jeuroute.gamecore.hud.presentation.HudLayout;
import org.jeuroute.model.records.hud.HudBounds;
import org.junit.jupiter.api.Test;

class GameManagerActionTest {

	@Test
	void cancellingLineCreationClearsModeAndValidationButton() {
		GameManager gameManager = new GameManager(new java.util.Random(42));
		Hud hud = gameManager.getHud();
		assertTrue(hud.handleClick(1020, 685, 1280, 720));
		assertTrue(hud.handleClick(1020, 639, 1280, 720));
		assertTrue(gameManager.getMouseHandlerManager().getMouseHandler().isLineCreationEnabled());
		assertTrue(hud.getOneShotActionButton() != null);

		gameManager.cancelActiveAction();

		MouseHandler mouseHandler = gameManager.getMouseHandlerManager().getMouseHandler();
		assertFalse(mouseHandler.isLineCreationEnabled());
		assertNull(hud.getOneShotActionButton());
		assertNull(gameManager.getLinePreview());
	}

	@Test
	void cancellingPlacementClearsPendingVehicleClick() {
		GameManager gameManager = new GameManager(new java.util.Random(42));
		MouseHandler mouseHandler = gameManager.getMouseHandlerManager().getMouseHandler();
		mouseHandler.setVehicleCreationEnabled(true);
		mouseHandler.onVehiclePlacement(250, 200);

		gameManager.cancelActiveAction();

		assertFalse(mouseHandler.isVehicleCreationEnabled());
		assertNull(mouseHandler.consumeVehiclePlacement());
	}

	@Test
	void personCanBePlacedFromTheBuildMenuAndGetsAHouseDestination() {
		GameManager gameManager = new GameManager(new Random(42));
		Hud hud = gameManager.getHud();
		MouseHandler mouseHandler = gameManager.getMouseHandlerManager().getMouseHandler();
		int initialPersonCount = gameManager.getPersonManager().getPeople().size();

		var menuBounds = HudLayout.rootButtonBounds(0, 1280, 720);
		assertTrue(hud.handleClick(menuBounds.x() + 10, menuBounds.y() + 10, 1280, 720));
		var personButtonBounds = HudLayout.submenuButtonBounds(0, 3, 1280, 720);
		assertTrue(
			hud.handleClick(personButtonBounds.x() + 10, personButtonBounds.y() + 10, 1280, 720)
		);
		assertTrue(mouseHandler.isPersonCreationEnabled());

		Point dropPosition = new Point(650, 375);
		mouseHandler.onPersonPlacement(dropPosition.x, dropPosition.y);
		gameManager.advanceFrame(0L);

		assertEquals(initialPersonCount + 1, gameManager.getPersonManager().getPeople().size());
		var addedPerson = gameManager.getPersonManager().getPeople().getLast();
		for (int tick = 0; tick < 10_000 && addedPerson.isAwaitingInitialJourney(); tick++) {
			gameManager.advanceFrame(16_666_667L);
		}
		assertFalse(addedPerson.isAwaitingInitialJourney());
		assertNotNull(addedPerson.getDestinationHouse());
		assertTrue(gameManager.getWorldMap().getTerrainMap().isLand(addedPerson.getPosition()));
		assertEquals(
			Integer.toString(initialPersonCount + 1),
			gameManager.getHudManager().getIndicator("people").getValue()
		);
	}

	@Test
	void debugWindowActionInvokesTheRegisteredWindowHandler() {
		GameManager gameManager = new GameManager(new Random(42));
		int[] invocationCount = { 0 };
		gameManager.setDebugWindowCreationHandler(() -> invocationCount[0]++);

		gameManager.getActionHandlers().get("create-debug-window").run();

		assertEquals(1, invocationCount[0]);
		assertFalse(gameManager.isDebugModeEnabled());
	}

	@Test
	void debugMouseCoordinatesAreAvailableOnlyWhileDebugModeIsEnabled() {
		GameManager gameManager = new GameManager(new Random(42));
		MouseHandler mouseHandler = gameManager.getMouseHandlerManager().getMouseHandler();
		mouseHandler.onMove(128.2, -45.7);

		assertNull(gameManager.getDebugMousePosition());
		gameManager.getActionHandlers().get("toggle-debug").run();
		assertEquals(new Point(128, -46), gameManager.getDebugMousePosition());
		gameManager.getActionHandlers().get("toggle-debug").run();
		assertNull(gameManager.getDebugMousePosition());
	}

	@Test
	void personRouteDisplayToggleIsIndependentFromDebugMode() {
		GameManager gameManager = new GameManager(new Random(42));

		gameManager.getActionHandlers().get("toggle-person-routes").run();

		assertTrue(gameManager.isPersonRouteDisplayEnabled());
		assertFalse(gameManager.isDebugModeEnabled());

		gameManager.getActionHandlers().get("toggle-debug").run();

		assertTrue(gameManager.isDebugModeEnabled());
		assertTrue(gameManager.isPersonRouteDisplayEnabled());
	}

	@Test
	void debugWindowButtonsToggleMouseAndPersonRoutesIndependently() {
		boolean[] debugEnabled = { false };
		boolean[] personRoutesEnabled = { false };
		DebugProfilerWindowContent content = new DebugProfilerWindowContent(
			new org.jeuroute.gamecore.PerformanceProfiler(),
			() -> new Point(0, 0),
			() -> debugEnabled[0],
			() -> debugEnabled[0] = !debugEnabled[0],
			() -> personRoutesEnabled[0],
			() -> personRoutesEnabled[0] = !personRoutesEnabled[0]
		);
		HudBounds contentBounds = new HudBounds(20, 30, 600, 200);

		assertTrue(content.handleClick(contentBounds, 30, 40));
		assertTrue(debugEnabled[0]);
		assertFalse(personRoutesEnabled[0]);

		assertTrue(content.handleClick(contentBounds, 190, 40));
		assertTrue(debugEnabled[0]);
		assertTrue(personRoutesEnabled[0]);
	}

	@Test
	void performanceSampleHandlerReceivesEachCompletedSimulationTick() {
		GameManager gameManager = new GameManager(new Random(42));
		List<Long> sampledTicks = new ArrayList<>();
		gameManager.setPerformanceSampleHandler(sampledTicks::add);

		gameManager.advanceFrame(50_000_000L);

		assertEquals(List.of(1L, 2L, 3L), sampledTicks);
	}

	@Test
	void simulationTickCountIsIndependentOfFrameTimeSlicing() {
		GameManager singleFrameGame = new GameManager(new Random(42));
		GameManager slicedFrameGame = new GameManager(new Random(42));

		singleFrameGame.advanceFrame(1_000_000_000L);
		for (int frame = 0; frame < 8; frame++) {
			slicedFrameGame.advanceFrame(125_000_000L);
		}

		assertEquals(60, singleFrameGame.getCurrentTickNumber());
		assertEquals(
			singleFrameGame.getCurrentTickNumber(),
			slicedFrameGame.getCurrentTickNumber()
		);
	}
}
