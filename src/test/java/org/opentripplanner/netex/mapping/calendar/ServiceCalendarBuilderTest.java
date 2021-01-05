package org.opentripplanner.netex.mapping.calendar;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.netex.mapping.AgencyAndIdFactory;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;

import javax.xml.bind.JAXBElement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createDatedServiceJourney;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayType;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeAssignment;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingPeriod;
import static org.opentripplanner.netex.NetexTestDataSupport.jaxbElement;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.FRIDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.MONDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.SATURDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.SUNDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEDNESDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEEKDAYS;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEEKEND;
import static org.rutebanken.netex.model.ServiceAlterationEnumeration.PLANNED;

public class ServiceCalendarBuilderTest {
  private static final LocalDate D2020_11_02 = LocalDate.of(2020, 11, 2);
  private static final LocalDate D2020_11_08 = LocalDate.of(2020, 11, 8);

  private static final String OP_1 = "OP-1";

  private static final String OP_DAY_1 = "OD-1";

  private static final String DAY_TYPE_1 = "D1";
  private static final String DAY_TYPE_2 = "D2";
  private static final String DAY_TYPE_3 = "D3";
  private static final String DAY_TYPE_4 = "D4";
  private static final String DAY_TYPE_5 = "D5";

  private static final String SJ_1 = "SJ-1";
  private static final String SJ_2 = "SJ-2";
  private static final String SJ_3 = "SJ-3";
  private static final String SJ_4 = "SJ-4";
  private static final String SJ_5 = "SJ-5";

  private static final Boolean AVAILABLE = TRUE;

  // Add Calendar(DayType, DayTypeAssignment, OperationPeriod) to level 1
  private final NetexDao netexDaoLvl1 = NetexDao.createForTest(null);

  // Add ServiceJourneys to level 2 A
  private final NetexDao netexDaoLvl2A = NetexDao.createForTest(netexDaoLvl1);

  // Add ServiceJourneys to level 2 B, data here should not interfare with data in A
  private final NetexDao netexDaoLvl2B = NetexDao.createForTest(netexDaoLvl1);


  @Before
  public void setup() {
    AgencyAndIdFactory.setAgencyId("F");
  }

  @Test
  public void tripServiceAlterationsBySJId() {
    netexDaoLvl1.operatingDaysById.add(createOperatingDay(OP_DAY_1, D2020_11_02));
    netexDaoLvl2A.datedServiceJourneyById.add(createDatedServiceJourney("1", OP_DAY_1, SJ_2, PLANNED));

    ServiceCalendarBuilder builder = new ServiceCalendarBuilder();
    builder.buildCalendar(netexDaoLvl1);

    builder.pushCache();
    builder.buildCalendar(netexDaoLvl2A);
    assertEquals(TripAlteration.planned , builder.tripServiceAlterationsBySJId().get(SJ_2));
    builder.popCache();
    builder.pushCache();
    builder.buildCalendar(netexDaoLvl2B);
    assertEquals("{}" , builder.tripServiceAlterationsBySJId().toString());
    builder.popCache();
  }

  @Test
  public void buildCalendar() {
    setupBuildCalendar();
    ServiceCalendarBuilder builder = new ServiceCalendarBuilder();

    builder.buildCalendar(netexDaoLvl1);

    builder.pushCache();
    builder.buildCalendar(netexDaoLvl2A);
    verifyBuildCalendarLevel2A(builder);
    var serviceId_SJ2 = builder.serviceIdByServiceJourneyId().get(SJ_2);
    builder.popCache();

    builder.pushCache();
    builder.buildCalendar(netexDaoLvl2B);
    verifyBuildCalendarLevel2B(builder, serviceId_SJ2);
    builder.popCache();
  }

