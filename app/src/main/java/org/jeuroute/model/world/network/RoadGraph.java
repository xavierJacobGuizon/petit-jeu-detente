package org.jeuroute.model.world.network;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import org.jeuroute.model.records.world.RoadGraphNodeDistance;
import org.jeuroute.model.records.world.RoadGraphPreviousStep;
import org.jeuroute.model.records.world.RoadGraphRoadInterval;
import org.jeuroute.model.records.world.RoadLeg;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.utils.GeometryUtils;

public final class RoadGraph {

	public static final int GRID_SIZE = TerrainMap.CELL_SIZE;
	public static final double ROUTE_NODE_SNAP_DISTANCE = 12.0;
	private static final double MAX_REATTACH_DISTANCE = (GRID_SIZE * Math.sqrt(2.0)) / 2.0 + 1.0;

	private final List<Road> roads = new ArrayList<>();
	private final TerrainMap terrain;
	private final Set<Point> stationNodes = new LinkedHashSet<>();
	private final Map<Point, List<Road>> intersectionConnections = new LinkedHashMap<>();
	private final Map<Point, List<Road>> stationConnections = new LinkedHashMap<>();
	private final List<Road> roadsView = Collections.unmodifiableList(roads);
	private long version;

	public RoadGraph() {
		this(null);
	}

	public RoadGraph(TerrainMap terrain) {
		this.terrain = terrain;
	}

	public void addRoad(Road road) {
		if (!canAddRoad(road.getStart(), road.getEnd())) {
			return;
		}
		List<Road> allRoads = new ArrayList<>(roads);
		allRoads.add(road);
		rebuild(mergeCollinearRoads(allRoads));
	}

	public boolean canAddRoad(Point start, Point end) {
		return (
			start != null &&
			end != null &&
			!start.equals(end) &&
			isLandSegment(start, end) &&
			!isFullyCoveredByExistingRoads(start, end)
		);
	}

	public boolean isLandPosition(Point position) {
		return position != null && (terrain == null || terrain.isLand(position));
	}

	public boolean isLandSegment(Point start, Point end) {
		return (
			start != null && end != null && (terrain == null || terrain.containsSegment(start, end))
		);
	}

	public Road createRoad(Point start, Point end) {
		Road road = new Road(start, end);
		addRoad(road);
		return road;
	}

	public void addStationNode(Point position) {
		Point snappedPosition = snapPoint(position);
		if (!stationNodes.add(snappedPosition)) {
			return;
		}
		rebuild(new ArrayList<>(roads));
	}

	public List<Road> getRoadSegmentsAfterAddingStation(Point position) {
		Point snappedPosition = snapPoint(position);
		if (snappedPosition == null || stationNodes.contains(snappedPosition)) {
			return List.of();
		}

		List<Road> proposedSegments = new ArrayList<>();
		for (Road road : roads) {
			if (
				!road.hasEndpoint(snappedPosition) &&
				distanceToRoad(snappedPosition, road) <= MAX_REATTACH_DISTANCE
			) {
				proposedSegments.addAll(splitRoad(road, new ArrayList<>(List.of(snappedPosition))));
			}
		}
		return List.copyOf(proposedSegments);
	}

	public void clear() {
		roads.clear();
		intersectionConnections.clear();
		stationConnections.clear();
		version++;
	}

	public long getVersion() {
		return version;
	}

	public List<Road> getRoads() {
		return roadsView;
	}

	public List<Point> getIntersectionPositions() {
		return intersectionConnections.keySet().stream().map(Point::new).toList();
	}

	public List<Point> getStationNodePositions() {
		return stationNodes.stream().map(Point::new).toList();
	}

	public Point getRouteSnapPoint(Point position) {
		if (position == null) {
			return null;
		}
		Point nearestNode = null;
		double nearestDistance = ROUTE_NODE_SNAP_DISTANCE;
		for (Road road : roads) {
			for (Point endpoint : List.of(road.getStart(), road.getEnd())) {
				double distance = position.distance(endpoint);
				if (distance <= nearestDistance) {
					nearestNode = endpoint;
					nearestDistance = distance;
				}
			}
		}
		return nearestNode == null ? null : new Point(nearestNode);
	}

