package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.standalone.config.SubmodesConfig;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

import static org.junit.Assert.assertEquals;

public class TransportModeMapperTest {

  private TransportModeMapper transportModeMapper = new TransportModeMapper(SubmodesConfig.getDefault(),
      new TransitModeConfiguration(SubmodesConfig.getDefault())
  );

  private TransitModeConfiguration transitModeConfiguration = new TransitModeConfiguration(
      SubmodesConfig.getDefault());

  @Test
  public void mapWithTransportModeOnly() {
    assertEquals(TransitModeConfiguration.getTransitMode(TransitMainMode.BUS),
        transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null)
    );
  }

  @Test
  public void mapWithSubMode() {
    assertEquals(transitModeConfiguration.getTransitMode(TransitMainMode.RAIL, "LONG_DISTANCE"),
        transportModeMapper.map(AllVehicleModesOfTransportEnumeration.RAIL,
            new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE)
        ));
  }

  @Test
  public void checkSubModePrecedensOverMainMode() {
    assertEquals(transitModeConfiguration.getTransitMode(
        TransitMainMode.FERRY,
        "INTERNATIONAL_PASSENGER_FERRY"
    ), transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS,
        new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    ));
  }
}
