package org.opentripplanner.netex.mapping;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.*;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.opentripplanner.model.StopPattern.*;

public class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private static final int DAY_IN_SECONDS = 3600 * 24;

    private BookingArrangementMapper bookingArrangementMapper = new BookingArrangementMapper();
    private String currentHeadsign;

    public void mapTripPattern(JourneyPattern journeyPattern, OtpTransitBuilder transitBuilder, NetexDao netexDao, String defaultFlexMaxTravelTime) {
        TripMapper tripMapper = new TripMapper();

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        Collection<ServiceJourney> serviceJourneys = netexDao.serviceJourneyByPatternId.lookup(journeyPattern.getId());

        StopPattern stopPattern = null;

        Route route = netexDao.routeById.lookup(journeyPattern.getRouteRef().getRef());
        org.opentripplanner.model.Route otpRoute = transitBuilder.getRoutes()
                .get(AgencyAndIdFactory.createAgencyAndId(route.getLineRef().getValue().getRef()));

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId() + " does not contain any serviceJourneys.");
            return;
        }

        for (ServiceJourney serviceJourney : serviceJourneys) {
            boolean isFlexible = isFlexible(serviceJourney, netexDao);
            if (isFlexible) {
                if (!flexibleStructureSupported(serviceJourney, netexDao)) {
                    LOG.warn("Flexible structure not supported for {}", serviceJourney.getId());
                    continue;
                }
            }

            Trip trip = tripMapper.mapServiceJourney(serviceJourney, transitBuilder, netexDao, defaultFlexMaxTravelTime);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTimes = passingTimes.getTimetabledPassingTime();

            List<StopTimeWithBookingArrangement> stopTimes = mapToStopTimes(
                    journeyPattern, transitBuilder, netexDao, trip, timetabledPassingTimes,isFlexible
            );

            tripMapper.setdrtAdvanceBookMin(trip, stopTimes.stream().map(s -> s.bookingArrangement).collect(Collectors.toList()));

            if (stopTimes != null && stopTimes.size() > 0) {
                transitBuilder.getStopTimesSortedByTrip().put(trip, stopTimes.stream().map(stwb -> stwb.stopTime).collect(Collectors.toList()));

                List<StopTimeWithBookingArrangement> stopTimesWithHeadsign = stopTimes.stream()
                        .filter(s -> s.stopTime.getStopHeadsign() != null && s.stopTime.getStopHeadsign() != "")
                        .collect(Collectors.toList());

                // Set first non-empty headsign as trip headsign
                if (stopTimesWithHeadsign.size() > 0) {
                    trip.setTripHeadsign(stopTimesWithHeadsign.stream().map(st -> st.stopTime)
                            .sorted(Comparator.comparingInt(StopTime::getStopSequence)).findFirst()
                            .get().getStopHeadsign());
                }
                else {
                    trip.setTripHeadsign("");
                }

                // We only generate a stopPattern for the first trip in the JourneyPattern.
                // We can do this because we assume the stopPatterrns are the same for all trips in a
                // JourneyPattern
                if (stopPattern == null) {
                    stopPattern = new StopPattern(transitBuilder.getStopTimesSortedByTrip().get(trip), getAreasById(transitBuilder)::get);
                    int i=0;
                    for (StopTimeWithBookingArrangement stwb: stopTimes) {
                        stopPattern.bookingArrangements[i++] = stwb.bookingArrangement;
                    }

                }
            }
            else {
                LOG.warn("No stop times found for trip " + serviceJourney.getId());
            }
        }

        if (stopPattern == null || stopPattern.size < 2) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain a valid stop pattern.");
            return;
        }

        TripPattern tripPattern = new TripPattern(otpRoute, stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();
        tripPattern.id = AgencyAndIdFactory.createAgencyAndId(journeyPattern.getId());

        Deduplicator deduplicator = new Deduplicator();

        for (Trip trip : trips) {
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip,
                        transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);

                if (Trip.ServiceAlteration.cancellation.equals(trip.getServiceAlteration()) ||
                    Trip.ServiceAlteration.replaced.equals(trip.getServiceAlteration())) {
                    // Trip is cancelled in plan data
                    tripTimes.cancelAllStops();
                }
                tripPattern.add(tripTimes);
                transitBuilder.getTrips().add(trip);
            }
        }

        if (route.getDirectionType() == null) {
            tripPattern.directionId = -1;
        }
        else {
            switch (route.getDirectionType()) {
                case OUTBOUND:
                    tripPattern.directionId = 0;
                    break;
                case INBOUND:
                    tripPattern.directionId = 1;
                    break;
                case CLOCKWISE:
                    tripPattern.directionId = 2;
                    break;
                case ANTICLOCKWISE:
                    tripPattern.directionId = 3;
                    break;
            }
        }

        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
    }

    private List<StopTimeWithBookingArrangement> mapToStopTimes(JourneyPattern journeyPattern, OtpTransitBuilder transitBuilder, NetexDao netexDao, Trip trip, List<TimetabledPassingTime> timetabledPassingTimes, boolean isFlexible) {
        List<StopTimeWithBookingArrangement> stopTimes = new ArrayList<>();

        int stopSequence = 0;

        for (TimetabledPassingTime passingTime : timetabledPassingTimes) {
            JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef
                    = passingTime.getPointInJourneyPatternRef();
            String ref = pointInJourneyPatternRef.getValue().getRef();

            Stop quay = findQuay(ref, journeyPattern, netexDao, transitBuilder);
            FlexibleQuayWithArea flexibleQuayWithArea = null;

            if (isFlexible) {
                flexibleQuayWithArea = findFlexibleQuayWithArea(ref, journeyPattern, netexDao, transitBuilder);
                if (flexibleQuayWithArea != null) {
                    quay = flexibleQuayWithArea.stop;
                }
            }

            if (quay != null) {
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, journeyPattern);
                StopTime stopTime = mapToStopTime(trip, stopPoint, quay, passingTime, stopSequence, netexDao);
                stopTime.setContinuousPickup(StopTime.MISSING_VALUE);
                stopTime.setContinuousDropOff(StopTime.MISSING_VALUE);

                // This only maps the case with exactly two passing times
                if (flexibleQuayWithArea != null) {
                    if (stopTimes.size() == 0) {
                        stopTime.setStartServiceArea(flexibleQuayWithArea.area);
                    } else if (stopTimes.size() == 1) {
                        stopTime.setEndServiceArea(stopTimes.get(0).stopTime.getStartServiceArea());
                    }
                }

                if (stopTimes.size() > 0 && stopTimeNegative(stopTimes.get(stopTimes.size() - 1).stopTime, stopTime)) {
                    LOG.error("Stoptime increased by negative amount in serviceJourney " + trip.getId().getId());
                    return null;
                }

                BookingArrangement bookingArrangement = mapBookingArrangement(stopPoint.getBookingArrangements());

                stopTimes.add(new StopTimeWithBookingArrangement(stopTime, bookingArrangement));
                ++stopSequence;
            } else {
                LOG.warn("No quay or flexible stop place found for timetabledPassingTimes: " + passingTime.getId());
            }
        }
        return stopTimes;
    }

    private BookingArrangement mapBookingArrangement(org.rutebanken.netex.model.BookingArrangementsStructure netexBookingArrangement) {
        if (netexBookingArrangement == null) {
            return null;
        }

        BookingArrangement otpBookingArrangement = bookingArrangementMapper.mapBookingArrangement(netexBookingArrangement.getBookingContact(), netexBookingArrangement.getBookingNote(),
                netexBookingArrangement.getBookingAccess(), netexBookingArrangement.getBookWhen(), netexBookingArrangement.getBuyWhen(), netexBookingArrangement.getBookingMethods(),
                netexBookingArrangement.getMinimumBookingPeriod(), netexBookingArrangement.getLatestBookingTime());

        return otpBookingArrangement;
    }

    private boolean stopTimeNegative(StopTime stopTime1, StopTime stopTime2) {
        int time1 = Math.max(stopTime1.getArrivalTime(), stopTime1.getDepartureTime());
        int time2 = Math.max(stopTime2.getArrivalTime(), stopTime2.getDepartureTime());

        return !(time1 >= 0 && time2 >= 0 && time2 >= time1);
    }

    private StopTime mapToStopTime(Trip trip, StopPointInJourneyPattern stopPoint, Stop quay,
                                   TimetabledPassingTime passingTime, int stopSequence, NetexDao netexDao) {
        StopTime stopTime = new StopTime();
        stopTime.setId(AgencyAndIdFactory.createAgencyAndId(passingTime.getId()));
        stopTime.setTrip(trip);
        stopTime.setStopSequence(stopSequence);
        stopTime.setStop(quay);

        stopTime.setArrivalTime(
                calculateOtpTime(new TimeWithOffset(passingTime.getArrivalTime(), passingTime.getArrivalDayOffset()),
                        Arrays.asList(
                        new TimeWithOffset(passingTime.getLatestArrivalTime(), passingTime.getLatestArrivalDayOffset()),
                        new TimeWithOffset(passingTime.getDepartureTime(), passingTime.getDepartureDayOffset()),
                        new TimeWithOffset(passingTime.getEarliestDepartureTime(), passingTime.getEarliestDepartureDayOffset()))
                )
        );

        stopTime.setDepartureTime(
                calculateOtpTime(new TimeWithOffset(passingTime.getDepartureTime(), passingTime.getDepartureDayOffset()),
                        Arrays.asList(
                                new TimeWithOffset(passingTime.getEarliestDepartureTime(), passingTime.getEarliestDepartureDayOffset()),
                                new TimeWithOffset(passingTime.getArrivalTime(), passingTime.getArrivalDayOffset()),
                                new TimeWithOffset(passingTime.getLatestArrivalTime(), passingTime.getLatestArrivalDayOffset()))
                )
        );

        if (stopPoint != null) {
            if (isFalse(stopPoint.isForAlighting())) {
                stopTime.setDropOffType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setDropOffType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setDropOffType(PICKDROP_SCHEDULED);
            }

            if (isFalse(stopPoint.isForBoarding())) {
                stopTime.setPickupType(PICKDROP_NONE);
            } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
                stopTime.setPickupType(PICKDROP_COORDINATE_WITH_DRIVER);
            } else {
                stopTime.setPickupType(PICKDROP_SCHEDULED);
            }
        }

        if (passingTime.getArrivalTime() == null && passingTime.getDepartureTime() == null) {
            LOG.warn("Time missing for trip " + trip.getId());
        }

        if (stopPoint.getDestinationDisplayRef() != null) {
            DestinationDisplay value = netexDao.destinationDisplayById.lookup(stopPoint.getDestinationDisplayRef().getRef());
            if (value != null) {
                currentHeadsign = value.getFrontText().getValue();
            }
        }

        if (currentHeadsign != null) {
            stopTime.setStopHeadsign(currentHeadsign);
        }

        return stopTime;
    }



    private Stop findQuay(String pointInJourneyPatterRef, JourneyPattern journeyPattern,
            NetexDao netexDao, OtpTransitBuilder transitBuilder) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if (stop.getId().equals(pointInJourneyPatterRef)) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point)
                            .getScheduledStopPointRef();
                    String stopId = netexDao.quayIdByStopPointRef.lookup(scheduledStopPointRef.getValue().getRef());
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef
                                .getValue().getRef());
                    } else {
                        Stop quay = transitBuilder.getStops()
                                .get(AgencyAndIdFactory.createAgencyAndId(stopId));
                        if (quay == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue()
                                    .getRef());
                        }
                        return quay;
                    }
                }
            }
        }

        return null;
    }



    private FlexibleQuayWithArea findFlexibleQuayWithArea(String pointInJourneyPatterRef, JourneyPattern journeyPattern,
                                                          NetexDao netexDao, OtpTransitBuilder transitBuilder) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if (stop.getId().equals(pointInJourneyPatterRef)) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point)
                            .getScheduledStopPointRef();
                    String stopId = netexDao.flexibleStopPlaceIdByStopPointRef.lookup(scheduledStopPointRef.getValue().getRef());
                    if (stopId == null) {
                        LOG.warn("No passengerStopAssignment found for " + scheduledStopPointRef
                                .getValue().getRef());
                    } else {
                        FlexibleQuayWithArea flexibleQuayWithArea = transitBuilder.getFlexibleQuayWithArea()
                                .get(AgencyAndIdFactory.createAgencyAndId(getFlexibleQuayRefFromStopPlaceRef(stopId)));
                        if (flexibleQuayWithArea == null) {
                            LOG.warn("Quay not found for " + scheduledStopPointRef.getValue()
                                    .getRef());
                        }
                        return flexibleQuayWithArea;
                    }
                }
            }
        }

        LOG.warn("Flexible quay with area not found");
        return null;
    }

    private String getFlexibleQuayRefFromStopPlaceRef(String stopPlaceRef) {
        return stopPlaceRef.replace("FlexibleStopPlace", "FlexibleQuay");
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef,
            JourneyPattern journeyPattern) {
        List<PointInLinkSequence_VersionedChildStructure> points = journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for (PointInLinkSequence_VersionedChildStructure point : points) {
            if (point instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if (stopPoint.getId().equals(pointInJourneyPatterRef)) {
                    return stopPoint;
                }
            }
        }
        return null;
    }

    private static int calculateOtpTime(TimeWithOffset timeWithOffset, List<TimeWithOffset> fallbackTimes) {
        if (timeWithOffset.time != null) {
            return calculateOtpTime(timeWithOffset.time, timeWithOffset.dayOffset);
        }
        else {
            for (TimeWithOffset fallBackTime : fallbackTimes) {
                if (fallBackTime.time != null) {
                    return calculateOtpTime(fallBackTime.time, fallBackTime.dayOffset);
                }
            }
            throw new IllegalArgumentException();
        }
    }

    static int calculateOtpTime(LocalTime time, BigInteger dayOffset) {
        int otpTime = time.toSecondOfDay();
        if (dayOffset != null) {
            otpTime += DAY_IN_SECONDS * dayOffset.intValue();
        }
        return otpTime;
    }

    private boolean isFalse(Boolean value) {
        return value != null && !value;
    }

    boolean isFlexible(ServiceJourney serviceJourney, NetexDao netexDao) {
        Line_VersionStructure line = TripMapper.lineFromServiceJourney(serviceJourney, netexDao);

        return line instanceof FlexibleLine;
    }

    /**
     * Only fixed or call-and-ride serviceJourney with two passingTimes currently supported.
     */

    boolean flexibleStructureSupported(ServiceJourney serviceJourney, NetexDao netexDao) {
        FlexibleLine flexibleLine = (FlexibleLine)TripMapper.lineFromServiceJourney(serviceJourney, netexDao);

        return (flexibleLine.getFlexibleLineType().value().equals("flexibleAreasOnly") &&
                serviceJourney.getPassingTimes().getTimetabledPassingTime().size() == 2)
                || (flexibleLine.getFlexibleLineType().value().equals("fixed"));
    }

    private class StopTimeWithBookingArrangement {

        StopTime stopTime;

        BookingArrangement bookingArrangement;

        public StopTimeWithBookingArrangement(StopTime stopTime, BookingArrangement bookingArrangement) {
            this.stopTime = stopTime;
            this.bookingArrangement = bookingArrangement;
        }
    }

    private Map<String, Geometry> getAreasById(OtpTransitBuilder transitBuilder ) {
        Map<String, Geometry> areasById = new HashMap<>();
        for (Area area : transitBuilder.getAreas()) {
            Geometry geometry = GeometryUtils.parseWkt(area.getWkt());
            areasById.put(area.getAreaId(), geometry);
        }
        return areasById;
    }

    private class TimeWithOffset {
        LocalTime time;
        BigInteger dayOffset;

        TimeWithOffset(LocalTime time, BigInteger dayOffset) {
            this.time = time;
            this.dayOffset = dayOffset;
        }
    }
}
