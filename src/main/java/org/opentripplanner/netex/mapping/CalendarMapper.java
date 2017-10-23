/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;


// TODO TGR - Add Unit tests
public class CalendarMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarMapper.class);

    public static Collection<ServiceCalendarDate> mapToCalendarDates(AgencyAndId serviceId, NetexDao netexDao) {
        Collection<ServiceCalendarDate> serviceCalendarDates = new ArrayList<>();
        Collection<ServiceCalendarDate> serviceCalendarDatesRemove = new ArrayList<>();
        String[] dayTypeIds = serviceId.getId().split("\\+");

        for(String dayTypeId : dayTypeIds) {
            List<DayTypeAssignment> dayTypeAssignmentList = netexDao.getDayTypeAssignment().get(dayTypeId);


            for (DayTypeAssignment dayTypeAssignment : dayTypeAssignmentList) {
                Boolean isAvailable = netexDao.getDayTypeAvailable().get(dayTypeAssignment.getId());

                // Add or remove single days
                if (dayTypeAssignment.getDate() != null) {
                    LocalDateTime date = dayTypeAssignment.getDate();
                    ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);
                    if (isAvailable) {
                        serviceCalendarDates.add(serviceCalendarDate);
                    } else {
                        serviceCalendarDatesRemove.add(serviceCalendarDate);
                    }
                }
                // Add or remove periods
                else if (dayTypeAssignment.getOperatingPeriodRef() != null &&
                        netexDao.getOperatingPeriodById().containsKey(dayTypeAssignment.getOperatingPeriodRef().getRef())) {

                    OperatingPeriod operatingPeriod = netexDao.getOperatingPeriodById().get(dayTypeAssignment.getOperatingPeriodRef().getRef());
                    LocalDateTime fromDate = operatingPeriod.getFromDate();
                    LocalDateTime toDate = operatingPeriod.getToDate();

                    List<DayOfWeekEnumeration> daysOfWeek = new ArrayList<>();
                    DayType dayType = netexDao.getDayTypeById().get(dayTypeId);
                    if (dayType.getProperties() != null) {
                        List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();
                        for (PropertyOfDay property : propertyOfDays) {
                            daysOfWeek.addAll(property.getDaysOfWeek());
                        }
                    }

                    for (LocalDateTime date = fromDate; date.isBefore(toDate); date = date.plusDays(1)) {
                        ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);

                        if (daysOfWeek.contains(DayOfWeekEnumeration.EVERYDAY)) {
                            serviceCalendarDates.add(serviceCalendarDate);
                        } else {
                            switch (date.getDayOfWeek()) {
                                case MONDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.MONDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case TUESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.TUESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case WEDNESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.WEDNESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case THURSDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.THURSDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case FRIDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.FRIDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SATURDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SATURDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SUNDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SUNDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                            }
                        }

                    }
                }
            }
        }

        for (ServiceCalendarDate serviceCalendarDate : serviceCalendarDatesRemove) {
            if (serviceCalendarDates.contains(serviceCalendarDate)) {
                serviceCalendarDates.remove(serviceCalendarDate);
            }
        }

        return serviceCalendarDates;
    }

    public static void mapDayOfWeekIntoServiceCalendar(
            DayOfWeekEnumeration dayOfWeek, ServiceCalendar calendar
    ) {
        switch (dayOfWeek) {
            case MONDAY:
                calendar.setMonday(1);
                break;
            case TUESDAY:
                calendar.setTuesday(1);
                break;
            case WEDNESDAY:
                calendar.setWednesday(1);
                break;
            case THURSDAY:
                calendar.setThursday(1);
                break;
            case FRIDAY:
                calendar.setFriday(1);
                break;
            case SATURDAY:
                calendar.setSaturday(1);
                break;
            case SUNDAY:
                calendar.setSunday(1);
                break;
            case WEEKDAYS:
                calendar.setWeekdays(1);
                break;
            case WEEKEND:
                calendar.setWeekend(1);
                break;
            case EVERYDAY:
                calendar.setAllDays(1);
                break;
            case NONE:
                calendar.setAllDays(0);
                break;
            default:
                throw new IllegalStateException(
                        "The enum value " + dayOfWeek + " is unknown.");
        }
    }

    private static ServiceCalendarDate mapServiceCalendarDate(LocalDateTime date, AgencyAndId serviceId, Integer exceptionType) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        serviceCalendarDate.setServiceId(serviceId);
        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        serviceCalendarDate.setExceptionType(exceptionType);
        return serviceCalendarDate;
    }
}
