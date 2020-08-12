package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.standalone.config.SubmodesConfig;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

  private final SubmodesConfig submodesConfig;

  private final TransitModeConfiguration transitModeConfiguration;

  public StopMapper() {
    this.submodesConfig = null;
    this.transitModeConfiguration = null;
  }

  public StopMapper(
      SubmodesConfig submodesConfig, TransitModeConfiguration transitModeConfiguration
  ) {
    this.submodesConfig = submodesConfig;
    this.transitModeConfiguration = transitModeConfiguration;
  }

  Collection<Stop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Stop map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private Stop doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
      throw new IllegalArgumentException(
          "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP + ", but got "
              + gtfsStop.getLocationType());
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    return new Stop(base.getId(),
        base.getName(),
        base.getCode(),
        base.getDescription(),
        base.getCoordinate(),
        base.getWheelchairBoarding(),
        base.getLevel(),
        gtfsStop.getPlatformCode(),
        gtfsStop.getZoneId(),
        gtfsStop.getUrl(),
        gtfsStop.getTimezone() == null ? null : TimeZone.getTimeZone(gtfsStop.getTimezone()),
        (
            submodesConfig != null ? TransitModeMapper.mapMode(gtfsStop.getVehicleType(),
                submodesConfig,
                transitModeConfiguration
            ) : null
        )
    );
  }
}
