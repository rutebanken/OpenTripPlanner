package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;
import org.opentripplanner.standalone.config.SubmodesConfig;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class TransportModeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransportModeMapper.class);

    private final SubmodesConfig submodesConfig;

    private final TransitModeService transitModeService;

    public TransportModeMapper(
        SubmodesConfig submodesConfig,
        TransitModeService transitModeService
    ) {
        this.submodesConfig = submodesConfig;
        this.transitModeService = transitModeService;
    }

    public TransitMode map(
        AllVehicleModesOfTransportEnumeration netexMode,
        TransportSubmodeStructure submode
    ) {
        TransitMode result = null;
        if (submode != null) {
            result = mapSubmodeFromConfiguration(getSubmodeAsString(submode));
        }
        // Fallback to main mode
        if (result == null) {
            result = mapAllVehicleModesOfTransport(netexMode);
        }

        return result;
    }

    private TransitMode mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode) {
        switch (mode) {
            case AIR:
                return TransitModeService.getTransitMode(TransitMainMode.AIRPLANE);
            case BUS:
            case TAXI:
                return TransitModeService.getTransitMode(TransitMainMode.BUS);
            case CABLEWAY:
                return TransitModeService.getTransitMode(TransitMainMode.CABLE_CAR);
            case COACH:
                return TransitModeService.getTransitMode(TransitMainMode.COACH);
            case FUNICULAR:
                return TransitModeService.getTransitMode(TransitMainMode.FUNICULAR);
            case METRO:
                return TransitModeService.getTransitMode(TransitMainMode.SUBWAY);
            case RAIL:
                return TransitModeService.getTransitMode(TransitMainMode.RAIL);
            case TRAM:
                return TransitModeService.getTransitMode(TransitMainMode.TRAM);
            case WATER:
                return TransitModeService.getTransitMode(TransitMainMode.FERRY);
            default:
                throw new IllegalArgumentException();
        }
    }

    private String getSubmodeAsString(TransportSubmodeStructure submode) {
        if (submode.getAirSubmode() != null) {
            return submode.getAirSubmode().value();
        } else if (submode.getBusSubmode() != null) {
            return submode.getBusSubmode().value();
        } else if (submode.getTelecabinSubmode() != null) {
            return submode.getTelecabinSubmode().value();
        } else if (submode.getCoachSubmode() != null) {
            return submode.getCoachSubmode().value();
        } else if (submode.getFunicularSubmode() != null) {
            return submode.getFunicularSubmode().value();
        } else if (submode.getMetroSubmode() != null) {
            return submode.getMetroSubmode().value();
        } else if (submode.getRailSubmode() != null) {
            return submode.getRailSubmode().value();
        } else if (submode.getTramSubmode() != null) {
            return submode.getTramSubmode().value();
        } else if (submode.getWaterSubmode() != null) {
            return submode.getWaterSubmode().value();
        }
        throw new IllegalArgumentException();
    }

    private TransitMode mapSubmodeFromConfiguration(
        String submodeString) {
        Optional<SubmodesConfig.ConfigItem> configItem =
            submodesConfig.getConfig().stream()
                .filter(c -> c.netexSubmodes.contains(submodeString))
                .findFirst();

        if (configItem.isEmpty()) {
            LOG.info("Submode {} not configured. Falling back to main mode.", submodeString);
            return null;
        }

        return transitModeService.getTransitMode(configItem.get().mode, configItem.get().name);
    }
}
