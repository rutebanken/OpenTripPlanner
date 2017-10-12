package org.opentripplanner.netex;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.calendar.ServiceDate;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.NetexBundle;
import org.opentripplanner.graph_builder.model.NetexStopPlaceBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.NetexModule;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MappingTest {

    private GtfsDao otpDaoFromGtfs;
    private GtfsDao otpDaoFromNetex;

    File gtfsFile = new File("src/test/resources/netex_mapping_test/gtfs_data/rb_rut-aggregated-gtfs.zip");
    File netexStopFile = new File("src/test/resources/netex_mapping_test/netex_data/stopPlaces.zip");
    File netexFile = new File("src/test/resources/netex_mapping_test/netex_data/rb_rut-aggregated-netex.zip");


    @Before
    public void setUpNetexMapping() throws Exception {
        if (gtfsFile == null || netexFile == null) {
            Assert.fail();
        }

        NetexStopPlaceBundle netexStopPlaceBundle = new NetexStopPlaceBundle(netexStopFile);
        NetexBundle netexBundle = new NetexBundle(netexFile);
        NetexModule netexModule = new NetexModule(new ArrayList<NetexBundle>() {
            {
                add(netexBundle);
            }
        },
                netexStopPlaceBundle);
        this.otpDaoFromNetex = netexModule.getOtpDao();

        GtfsBundle gtfsBundle = new GtfsBundle(gtfsFile);
        GtfsModule gtfsModule = new GtfsModule(new ArrayList<GtfsBundle>() {
            {
                add(gtfsBundle);
            }
        });
        this.otpDaoFromGtfs = gtfsModule.getOtpDao().stream().findFirst().get();
    }

    @Test
    public void singleTripTest() {
        Collection<StopTime> gtfsStopTimes = otpDaoFromGtfs.getAllStopTimes().stream().filter(t -> t.getTrip()
                .getId().getId().equals("RUT:ServiceJourney:215-102139-2968")).collect(Collectors.toList());
        Collection<StopTime> netexStopTimes = otpDaoFromNetex.getAllStopTimes().stream().filter(t -> t.getTrip()
                .getId().getId().equals("RUT:ServiceJourney:215-102139-2968")).collect(Collectors.toList());

        Collection<StopTime> gtfsStopTimesRemove = otpDaoFromGtfs.getAllStopTimes().stream().filter(t -> t.getTrip()
                .getId().getId().equals("RUT:ServiceJourney:215-102139-2968")).collect(Collectors.toList());
        Collection<StopTime> netexStopTimesRemove = otpDaoFromNetex.getAllStopTimes().stream().filter(t -> t.getTrip()
                .getId().getId().equals("RUT:ServiceJourney:215-102139-2968")).collect(Collectors.toList());

        gtfsStopTimes.removeAll(netexStopTimesRemove);
        netexStopTimes.removeAll(gtfsStopTimesRemove);

        Assert.assertEquals(0, gtfsStopTimes.size());
        Assert.assertEquals(0, netexStopTimes.size());
    }

    @Test
    public void testNetexRoutes() {
        ArrayList<Route> routesGtfs = new ArrayList<>(otpDaoFromGtfs.getAllRoutes());
        routesGtfs.removeAll(otpDaoFromNetex.getAllRoutes());

        ArrayList<Route> routesNetex = new ArrayList<>(otpDaoFromNetex.getAllRoutes());
        routesNetex.removeAll(otpDaoFromGtfs.getAllRoutes());

        Assert.assertEquals(0, routesGtfs.size());
        Assert.assertEquals(0, routesNetex.size());
    }

    @Test
    public void testNetexStopTimes() {
        HashSet<StopTime> stopTimesGtfs = new HashSet<>(otpDaoFromGtfs.getAllStopTimes());
        HashSet<StopTime> stopTimesNetex = new HashSet<>(otpDaoFromNetex.getAllStopTimes());

        HashSet<StopTime> stopTimesGtfsComp = new HashSet<>(otpDaoFromGtfs.getAllStopTimes());
        HashSet<StopTime> stopTimesNetexComp = new HashSet<>(otpDaoFromNetex.getAllStopTimes());

        stopTimesGtfs.removeAll(stopTimesNetexComp);
        stopTimesNetex.removeAll(stopTimesGtfsComp);

        Assert.assertEquals(0, stopTimesGtfs.size());
        Assert.assertEquals(0, stopTimesNetex.size());
    }

    @Test
    public void testNetexCalendar() {
        Collection<ServiceCalendarDate> serviceCalendarDates = new ArrayList<>();
        Collection<Date> serviceCalendarDatesRemove = otpDaoFromGtfs.getAllCalendarDates()
                .stream().filter(date -> date.getExceptionType() == 2).map(n -> n.getDate()
                .getAsDate()).collect(Collectors.toList());
        for (ServiceCalendar serviceCalendar : otpDaoFromGtfs.getAllCalendars()) {
            serviceCalendarDates.addAll(calendarToCalendarDates(serviceCalendar, serviceCalendarDatesRemove));
        }

        otpDaoFromGtfs.getAllCalendarDates().addAll(serviceCalendarDates);


        for (ServiceCalendarDate serviceCalendarDate : otpDaoFromNetex.getAllCalendarDates()) {
            String newId = convertServiceIdFormat(serviceCalendarDate.getServiceId().getId());
            serviceCalendarDate.getServiceId().setId(newId);
        }

        Collection<ServiceCalendarDate> datesGtfs = otpDaoFromGtfs.getAllCalendarDates().stream().distinct()
                .filter(d -> d.getExceptionType() == 1) .collect(Collectors.toList());
        Collection<ServiceCalendarDate> datesNetex = otpDaoFromNetex.getAllCalendarDates().stream().distinct()
                .collect(Collectors.toList());

        datesGtfs.removeAll(otpDaoFromNetex.getAllCalendarDates());
        datesNetex.removeAll(otpDaoFromGtfs.getAllCalendarDates());


        Assert.assertEquals(0, datesGtfs.size());
        Assert.assertEquals(0, datesNetex.size());
    }

    private String convertServiceIdFormat(String netexServiceId) {
        String gtfsServiceId = "";
        Boolean first = true;

        String[] splitId = netexServiceId.split("\\+");
        Arrays.sort(splitId);

        for (String singleId : splitId ) {
            if (first) {
                gtfsServiceId += singleId;
                first = false;
            }
            else {
                gtfsServiceId += "-";
                gtfsServiceId += singleId.split(":")[2];
            }
        }

        return gtfsServiceId;
    }

    public Collection<ServiceCalendarDate> calendarToCalendarDates(ServiceCalendar serviceCalendar, Collection<Date> calendarDatesRemove) {
        Collection<ServiceCalendarDate> serviceCalendarDates = new ArrayList<>();

        DateTime startDate = new DateTime(serviceCalendar.getStartDate().getAsDate());
        DateTime endDate = new DateTime(serviceCalendar.getEndDate().getAsDate());

        for (MutableDateTime date = new MutableDateTime(startDate); date.isBefore(endDate); date.addDays(1)) {
            if (calendarDatesRemove.stream().anyMatch(d -> d.toString().equals(date.toString()))) {
                continue;
            }

            ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
            serviceCalendarDate.setExceptionType(1);
            serviceCalendarDate.setServiceId(serviceCalendar.getServiceId());
            switch (date.getDayOfWeek()) {
                case 1:
                    if (serviceCalendar.getMonday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 2:
                    if (serviceCalendar.getTuesday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 3:
                    if (serviceCalendar.getWednesday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 4:
                    if (serviceCalendar.getThursday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 5:
                    if (serviceCalendar.getFriday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 6:
                    if (serviceCalendar.getSaturday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                case 7:
                    if (serviceCalendar.getSunday() == 1) {
                        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
                        serviceCalendarDates.add(serviceCalendarDate);
                    }
                    break;
                default:
                    break;
            }
        }

        return serviceCalendarDates;
    }
}