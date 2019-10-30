package org.opentripplanner.netex.mapping;

import com.google.common.collect.ListMultimap;
import org.opentripplanner.graph_builder.annotation.FlexibleStopPlaceWithoutCoordinates;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.QuayWithoutCoordinates;
import org.opentripplanner.graph_builder.annotation.StopPlaceWithoutCoordinates;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.EntityMap;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;
import org.rutebanken.netex.model.TariffZoneRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    protected StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

    protected TransportModeMapper transportModeMapper = new TransportModeMapper();

    protected AdjacentSitesMapper adjacentSitesMapper = new AdjacentSitesMapper();

    protected String DEFAULT_TIMEZONE = "Europe/Oslo";

    private final AddBuilderAnnotation addBuilderAnnotation;

    public StopMapper(AddBuilderAnnotation addBuilderAnnotation) {
        this.addBuilderAnnotation = addBuilderAnnotation;
    }

    protected void addBuilderAnnotation(GraphBuilderAnnotation annotation) {
        addBuilderAnnotation.addBuilderAnnotation(annotation);
    }

    public Collection<Stop> mapParentAndChildStops(Collection<StopPlace> stopPlaces, OtpTransitBuilder transitBuilder, NetexDao netexDao){
        // Extract current stop based on validity and version
        StopPlaceVersionAndValidityComparator comparator = new StopPlaceVersionAndValidityComparator();
        List<StopPlace> stopPlaceList = new ArrayList<>(stopPlaces);
        stopPlaceList.sort(comparator);
        StopPlace currentStopPlace = stopPlaceList.get(0);

        // List of current stop with quays
        ArrayList<Stop> stops = new ArrayList<>();
        Stop stop = new Stop();
        stops.add(stop);
        // StopPlace maps to parent stop (location type 1)
        stop.setLocationType(1);
        stop.setId(AgencyAndIdFactory.createAgencyAndId(currentStopPlace.getId()));
        stop.setVehicleType(stopPlaceTypeMapper.getTransportMode(currentStopPlace));
        stop.setTransportSubmode(transportModeMapper.getTransportSubmode(currentStopPlace));
        stop.setTimezone(DEFAULT_TIMEZONE);
        stop.setWeight(mapInterchange(currentStopPlace));
        stop.getAdjacentSites().addAll(adjacentSitesMapper.mapAdjacentSites(currentStopPlace.getAdjacentSites()));

        // Map coordinates
        if(currentStopPlace.getCentroid() != null){
            stop.setLat(currentStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            stop.setLon(currentStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            addBuilderAnnotation(new StopPlaceWithoutCoordinates(currentStopPlace.getId()));
        }

        // Find parent multimodal stop if it present
        Stop multiModalStop = null;
        if (currentStopPlace.getParentSiteRef() != null) {
            AgencyAndId id = AgencyAndIdFactory.createAgencyAndId(currentStopPlace.getParentSiteRef().getRef());
            if (transitBuilder.getMultiModalStops().containsKey(id)) {
                multiModalStop = transitBuilder.getMultiModalStops().get(id);
                transitBuilder.getStationsByMultiModalStop().put(multiModalStop, stop);
            }
        }
        if (multiModalStop != null) {
            stop.setMultiModalStation(multiModalStop.getId().getId());
        }

        // Inherit name from multimodal stop if present
        if (currentStopPlace.getName() != null) {
            stop.setName(currentStopPlace.getName().getValue());
        } else if (multiModalStop != null) {
            String parentName = multiModalStop.getName();
            if (parentName != null) {
                stop.setName(parentName);
            } else {
                LOG.warn("No name found for stop " + currentStopPlace.getId() + " or in parent stop");
                stop.setName("N/A");
            }
        } else {
            stop.setName("N/A");
        }

        if (currentStopPlace.getAccessibilityAssessment() != null
                && currentStopPlace.getAccessibilityAssessment().getLimitations() != null
                && currentStopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation() != null &&
                currentStopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess() != null){
            switch (currentStopPlace.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value()) {
                case "true":
                    stop.setWheelchairBoarding(1);
                    break;
                case "false":
                    stop.setWheelchairBoarding(2);
                    break;
                case "unknown":
                    stop.setWheelchairBoarding(1);
                    break;
                default:
                    stop.setWheelchairBoarding(0);
                    break;
            }
        }
        else {
            stop.setWheelchairBoarding(0);
        }

        if (currentStopPlace.getDescription() != null) {
            stop.setDesc(currentStopPlace.getDescription().getValue());
        }

        mapTariffZonesForStopPlace(transitBuilder, currentStopPlace, stop);

        // Get quays from all versions of stop place
        Set<String> quaysSeen = new HashSet<>();

        for (StopPlace stopPlace : stopPlaces) {
            if (stopPlace.getQuays() != null) {
                List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                for (Object quayObject : quayRefOrQuay) {
                    if (quayObject instanceof Quay) {
                        Quay quay = (Quay) quayObject;
                        Stop stopQuay = new Stop();
                        stopQuay.setLocationType(0);
                        if (quay.getCentroid() == null || quay.getCentroid().getLocation() == null
                                || quay.getCentroid().getLocation().getLatitude() == null
                                || quay.getCentroid().getLocation().getLatitude() == null) {
                            addBuilderAnnotation.addBuilderAnnotation(new QuayWithoutCoordinates(quay.getId()));
                            continue;
                        }
                        stopQuay.setName(stop.getName());
                        stopQuay.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());
                        stopQuay.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
                        stopQuay.setId(AgencyAndIdFactory.createAgencyAndId(quay.getId()));
                        stopQuay.setPlatformCode(quay.getPublicCode());
                        stopQuay.setVehicleType(stop.getVehicleType());
                        stopQuay.setParentStation(stop.getId().getId());
                        stopQuay.setWeight(stop.getWeight());
                        if (quay.getDescription() != null) {
                            stopQuay.setDesc(quay.getDescription().getValue());
                        }
                        if (multiModalStop != null) {
                            stopQuay.setMultiModalStation(multiModalStop.getId().getId());
                        }

                        LocalDateTime toDate = stopPlace.getValidBetween() != null && stopPlace.getValidBetween().size() > 0
                                ? stopPlace.getValidBetween().get(0).getToDate() : null;
                        if (toDate != null && toDate.isBefore(LocalDateTime.now())) {
                            stopQuay.setExpired(true);
                        }

                        if (quay.getAccessibilityAssessment() != null
                                && quay.getAccessibilityAssessment().getLimitations() != null
                                && quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation() != null &&
                                quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess() != null){
                            switch (quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().value()) {
                                case "true":
                                    stopQuay.setWheelchairBoarding(1);
                                    break;
                                case "false":
                                    stopQuay.setWheelchairBoarding(2);
                                    break;
                                case "unknown":
                                    stopQuay.setWheelchairBoarding(1);
                                    break;
                                default:
                                    stopQuay.setWheelchairBoarding(0);
                                    break;
                            }
                        } else {
                            stopQuay.setWheelchairBoarding(stop.getWheelchairBoarding());
                        }

                        stopQuay.setTimezone(DEFAULT_TIMEZONE);

                        // Continue if this is not newest version of quay
                        try {
                            if (netexDao.quayById.lookup(stopQuay.getId().getId()).stream()
                                    .anyMatch(q -> Integer.parseInt(q.getVersion()) > Integer.parseInt(quay.getVersion()))) {
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            LOG.info("Quay version not an integer.");
                        }

                        if (!quaysSeen.contains(quay.getId())) {
                            stops.add(stopQuay);
                            quaysSeen.add(quay.getId());
                        }
                    }
                }
            }
        }
        return stops;
    }

    private void mapTariffZonesForStopPlace(OtpTransitBuilder transitBuilder, StopPlace currentStopPlace, Stop stop) {
        if (currentStopPlace.getTariffZones() != null) {
            for (TariffZoneRef tariffZoneRef : currentStopPlace.getTariffZones().getTariffZoneRef()) {
                AgencyAndId ref = AgencyAndIdFactory.createAgencyAndId(tariffZoneRef.getRef());
                if (transitBuilder.getTariffZones().containsKey(ref)) {
                    stop.getTariffZones().add(transitBuilder.getTariffZones().get(ref));
                }
            }
        }
    }

    // Mapped same way as parent stops for now
    Stop mapMultiModalStop(StopPlace stopPlace, OtpTransitBuilder transitBuilder) {
        Stop stop = new Stop();
        stop.setId(AgencyAndIdFactory.createAgencyAndId(stopPlace.getId()));
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
            addBuilderAnnotation.addBuilderAnnotation(new StopPlaceWithoutCoordinates(stopPlace.getId()));
        }

        stop.setWeight(mapInterchange(stopPlace));
        mapTariffZonesForStopPlace(transitBuilder, stopPlace, stop);

        return stop;
    }

    // Mapped same way as parent stops for now
    Stop mapGroupsOfStopPlaces(GroupOfStopPlaces groupOfStopPlaces, ListMultimap<Stop, Stop> stopsByGroupOfStopPlaces, EntityMap<AgencyAndId, Stop> stops) {
        Stop group = new Stop();
        group.setId(AgencyAndIdFactory.createAgencyAndId(groupOfStopPlaces.getId()));
        group.setLocationType(1); // Set same as parent stop for now
        if (groupOfStopPlaces.getName() != null) {
            group.setName(groupOfStopPlaces.getName().getValue());
        } else {
            LOG.warn("No name found for group of stop places " + groupOfStopPlaces.getId());
            group.setName("Not found");
        }
        if(groupOfStopPlaces.getCentroid() != null){
            group.setLat(groupOfStopPlaces.getCentroid().getLocation().getLatitude().doubleValue());
            group.setLon(groupOfStopPlaces.getCentroid().getLocation().getLongitude().doubleValue());
        } else{
            addBuilderAnnotation.addBuilderAnnotation(new StopPlaceWithoutCoordinates(groupOfStopPlaces.getId()));
        }

        StopPlaceRefs_RelStructure members = groupOfStopPlaces.getMembers();
        if (members != null) {
            List<StopPlaceRefStructure> memberList = members.getStopPlaceRef();
            for (StopPlaceRefStructure stopPlaceRefStructure : memberList) {
                AgencyAndId stopId = AgencyAndIdFactory.createAgencyAndId(stopPlaceRefStructure.getRef());
                if (stops.containsKey(stopId)) {
                    Stop stop = stops.get(stopId);
                    stopsByGroupOfStopPlaces.put(group, stop);
                }
            }
        }

        return group;
    }

    Stop.interchangeWeightingEnumeration mapInterchange(StopPlace stopPlace) {
        if  (stopPlace.getWeighting() != null) {
            switch (stopPlace.getWeighting()) {
                case PREFERRED_INTERCHANGE:
                    return Stop.interchangeWeightingEnumeration.PREFERRED_INTERCHANGE;
                case RECOMMENDED_INTERCHANGE:
                    return Stop.interchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE;
                case INTERCHANGE_ALLOWED:
                    return Stop.interchangeWeightingEnumeration.INTERCHANGE_ALLOWED;
                case NO_INTERCHANGE:
                    return Stop.interchangeWeightingEnumeration.NO_INTERCHANGE;
                default:
                    return Stop.interchangeWeightingEnumeration.INTERCHANGE_ALLOWED;
            }
        }
        else {
            return Stop.interchangeWeightingEnumeration.INTERCHANGE_ALLOWED;
        }
    }


}