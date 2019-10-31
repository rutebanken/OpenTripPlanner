package org.opentripplanner.graph_builder.annotation;

public class FloorNumberUnknownAssumedGroundLevel extends GraphBuilderAnnotation {

    public static final String FMT = "Could not determine floor number for layer %s, assumed to be ground-level.";

    final String layer;
    final Integer floorNumber;

    public FloorNumberUnknownAssumedGroundLevel(String layer, Integer floorNumber) {
        this.layer=layer;
        this.floorNumber=floorNumber;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, layer, floorNumber);
    }
}
