package org.jeuroute.gamecore.rendering.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.jeuroute.gamecore.rendering.debug.DebugRouteLineMerger.Line;
import org.junit.jupiter.api.Test;

class DebugRouteLineMergerTest {

	@Test
	void mergesOverlappingLinesInTheSameDirectionOnly() {
		List<Line> merged = DebugRouteLineMerger.merge(
			List.of(
				new Line(0.0, 0.0, 8.0, 0.0),
				new Line(5.0, 0.0, 12.0, 0.0),
				new Line(12.0, 0.0, 4.0, 0.0),
				new Line(20.0, 0.0, 22.0, 0.0),
				new Line(0.0, 2.0, 8.0, 2.0)
			)
		);

		assertEquals(4, merged.size());
		assertLine(merged.get(0), 0.0, 0.0, 12.0, 0.0);
		assertLine(merged.get(1), 20.0, 0.0, 22.0, 0.0);
		assertLine(merged.get(2), 12.0, 0.0, 4.0, 0.0);
		assertLine(merged.get(3), 0.0, 2.0, 8.0, 2.0);
	}

	@Test
	void mergesOverlappingDiagonalLines() {
		List<Line> merged = DebugRouteLineMerger.merge(
			List.of(new Line(0.0, 0.0, 4.0, 4.0), new Line(2.0, 2.0, 8.0, 8.0))
		);

		assertEquals(1, merged.size());
		assertLine(merged.getFirst(), 0.0, 0.0, 8.0, 8.0);
	}

	@Test
	void incrementallyRemovesOneOverlappingLineAndReusesUnchangedSnapshot() {
		DebugRouteLineMerger.Index index = new DebugRouteLineMerger.Index();
		DebugRouteLineMerger.IndexedLine first = index.add(new Line(0.0, 0.0, 8.0, 0.0));
		index.add(new Line(5.0, 0.0, 12.0, 0.0));
		List<Line> mergedBeforeRemoval = index.mergedLines();

		assertLine(mergedBeforeRemoval.getFirst(), 0.0, 0.0, 12.0, 0.0);
		assertSame(mergedBeforeRemoval, index.mergedLines());

		index.remove(first);

		assertLine(index.mergedLines().getFirst(), 5.0, 0.0, 12.0, 0.0);
	}

	private static void assertLine(
		Line line,
		double startX,
		double startY,
		double endX,
		double endY
	) {
		assertEquals(startX, line.startX(), 1.0e-9);
		assertEquals(startY, line.startY(), 1.0e-9);
		assertEquals(endX, line.endX(), 1.0e-9);
		assertEquals(endY, line.endY(), 1.0e-9);
	}
}
