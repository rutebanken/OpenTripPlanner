package org.opentripplanner.graph_builder.model;

import org.rutebanken.netex.model.*;

import java.util.*;

public class NetexDao {

    private Map<String, StopPlace> stopPlaceMap = new HashMap<>();

    public Map<String, StopPlace> getStopPlaceMap() {
        return stopPlaceMap;
    }

    public Map<String, Quay> getQuayMap() {
        return quayMap;
    }

    private Map<String, Quay> quayMap = new HashMap<>();
    private Map<String, String> stopPointStopPlaceMap = new HashMap<>();
    private Map<String, String> stopPointQuayMap = new HashMap<>();
    private Map<String, ServiceJourneyPattern> journeyPatternsById = new HashMap<>();
    private Map<String, Route> routeById = new HashMap<>();
    private Map<String, Line> lineById = new HashMap<>();
    private Map<String, List<ServiceJourney>> serviceJourneyById = new HashMap<>();
    private Map<String, DayType> dayTypeById = new HashMap<>();
    private Map<String, List<DayTypeAssignment>> dayTypeAssignment = new HashMap<>();
    private Map<String, Boolean> dayTypeAvailable = new HashMap<>();
    private Map<String, OperatingPeriod> operatingPeriodById = new HashMap<>();
    private Map<String, Operator> operators = new HashMap<>();
    private Map<String, Authority> authorities = new HashMap<>();
    private Map<String, String> authoritiesByGroupOfLinesId = new HashMap<>();
    private Map<String, String> authoritiesByNetworkId = new HashMap<>();
    private Map<String, String> serviceIds = new HashMap<>();
    private Map<String, StopPlace> parentStopPlaceById = new HashMap<>();
    private Map<String, Notice> noticeMap = new HashMap<>();
    private Map<String, NoticeAssignment> noticeAssignmentMap = new HashMap<>();
    private Map<String, ServiceJourneyInterchange> interchanges = new HashMap<>();

    public Map<String, ServiceJourneyInterchange> getInterchanges() {
        return interchanges;
    }

    public Map<String, Notice> getNoticeMap() {
        return noticeMap;
    }

    public Map<String, NoticeAssignment> getNoticeAssignmentMap() {
        return noticeAssignmentMap;
    }

    public Map<String, StopPlace> getParentStopPlaceById() {
        return parentStopPlaceById;
    }


    private String timeZone;


    public Map<String, Boolean> getDayTypeAvailable() {
        return dayTypeAvailable;
    }

    public Map<String, String> getServiceIds() {
        return serviceIds;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Map<String, String> getStopPointStopPlaceMap() {
        return stopPointStopPlaceMap;
    }

    public Map<String, List<ServiceJourney>> getServiceJourneyById() {
        return serviceJourneyById;
    }

    public Map<String, ServiceJourneyPattern> getJourneyPatternsById() {
        return journeyPatternsById;
    }

    public Map<String, Line> getLineById() {
        return lineById;
    }

    public Map<String, Route> getRouteById() {
        return routeById;
    }

    public Map<String, List<DayTypeAssignment>> getDayTypeAssignment() {
        return dayTypeAssignment;
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

    public Map<String, Operator> getOperators() {
        return operators;
    }

    public Map<String, String> getAuthoritiesByGroupOfLinesId() {
        return authoritiesByGroupOfLinesId;
    }

    public Map<String, String> getAuthoritiesByNetworkId() {
        return authoritiesByNetworkId;
    }

    public Map<String, String> getStopPointQuayMap() {
        return stopPointQuayMap;
    }
}
