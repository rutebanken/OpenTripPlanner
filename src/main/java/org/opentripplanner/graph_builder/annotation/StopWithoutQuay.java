package org.opentripplanner.graph_builder.annotation;

public class StopWithoutQuay extends GraphBuilderAnnotation {

    public static final String FMT = "%s does not contain any quays";

    final String stopId;

    public StopWithoutQuay(String stopId) {
        this.stopId = stopId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, stopId);
    }
}
