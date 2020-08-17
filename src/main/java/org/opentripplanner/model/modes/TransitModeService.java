package org.opentripplanner.model.modes;

import org.opentripplanner.standalone.config.SubmodesConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

  private static Map<TransitMainMode, TransitMode> mainTransitModes = Arrays
      .stream(TransitMainMode.values())
      .map(m -> new TransitMode(null, m))
      .collect(Collectors.toMap(TransitMode::getMainMode, m -> m));

  public static TransitMode getTransitMode(TransitMainMode mainMode) {
    return mainTransitModes.get(mainMode);
  }

  public static Collection<TransitMode> getAllMainModes() {
    return mainTransitModes.values();
  }

  public static Collection<TransitMode> getMainModesExceptAirplane() {
    return mainTransitModes
        .values()
        .stream()
        .filter(t -> !t.getMainMode().equals(TransitMainMode.AIRPLANE))
        .collect(Collectors.toList());
  }

  /**
   * Default subModes configuration used for testing.
   */
  public static TransitModeService getDefault() {
    return new TransitModeService(SubmodesConfig.getDefault());
  }

  public TransitModeService() {
    configuredTransitModes = new HashSet<>();
  }

  public TransitModeService(SubmodesConfig submodesConfig) {
    Set<TransitMode> transitModes = new HashSet<>();
    for (SubmodesConfig.ConfigItem configItem : submodesConfig.getConfig()) {
      transitModes.add(new TransitMode(configItem.name, configItem.mode));
    }
    this.configuredTransitModes = transitModes;
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
