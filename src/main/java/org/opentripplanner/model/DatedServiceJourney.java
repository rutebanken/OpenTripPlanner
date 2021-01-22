package org.opentripplanner.model;


import org.opentripplanner.model.calendar.ServiceDate;

import javax.annotation.Nullable;

/**
 * A Netex DatedServiceJourney(DSJ) plays not role in the internal OTP model, but is used by the
 * TransModel API. This class is a composition of {@link Trip} and {@link TripAlterationOnDate},
 * witch together hold all the information needed to represent a DSJ.
 * <p>
 * This object should ONLY live in the graph-index and do not exist as part of
 * the routing model.
 */
public class DatedServiceJourney {
  private final Trip trip;
  private final TripAlterationOnDate alteration;

  public DatedServiceJourney(Trip trip, TripAlterationOnDate alteration) {
    this.trip = trip;
    this.alteration = alteration;
  }

  public AgencyAndId getId() {
    return alteration.getId();
  }

  public Trip getTrip() {
    return trip;
  }

  public TripAlteration getAlteration() {
    return alteration.getAlteration();
  }

  public ServiceDate getDate() {
    return alteration.getDate();
  }

  @Nullable
  public AgencyAndId getReplacesId() {
    return alteration.getReplaces() == null ? null : alteration.getReplaces().getId();
  }
}
