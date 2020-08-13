package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AllowedTransitModes {
  private final Set<TransitMode> transitModes;

  public AllowedTransitModes(Collection<TransitMode> transitModes) {
    this.transitModes = new HashSet<>(transitModes);
  }

  public boolean isAllowed(TransitMode transitMode) {
    return transitModes.stream()
        .filter(t -> t.getMainMode().equals(transitMode.getMainMode()))
        .anyMatch(t -> t.getSubmode() == null || t.getSubmode().equals(transitMode.getSubmode()));
  }

  public boolean isEmpty() {
    return transitModes.isEmpty();
  }
}
