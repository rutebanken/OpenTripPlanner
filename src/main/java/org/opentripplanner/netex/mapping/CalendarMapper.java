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
import org.onebusaway2.gtfs.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


// TODO TGR - Add Unit tests
public class CalendarMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarMapper.class);

    public static ServiceCalendar mapToServiceCalendar(
            DayType dayType, OperatingPeriod period, AgencyAndId serviceId
    ) {
        if (period == null) {
            return null;
        }

        ServiceCalendar serviceCalendar = new ServiceCalendar();
        serviceCalendar.setServiceId(serviceId);

        serviceCalendar.setStartDate(mapToStartDate(period.getFromDate()));
        serviceCalendar.setEndDate(mapToStartDate(period.getToDate()));

        if (dayType.getProperties() != null) {
            List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();
            for (PropertyOfDay property : propertyOfDays) {
                List<DayOfWeekEnumeration> daysOfWeek = property.getDaysOfWeek();
                for (DayOfWeekEnumeration dayOfWeek : daysOfWeek) {
                    mapDayOfWeekIntoServiceCalendar(dayOfWeek, serviceCalendar);
                }
            }
        }
        return serviceCalendar;
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

    public static ServiceDate mapToStartDate(LocalDateTime date) {
        return new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
