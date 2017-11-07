package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.impl.GtfsDaoImpl;
import org.onebusaway2.gtfs.model.*;
import org.onebusaway2.gtfs.model.NoticeAssignment;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Notice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class NetexMapper {

    private static final Logger LOG = LoggerFactory.getLogger(NetexMapper.class);

    GtfsDaoImpl gtfsDao = new GtfsDaoImpl();

    AgencyMapper agencyMapper = new AgencyMapper();
    RouteMapper routeMapper = new RouteMapper();
    StopMapper stopMapper = new StopMapper();
    TripPatternMapper tripPatternMapper = new TripPatternMapper();
    CalendarMapper calendarMapper = new CalendarMapper();
    NoticeMapper noticeMapper = new NoticeMapper();
    NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper();
    TransferMapper transferMapper = new TransferMapper();

    public GtfsDao mapNetexToOtp(NetexDao netexDao) {
        for (Operator operator : netexDao.getOperators().values()) {
            if (operator != null) {
                gtfsDao.getAllAgencies().add(agencyMapper.mapAgency(operator, "Europe/Oslo"));
            }
        }

        for (Line line : netexDao.getLineById().values()) {
            if (line != null) {
                Route route = routeMapper.mapRoute(line, gtfsDao);
                gtfsDao.getAllRoutes().add(route);
                gtfsDao.getRouteById().put(route.getId(), route);
            }
        }

        for (StopPlace stopPlace : netexDao.getMultimodalStopPlaceById().values()) {
            if (stopPlace != null) {
                Stop stop = stopMapper.mapMultiModalStop(stopPlace);
                gtfsDao.getMultiModalStops().put(stop.getId(), stop);
            }
        }

        for (StopPlace stopPlace : netexDao.getStopPlaceMap().values()) {
            if (stopPlace != null) {
                for (Stop stop : stopMapper.mapParentAndChildStops(stopPlace, gtfsDao)) {
                    gtfsDao.stopsById().put(stop.getId(), stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexDao.getJourneyPatternsById().values()) {
            if (journeyPattern != null) {
                tripPatternMapper.mapTripPattern(journeyPattern, gtfsDao, netexDao);
                gtfsDao.getTripsById().putAll(gtfsDao.getAllTrips().stream().collect(toMap(Trip::getId, identity())));
            }
        }

        for (String serviceId : netexDao.getServiceIds().values()) {
            gtfsDao.getAllCalendarDates().addAll(calendarMapper.mapToCalendarDates(AgencyAndIdFactory.getAgencyAndId(serviceId), netexDao));
        }

        for (Notice notice : netexDao.getNoticeMap().values()) {
            if (notice != null) {
                org.onebusaway2.gtfs.model.Notice otpNotice = noticeMapper.mapNotice(notice);
                gtfsDao.getNoticeById().put(otpNotice.getId(), otpNotice);
            }
        }

        for (org.rutebanken.netex.model.NoticeAssignment noticeAssignment : netexDao.getNoticeAssignmentMap().values()) {
            if (noticeAssignment != null) {
                Collection<NoticeAssignment> otpNoticeAssignments = noticeAssignmentMapper.mapNoticeAssignment(noticeAssignment, netexDao);
                for (org.onebusaway2.gtfs.model.NoticeAssignment otpNoticeAssignment : otpNoticeAssignments) {
                    gtfsDao.getNoticeAssignmentById().put(otpNoticeAssignment.getId(), otpNoticeAssignment);
                }
            }
        }

        for (org.rutebanken.netex.model.ServiceJourneyInterchange interchange : netexDao.getInterchanges().values()) {
            if (interchange != null) {
                Transfer transfer = transferMapper.mapTransfer(interchange, gtfsDao, netexDao);
                if (transfer != null) {
                    gtfsDao.getAllTransfers().add(transfer);
                }
            }
        }

        return gtfsDao;
    }
}
