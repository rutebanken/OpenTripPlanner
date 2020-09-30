package org.opentripplanner.api.parameter;

import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.modes.TransitMainMode;

import java.util.Collection;
import java.util.Collections;

public enum ApiRequestMode {
  WALK(),
  BICYCLE(),
  CAR(),
  TRAM(AllowedTransitMode.fromMainModeEnum(TransitMainMode.TRAM)),
  SUBWAY(AllowedTransitMode.fromMainModeEnum(TransitMainMode.SUBWAY)),
  RAIL(AllowedTransitMode.fromMainModeEnum(TransitMainMode.RAIL)),
  BUS(AllowedTransitMode.fromMainModeEnum(TransitMainMode.BUS)),
  FERRY(AllowedTransitMode.fromMainModeEnum(TransitMainMode.FERRY)),
  CABLE_CAR(AllowedTransitMode.fromMainModeEnum(TransitMainMode.CABLE_CAR)),
  GONDOLA(AllowedTransitMode.fromMainModeEnum(TransitMainMode.GONDOLA)),
  FUNICULAR(AllowedTransitMode.fromMainModeEnum(TransitMainMode.FUNICULAR)),
  TRANSIT(AllowedTransitMode.getAllTransitModes()),
  AIRPLANE(AllowedTransitMode.fromMainModeEnum(TransitMainMode.AIRPLANE)),
  FLEXIBLE();

  private final Collection<AllowedTransitMode> transitModes;

  ApiRequestMode(Collection<AllowedTransitMode> transitModes) {
    this.transitModes = transitModes;
  }

  ApiRequestMode(AllowedTransitMode transitMode) {
    this.transitModes = Collections.singleton(transitMode);
  }

  ApiRequestMode() {
    this.transitModes = Collections.emptySet();
  }

  public Collection<AllowedTransitMode> getTransitModes() {
    return transitModes;
  }
}
