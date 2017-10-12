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
package org.onebusaway2.gtfs.model;

import org.onebusaway2.gtfs.model.calendar.ServiceDate;

import java.util.Objects;

/**
 * @author bdferris
 *
 */
public final class ServiceCalendarDate extends IdentityBean<Integer> {

    private static final long serialVersionUID = 1L;

    public static final int EXCEPTION_TYPE_ADD = 1;

    public static final int EXCEPTION_TYPE_REMOVE = 2;

    private int id;

    private AgencyAndId serviceId;

    private ServiceDate date;

    private int exceptionType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AgencyAndId getServiceId() {
        return serviceId;
    }

    public void setServiceId(AgencyAndId serviceId) {
        this.serviceId = serviceId;
    }

    public ServiceDate getDate() {
        return date;
    }

    public void setDate(ServiceDate date) {
        this.date = date;
    }

    public int getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(int exceptionType) {
        this.exceptionType = exceptionType;
    }

    @Override
    public String toString() {
        return "<CalendarDate serviceId=" + this.serviceId + " date=" + this.date + " exception="
                + this.exceptionType + ">";
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        ServiceCalendarDate serviceCalendarDate = (ServiceCalendarDate) o;
        String test1 = serviceCalendarDate.getDate().toString();
        String test2 = this.getDate().toString();

        String test3 = serviceCalendarDate.getServiceId().toString();
        String test4 = this.getServiceId().toString();


        // field comparison
        return (serviceCalendarDate.getDate().toString().equals(this.getDate().toString()) &&
                serviceCalendarDate.getServiceId().toString().equals(this.getServiceId().toString())) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDate().toString(), getServiceId().toString());
    }
}
