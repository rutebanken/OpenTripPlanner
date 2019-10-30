/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.model;

import java.util.List;

public final class Trip extends IdentityBean<AgencyAndId> {

    public enum ServiceAlteration {cancellation, planned, extraJourney, replaced}

    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private Route route;

    private Operator operator;

    private AgencyAndId serviceId;

    private String tripShortName;

    private String tripPublicCode;

    private String tripPrivateCode;

    private String tripHeadsign;

    private String routeShortName;

    private String directionId;

    private String blockId;

    private AgencyAndId shapeId;

    private int wheelchairAccessible = 0;

    private ServiceAlteration serviceAlteration;

    private List<KeyValue> keyValues;

    private TransmodelTransportSubmode transportSubmode;

    @Deprecated private int tripBikesAllowed = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private int bikesAllowed = 0;

    /** Custom extension for KCM to specify a fare per-trip */
    private String fareId;

    private String drtMaxTravelTime;

    private String drtAvgTravelTime;

    private double drtAdvanceBookMin;

    private String drtPickupMessage;

    private String drtDropOffMessage;

    private String continuousPickupMessage;

    private String continuousDropOffMessage;

    private BookingArrangement bookingArrangements;

    private FlexibleTripTypeEnum flexibleTripType;

    public Trip() {
    }

