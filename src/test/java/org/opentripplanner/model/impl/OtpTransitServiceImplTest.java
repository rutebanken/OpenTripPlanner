/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.opentripplanner.model.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.ConstantsForTests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

public class OtpTransitServiceImplTest {
    private static final String FEED_ID = "Z";

    private static final AgencyAndId STATION_ID = new AgencyAndId(FEED_ID, "station");

    // The subject is used as read only; hence static is ok
    private static OtpTransitService subject;

    private static Agency agency;

    @BeforeClass
    public static void setup() throws IOException {
        GtfsContextBuilder contextBuilder = contextBuilder(FEED_ID, ConstantsForTests.FAKE_GTFS);
        OtpTransitBuilder builder = contextBuilder.getTransitBuilder();

        agency = first(builder.getAgencies());

        // Supplement test data with at least one entity in all collections
        FareRule rule = createFareRule();
        builder.getFareAttributes().add(rule.getFare());
        builder.getFareRules().add(rule);
        builder.getFeedInfos().add(new FeedInfo());

        subject = builder.build();
    }

    @Test
    public void testGetAllAgencies() {
        Collection<Agency> agencies = subject.getAllAgencies();
        Agency organization = first(agencies);

        assertEquals(1, agencies.size());
        assertEquals("agency", organization.getId());
        assertEquals("Fake Agency", organization.getName());
    }

    @Test
    public void testGetAllFareAttributes() {
        Collection<FareAttribute> fareAttributes = subject.getAllFareAttributes();

        assertEquals(1, fareAttributes.size());
        assertEquals("<FareAttribute agency_FA>", first(fareAttributes).toString());
    }

    @Test
    public void testGetAllFareRules() {
        Collection<FareRule> fareRules = subject.getAllFareRules();

        assertEquals(1, fareRules.size());
        assertEquals(
                "<FareRule  origin='Zone A' contains='Zone B' destination='Zone C'>",
                first(fareRules).toString()
        );
    }

    @Test
    public void testGetAllFeedInfos() {
        Collection<FeedInfo> feedInfos = subject.getAllFeedInfos();

        assertEquals(1, feedInfos.size());
        assertEquals("<FeedInfo 1>", first(feedInfos).toString());
    }

    @Test
    public void testGetAllPathways() {
        Collection<Pathway> pathways = subject.getAllPathways();

        assertEquals(4, pathways.size());
        assertEquals("<Pathway Z_pathways_1_1>", first(pathways).toString());
    }

    @Test
    public void testGetAllTransfers() {
        Collection<Transfer> transfers = subject.getAllTransfers();

        assertEquals(9, transfers.size());
        assertEquals("<Transfer stop=Z_F..Z_E>", first(transfers).toString());
    }

    @Test
    public void testGetAllStops() {
        Collection<Stop> stops = subject.getAllStops();

        assertEquals(25, stops.size());
        assertEquals("<Stop Z_A>", first(stops).toString());
    }

    @Test
    public void testGetAllStopTimes() {
        List<StopTime> stopTimes = new ArrayList<>();
        for (Trip trip : subject.getAllTrips()) {
            stopTimes.addAll(subject.getStopTimesForTrip(trip));
        }

        assertEquals(80, stopTimes.size());
        assertEquals("StopTime(seq=1 stop=Z_A trip=agency_1.1 times=00:00:00-00:00:00)",
                first(stopTimes).toString());
    }

    @Test
    public void testGetAllTrips() {
        Collection<Trip> trips = subject.getAllTrips();

        assertEquals(33, trips.size());
        assertEquals("<Trip agency_1.1>", first(trips).toString());
    }

    @Test
    public void testGetStopForId() {
        Stop stop = subject.getStopForId(new AgencyAndId("Z", "P"));
        assertEquals("<Stop Z_P>", stop.toString());
    }

    @Test
    public void testGetStopsForStation() {
        List<Stop> stops = subject.getStopsForStation(subject.getStopForId(STATION_ID));
        assertEquals("[<Stop Z_A>]", stops.toString());
    }

    @Test
    public void testGetShapePointsForShapeId() {
        Collection<ShapePoint> shapePoints = subject.getShapePointsForShapeId(new AgencyAndId("Z", "5"));
        assertEquals("[#1 (41,-72), #2 (41,-72), #3 (40,-72), #4 (41,-73), #5 (41,-74)]",
                shapePoints.stream().map(OtpTransitServiceImplTest::toString).collect(toList()).toString());
    }

    @Test
    public void testGetStopTimesForTrip() {
        List<StopTime> stopTimes = subject.getStopTimesForTrip(first(subject.getAllTrips()));
        assertEquals("[<Stop Z_A>, <Stop Z_B>, <Stop Z_C>]",
                stopTimes.stream().map(StopTime::getStop).collect(toList()).toString());
    }

    @Test
    public void testGetAllServiceIds() {
        Collection<AgencyAndId> serviceIds = subject.getAllServiceIds();

        assertEquals(2, serviceIds.size());
        assertEquals("Z_alldays", first(serviceIds).toString());
    }

    private static FareRule createFareRule() {
        FareAttribute fa = new FareAttribute();
        fa.setId(new AgencyAndId(agency.getId(), "FA"));
        FareRule rule = new FareRule();
        rule.setOriginId("Zone A");
        rule.setContainsId("Zone B");
        rule.setDestinationId("Zone C");
        rule.setFare(fa);
        return rule;
    }

    private static <T> T first(Collection<? extends T> c) {
        //noinspection ConstantConditions
        return c.stream().sorted(comparing(T::toString)).findFirst().get();
    }

    private static String toString(ShapePoint sp) {
        int lat = (int) sp.getLat();
        int lon = (int) sp.getLon();
        return "#" + sp.getSequence() + " (" + lat + "," + lon + ")";
    }
}