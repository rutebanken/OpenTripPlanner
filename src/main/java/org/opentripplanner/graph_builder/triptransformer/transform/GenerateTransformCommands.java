package org.opentripplanner.graph_builder.triptransformer.transform;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class GenerateTransformCommands {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTransformCommands.class);

    private final TransitServiceDecorator transit;
    private final ServiceDate serviceDate;


    public GenerateTransformCommands(ServiceDate date, OtpTransitBuilder transitService) {
        this.serviceDate = date;
        this.transit = new TransitServiceDecorator(transitService);
    }

    public void generateCmds() {
        try {
            shiftBeforeAndCopyTrips("100", 60, "3:14", "4:30");
            shiftBeforeAndCopyTrips("110", 60, "3:40", "4:46");
            shiftBeforeAndCopyTrips("300", 60, "4:14", "4:51");
            shiftBeforeAndCopyTrips("31", 46, "4:44", "4:45");
            shiftBeforeAndCopyTrips("11N", 60, "4:59", "4:49");
            shiftBeforeAndCopyTrips("12N", 60, "4:41", "4:34");

            // TODO CHECK THIS - One only?
            //      Line 2N -> Helsfyr T, input timetable: 4:59 5:29
            shiftBeforeAndCopyTrips("2N", -1, "3:29", "4:59", "3:22");

            // TODO CHECK THIS - Last departure is hanging
            //      Line 5N -> Jernbanetorget, input timetable: 1:12 1:42 2:12 2:42 3:12 4:42 5:12
            shiftBeforeAndCopyTrips("5N", -1, "4:42", "4:00");

            // TODO CHECK THIS - Is is ok to move these 2 random duplicates
            //      Line 20 -> Helsfyr, input timetable: 1:20 1:50 2:20 2:20 2:50 2:50 3:20 3:50 ... (more 2)
            shiftBeforeAndCopyTrips("20", 60, "2:50", "2:32");

            // TODO CHECK THIS - Is is ok to move these 2 random duplicates
            //      Line 21 -> Helsfyr T, input timetable: 1:25 1:55 2:25 2:25 2:55 2:55 3:25 ... (more 109)
            //      Line 21 -> Tjuvholmen, input timetable: 0:34 1:04 1:34 2:04 2:04 2:34 2:34 ... (more 106)
            shiftBeforeAndCopyTrips("21", 60, "2:55", "2:34");

            // TODO CHECK THIS - Is is ok to move these 2 random duplicates
            //      Line 37 -> Helsfyr, input timetable: 0:47 1:17 1:47 2:17 2:17 2:47 2:47 ... (more 104)
            //      Line 37 -> Nydalen T, input timetable: 1:05 1:35 2:05 2:05 2:35 2:35 3:05 ... (more 105)
            shiftBeforeAndCopyTrips("37", 60, "2:47", "2:35");

            // TODO CHECK THIS - Is is ok to move these 2 random duplicates
            //      Line 54 -> Tjuvholmen, input timetable: 0:38 1:08 1:38 2:08 2:08 2:38 2:38 ... (more 63)
            //      Line 54 -> Kjelsås stasjon, input timetable: 1:12 1:42 2:12 2:12 2:42 2:42 3:12 ... (more 72)
            shiftBeforeAndCopyTrips("54", 60, "2:38", "2:42");

            // All ok, except 400
            shiftBeforeNewOperationDayStarts("150", "160", "210", "220", "230", "340", "380", "390", "400");
            shiftBeforeNewOperationDayStarts("130N", "140N", "1N", "240N", "250N", "30N", "32N", "500N", "540N");
            shiftBeforeNewOperationDayStarts("63N", "3N", "4N", "70N");
        }
        catch (Exception e) {
            LOG.error("Unable to process input.", e);
        }
    }

    private void shiftBeforeNewOperationDayStarts(String ... publicCodes) {
        System.out.println();
        for (String publicCode : publicCodes) {
            System.out.println();
            shiftBeforeOperationDayStarts(publicCode);
        }
    }

    private void shiftBeforeAndCopyTrips(String publicCode, int copyLastMinutes, String ... times) {
        System.out.println();
        for (String time : times) {
            shiftBeforeAddTrips(publicCode, copyLastMinutes, time);
        }
    }

    int gap(List<TripDeparture> tds, int idx) {
        return tds.get(idx+1).departureTime - tds.get(idx).departureTime;
    }


    private void shiftBeforeOperationDayStarts(String publicCode) {
        List<TripDeparture> trips = transit.findAllTripsByPublicCode(serviceDate, publicCode);
        Multimap<String, TripDeparture> tripBySign = ArrayListMultimap.create();

        for (TripDeparture it : trips) tripBySign.put(it.trip.getTripHeadsign(), it);

        for (String headSign : tripBySign.keySet()) {
            List<TripDeparture> tds = new ArrayList<>(tripBySign.get(headSign));

            Collections.sort(tds);

            // First departure is after 05:00
            if(tds.get(0).departureTime > 5 * 3600) continue;

            int includePos = -1;
            int maxGap = -1;

            // Last departure is before 05:22
            if(tds.get(tds.size()-1).departureTime < 5 * 3600 + 22 * 60) {
                includePos = tds.size()-1;
            }
            else {
                maxGap = tds.get(0).departureTime;
                if(tds.size() < 2) {
                    System.err.println("Unexpected size of trips: " + headSign + " " + tds);
                    continue;
                }
                for (int i = 0; i < tds.size() - 1 && tds.get(i).departureTime < 8 * 3600; i++) {
                    int gap = gap(tds, i);
                    if (gap > maxGap) {
                        maxGap = gap;
                        includePos = i;
                    }
                }
                if (includePos == -1 || tds.get(includePos).departureTime > 8 * 3600) {
                    System.out.println("# Line " + publicCode + " -> " + headSign +  " input timetable: " + depTimesShort(tds, includePos));
                    System.err.println("Gap not found!. pos:" + includePos + ", maxGap" + maxGap + ", trips: " + tds);
                    continue;
                }
            }
            List<TripDeparture> mv = tds.subList(0, includePos+1);
            String lineId = tds.get(0).trip.getRoute().getId().getId();


            System.out.println("# Line " + publicCode + " -> " + headSign +  " input timetable: " + depTimesShort(tds, includePos));
            System.out.println("MoveTimeShift; " + lineId + "; " + headSign + "; -1h; " + depTimes(mv));
        }
    }

    private void shiftBeforeAddTrips(String publicCode, int copyLastMinutes, String departureTimeST) {
        int hits = 0;
        List<TripDeparture> trips = transit.findAllTripsByPublicCode(serviceDate, publicCode);
        Multimap<String, TripDeparture> tripBySign = ArrayListMultimap.create();

        int maxTime = TripTransformerTimeUtil.timeInSec(departureTimeST);
        int cpTimeLimit = maxTime - 60 * copyLastMinutes;

        for (TripDeparture it : trips) tripBySign.put(it.trip.getTripHeadsign(), it);

        for (String headSign : tripBySign.keySet()) {
            List<TripDeparture> tds = new ArrayList<>(tripBySign.get(headSign));
            List<TripDeparture> cp = new ArrayList<>();
            List<TripDeparture> mv = new ArrayList<>();

            Collections.sort(tds);
            String lineId = tds.get(0).trip.getRoute().getId().getId();
            int pos;
            for (pos = 0; pos < tds.size(); ++pos) {
                TripDeparture trip = tds.get(pos);
                if(!trip.trip.getRoute().getId().getId().equals(lineId)) throw new IllegalArgumentException();
                if(trip.departureTime <= cpTimeLimit) mv.add(trip);
                else cp.add(trip);
                if(trip.departureTime == maxTime) {
                    break;
                }
            }
            if(pos < tds.size()) {
                ++hits;
                System.out.println("# Line " + publicCode + " -> " + headSign +  " input timetable: " + depTimesShort(tds, pos));
                System.out.println("MoveTimeShift; " + lineId + "; " + headSign + "; -1h; " + depTimes(mv));
                if(!cp.isEmpty()) System.out.println("CopyTimeShift; " + lineId + "; " + headSign + "; -1h; " + depTimes(cp));
            }
        }

        if(hits != 1) {
            throw new IllegalStateException(
                    "Not able to generate cmd for Line " + publicCode
                            + ", cpMin: " + copyLastMinutes + ", depTimeST:  " + departureTimeST
            );
        }
    }

    private String depTimes(List<TripDeparture> tripDepartures) {
        return tripDepartures.stream()
                .map(it -> TripTransformerTimeUtil.timeToString(it.departureTime))
                .collect(Collectors.joining("; "));
    }

    private String depTimesShort(List<TripDeparture> tripDepartures, int pos) {
        int limit = pos + 5;
        int rest = tripDepartures.size() - limit;

        return tripDepartures.stream()
                .limit(limit)
                .map(it -> TripTransformerTimeUtil.timeToString(it.departureTime))
                .collect(Collectors.joining(" "))
                + (rest > 0 ? " ... (more " + rest + ")" : "") ;
    }
}