	public Point snapRoutePoint(Point position) {
		Point nearestNode = getRouteSnapPoint(position);
		return nearestNode == null ? snapPoint(position) : nearestNode;
	}

	public List<Point> getIntersectionsAfterAddingRoad(Point start, Point end) {
		Road proposedRoad = getMergedRouteCandidate(start, end);
		if (proposedRoad == null) {
			return List.of();
		}
		Set<Point> futureIntersections = new LinkedHashSet<>();
		for (Road existingRoad : roads) {
			Point intersection = findIntersection(proposedRoad, existingRoad);
			if (
				intersection != null &&
				!intersectionConnections.containsKey(intersection) &&
				!stationNodes.contains(intersection)
			) {
				futureIntersections.add(intersection);
			}
		}
		return futureIntersections.stream().map(Point::new).toList();
	}

	public List<Road> getRoadSegmentsAfterAddingRoad(Point start, Point end) {
		Road proposedRoad = getMergedRouteCandidate(start, end);
		if (proposedRoad == null) {
			return List.of();
		}

		List<Point> splitPoints = new ArrayList<>();
		for (Road existingRoad : roads) {
			Point intersection = findIntersection(proposedRoad, existingRoad);
			if (intersection != null && !splitPoints.contains(intersection)) {
				splitPoints.add(intersection);
			}
		}
		for (Point stationPosition : stationNodes) {
			if (
				distanceToRoad(stationPosition, proposedRoad) <= MAX_REATTACH_DISTANCE &&
				!splitPoints.contains(stationPosition)
			) {
				splitPoints.add(stationPosition);
			}
		}
		return List.copyOf(splitRoad(proposedRoad, splitPoints));
	}

	private Road getMergedRouteCandidate(Point start, Point end) {
		if (!canAddRoad(start, end)) {
			return null;
		}
		Road candidate = new Road(start, end);
		List<Road> sourceRoads = new ArrayList<>(roads);
		sourceRoads.add(candidate);
		return mergeCollinearRoads(sourceRoads)
			.stream()
			.filter(
				road ->
					road.containsPoint(candidate.getStart()) &&
					road.containsPoint(candidate.getEnd())
			)
			.findFirst()
			.orElse(candidate);
	}

	private boolean isFullyCoveredByExistingRoads(Point start, Point end) {
		double directionX = end.x - start.x;
		double directionY = end.y - start.y;
		double length = Math.hypot(directionX, directionY);
		if (length <= 0.0) {
			return true;
		}
		double unitX = directionX / length;
		double unitY = directionY / length;
		List<RoadGraphRoadInterval> intervals = new ArrayList<>();
		for (Road road : roads) {
			Point roadStart = road.getStart();
			Point roadEnd = road.getEnd();
			double startDistance =
				Math.abs(
					directionX * (roadStart.y - start.y) - directionY * (roadStart.x - start.x)
				) / length;
			double endDistance =
				Math.abs(directionX * (roadEnd.y - start.y) - directionY * (roadEnd.x - start.x)) /
				length;
			if (startDistance > 1.5 || endDistance > 1.5) {
				continue;
			}

			double projectedStart =
				(roadStart.x - start.x) * unitX + (roadStart.y - start.y) * unitY;
			double projectedEnd = (roadEnd.x - start.x) * unitX + (roadEnd.y - start.y) * unitY;
			double intervalStart = Math.max(0.0, Math.min(projectedStart, projectedEnd));
			double intervalEnd = Math.min(length, Math.max(projectedStart, projectedEnd));
			if (intervalEnd > intervalStart) {
				intervals.add(new RoadGraphRoadInterval(intervalStart, intervalEnd));
			}
		}

		intervals.sort(Comparator.comparingDouble(interval -> interval.start()));
		double coveredUntil = 0.0;
		for (RoadGraphRoadInterval interval : intervals) {
			if (interval.start() > coveredUntil + 0.001) {
				return false;
			}
			coveredUntil = Math.max(coveredUntil, interval.end());
			if (coveredUntil >= length - 0.001) {
				return true;
			}
		}
		return false;
	}

