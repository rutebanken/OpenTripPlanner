/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation; either version 3 of
 the License; or (at your option) any later version.

 This program is distributed in the hope that it will be useful;
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not; see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.model.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.*;
import org.opentripplanner.netex.mapping.AgencyAndIdFactory;
import org.opentripplanner.netex.mapping.FlexibleQuayWithArea;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class OtpTransitBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OtpTransitBuilder.class);

    private final List<Agency> agencies = new ArrayList<>();

    private final List<Area> areas = new ArrayList<>();

    private final List<ServiceCalendarDate> calendarDates = new ArrayList<>();

    private final List<ServiceCalendar> calendars = new ArrayList<>();

    private final List<FareAttribute> fareAttributes = new ArrayList<>();

    private final List<FareRule> fareRules = new ArrayList<>();

    private final List<FeedInfo> feedInfos = new ArrayList<>();

    private final EntityMap<AgencyAndId, FlexibleQuayWithArea> flexibleStopsWithArea = new EntityMap<>();

    private final List<Frequency> frequencies = new ArrayList<>();

    private final EntityMap<AgencyAndId, Stop> groupsOfStopPlaces = new EntityMap<>();

    private final ListMultimap<Stop, Stop> stopByGroupOfStopPlaces = ArrayListMultimap.create();

    private final List<Parking> parkings = new ArrayList<>();

    private final EntityMap<AgencyAndId, Stop> multiModalStops = new EntityMap<>();

    private final ListMultimap<Stop, Stop> stationsByMultiModalStop = ArrayListMultimap.create();

    private final EntityMap<AgencyAndId, Notice> noticesById = new EntityMap<>();

    private final EntityMap<AgencyAndId, NoticeAssignment> noticeAssignmentsById = new EntityMap<>();

    private final EntityMap<AgencyAndId, Branding> brandingById = new EntityMap<>();

    private final EntityMap<AgencyAndId, Operator> operatorsById = new EntityMap<>();

    private final List<Pathway> pathways = new ArrayList<>();

    private final EntityMap<AgencyAndId, Route> routesById = new EntityMap<>();

    private final ListMultimap<AgencyAndId, ShapePoint> shapePoints = ArrayListMultimap.create();

    private final EntityMap<AgencyAndId, Stop> stopsById = new EntityMap<>();

    private final SortedMultimap<Trip, StopTime> stopTimesByTrip = new SortedMultimap<>();

    private final EntityMap<AgencyAndId, TariffZone> tariffZonesById = new EntityMap<>();

    private final List<Transfer> transfers = new ArrayList<>();

    private final EntityMap<AgencyAndId, Trip> trips = new EntityMap<>();

    private final ListMultimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();


    /* Accessors */

    public List<Agency> getAgencies() {
        return agencies;
    }

    public List<Area> getAreas() { return areas; }

    public Agency findAgencyById(String id) {
        return agencies.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    public List<ServiceCalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<ServiceCalendar> getCalendars() {
        return calendars;
    }

    public List<FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public List<FeedInfo> getFeedInfos() {
        return feedInfos;
    }

    public EntityMap<AgencyAndId, FlexibleQuayWithArea> getFlexibleQuayWithArea() { return flexibleStopsWithArea; }

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public EntityMap<AgencyAndId, Stop> getGroupsOfStopPlaces() {
        return groupsOfStopPlaces;
    }

    public ListMultimap<Stop, Stop> getStopByGroupOfStopPlaces() {
        return stopByGroupOfStopPlaces;
    }

    public EntityMap<AgencyAndId, Stop> getMultiModalStops() {
        return multiModalStops;
    }

    public EntityMap<AgencyAndId, Notice> getNoticesById() {
        return noticesById;
    }

    public EntityMap<AgencyAndId, NoticeAssignment> getNoticeAssignmentsById() {
        return noticeAssignmentsById;
    }

    public EntityMap<AgencyAndId, Branding> getBrandingById() {
        return brandingById;
    }

    public EntityMap<AgencyAndId, Operator> getOperatorsById() {
        return operatorsById;
    }

    public List<Parking> getParkings() { return parkings; }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public EntityMap<AgencyAndId, Route> getRoutes() {
        return routesById;
    }

    public Multimap<AgencyAndId, ShapePoint> getShapePoints() {
        return shapePoints;
    }

    public ListMultimap<Stop, Stop> getStationsByMultiModalStop() {
        return stationsByMultiModalStop;
    }

    public EntityMap<AgencyAndId, Stop> getStops() {
        return stopsById;
    }

    public SortedMultimap<Trip, StopTime> getStopTimesSortedByTrip() {
        return stopTimesByTrip;
    }

    public EntityMap<AgencyAndId, TariffZone> getTariffZones() {
        return tariffZonesById;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public EntityMap<AgencyAndId, Trip> getTrips() {
        return trips;
    }

    public Multimap<StopPattern, TripPattern> getTripPatterns() {
        return tripPatterns;
    }

    /**
     * Find all serviceIds in both CalendarServices and CalendarServiceDates.
     */
    Set<AgencyAndId> findAllServiceIds() {
        Set<AgencyAndId> serviceIds = new HashSet<>();
        for (ServiceCalendar calendar : getCalendars()) {
            serviceIds.add(calendar.getServiceId());
        }
        for (ServiceCalendarDate date : getCalendarDates()) {
            serviceIds.add(date.getServiceId());
        }
        return serviceIds;
    }

    public OtpTransitService build() {
        createNoneExistentIds();

        return new OtpTransitServiceImpl(this);
    }

    public void removeStopsNotInUse() {
        // Quays are location type 0
        Set<Stop> quays = stopsById.values().stream().filter(s -> s.getLocationType() == 0).collect(Collectors.toSet());

        // StopPlaces are location type 0 except multimodal and groupsOfStopPlaces
        Set<Stop> stopPlaces = stopsById.values().stream().filter(s -> s.getLocationType() == 1
                && !multiModalStops.containsKey(s.getId()) && !groupsOfStopPlaces.containsKey(s.getId())).collect(Collectors.toSet());

        // Create quaysByStopPlace
        ListMultimap<Stop, Stop> quaysByStopPlace = ArrayListMultimap.create();
        for (Stop stop : quays) {
            quaysByStopPlace.put(stopsById.get(AgencyAndIdFactory.createAgencyAndId(stop.getParentStation())), stop);
        }

        // Find quays not used in tripPatterns
        Set<Stop> quaysNotInUse = new HashSet<>(quays);
        for (StopPattern stopPattern : tripPatterns.keySet()) {
            for (Stop stop : stopPattern.stops) {
                quaysNotInUse.remove(stop);
            }
        }

        // Also filter by expired
        quaysNotInUse.removeIf(q -> !q.isExpired());

        // Find stopPlaces not used
        Set<Stop> stopPlacesNotInUse = new HashSet<>(stopPlaces);
        for (Map.Entry<Stop, Collection<Stop>> entry : quaysByStopPlace.asMap().entrySet()) {
            if (entry.getValue().stream().anyMatch(s -> !quaysNotInUse.contains(s))) {
                stopPlacesNotInUse.remove(entry.getKey());
            }
        }

        // Find stop places not used in tripPatterns
        for (StopPattern stopPattern : tripPatterns.keySet()) {
            for (Stop stop : stopPattern.stops) {
                stopPlacesNotInUse.remove(stop);
            }
        }

        // Remove  multimodal stopPlaces not used
        Set<Stop> multiModalStopPlacesNotInUse = new HashSet<>(multiModalStops.values());
        for (Map.Entry<Stop, Collection<Stop>> entry : stationsByMultiModalStop.asMap().entrySet()) {
            if (entry.getValue().stream().anyMatch(s -> !stopPlacesNotInUse.contains(s))) {
                multiModalStopPlacesNotInUse.remove(entry.getKey());
            }
        }

        // Remove  groupOfStopPlaces not used
        Set<Stop> groupOfStopPlacesNotInUse = new HashSet<>(groupsOfStopPlaces.values());
        for (Map.Entry<Stop, Collection<Stop>> entry : stopByGroupOfStopPlaces.asMap().entrySet()) {
            if (entry.getValue().stream().anyMatch(s -> !stopPlacesNotInUse.contains(s))
                    && entry.getValue().stream().anyMatch(s -> !multiModalStopPlacesNotInUse.contains(s))) {
                groupOfStopPlacesNotInUse.remove(entry.getKey());
            }
        }

        // Remove quays
        quaysNotInUse.forEach(stopsById::remove);

        LOG.info(quaysNotInUse.size() + " quays removed (expired and not in use)");

        // Remove stopPlaces
        stopPlacesNotInUse.forEach(stopsById::remove);

        LOG.info(stopPlacesNotInUse.size() + " stopPlaces removed (expired and not in use)");

        // Remove multimodal stopPlaces
        multiModalStopPlacesNotInUse.forEach(m -> {
            stopsById.remove(m);
            multiModalStops.remove(m);
            stationsByMultiModalStop.removeAll(m);
        });

        LOG.info(multiModalStopPlacesNotInUse.size() + " multimodal stopPlaces removed (expired and not in use)");

        // Remove groupsOfStopPlaces
        groupOfStopPlacesNotInUse.forEach(g -> {
            stopsById.remove(g);
            groupsOfStopPlaces.remove(g);
            stopByGroupOfStopPlaces.removeAll(g);
        });

        LOG.info(groupOfStopPlacesNotInUse.size() + " groupsOfStopPlaces removed (expired and not in use)");
    }

    private void createNoneExistentIds() {
        generateNoneExistentIds(feedInfos);
    }

    static <T extends IdentityBean<String>> void generateNoneExistentIds(List<T> entities) {
        int maxId = 0;


        for (T it : entities) {
            try {
                if(it.getId() != null) {
                    maxId = Math.max(maxId, Integer.parseInt(it.getId()));
                }
            } catch (NumberFormatException ignore) {}
        }

        for (T it : entities) {
            try {
                if(it.getId() == null || Integer.parseInt(it.getId()) == 0) {
                    it.setId(Integer.toString(++maxId));
                }
            }
            catch (NumberFormatException ignore) { }
        }
    }

    public void regenerateIndexes() {
        trips.reindex();
        this.stopsById.reindex();
        this.routesById.reindex();
        this.stopTimesByTrip.reindex();
    }
}
