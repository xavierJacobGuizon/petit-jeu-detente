package org.jeuroute.gamecore;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.List;
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
		WorldTerrainRenderer.render(world.terrain(), zoom);
		renderEnvironment(world, zoom);
		renderTransitNetwork(world, zoom);
		renderTransport(world, zoom);
	}

	private static void renderEnvironment(WorldRenderData world, double zoom) {
		renderRoads(world.roads(), zoom);
		renderResourceBuildings(world.resourceBuildings(), zoom);
		renderHouses(world.houses(), zoom);
	}

	private static void renderTransitNetwork(WorldRenderData world, double zoom) {
		renderLines(world.lines(), zoom);
		renderIntersections(world.intersections());
		renderStations(world.stations(), zoom);
	}

	private static void renderTransport(WorldRenderData world, double zoom) {
		renderDepots(world.depots(), zoom);
		renderVehicles(world.vehicles());
		renderPeople(world.people(), zoom);
	}

	private static void renderRoads(List<Road> roads, double zoom) {
		for (Road road : roads) {
			if (road != null) {
				road.display(zoom);
			}
		}
	}

	private static void renderIntersections(List<Intersection> intersections) {
		for (Intersection intersection : intersections) {
			if (intersection != null) {
				intersection.display();
			}
		}
	}

	private static void renderResourceBuildings(List<ResourceBuilding> buildings, double zoom) {
		for (ResourceBuilding building : buildings) {
			if (building != null) {
				building.display(zoom);
			}
		}
	}

	private static void renderHouses(List<House> houses, double zoom) {
		for (House house : houses) {
			if (house != null) {
				house.display(zoom);
			}
		}
	}

	private static void renderPeople(List<Person> people, double zoom) {
		for (Person person : people) {
			if (person != null) {
				person.display(zoom);
			}
		}
	}

	private static void renderStations(List<Station> stations, double zoom) {
		for (Station station : stations) {
			if (station != null) {
				station.display(zoom);
			}
		}
	}

	private static void renderDepots(List<Depot> depots, double zoom) {
		for (Depot depot : depots) {
			depot.display(zoom);
		}
	}

	private static void renderVehicles(List<Vehicle> vehicles) {
		for (Vehicle vehicle : vehicles) {
			if (vehicle != null) {
				vehicle.display();
			}
		}
	}

	private static void renderLines(List<TransitLine> lines, double zoom) {
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
					glVertex2i(start.x, start.y);
					glVertex2i(end.x, end.y);
				}
			}
		}
		glEnd();
		glLineWidth(1.0f);
	}
}
