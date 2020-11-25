package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitModeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitModeMapper.class);

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
            return TransitMode.fromMainModeEnum(TransitMainMode.RAIL);
        } else if (routeType >= 200 && routeType < 300) { //Coach Service
            return TransitMode.fromMainModeEnum(TransitMainMode.BUS);
        } else if (routeType >= 300
            && routeType < 500) { //Suburban Railway Service and Urban Railway service
            if (routeType >= 401 && routeType <= 402) {
                return TransitMode.fromMainModeEnum(TransitMainMode.SUBWAY);
            }
            return TransitMode.fromMainModeEnum(TransitMainMode.RAIL);
        } else if (routeType >= 500 && routeType < 700) { //Metro Service and Underground Service
            return TransitMode.fromMainModeEnum(TransitMainMode.SUBWAY);
        } else if (routeType >= 700 && routeType < 900) { //Bus Service and Trolleybus service
            return TransitMode.fromMainModeEnum(TransitMainMode.BUS);
        } else if (routeType >= 900 && routeType < 1000) { //Tram service
            return TransitMode.fromMainModeEnum(TransitMainMode.TRAM);
        } else if (routeType >= 1000 && routeType < 1100) { //Water Transport Service
            return TransitMode.fromMainModeEnum(TransitMainMode.FERRY);
        } else if (routeType >= 1100 && routeType < 1200) { //Air Service
            return TransitMode.fromMainModeEnum(TransitMainMode.AIRPLANE);
        } else if (routeType >= 1200 && routeType < 1300) { //Ferry Service
            return TransitMode.fromMainModeEnum(TransitMainMode.FERRY);
        } else if (routeType >= 1300 && routeType < 1400) { //Telecabin Service
            return TransitMode.fromMainModeEnum(TransitMainMode.GONDOLA);
        } else if (routeType >= 1400 && routeType < 1500) { //Funicalar Service
            return TransitMode.fromMainModeEnum(TransitMainMode.FUNICULAR);
        } else if (routeType >= 1500 && routeType < 1600) { //Taxi Service
            LOG.warn("Treating taxi extended route type {} as a bus.", routeType);
            return TransitMode.fromMainModeEnum(TransitMainMode.BUS);
        } else if (routeType >= 1600 && routeType < 1700) { //Self drive
            return TransitMode.fromMainModeEnum(TransitMainMode.BUS);
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (routeType) {
            case 0:
                return TransitMode.fromMainModeEnum(TransitMainMode.TRAM);
            case 1:
                return TransitMode.fromMainModeEnum(TransitMainMode.SUBWAY);
            case 2:
                return TransitMode.fromMainModeEnum(TransitMainMode.RAIL);
            case 3:
                return TransitMode.fromMainModeEnum(TransitMainMode.BUS);
            case 4:
                return TransitMode.fromMainModeEnum(TransitMainMode.FERRY);
            case 5:
                return TransitMode.fromMainModeEnum(TransitMainMode.CABLE_CAR);
            case 6:
                return TransitMode.fromMainModeEnum(TransitMainMode.GONDOLA);
            case 7:
                return TransitMode.fromMainModeEnum(TransitMainMode.FUNICULAR);
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
