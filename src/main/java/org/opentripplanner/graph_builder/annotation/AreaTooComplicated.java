package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.graph_builder.module.osm.WalkableAreaBuilder;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class AreaTooComplicated extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Area {} is too complicated ({} > " + WalkableAreaBuilder.MAX_AREA_NODES + ")";

    final OSMWithTags area;
    final int nbNodes;

    public AreaTooComplicated(OSMWithTags area, int nbNodes) {
        this.area = area;
        this.nbNodes = nbNodes;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, area, nbNodes);
    }
}
