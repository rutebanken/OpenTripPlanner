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

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


// TODO TGR - Add Unit tests
public class CalendarMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarMapper.class);

    public static Collection<ServiceCalendarDate> mapToCalendarDates(AgencyAndId serviceId, NetexDao netexDao) {

        String[] dayTypeIds = serviceId.getId().split("\\+");

        Collection<ServiceCalendarDate> allServiceCalendarDates = new HashSet<>();

        for(String dayTypeId : dayTypeIds) {
            Collection<ServiceCalendarDate> serviceCalendarDatesForDayType = new HashSet<>();
            Collection<ServiceCalendarDate> serviceCalendarDatesRemoveForDayType = new HashSet<>();
            Collection<DayTypeAssignment> dayTypeAssignmentList = netexDao.dayTypeAssignmentByDayTypeId.lookup(dayTypeId);

            for (DayTypeAssignment dayTypeAssignment : dayTypeAssignmentList) {
                Boolean isAvailable = netexDao.dayTypeAvailable.lookup(dayTypeAssignment.getId());

                // Add or remove single days
                if (dayTypeAssignment.getDate() != null) {
                    LocalDateTime date = dayTypeAssignment.getDate();
                    ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);
                    if (isAvailable) {
                        serviceCalendarDatesForDayType.add(serviceCalendarDate);
                    } else {
                        serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                    }
                }
                // Add or remove periods
                else if (dayTypeAssignment.getOperatingPeriodRef() != null &&
                        netexDao.operatingPeriodById.containsKey(dayTypeAssignment.getOperatingPeriodRef().getRef())) {

                    OperatingPeriod operatingPeriod = netexDao.operatingPeriodById.lookup(dayTypeAssignment.getOperatingPeriodRef().getRef());
                    LocalDateTime fromDate = operatingPeriod.getFromDate();
                    LocalDateTime toDate = operatingPeriod.getToDate();

                    List<DayOfWeekEnumeration> daysOfWeek = new ArrayList<>();
                    DayType dayType = netexDao.dayTypeById.lookup(dayTypeId);
                    if (dayType.getProperties() != null) {
                        List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();
                        for (PropertyOfDay property : propertyOfDays) {
                            daysOfWeek.addAll(property.getDaysOfWeek());
                        }
                    }

                    for (LocalDateTime date = fromDate; date.isBefore(toDate.plusDays(1)); date = date.plusDays(1)) {
                        ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);

                        if (daysOfWeek.contains(DayOfWeekEnumeration.EVERYDAY)) {
                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                        } else {
                            switch (date.getDayOfWeek()) {
                                case MONDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.MONDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case TUESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.TUESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case WEDNESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.WEDNESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case THURSDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.THURSDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case FRIDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.FRIDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SATURDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SATURDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SUNDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SUNDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDatesForDayType.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemoveForDayType.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                            }
                        }

                    }
                }
            }
            serviceCalendarDatesForDayType.removeAll(serviceCalendarDatesRemoveForDayType);
            allServiceCalendarDates.addAll(serviceCalendarDatesForDayType);
        }

        if (allServiceCalendarDates.isEmpty()) {
            LOG.warn("ServiceCode " + serviceId + " does not contain any serviceDates");
            // Add one date exception when list is empty to ensure serviceId is not lost
            allServiceCalendarDates.add(mapServiceCalendarDate(LocalDate.now().atStartOfDay(), serviceId, 2));
        }

        return allServiceCalendarDates;
    }

    private static ServiceCalendarDate mapServiceCalendarDate(LocalDateTime date, AgencyAndId serviceId, Integer exceptionType) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        serviceCalendarDate.setServiceId(serviceId);
        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        serviceCalendarDate.setExceptionType(exceptionType);
        return serviceCalendarDate;
    }
}