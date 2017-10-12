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
package org.opentripplanner.graph_builder.model;

import com.google.common.collect.Multimap;
import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.Pathway;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.ShapePoint;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.model.Trip;
import org.onebusaway2.gtfs.model.calendar.ServiceDate;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.OperatingPeriod;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToServiceCalendar;

public class NetexToGtfsDao implements GtfsDao {

    private final NetexDao netexDao;

    public NetexToGtfsDao(NetexDao netexDao) {
        this.netexDao = netexDao;
    }

    @Override
    public Collection<Agency> getAllAgencies() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AgencyAndId> getAllServiceIds() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<ServiceCalendarDate> getCalendarDatesForServiceId(AgencyAndId serviceId) {
        List<ServiceCalendarDate> dates = new ArrayList<>();
        String[] dayTypeIds = serviceId.getId().split("\\+");
        for (String dayTypeId : dayTypeIds) {
            Object o = netexDao.getDayTypeAssignment().get(dayTypeId);
            if (o instanceof OffsetDateTime) {
                OffsetDateTime date = (OffsetDateTime) o;
                ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
                serviceCalendarDate.setServiceId(serviceId);
                serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthValue(),
                        date.getDayOfMonth()));
                serviceCalendarDate.setExceptionType(1);
                dates.add(serviceCalendarDate);
            }
        }
        return dates;
    }

    @Override
    public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId) {
        String[] dayTypeIds = serviceId.getId().split("\\+");
        for (String dayTypeId : dayTypeIds) {
            DayType dayType = netexDao.getDayTypeById().get(dayTypeId);
            Object o = netexDao.getDayTypeAssignment().get(dayTypeId);

            if (o instanceof OperatingPeriod) {
                return mapToServiceCalendar(dayType, (OperatingPeriod) o, serviceId);
            }

        }
        return null;
    }

    @Override
    public Collection<ServiceCalendar> getAllCalendars() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<ServiceCalendarDate> getAllCalendarDates() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<FareAttribute> getAllFareAttributes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<FareRule> getAllFareRules() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<FeedInfo> getAllFeedInfos() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Frequency> getAllFrequencies() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Pathway> getAllPathways() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Route> getAllRoutes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<ShapePoint> getAllShapePoints() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Stop> getAllStops() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Stop getStopForId(AgencyAndId id) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<StopTime> getAllStopTimes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<StopTime> getStopTimesForTrip(Trip trip) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Transfer> getAllTransfers() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Trip> getAllTrips() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Multimap<StopPattern, TripPattern> getTripPatterns() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Stop> getStopsForStation(Stop station) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
