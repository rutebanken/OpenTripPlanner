package org.opentripplanner.graph_builder.triptransformer.transform;

import org.opentripplanner.graph_builder.triptransformer.TripTransformService;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;


/**
 * This service is used to patch (copy/move) trip using a "TransitTransform.txt" file.
 * Put the file in the same directory as the GTFS or Netex Zip file and make sure the
 * GTFS/Netex module run this service after the ServiceCalendar and transit data is
 * loaded. (Currently the {@link TripTransformService} is only invoked from the Netex
 * import, but if enabled in the GTFS import it would work there too.)
 * <p>
 * The "TransitTransform.txt" should be UTF-8 encoded and use the following format:
 * <p>
 * Transform transit timetable using some simple commands
 * <p>
 * <p>
 * COMMANDS
 * <ul>
 * <li>{@code applyOnDate; [yyyy-mm-dd]} All commands after this line will apply the command on trips found
 * on this DATE. This command must be run at least once, and before any copy/move command.
 * <li>{@code CopyFromTo; [Route ID]; [Headsign]; [Source time]; [Target time]} Copy a trip with the given
 * [Route ID], [Headsign] and/or [source time] to the given [target time]. All stop times are moved with the same
 * delta as the depature time.
 * <li>{@code CopyTimeShift; [Route ID]; [Headsign]; [Time shift]; [List of source times]} Copy a trip with the
 * to the given [target time]. All stop times are moved with the same delta as the depature time.
 * <li>{@code MoveTimeShift; [Route ID]; [Headsign]; [Time shift]; [List of source times]} Copy a trip with the
 * given [Route ID], [Headsign] and/or [source time] to the given [target time]. All stop times are moved with
 * the same delta as the depature time.
 * <p>
 * <p>
 * ARGUMENTS
 * <ul>
 * <li>{@code [yyyy-mm-dd]} Service date in ISO8601 format, using the feed timezone.
 * <li>{@code [Route ID]}    The Line ID (Netex) or Route ID (GTFS) witch the trip (source) is part of. Without
 * feed id prefix.
 * <li>{@code [Headsign]}    The headsign (GTFS) or DestinationDisplay (Netex) for the trip used. If a '*' is
 * used ALL trips are selected.
 * <li>{@code [Source time]} The source trip depature time used to find the correct trip. If a '*' is used ALL
 * trip depatures are accepted.
 * <li>{@code [Target time]} The new or moved trip will have this time as its first depature time, all other
 * stop times are adjusted with the same delta.
 * <li>{@code [Time shift]}  A duration that specify how mutch to change the trip stop times. It can be negative
 * an is specified like this:
 *     <p>{@code 1h2m} is 1 hour and 2 minutes
 *     <p>{@code -7m} is minus 7 minutes
 * <li>{@code [List of source times]} A list of source times seperated by ';'.
 * </ul>
 * <p>
 *  The commands are not case sensitive and the 'applyOnDate' and 'moveToServiceId' only affect the commands following
 *  them until the same command
 *  ('applyOnDate' or 'moveToServiceId') is repeted.
 */
public class TripCopyAndMove {
    private static final Logger LOG = LoggerFactory.getLogger(TripCopyAndMove.class);

    private final TransitServiceDecorator transit;
    private final TransitTransformFileInput input;
    private ServiceDate currentServiceDate = null;
    private String currentLine = null;


    private TripCopyAndMove(File inputDir, OtpTransitBuilder transitService) {
       this.transit = new TransitServiceDecorator(transitService);
       this.input = new TransitTransformFileInput(inputDir);
    }

    public static void run(File inputDir, OtpTransitBuilder transitService) {
        new TripCopyAndMove(inputDir, transitService).run();
    }

