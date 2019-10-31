package org.opentripplanner.graph_builder.annotation;

public class NoPassengerStopAssignment extends GraphBuilderAnnotation {

    public static final String FMT = "No passengerStopAssignment found for : %s";

    final String scheduledStopPointRef;

    public NoPassengerStopAssignment(String scheduledStopPointRef) {
        this.scheduledStopPointRef = scheduledStopPointRef;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, scheduledStopPointRef);
    }
}
