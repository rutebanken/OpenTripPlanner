/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new ConcurrentHashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByRoute = new ConcurrentHashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByStop = new ConcurrentHashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndRoute = new ConcurrentHashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndTrip = new ConcurrentHashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByTrip = new ConcurrentHashMap<>();
    private Map<String, Set<AlertPatch>> patchesByAgency = new ConcurrentHashMap<>();
    private Map<String, Set<AlertPatch>> patchesByTripPattern = new ConcurrentHashMap<>();

    public AlertPatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<AlertPatch> getAllAlertPatches() {
        return alertPatches.values();
    }

    @Override
    public AlertPatch getPatchById(String id) {
        return alertPatches.get(id);
    }

    @Override
    public Collection<AlertPatch> getStopPatches(AgencyAndId stop) {
        Set<AlertPatch> result = patchesByStop.get(stop);
        if (result == null || result.isEmpty()) {
            result = new HashSet<>();
            // Search for alerts on parent-stop
            if (graph != null && graph.index != null) {
                Stop quay = graph.index.stopForId.get(stop);
                if (quay != null) {
                    if ( quay.getParentStation() != null) {
                        // Add alerts for parent-station
                        result.addAll(patchesByStop.getOrDefault(quay.getParentStationAgencyAndId(), Collections.emptySet()));
                    }
                    if (quay.getMultiModalStation() != null) {
                        // Add alerts for multimodal-station
                        result.addAll(patchesByStop.getOrDefault(new AgencyAndId(stop.getAgencyId(), quay.getMultiModalStation()), Collections.emptySet()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(AgencyAndId route) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByRoute.containsKey(route)) {
            result.addAll(patchesByRoute.get(route));
        }

        return result;

    }

    @Override
    public Collection<AlertPatch> getTripPatches(AgencyAndId trip) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByTrip.containsKey(trip)) {
            result.addAll(patchesByTrip.get(trip));
        }
        return result;
    }


    @Override
    public Collection<AlertPatch> getAgencyPatches(String agency) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByAgency.containsKey(agency)) {
            result.addAll(patchesByAgency.get(agency));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndRoutePatches(AgencyAndId stop, AgencyAndId route) {
        Set<AlertPatch> result = new HashSet<>();
        StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, route);
        if (patchesByStopAndRoute.containsKey(key)) {
            result.addAll(patchesByStopAndRoute.get(key));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndTripPatches(AgencyAndId stop, AgencyAndId trip) {
        Set<AlertPatch> result = new HashSet<>();
        StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, trip);
        if (patchesByStopAndTrip.containsKey(key)) {
            result.addAll(patchesByStopAndTrip.get(key));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getTripPatternPatches(TripPattern pattern) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByTripPattern.containsKey(pattern.code)) {
            result.addAll(patchesByTripPattern.get(pattern.code));
        }
        return result;
    }

    @Override
    public synchronized void applyAll(Set<AlertPatch> alertPatches) {
        for (AlertPatch alertPatch : alertPatches) {
            apply(alertPatch);
        }
    }

    @Override
    public synchronized void apply(AlertPatch alertPatch) {
        if (alertPatches.containsKey(alertPatch.getId())) {
            expire(alertPatches.get(alertPatch.getId()));
        }

        alertPatch.apply(graph);
        alertPatches.put(alertPatch.getId(), alertPatch);

        AgencyAndId stop = alertPatch.getStop();
        AgencyAndId route = alertPatch.getRoute();
        AgencyAndId trip = alertPatch.getTrip();

        if (stop != null && trip != null) {
            StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, trip);
            Set<AlertPatch> set = patchesByStopAndTrip.getOrDefault(key, new HashSet());
            set.add(alertPatch);
            patchesByStopAndTrip.put(key, set);
        } else if (stop != null && route != null) {
            StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, route);
            Set<AlertPatch> set = patchesByStopAndRoute.getOrDefault(key, new HashSet());
            set.add(alertPatch);
            patchesByStopAndRoute.put(key, set);
        } else {
            if (stop != null) {
                Set<AlertPatch> set = patchesByStop.getOrDefault(stop, new HashSet());
                set.add(alertPatch);
                patchesByStop.put(stop, set);
            }

            if (route != null) {
                Set<AlertPatch> set = patchesByRoute.getOrDefault(route, new HashSet());
                set.add(alertPatch);
                patchesByRoute.put(route, set);
            }

            if (trip != null) {
                Set<AlertPatch> set = patchesByTrip.getOrDefault(trip, new HashSet());
                set.add(alertPatch);
                patchesByTrip.put(trip, set);
            }
        }

        String agency = alertPatch.getAgency();
        if (agency != null && !agency.isEmpty()) {
            Set<AlertPatch> set = (Set) patchesByAgency.getOrDefault(agency, new HashSet());
            set.add(alertPatch);
            patchesByAgency.put(agency, set);
        }

        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                Set<AlertPatch> set = patchesByTripPattern.getOrDefault(pattern.code, new HashSet());
                set.add(alertPatch);
                patchesByTripPattern.put(pattern.code, set);
            }
        }

    }


    @Override
    public void expire(Set<String> purge) {
        for (String patchId : purge) {
            if (alertPatches.containsKey(patchId)) {
                expire(alertPatches.get(patchId));
            }
        }

        alertPatches.keySet().removeAll(purge);
    }

    @Override
    public void expireAll() {
        for (AlertPatch alertPatch : alertPatches.values()) {
            expire(alertPatch);
        }
        alertPatches.clear();
    }

    @Override
    public void expireAllExcept(Set<String> retain) {
        ArrayList<String> toRemove = new ArrayList<String>();

        for (Entry<String, AlertPatch> entry : alertPatches.entrySet()) {
            final String key = entry.getKey();
            if (!retain.contains(key)) {
                toRemove.add(key);
                expire(entry.getValue());
            }
        }
        alertPatches.keySet().removeAll(toRemove);
    }

    private void expire(AlertPatch alertPatch) {
        AgencyAndId stop = alertPatch.getStop();
        AgencyAndId route = alertPatch.getRoute();
        AgencyAndId trip = alertPatch.getTrip();

        if (stop != null) {
            removeAlertPatch(patchesByStop.get(stop), alertPatch);
        }

        if (route != null) {
            removeAlertPatch(patchesByRoute.get(route), alertPatch);
        }

        if (trip != null) {
            removeAlertPatch(patchesByTrip.get(trip), alertPatch);
        }

        if (stop != null && route != null) {
            removeAlertPatch(patchesByStopAndRoute.get(new StopAndRouteOrTripKey(stop, route)), alertPatch);
        }

        if (stop != null && trip != null) {
            removeAlertPatch(patchesByStopAndTrip.get(new StopAndRouteOrTripKey(stop, trip)), alertPatch);
        }

        String agency = alertPatch.getAgency();
        if (agency != null) {
            removeAlertPatch(patchesByAgency.get(agency), alertPatch);
        }

        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                removeAlertPatch(patchesByTripPattern.get(pattern), alertPatch);
            }
        }
        alertPatch.remove(graph);
    }

    private void removeAlertPatch(Set<AlertPatch> alertPatches, AlertPatch alertPatch) {

        if (alertPatches != null) {
            alertPatches.remove(alertPatch);
        }
    }

    private class StopAndRouteOrTripKey {
        private final AgencyAndId stop;
        private final AgencyAndId routeOrTrip;
        private transient int hash = 0;

        public StopAndRouteOrTripKey(AgencyAndId stop, AgencyAndId routeOrTrip) {
            this.stop = stop;
            this.routeOrTrip = routeOrTrip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StopAndRouteOrTripKey that = (StopAndRouteOrTripKey) o;

            if (!stop.equals(that.stop)) {
                return false;
            }
            return routeOrTrip.equals(that.routeOrTrip);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                int result = stop.hashCode();
                hash = 31 * result + routeOrTrip.hashCode();
            }
            return hash;
        }
    }
}
