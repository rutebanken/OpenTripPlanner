package org.opentripplanner.graph_builder.triptransformer.transform;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransitServiceDecorator {
    private final OtpTransitBuilder transitService;
    private final TTCalService calService ;
    private final Map<Trip, TripPattern> patterns = new HashMap<>();

    public TransitServiceDecorator(OtpTransitBuilder transitService) {
        this.transitService = transitService;
        this.calService = new TTCalService(transitService.getCalendarDates());

        for (TripPattern pattern : transitService.getTripPatterns().values()) {
            for (Trip trip : pattern.getTrips()) {
                this.patterns.put(trip, pattern);
            }
        }
    }

    public Multimap<Route, Trip> tripsByRoute(ServiceDate date, Function<Trip, Boolean> filter, boolean allTrips) {
        Set<AgencyAndId> serviceIds = serviceIdsForDate(date);

        List<Trip> trips = findAllTripsByServiceIds(serviceIds);

        Set<Route> routes = new HashSet<>();
        Multimap<Route, Trip> tripsByRoute = ArrayListMultimap.create();

        for (Trip trip : trips) {
            if (filter.apply(trip)) {
                if(allTrips) {
                    routes.add(trip.getRoute());
                }
                else {
                    tripsByRoute.put(trip.getRoute(), trip);
                }
            }
        }
        if(allTrips) {
            for (Trip trip : trips) {
                if (routes.contains(trip.getRoute())) {
                    tripsByRoute.put(trip.getRoute(), trip);
                }
            }
        }
        return tripsByRoute;
    }

    @Nonnull
    List<TripDeparture> findAllTrips(
            ServiceDate date,
            AgencyAndId routeId,
            Predicate<String> headsignFilter,
            IntPredicate departureTimeMinutesFilter
    ) {
        Set<AgencyAndId> serviceIds = serviceIdsForDate(date);
        Stream<Trip> tripStream = findAllTripsByServiceIds(serviceIds).stream()
                .filter(trip -> trip.getRoute().getId().equals(routeId));

        if(headsignFilter != null) {
            tripStream = tripStream.filter(it -> headsignFilter.test(it.getTripHeadsign()));
        }
        if(departureTimeMinutesFilter != null) {
            tripStream = tripStream.filter(it -> departureTimeMinutesFilter.test(departureTime(it)));
        }
        return tripStream.map(t -> new TripDeparture(t, departureTime(t))).collect(Collectors.toList());
    }

    @Nonnull
    TripDeparture findTrip(
            ServiceDate date,
            AgencyAndId routeId,
            Predicate<String> headsignFilter,
            IntPredicate departureTimeMinutesFilter
    ) {
        List<TripDeparture> trips = findAllTrips(date, routeId, headsignFilter, departureTimeMinutesFilter);
        if(trips.size() != 1) throw new IllegalStateException(
                "Unexpected number of trips found for: date=" + date + ", route=" + routeId
                + ", result=" + trips + ", departureFilter=" + departureTimeMinutesFilter
                + ", headsignFilter=" + headsignFilter
        );
        return trips.get(0);
    }

    @Nonnull
    List<TripDeparture> findAllTripsByPublicCode(ServiceDate date, String publicCode) {
        final Set<AgencyAndId> serviceIds = serviceIdsForDate(date);
        return trips().stream()
                .filter(t -> serviceIds.contains(t.getServiceId()))
                .filter(t -> t.getRoute().getShortName().equalsIgnoreCase(publicCode))
                .map(t -> new TripDeparture(t, departureTime(t)))
                .collect(Collectors.toList());
    }

    void copyTrip(TripDeparture tripDeparture, int timeShiftSeconds, ServiceDate date) {
        int newDepartureTime = tripDeparture.departureTime + timeShiftSeconds;
        Trip trip = tripDeparture.trip;

        if(newDepartureTime < 0) {
            // No use for so fare, can be implemented in the same way as the move method below
            throw new IllegalArgumentException("Can no copy trip to previous day. time: "
                    + TripTransformerTimeUtil.timeToString(newDepartureTime) + ", trip: " + trip);
        }
        copyTrip(trip, timeShiftSeconds, calService.getServiceIdForOnlyDate(date));
    }

    void moveTrip(TripDeparture tripDeparture, int timeShiftSeconds, ServiceDate date) {
        int newDepartureTime = tripDeparture.departureTime + timeShiftSeconds;
        Trip trip = tripDeparture.trip;
        ServiceDate newDate = date;
        int newTimeShift = timeShiftSeconds;

        // Move trip to previous operation day
        if(newDepartureTime < 0) {
            newDate = TripTransformerTimeUtil.dayBefore(date);
            newTimeShift = timeShiftSeconds + TripTransformerTimeUtil.offsetPrevDaySec(date);
        }

        NewServiceIds ids = calService.newServiceIds(trip.getServiceId(), date, newDate);

        // Trip is running on move than one service day
        // Service needs to be split
        if(ids.otherDaysId != null) {
            // Copy trip onto new service and timeshift it
            copyTrip(trip, newTimeShift, ids.targetId);
            // Remove day from original trip
            trip.setServiceId(ids.otherDaysId);
        }
        else {
            moveTrip(trip, newTimeShift, ids.targetId);
        }
    }

    public StopTimesWrapper stopTimes(Trip trip) {
        return new StopTimesWrapper(trip, transitService.getStopTimesSortedByTrip().get(trip));
    }

    public int departureTime(Trip trip) {
        return stopTimes(trip).departureTime();
    }


    /* private methods */

    private List<Trip> findAllTripsByServiceIds(Collection<AgencyAndId> serviceIds) {
        List<Trip> trips = new ArrayList<>();
        for (Trip it : trips()) {
            if (serviceIds.contains(it.getServiceId())) {
                trips.add(it);
            }
        }
        return trips;
    }

    private void moveTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
        List<StopTime> times = getStopTimesForTrip(trip);
        TripTimes tt = patterns.get(trip).scheduledTimetable.getTripTimes(trip);

        trip.setServiceId(targetServiceId);
        tt.timeShiftThis(timeShiftSeconds);

        for (StopTime it : times) {
            it.setId(newId(it.getId(), timeShiftSeconds));
            it.setDepartureTime(calculateNewTime(it.getDepartureTime(), timeShiftSeconds));
            it.setArrivalTime(calculateNewTime(it.getArrivalTime(), timeShiftSeconds));
        }
    }

    private void copyTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
        Trip newTrip = new Trip(trip);
        newTrip.setId(newId(trip.getId(), timeShiftSeconds));
        newTrip.setServiceId(targetServiceId);

        List<StopTime> newTimes = copyTimeShiftedStopTimes(trip, timeShiftSeconds);

        addTrip(newTrip, newTimes, timeShiftSeconds, trip);
    }

    private List<StopTime> copyTimeShiftedStopTimes(Trip trip, int timeShiftSeconds) {
        List<StopTime> newTimes = new ArrayList<>();

        for (StopTime it : getStopTimesForTrip(trip)) {
            StopTime newTime = new StopTime(it);
            newTime.setId(newId(it.getId(), timeShiftSeconds));
            newTime.setDepartureTime(calculateNewTime(it.getDepartureTime(), timeShiftSeconds));
            newTime.setArrivalTime(calculateNewTime(it.getArrivalTime(), timeShiftSeconds));
            newTimes.add(newTime);
        }
        return newTimes;
    }

    private List<StopTime> getStopTimesForTrip(Trip trip) {
        return transitService.getStopTimesSortedByTrip().get(trip);
    }

    private Set<AgencyAndId> serviceIdsForDate(ServiceDate date) {
        return calService.getServiceIdsForDate(date);
    }

    private Collection<Trip> trips() {
        return transitService.getTrips().values();
    }

    private void addTrip(Trip newTrip, List<StopTime> stopTimes, int timeShiftSeconds, Trip oldTrip) {
        transitService.getTrips().add(newTrip);
        transitService.getStopTimesSortedByTrip().put(newTrip, stopTimes);

        TripPattern tripPattern = patterns.get(oldTrip);
        tripPattern.getTrips().add(newTrip);
        TripTimes tt = new TripTimes(newTrip, timeShiftSeconds, tripPattern.scheduledTimetable.getTripTimes(oldTrip).clone());
        tripPattern.scheduledTimetable.addTripTimes(tt);
    }

    private static AgencyAndId newId(AgencyAndId id, int suffix) {
        return new AgencyAndId(id.getAgencyId(), id.getId() + "-" + suffix);
    }

    private int calculateNewTime(int original, int delta) {
        if(original < 0) return original;
        int newTime = original + delta;
        if(newTime < 0) throw new IllegalArgumentException(
                "Cant time-shift to a negative value: " + TripTransformerTimeUtil.timeToString(newTime)
        );
        return newTime;
    }
}
