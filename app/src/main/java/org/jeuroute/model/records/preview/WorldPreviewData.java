package org.jeuroute.model.records.preview;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import org.jeuroute.model.world.network.Road;

public record WorldPreviewData(
	Point routeSnapPoint,
	RoutePreview route,
	List<Road> stationRoadSegments,
	PlacementPreview placement,
	LinePreview line
) {
	public WorldPreviewData {
		routeSnapPoint = routeSnapPoint == null ? null : new Point(routeSnapPoint);
		stationRoadSegments = List.copyOf(
			Objects.requireNonNull(stationRoadSegments, "stationRoadSegments cannot be null")
		);
	}

	@Override
	public Point routeSnapPoint() {
		return routeSnapPoint == null ? null : new Point(routeSnapPoint);
	}
}
