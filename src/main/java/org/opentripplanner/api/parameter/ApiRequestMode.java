package org.opentripplanner.api.parameter;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;

import java.util.Collection;
import java.util.Collections;

public enum ApiRequestMode {
  WALK(TransitModeService.getAllMainModes()),
  BICYCLE(),
  CAR(),
  TRAM(TransitModeService.getTransitMode(TransitMainMode.TRAM)),
  SUBWAY(TransitModeService.getTransitMode(TransitMainMode.SUBWAY)),
  RAIL(TransitModeService.getTransitMode(TransitMainMode.RAIL)),
  BUS(TransitModeService.getTransitMode(TransitMainMode.BUS)),
  FERRY(TransitModeService.getTransitMode(TransitMainMode.FERRY)),
  CABLE_CAR(TransitModeService.getTransitMode(TransitMainMode.CABLE_CAR)),
  GONDOLA(TransitModeService.getTransitMode(TransitMainMode.GONDOLA)),
  FUNICULAR(TransitModeService.getTransitMode(TransitMainMode.FUNICULAR)),
  TRANSIT(TransitModeService.getAllMainModes()),
  AIRPLANE(TransitModeService.getTransitMode(TransitMainMode.AIRPLANE));

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
