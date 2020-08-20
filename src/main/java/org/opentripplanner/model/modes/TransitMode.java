package org.opentripplanner.model.modes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  private final List<String> netexSubmodes;

  private final List<String> gtfsExtendRouteTypes;

  private final String netexOutputSubmode;

  private final String gtfsOutputExtendedRouteType;

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

  public static Set<TransitMode> getAllMainModes() {
    return new HashSet<>(mainTransitModes.values());
  }

  public static Set<TransitMode> getMainModesExceptAirplane() {
    return mainTransitModes
        .values()
        .stream()
        .filter(t -> !t.getMainMode().equals(TransitMainMode.AIRPLANE))
        .collect(Collectors.toSet());
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
