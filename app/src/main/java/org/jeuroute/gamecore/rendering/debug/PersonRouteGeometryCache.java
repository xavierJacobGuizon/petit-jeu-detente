package org.jeuroute.gamecore.rendering.debug;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jeuroute.model.records.camera.WorldViewBounds;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonRoute;

final class PersonRouteGeometryCache {

	private static final double LINE_PADDING = 0.75;

	private final Map<Person, CachedRoute> routesByPerson = new IdentityHashMap<>();
	private final SpatialRouteLineIndex routeIndex = new SpatialRouteLineIndex();
	private final List<DebugRouteLineMerger.Line> visibleLines = new ArrayList<>();
	private final List<DebugRouteLineMerger.Line> movingHeadLines = new ArrayList<>();
	private int rawSegmentCount;
	private int visibleSegmentCount;
	private int changedRouteCount;
	private int segmentTransitionCount;
	private int mergedLineCount;

	List<DebugRouteLineMerger.Line> collectVisibleLines(
		Collection<Person> people,
		WorldViewBounds viewBounds
	) {
		visibleLines.clear();
		movingHeadLines.clear();
		rawSegmentCount = 0;
		visibleSegmentCount = 0;
		changedRouteCount = 0;
		segmentTransitionCount = 0;
		for (Person person : people) {
			if (person == null || !person.isWalking()) {
				continue;
			}
			CachedRoute route = routesByPerson.computeIfAbsent(person, ignored ->
				new CachedRoute()
			);
			if (route.refreshPlanIfNeeded(person)) {
				changedRouteCount++;
			}
			int advancedSegments = route.advanceToSegment(person, routeIndex);
			segmentTransitionCount += advancedSegments;
			rawSegmentCount += route.remainingSegmentCount();
			DebugRouteLineMerger.Line headLine = route.updateHead(person, viewBounds);
			if (headLine != null) {
				visibleSegmentCount++;
				movingHeadLines.add(headLine);
			}
		}
		removeInactiveRoutes();
		SpatialRouteLineIndex.Query staticLines = routeIndex.query(viewBounds, LINE_PADDING);
		visibleSegmentCount += staticLines.visibleSegments();
		visibleLines.addAll(staticLines.lines());
		visibleLines.addAll(DebugRouteLineMerger.merge(movingHeadLines));
		mergedLineCount = visibleLines.size();
		return List.copyOf(visibleLines);
	}

	Diagnostics diagnostics() {
		return new Diagnostics(
			rawSegmentCount,
			visibleSegmentCount,
			mergedLineCount,
			changedRouteCount,
			segmentTransitionCount
		);
	}

	int rawSegmentCount() {
		return rawSegmentCount;
	}

	int visibleSegmentCount() {
		return visibleSegmentCount;
	}

	int mergedLineCount() {
		return mergedLineCount;
	}

	int changedRouteCount() {
		return changedRouteCount;
	}

	int staticMergeBuildCount() {
		return routeIndex.mergeBuildCount();
	}

	void clear() {
		clearRouteGeometry();
		visibleLines.clear();
		movingHeadLines.clear();
	}

	private void clearRouteGeometry() {
		routeIndex.clear();
		routesByPerson.clear();
		visibleLines.clear();
	}

	private void removeInactiveRoutes() {
		Iterator<Map.Entry<Person, CachedRoute>> routes = routesByPerson.entrySet().iterator();
		while (routes.hasNext()) {
			Map.Entry<Person, CachedRoute> entry = routes.next();
			if (!entry.getKey().isWalking()) {
				entry.getValue().removeFrom(routeIndex);
				routes.remove();
				changedRouteCount++;
			}
		}
	}

	private final class CachedRoute {

		private long routePlanVersion = Long.MIN_VALUE;
		private int currentSegmentIndex = -1;
		private int remainingSegmentCount;
		private double headTargetX;
		private double headTargetY;
		private final List<CachedTailSegment> tailSegments = new ArrayList<>();
		private int nextTailSegmentIndex;

		private boolean refreshPlanIfNeeded(Person person) {
			long currentPlanVersion = person.getRoutePlanVersion();
			if (routePlanVersion == currentPlanVersion) {
				return false;
			}

			List<PersonRoute.Segment> segments = person.getRemainingRouteSegments();
			removeFrom(routeIndex);
			remainingSegmentCount = segments.size();
			if (!segments.isEmpty()) {
				currentSegmentIndex = person.getCurrentSegmentIndex();
				Point headTarget = segments.getFirst().target();
				headTargetX = headTarget.x;
				headTargetY = headTarget.y;
				for (int index = 1; index < segments.size(); index++) {
					PersonRoute.Segment segment = segments.get(index);
					Point start = segment.start();
					Point end = segment.target();
					SpatialRouteLineIndex.IndexedLine indexedLine = routeIndex.add(
						new DebugRouteLineMerger.Line(start.x, start.y, end.x, end.y)
					);
					tailSegments.add(new CachedTailSegment(end.x, end.y, indexedLine));
				}
			}
			routePlanVersion = currentPlanVersion;
			return true;
		}

		private int remainingSegmentCount() {
			return remainingSegmentCount;
		}

		private int advanceToSegment(Person person, SpatialRouteLineIndex index) {
			int nextSegmentIndex = person.getCurrentSegmentIndex();
			int advancedSegments = 0;
			while (currentSegmentIndex < nextSegmentIndex) {
				if (nextTailSegmentIndex >= tailSegments.size()) {
					currentSegmentIndex = nextSegmentIndex;
					remainingSegmentCount = 0;
					return advancedSegments;
				}
				CachedTailSegment promotedSegment = tailSegments.get(nextTailSegmentIndex++);
				if (promotedSegment.indexedLine() != null) {
					index.remove(promotedSegment.indexedLine());
				}
				headTargetX = promotedSegment.targetX();
				headTargetY = promotedSegment.targetY();
				currentSegmentIndex++;
				advancedSegments++;
			}
			remainingSegmentCount = Math.max(0, remainingSegmentCount - advancedSegments);
			return advancedSegments;
		}

		private DebugRouteLineMerger.Line updateHead(Person person, WorldViewBounds viewBounds) {
			double startX = person.getPreciseX();
			double startY = person.getPreciseY();
			boolean visible = viewBounds.intersectsSegment(
				startX,
				startY,
				headTargetX,
				headTargetY,
				LINE_PADDING
			);
			return visible
				? new DebugRouteLineMerger.Line(startX, startY, headTargetX, headTargetY)
				: null;
		}

		private void removeFrom(SpatialRouteLineIndex index) {
			for (CachedTailSegment segment : tailSegments) {
				index.remove(segment.indexedLine());
			}
			tailSegments.clear();
			nextTailSegmentIndex = 0;
			currentSegmentIndex = -1;
		}
	}

	private record CachedTailSegment(
		double targetX,
		double targetY,
		SpatialRouteLineIndex.IndexedLine indexedLine
	) {}

	public record Diagnostics(
		int rawSegments,
		int visibleSegments,
		int mergedLines,
		int changedRoutes,
		int segmentTransitions
	) {}
}
