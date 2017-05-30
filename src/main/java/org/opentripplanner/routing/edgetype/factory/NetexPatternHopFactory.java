package org.opentripplanner.routing.edgetype.factory;


import com.beust.jcommander.internal.Maps;
import com.vividsolutions.jts.geom.LineString;
import org.onebusaway2.gtfs.model.*;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NetexPatternHopFactory extends PatternHopFactory{

    private static final Logger LOG = LoggerFactory.getLogger(NetexPatternHopFactory.class);

    private NetexStopContext stopContext = null;

    private NetexDao netexDao;

    @Override
    protected List<Agency> getAllAgencies() {
        return netexDao.getAllAgencies();
    }

    @Override
    protected Collection<Stop> getAllStops() {
        return netexDao.getAllStops();
    }

    @Override
    protected List<ShapePoint> getShapePointsForShapeId(AgencyAndId agencyAndId) {
        return new ArrayList<>();
    }

    @Override
    protected List<Stop> getStopsForStation(Stop stop) {
        return new ArrayList<>();
    }

    public void run(Graph graph) {
        loadStops(graph, netexDao);
        loadAgencies(graph);

        /* Assign 0-based numeric codes to all GTFS service IDs. */
        for (AgencyAndId serviceId : netexDao.getServiceIds()) {
            // TODO: FIX Service code collision for multiple feeds.
            graph.serviceCodes.put(serviceId, graph.serviceCodes.size());
        }

        /* Generate unique human-readable names for all the TableTripPatterns. */
        TripPattern.generateUniqueNames(netexDao.getTripPatterns());

        /* Generate unique short IDs for all the TableTripPatterns. */
        TripPattern.generateUniqueIds(netexDao.getTripPatterns());

        /* Loop over all new TripPatterns, creating edges, setting the service codes and geometries, etc. */

        for (TripPattern tripPattern : netexDao.getTripPatterns()) {

            Map<TripPattern, LineString[]> geometriesByTripPattern = Maps.newHashMap();

            for(Trip trip : tripPattern.getTrips()){
                if (!geometriesByTripPattern.containsKey(tripPattern) &&
                        trip.getShapeId() != null && trip.getShapeId().getId() != null &&
                        !trip.getShapeId().getId().equals("")) {
                    // save the geometry to later be applied to the hops
                    geometriesByTripPattern.put(tripPattern,  createGeometry(graph, trip, netexDao.getStopTimesForTrip().get(trip)));
                }
            }

            tripPattern.makePatternVerticesAndEdges(graph, stopContext.stationStopNodes);

            // Add the geometries to the hop edges.
            LineString[] geom = geometriesByTripPattern.get(tripPattern);
            if (geom != null) {
                for (int i = 0; i < tripPattern.hopEdges.length; i++) {
                    tripPattern.hopEdges[i].setGeometry(geom[i]);
                }
                // Make a geometry for the whole TripPattern from all its constituent hops.
                // This happens only if geometry is found in geometriesByTripPattern,
                // because that means that geometry was created from shapes instead "as crow flies"
                tripPattern.makeGeometry();
            }
            tripPattern.setServiceCodes(graph.serviceCodes);
            recordModeInformation(graph, tripPattern);

        }

        /* Identify interlined trips and create the necessary edges. */
        interline(netexDao.getTripPatterns(), graph);

        for (TripPattern tableTripPattern : netexDao.getTripPatterns()) {
            tableTripPattern.scheduledTimetable.finish();
        }

        clearCachedData();

    }

    private void loadStops(Graph graph, NetexDao dao) {
        for (Stop stop : dao.getAllStops()) {
            if (stopContext.stops.contains(stop.getId())) {
                LOG.error("Skipping stop {} because we already loaded an identical ID.", stop.getId());
                continue;
            }
            stopContext.stops.add(stop.getId());

            int locationType = stop.getLocationType();

            //add a vertex representing the stop
            if (locationType == 1) {
                stopContext.stationStopNodes.put(stop, new TransitStation(graph, stop));
            } else {
                TransitStop stopVertex = new TransitStop(graph, stop);
                stopContext.stationStopNodes.put(stop, stopVertex);
                if (locationType != 2) {
                    // Add a vertex representing arriving at the stop
                    TransitStopArrive arrive = new TransitStopArrive(graph, stop, stopVertex);
                    stopVertex.arriveVertex = arrive;

                    // Add a vertex representing departing from the stop
                    TransitStopDepart depart = new TransitStopDepart(graph, stop, stopVertex);
                    stopVertex.departVertex = depart;

                    // Add edges from arrive to stop and stop to depart
                    new PreAlightEdge(arrive, stopVertex);
                    new PreBoardEdge(stopVertex, depart);
                }
            }
        }
    }

    private void loadAgencies(Graph graph) {
        for (Agency agency : getAllAgencies()) {
            graph.addAgency("F1", agency);
        }
    }

    public void setStopContext(NetexStopContext stopContext) {
        this.stopContext = stopContext;
    }

    public void setNetexDao(NetexDao netexDao) {
        this.netexDao = netexDao;
    }
}
