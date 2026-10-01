package org.jeuroute.model.records.hud;

import java.util.List;

public record HudDialogPlacement(
	HudBounds panel,
	List<HudBounds> options,
	HudBounds previous,
	HudBounds next
) {}
