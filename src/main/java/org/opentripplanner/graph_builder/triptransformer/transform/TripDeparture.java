package org.opentripplanner.graph_builder.triptransformer.transform;

import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;


/**
 * Wrap trip and first departure time
 */
class TripDeparture implements Comparable<TripDeparture> {
    final Trip trip;
    final int departureTime;

    TripDeparture(Trip trip, int departureTime) {
        this.trip = trip;
        this.departureTime = departureTime;
    }

    String departureTimeAsString() {
        return TripTransformerTimeUtil.timeToString(departureTime);
    }

    /** A String that "most likely" kan be used to identify a trip. */
    String key() {
        return String.format(
                "%s-%s-%s-%s",
                trip.getRoute().getId().getId(),
                departureTimeAsString(),
                trip.getTripHeadsign(),
                trip.getServiceId()
        );
    }

    @Override
    public int compareTo(TripDeparture o) {
        return departureTime - o.departureTime;
    }

    @Override
    public String toString() {
        Route route = trip.getRoute();
        return "<" + route.getShortName()+ " " + route.getId().getId() + " " + trip.getTripHeadsign()
                + " " + TripTransformerTimeUtil.timeToString(departureTime) + '>';
    }
}
