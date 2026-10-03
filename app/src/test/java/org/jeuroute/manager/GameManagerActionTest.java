package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.Random;
import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.hud.Hud;
import org.jeuroute.gamecore.hud.presentation.HudLayout;
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
		gameManager.update(0.0);

		assertEquals(initialPersonCount + 1, gameManager.getPersonManager().getPeople().size());
		var addedPerson = gameManager.getPersonManager().getPeople().getLast();
		assertNotNull(addedPerson.getDestinationHouse());
		assertTrue(gameManager.getWorldMap().getTerrainMap().isLand(addedPerson.getPosition()));
		assertEquals(
			Integer.toString(initialPersonCount + 1),
			gameManager.getHudManager().getIndicator("people").getValue()
		);
	}

	@Test
	void debugModeCanBeToggledFromTheDefaultMenu() {
		GameManager gameManager = new GameManager(new Random(42));
		Hud hud = gameManager.getHud();

		var menuBounds = HudLayout.rootButtonBounds(0, 1280, 720);
		assertTrue(hud.handleClick(menuBounds.x() + 10, menuBounds.y() + 10, 1280, 720));
		var debugButtonBounds = HudLayout.submenuButtonBounds(0, 4, 1280, 720);
		assertTrue(
			hud.handleClick(debugButtonBounds.x() + 10, debugButtonBounds.y() + 10, 1280, 720)
		);

		assertTrue(gameManager.isDebugModeEnabled());
		gameManager.getActionHandlers().get("toggle-debug").run();
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
	void simulationTickCountIsIndependentOfFrameTimeSlicing() {
		GameManager singleFrameGame = new GameManager(new Random(42));
		GameManager slicedFrameGame = new GameManager(new Random(42));

		singleFrameGame.update(1.0);
		for (int frame = 0; frame < 8; frame++) {
			slicedFrameGame.update(0.125);
		}

		assertEquals(60, singleFrameGame.getCurrentTickNumber());
		assertEquals(
			singleFrameGame.getCurrentTickNumber(),
			slicedFrameGame.getCurrentTickNumber()
		);
	}
}
