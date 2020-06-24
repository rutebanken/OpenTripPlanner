package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.standalone.config.SubmodesConfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TransitModeMapper {

    public static TransitMode mapMode(
        int routeType,
        SubmodesConfig submodeConfig,
        TransitModeConfiguration transitModeConfiguration
    ) {
        TransitMode transitMode = submodeConfig != null ?
            mapSubmodeFromConfiguration(routeType, submodeConfig, transitModeConfiguration) : null;

        if (transitMode == null) {
            transitMode = mapTransitModeOnly(routeType);
        }

        return transitMode;
    }

    private static TransitMode mapSubmodeFromConfiguration(
        int routeType,
        SubmodesConfig submodeConfig,
        TransitModeConfiguration transitModeConfiguration) {

        Optional<SubmodesConfig.ConfigItem> configItem =
            submodeConfig.getConfig().stream()
                .filter(c -> c.gtfsExtendRouteTypes.contains(String.valueOf(routeType)))
                .findFirst();

        if (configItem.isEmpty()) return null;

        return transitModeConfiguration.getTransitMode(configItem.get().mode, configItem.get().name);
    }

    private static TransitMode mapTransitModeOnly(
        int routeType
    ) {
        /* TPEG Extension  https://groups.google.com/d/msg/gtfs-changes/keT5rTPS7Y0/71uMz2l6ke0J */
        if (routeType >= 100 && routeType < 200) { // Railway Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.RAIL);
        } else if (routeType >= 200 && routeType < 300) { //Coach Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.BUS);
        } else if (routeType >= 300
            && routeType < 500) { //Suburban Railway Service and Urban Railway service
            if (routeType >= 401 && routeType <= 402) {
                return TransitModeConfiguration.getTransitMode(TransitMainMode.SUBWAY);
            }
            return TransitModeConfiguration.getTransitMode(TransitMainMode.RAIL);
        } else if (routeType >= 500 && routeType < 700) { //Metro Service and Underground Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.SUBWAY);
        } else if (routeType >= 700 && routeType < 900) { //Bus Service and Trolleybus service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.BUS);
        } else if (routeType >= 900 && routeType < 1000) { //Tram service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.TRAM);
        } else if (routeType >= 1000 && routeType < 1100) { //Water Transport Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.FERRY);
        } else if (routeType >= 1100 && routeType < 1200) { //Air Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.AIRPLANE);
        } else if (routeType >= 1200 && routeType < 1300) { //Ferry Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.FERRY);
        } else if (routeType >= 1300 && routeType < 1400) { //Telecabin Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.GONDOLA);
        } else if (routeType >= 1400 && routeType < 1500) { //Funicalar Service
            return TransitModeConfiguration.getTransitMode(TransitMainMode.FUNICULAR);
        } else if (routeType >= 1500 && routeType < 1600) { //Taxi Service
            throw new IllegalArgumentException("Taxi service not supported" + routeType);
        } else if (routeType >= 1600 && routeType < 1700) { //Self drive
            return TransitModeConfiguration.getTransitMode(TransitMainMode.BUS);
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (routeType) {
            case 0:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.TRAM);
            case 1:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.SUBWAY);
            case 2:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.RAIL);
            case 3:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.BUS);
            case 4:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.FERRY);
            case 5:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.CABLE_CAR);
            case 6:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.GONDOLA);
            case 7:
                return TransitModeConfiguration.getTransitMode(TransitMainMode.FUNICULAR);
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
