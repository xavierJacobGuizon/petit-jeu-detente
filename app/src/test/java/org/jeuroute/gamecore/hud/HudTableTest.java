package org.jeuroute.gamecore.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class HudTableTest {

	@Test
	void storesRowsMatchingItsColumnsAndTracksChanges() {
		HudTable table = new HudTable(
			List.of(
				new HudTable.Column("NAME", 160, HudTable.Alignment.LEFT),
				new HudTable.Column("TIME", 100, HudTable.Alignment.RIGHT)
			)
		);

		table.setRows(List.of(HudTable.Row.of("FRAME", "16.7")));

		assertEquals(260, table.width());
		assertEquals(1, table.version());
		assertEquals("16.7", table.rows().getFirst().cells().get(1));
		assertThrows(IllegalArgumentException.class, () ->
			table.setRows(List.of(HudTable.Row.of("INVALID")))
		);
	}
}
