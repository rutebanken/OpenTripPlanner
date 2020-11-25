package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

  // TODO These mappings should be in another class, so they are not part of the OTP model itself
  private final List<String> netexSubmodes;

  private final List<String> gtfsExtendRouteTypes;

  private final String netexOutputSubmode;

  private final String gtfsOutputExtendedRouteType;

  /**
   * Mapping between the old TransitMainMode enum and the new detailed TransitMode class. This
   * ensures that we are reusing TransitMode objects when the fromMainModeEnum method is called.
   */
  private static final Map<TransitMainMode, TransitMode> mainTransitModes = Arrays
      .stream(TransitMainMode.values())
      .map(m -> new TransitMode(m,
          null,
          null,
          Collections.emptyList(),
          Collections.emptyList(),
          null,
          null
      ))
      .collect(Collectors.toMap(TransitMode::getMainMode, m -> m));

  public static TransitMode fromMainModeEnum(TransitMainMode mainMode) {
    return mainTransitModes.get(mainMode);
  }

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
