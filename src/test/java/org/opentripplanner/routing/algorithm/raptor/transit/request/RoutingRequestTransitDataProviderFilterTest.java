package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.junit.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.*;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoutingRequestTransitDataProviderFilterTest {

  private static final FeedScopedId TEST_ROUTE_ID = new FeedScopedId("TEST", "ROUTE");

  private static final Stop STOP_FOR_TEST = Stop.stopForTest("TEST:STOP", 0, 0);

  @Test
  public void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        false,
        Set.of(AllowedTransitMode.fromMainModeEnum(TransitMainMode.BUS)),
        Collections.emptySet()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertTrue(valid);
  }

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = new Route(TEST_ROUTE_ID);
    route.setMode( TransitMode.fromMainModeEnum(TransitMainMode.BUS));

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    TripPattern pattern = new TripPattern(null, route, stopPattern);

    TripPatternWithRaptorStopIndexes tripPattern = new TripPatternWithRaptorStopIndexes(
        new int[0], pattern
    );

    TripTimes tripTimes = Mockito.mock(TripTimes.class);

    return new TripPatternForDate(tripPattern, new TripTimes[] {tripTimes}, LocalDate.now());
  }

  @Test
  public void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        false,
        Set.of(AllowedTransitMode.fromMainModeEnum(TransitMainMode.BUS)),
        Set.of(TEST_ROUTE_ID)
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void transitModeFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertTrue(valid);
  }

  private TripTimes createTestTripTimes() {
    Trip trip = new Trip(new FeedScopedId("TEST", "TRIP"));
    trip.setBikesAllowed(2);

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }

  @Test
  public void bikesAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        true,
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void wheelchairAccessibleFilteringTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        true,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void includePlannedCancellationsTest() {
    TripTimes tripTimes1 = createTestTripTimes();
    TripTimes tripTimes2 = createTestTripTimes();
    tripTimes1.trip.setAlteration(TripAlteration.cancellation);
    tripTimes2.trip.setAlteration(TripAlteration.replaced);

    var filter1 = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        true,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid1 = filter1.tripTimesPredicate(tripTimes1);
    boolean valid2 = filter1.tripTimesPredicate(tripTimes2);

    assertTrue(valid1);
    assertTrue(valid2);


    var filter2 = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid3 = filter2.tripTimesPredicate(tripTimes1);
    boolean valid4 = filter2.tripTimesPredicate(tripTimes2);

    assertFalse(valid3);
    assertFalse(valid4);
  }
}
