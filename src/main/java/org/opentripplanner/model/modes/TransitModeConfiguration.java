package org.opentripplanner.model.modes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TransitModeConfiguration {

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

  public TransitModeConfiguration() {
    configuredTransitModes = new HashSet<>();
  }

  public TransitModeConfiguration(List<TransitMode> transitModes) {
    this.configuredTransitModes = new HashSet<>(transitModes);
  }

  public TransitMode getTransitMode(TransitMainMode mainMode, String submode) {
    Optional<TransitMode> transitSubmode = configuredTransitModes
        .stream()
        .filter(t -> t.getMainMode().equals(mainMode))
        .filter(t -> t.getSubmode().equals(submode))
        .findFirst();

    if (transitSubmode.isEmpty()) {
      throw new IllegalArgumentException("Requested transit submode is not configured.");
    }

    return transitSubmode.get();
  }
}
