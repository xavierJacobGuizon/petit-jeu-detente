package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.List;
import org.jeuroute.gamecore.camera.WorldViewBounds;
import org.jeuroute.model.records.world.WorldRenderData;
import org.jeuroute.model.world.network.Intersection;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;
import org.jeuroute.model.world.transport.TransitLine;
import org.jeuroute.model.world.transport.Vehicle;

public final class WorldRenderer {

	private WorldRenderer() {}

	public static void renderWorld(WorldRenderData world, double zoom) {
		renderWorld(world, zoom, WorldViewBounds.UNBOUNDED);
	}

	public static void renderWorld(WorldRenderData world, double zoom, WorldViewBounds viewBounds) {
		WorldTerrainRenderer.render(world.terrain(), zoom, viewBounds);
		renderEnvironment(world, zoom, viewBounds);
		renderTransitNetwork(world, zoom, viewBounds);
		renderTransport(world, zoom, viewBounds);
	}

	private static void renderEnvironment(
		WorldRenderData world,
		double zoom,
		WorldViewBounds viewBounds
	) {
		renderRoads(world.roads(), zoom, viewBounds);
		renderResourceBuildings(world.resourceBuildings(), zoom, viewBounds);
		renderHouses(world.houses(), zoom, viewBounds);
	}

	private static void renderTransitNetwork(
		WorldRenderData world,
		double zoom,
		WorldViewBounds viewBounds
	) {
		renderLines(world.lines(), zoom, viewBounds);
		renderIntersections(world.intersections(), viewBounds);
		renderStations(world.stations(), zoom, viewBounds);
	}

	private static void renderTransport(
		WorldRenderData world,
		double zoom,
		WorldViewBounds viewBounds
	) {
		renderDepots(world.depots(), zoom, viewBounds);
		renderVehicles(world.vehicles(), viewBounds);
		renderPeople(world.people(), zoom, viewBounds);
	}

	private static void renderRoads(List<Road> roads, double zoom, WorldViewBounds viewBounds) {
		for (Road road : roads) {
			if (road == null) {
				continue;
			}
			Point start = road.getStart();
			Point end = road.getEnd();
			if (viewBounds.intersectsSegment(start.x, start.y, end.x, end.y, 5.0)) {
				road.display(zoom);
			}
		}
	}

	private static void renderIntersections(
		List<Intersection> intersections,
		WorldViewBounds viewBounds
	) {
		for (Intersection intersection : intersections) {
			if (intersection == null) {
				continue;
			}
			Point position = intersection.getPosition();
			if (viewBounds.contains(position.x, position.y, 5.0)) {
				intersection.display();
			}
		}
	}

	private static void renderResourceBuildings(
		List<ResourceBuilding> buildings,
		double zoom,
		WorldViewBounds viewBounds
	) {
		for (ResourceBuilding building : buildings) {
			if (building == null) {
				continue;
			}
			Point position = building.getPosition();
			if (viewBounds.contains(position.x, position.y, ResourceBuilding.HALF_SIZE)) {
				building.display(zoom);
			}
		}
	}

	private static void renderHouses(List<House> houses, double zoom, WorldViewBounds viewBounds) {
		for (House house : houses) {
			if (
				house != null &&
				viewBounds.contains(house.getPositionX(), house.getPositionY(), House.HALF_SIZE)
			) {
				house.display(zoom);
			}
		}
	}

	private static void renderPeople(List<Person> people, double zoom, WorldViewBounds viewBounds) {
		PersonRenderer.render(people, zoom, viewBounds);
	}

	private static void renderStations(
		List<Station> stations,
		double zoom,
		WorldViewBounds viewBounds
	) {
		for (Station station : stations) {
			if (station == null) {
				continue;
			}
			Point position = station.getPosition();
			if (viewBounds.contains(position.x, position.y, Station.CAPTURE_RADIUS)) {
				station.display(zoom);
			}
		}
	}

	private static void renderDepots(List<Depot> depots, double zoom, WorldViewBounds viewBounds) {
		for (Depot depot : depots) {
			Point position = depot.getPosition();
			if (viewBounds.contains(position.x, position.y, Depot.HALF_SIZE)) {
				depot.display(zoom);
			}
		}
	}

	private static void renderVehicles(List<Vehicle> vehicles, WorldViewBounds viewBounds) {
		for (Vehicle vehicle : vehicles) {
			if (
				vehicle != null &&
				viewBounds.contains(
					vehicle.getPositionX(),
					vehicle.getPositionY(),
					vehicle.getSize() / 2.0
				)
			) {
				vehicle.display();
			}
		}
	}

	private static void renderLines(
		List<TransitLine> lines,
		double zoom,
		WorldViewBounds viewBounds
	) {
		glLineWidth((float) (3.0 * zoom));
		glBegin(GL_LINES);
		for (TransitLine line : lines) {
			glColor3f(
				line.getColor().getRed(),
				line.getColor().getGreen(),
				line.getColor().getBlue()
			);
			for (var path : List.of(line.getPath(), line.getReturnPath())) {
				for (var leg : path.getLegs()) {
					Point start = leg.start();
					Point end = leg.target();
					if (!viewBounds.intersectsSegment(start.x, start.y, end.x, end.y, 1.5)) {
						continue;
					}
					glVertex2i(start.x, start.y);
					glVertex2i(end.x, end.y);
				}
			}
		}
		glEnd();
		glLineWidth(1.0f);
	}
}
