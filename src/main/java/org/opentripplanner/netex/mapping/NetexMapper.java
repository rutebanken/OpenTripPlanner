package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.model.transfers.Transfer;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.calendar.DatedServiceJourneyMapper;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * This is the ROOT mapper to map from the Netex domin model into the OTP internal model. This class delegates to
 * type/argegate specific mappers and take the result from each such mapper and add the result to the
 * {@link OtpTransitServiceBuilder}.
 * </p>
 * <p>
 * The transit builder is updated with the new OTP model entities, holding ALL entities parsed so fare including
 * previous Netex files in the same bundle. This enable the mapping code to make direct references between entities
 * in the OTP domain model.
 * </p>
 */
public class NetexMapper {

    private final OtpTransitServiceBuilder transitBuilder;
    private final FeedScopedIdFactory idFactory;
    private final Deduplicator deduplicator;
    private final DataImportIssueStore issueStore;
    private final Multimap<String, Station> stationsByMultiModalStationRfs = ArrayListMultimap.create();
    private final CalendarServiceBuilder calendarServiceBuilder;
    private final TripCalendarBuilder tripCalendarBuilder;

    /**
     * This is needed to assign a notice to a stop time. It is not part of the target OTPTransitService,
     * so we need to temporally cash this here.
     */
    private final Map<String, StopTime> stopTimesByNetexId = new HashMap<>();

    public NetexMapper(
            OtpTransitServiceBuilder transitBuilder,
            String feedId,
            Deduplicator deduplicator,
            DataImportIssueStore issueStore
    ) {
        this.transitBuilder = transitBuilder;
        this.deduplicator = deduplicator;
        this.idFactory = new FeedScopedIdFactory(feedId);
        this.issueStore = issueStore;
        this.calendarServiceBuilder = new CalendarServiceBuilder(idFactory);
        this.tripCalendarBuilder = new TripCalendarBuilder(this.calendarServiceBuilder, issueStore);
    }

    /**
     * Prepare to for mapping of a new sub-level of entities(shared-files, shared-group-files and
     * group-files). This is a life-cycle method used to notify this class that a new dataset is
     * about to be processed. Any existing intermediate state must be saved, so it can be accessed
     * during the next call to {@link #mapNetexToOtp(NetexEntityIndexReadOnlyView)} and after.
     */
    public NetexMapper push() {
        this.tripCalendarBuilder.push();
        return this;
    }

    /**
     * It is now safe to discard any intermediate state generated by the last call to
     * {@link #mapNetexToOtp(NetexEntityIndexReadOnlyView)}.
     */
    public NetexMapper pop() {
        this.tripCalendarBuilder.pop();
        return this;
    }

    /**
     * Any post processing step in the mapping is done in this method. The method is called
     * ONCE after all other mapping is complete. Note! Hierarchical data structures are not
     * accessible any more.
     */
    public void finnishUp() {
        // Add Calendar data created during the mapping of dayTypes, dayTypeAssignments,
        // datedServiceJourney and ServiceJourneys
        transitBuilder.getCalendarDates().addAll(
            calendarServiceBuilder.createServiceCalendar()
        );
    }

    /**
     * <p>
     * This method mapes the last Netex file imported using the *local* entities in the
     * hierarchical {@link NetexEntityIndexReadOnlyView}.
     * </p>
     * <p>
     * Note that the order in which the elements are mapped is important. For example, if a file
     * contains Authorities, Line and Notices - they need to be mapped in that order, since
     * Route have a reference on Agency, and Notice may reference on Route.
     * </p>
     *
     * @param netexIndex The parsed Netex entities to be mapped
     */
    public void mapNetexToOtp(NetexEntityIndexReadOnlyView netexIndex) {
        // Be careful, the order matter. For example a Route has a reference to Agency; Hence Agency must be mapped
        // before Route - if both entities are defined in the same file.
        mapAuthorities(netexIndex);
        mapOperators(netexIndex);
        mapShapePoints(netexIndex);
        mapTariffZones(netexIndex);
        mapStopPlaceAndQuays(netexIndex);
        mapMultiModalStopPlaces(netexIndex);
        mapGroupsOfStopPlaces(netexIndex);
        mapFlexibleStopPlaces(netexIndex);
        mapDatedServiceJourneys(netexIndex);
        mapDayTypeAssignments(netexIndex);

        // DayType and DSJ is mapped to a service calendar and a serviceId is generated
        Map<String, FeedScopedId> serviceIds = createCalendarForServiceJourney(netexIndex);

        mapRoute(netexIndex);
        mapTripPatterns(serviceIds, netexIndex);
        mapInterchanges(netexIndex);
        mapNoticeAssignments(netexIndex);
    }


