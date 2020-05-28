package org.opentripplanner.routing.algorithm;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.routing.RoutingResponse;
import org.opentripplanner.model.routing.TripSearchMetadata;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.request.RoutingRequest;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Does a complete transit search, including access and egress legs.
 * <p>
 * This class has a request scope, hence the "Worker" name.
 */
public class RoutingWorker {
    private static final int NOT_SET = -1;

    private static final int TRANSIT_SEARCH_RANGE_IN_DAYS = 2;
    private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);

    private final RaptorService<TripSchedule> raptorService;

    /** Filter itineraries down to this limit, but not below. */
    private static final int MIN_NUMBER_OF_ITINERARIES = 3;

    /** Never return more that this limit of itineraries. */
    private static final int MAX_NUMBER_OF_ITINERARIES = 200;

    private final RoutingRequest request;
    private Instant filterOnLatestDepartureTime = null;
    private int searchWindowUsedInSeconds = NOT_SET;
    private Itinerary firstRemovedItinerary = null;

    public RoutingWorker(RaptorConfig<TripSchedule> config, RoutingRequest request) {
        this.raptorService = new RaptorService<>(config);
        this.request = request;
    }

    public RoutingResponse route(Router router) {
        try {
            List<Itinerary> itineraries;

            // Direct street routing
            itineraries = new ArrayList<>(DirectStreetRouter.route(router, request));

            // Transit routing
            itineraries.addAll(routeTransit(router));

            long startTimeFiltering = System.currentTimeMillis();

            // Filter itineraries
            List<Itinerary> filteredItineraries = filterItineraries(itineraries);

            LOG.debug("Filtering took {} ms", System.currentTimeMillis() - startTimeFiltering);
            LOG.debug("Return TripPlan with {} itineraries", itineraries.size());

            return new RoutingResponse(
                    TripPlanMapper.mapTripPlan(request, filteredItineraries),
                    createTripSearchMetadata()
            );
        }
        finally {
            request.cleanup();
        }
    }

    private Collection<Itinerary> routeTransit(Router router) {
        if (request.modes.transitModes.isEmpty()) { return Collections.emptyList(); }

        long startTime = System.currentTimeMillis();

        TransitLayer transitLayer = request.ignoreRealtimeUpdates
            ? router.graph.getTransitLayer()
            : router.graph.getRealtimeTransitLayer();

        RaptorRoutingRequestTransitData requestTransitDataProvider;
        requestTransitDataProvider = new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDateTime().toInstant(),
                TRANSIT_SEARCH_RANGE_IN_DAYS,
                request.modes.transitModes,
                request.rctx.bannedRoutes,
                request.walkSpeed
        );
        LOG.debug("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);

        /* Prepare access/egress transfers */

        double startTimeAccessEgress = System.currentTimeMillis();

        Collection<AccessEgress> accessTransfers = AccessEgressRouter.streetSearch(request, false, 2000, transitLayer.getStopIndex());
        Collection<AccessEgress> egressTransfers = AccessEgressRouter.streetSearch(request, true, 2000, transitLayer.getStopIndex());

        LOG.debug("Access/egress routing took {} ms",
                System.currentTimeMillis() - startTimeAccessEgress
        );
        verifyEgressAccess(accessTransfers, egressTransfers);

        // Prepare transit search
        double startTimeRouting = System.currentTimeMillis();
        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessTransfers,
                egressTransfers
        );

        // Route transit
        RaptorResponse<TripSchedule> transitResponse = raptorService.route(
            raptorRequest,
            requestTransitDataProvider
        );

        LOG.debug("Found {} transit itineraries", transitResponse.paths().size());
        LOG.debug("Transit search params used: {}", transitResponse.requestUsed().searchParams());
        LOG.debug("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

        // Create itineraries

        double startItineraries = System.currentTimeMillis();

        RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
                transitLayer,
                requestTransitDataProvider.getStartOfTime(),
                request
        );
        FareService fareService = request.getRoutingContext().graph.getService(FareService.class);

        List<Itinerary> itineraries = new ArrayList<>();
        for (Path<TripSchedule> path : transitResponse.paths()) {
            // Convert the Raptor/Astar paths to OTP API Itineraries
            Itinerary itinerary = itineraryMapper.createItinerary(path);
            // Decorate the Itineraries with fare information.
            // Itinerary and Leg are API model classes, lacking internal object references needed for effective
            // fare calculation. We derive the fares from the internal Path objects and add them to the itinerary.
            if (fareService != null) {
                itinerary.fare = fareService.getCost(path, transitLayer);
            }
            itineraries.add(itinerary);
        }

        // Filter itineraries away that depart after the latest-departure-time for depart after
        // search. These itineraries is a result of timeshifting the access leg and is needed for
        // the raptor to prune the results. These itineraries are often not ideal, but if they
        // pareto optimal for the "next" window, they will appear when a "next" search is performed.
        searchWindowUsedInSeconds = transitResponse.requestUsed().searchParams().searchWindowInSeconds();
        if(!request.arriveBy && searchWindowUsedInSeconds > 0) {
            filterOnLatestDepartureTime = Instant.ofEpochSecond(request.dateTime + searchWindowUsedInSeconds);
        }

        LOG.debug("Creating {} itineraries took {} ms",
                itineraries.size(),
                System.currentTimeMillis() - startItineraries
        );

        return itineraries;
    }

    private List<Itinerary> filterItineraries(List<Itinerary> itineraries) {
        ItineraryFilterChainBuilder builder = new ItineraryFilterChainBuilder(request.arriveBy);
        builder.setApproximateMinLimit(Math.min(request.numItineraries, MIN_NUMBER_OF_ITINERARIES));
        builder.setMaxLimit(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES));
        builder.setLatestDepartureTimeLimit(filterOnLatestDepartureTime);
        builder.setMaxLimitReachedSubscriber(it -> firstRemovedItinerary = it);

        // TODO OTP2 - Only set these if timetable view is enabled. The time-table-view is not
        //           - exposed as a parameter in the APIs yet.
        builder.removeTransitWithHigherCostThanBestOnStreetOnly(true);

        if(request.debugItineraryFilter) {
            builder.debug();
        }

        ItineraryFilter filterChain = builder.build();

        return filterChain.filter(itineraries);
    }

    private void verifyEgressAccess(
            Collection<?> access,
            Collection<?> egress
    ) {
        boolean accessExist = !access.isEmpty();
        boolean egressExist = !egress.isEmpty();

        if(accessExist && egressExist) { return; }

        List<String> missingPlaces = new ArrayList<>();
        if(!accessExist) { missingPlaces.add("from"); }
        if(!egressExist) { missingPlaces.add("to"); }

        throw new VertexNotFoundException(missingPlaces);
    }

    private TripSearchMetadata createTripSearchMetadata() {
        if(searchWindowUsedInSeconds == NOT_SET) { return null; }

        Instant reqTime = Instant.ofEpochSecond(request.dateTime);

        if (request.arriveBy) {
            return TripSearchMetadata.createForArriveBy(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.endTime().toInstant()
            );
        }
        else {
            return TripSearchMetadata.createForDepartAfter(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.startTime().toInstant()
            );
        }
    }
}
