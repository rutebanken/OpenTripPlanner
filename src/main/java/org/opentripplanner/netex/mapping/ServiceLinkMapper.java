package org.opentripplanner.netex.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.annotation.MissingProjectionInServiceLink;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServiceLinkMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);
    private final AddBuilderAnnotation addBuilderAnnotation;

    public ServiceLinkMapper(AddBuilderAnnotation addBuilderAnnotation) {
        this.addBuilderAnnotation = addBuilderAnnotation;
    }

    private Collection<ShapePoint> mapServiceLink(ServiceLink serviceLink, JourneyPattern journeyPattern, MutableInt sequenceCounter, MutableDouble distanceCounter, NetexDao netexDao) {
        Collection<ShapePoint> shapePoints = new ArrayList<>();
        AgencyAndId shapePointIdFromJourneyPatternId = createShapePointIdFromJourneyPatternId(AgencyAndIdFactory.createAgencyAndId(journeyPattern.getId()));

        if (serviceLink.getProjections() == null || serviceLink.getProjections().getProjectionRefOrProjection() == null) {


            String fromPointQuayId = netexDao.quayIdByStopPointRef.lookup(serviceLink.getFromPointRef().getRef());
            Quay fromPointQuay = netexDao.quayById.lookupLastVersionById(fromPointQuayId);

            String toPointQuayId = netexDao.quayIdByStopPointRef.lookup(serviceLink.getToPointRef().getRef());
            Quay toPointQuay = netexDao.quayById.lookupLastVersionById(toPointQuayId);

            if (fromPointQuay != null && fromPointQuay.getCentroid() != null && toPointQuay != null && toPointQuay.getCentroid() != null) {
                addBuilderAnnotation.addBuilderAnnotation(new MissingProjectionInServiceLink(serviceLink.getId()));

                ShapePoint fromShapePoint = new ShapePoint();
                fromShapePoint.setShapeId(shapePointIdFromJourneyPatternId);
                fromShapePoint.setLat(fromPointQuay.getCentroid().getLocation().getLatitude().doubleValue());
                fromShapePoint.setLon(fromPointQuay.getCentroid().getLocation().getLongitude().doubleValue());
                fromShapePoint.setSequence(sequenceCounter.toInteger());
                fromShapePoint.setDistTraveled(distanceCounter.getValue());
                shapePoints.add(fromShapePoint);
                sequenceCounter.increment();

                ShapePoint toShapePoint = new ShapePoint();
                toShapePoint.setShapeId(shapePointIdFromJourneyPatternId);
                toShapePoint.setLat(toPointQuay.getCentroid().getLocation().getLatitude().doubleValue());
                toShapePoint.setLon(toPointQuay.getCentroid().getLocation().getLongitude().doubleValue());
                toShapePoint.setSequence(sequenceCounter.toInteger());
                shapePoints.add(toShapePoint);
                sequenceCounter.increment();

                double distance;

                if (serviceLink.getDistance() != null) {
                    distance = serviceLink.getDistance().doubleValue();

                } else {
                    Coordinate fromCoord = new Coordinate(fromShapePoint.getLon(), fromShapePoint.getLat());
                    Coordinate toCoord = new Coordinate(toShapePoint.getLon(), toShapePoint.getLat());
                    distance = SphericalDistanceLibrary.degreesToMeters(toCoord.distance(fromCoord));
                }
                distanceCounter.add(distance);
                toShapePoint.setDistTraveled(distanceCounter.doubleValue());

            } else {
                LOG.warn("Ignore service link without projection and missing or unknown quays: " + serviceLink);
            }


        } else {
            for (JAXBElement<?> projectionElement : serviceLink.getProjections().getProjectionRefOrProjection()) {
                Object projectionObj = projectionElement.getValue();
                if (projectionObj instanceof LinkSequenceProjection_VersionStructure) {
                    LinkSequenceProjection_VersionStructure linkSequenceProjection = (LinkSequenceProjection_VersionStructure) projectionObj;
                    if (linkSequenceProjection.getLineString() != null) {
                        List<Double> coordinates = linkSequenceProjection.getLineString().getPosList().getValue();
                        double distance = serviceLink.getDistance() != null ? serviceLink.getDistance().doubleValue() : -1;
                        for (int i = 0; i < coordinates.size(); i += 2) {
                            ShapePoint shapePoint = new ShapePoint();
                            shapePoint.setShapeId(shapePointIdFromJourneyPatternId);
                            shapePoint.setLat(coordinates.get(i));
                            shapePoint.setLon(coordinates.get(i + 1));
                            shapePoint.setSequence(sequenceCounter.toInteger());
                            if (distance != -1) {
                                shapePoint.setDistTraveled(distanceCounter.doubleValue() + (distance / (coordinates.size() / 2) * (i / 2)));
                            }
                            sequenceCounter.increment();
                            shapePoints.add(shapePoint);
                        }
                        distanceCounter.add(distance != -1 ? distance : 0);
                    } else {
                        LOG.warn("Ignore linkSequenceProjection without linestring for: " + linkSequenceProjection.toString());
                    }
                }
            }
        }

        return shapePoints;
    }

    public Collection<ShapePoint> getShapePointsByJourneyPattern(JourneyPattern journeyPattern, NetexDao netexDao) {
        Collection<ShapePoint> shapePoints = new ArrayList<>();
        if (journeyPattern.getLinksInSequence() != null) {
            MutableInt sequenceCounter = new MutableInt(0);
            MutableDouble distance = new MutableDouble(0);
            for (LinkInLinkSequence_VersionedChildStructure linkInLinkSequence_versionedChildStructure
                    : journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern()) {

                String serviceLinkRef = ((ServiceLinkInJourneyPattern_VersionedChildStructure) linkInLinkSequence_versionedChildStructure).getServiceLinkRef().getRef();
                for (ShapePoint shapePoint : mapServiceLink(netexDao.serviceLinkById.lookup(serviceLinkRef),
                        journeyPattern, sequenceCounter, distance, netexDao)) {
                    shapePoints.add(shapePoint);
                }
            }
        }
        return shapePoints;
    }

    private AgencyAndId createShapePointIdFromJourneyPatternId(AgencyAndId journeyPatternId) {
        AgencyAndId id = new AgencyAndId(journeyPatternId.getAgencyId(), journeyPatternId.getId().replace("JourneyPattern", "ServiceLink"));
        return id;
    }

}