    public Trip(Trip obj) {
        this.id = obj.id;
        this.route = obj.route;
        this.operator = obj.operator;
        this.serviceId = obj.serviceId;
        this.tripShortName = obj.tripShortName;
        this.tripPrivateCode = obj.tripPrivateCode;
        this.tripHeadsign = obj.tripHeadsign;
        this.routeShortName = obj.routeShortName;
        this.directionId = obj.directionId;
        this.blockId = obj.blockId;
        this.shapeId = obj.shapeId;
        this.wheelchairAccessible = obj.wheelchairAccessible;
        this.tripBikesAllowed = obj.tripBikesAllowed;
        this.bikesAllowed = obj.bikesAllowed;
        this.fareId = obj.fareId;
        this.serviceAlteration = obj.serviceAlteration;
        this.keyValues = obj.keyValues;
        this.transportSubmode = obj.transportSubmode;
        this.drtMaxTravelTime = obj.drtMaxTravelTime;
        this.drtAvgTravelTime = obj.drtAvgTravelTime;
        this.drtAdvanceBookMin = obj.drtAdvanceBookMin;
        this.drtPickupMessage = obj.drtPickupMessage;
        this.drtDropOffMessage = obj.drtDropOffMessage;
        this.continuousPickupMessage = obj.continuousPickupMessage;
        this.continuousDropOffMessage = obj.continuousDropOffMessage;
    }

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId id) {
        this.id = id;
    }


    /**
     * Operator running the trip. Returns operator of this trip, if it exist, or else the route operator.
     */
    public Operator getOperator() {
        return operator != null ? operator : route.getOperator();
    }

    public Operator getTripOperator() {
        return operator;
    }

    public void setTripOperator(Operator operator) {
        this.operator = operator;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public AgencyAndId getServiceId() {
        return serviceId;
    }

    public void setServiceId(AgencyAndId serviceId) {
        this.serviceId = serviceId;
    }

    public String getTripShortName() {
        return tripPrivateCode != null && !tripPrivateCode.equals("") ? tripPrivateCode : tripPublicCode;
    }

    public void setTripShortName(String tripShortName) {
        this.tripShortName = tripShortName;
    }

    public String getTripPublicCode() {
        return tripPublicCode;
    }

    public void setTripPublicCode(String tripPublicCode) {
        this.tripPublicCode = tripPublicCode;
    }

    public String getTripPrivateCode() {
        return tripPrivateCode;
    }

    public void setTripPrivateCode(String tripPrivateCode) {
        this.tripPrivateCode = tripPrivateCode;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public void setTripHeadsign(String tripHeadsign) {
        this.tripHeadsign = tripHeadsign;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public String getDirectionId() {
        return directionId;
    }

    public void setDirectionId(String directionId) {
        this.directionId = directionId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public AgencyAndId getShapeId() {
        return shapeId;
    }

    public void setShapeId(AgencyAndId shapeId) {
        this.shapeId = shapeId;
    }

    public void setWheelchairAccessible(int wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public int getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    @Deprecated
    public void setTripBikesAllowed(int tripBikesAllowed) {
        this.tripBikesAllowed = tripBikesAllowed;
    }

    @Deprecated
    public int getTripBikesAllowed() {
        return tripBikesAllowed;
    }

    /**
     * @return 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public int getBikesAllowed() {
        return bikesAllowed;
    }

    /**
     * @param bikesAllowed 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public void setBikesAllowed(int bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public String toString() {
        return "<Trip " + getId() + ">";
    }

    public String getFareId() {
        return fareId;
    }

    public void setFareId(String fareId) {
        this.fareId = fareId;
    }

    public String getDrtMaxTravelTime() {
        return drtMaxTravelTime;
    }

    public void setDrtMaxTravelTime(String drtMaxTravelTime) {
        this.drtMaxTravelTime = drtMaxTravelTime;
    }

    public String getDrtAvgTravelTime() {
        return drtAvgTravelTime;
    }

    public void setDrtAvgTravelTime(String drtAvgTravelTime) {
        this.drtAvgTravelTime = drtAvgTravelTime;
    }

    public double getDrtAdvanceBookMin() {
        return drtAdvanceBookMin;
    }

    public void setDrtAdvanceBookMin(double drtAdvanceBookMin) {
        this.drtAdvanceBookMin = drtAdvanceBookMin;
    }

    public String getDrtPickupMessage() {
        return drtPickupMessage;
    }

    public void setDrtPickupMessage(String drtPickupMessage) {
        this.drtPickupMessage = drtPickupMessage;
    }

    public String getDrtDropOffMessage() {
        return drtDropOffMessage;
    }

    public void setDrtDropOffMessage(String drtDropOffMessage) {
        this.drtDropOffMessage = drtDropOffMessage;
    }

    public String getContinuousPickupMessage() {
        return continuousPickupMessage;
    }

    public void setContinuousPickupMessage(String continuousPickupMessage) {
        this.continuousPickupMessage = continuousPickupMessage;
    }

    public String getContinuousDropOffMessage() {
        return continuousDropOffMessage;
    }

    public void setContinuousDropOffMessage(String continuousDropOffMessage) {
        this.continuousDropOffMessage = continuousDropOffMessage;
    }

    public ServiceAlteration getServiceAlteration() {
        return serviceAlteration;
    }

    public void setServiceAlteration(ServiceAlteration serviceAlteration) {
        this.serviceAlteration = serviceAlteration;
    }

    public List<KeyValue> getKeyValues() {
        return keyValues;
    }

    public void setKeyValues(List<KeyValue> keyValues) {
        this.keyValues = keyValues;
    }

    public TransmodelTransportSubmode getTransportSubmode() {
        return transportSubmode;
    }

    public void setTransportSubmode(TransmodelTransportSubmode transportSubmode) {
        this.transportSubmode = transportSubmode;
    }

    public BookingArrangement getBookingArrangements() {
        return bookingArrangements;
    }

    public void setBookingArrangements(BookingArrangement bookingArrangements) {
        this.bookingArrangements = bookingArrangements;
    }

    public FlexibleTripTypeEnum getFlexibleTripType() {
        return flexibleTripType;
    }

    public void setFlexibleTripType(FlexibleTripTypeEnum flexibleTripType) {
        this.flexibleTripType = flexibleTripType;
    }

    public enum FlexibleTripTypeEnum {
        dynamicPassingTimes, fixedHeadwayFrequency, fixedPassingTimes, notFlexible, other
    }
}
