package org.opentripplanner.graph_builder.annotation;

public class FlexibleStopPlaceWithoutCoordinates extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "%s  does not contain any coordinates.";

    final String flexibleStopPlaceId;

    public FlexibleStopPlaceWithoutCoordinates(String flexibleStopPlaceId) {
        this.flexibleStopPlaceId = flexibleStopPlaceId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, flexibleStopPlaceId);
    }
}
