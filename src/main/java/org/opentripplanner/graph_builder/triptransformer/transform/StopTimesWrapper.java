package org.opentripplanner.graph_builder.triptransformer.transform;

import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.util.List;

/**
 * Wrapper around Trip and StopTimes to simplify access to a small set of properties.
 */
public class StopTimesWrapper {
    public final Trip trip;
    private final List<StopTime> times;

    StopTimesWrapper(Trip trip, List<StopTime> times) {
        this.trip = trip;
        this.times = times;
    }

    private StopTime first() {
        return times.get(0);
    }

    private StopTime last() {
        return times.get(times.size() - 1);
    }

    public int departureTime() {
        return first().getDepartureTime();
    }

    public String originStopId() {
        return first().getStop().getId().getId();
    }

    public String destinationStopId() {
        return last().getStop().getId().getId();
    }

    public boolean startAndEndsAt(String fromStopId, String toStopId) {
        return originStopId().equals(fromStopId) && destinationStopId().equals(toStopId);
    }

    /**
     * Create a key witch can be used to group trips according to their Journey pattern.
     */
    public String journeyPatterKey() {
        // We use trip headsign, fromStop and toStop to emulate JourneyPattern id, this should be good enough
        // to group trips together.
        return trip.getTripHeadsign() + "-" + originStopId() + "-" + destinationStopId();
    }

    @Override
    public String toString() {
        return trip.getId().getId() + ", from " + originStopId() + " to " + destinationStopId();
    }
}
