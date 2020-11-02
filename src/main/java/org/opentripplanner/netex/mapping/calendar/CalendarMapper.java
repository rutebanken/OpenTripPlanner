package org.opentripplanner.netex.mapping.calendar;

import org.glassfish.jersey.internal.util.Producer;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.TripServiceAlteration;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.support.HierarchicalMapById;
import org.opentripplanner.netex.loader.support.HierarchicalMultimap;
import org.opentripplanner.netex.mapping.TripServiceAlterationMapper;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


class CalendarMapper {
    static Map<String, Set<ServiceDate>> mapDayTypesToLocalDates(
        HierarchicalMapById<DayType> dayTypeById,
        HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId,
        HierarchicalMapById<OperatingPeriod> operatingPeriodById
    ) {
        Map<String, Set<ServiceDate>> result = new HashMap<>();
        for (String dayTypeId : dayTypeById.keys()) {

            DayType dayType = dayTypeById.lookup(dayTypeId);
            Collection<LocalDateTime> serviceCalendarDatesForDayType = new HashSet<>();
            Collection<LocalDateTime> serviceCalendarDatesRemoveForDayType = new HashSet<>();

            for (DayTypeAssignment dtAssignment : dayTypeAssignmentByDayTypeId.lookup(dayTypeId)) {
                boolean available = dtAssignment.isIsAvailable() == null || dtAssignment.isIsAvailable();

                // Add or remove single days
                if (dtAssignment.getDate() != null) {
                    LocalDateTime date = dtAssignment.getDate();
                    if (available) {
                        serviceCalendarDatesForDayType.add(date);
                    } else {
                        serviceCalendarDatesRemoveForDayType.add(date);
                    }
                }
                // Add or remove periods
                else if (assignmentHasOperatingPeriod(dtAssignment, operatingPeriodById)) {
                    OperatingPeriod operatingPeriod = operatingPeriodById.lookup(
                        dtAssignment.getOperatingPeriodRef().getRef()
                    );
                    LocalDateTime fromDate = operatingPeriod.getFromDate();
                    LocalDateTime toDate = operatingPeriod.getToDate();

                    EnumSet<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

                    if (dayType.getProperties() != null) {
                        for (PropertyOfDay property : dayType.getProperties().getPropertyOfDay()) {
                            daysOfWeek.addAll(DayOfWeekMapper.mapDayOfWeeks(property.getDaysOfWeek()));
                        }
                    }

                    LocalDateTime toDateExclusive = toDate.plusDays(1);
                    for (LocalDateTime date = fromDate; date.isBefore(toDateExclusive); date = date.plusDays(1)) {
                        if(daysOfWeek.contains(date.getDayOfWeek())) {
                            if (available) {
                                serviceCalendarDatesForDayType.add(date);
                            } else {
                                serviceCalendarDatesRemoveForDayType.add(date);
                            }
                        }
                    }
                }
            }
            serviceCalendarDatesForDayType.removeAll(serviceCalendarDatesRemoveForDayType);

            // Map to ServiceDate
            Set<ServiceDate> newDates = serviceCalendarDatesForDayType
                            .stream()
                            .map(d -> new ServiceDate(d.toLocalDate()))
                            // Prevent accidental updates to the set by making it READ-ONLY
                            .collect(Collectors.toUnmodifiableSet());

            // Add to result
            result.put(dayTypeId, newDates);
        }
        return result;
    }

    static Map<Collection<ServiceDate>, AgencyAndId> mapDatesToServiceId(
            Collection<Set<ServiceDate>> serviceDates,
            Producer<AgencyAndId> serviceIdGenerator
    ) {
        Map<Collection<ServiceDate>, AgencyAndId> serviceIds = new HashMap<>();
        for (Set<ServiceDate> dates : serviceDates) {
            serviceIds.computeIfAbsent(dates, it -> serviceIdGenerator.call());
        }
        return serviceIds;
    }

    static Map<String, Map<ServiceDate, TripServiceAlteration>> createDatedServiceJourneyCalendar(
        final HierarchicalMapById<DatedServiceJourney> datedServiceJourneyById,
        final HierarchicalMapById<OperatingDay> operatingDaysById
    ) {
        Map<String, Map<ServiceDate, TripServiceAlteration>> alternations = new HashMap<>();

        for (DatedServiceJourney dsj : datedServiceJourneyById.values()) {
            var sjId = dsj.getJourneyRef().get(0).getValue().getRef();
            var alt = mapAlterationWithDefaultPlanned(dsj.getServiceAlteration());
            var date = operatingDaysById.lookup(dsj.getOperatingDayRef().getRef()).getCalendarDate();
            var serviceDate = new ServiceDate(date.toLocalDate());

            alternations.computeIfAbsent(sjId, (k) -> new HashMap<>()).put(serviceDate, alt);
        }
        return alternations;
    }

    private static boolean assignmentHasOperatingPeriod(
        DayTypeAssignment dtAssignment,
        HierarchicalMapById<OperatingPeriod> operatingPeriodById
    ) {
        var ref = dtAssignment.getOperatingPeriodRef();
        return ref != null && operatingPeriodById.containsKey(ref.getRef());
    }

    private static TripServiceAlteration mapAlterationWithDefaultPlanned(ServiceAlterationEnumeration netexValue) {
        if (netexValue == null) {
            return TripServiceAlteration.planned;
        }
        return TripServiceAlterationMapper.mapAlteration(netexValue);
    }
}