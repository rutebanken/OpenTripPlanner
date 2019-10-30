package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.graph_builder.module.osm.WalkableAreaBuilder;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class AreaNotEpsilonValid extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Area %s is not epsilon-valid (epsilon = " + WalkableAreaBuilder.VISIBILITY_EPSILON + ")";

    final OSMWithTags area;

    public AreaNotEpsilonValid(OSMWithTags area) {
        this.area = area;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, area);
    }
}
