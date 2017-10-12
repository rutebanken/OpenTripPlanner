package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    public Collection<Stop> mapParentAndChildStops(StopPlace stopPlace, Map<String, StopPlace> parentStopPlacesById){
        ArrayList<Stop> stops = new ArrayList<>();

        Stop stop = new Stop();
        if (stopPlace.getName() != null) {
            stop.setName(stopPlace.getName().getValue());
        } else if (stopPlace.getParentSiteRef() != null && parentStopPlacesById.containsKey(stopPlace.getParentSiteRef().getRef())) {
            String parentName = parentStopPlacesById.get(stopPlace.getParentSiteRef().getRef()).getName().getValue();
            if (parentName != null) {
                stop.setName(parentName);
            } else {
                LOG.warn("No name found for stop " + stopPlace.getId() + " or in parent stop");
                stop.setName("Not found");
            }
        } else {

            LOG.warn("No name found for stop " + stopPlace.getId());
            stop.setName("Not found");
        }
        if(stopPlace.getCentroid() != null){
            stop.setLat(stopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            stop.setLon(stopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.warn(stopPlace.getId() + " does not contain any coordinates.");
        }

        stop.setId(AgencyAndIdFactory.getAgencyAndId(stopPlace.getId()));
        stops.add(stop);
        List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
        for(Object quayObject : quayRefOrQuay){
            if(quayObject instanceof Quay){
                Quay quay = (Quay) quayObject;
                Stop stopQuay = new Stop();
                stopQuay.setName(stop.getName());
                stopQuay.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());
                stopQuay.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
                stopQuay.setId(AgencyAndIdFactory.getAgencyAndId(quay.getId()));
                stopQuay.setParentStation(stop.getId().getId());
                stops.add(stopQuay);
            }
        }

        return stops;
    }
}
