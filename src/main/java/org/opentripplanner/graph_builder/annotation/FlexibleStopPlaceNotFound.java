package org.opentripplanner.graph_builder.annotation;

public class FlexibleStopPlaceNotFound extends GraphBuilderAnnotation {

    public static final String FMT = "Flexible StopPlace %s not found.";

    final String flexstopRef;

    public FlexibleStopPlaceNotFound(String flexstopRef) {
        this.flexstopRef = flexstopRef;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, flexstopRef);
    }
}