	public boolean isStationNodeAt(Point position) {
		return stationNodes.contains(position);
	}

	public boolean isIntersectionAt(Point position) {
		return intersectionConnections.containsKey(position);
	}

	public boolean isDecisionNodeAt(Point position) {
		return isIntersectionAt(position) || isStationNodeAt(position);
	}

	public List<Road> getConnectedRoadsAt(Point position) {
		List<Road> connections = stationConnections.get(position);
		if (connections == null) {
			connections = intersectionConnections.get(position);
		}
		return connections != null ? connections : List.of();
	}

	public Point getSharedRoadConnection(Road firstRoad, Road secondRoad) {
		if (firstRoad == null || secondRoad == null || firstRoad == secondRoad) {
			return null;
		}
		for (Point endpoint : List.of(firstRoad.getStart(), firstRoad.getEnd())) {
			if (
				secondRoad.hasEndpoint(endpoint) &&
				getConnectedRoadsAt(endpoint).contains(firstRoad) &&
				getConnectedRoadsAt(endpoint).contains(secondRoad)
			) {
				return new Point(endpoint);
			}
		}
		return null;
	}

	public Road firstRightRoadAt(Point position, Point arrivalPoint) {
		List<Road> connections = getConnectedRoadsAt(position);
		double arrivalX = arrivalPoint.x - position.x;
		double arrivalY = arrivalPoint.y - position.y;
		double arrivalLength = Math.hypot(arrivalX, arrivalY);
		if (arrivalLength <= 0.000001) {
			return null;
		}

		arrivalX /= arrivalLength;
		arrivalY /= arrivalLength;
		Road selectedRoad = null;
		double smallestClockwiseTurn = 360.0;

		for (Road candidate : connections) {
			if (!candidate.hasEndpoint(position)) {
				continue;
			}
			Point exit = candidate.otherEndpoint(position);
			if (exit.equals(arrivalPoint)) {
				continue;
			}

			double exitX = exit.x - position.x;
			double exitY = exit.y - position.y;
			double exitLength = Math.hypot(exitX, exitY);
			if (exitLength <= 0.000001) {
				continue;
			}
			exitX /= exitLength;
			exitY /= exitLength;

			double clockwiseTurn = Math.toDegrees(
				Math.atan2(arrivalY * exitX - arrivalX * exitY, arrivalX * exitX + arrivalY * exitY)
			);
			if (clockwiseTurn <= 0.000001) {
				clockwiseTurn += 360.0;
			}
			if (clockwiseTurn < smallestClockwiseTurn) {
				smallestClockwiseTurn = clockwiseTurn;
				selectedRoad = candidate;
			}
		}
		return selectedRoad;
	}

	public Optional<RoadPath> findPath(Point start, Point destination) {
		if (start.equals(destination)) {
			return Optional.of(new RoadPath(List.of()));
		}

		Map<Point, Double> distances = new HashMap<>();
		Map<Point, RoadGraphPreviousStep> previousSteps = new HashMap<>();
		PriorityQueue<RoadGraphNodeDistance> queue = new PriorityQueue<>(
			Comparator.comparingDouble(node -> node.distance())
		);
		distances.put(new Point(start), 0.0);
		queue.add(new RoadGraphNodeDistance(new Point(start), 0.0));

		while (!queue.isEmpty()) {
			RoadGraphNodeDistance current = queue.remove();
			if (
				current.distance() >
				distances.getOrDefault(current.position(), Double.POSITIVE_INFINITY)
			) {
				continue;
			}
			if (current.position().equals(destination)) {
				break;
			}

			for (Road road : roads) {
				Point next;
				if (road.getStart().equals(current.position())) {
					next = road.getEnd();
				} else if (road.getEnd().equals(current.position())) {
					next = road.getStart();
				} else {
					continue;
				}

				double candidateDistance = current.distance() + road.getLength();
				if (candidateDistance < distances.getOrDefault(next, Double.POSITIVE_INFINITY)) {
					Point nextPosition = new Point(next);
					distances.put(nextPosition, candidateDistance);
					previousSteps.put(
						nextPosition,
						new RoadGraphPreviousStep(new Point(current.position()), road)
					);
					queue.add(new RoadGraphNodeDistance(nextPosition, candidateDistance));
				}
			}
		}

		if (!previousSteps.containsKey(destination)) {
			return Optional.empty();
		}

		List<RoadLeg> reversedLegs = new ArrayList<>();
		Point cursor = new Point(destination);
		while (!cursor.equals(start)) {
			RoadGraphPreviousStep step = previousSteps.get(cursor);
			if (step == null) {
				return Optional.empty();
			}
			reversedLegs.add(new RoadLeg(step.road(), step.previous(), cursor));
			cursor = step.previous();
		}
		Collections.reverse(reversedLegs);
		return Optional.of(new RoadPath(reversedLegs));
	}

