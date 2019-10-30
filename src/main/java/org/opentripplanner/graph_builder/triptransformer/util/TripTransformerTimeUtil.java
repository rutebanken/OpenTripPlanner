package org.opentripplanner.graph_builder.triptransformer.util;

import org.opentripplanner.model.calendar.ServiceDate;

import java.time.Duration;
import java.util.HashMap;

/**
 * Time Util used by The Transit Transform. It is ha some DST specific logic that onl apply to
 * this spesific
 */
public class TripTransformerTimeUtil {
    public static final ServiceDate DST_2019_MAR = new ServiceDate(2019, 3, 31);
    public static final ServiceDate DST_2019_OCT = new ServiceDate(2019, 10, 27);
    public static final ServiceDate DST_2020_MAR = new ServiceDate(2020, 3, 29);
    public static final ServiceDate DST_2020_OCT = new ServiceDate(2020, 10, 25);
    public static final ServiceDate DST_2021_MAR = new ServiceDate(2021, 3, 28);
    public static final ServiceDate DST_2021_OCT = new ServiceDate(2021, 10, 31);

    /** The time the clock is changed from summer time to winter time given in service wintertime (seconds). */
    private static final int DST_CHANGE_SEC = 2 * 60 * 60;
    private static final HashMap<ServiceDate, Integer> DST_OFFSET_PREV_DAY = new HashMap<>();

    static {
        DST_OFFSET_PREV_DAY.put(DST_2019_MAR, 23 * 3600);
        DST_OFFSET_PREV_DAY.put(DST_2019_OCT, 25 * 3600);
        DST_OFFSET_PREV_DAY.put(DST_2020_MAR, 23 * 3600);
        DST_OFFSET_PREV_DAY.put(DST_2020_OCT, 25 * 3600);
        DST_OFFSET_PREV_DAY.put(DST_2021_MAR, 23 * 3600);
        DST_OFFSET_PREV_DAY.put(DST_2021_OCT, 25 * 3600);
    }

    public static boolean isDstSummerTime(int timeSec) {
        return timeSec <= DST_CHANGE_SEC;
    }

    public static String timeToString(int timeSec) {
        boolean neg = timeSec < 0;
        int temp = (neg ? -timeSec : timeSec) / 60;
        int hour = temp / 60;
        int min = temp % 60;

        return (neg ? "-" : "") + hour + ":" + String.format("%02d", min);
    }

    public static ServiceDate dayBefore(ServiceDate date) {
        if(date.getDay() < 2) {
            throw new IllegalArgumentException(
                "Only support for finding the day before in the same month."
            );
        }
        return new ServiceDate(date.getYear(), date.getMonth(), date.getDay()-1);
    }

    public static int timeInSec(String text) {
        text = text.trim();
        boolean summertime = false;
        boolean negative = false;

        if(text.endsWith("*")) {
            text = text.substring(0, text.length()-1);
            summertime = true;
        }
        if(text.startsWith("-")) {
            text = text.substring(1);
            negative = true;
        }

        String[] tokens = text.split(":");
        int hh = Integer.parseInt(tokens[0]);
        int mm = Integer.parseInt(tokens[1]);

        return (negative ? -1 : 1) * (((summertime ? hh-1 : hh) * 60) + mm) * 60;
    }

    public static int durationInSec(String text) {
        if(text.startsWith("-")) {
            text = "-PT" + text.substring(1);
        }
        else {
            text = "PT" + text;
        }
        long d = Duration.parse(text).getSeconds();

        return (int)d;
    }

    public static int offsetPrevDaySec(ServiceDate date) {
        return DST_OFFSET_PREV_DAY.getOrDefault(date, 24 * 3600);
    }
}