  private void setupBuildCalendar() {

    netexDaoLvl1.dayTypeById.add(createDayType(DAY_TYPE_1, WEEKDAYS));
    netexDaoLvl1.dayTypeById.add(createDayType(DAY_TYPE_2, WEEKEND));
    netexDaoLvl1.dayTypeById.add(createDayType(DAY_TYPE_3, SATURDAY));
    netexDaoLvl1.dayTypeById.add(createDayType(DAY_TYPE_4, SUNDAY));
    netexDaoLvl1.dayTypeById.add(createDayType(DAY_TYPE_5, MONDAY, WEDNESDAY, FRIDAY));

    netexDaoLvl1.operatingPeriodById.add(createOperatingPeriod(OP_1, D2020_11_02, D2020_11_08));

    // Create DayTypeAssignments for the first week in november for all dayTypes
    List.of(DAY_TYPE_1, DAY_TYPE_2, DAY_TYPE_3, DAY_TYPE_4, DAY_TYPE_5).forEach(dt ->
        netexDaoLvl1.dayTypeAssignmentByDayTypeId.add(dt, createDayTypeAssignment(dt, OP_1, AVAILABLE))
    );

    // SJ 1 : Mon - Fri (Weekdays)
    // SJ 2 : Sat, Sun (Weekend)
    // SJ 3 : Mon, Wed, Thu
    // SJ 4 : Sat, Sun - Same as SJ 2, but combining DayType 4 and 5
    netexDaoLvl2A.serviceJourneyById.add(createServiceJourney(SJ_1, DAY_TYPE_1));
    netexDaoLvl2A.serviceJourneyById.add(createServiceJourney(SJ_2, DAY_TYPE_2));
    netexDaoLvl2A.serviceJourneyById.add(createServiceJourney(SJ_3, DAY_TYPE_3, DAY_TYPE_4));
    netexDaoLvl2A.serviceJourneyById.add(createServiceJourney(SJ_4, DAY_TYPE_3, DAY_TYPE_5));
    netexDaoLvl2B.serviceJourneyById.add(createServiceJourney(SJ_5, DAY_TYPE_5));
  }

  private void verifyBuildCalendarLevel2A(ServiceCalendarBuilder builder) {
    List<ServiceCalendarDate> calendarDates = builder.calendarDates();

    var serviceId1 = builder.serviceIdByServiceJourneyId().get(SJ_1);
    var serviceId2 = builder.serviceIdByServiceJourneyId().get(SJ_2);
    var serviceId3 = builder.serviceIdByServiceJourneyId().get(SJ_3);
    var serviceId4 = builder.serviceIdByServiceJourneyId().get(SJ_4);

    assertEquals(
        // Mon - Fri
        "2020-11-02, 2020-11-03, 2020-11-04, 2020-11-05, 2020-11-06",
        toStr(serviceId1, calendarDates)
    );
    assertEquals(
        // Weekend
        "2020-11-07, 2020-11-08",
        toStr(serviceId2, calendarDates)
    );

    // Test reuse of DayTypes, in another combination. Expect [Mon, Wed, Fri] + [Sat]
    assertEquals(
        "2020-11-02, 2020-11-04, 2020-11-06, 2020-11-07",
        toStr(serviceId4, calendarDates)
    );

    // SJ 2 and 3 run on the same days(Sat, Sun), there should be just ONE serviceId used by both
    assertEquals(serviceId2, serviceId3);
  }

  private void verifyBuildCalendarLevel2B(ServiceCalendarBuilder builder, AgencyAndId sameIdAsSJ2) {
    var serviceId4 = builder.serviceIdByServiceJourneyId().get(SJ_5);

    assertEquals(
        // Mon, Wed, Fri
        "2020-11-02, 2020-11-04, 2020-11-06",
        toStr(serviceId4, builder.calendarDates())
    );
  }

  private String toStr(AgencyAndId serviceId, Collection<ServiceCalendarDate> list) {
    return list.stream()
        .filter(it -> it.getServiceId().equals(serviceId))
        .map(it -> it.getDate().toString())
        .sorted()
        .collect(Collectors.joining(", "));
  }

  private static ServiceJourney createServiceJourney(String sjId, String ... dayTypes) {
    var dtList = new ArrayList<JAXBElement<? extends DayTypeRefStructure>>();
    for (String it : dayTypes) {
      dtList.add(jaxbElement(new DayTypeRefStructure().withRef(it), DayTypeRefStructure.class));
    }
    return new ServiceJourney()
        .withId(sjId)
        .withDayTypes(new DayTypeRefs_RelStructure().withDayTypeRef(dtList));
  }
}