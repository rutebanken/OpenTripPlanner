package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Set;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final boolean requireWheelchairAccessible;

  private final boolean includePlannedCancellations;

  private final Set<AllowedTransitMode> allowedTransitModes;

  private final Set<FeedScopedId> bannedRoutes;

  public RoutingRequestTransitDataProviderFilter(
      boolean requireBikesAllowed,
      boolean requireWheelchairAccessible,
      boolean includePlannedCancellations,
      Set<AllowedTransitMode> allowedTransitModes,
      Set<FeedScopedId> bannedRoutes
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.requireWheelchairAccessible = requireWheelchairAccessible;
    this.allowedTransitModes = allowedTransitModes;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = bannedRoutes;
  }

  public RoutingRequestTransitDataProviderFilter(RoutingRequest request) {
    this(
        request.modes.directMode == StreetMode.BIKE,
        request.wheelchairAccessible,
        request.includePlannedCancellations,
        request.modes.transitModes,
        request.rctx.bannedRoutes
    );
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    return routeIsNotBanned(tripPatternForDate) && transitModeIsAllowed(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    if (requireBikesAllowed) {
      return bikeAccessForTrip(tripTimes.trip) == BikeAccess.ALLOWED;
    }

    if (requireWheelchairAccessible) {
      return tripTimes.trip.getWheelchairAccessible() == 1;
    }

    if (!includePlannedCancellations) {
      return !tripTimes.trip.getTripAlteration().isCanceledOrReplaced();
    }

    return true;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().getPattern().route.getId();
    return !bannedRoutes.contains(routeId);
  }

  private boolean transitModeIsAllowed(TripPatternForDate tripPatternForDate) {
    TransitMode transitMode = tripPatternForDate.getTripPattern().getTransitMode();
    return allowedTransitModes.stream().anyMatch(m -> m.allows(transitMode));
  }

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }
}
