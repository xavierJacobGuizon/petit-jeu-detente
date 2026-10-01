package org.jeuroute.model.world.terrain.generation;

import java.util.Random;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.terrain.TerrainMap;

public interface TerrainGenerator {
	TerrainType type();

	TerrainMap generate(Random random);
}
