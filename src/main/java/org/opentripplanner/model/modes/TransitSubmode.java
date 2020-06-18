package org.opentripplanner.model.modes;

import org.opentripplanner.model.TransitMode;

public class TransitSubmode {

  private final String name;

  private final TransitMode transitMode;

  public TransitSubmode(String name, TransitMode transitMode) {
    this.name = name;
    this.transitMode = transitMode;
  }

  public String getName() {
    return name;
  }

  public TransitMode getTransitMode() {
    return transitMode;
  }
}
