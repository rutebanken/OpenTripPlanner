package org.opentripplanner.graph_builder.triptransformer;


import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.graph_builder.triptransformer.util.TripTransformerTimeUtil;
import org.opentripplanner.model.calendar.ServiceDate;


public class TripTransformerTimeUtilTest {
    @Test
    public void isSummerTime() {
        Assert.assertTrue(TripTransformerTimeUtil.isDstSummerTime(2*3600));
        Assert.assertFalse(TripTransformerTimeUtil.isDstSummerTime(2*3600+1));
    }

    @Test
    public void timeToString() {
        Assert.assertEquals(
                "2:00",
                TripTransformerTimeUtil.timeToString(2 * 3600)
        );
    }

    @Test
    public void dayBefore() {
        Assert.assertEquals(
                new ServiceDate(2019, 10, 22),
                TripTransformerTimeUtil.dayBefore(new ServiceDate(2019, 10, 23))
        );
    }

    @Test
    public void timeInSec() {
        Assert.assertEquals(0, TripTransformerTimeUtil.timeInSec("0:0"));
        Assert.assertEquals(0, TripTransformerTimeUtil.timeInSec("0:00"));
        Assert.assertEquals(60, TripTransformerTimeUtil.timeInSec("0:01"));
        Assert.assertEquals(60*60, TripTransformerTimeUtil.timeInSec("1:00"));
        Assert.assertEquals(0, TripTransformerTimeUtil.timeInSec("1:00*"));
        Assert.assertEquals((2*60+7)*60, TripTransformerTimeUtil.timeInSec("2:07"));
        Assert.assertEquals((9*60+9)*60, TripTransformerTimeUtil.timeInSec("09:09"));
        Assert.assertEquals((60+7)*60, TripTransformerTimeUtil.timeInSec("2:07*"));

        // Negative times
        Assert.assertEquals(0, TripTransformerTimeUtil.timeInSec("-0:0"));
        Assert.assertEquals(-60, TripTransformerTimeUtil.timeInSec("-0:01"));
        Assert.assertEquals(-60*60, TripTransformerTimeUtil.timeInSec("-1:00"));
        Assert.assertEquals(-(2*60+7)*60, TripTransformerTimeUtil.timeInSec("-2:07"));
        Assert.assertEquals(-(60+7)*60, TripTransformerTimeUtil.timeInSec("-2:07*"));
    }
}
