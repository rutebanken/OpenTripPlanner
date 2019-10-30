package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.graph_builder.triptransformer.timetablereport.PrintTripTimetableReport;
import org.opentripplanner.graph_builder.triptransformer.transform.GenerateTransformCommands;
import org.opentripplanner.graph_builder.triptransformer.transform.TripCopyAndMove;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * The Trip Transform Service can be used to:
 * <ul>
 *     <li>log trip schedules on a given day
 *     <li>copy or move trips
 *     <li>generate transform script
 * </ul>
 *
 * To enable the service the system property "tripTransform" must be set to one or more of these:
 * <ul>
 *     <li>{@code cmd} Generate transformation commands
 *     <li>{@code run} Run transformation in the "TransformTransit.txt" file.
 *     <li>{@code print} Print trip schedule.
 * </ul>
 * Use: {@code -DtripTransform=cmd|run|print} to run all.
 */
public class TripTransformService {
    private static final Logger LOG = LoggerFactory.getLogger(TripTransformService.class);
    private static final String SYS_PROP_TRIP_TRANSFORM = "tripTransform";
    private static final String HEADER = "\n\n* * * * * * * * * * * * * * * * * * * * * * * *";
    private static final String FOOTER = "* * * * * * * * * * * * * * * * * * * * * * * *\n\n";

    private static boolean generateCommands;
    private static boolean runTransform;
    private static boolean printReport;
    private static ServiceDate date;
    /**
     * Can be negative to reference a time on the previous day, if so the time is converted by adding 24 hours
     * and the time zone of the previous day is used. Any Daylight Saving Time differences is ignored.
     */
    private static String reportStartTime = "-02:15";
    private static String reportEndTime = "08:00";

    /**
     * Note that operation day may continue into the next day, 24:30 and 47:59 is legal. Setting this to true
     * include a column in the report for the previous service day and list all trips within the time-period
     * specified.
     */
    private static boolean reportIncludePreviousDay = true;

    static {
        // TODO TGR - Replace this with feature toggling
        String enableFeatures = getEnabledSystemProperty();
        generateCommands = enableFeatures.contains("cmd");
        runTransform = enableFeatures.contains("run");
        printReport = enableFeatures.contains("print");
        date = TripTransformerTimeUtil.DST_2019_OCT;
    }

    public static void runTripTransform(OtpTransitBuilder otpBuilder, File feedDirectory) {

        PrintTripTimetableReport report = null;

        if (printReport) {
            System.out.println(HEADER);
            report = createReport(otpBuilder);
            report.collectData("IN  >");
            report.print(null);
        }

        if(generateCommands) {
            new GenerateTransformCommands(date, otpBuilder).generateCmds();
        }

        if(runTransform) {
            TripCopyAndMove.run(feedDirectory, otpBuilder);
            if (report != null) report.collectData("OUT <");
        }

        if (report != null) {
            // Print to System.out and to file
            report.print(null);
            report.print("TripTimetable.csv");
        }

        if(printReport) {
            System.out.println(FOOTER);
        }
    }

    public static void printTimeTable(OtpTransitBuilder otpBuilder, File feedDirectory) {
        if (printReport) {
            System.out.println(HEADER);
            PrintTripTimetableReport p = createReport(otpBuilder);
            p.collectData(null);
            p.print(null);
            System.out.println(FOOTER);
        }
    }

    private static String getEnabledSystemProperty() {
        try {
            return System.getProperty(SYS_PROP_TRIP_TRANSFORM, "<no services enabled>");
        }
        catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "<no services enabled>";
        }
    }

    private static PrintTripTimetableReport createReport(OtpTransitBuilder transitService) {
        return new PrintTripTimetableReport(
                transitService, date, reportStartTime, reportEndTime, reportIncludePreviousDay
        );
    }
}