    /* PRIVATE METHODS */

    private void mapAuthorities(NetexEntityIndexReadOnlyView netexIndex) {
        AuthorityToAgencyMapper agencyMapper = new AuthorityToAgencyMapper(idFactory, netexIndex.getTimeZone());
        for (Authority authority : netexIndex.getAuthoritiesById().localValues()) {
            Agency agency = agencyMapper.mapAuthorityToAgency(authority);
            transitBuilder.getAgenciesById().add(agency);
        }
    }

    private void mapOperators(NetexEntityIndexReadOnlyView netexIndex) {
        OperatorToAgencyMapper mapper = new OperatorToAgencyMapper(idFactory);
        for (org.rutebanken.netex.model.Operator operator : netexIndex.getOperatorsById().localValues()) {
            transitBuilder.getOperatorsById().add(mapper.mapOperator(operator));
        }
    }

    private void mapShapePoints(NetexEntityIndexReadOnlyView netexIndex) {
        ServiceLinkMapper serviceLinkMapper = new ServiceLinkMapper(idFactory, issueStore);
        for (JourneyPattern journeyPattern : netexIndex.getJourneyPatternsById().localValues()) {

            Collection<ShapePoint> shapePoints = serviceLinkMapper.getShapePointsByJourneyPattern(
                journeyPattern,
                netexIndex.getServiceLinkById(),
                netexIndex.getQuayIdByStopPointRef(),
                netexIndex.getQuayById());

            for (ShapePoint shapePoint : shapePoints) {
                transitBuilder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
            }
        }
    }

    private void mapTariffZones(NetexEntityIndexReadOnlyView netexIndex) {
        TariffZoneMapper tariffZoneMapper = new TariffZoneMapper(idFactory);
        for (TariffZone tariffZone : netexIndex.getTariffZonesById().localValues()) {
            transitBuilder.getFareZonesById().add(tariffZoneMapper.mapTariffZone(tariffZone));
        }
    }

    private void mapStopPlaceAndQuays(NetexEntityIndexReadOnlyView netexIndex) {
        for (String stopPlaceId : netexIndex.getStopPlaceById().localKeys()) {
            Collection<StopPlace> stopPlaceAllVersions = netexIndex.getStopPlaceById().lookup(stopPlaceId);
            StopAndStationMapper stopMapper = new StopAndStationMapper(
                idFactory,
                netexIndex.getQuayById(),
                transitBuilder.getFareZonesById(),
                issueStore
            );
            stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
            transitBuilder.getStops().addAll(stopMapper.resultStops);
            transitBuilder.getStations().addAll(stopMapper.resultStations);
            stationsByMultiModalStationRfs.putAll(stopMapper.resultStationByMultiModalStationRfs);
        }
    }

    private void mapMultiModalStopPlaces(NetexEntityIndexReadOnlyView netexIndex) {
        MultiModalStationMapper mapper = new MultiModalStationMapper(idFactory);
        for (StopPlace multiModalStopPlace : netexIndex.getMultiModalStopPlaceById().localValues()) {
            transitBuilder.getMultiModalStationsById().add(
                mapper.map(
                    multiModalStopPlace,
                    stationsByMultiModalStationRfs.get(multiModalStopPlace.getId())
                )
            );
        }
    }

    private void mapGroupsOfStopPlaces(NetexEntityIndexReadOnlyView netexIndex) {
        GroupOfStationsMapper groupOfStationsMapper = new GroupOfStationsMapper(
                idFactory,
                transitBuilder.getMultiModalStationsById(),
                transitBuilder.getStations()
        );
        for (GroupOfStopPlaces groupOfStopPlaces : netexIndex.getGroupOfStopPlacesById().localValues()) {
            transitBuilder.getGroupsOfStationsById().add(groupOfStationsMapper.map(groupOfStopPlaces));
        }
    }