	public Optional<RoadPath> findPathFromRoadPosition(
		Point position,
		Road currentRoad,
		Point destination
	) {
		if (currentRoad == null) {
			return Optional.empty();
		}

		Point snappedPosition = closestPointOnRoad(position, currentRoad);
		RoadPath bestPath = null;
		double bestLength = Double.POSITIVE_INFINITY;
		for (Point endpoint : List.of(currentRoad.getStart(), currentRoad.getEnd())) {
			Optional<RoadPath> remainder = findPath(endpoint, destination);
			if (remainder.isEmpty()) {
				continue;
			}

			List<RoadLeg> legs = new ArrayList<>();
			if (!snappedPosition.equals(endpoint)) {
				legs.add(new RoadLeg(currentRoad, snappedPosition, endpoint));
			}
			legs.addAll(remainder.get().getLegs());
			RoadPath candidate = new RoadPath(legs);
			if (candidate.getLength() < bestLength) {
				bestPath = candidate;
				bestLength = candidate.getLength();
			}
		}
		return Optional.ofNullable(bestPath);
	}

	public Optional<RoadPath> findPathBetweenRoadPositions(
		Road startRoad,
		Point startPosition,
		Road destinationRoad,
		Point destinationPosition
	) {
		if (
			startRoad == null ||
			destinationRoad == null ||
			startPosition == null ||
			destinationPosition == null ||
			!roads.contains(startRoad) ||
			!roads.contains(destinationRoad) ||
			!startRoad.containsPoint(startPosition) ||
			!destinationRoad.containsPoint(destinationPosition)
		) {
			return Optional.empty();
		}

		RoadPath bestPath = null;
		if (startRoad == destinationRoad) {
			bestPath = new RoadPath(
				startPosition.equals(destinationPosition)
					? List.of()
					: List.of(new RoadLeg(startRoad, startPosition, destinationPosition))
			);
		}

		for (Point startEndpoint : List.of(startRoad.getStart(), startRoad.getEnd())) {
			for (Point destinationEndpoint : List.of(
				destinationRoad.getStart(),
				destinationRoad.getEnd()
			)) {
				Optional<RoadPath> middlePath = findPath(startEndpoint, destinationEndpoint);
				if (middlePath.isEmpty()) {
					continue;
				}

				List<RoadLeg> legs = new ArrayList<>();
				if (!startPosition.equals(startEndpoint)) {
					legs.add(new RoadLeg(startRoad, startPosition, startEndpoint));
				}
				legs.addAll(middlePath.get().getLegs());
				if (!destinationEndpoint.equals(destinationPosition)) {
					legs.add(
						new RoadLeg(destinationRoad, destinationEndpoint, destinationPosition)
					);
				}
				RoadPath candidate = new RoadPath(legs);
				if (bestPath == null || candidate.getLength() < bestPath.getLength()) {
					bestPath = candidate;
				}
			}
		}
		return Optional.ofNullable(bestPath);
	}

	public Optional<RoadPath> findNearestStationPath(Point position, Road currentRoad) {
		RoadPath nearestPath = null;
		double nearestLength = Double.POSITIVE_INFINITY;
		for (Point stationPosition : stationNodes) {
			Optional<RoadPath> candidate =
				currentRoad == null
					? findPath(position, stationPosition)
					: findPathFromRoadPosition(position, currentRoad, stationPosition);
			if (candidate.isPresent() && candidate.get().getLength() < nearestLength) {
				nearestPath = candidate.get();
				nearestLength = nearestPath.getLength();
			}
		}
		return Optional.ofNullable(nearestPath);
	}

