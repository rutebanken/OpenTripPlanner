package org.opentripplanner.routing.api.request;

import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.modes.TransitMode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RequestModes {
  public StreetMode accessMode;
  public StreetMode egressMode;
  public StreetMode directMode;
  public Set<AllowedTransitMode> transitModes;

  public RequestModes(
      StreetMode accessMode,
      StreetMode egressMode,
      StreetMode directMode,
      Collection<AllowedTransitMode> transitModes
  ) {
    this.accessMode = (accessMode != null && accessMode.access) ? accessMode : null;
    this.egressMode = (egressMode != null && egressMode.egress) ? egressMode : null;
    this.directMode = directMode;
    this.transitModes = new HashSet<>(transitModes);
  }
}
