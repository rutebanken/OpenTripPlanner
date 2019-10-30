package org.opentripplanner.graph_builder.triptransformer.timetablereport;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class CsvLine implements Comparable<CsvLine> {
    private final static int MAX_PRINT_SIZE = 16;
    private static final String SEP = "; ";

    private final String tag;
    private final String agency;
    private final Route route;
    private final String fromStopId;
    private final String toStopId;
    private final String headsign;
    private final List<Departure> departures = new ArrayList<>();
    private final List<Departure> depDayBefore = new ArrayList<>();


    CsvLine(String tag, String fromStopId, String toStopId, Route route, String headsign) {
        this.tag = tag;
        this.agency = route.getAgency().getName();
        this.route = route;
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.headsign = headsign;
    }

    Route route() { return route; }
    String headsign() { return headsign; }
    String tag() { return tag; }

    void addDeparture(Trip trip, Integer tripDepartureTime) {
        departures.add(new Departure(trip, tripDepartureTime));
    }

    void addDepDayBefore(Trip trip, Integer tripDepartureTime) {
        depDayBefore.add(new Departure(trip, tripDepartureTime));
    }

    static String csvHeader() {
        return "Tag; Agency; Route id; From Quay; To Quay; Public code; Route; Headsign; Dep. day before; Departures";
    }

    private void sortDepartures() {
        Collections.sort(departures);
        Collections.sort(depDayBefore);
    }

    String toCsv(){
        sortDepartures();
        return (tag==null ? "" : tag) + SEP
                + agency + SEP
                + route.getId() + SEP
                + fromStopId + SEP
                + toStopId + SEP
                + route.getShortName() + SEP
                + route.getLongName() + SEP
                + headsign + SEP
                + timesToString(depDayBefore) + SEP
                + timesToString(departures) + SEP
                ;
    }

    private static String timesToString(Collection<Departure> departures) {
        return departures.stream()
                .limit(doLimit(departures.size()) ? MAX_PRINT_SIZE : 1_000)
                .map(Departure::timeToString)
                .collect(Collectors.joining(" "))
                + more(departures);
    }

    private static boolean doLimit(int size) {
        // allow 2 extra to be printed - take about the same place as: " .. (2 more)"
        return size > MAX_PRINT_SIZE + 2;
    }


    private static String more(Collection<Departure> list) {
        final int size = list.size();
        return doLimit(size) ? " .. (" + (size - MAX_PRINT_SIZE) + " more)" : "";
    }

    @Override
    public String toString() {
        return toCsv();
    }

    @Override
    public int compareTo(CsvLine other) {
        int c = compare(fromStopId, other.fromStopId);
        if(c == 0) c = compare(toStopId, other.toStopId);
        return c == 0 ? compare(tag, other.tag) : c;
    }

    private static int compare(String a, String b) {
        if(a==null) {
            if(b == null) return 0;
            else return -1;
        }
        if(b == null) return 1;
        return a.compareTo(b);
    }
}
