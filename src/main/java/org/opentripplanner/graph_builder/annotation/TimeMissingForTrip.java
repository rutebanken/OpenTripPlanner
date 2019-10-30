package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.AgencyAndId;

public class TimeMissingForTrip extends GraphBuilderAnnotation {

    public static final String FMT = "Time missing for trip %s";

    final AgencyAndId tripId;

    public TimeMissingForTrip(AgencyAndId tripId) {
        this.tripId = tripId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, tripId);
    }
}
