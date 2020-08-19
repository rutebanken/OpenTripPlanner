package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A mode specified either by the TransitMainMode enum or the TransitMainMode enum and a
 * customizable subMode string specified by the TransitModeServiceModule module at graph build time.
 * <p>
 * TransitMainMode is equivalent to GTFS route_type or to NeTEx TransportMode.
 * <p>
 * SubMode is equivalent to either GTFS extended route_type or NeTEx TransportSubMode.
 * <p>
 * This should only be instantiated in the TransitModeService class, to ensure only configured modes
 * are used.
 */
public class TransitMode implements Serializable {

  private final TransitMainMode mainMode;

  private final String subMode;

  private final String description;

  private final List<String> netexSubmodes;

  private final List<String> gtfsExtendRouteTypes;

  private final String netexOutputSubmode;

  private final String gtfsOutputExtendedRouteType;

  public TransitMode(
      TransitMainMode mainMode,
      String subMode,
      String description,
      List<String> netexSubmodes,
      List<String> gtfsExtendRouteTypes,
      String netexOutputSubmode,
      String gtfsOutputExtendedRouteType
  ) {
    this.mainMode = mainMode;
    this.subMode = subMode;
    this.description = description;
    this.netexSubmodes = new ArrayList<>(netexSubmodes);
    this.gtfsExtendRouteTypes = new ArrayList<>(gtfsExtendRouteTypes);
    this.netexOutputSubmode = netexOutputSubmode;
    this.gtfsOutputExtendedRouteType = gtfsOutputExtendedRouteType;
  }

  public String getSubMode() {
    return subMode;
  }

  public TransitMainMode getMainMode() {
    return mainMode;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getNetexSubmodes() {
    return netexSubmodes;
  }

  public List<String> getGtfsExtendRouteTypes() {
    return gtfsExtendRouteTypes;
  }

  public String getNetexOutputSubmode() {
    return netexOutputSubmode;
  }

  public String getGtfsOutputExtendedRouteType() {
    return gtfsOutputExtendedRouteType;
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