	public Optional<RoadPath> findNearestPathToAny(
		Point position,
		Road currentRoad,
		List<Point> destinationPositions
	) {
		RoadPath bestPath = null;
		double bestLength = Double.POSITIVE_INFINITY;
		for (Point destination : destinationPositions) {
			Optional<RoadPath> candidate =
				currentRoad == null
					? findPath(position, destination)
					: findPathFromRoadPosition(position, currentRoad, destination);
			if (candidate.isPresent() && candidate.get().getLength() < bestLength) {
				bestPath = candidate.get();
				bestLength = bestPath.getLength();
			}
		}
		return Optional.ofNullable(bestPath);
	}

	public RoadPosition findClosestRoadPosition(
		Point position,
		Point destination,
		Point directionStart
	) {
		Road selectedRoad = null;
		Point selectedPosition = null;
		double selectedDistance = Double.POSITIVE_INFINITY;
		double bestAlignment = Double.NEGATIVE_INFINITY;

		for (Road road : roads) {
			double distance = distanceToRoad(position, road);
			if (distance > MAX_REATTACH_DISTANCE) {
				continue;
			}

			if (destination == null) {
				if (distance < selectedDistance) {
					selectedRoad = road;
					selectedPosition = closestPointOnRoad(position, road);
					selectedDistance = distance;
				}
				continue;
			}

			double alignment = directionAlignment(position, directionStart, destination, road);
			if (
				alignment > bestAlignment ||
				(alignment == bestAlignment && distance < selectedDistance)
			) {
				selectedRoad = road;
				selectedPosition = closestPointOnRoad(position, road);
				bestAlignment = alignment;
				selectedDistance = distance;
			}
		}

		if (selectedRoad == null || selectedPosition == null) {
			return null;
		}

		Point target = selectedRoad.endpointInDirection(selectedPosition, destination);
		if (selectedPosition.equals(target)) {
			target = selectedRoad.otherEndpoint(target);
		}
		return new RoadPosition(selectedRoad, selectedPosition, target);
	}

	public Road findRoadNear(Point position, double tolerance) {
		Road selectedRoad = null;
		double closestDistance = tolerance;

		for (Road road : roads) {
			double distance = distanceToRoad(position, road);
			if (distance <= closestDistance) {
				closestDistance = distance;
				selectedRoad = road;
			}
		}

		return selectedRoad;
	}

	public Point closestPointOnRoad(Point point, Road road) {
		double startX = road.getStart().x;
		double startY = road.getStart().y;
		double directionX = road.getEnd().x - startX;
		double directionY = road.getEnd().y - startY;
		double lengthSquared = directionX * directionX + directionY * directionY;
		if (lengthSquared == 0.0) {
			return new Point(road.getStart());
		}

		double projection =
			((point.x - startX) * directionX + (point.y - startY) * directionY) / lengthSquared;
		projection = Math.max(0.0, Math.min(1.0, projection));
		return new Point(
			(int) Math.round(startX + projection * directionX),
			(int) Math.round(startY + projection * directionY)
		);
	}

