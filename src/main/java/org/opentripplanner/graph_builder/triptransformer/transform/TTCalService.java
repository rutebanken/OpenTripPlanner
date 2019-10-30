package org.opentripplanner.graph_builder.triptransformer.transform;

import com.google.common.collect.HashMultimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class TTCalService {
    private final List<ServiceCalendarDate> calendarDates;

    private final HashMultimap<AgencyAndId, ServiceDate> datesByServiceId = HashMultimap.create();
    private final HashMultimap<ServiceDate, AgencyAndId> serviceIdsByDate = HashMultimap.create();

    private final String feedId;


    TTCalService(List<ServiceCalendarDate> calendarDates) {
        this.calendarDates = calendarDates;
        this.feedId = calendarDates.get(0).getServiceId().getAgencyId();

        for (ServiceCalendarDate it : calendarDates) {
            if(it.getExceptionType() == 1) {
                datesByServiceId.put(it.getServiceId(), it.getDate());
                serviceIdsByDate.put(it.getDate(), it.getServiceId());
            }
        }
    }

    Set<AgencyAndId> getServiceIdsForDate(ServiceDate date) {
        return serviceIdsByDate.get(date);
    }

    NewServiceIds newServiceIds(AgencyAndId originalServiceId, ServiceDate originalDate, ServiceDate targetDate) {
        Set<ServiceDate> oldDates = datesByServiceId.get(originalServiceId);
        Set<ServiceDate> newOtherDates = oldDates.stream().filter(it -> !it.equals(originalDate)).collect(Collectors.toSet());
        return new NewServiceIds(getServiceIdForOnlyDate(targetDate), getServiceIdForDates(newOtherDates));
    }

    AgencyAndId getServiceIdForOnlyDate(ServiceDate date) {
        for (AgencyAndId serviceId : serviceIdsByDate.get(date)) {
            if (isOnlyOneDatesForServiceId(serviceId)) {
                return serviceId;
            }
        }
        return createServiceIdForDates(Collections.singleton(date));
    }

    boolean isOnlyOneDatesForServiceId(AgencyAndId serviceId) {
        return datesByServiceId.get(serviceId).size() == 1;
    }

    private AgencyAndId getServiceIdForDates(Set<ServiceDate> dates) {
        if(dates.isEmpty()) return null;

        Set<AgencyAndId> serviceIds = new HashSet<>();
        dates.forEach(it -> serviceIds.addAll(serviceIdsByDate.get(it)));

        for (AgencyAndId serviceId : serviceIds) {
            if(datesByServiceId.get(serviceId).equals(dates)) {
                return serviceId;
            }
        }
        return createServiceIdForDates(dates);
    }

    private AgencyAndId createServiceIdForDates(Set<ServiceDate> dates) {
        AgencyAndId serviceId = newServiceId(dates);
        for (ServiceDate date : dates) {
            calendarDates.add(newCalDate(serviceId, date));
            datesByServiceId.put(serviceId, date);
            serviceIdsByDate.put(date, serviceId);
        }
        return serviceId;
    }

    private ServiceCalendarDate newCalDate(AgencyAndId serviceId, ServiceDate date) {
        ServiceCalendarDate v = new ServiceCalendarDate();
        v.setExceptionType(ServiceCalendarDate.EXCEPTION_TYPE_ADD);
        v.setDate(date);
        v.setServiceId(serviceId);
        return v;
    }

    private AgencyAndId newServiceId(Collection<ServiceDate> dates) {
        String temp = "SrvId-" + dates.stream()
                .sorted()
                .map(d -> String.format("%d%02d%02d", d.getYear()-2000, d.getMonth(), d.getDay()))
                .collect(Collectors.joining("-"));
        return new AgencyAndId(feedId, temp);
    }

}
