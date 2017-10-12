package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.impl.GtfsDaoImpl;
import org.onebusaway2.gtfs.model.*;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    public void mapTripPattern(ServiceJourneyPattern serviceJourneyPattern, GtfsDaoImpl gtfsDao, NetexDao netexDao){
        TripMapper tripMapper = new TripMapper();

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        List<ServiceJourney> serviceJourneys = netexDao.getServiceJourneyById().get(serviceJourneyPattern.getId());

        StopPattern stopPattern = null;

        Route route = netexDao.getRouteById().get(serviceJourneyPattern.getRouteRef().getRef());
        org.onebusaway2.gtfs.model.Route otpRoute = gtfsDao.getRouteById().get(AgencyAndIdFactory.getAgencyAndId(route.getLineRef().getValue().getRef()));

        if (serviceJourneys == null) {
            LOG.warn("ServiceJourneyPattern " + serviceJourneyPattern.getId() + " does not contain any serviceJourneys.");
            return;
        }

        for(ServiceJourney serviceJourney : serviceJourneys){
            Trip trip = tripMapper.mapServiceJourney(serviceJourney, gtfsDao, netexDao);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTime = passingTimes.getTimetabledPassingTime();
            List<StopTime> stopTimes = new ArrayList<>();

            int stopSequence = 0;

            for(TimetabledPassingTime passingTime : timetabledPassingTime){
                JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef = passingTime.getPointInJourneyPatternRef();
                String ref = pointInJourneyPatternRef.getValue().getRef();

                Stop quay = findQuay(ref, serviceJourneyPattern, netexDao, gtfsDao);

                if (quay == null) {
                    LOG.warn("Quay not found for timetabledPassingTime: " + passingTime.getId());
                    break;
                }

                StopTime stopTime = new StopTime();
                stopTime.setTrip(trip);
                stopTime.setStopSequence(stopSequence++);

                if(passingTime.getArrivalTime() != null){
                    int arrivalTime = passingTime.getArrivalTime().toSecondOfDay();
                    if(passingTime.getArrivalDayOffset() != null && passingTime.getArrivalDayOffset().intValue() == 1){
                        arrivalTime = arrivalTime + (3600 * 24);
                    }
                    stopTime.setArrivalTime(arrivalTime);
                }else if(passingTime.getDepartureTime() != null) {
                    int arrivalTime = passingTime.getDepartureTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        arrivalTime = arrivalTime + (3600 * 24);
                    }
                    stopTime.setArrivalTime(arrivalTime);
                }

                if(passingTime.getDepartureTime() != null) {
                    int departureTime = passingTime.getDepartureTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        departureTime = departureTime + (3600 * 24);
                    }
                    stopTime.setDepartureTime(departureTime);
                }
                else if(passingTime.getArrivalTime() != null) {
                    int departureTime = passingTime.getArrivalTime().toSecondOfDay();
                    if(passingTime.getArrivalDayOffset() != null && passingTime.getArrivalDayOffset().intValue() == 1){
                        departureTime = departureTime + (3600 * 24);
                    }
                    stopTime.setDepartureTime(departureTime);
                }
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, serviceJourneyPattern);
                if(stopPoint != null){
                    stopTime.setDropOffType(stopPoint.isForAlighting() != null && !stopPoint.isForAlighting() ? 1 : 0);
                    stopTime.setPickupType(stopPoint.isForBoarding() != null && !stopPoint.isForBoarding() ? 1 : 0);
                }

                if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
                    LOG.warn("Time missing for trip " + trip.getId());
                }

                stopTime.setStop(quay);
                stopTimes.add(stopTime);
                gtfsDao.getAllStopTimes().add(stopTime);
            }

            if(stopPattern == null){
                stopPattern = new StopPattern(stopTimes);
            }

            gtfsDao.getStopTimesByTrip().put(trip, stopTimes);
        }

        if (stopPattern.size == 0) {
            LOG.warn("ServiceJourneyPattern " + serviceJourneyPattern.getId() + " does not contain a valid stop pattern.");
            return;
        }

        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = serviceJourneyPattern.getId();
        tripPattern.name = serviceJourneyPattern.getName() == null ? "" : serviceJourneyPattern.getName().getValue();

        Deduplicator deduplicator = new Deduplicator();

        for(Trip trip : trips){
            if (gtfsDao.getStopTimesByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip, gtfsDao.getStopTimesForTrip(trip), deduplicator);
                tripPattern.add(tripTimes);
                gtfsDao.getAllTrips().add(trip);
            }
        }

        gtfsDao.getTripPatterns().put(stopPattern, tripPattern);
    }



    private Stop findStopPlace(String pointInJourneyPatterRef, ServiceJourneyPattern serviceJourneyPattern, NetexDao netexDao, GtfsDao gtfsDao){
        List<PointInLinkSequence_VersionedChildStructure> points =
                serviceJourneyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if(stop.getId().equals(pointInJourneyPatterRef)){
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point).getScheduledStopPointRef();

                    String stopId = netexDao.getStopPointStopPlaceMap().get(scheduledStopPointRef.getValue().getRef());

                    if (stopId == null) {
                        LOG.warn("StopPlace not found for " + scheduledStopPointRef.getValue().getRef());
                    }
                    else {
                        Stop stopPlace = gtfsDao.getStopForId(AgencyAndIdFactory.getAgencyAndId(stopId));
                        if (stopPlace == null) {
                            LOG.warn("StopPlace not found for " + scheduledStopPointRef.getValue().getRef());
                        }
                        return stopPlace;
                    }
                }
            }
        }
        return null;
    }

    private Stop findQuay(String pointInJourneyPatterRef, ServiceJourneyPattern serviceJourneyPattern, NetexDao netexDao, GtfsDao gtfsDao){
        List<PointInLinkSequence_VersionedChildStructure> points =
                serviceJourneyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if(stop.getId().equals(pointInJourneyPatterRef)){
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point).getScheduledStopPointRef();
                    String stopId = netexDao.getStopPointQuayMap().get(scheduledStopPointRef.getValue().getRef());
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef.getValue().getRef());
                    }
                    else {
                        Stop quay = gtfsDao.getStopForId(AgencyAndIdFactory.getAgencyAndId(stopId));
                        if (quay == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue().getRef());
                        }
                        return quay;
                    }
                }
            }
        }

        return null;
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef, ServiceJourneyPattern serviceJourneyPattern){
        List<PointInLinkSequence_VersionedChildStructure> points =
                serviceJourneyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if(stopPoint.getId().equals(pointInJourneyPatterRef)){
                    return stopPoint;
                }
            }
        }
        return null;
    }
}
