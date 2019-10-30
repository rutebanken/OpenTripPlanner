package org.opentripplanner.graph_builder.triptransformer.timetablereport;

import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.Trip;

class Departure implements Comparable<Departure> {
    final Trip trip;
    final int time;

    Departure(Trip trip, int time) {
        this.trip = trip;
        this.time = time;
    }

    String timeToString() {
        return TripTransformerTimeUtil.timeToString(time);
    }

    @Override
    public int compareTo(Departure o) {
        return time - o.time;
    }

    @Override
    public String toString() {
        return timeToString()
                + " " + trip.getRoute().getShortName()
                + " " + trip.getServiceId();
    }
}