	private void rebuild(List<Road> sourceRoads) {
		Set<Point> foundIntersections = new LinkedHashSet<>();
		Map<Road, List<Point>> splitPoints = new IdentityHashMap<>();

		for (int firstIndex = 0; firstIndex < sourceRoads.size(); firstIndex++) {
			Road firstRoad = sourceRoads.get(firstIndex);
			for (
				int secondIndex = firstIndex + 1;
				secondIndex < sourceRoads.size();
				secondIndex++
			) {
				Road secondRoad = sourceRoads.get(secondIndex);
				Point crossing = findIntersection(firstRoad, secondRoad);
				if (crossing == null) {
					continue;
				}

				foundIntersections.add(crossing);
				splitPoints.computeIfAbsent(firstRoad, ignored -> new ArrayList<>()).add(crossing);
				splitPoints.computeIfAbsent(secondRoad, ignored -> new ArrayList<>()).add(crossing);
			}
		}

		for (Point stationPosition : stationNodes) {
			for (Road road : sourceRoads) {
				if (distanceToRoad(stationPosition, road) <= MAX_REATTACH_DISTANCE) {
					List<Point> points = splitPoints.computeIfAbsent(road, ignored ->
						new ArrayList<>()
					);
					if (!points.contains(stationPosition)) {
						points.add(stationPosition);
					}
				}
			}
		}

		List<Road> rebuiltRoads = new ArrayList<>();
		for (Road sourceRoad : sourceRoads) {
			List<Point> points = splitPoints.get(sourceRoad);
			if (points == null || points.isEmpty()) {
				rebuiltRoads.add(sourceRoad);
				continue;
			}
			rebuiltRoads.addAll(splitRoad(sourceRoad, points));
		}

		roads.clear();
		roads.addAll(rebuiltRoads);
		intersectionConnections.clear();
		stationConnections.clear();
		for (Point intersectionPosition : foundIntersections) {
			if (stationNodes.contains(intersectionPosition)) {
				continue;
			}
			List<Road> connectedRoads = roads
				.stream()
				.filter(road -> road.hasEndpoint(intersectionPosition))
				.toList();
			intersectionConnections.put(new Point(intersectionPosition), connectedRoads);
		}
		for (Point stationPosition : stationNodes) {
			List<Road> connectedRoads = roads
				.stream()
				.filter(road -> road.hasEndpoint(stationPosition))
				.toList();
			stationConnections.put(new Point(stationPosition), connectedRoads);
		}
		version++;
	}

	private static List<Road> splitRoad(Road sourceRoad, List<Point> splitPoints) {
		splitPoints.sort(Comparator.comparingDouble(sourceRoad.getStart()::distance));
		List<Road> segments = new ArrayList<>();
		Point segmentStart = sourceRoad.getStart();
		for (Point splitPoint : splitPoints) {
			if (!segmentStart.equals(splitPoint)) {
				segments.add(Road.createSegment(segmentStart, splitPoint));
			}
			segmentStart = splitPoint;
		}
		if (!segmentStart.equals(sourceRoad.getEnd())) {
			segments.add(Road.createSegment(segmentStart, sourceRoad.getEnd()));
		}
		return segments;
	}

	private static List<Road> mergeCollinearRoads(List<Road> sourceRoads) {
		List<Road> mergedRoads = new ArrayList<>();
		for (Road sourceRoad : sourceRoads) {
			Road combinedRoad = sourceRoad;
			boolean merged;
			do {
				merged = false;
				for (int index = 0; index < mergedRoads.size(); index++) {
					Road mergedRoad = mergeCollinearRoads(mergedRoads.get(index), combinedRoad);
					if (mergedRoad == null) {
						continue;
					}
					combinedRoad = mergedRoad;
					mergedRoads.remove(index);
					merged = true;
					break;
				}
			} while (merged);
			mergedRoads.add(combinedRoad);
		}
		return mergedRoads;
	}

