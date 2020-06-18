package org.opentripplanner.model.modes;

import java.util.Collections;
import java.util.List;

public class TransitSubmodeConfiguration {

  private final List<TransitSubmode> configuredTransitSubmodes;

  public TransitSubmodeConfiguration(List<TransitSubmode> transitSubmodes) {
    this.configuredTransitSubmodes = Collections.unmodifiableList(transitSubmodes);
  }

  public List<TransitSubmode> getConfiguredTransitSubmodes() {
    return configuredTransitSubmodes;
  }
}