    private void run() {
        try {
            List<List<String>> lines = input.readFile();
            for (List<String> line : lines) {
                currentLine = String.join("; ", line);
                if(line.size() < 2) throw new IllegalArgumentException("Not enough args.");
                executeCommand(line);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to process input: " + currentLine, e);
        }
    }

    private void executeCommand(List<String> tokens) {
        try {
            Command cmd = parseCommand(tokens.get(0));
            String arg1 = tokens.get(1);
            List<String> args = tokens.subList(2, tokens.size());

            switch (cmd) {
                case CopyFromTo:
                    copyFromTo(createId(arg1), args);
                    break;
                case CopyTimeShift:
                    copyTimeShift(createId(arg1), args);
                    break;
                case MoveTimeShift:
                    moveTimeShift(createId(arg1), args);
                    break;
                case ApplyOnDate:
                    // applyOnDate; 2019-10-27
                    currentServiceDate = parseServiceDate(arg1);
                    break;
                default:
                    parseError("Unknown command: " + cmd);
                    break;
            }
        }
        catch (IllegalArgumentException e) {
            LOG.error("{} Command: '{}'", e.getMessage(), currentLine);
        }
    }

    private void copyFromTo(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 3)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int sourceDepTimeSec = TripTransformerTimeUtil.timeInSec(args.get(1));
        int targetDepTimeSec = TripTransformerTimeUtil.timeInSec(args.get(2));
        int timeShiftSec = targetDepTimeSec - sourceDepTimeSec;
        TripDeparture trip = transit.findTrip(currentServiceDate, lineId, headsignFilter, t -> t == sourceDepTimeSec);

        if(trip == null) throw new IllegalArgumentException("No trip found.");

        transit.copyTrip(trip, timeShiftSec, currentServiceDate);
        success();
    }


    private void copyTimeShift(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 33)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int timeshiftSec = TripTransformerTimeUtil.durationInSec(args.get(1));
        IntPredicate departureFilter = departureFilter(args.subList(2, args.size()));
        List<TripDeparture> trips = transit.findAllTrips(currentServiceDate, lineId, headsignFilter, departureFilter);

        if(trips.isEmpty()) throw new IllegalArgumentException("No trips found.");
        removeDuplicates(trips);

        for (TripDeparture trip : trips) {
            transit.copyTrip(trip, timeshiftSec, currentServiceDate);
        }
        success();
    }
    private void moveTimeShift(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 33)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int timeshiftSec = TripTransformerTimeUtil.durationInSec(args.get(1));
        IntPredicate departureFilter = departureFilter(args.subList(2, args.size()));
        List<TripDeparture> trips = transit.findAllTrips(currentServiceDate, lineId, headsignFilter, departureFilter);

        if(trips.isEmpty()) throw new IllegalArgumentException("No trips found.");
        removeDuplicates(trips);

        for (TripDeparture trip : trips) {
            transit.moveTrip(trip, timeshiftSec, currentServiceDate);
        }
        success();
    }

    private static void removeDuplicates(List<TripDeparture> trips) {
        List<TripDeparture> removeList = new ArrayList<>();
        Set<String> exist = new HashSet<>();
        for (TripDeparture t : trips) {
            String key = t.key();
            if(exist.contains(key)) {
                removeList.add(t);
            }
            else {
                exist.add(key);
            }
        }
        trips.removeAll(removeList);
    }

    private IntPredicate departureFilter(List<String> args) {
        if(args.size() == 1 && "*".equals(args.get(0))) {
            return null;
        }
        final int[] times = args.stream().mapToInt(TripTransformerTimeUtil::timeInSec).toArray();

        return (t) -> {
            for (int time : times) {
                if (t == time) return true;
            }
            return false;
        };
    }

    private static Predicate<String> headsignFilter(String arg) {
        return "*".equals(arg) ? null : arg::equals;
    }

    private static AgencyAndId createId(String text) {
        return new AgencyAndId("RB", text);
    }

    private static ServiceDate parseServiceDate(String text) {
        String[] tokens = text.split("-");
        int year = parseInt(tokens[0]);
        int month = parseInt(tokens[1]);
        int day = parseInt(tokens[2]);
        return new ServiceDate(year, month, day);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean verifyInputIsOk(List<String> args, int minLimitArgs, int maxLimitArgs) {
        if(args.size() < minLimitArgs || args.size() > maxLimitArgs) {
            parseError("Args limit excided. " + args.size() + " not between " + minLimitArgs + " - " + maxLimitArgs);
            return false;
        }

        if(currentServiceDate == null) {
            LOG.error(
                    "Transit transformation line skipped. ApplyOnDate: {}, Line: {}",
                    currentServiceDate, currentLine
            );
            return false;
        }
        return true;
    }

    private void parseError(String message) {
        LOG.error("Transit transformation parsing error. {}, Line: '{}'", message, currentLine);
    }

    private void success() {
        LOG.info("Transit transformation ok: '{}'", currentLine);
    }

    private static Command parseCommand(String value) {
        for (Command command : Command.values()) {
            if(command.name().equalsIgnoreCase(value)) return command;
        }
        return null;
    }

    private static int parseInt(String text) {
        return Integer.parseInt(text.trim());
    }

    enum Command {
        CopyFromTo, CopyTimeShift, MoveTimeShift, ApplyOnDate;
    }
}
