package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;

public class TransitModeMapper {

    public static TransitMode mapMode(
        int routeType,
        TransitModeService transitModeService
    ) {
        TransitMode transitMode = transitModeService != null ?
            mapSubmodeFromConfiguration(routeType, transitModeService) : null;

        if (transitMode == null) {
            transitMode = mapTransitModeOnly(routeType);
        }

        return transitMode;
    }

    private static TransitMode mapSubmodeFromConfiguration(
        int routeType,
        TransitModeService transitModeService
    ) {
        if (transitModeService == null) {
            return null;
        }

        TransitMode transitMode;

        try {
            transitMode = transitModeService.getTransitModeByGtfsExtendedRouteType(
                String.valueOf(routeType));
        } catch (IllegalArgumentException e) {
            transitMode = null;
        }

        return transitMode;
    }

    private static TransitMode mapTransitModeOnly(
        int routeType
    ) {
        // Should really be reference to org.onebusaway.gtfs.model.Stop.MISSING_VALUE, but it is private.
        if (routeType == -999) { return null; }

        /* TPEG Extension  https://groups.google.com/d/msg/gtfs-changes/keT5rTPS7Y0/71uMz2l6ke0J */
        if (routeType >= 100 && routeType < 200) { // Railway Service
            return TransitModeService.getTransitMode(TransitMainMode.RAIL);
        } else if (routeType >= 200 && routeType < 300) { //Coach Service
            return TransitModeService.getTransitMode(TransitMainMode.BUS);
        } else if (routeType >= 300
            && routeType < 500) { //Suburban Railway Service and Urban Railway service
            if (routeType >= 401 && routeType <= 402) {
                return TransitModeService.getTransitMode(TransitMainMode.SUBWAY);
            }
            return TransitModeService.getTransitMode(TransitMainMode.RAIL);
        } else if (routeType >= 500 && routeType < 700) { //Metro Service and Underground Service
            return TransitModeService.getTransitMode(TransitMainMode.SUBWAY);
        } else if (routeType >= 700 && routeType < 900) { //Bus Service and Trolleybus service
            return TransitModeService.getTransitMode(TransitMainMode.BUS);
        } else if (routeType >= 900 && routeType < 1000) { //Tram service
            return TransitModeService.getTransitMode(TransitMainMode.TRAM);
        } else if (routeType >= 1000 && routeType < 1100) { //Water Transport Service
            return TransitModeService.getTransitMode(TransitMainMode.FERRY);
        } else if (routeType >= 1100 && routeType < 1200) { //Air Service
            return TransitModeService.getTransitMode(TransitMainMode.AIRPLANE);
        } else if (routeType >= 1200 && routeType < 1300) { //Ferry Service
            return TransitModeService.getTransitMode(TransitMainMode.FERRY);
        } else if (routeType >= 1300 && routeType < 1400) { //Telecabin Service
            return TransitModeService.getTransitMode(TransitMainMode.GONDOLA);
        } else if (routeType >= 1400 && routeType < 1500) { //Funicalar Service
            return TransitModeService.getTransitMode(TransitMainMode.FUNICULAR);
        } else if (routeType >= 1500 && routeType < 1600) { //Taxi Service
            throw new IllegalArgumentException("Taxi service not supported" + routeType);
        } else if (routeType >= 1600 && routeType < 1700) { //Self drive
            return TransitModeService.getTransitMode(TransitMainMode.BUS);
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (routeType) {
            case 0:
                return TransitModeService.getTransitMode(TransitMainMode.TRAM);
            case 1:
                return TransitModeService.getTransitMode(TransitMainMode.SUBWAY);
            case 2:
                return TransitModeService.getTransitMode(TransitMainMode.RAIL);
            case 3:
                return TransitModeService.getTransitMode(TransitMainMode.BUS);
            case 4:
                return TransitModeService.getTransitMode(TransitMainMode.FERRY);
            case 5:
                return TransitModeService.getTransitMode(TransitMainMode.CABLE_CAR);
            case 6:
                return TransitModeService.getTransitMode(TransitMainMode.GONDOLA);
            case 7:
                return TransitModeService.getTransitMode(TransitMainMode.FUNICULAR);
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
