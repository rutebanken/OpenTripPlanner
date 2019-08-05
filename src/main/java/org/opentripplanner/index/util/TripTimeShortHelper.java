package org.opentripplanner.index.util;

import org.opentripplanner.api.model.Leg;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TripTimeShortHelper {

    private GraphIndex index;

    public TripTimeShortHelper(GraphIndex index) {
        this.index = index;
    }

    public List<TripTimeShort> getTripTimesShort(Trip trip, ServiceDate serviceDate) {
        final ServiceDay serviceDay = new ServiceDay(index.graph, serviceDate,
                                                            index.graph.getCalendarService(), trip.getRoute().getAgency().getId());
        TimetableSnapshotSource timetableSnapshotSource = index.graph.timetableSnapshotSource;
        Timetable timetable = null;
        if (timetableSnapshotSource != null) {
            TimetableSnapshot timetableSnapshot = timetableSnapshotSource.getTimetableSnapshot();
            if (timetableSnapshot != null) {
                // Check if realtime-data is available for trip
                TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(timetableSnapshotSource.getFeedId(), trip.getId().getId(), serviceDate);
                if (pattern == null) {
                    pattern = index.patternForTrip.get(trip);
                }
                timetable = timetableSnapshot.resolve(pattern, serviceDate);
            }
        }
        if (timetable == null) {
            timetable = index.patternForTrip.get(trip).scheduledTimetable;
        }

        // This check is made here to avoid changing TripTimeShort.fromTripTimes
        TripTimes times = timetable.getTripTimes(timetable.getTripIndex(trip.getId()));
        if (!serviceDay.serviceRunning(times.serviceCode)) {
            return new ArrayList<>();
        }
        else {
            return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
        }
    }


    /**
     * Find trip time short for the from place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForFromPlace(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);

        TripTimeShort tripTimeShort = tripTimes.get(leg.from.stopSequence);

        if (leg.isFlexible()) {
            long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

            tripTimeShort.scheduledDeparture = (int) startTimeSeconds;
            tripTimeShort.realtimeDeparture = (int) startTimeSeconds;
        }

        return tripTimeShort;
    }

    /**
     * Find trip time short for the to place in transit leg, or null.
     */
    public TripTimeShort getTripTimeShortForToPlace(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);
        if (trip == null) {
            return null;
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);

        TripTimeShort tripTimeShort = tripTimes.get(leg.to.stopSequence);

        if (leg.isFlexible()) {
            long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;

            tripTimeShort.scheduledArrival = (int) endTimeSeconds;
            tripTimeShort.realtimeArrival = (int) endTimeSeconds;
        }

        return tripTimeShort;
    }


    /**
     * Find trip time shorts for all stops for the full trip of a leg.
     */
    public List<TripTimeShort> getAllTripTimeShortsForLegsTrip(Leg leg) {
        if (leg.tripId == null || leg.serviceDate == null) {
            return new ArrayList<>();
        }
        Trip trip = index.tripForId.get(leg.tripId);
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);
        return getTripTimesShort(trip, serviceDate);
    }

    /**
     * Find trip time shorts for all intermediate stops for a leg.
     */
    public List<TripTimeShort> getIntermediateTripTimeShortsForLeg(Leg leg) {
        Trip trip = index.tripForId.get(leg.tripId);

        if (trip == null) {
            return new ArrayList<>();
        }
        ServiceDate serviceDate = parseServiceDate(leg.serviceDate);

        List<TripTimeShort> tripTimes = getTripTimesShort(trip, serviceDate);
        List<TripTimeShort> filteredTripTimes = new ArrayList<>();

        long startTimeSeconds = (leg.startTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        long endTimeSeconds = (leg.endTime.toInstant().toEpochMilli() - serviceDate.getAsDate().getTime()) / 1000;
        boolean boardingStopFound = false;
        for (TripTimeShort tripTime : tripTimes) {

            long boardingTime = leg.realTime ? tripTime.realtimeDeparture : tripTime.scheduledDeparture;

            if (!boardingStopFound) {
                boardingStopFound |= boardingTime == startTimeSeconds && matchesQuayOrSiblingQuay(leg.from.stopId, tripTime.stopId);
                continue;
            }

            long arrivalTime = leg.realTime ? tripTime.realtimeArrival : tripTime.scheduledArrival;
            if (arrivalTime == endTimeSeconds && matchesQuayOrSiblingQuay(leg.to.stopId, tripTime.stopId)) {
                break;
            }

            filteredTripTimes.add(tripTime);
        }

        return filteredTripTimes;
    }


    private ServiceDate parseServiceDate(String serviceDateString) {
        ServiceDate serviceDate;
        try {
            serviceDate = ServiceDate.parseString(serviceDateString);
        } catch (ParseException pe) {
            throw new RuntimeException("Unparsable service date: " + serviceDateString, pe);
        }
        return serviceDate;
    }

    private boolean matchesQuayOrSiblingQuay(AgencyAndId quayId, AgencyAndId candidate) {
        boolean foundMatch = quayId.equals(candidate);
        if (!foundMatch) {
            //Check parentStops
            Stop stop = index.stopForId.get(quayId);
            if (stop != null && stop.getParentStation() != null) {
                AgencyAndId parentStopId = stop.getParentStationAgencyAndId();
                Stop parentStation = index.stationForId.get(parentStopId);
                if (parentStation != null) {
                    Collection<Stop> childStops = index.stopsForParentStation.get(parentStation.getId());
                    for (Stop childStop : childStops) {
                        if (childStop.getId().equals(candidate)) {
                            return true;
                        }
                    }
                }
            }
        }
        return foundMatch;
    }
}
