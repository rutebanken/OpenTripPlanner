package org.opentripplanner.netex;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.netex.loader.NetexLoader;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.OTPMain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class MappingTest {
    static final String gtfsFile = "src/test/resources/netex_mapping_test/gtfs_minimal_fileset/gtfs_minimal.zip";
    static final File netexFile = new File("src/test/resources/netex_mapping_test/netex_minimal_fileset/netex_minimal.zip");
    static final File netexConfigFile = new File("src/test/resources/netex_mapping_test/build-config.json");

    private static OtpTransitBuilder otpBuilderFromGtfs;
    private static OtpTransitBuilder otpBuilderFromNetex;

    @BeforeClass
    public static void setUpNetexMapping() throws Exception {
        JsonNode buildConfig = OTPMain.loadJson(netexConfigFile);
        NetexBundle netexBundle = new NetexBundle(netexFile, new GraphBuilderParameters(buildConfig));

        otpBuilderFromNetex = new NetexLoader(netexBundle, gba -> {}).loadBundle();
        otpBuilderFromGtfs = GtfsContextBuilder
                .contextBuilder(gtfsFile)
                .turnOnSetAgencyToFeedIdForAllElements().build()
                .getTransitBuilder();
    }

    @Test
    public void testAgencies() {
        List<Agency> ga = otpBuilderFromGtfs.getAgencies();
        List<Agency> na = otpBuilderFromNetex.getAgencies();

        // TODO TGR - fix this test
        Assert.assertEquals(ga.size(), na.size());
    }

    @Test
    public void testNetexRoutes() {
        // TODO TGR - fix this test
        List<Route> routesGtfs = new ArrayList<Route>(otpBuilderFromGtfs.getRoutes().values());
        routesGtfs.removeAll(otpBuilderFromNetex.getRoutes().values());

        ArrayList<Route> routesNetex = new ArrayList<>(otpBuilderFromNetex.getRoutes().values());
        routesNetex.removeAll(otpBuilderFromGtfs.getRoutes().values());

        Assert.assertEquals(0, routesGtfs.size());
        Assert.assertEquals(0, routesNetex.size());
    }

    @Test
    @Ignore
    public void testNetexStopTimes() {
        HashSet<StopTime> stopTimesGtfs = new HashSet<>(otpBuilderFromGtfs.getStopTimesSortedByTrip().values());
        HashSet<StopTime> stopTimesNetex = new HashSet<>(otpBuilderFromNetex.getStopTimesSortedByTrip().values());

        HashSet<StopTime> stopTimesGtfsComp = new HashSet<>(otpBuilderFromGtfs.getStopTimesSortedByTrip().values());
        HashSet<StopTime> stopTimesNetexComp = new HashSet<>(otpBuilderFromNetex.getStopTimesSortedByTrip().values());

        stopTimesGtfs.removeAll(stopTimesNetexComp);
        stopTimesNetex.removeAll(stopTimesGtfsComp);

        Assert.assertEquals(0, stopTimesGtfs.size());
        Assert.assertEquals(0, stopTimesNetex.size());
    }

    @Test
    public void testNetexCalendar() {
        Collection<ServiceCalendarDate> serviceCalendarDates = new ArrayList<>();
        Collection<Date> serviceCalendarDatesRemove = otpBuilderFromGtfs.getCalendarDates()
                .stream().filter(date -> date.getExceptionType() == 2).map(n -> n.getDate()
                .getAsDate()).collect(Collectors.toList());
        for (ServiceCalendar serviceCalendar : otpBuilderFromGtfs.getCalendars()) {
            serviceCalendarDates.addAll(calendarToCalendarDates(serviceCalendar, serviceCalendarDatesRemove));
        }

        otpBuilderFromGtfs.getCalendarDates().addAll(serviceCalendarDates);

        for (ServiceCalendarDate serviceCalendarDate : otpBuilderFromNetex.getCalendarDates()) {
            String newId = convertServiceIdFormat(serviceCalendarDate.getServiceId().getId());
            serviceCalendarDate.setServiceId(new AgencyAndId(serviceCalendarDate.getServiceId().getAgencyId(),newId));
        }

        Collection<ServiceCalendarDate> datesGtfs = otpBuilderFromGtfs.getCalendarDates().stream()
                .filter(d -> d.getExceptionType() == 1).collect(Collectors.toList());
        final Collection<ServiceCalendarDate> datesNetex = new ArrayList<>(otpBuilderFromNetex.getCalendarDates());

        Set<ServiceCalendarDate> removeGtfs = new HashSet<>(datesGtfs);
        Set<ServiceCalendarDate> removeNetex = new HashSet<>(datesNetex);

        datesGtfs.removeAll(removeNetex);
        datesNetex.removeAll(removeGtfs);

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

        for (MutableDateTime date = new MutableDateTime(startDate); date.isBefore(endDate.plusDays(1)); date.addDays(1)) {
            if (calendarDatesRemove.stream().map(it -> it.toString()).collect(Collectors.toList()).contains(date.toDate().toString())) {
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