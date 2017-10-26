package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    public Collection<Stop> mapParentAndChildStops(StopPlace stopPlace, GtfsDao gtfsDao){
        Stop stop = new Stop();
        Stop multiModalStop = null;

        if (stopPlace.getParentSiteRef() != null &&
                gtfsDao.getMultiModalStops().containsKey(AgencyAndIdFactory.getAgencyAndId(stopPlace.getParentSiteRef().getRef()))) {


            multiModalStop = gtfsDao.getMultiModalStops().get(AgencyAndIdFactory.getAgencyAndId(stopPlace.getParentSiteRef().getRef()));

            if (gtfsDao.getStationsByMultiModalStop().containsKey(multiModalStop)) {
                gtfsDao.getStationsByMultiModalStop().get(multiModalStop).add(stop);
            }
            else {
                gtfsDao.getStationsByMultiModalStop().put(multiModalStop,
                        new ArrayList(Arrays.asList(stop)));
            }
        }

        ArrayList<Stop> stops = new ArrayList<>();
        stop.setLocationType(1);
        if (stopPlace.getName() != null) {
            stop.setName(stopPlace.getName().getValue());
        } else if (multiModalStop != null) {
            String parentName = multiModalStop.getName();
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
                stopQuay.setLocationType(0);
                stopQuay.setName(stop.getName());
                stopQuay.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());
                stopQuay.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
                stopQuay.setId(AgencyAndIdFactory.getAgencyAndId(quay.getId()));
                stopQuay.setParentStation(stop.getId().getId());
                if (multiModalStop != null) {
                    stopQuay.setMultiModalStation(multiModalStop.getId().getId());
                }
                stops.add(stopQuay);
            }
        }

        return stops;
    }

    // Mapped same way as parent stops for now
    public Stop mapMultiModalStop(StopPlace stopPlace) {
        Stop stop = new Stop();
        stop.setId(AgencyAndIdFactory.getAgencyAndId(stopPlace.getId()));
        stop.setLocationType(1); // Set same as parent stop for now
        if (stopPlace.getName() != null) {
            stop.setName(stopPlace.getName().getValue());
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

        return stop;
    }
}