    private void mapFlexibleStopPlaces(NetexEntityIndexReadOnlyView netexIndex) {
        FlexStopLocationMapper flexStopLocationMapper = new FlexStopLocationMapper(idFactory);

        for (FlexibleStopPlace flexibleStopPlace : netexIndex.getFlexibleStopPlacesById().localValues()) {
            FlexStopLocation stopLocation = flexStopLocationMapper.map(flexibleStopPlace);
            if (stopLocation != null) {
                transitBuilder.getLocations().add(stopLocation);
            }
        }
    }

    private void mapDatedServiceJourneys(NetexEntityIndexReadOnlyView netexIndex) {
        tripCalendarBuilder.addDatedServiceJourneys(
            netexIndex.getOperatingDayById(),
            DatedServiceJourneyMapper.indexDSJBySJId(
                netexIndex.getDatedServiceJourneys()
            )
        );
    }

    private void mapDayTypeAssignments(NetexEntityIndexReadOnlyView netexIndex) {
        tripCalendarBuilder.addDayTypeAssignments(
            netexIndex.getDayTypeById(),
            netexIndex.getDayTypeAssignmentByDayTypeId(),
            netexIndex.getOperatingDayById(),
            netexIndex.getOperatingPeriodById()
        );
    }

    private Map<String, FeedScopedId> createCalendarForServiceJourney(NetexEntityIndexReadOnlyView netexIndex) {
        return tripCalendarBuilder.createTripCalendar(
            netexIndex.getServiceJourneyById().localValues()
        );
    }

    private void mapRoute(NetexEntityIndexReadOnlyView netexIndex) {
        RouteMapper routeMapper = new RouteMapper(
                idFactory,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
                netexIndex,
                netexIndex.getTimeZone()
        );
        for (Line line : netexIndex.getLineById().localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }
        for (FlexibleLine line : netexIndex.getFlexibleLineById().localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }
    }

    private void mapTripPatterns(
        Map<String, FeedScopedId> serviceIds,
        NetexEntityIndexReadOnlyView netexIndex
    ) {
        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                idFactory,
                transitBuilder.getOperatorsById(),
                transitBuilder.getStops(),
                transitBuilder.getLocations(),
                transitBuilder.getRoutes(),
                transitBuilder.getShapePoints().keySet(),
                netexIndex.getRouteById(),
                netexIndex.getJourneyPatternsById(),
                netexIndex.getQuayIdByStopPointRef(),
                netexIndex.getFlexibleStopPlaceByStopPointRef(),
                netexIndex.getDestinationDisplayById(),
                netexIndex.getServiceJourneyById(),
                netexIndex.getFlexibleLineById(),
                serviceIds,
                deduplicator
        );

        for (JourneyPattern journeyPattern : netexIndex.getJourneyPatternsById().localValues()) {
            TripPatternMapper.Result result = tripPatternMapper.mapTripPattern(journeyPattern);

            for (Map.Entry<Trip, List<StopTime>> it : result.tripStopTimes.entrySet()) {
                transitBuilder.getStopTimesSortedByTrip().put(it.getKey(), it.getValue());
                transitBuilder.getTripsById().add(it.getKey());
            }
            for (TripPattern it : result.tripPatterns) {
                transitBuilder.getTripPatterns().put(it.stopPattern, it);
            }
            stopTimesByNetexId.putAll(result.stopTimeByNetexId);
        }
    }

    private void mapInterchanges(NetexEntityIndexReadOnlyView netexIndex) {
        var mapper = new TransferMapper(
            idFactory,
            netexIndex.getQuayIdByStopPointRef(),
            transitBuilder.getStops(),
            transitBuilder.getTripsById()
        );
        for (ServiceJourneyInterchange it : netexIndex.getServiceJourneyInterchangeById().localValues()) {
            Transfer result = mapper.mapToTransfer(it);
            if(result != null) {
                transitBuilder.getTransfers().add(result);
            }
        }
    }

    private void mapNoticeAssignments(NetexEntityIndexReadOnlyView netexIndex) {
        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                idFactory,
                netexIndex.getServiceJourneyById().localValues(),
                netexIndex.getNoticeById(),
                transitBuilder.getRoutes(),
                transitBuilder.getTripsById(),
                stopTimesByNetexId
        );
        for (NoticeAssignment noticeAssignment : netexIndex.getNoticeAssignmentById().localValues()) {
            Multimap<TransitEntity, Notice> noticesByElementId;
            noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
            transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
        }
    }
}
