package org.jeuroute.model.records.world;

import java.util.List;
import org.jeuroute.model.world.network.Intersection;
import org.jeuroute.model.world.network.Road;
import org.jeuroute.model.world.resources.ResourceBuilding;
import org.jeuroute.model.world.settlement.House;
import org.jeuroute.model.world.settlement.Person;
import org.jeuroute.model.world.terrain.TerrainMap;
import org.jeuroute.model.world.transport.Depot;
import org.jeuroute.model.world.transport.Station;
import org.jeuroute.model.world.transport.TransitLine;
import org.jeuroute.model.world.transport.Vehicle;

public record WorldRenderData(
	TerrainMap terrain,
	List<Road> roads,
	List<ResourceBuilding> resourceBuildings,
	List<House> houses,
	List<TransitLine> lines,
	List<Intersection> intersections,
	List<Station> stations,
	List<Depot> depots,
	List<Vehicle> vehicles,
	List<Person> people
) {}
