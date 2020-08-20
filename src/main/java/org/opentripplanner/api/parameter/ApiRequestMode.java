package org.opentripplanner.api.parameter;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;

import java.util.Collection;
import java.util.Collections;

public enum ApiRequestMode {
  WALK(),
  BICYCLE(),
  CAR(),
  TRAM(TransitMode.fromMainModeEnum(TransitMainMode.TRAM)),
  SUBWAY(TransitMode.fromMainModeEnum(TransitMainMode.SUBWAY)),
  RAIL(TransitMode.fromMainModeEnum(TransitMainMode.RAIL)),
  BUS(TransitMode.fromMainModeEnum(TransitMainMode.BUS)),
  FERRY(TransitMode.fromMainModeEnum(TransitMainMode.FERRY)),
  CABLE_CAR(TransitMode.fromMainModeEnum(TransitMainMode.CABLE_CAR)),
  GONDOLA(TransitMode.fromMainModeEnum(TransitMainMode.GONDOLA)),
  FUNICULAR(TransitMode.fromMainModeEnum(TransitMainMode.FUNICULAR)),
  TRANSIT(TransitMode.getAllMainModes()),
  AIRPLANE(TransitMode.fromMainModeEnum(TransitMainMode.AIRPLANE));

  private final Collection<TransitMode> transitModes;

  ApiRequestMode(Collection<TransitMode> transitModes) {
    this.transitModes = transitModes;
  }

  ApiRequestMode(TransitMode transitMode) {
    this.transitModes = Collections.singleton(transitMode);
  }

  ApiRequestMode() {
    this.transitModes = Collections.emptySet();
  }

  public Collection<TransitMode> getTransitModes() {
    return transitModes;
  }
}
