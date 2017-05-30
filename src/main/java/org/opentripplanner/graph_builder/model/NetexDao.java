package org.opentripplanner.graph_builder.model;

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Trip;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;

import java.util.*;

public class NetexDao {

    private Map<String, Stop> stopsById = new HashMap<>();
    private Map<String, String> stopPointStopPlaceMap = new HashMap<>();
    private Map<String, String> stopPointQuayMap = new HashMap<>();
    private Map<String, JourneyPattern> journeyPatternsById = new HashMap<>();
    private Map<String, Route> routeById = new HashMap<>();
    private Map<String, Line> lineById = new HashMap<>();
    private Map<String, org.onebusaway2.gtfs.model.Route> otpRouteById = new HashMap<>();
    private Map<String, List<ServiceJourney>> serviceJourneyById = new HashMap<>();
    private List<TripPattern> tripPatterns = new ArrayList<>();
    private List<Trip> trips = new ArrayList<>();
    private Map<Trip, List<StopTime>> stopTimesForTrip = new HashMap<>();
    private Set<AgencyAndId> serviceIds = new HashSet<>();
    private Map<String, DayType> dayTypeById = new HashMap<>();
    private Map<String, Object> dayTypeAssignment = new HashMap<>();
    private Map<String, OperatingPeriod> operatingPeriodById = new HashMap<>();
    private Map<String, Operator> operators = new HashMap<>();
    private Map<String, Authority> authorities = new HashMap<>();
    private Map<String , String> authoritiesByGroupOfLinesId = new HashMap<>();
    private List<Agency> agencies = new ArrayList<>();
    private String timeZone;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Map<String, Stop> getStopsById() {
        return stopsById;
    }

    public Map<String, String> getStopPointStopPlaceMap() {
        return stopPointStopPlaceMap;
    }

    public Map<String, List<ServiceJourney>> getServiceJourneyById() {
        return serviceJourneyById;
    }

    public Collection<Stop> getAllStops(){
        return stopsById.values();
    }

    public Map<String, JourneyPattern> getJourneyPatternsById() {
        return journeyPatternsById;
    }

    public Map<String, Line> getLineById() {
        return lineById;
    }

    public Map<String, Route> getRouteById() {
        return routeById;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }

    public List<TripPattern> getTripPatterns() {
        return tripPatterns;
    }

    public Map<Trip, List<StopTime>> getStopTimesForTrip() {
        return stopTimesForTrip;
    }

    public Set<AgencyAndId> getServiceIds() {
        return serviceIds;
    }

    public Map<String, Object> getDayTypeAssignment() {
        return dayTypeAssignment;
    }

    public Map<String, org.onebusaway2.gtfs.model.Route> getOtpRouteById() {
        return otpRouteById;
    }

    public Map<String, Operator> getOperators() {
        return operators;
    }

    public List<Agency> getAllAgencies(){
        return agencies;
    }

    public Map<String, DayType> getDayTypeById() {
        return dayTypeById;
    }

    public Map<String, OperatingPeriod> getOperatingPeriodById() {
        return operatingPeriodById;
    }

    public Map<String, Authority> getAuthorities() {
        return authorities;
    }

    public Map<String, String> getAuthoritiesByGroupOfLinesId() {
        return authoritiesByGroupOfLinesId;
    }

    public void clearJourneyPatterns(){
        journeyPatternsById = new HashMap<>();
    }

    public Map<String, String> getStopPointQuayMap() {
        return stopPointQuayMap;
    }

    public void setStopPointQuayMap(Map<String, String> stopPointQuayMap) {
        this.stopPointQuayMap = stopPointQuayMap;
    }
}
