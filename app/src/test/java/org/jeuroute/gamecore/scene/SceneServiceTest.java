package org.jeuroute.gamecore.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SceneServiceTest {

	@Test
	void requestedSceneActivatesOnUpdateAndClosesPreviousScene() {
		SceneService sceneService = new SceneService();
		List<String> lifecycle = new ArrayList<>();
		RecordingScene menu = new RecordingScene("menu", lifecycle);
		RecordingScene game = new RecordingScene("game", lifecycle);

		sceneService.start(() -> menu);
		sceneService.request(() -> game);

		assertEquals(List.of("menu:enter"), lifecycle);

		sceneService.updateScene(16_000_000L);
		sceneService.close();

		assertEquals(
			List.of("menu:enter", "menu:exit", "game:enter", "game:update", "game:exit"),
			lifecycle
		);
	}

	private static final class RecordingScene implements Scene {

		private final String name;
		private final List<String> lifecycle;

		private RecordingScene(String name, List<String> lifecycle) {
			this.name = name;
			this.lifecycle = lifecycle;
		}

		@Override
		public void enter() {
			lifecycle.add(name + ":enter");
		}

		@Override
		public void update(long elapsedNanoseconds) {
			lifecycle.add(name + ":update");
		}

		@Override
		public void render(org.jeuroute.model.records.window.WindowMetrics metrics) {}

		@Override
		public void exit() {
			lifecycle.add(name + ":exit");
		}
	}
}
