package org.opentripplanner.graph_builder.model;

import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NetexStopDao {
    private Map<String, StopPlace> stopsById = new HashMap<>();
    private Map<String, Quay> quayById = new HashMap<>();
    private Map<Quay, StopPlace> stopPlaceByQuay = new HashMap<>();

    public Map<Quay, StopPlace> getStopPlaceByQuay() {
        return stopPlaceByQuay;
    }

    public Map<String, StopPlace> getStopsById() {
        return stopsById;
    }

    public Map<String, Quay> getQuayById() {
        return quayById;
    }

    public Collection<StopPlace> getAllStopPlaces() {
        return stopsById.values();
    }

    public Map<String, StopPlace> parentStopPlaceById = new HashMap<>();
}
