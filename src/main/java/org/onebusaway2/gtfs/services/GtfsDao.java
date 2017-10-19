package org.onebusaway2.gtfs.services;

import com.google.common.collect.Multimap;
import org.onebusaway2.gtfs.model.*;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Methods for accessing imported entities.
 */
public interface GtfsDao {

    Collection<Agency> getAllAgencies();

    Collection<ServiceCalendar> getAllCalendars();

    ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId);

    Collection<ServiceCalendarDate> getAllCalendarDates();

    List<ServiceCalendarDate> getCalendarDatesForServiceId(AgencyAndId serviceId);

    /** @return all ids for both Calendars and CalendarDates merged into on list without duplicates */
    List<AgencyAndId> getAllServiceIds();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    Collection<Frequency> getAllFrequencies();

    Collection<Pathway> getAllPathways();

    Collection<Route> getAllRoutes();

    Collection<ShapePoint> getAllShapePoints();

    List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId);

    Collection<Stop> getAllStops();

    Stop getStopForId(AgencyAndId id);

    Collection<StopTime> getAllStopTimes();

    /**
     * @return the list of {@link StopTime} objects associated with the trip,
     * sorted by {@link StopTime#getStopSequence()}
     */
    List<StopTime> getStopTimesForTrip(Trip trip);

    Collection<Transfer> getAllTransfers();

    Collection<Trip> getAllTrips();

    Map<AgencyAndId, Notice> getNoticeById();

    Map<AgencyAndId, NoticeAssignment> getNoticeAssignmentById();

    Multimap<StopPattern, TripPattern> getTripPatterns();

    List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId);

    List<Stop> getStopsForStation(Stop station);
}
