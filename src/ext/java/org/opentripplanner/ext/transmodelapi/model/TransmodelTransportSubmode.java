package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.model.modes.TransitMainMode;

public enum TransmodelTransportSubmode {

    // Air
    INTERNATIONAL_FLIGHT("internationalFlight", TransitMainMode.AIRPLANE, "INTERNATIONAL"),
    DOMESTIC_FLIGHT("domesticFlight", TransitMainMode.AIRPLANE, "DOMESTIC"),
    HELICOPTER_SERVICE("helicopterService", TransitMainMode.AIRPLANE, "HELICOPTER"),
    // Bus
    LOCAL_BUS("localBus", TransitMainMode.BUS, "LOCAL"),
    REGIONAL_BUS("regionalBus", TransitMainMode.BUS, "REGIONAL"),
    EXPRESS_BUS("expressBus", TransitMainMode.BUS, "EXPRESS"),
    NIGHT_BUS("nightBus", TransitMainMode.BUS, "NIGHT"),
    SIGHTSEEING_BUS("sightseeingBus", TransitMainMode.BUS, "SIGHTSEEING"),
    SHUTTLE_BUS("shuttleBus", TransitMainMode.BUS, "SHUTTLE"),
    SCHOOL_BUS("schoolBus", TransitMainMode.BUS, "SCHOOL"),
    RAIL_REPLACEMENT_BUS("railReplacementBus", TransitMainMode.BUS, "RAIL_REPLACEMENT"),
    AIRPORT_LINK_BUS("airportLinkBus", TransitMainMode.BUS, "AIRPORT_LINK"),
    // Coach
    INTERNATIONAL_COACH("internationalCoach", TransitMainMode.COACH, "INTERNATIONAL"),
    NATIONAL_COACH("nationalCoach", TransitMainMode.COACH, "NATIONAL"),
    // Rail
    LOCAL("local", TransitMainMode.RAIL, "LOCAL"),
    REGIONAL_RAIL("regionalRail", TransitMainMode.RAIL, "REGIONAL"),
    INTERREGIONAL_RAIL("interregionalRail", TransitMainMode.RAIL, "INTERREGIONAL"),
    LONG_DISTANCE("longDistance", TransitMainMode.RAIL, "LONG_DISTANCE"),
    INTERNATIONAL("international", TransitMainMode.RAIL, "INTERNATIONAL"),
    NIGHT_RAIL("nightRail", TransitMainMode.RAIL, "NIGHT"),
    TOURIST_RAILWAY("touristRailway", TransitMainMode.RAIL, "TOURIST"),
    AIRPORT_LINK_RAIL("airportLinkRail", TransitMainMode.RAIL, "AIRPORT_LINK"),
    // Water
    INTERNATIONAL_CAR_FERRY("internationalCarFerry", TransitMainMode.FERRY, "INTERNATIONAL_CAR_FERRY"),
    NATIONAL_CAR_FERRY("nationalCarFerry", TransitMainMode.FERRY, "NATIONAL_CAR_FERRY"),
    LOCAL_CAR_FERRY("localCarFerry", TransitMainMode.FERRY, "LOCAL_CAR_FERRY"),
    INTERNATIONAL_PASSENGER_FERRY("internationalPassengerFerry", TransitMainMode.FERRY, "INTERNATIONAL_PASSENGER_FERRY"),
    LOCAL_PASSENGER_FERRY("localPassengerFerry", TransitMainMode.FERRY, "LOCAL_PASSENGER_FERRY"),
    HIGH_SPEED_VEHICLE_SERVICE("highSpeedVehicleService", TransitMainMode.FERRY, "HIGH_SPEED_VEHICLE_SERVICE"),
    HIGH_SPEED_PASSENGER_SERVICE("highSpeedPassengerService", TransitMainMode.FERRY, "HIGH_SPEED_PASSENGER_SERVICE"),
    SIGHTSEEING_SERVICE("sightseeingService", TransitMainMode.FERRY, "SIGHTSEEING_SERVICE");

    private final String transmodelName;

    private final TransitMainMode transitMainMode;

    private final String otpName;

    TransmodelTransportSubmode(String transmodelName, TransitMainMode transitMainMode, String otpName) {
        this.transmodelName = transmodelName;
        this.transitMainMode = transitMainMode;
        this.otpName = otpName;
    }

    public String getTransmodelName() {
        return transmodelName;
    }

    public TransitMainMode getTransitMainMode() {
        return transitMainMode;
    }

    public String getOtpName() {
        return otpName;
    }
}
