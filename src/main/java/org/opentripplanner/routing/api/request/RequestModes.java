package org.opentripplanner.routing.api.request;

import org.opentripplanner.model.modes.AllowedTransitModes;
import org.opentripplanner.model.modes.TransitMode;

import java.util.Collection;

public class RequestModes {
  public StreetMode accessMode;
  public StreetMode egressMode;
  public StreetMode directMode;
  public AllowedTransitModes transitModes;

  public RequestModes(
      StreetMode accessMode,
      StreetMode egressMode,
      StreetMode directMode,
      Collection<TransitMode> transitModes
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : null;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : null;
    this.directMode = directMode;
    this.transitModes = new AllowedTransitModes(transitModes);
  }
}
