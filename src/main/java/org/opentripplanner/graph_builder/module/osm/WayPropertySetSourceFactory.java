package org.opentripplanner.graph_builder.module.osm;

public class WayPropertySetSourceFactory {

    /**
     * Return the given WayPropertySetSource or throws IllegalArgumentException if an unkown type is specified
     */
    public static WayPropertySetSource fromConfig(String type) {
        // type is set to "default" by GraphBuilderParameters if not provided in build-config.json
    	if("default".equals(type)) {
            return new DefaultWayPropertySetSource();
        } else if("norway".equals(type)) {
            return new NorwayWayPropertySetSource();
        } else {
            throw new IllegalArgumentException(String.format("Unknown osmWayPropertySet: '%s'", type));
        }
    }
}