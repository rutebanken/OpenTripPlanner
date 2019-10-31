package org.opentripplanner.graph_builder.annotation;

public class NoQuayOrFlexibleStopPlaceForTimetabledPassingTimes extends GraphBuilderAnnotation {

    public static final String FMT = "No quay or flexible stop place found for timetabledPassingTimes: %s";

    final String timetabledPassingTimesId;

    public NoQuayOrFlexibleStopPlaceForTimetabledPassingTimes(String timetabledPassingTimesId) {
        this.timetabledPassingTimesId = timetabledPassingTimesId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, timetabledPassingTimesId);
    }
}
