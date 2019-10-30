package org.opentripplanner.graph_builder.triptransformer.timetablereport;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.triptransformer.transform.StopTimesWrapper;
import org.opentripplanner.graph_builder.triptransformer.transform.TransitServiceDecorator;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;



/**
 * This service is used to identify, print and modify services/routes. One of the use cases is to deal with
 * the effectof daylight saving time adjustments.
 * <p>
 * - It can be used to extract a csv file of all routes that are affected by DST.
 * - It can move and copy trips to compansate.
 * - It ca
 * <p/>
 * TODO Improvements: Add the possibility to filter on route public id.
 */
public class PrintTripTimetableReport {
    private final TransitServiceDecorator transit;
    private final List<CsvLine> lines = new ArrayList<>();
    private final ServiceDate serviceDate;
    private final int startTime;
    private final int endTime;
    private final boolean includePreviousDay;

    /**
     * @param transitService The service to querry for trip, stop-times and so on.
     * @param serviceDate The service date to use for the report.
     * @param startTime The minimum time of first trip to include in the report. Can be negative to reference a time
     *                  the previous service day, if so the time is converted by adding 24 hours and the time zone of
     *                  the previous day is used. Any Daylight Saving Time adjustments are ignored. Legal values are
     *                  -24:00 to 48:00.
     * @param endTime The time of the last trip to include in the report. Must be a valid service time: 0:00 to 48:00.
     * @param includePreviousDay Note that operation day may continue into the next day, 24:30 and 47:59 is legal.
     *                           Setting this to true include a column in the report for the previous service day
     *                           and list all trips within the time-period specified.
     */
    public PrintTripTimetableReport(
            OtpTransitBuilder transitService,
            ServiceDate serviceDate,
            String startTime,
            String endTime,
            boolean includePreviousDay
    ) {
        this.transit = new TransitServiceDecorator(transitService);
        this.serviceDate = serviceDate;
        this.startTime = TripTransformerTimeUtil.timeInSec(startTime);
        this.endTime = TripTransformerTimeUtil.timeInSec(endTime);
        this.includePreviousDay = includePreviousDay;

        if(this.startTime > this.endTime) {
            throw new IllegalArgumentException("Start time is after end time. Start: " + startTime + ", end: " + endTime);
        }
    }

    public void collectData(String tag) {
        Function<Trip, Boolean> currDayFilter;
        if(startTime < 0) {
            // We do not need to check if service time is less then 0, it is not allowed.
            // When DST is adjusted in spring this is strictly not correct, but we only include "illegal"
            // trip, so it might help detecting that.
            currDayFilter = (t) -> transit.departureTime(t) < endTime;
        }
        else {
            currDayFilter = (t) -> {
                int d = transit.departureTime(t);
                return d > startTime && d < endTime;
            };
        }

        if(includePreviousDay) {
            ServiceDate prevDay = TripTransformerTimeUtil.dayBefore(serviceDate);
            Function<Trip, Boolean> prevDayFilter;
            prevDayFilter = (t) -> transit.departureTime(t) > (startTime + 24 * 3600);

            lines.addAll(
                    mapOfCsvLines(
                            tag,
                            transit.tripsByRoute(serviceDate, currDayFilter, false),
                            transit.tripsByRoute(prevDay, prevDayFilter, false)
                    )
            );
        }
        else {
            lines.addAll(
                    mapOfCsvLines(
                            tag,
                            transit.tripsByRoute(serviceDate, currDayFilter, false),
                            ArrayListMultimap.create()
                    )
            );
        }
    }

    public void print(String filename) {
        printTripsOperatingInPeriod(filename, lines);
    }

    public void printTrip(ServiceDate date, AgencyAndId routeId, int departureTimeMin) {
        List<CsvLine> lines = mapOfCsvLines(
                null,
                transit.tripsByRoute(date, (t) -> filterTimeShift(t, routeId, departureTimeMin), false),
                ArrayListMultimap.create()
        );
        System.out.println("\n---------------------------------------");
        printTripsOperatingInPeriod(null, lines);
        System.out.println("---------------------------------------\n");
    }

    private boolean filterTimeShift(Trip t, AgencyAndId routeId, int depatureTime) {
        return t.getRoute().getId().equals(routeId) && transit.departureTime(t) == depatureTime;
    }

    private List<CsvLine> mapOfCsvLines(
            String tag,
            Multimap<Route, Trip> tripsByRouteDst,
            Multimap<Route, Trip> tripsByRouteDayBeforeDst
    ) {
        List<CsvLine> lines = new ArrayList<>();
        List<Route> routes = new ArrayList<>(tripsByRouteDst.keySet());
        routes.sort(new RouteComparator());

        for (Route route : routes) {
            final Multimap<String, StopTimesWrapper> tripNextDay = mapTripByJourneyPatternKey(tripsByRouteDst.get(route));
            final Multimap<String, StopTimesWrapper> tripPrevDay = mapTripByJourneyPatternKey(tripsByRouteDayBeforeDst.get(route));

            for (String jpKey : tripNextDay.keySet()) {
                StopTimesWrapper aStopTimes = tripNextDay.get(jpKey).iterator().next();
                String fromStopId = aStopTimes.originStopId();
                String toStopId = aStopTimes.destinationStopId();

                CsvLine csvLine = new CsvLine(tag, fromStopId, toStopId, route, aStopTimes.trip.getTripHeadsign());
                lines.add(csvLine);

                for (StopTimesWrapper st : tripNextDay.get(jpKey)) {
                    if(!st.startAndEndsAt(fromStopId, toStopId)) {
                        throw new IllegalStateException(
                                "To of the trips in this line have different from/to stops:"
                                + "\n\t" + aStopTimes
                                + "\n\t" + st
                        );
                    }
                    csvLine.addDeparture(
                            st.trip,
                            st.departureTime()
                    );
                }
                Collection<StopTimesWrapper> tripsDayBeforeDst = tripPrevDay.get(jpKey);
                if(tripsDayBeforeDst != null) {
                    for (StopTimesWrapper st : tripsDayBeforeDst) {
                        csvLine.addDepDayBefore(st.trip, st.departureTime());
                    }
                }
            }
        }
        return lines;
    }

    private void printTripsOperatingInPeriod(String filename, List<CsvLine> lines) {
        try {
            List<Route> routes = lines.stream()
                    .map(CsvLine::route)
                    .distinct()
                    .sorted(new RouteComparator())
                    .collect(Collectors.toList());

            PrintWriter out = filename != null
                    ? new PrintWriter(new File(filename))
                    : new PrintWriter(System.out, true);

            out.println(CsvLine.csvHeader());

            ListMultimap<Route, CsvLine> linesByRoute =  ArrayListMultimap.create();
            for (CsvLine line : lines) {
                linesByRoute.put(line.route(), line);
            }

            for (Route route : routes) {
                List<CsvLine> sortedLines = linesByRoute.get(route).stream().sorted().collect(Collectors.toList());
                for (CsvLine it : sortedLines) {
                    out.println(it.toCsv());
                }
            }
            if(filename != null) {
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Multimap<String, StopTimesWrapper> mapTripByJourneyPatternKey(Collection<Trip> trips) {
        final Multimap<String, StopTimesWrapper> tripByJourneyPattern = ArrayListMultimap.create();
        for (Trip trip : trips) {
            StopTimesWrapper st = transit.stopTimes(trip);
            tripByJourneyPattern.put(st.journeyPatterKey(), st);
        }
        return tripByJourneyPattern;
    }
}
