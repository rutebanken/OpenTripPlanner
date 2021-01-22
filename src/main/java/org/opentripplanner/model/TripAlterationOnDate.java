package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import javax.annotation.Nullable;
import java.util.Objects;


/**
 * This class represent a trip scheduled on a given date. The trip alteration is cancellation,
 * planned, extraJourney, or replaced.
 * <p>
 * This entity is ONLY used if the input date is NeTEx and the NeTEx data have
 * DatedServiceJourneys. There is a one-to-one relationship between instances of this class
 * and instances of the NeTEx DatedServiceJourney.
 */
public class TripAlterationOnDate extends IdentityBean<AgencyAndId> {
  private final AgencyAndId id;
  private final TripAlteration alteration;
  private final ServiceDate date;
  private final TripAlterationOnDate replaces;

  public TripAlterationOnDate(
      AgencyAndId id,
      TripAlteration alteration,
      ServiceDate date,
      TripAlterationOnDate dsjRef
  ) {
    Objects.requireNonNull(id, "id");
    this.id = id;
    this.alteration = Objects.requireNonNullElse(alteration, TripAlteration.planned);
    this.date = date;
    this.replaces = dsjRef;
  }

  @Override
  public AgencyAndId getId() {
    return id;
  }

  @Override
  public void setId(AgencyAndId id) {
    throw new IllegalStateException("Id is final.");
  }

  public TripAlteration getAlteration() {
    return alteration;
  }

  public ServiceDate getDate() {
    return date;
  }

  @Nullable
  public TripAlterationOnDate getReplaces() {
    return replaces;
  }
}
