package org.opentripplanner.model.modes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is used for filtering a by transit modes and submodes. A mode is allowed if at least one
 * of the modes specified in the set matches the exact main mode and submode OR if the allowes set
 * specifies the same main mode with the submode set to null.
 */
public class AllowedTransitModes {
  private final Set<TransitMode> transitModes;

  public AllowedTransitModes(Collection<TransitMode> transitModes) {
    this.transitModes = new HashSet<>(transitModes);
  }

  public boolean isAllowed(TransitMode transitMode) {
    return transitModes.stream()
        .filter(t -> t.getMainMode().equals(transitMode.getMainMode()))
        .anyMatch(t -> t.getSubMode() == null || t.getSubMode().equals(transitMode.getSubMode()));
  }

  public boolean isEmpty() {
    return transitModes.isEmpty();
  }
}
