package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jeuroute.gamecore.MouseHandler;
import org.jeuroute.gamecore.hud.Hud;
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
}
