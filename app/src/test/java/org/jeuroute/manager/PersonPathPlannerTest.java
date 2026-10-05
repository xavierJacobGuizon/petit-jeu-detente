package org.jeuroute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jeuroute.model.world.enums.ResourceType;
import org.jeuroute.model.world.enums.TerrainType;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.network.RoadGraph;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.settlement.PersonRoute;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.junit.jupiter.api.Test;

class PersonPathPlannerTest {

	@Test
	void selectsRoadTravelWhenItProducesTheFastestRoute() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		roadGraph.createRoad(gridPoint(10, 24), gridPoint(50, 24));
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of());
		Point start = cellCenter(10, 24);
		Point destination = cellCenter(50, 24);

		PersonRoute route = planner.findFastestRouteToPoint(start, destination).orElseThrow();

		assertTrue(
			route
				.segments()
				.stream()
				.anyMatch(segment -> segment.surface() == PersonRoute.Surface.ROAD)
		);
		assertTrue(
			route.travelTimeSeconds(Person.WALK_SPEED_PIXELS_PER_SECOND) <
				start.distance(destination) / Person.WALK_SPEED_PIXELS_PER_SECOND
		);
	}

	@Test
	void followsConnectedRoadSegmentsThroughAnIntersection() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		Point intersection = new Point(400, 375);
		roadGraph.createRoad(new Point(150, 375), intersection);
		roadGraph.createRoad(intersection, new Point(400, 700));
		List<Road> connectedRoads = roadGraph.getConnectedRoadsAt(intersection);
		assertTrue(connectedRoads.size() >= 2);
		assertNotNull(
			roadGraph.getSharedRoadConnection(connectedRoads.get(0), connectedRoads.get(1))
		);

		List<ResourceBuilding> blockingBuildings = List.of(
			new ResourceBuilding(new Point(250, 466), ResourceType.values()[0]),
			new ResourceBuilding(new Point(330, 565), ResourceType.values()[0])
		);
		PersonPathPlanner planner = new PersonPathPlanner(
			terrain,
			roadGraph,
			List.of(),
			blockingBuildings,
			List.of()
		);
		PersonRoute route = planner
			.findFastestRouteToPoint(new Point(175, 375), new Point(400, 650))
			.orElseThrow();

		List<PersonRoute.Segment> roadSegments = route
			.segments()
			.stream()
			.filter(segment -> segment.surface() == PersonRoute.Surface.ROAD)
			.toList();
		assertTrue(roadSegments.size() >= 2, () -> route.segments().toString());
		assertTrue(
			roadSegments
				.stream()
				.anyMatch(
					segment ->
						segment.start().y == intersection.y && segment.target().y == intersection.y
				),
			route.segments().toString()
		);
		assertTrue(
			roadSegments
				.stream()
				.anyMatch(
					segment ->
						segment.start().x == intersection.x && segment.target().x == intersection.x
				),
			route.segments().toString()
		);
	}

	@Test
	void personPlacedOffRoadJoinsANearbyRoadBeforeContinuing() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		roadGraph.createRoad(new Point(150, 375), new Point(700, 375));
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of());

		PersonRoute route = planner
			.findFastestRouteToPoint(new Point(175, 450), new Point(650, 375))
			.orElseThrow();

		assertTrue(
			route
				.segments()
				.stream()
				.anyMatch(segment -> segment.surface() == PersonRoute.Surface.ROAD)
		);
		assertTrue(route.segments().getFirst().surface() == PersonRoute.Surface.WALKING);
	}

	@Test
	void walkingRouteGoesAroundHouseFootprints() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		House blockingHouse = new House(cellCenter(30, 24));
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of(blockingHouse));
		Point start = cellCenter(10, 24);
		Point destination = cellCenter(50, 24);

		PersonRoute route = planner.findFastestRouteToPoint(start, destination).orElseThrow();
		Rectangle2D footprint = inflatedFootprint(blockingHouse);

		for (PersonRoute.Segment segment : route.segments()) {
			assertFalse(
				footprint.intersectsLine(
					segment.start().x,
					segment.start().y,
					segment.target().x,
					segment.target().y
				)
			);
		}
	}

	@Test
	void houseJourneyEndsOutsideTheHouseFootprint() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		House destination = new House(cellCenter(50, 24));
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of(destination));

		PersonRoute route = planner.findFastestRoute(cellCenter(10, 24), destination).orElseThrow();

		assertNotNull(route.destination());
		assertTrue(
			route.destination().distance(destination.getPosition()) >
				House.HALF_SIZE + Person.COLLISION_RADIUS
		);
	}

	@Test
	void positionsInsideBuildingsAreNotWalkable() {
		TerrainMap terrain = rectangularTerrain();
		House house = new House(cellCenter(30, 24));
		PersonPathPlanner planner = planner(terrain, new RoadGraph(terrain), List.of(house));

		assertFalse(planner.isWalkablePosition(house.getPosition()));
	}

	@Test
	void reusesIdenticalCompletedRouteSearches() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of());
		Point start = cellCenter(10, 24);
		Point destination = cellCenter(50, 24);

		PersonRoute firstRoute = planner.findFastestRouteToPoint(start, destination).orElseThrow();
		PersonRoute secondRoute = planner.findFastestRouteToPoint(start, destination).orElseThrow();

		assertEquals(firstRoute.segments(), secondRoute.segments());
		assertEquals(1, planner.routeCacheHits());
		assertEquals(1, planner.routeCacheMisses());
	}

	@Test
	void coalescesIdenticalSearchesWhileTheFirstSearchIsStillRunning() {
		TerrainMap terrain = rectangularTerrain();
		RoadGraph roadGraph = new RoadGraph(terrain);
		PersonPathPlanner planner = planner(terrain, roadGraph, List.of());
		Point start = cellCenter(10, 24);
		Point destination = cellCenter(50, 24);

		PersonPathPlanner.SearchSession first = planner.beginFastestRouteToPoint(
			start,
			destination
		);
		PersonPathPlanner.SearchSession second = planner.beginFastestRouteToPoint(
			start,
			destination
		);

		assertSame(first, second);
		assertEquals(1, planner.coalescedSearches());
	}

	private static PersonPathPlanner planner(
		TerrainMap terrain,
		RoadGraph roadGraph,
		List<House> houses
	) {
		return new PersonPathPlanner(terrain, roadGraph, houses, List.of(), List.of());
	}

	private static Rectangle2D inflatedFootprint(House house) {
		Point center = house.getPosition();
		double extent = House.HALF_SIZE + Person.COLLISION_RADIUS;
		return new Rectangle2D.Double(center.x - extent, center.y - extent, extent * 2, extent * 2);
	}

	private static TerrainMap rectangularTerrain() {
		boolean[][] land = new boolean[TerrainMap.ROWS][TerrainMap.COLUMNS];
		for (int row = 0; row < TerrainMap.ROWS; row++) {
			java.util.Arrays.fill(land[row], true);
		}
		return new TerrainMap(TerrainType.ISLAND, land);
	}

	private static Point gridPoint(int column, int row) {
		return new Point(TerrainMap.gridX(column), TerrainMap.gridY(row));
	}

	private static Point cellCenter(int column, int row) {
		return new Point(
			TerrainMap.gridX(column) + TerrainMap.CELL_SIZE / 2,
			TerrainMap.gridY(row) + TerrainMap.CELL_SIZE / 2
		);
	}
}