	private static Road mergeCollinearRoads(Road firstRoad, Road secondRoad) {
		Point axisStart = firstRoad.getStart();
		Point axisEnd = firstRoad.getEnd();
		double axisX = axisEnd.x - axisStart.x;
		double axisY = axisEnd.y - axisStart.y;
		double axisLength = Math.hypot(axisX, axisY);
		if (axisLength <= 0.0) {
			return null;
		}

		Point secondStart = secondRoad.getStart();
		Point secondEnd = secondRoad.getEnd();
		double startOffset =
			Math.abs(
				axisX * (secondStart.y - axisStart.y) - axisY * (secondStart.x - axisStart.x)
			) / axisLength;
		double endOffset =
			Math.abs(axisX * (secondEnd.y - axisStart.y) - axisY * (secondEnd.x - axisStart.x)) /
			axisLength;
		if (startOffset > 1.5 || endOffset > 1.5) {
			return null;
		}

		double unitX = axisX / axisLength;
		double unitY = axisY / axisLength;
		double secondStartProjection =
			(secondStart.x - axisStart.x) * unitX + (secondStart.y - axisStart.y) * unitY;
		double secondEndProjection =
			(secondEnd.x - axisStart.x) * unitX + (secondEnd.y - axisStart.y) * unitY;
		double secondMinimum = Math.min(secondStartProjection, secondEndProjection);
		double secondMaximum = Math.max(secondStartProjection, secondEndProjection);
		if (secondMinimum > axisLength + 0.001 || secondMaximum < -0.001) {
			return null;
		}

		List<Point> endpoints = List.of(axisStart, axisEnd, secondStart, secondEnd);
		Point mergedStart = endpoints
			.stream()
			.min(
				Comparator.comparingDouble(
					point -> (point.x - axisStart.x) * unitX + (point.y - axisStart.y) * unitY
				)
			)
			.orElseThrow();
		Point mergedEnd = endpoints
			.stream()
			.max(
				Comparator.comparingDouble(
					point -> (point.x - axisStart.x) * unitX + (point.y - axisStart.y) * unitY
				)
			)
			.orElseThrow();
		return Road.createSegment(mergedStart, mergedEnd);
	}

	private static Point findIntersection(Road firstRoad, Road secondRoad) {
		double x1 = firstRoad.getStart().x;
		double y1 = firstRoad.getStart().y;
		double x2 = firstRoad.getEnd().x;
		double y2 = firstRoad.getEnd().y;
		double x3 = secondRoad.getStart().x;
		double y3 = secondRoad.getStart().y;
		double x4 = secondRoad.getEnd().x;
		double y4 = secondRoad.getEnd().y;

		double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		if (Math.abs(denominator) < 0.000001) {
			return null;
		}

		double firstParameter = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator;
		double secondParameter = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator;
		if (
			firstParameter < 0.0 ||
			firstParameter > 1.0 ||
			secondParameter < 0.0 ||
			secondParameter > 1.0
		) {
			return null;
		}

		Point crossing = new Point(
			(int) Math.round(x1 + firstParameter * (x2 - x1)),
			(int) Math.round(y1 + firstParameter * (y2 - y1))
		);
		return snapPoint(crossing);
	}

	private static double distanceToRoad(Point point, Road road) {
		double startX = road.getStart().x;
		double startY = road.getStart().y;
		double directionX = road.getEnd().x - startX;
		double directionY = road.getEnd().y - startY;
		double lengthSquared = directionX * directionX + directionY * directionY;
		if (lengthSquared == 0.0) {
			return point.distance(road.getStart());
		}

		double projection =
			((point.x - startX) * directionX + (point.y - startY) * directionY) / lengthSquared;
		projection = Math.max(0.0, Math.min(1.0, projection));
		double closestX = startX + projection * directionX;
		double closestY = startY + projection * directionY;
		return Math.hypot(point.x - closestX, point.y - closestY);
	}

	private static double directionAlignment(
		Point position,
		Point directionStart,
		Point destination,
		Road road
	) {
		double directionX = destination.x - directionStart.x;
		double directionY = destination.y - directionStart.y;
		Point forwardEnd = road.endpointInDirection(position, destination);
		Point backwardEnd = road.otherEndpoint(forwardEnd);
		double roadX = forwardEnd.x - backwardEnd.x;
		double roadY = forwardEnd.y - backwardEnd.y;
		double dot = GeometryUtils.dotProduct(directionX, directionY, roadX, roadY);
		if (dot <= 0.0) {
			return Double.NEGATIVE_INFINITY;
		}

		double directionLength = Math.hypot(directionX, directionY);
		double roadLength = Math.hypot(roadX, roadY);
		if (directionLength == 0.0 || roadLength == 0.0) {
			return Double.NEGATIVE_INFINITY;
		}
		return dot / (directionLength * roadLength);
	}

	public static Point snapPoint(Point point) {
		if (point == null) {
			return null;
		}
		return new Point(snapToGrid(point.x), snapToGrid(point.y));
	}

	public static int snapToGrid(int value) {
		return (int) Math.round(value / (double) GRID_SIZE) * GRID_SIZE;
	}
}
