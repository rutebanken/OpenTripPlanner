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

import org.opentripplanner.model.calendar.ServiceDate;

import java.io.Serializable;
import java.util.Objects;

/**
 * Note that I decided to call this class ServiceCalendar instead of Calendar,
 * so as to avoid confusion with java.util.Calendar
 *
 * @author bdferris
 *
 */
public final class ServiceCalendar implements Serializable {

    private static final long serialVersionUID = 1L;

    private AgencyAndId serviceId;

    private int monday;

    private int tuesday;

    private int wednesday;

    private int thursday;

    private int friday;

    private int saturday;

    private int sunday;

    private ServiceDate startDate;

    private ServiceDate endDate;

    public AgencyAndId getServiceId() {
        return serviceId;
    }

    public void setServiceId(AgencyAndId serviceId) {
        this.serviceId = serviceId;
    }

    public int getMonday() {
        return monday;
    }

    public void setMonday(int monday) {
        this.monday = monday;
    }

    public int getTuesday() {
        return tuesday;
    }

    public void setTuesday(int tuesday) {
        this.tuesday = tuesday;
    }

    public int getWednesday() {
        return wednesday;
    }

    public void setWednesday(int wednesday) {
        this.wednesday = wednesday;
    }

    public int getThursday() {
        return thursday;
    }

    public void setThursday(int thursday) {
        this.thursday = thursday;
    }

    public int getFriday() {
        return friday;
    }

    public void setFriday(int friday) {
        this.friday = friday;
    }

    public int getSaturday() {
        return saturday;
    }

    public void setSaturday(int saturday) {
        this.saturday = saturday;
    }

    public int getSunday() {
        return sunday;
    }

    public void setSunday(int sunday) {
        this.sunday = sunday;
    }

    public void setWeekdays(int value) {
        setMonday(value);
        setTuesday(value);
        setWednesday(value);
        setThursday(value);
        setFriday(value);
    }
    public void setWeekend(int value) {
        setSaturday(value);
        setSunday(value);
    }
    public void setAllDays(int value) {
        setWeekdays(value);
        setWeekend(value);
    }

    public ServiceDate getStartDate() {
        return startDate;
    }

    public void setStartDate(ServiceDate startDate) {
        this.startDate = startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }

    public void setEndDate(ServiceDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceCalendar that = (ServiceCalendar) o;
        return monday == that.monday && tuesday == that.tuesday && wednesday == that.wednesday
                && thursday == that.thursday && friday == that.friday && saturday == that.saturday
                && sunday == that.sunday && Objects.equals(serviceId, that.serviceId) && Objects
                .equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday,
                        startDate, endDate);
    }

    public String toString() {
        return "<ServiceCalendar " + this.serviceId + " [" + this.monday + this.tuesday
                + this.wednesday + this.thursday + this.friday + this.saturday + this.sunday + "]>";
    }
}
