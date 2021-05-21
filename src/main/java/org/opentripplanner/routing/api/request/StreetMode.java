package org.opentripplanner.routing.api.request;

public enum StreetMode {
  /**
   * Walk only
   */
  WALK(true, true, true, false, false),
  /**
   * Bike only
   *
   * This can be used as access/egress, but transfers will still be walk only.
   * // TODO OTP2 Implement bicycle transfers
   */
  BIKE(true, true, true, true, false),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  BIKE_TO_PARK(true, false, true, true, false),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  BIKE_RENTAL(true, true, true, true ,false),
  /**
   * Car only
   *
   * Direct mode only.
   */
  CAR(false, false, false, false, true),
  /**
   * Start in the car, drive to a parking area, and walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  CAR_TO_PARK(true, false, true, false, true),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road,
   * and walk the rest of the way. This can include various taxi-services or kiss & ride.
   */
  CAR_PICKUP(true, true, true, false, true),
  /**
   * Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
   * This can include car rental at fixed locations or free-floating services.
   */
  // TODO OTP2 Not implemented
  CAR_RENTAL(true, true, true, false, true),

  /**
   * Encompasses all types of on-demand and flexible transportation.
   */
  FLEXIBLE(true, true, true, false, true);

  boolean access;

  boolean egress;

  boolean includesWalking;

  boolean includesBiking;

  boolean includesDriving;

  StreetMode(
      boolean access,
      boolean egress,
      boolean includesWalking,
      boolean includesBiking,
      boolean includesDriving
  ) {
    this.access = access;
    this.egress = egress;
    this.includesWalking = includesWalking;
    this.includesBiking = includesBiking;
    this.includesDriving = includesDriving;
  }

  public boolean includesWalking() {
    return includesWalking;
  }

  public boolean includesBiking() {
    return includesBiking;
  }

  public boolean includesDriving() {
    return includesDriving;
  }
}
