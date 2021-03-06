package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.StopPlace;


public class StopPlaceTypeMapper {

    private static final Integer DEFAULT_OTP_VALUE = 3;

    public int getTransportMode(StopPlace stopPlace) {
        if (stopPlace.getTransportMode() != null) {
            switch (stopPlace.getTransportMode()) {
                case AIR:
                    if (stopPlace.getAirSubmode() != null) {
                        switch (stopPlace.getAirSubmode()) {
                            case DOMESTIC_FLIGHT:
                                return 1102;
                            case HELICOPTER_SERVICE:
                                return 1110;
                            case INTERNATIONAL_FLIGHT:
                                return 1101;
                            default:
                                return 1100;
                        }
                    } else {
                        return 1100;
                    }
                case BUS:
                    if (stopPlace.getBusSubmode() != null) {
                        switch (stopPlace.getBusSubmode()) {
                            case AIRPORT_LINK_BUS:
                                return 700; // ?
                            case EXPRESS_BUS:
                                return 702;
                            case LOCAL_BUS:
                                return 704;
                            case NIGHT_BUS:
                                return 705;
                            case RAIL_REPLACEMENT_BUS:
                                return 714;
                            case REGIONAL_BUS:
                                return 701;
                            case SCHOOL_BUS:
                                return 712;
                            case SHUTTLE_BUS:
                                return 711;
                            case SIGHTSEEING_BUS:
                                return 710;
                            default:
                                return 700;
                        }
                    } else {
                        return 700;
                    }
                case CABLEWAY:
                    if (stopPlace.getTelecabinSubmode() != null) {
                        switch (stopPlace.getTelecabinSubmode()) {
                            case TELECABIN:
                                return 1301;
                            default:
                                return 1700;
                        }
                    } else {
                        return 1700;
                    }
                case COACH:
                    if (stopPlace.getCoachSubmode() != null) {
                        switch (stopPlace.getCoachSubmode()) {
                            case INTERNATIONAL_COACH:
                                return 201;
                            case NATIONAL_COACH:
                                return 202;
                            case TOURIST_COACH:
                                return 207;
                            default:
                                return 200;
                        }
                    } else {
                        return 200;
                    }
                case FUNICULAR:
                    if (stopPlace.getFunicularSubmode() != null) {
                        switch (stopPlace.getFunicularSubmode()) {
                            case FUNICULAR:
                                return 1401;
                            default:
                                return 1400;
                        }
                    } else {
                        return 1400;
                    }
                case METRO:
                    if (stopPlace.getMetroSubmode() != null) {
                        switch (stopPlace.getMetroSubmode()) {
                            case METRO:
                                return 401;
                            case URBAN_RAILWAY:
                                return 403;
                            default:
                                return 401;
                        }
                    } else {
                        return 401;
                    }
                case RAIL:
                    if (stopPlace.getRailSubmode() != null) {
                        switch (stopPlace.getRailSubmode()) {
                            case AIRPORT_LINK_RAIL:
                                return 100; // ?
                            case INTERNATIONAL:
                                return 100; // ?
                            case INTERREGIONAL_RAIL:
                                return 103;
                            case LOCAL:
                                return 100; // ?
                            case LONG_DISTANCE:
                                return 102;
                            case NIGHT_RAIL:
                                return 105;
                            case REGIONAL_RAIL:
                                return 106;
                            case TOURIST_RAILWAY:
                                return 107;
                            default:
                                return 100;
                        }
                    } else {
                        return 100;
                    }
                case TRAM:
                    if (stopPlace.getTramSubmode() != null) {
                        switch (stopPlace.getTramSubmode()) {
                            case LOCAL_TRAM:
                                return 902;
                            default:
                                return 900;
                        }
                    } else {
                        return 900;
                    }
                case WATER:
                    if (stopPlace.getWaterSubmode() != null) {
                        switch (stopPlace.getWaterSubmode()) {
                            case HIGH_SPEED_PASSENGER_SERVICE:
                                return 1014;
                            case HIGH_SPEED_VEHICLE_SERVICE:
                                return 1013;
                            case INTERNATIONAL_CAR_FERRY:
                                return 1001;
                            case INTERNATIONAL_PASSENGER_FERRY:
                                return 1005;
                            case LOCAL_CAR_FERRY:
                                return 1004;
                            case LOCAL_PASSENGER_FERRY:
                                return 1008;
                            case NATIONAL_CAR_FERRY:
                                return 1002;
                            case SIGHTSEEING_SERVICE:
                                return 1015;
                            default:
                                return 1000;
                        }
                    } else {
                        return 1000;
                    }
                default:
                    return DEFAULT_OTP_VALUE;
            }

        } else {
            return DEFAULT_OTP_VALUE;
        }
    }
}