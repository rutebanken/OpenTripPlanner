package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A mode specified either by the TransitMainMode enum or the TransitMainMode enum and a
 * customizable subMode string specified by the SubmodesConfiguration module at graph build time.
 * <p>
 * TransitMainMode is equivalent to GTFS route_type or to NeTEx TransportMode.
 * <p>
 * SubMode is equivalent to either GTFS extended route_type or NeTEx TransportSubMode.
 * <p>
 * This should only be instantiated in the TransitModeConfiguration, to ensure only configured modes
 * are used.
 */
public class TransitMode implements Serializable {

  private final TransitMainMode mainMode;

  private final String subMode;

  public final String description;

  public final List<String> netexSubmodes;

  public final List<String> gtfsExtendRouteTypes;

  public TransitMode(
      TransitMainMode mainMode,
      String subMode,
      String description,
      List<String> netexSubmodes,
      List<String> gtfsExtendRouteTypes
  ) {
    this.mainMode = mainMode;
    this.subMode = subMode;
    this.description = description;
    this.netexSubmodes = netexSubmodes;
    this.gtfsExtendRouteTypes = gtfsExtendRouteTypes;
  }

  public String getSubMode() {
    return subMode;
  }

  public TransitMainMode getMainMode() {
    return mainMode;
  }

  public boolean containedIn(Collection<TransitMode> transitModes) {
    return transitModes.stream().anyMatch(this::contains);
  }

  public boolean contains(TransitMode other) {
    return mainMode == other.getMainMode() && (
        other.subMode == null || other.subMode.equals(subMode)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    TransitMode that = (TransitMode) o;
    return mainMode == that.mainMode && Objects.equals(subMode, that.subMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, subMode);
  }
}
