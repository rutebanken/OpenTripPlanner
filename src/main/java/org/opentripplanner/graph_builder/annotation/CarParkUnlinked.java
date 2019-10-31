package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.vertextype.ParkAndRideVertex;

public class CarParkUnlinked extends GraphBuilderAnnotation {

    private static final String FMT = "Car park %s not near any streets; it will not be usable.";
    private static final String HTMLFMT = "Car park <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\"</a> not near any streets; it will not be usable.";

    final ParkAndRideVertex carParkVertex;

    public CarParkUnlinked(ParkAndRideVertex carParkVertex) {
        this.carParkVertex = carParkVertex;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, carParkVertex.getLat(), carParkVertex.getLon(), carParkVertex);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, carParkVertex);
    }

}
