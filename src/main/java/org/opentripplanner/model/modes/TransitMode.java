package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.Objects;

/**
 * A mode specified either by the TransitMainMode enum or the TransitMainMode enum and a customizable
 * subMode string specified by the SubmodesConfiguration module at graph build time.
 *
 * TransitMainMode is equivalent to GTFS route_type or to NeTEx TransportMode.
 *
 * SubMode is equivalent to either GTFS extended route_type or NeTEx TransportSubMode.
 *
 * This should only be instantiated in the TransitModeConfiguration, to ensure only configured modes
 * are used.
 */
public class TransitMode implements Serializable {

  private final TransitMainMode mainMode;

  private final String subMode;

  public TransitMode(String subMode, TransitMainMode mainMode) {
    this.subMode = subMode;
    this.mainMode = mainMode;
  }

  public String getSubMode() {
    return subMode;
  }

  public TransitMainMode getMainMode() {
    return mainMode;
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
