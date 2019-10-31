package org.opentripplanner.graph_builder.annotation;

public class QuayNotFoundInStopPlaceFile extends GraphBuilderAnnotation {

    public static final String FMT = "Quay % not found in stop place file.";

    final String quayRef;

    public QuayNotFoundInStopPlaceFile(String quayRef) {
        this.quayRef = quayRef;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, quayRef);
    }
}
