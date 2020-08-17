package org.opentripplanner.model.modes;

import org.opentripplanner.standalone.config.SubmodesConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all of the configured transit modes. The main modes are not configurable, and are
 * accessible via a static method. This is instantiated by the SubmodesConfiguration graph builder
 * module.
 */
public class TransitModeService {

  private final Set<TransitMode> configuredTransitModes;

  private static final Map<TransitMainMode, TransitMode> mainTransitModes = Arrays
      .stream(TransitMainMode.values())
      .map(m -> new TransitMode(m, null, null, null, null))
      .collect(Collectors.toMap(TransitMode::getMainMode, m -> m));

  public static TransitMode getTransitMode(TransitMainMode mainMode) {
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

  /**
   * Default subModes configuration used for testing.
   */
  public static TransitModeService getDefault() {
    return new TransitModeService(SubmodesConfig.getDefault().getSubmodes());
  }

  public TransitModeService() {
    configuredTransitModes = new HashSet<>();
  }

  public TransitModeService(List<TransitMode> transitModes) {
    this.configuredTransitModes = new HashSet<>(transitModes);
  }

  public TransitMode getTransitModeByGtfsExtendedRouteType(String gtfsExtendedRouteType) {
    Optional<TransitMode> transitSubMode = configuredTransitModes
        .stream()
        .filter(t -> t.gtfsExtendRouteTypes.contains(gtfsExtendedRouteType))
        .findFirst();

    if (transitSubMode.isEmpty()) {
      throw new IllegalArgumentException("Gtfs extended route type not configured.");
    }

    return transitSubMode.get();
  }

  public TransitMode getTransitModeByNetexSubMode(String netexSubMode) {
    Optional<TransitMode> transitSubMode = configuredTransitModes
        .stream()
        .filter(t -> t.netexSubmodes.contains(netexSubMode))
        .findFirst();

    if (transitSubMode.isEmpty()) {
      throw new IllegalArgumentException("NeTEx subMode not configured.");
    }

    return transitSubMode.get();
  }

  /**
   * Get a configured subMode by TransitMainMode enum value and subMode string.
   */
  public TransitMode getTransitMode(TransitMainMode mainMode, String subMode) {
    Optional<TransitMode> transitSubMode = configuredTransitModes
        .stream()
        .filter(t -> t.getMainMode().equals(mainMode))
        .filter(t -> t.getSubMode().equals(subMode))
        .findFirst();

    if (transitSubMode.isEmpty()) {
      throw new IllegalArgumentException("Requested transit subMode is not configured.");
    }

    return transitSubMode.get();
  }
}
