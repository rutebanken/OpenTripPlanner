package org.opentripplanner.netex.mapping;

import net.opengis.gml._3.LinearRingType;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.annotation.FlexibleStopPlaceWithoutCoordinates;
import org.opentripplanner.model.Area;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.HailAndRideArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FlexibleStopPlaceMapper extends StopMapper {

    private static final Logger LOG = LoggerFactory.getLogger(FlexibleStopPlaceMapper.class);

    private static int positionOffset = 0;

    private FlexibleStopPlaceTypeMapper flexibleStopPlaceTypeMapper = new FlexibleStopPlaceTypeMapper();

    public FlexibleStopPlaceMapper(AddBuilderAnnotation addBuilderAnnotation) {
        super(addBuilderAnnotation);
    }

    public void mapFlexibleStopPlaceWithQuay(FlexibleStopPlace flexibleStopPlace, OtpTransitBuilder transitBuilder) {
        Stop quay = new Stop();
        Area area = new Area();
        quay.setStopType(Stop.stopTypeEnumeration.FLEXIBLE_AREA);
        quay.setId(AgencyAndIdFactory.createAgencyAndId(createQuayIdForStopPlaceId(flexibleStopPlace.getId())));
        // StopPlace maps to parent stop (location type 1)
        quay.setLocationType(0);

        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null){
            quay.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            quay.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            addBuilderAnnotation(new FlexibleStopPlaceWithoutCoordinates(flexibleStopPlace.getId()));
        }

        quay.setVehicleType(flexibleStopPlaceTypeMapper.getTransportMode(flexibleStopPlace));
        quay.setTimezone(super.DEFAULT_TIMEZONE);
        quay.setName(flexibleStopPlace.getName().getValue());
        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null){
            quay.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            quay.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else {
            addBuilderAnnotation(new FlexibleStopPlaceWithoutCoordinates(flexibleStopPlace.getId()));
        }
        if (flexibleStopPlace.getAreas() != null && flexibleStopPlace.getAreas().getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea() != null) {
            List<Object> areas = flexibleStopPlace.getAreas().getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea();
            if (areas.size() != 1) {
                LOG.info("{} areas found. Only the first area (if present) will be mapped.");
            }
            if (!areas.isEmpty()) {
                if (areas.get(0) instanceof FlexibleArea) {
                    FlexibleArea flexibleArea = (FlexibleArea)areas.get(0);
                    area = mapFlexibleArea(flexibleArea);
                } else if (areas.get(0) instanceof HailAndRideArea) {
                    LOG.info("StopPlace {} contains a hail and ride area which is not supported.", flexibleStopPlace.getId());
                }
            }
        }

        Stop stopPlace = new Stop();
        stopPlace.setName(flexibleStopPlace.getName().getValue());
        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null) {
            stopPlace.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            stopPlace.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        } else if (area.getCoordinates().length > 0) {
            Coordinate centroid = GeometryUtils.calculateCentroid(area.getCoordinates());
            stopPlace.setLat(centroid.y);
            // PositionOffset has to be done because the flex implementation does not work with duplicate
            // centroids in the same trip
            stopPlace.setLon(centroid.x + positionOffset / 1000.0);
            positionOffset += 1;
        }else {
            addBuilderAnnotation(new FlexibleStopPlaceWithoutCoordinates(flexibleStopPlace.getId()));
        }
        stopPlace.setId(AgencyAndIdFactory.createAgencyAndId(flexibleStopPlace.getId()));
        stopPlace.setLocationType(1);


        quay.setParentStation(stopPlace.getId().toString());
        quay.setArea(area);
        quay.setLat(stopPlace.getLat());
        quay.setLon(stopPlace.getLon());

        transitBuilder.getFlexibleQuayWithArea().add(new FlexibleQuayWithArea(quay, area));
        transitBuilder.getAreas().add(area);
        transitBuilder.getStops().add(quay);
        transitBuilder.getStops().add(stopPlace);
    }

    private Area mapFlexibleArea(FlexibleArea flexibleArea) {
        Area area = new Area();
        area.setId(AgencyAndIdFactory.createAgencyAndId(flexibleArea.getId()));
        area.setWkt(((LinearRingType)(flexibleArea.getPolygon().getExterior().getAbstractRing().getValue())).getPosList().getValue());
        return area;
    }

    private String createQuayIdForStopPlaceId(String stopPlaceId) {
        return stopPlaceId.replace("FlexibleStopPlace", "FlexibleQuay");
    }
}
