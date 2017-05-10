package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.onebusaway2.gtfs.model.*;
import org.onebusaway2.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl;
import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.model.NetexBundle;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.graph_builder.model.NetexToGtfsDao;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.factory.NetexPatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.NetexStopContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;

public class NetexModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private List<NetexBundle> netexBundles;

    private NetexDao dao;

    public NetexModule(List<NetexBundle> netexBundles) {
        this.netexBundles = netexBundles;
        dao = new NetexDao();
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        NetexStopContext netexStopContext = new NetexStopContext();

        try{
            for(NetexBundle bundle : netexBundles){
                loadBundle(bundle);

                NetexToGtfsDao otpDao = new NetexToGtfsDao(this.dao);
                calendarService.addData(createCalendarServiceData(otpDao), otpDao);

                NetexPatternHopFactory hf = new NetexPatternHopFactory();
                hf.setStopContext(netexStopContext);
                hf.setNetexDao(this.dao);
                hf.run(graph);

            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        CalendarServiceData data = calendarService.getData();
        graph.putService(CalendarServiceData.class, data);
        graph.updateTransitFeedValidity(data);

        graph.hasTransit = true;
        graph.calculateTransitCenter();

    }

    @Override
    public void checkInputs() {
        netexBundles.forEach(NetexBundle::checkInputs);
    }

    void loadBundle(NetexBundle netexBundle) throws Exception {
        loadFile(netexBundle.getCommonFile());
        List<ZipEntry> entries = netexBundle.getFileEntries();
        for(ZipEntry entry : entries){
            InputStream fileInputStream = netexBundle.getFileInputStream(entry);
            loadFile(fileInputStream);
        }
    }

    private void loadFile(InputStream is) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        byte[] bytesArray = IOUtils.toByteArray(is);

        JAXBElement<PublicationDeliveryStructure> jaxbElement = (JAXBElement<PublicationDeliveryStructure>) unmarshaller
                .unmarshal(new ByteArrayInputStream(bytesArray));

        PublicationDeliveryStructure value = jaxbElement.getValue();
        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrames = value.getDataObjects().getCompositeFrameOrCommonFrame();
        for(JAXBElement frame : compositeFrameOrCommonFrames){

            if(frame.getValue() instanceof CompositeFrame) {
                CompositeFrame cf = (CompositeFrame) frame.getValue();
                VersionFrameDefaultsStructure frameDefaults = cf.getFrameDefaults();
                String timeZone = "GMT";
                if(frameDefaults != null && frameDefaults.getDefaultLocale() != null
                        && frameDefaults.getDefaultLocale().getTimeZone() != null){
                    timeZone = frameDefaults.getDefaultLocale().getTimeZone();
                }

                dao.setTimeZone(timeZone);
                List<JAXBElement<? extends Common_VersionFrameStructure>> commonFrames = cf.getFrames().getCommonFrame();
                for (JAXBElement commonFrame : commonFrames) {
                    loadResourceFrames(commonFrame);
                    loadSiteFrames(commonFrame);
                    loadServiceCalendarFrames(commonFrame);
                    loadTimeTableFrames(commonFrame);
                    loadServiceFrames(commonFrame);
                }

            }
        }

        for(JourneyPattern journeyPattern : dao.getJourneyPatternsById().values()){
            TripPattern tripPattern = mapTripPattern(journeyPattern);
            dao.getTripPatterns().add(tripPattern);
        }
        dao.clearJourneyPatterns();
    }

    private void loadServiceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //stop assignments
            StopAssignmentsInFrame_RelStructure stopAssignments = sf.getStopAssignments();
            if(stopAssignments != null){
                List<JAXBElement<? extends StopAssignment_VersionStructure>> assignments = stopAssignments.getStopAssignment();
                for (JAXBElement assignment : assignments) {
                    if(assignment.getValue() instanceof PassengerStopAssignment) {
                        PassengerStopAssignment passengerStopAssignment = (PassengerStopAssignment) assignment.getValue();
                        dao.getStopPointStopPlaceMap().put(passengerStopAssignment.getScheduledStopPointRef().getRef(), passengerStopAssignment.getStopPlaceRef().getRef());
                    }
                }
            }

            //routes
            RoutesInFrame_RelStructure routes = sf.getRoutes();
            if(routes != null){
                List<JAXBElement<? extends LinkSequence_VersionStructure>> route_ = routes.getRoute_();
                for (JAXBElement element : route_) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        dao.getRouteById().put(route.getId(), route);
                    }
                }
            }

            //network
            Network network = sf.getNetwork();
            if(network != null){
                OrganisationRefStructure orgRef = network.getTransportOrganisationRef().getValue();
                GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();
                List<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
                for(GroupOfLines group: groupOfLines){
                    dao.getAuthoritiesByGroupOfLinesId().put(group.getId(), orgRef.getRef());
                }
            }


            //lines
            LinesInFrame_RelStructure lines = sf.getLines();
            if(lines != null){
                List<JAXBElement<? extends DataManagedObjectStructure>> line_ = lines.getLine_();
                for (JAXBElement element : line_) {
                    if (element.getValue() instanceof Line) {
                        Line line = (Line) element.getValue();
                        dao.getLineById().put(line.getId(), line);
                        dao.getOtpRouteById().put(line.getId(), mapRoute(line));
                    }

                }
            }

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if(journeyPatterns != null){
                List<JAXBElement<?>> journeyPattern_orJourneyPatternView = journeyPatterns.getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPattern_orJourneyPatternView) {
                    if (pattern.getValue() instanceof JourneyPattern) {
                        JourneyPattern journeyPattern = (JourneyPattern) pattern.getValue();
                        dao.getJourneyPatternsById().put(journeyPattern.getId(), journeyPattern);
                    }
                }
            }
        }
    }

    private void loadTimeTableFrames(JAXBElement commonFrame) {
        if(commonFrame.getValue() instanceof TimetableFrame){
            TimetableFrame timetableFrame = (TimetableFrame) commonFrame.getValue();

            JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
            List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys.getDatedServiceJourneyOrDeadRunOrServiceJourney();
            for(Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney){
               if(jStructure instanceof ServiceJourney){
                   ServiceJourney sj = (ServiceJourney) jStructure;
                   String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

                   if(dao.getServiceJourneyById().get(journeyPatternId) != null){
                       dao.getServiceJourneyById().get(journeyPatternId).add(sj);
                   }else{
                       dao.getServiceJourneyById().put(journeyPatternId, Lists.newArrayList(sj));
                   }
               }
            }
        }
    }

    private void loadServiceCalendarFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame){
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();
            DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
            for(JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()){
                if(dt.getValue() instanceof DayType){
                    DayType dayType = (DayType) dt.getValue();
                    dao.getDayTypeById().put(dayType.getId(), dayType);
                }
            }

            List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments().getDayTypeAssignment();
            for(DayTypeAssignment dayTypeAssignment : dayTypeAssignments){
                String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
                if(dayTypeAssignment.getOperatingDayRef() != null){
                    dao.getDayTypeAssignment().put(ref, dayTypeAssignment.getOperatingDayRef().getRef());
                }else{
                    dao.getDayTypeAssignment().put(ref, dayTypeAssignment.getDate());
                }
            }

            OperatingPeriods_RelStructure operatingPeriods = scf.getServiceCalendar().getOperatingPeriods();
            if(operatingPeriods != null){
                List<Object> periods = operatingPeriods.getOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod();
                for(Object obj : periods){
                    if(obj instanceof OperatingPeriod){
                        OperatingPeriod op = (OperatingPeriod) obj;
                        dao.getOperatingPeriodById().put(op.getId(), op);
                    }
                }
            }
        }
    }

    private void loadSiteFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof SiteFrame) {
            SiteFrame sf = (SiteFrame) commonFrame.getValue();
            StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
            List<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
            for (StopPlace stopPlace : stopPlaceList) {
                List<Stop> stops = mapStopPlace(stopPlace);
                for(Stop stop : stops){
                    dao.getStopsById().put(stopPlace.getId(), stop);
                }
            }
        }
    }

    private void loadResourceFrames(JAXBElement commonFrame) {
        if(commonFrame.getValue() instanceof ResourceFrame){
            ResourceFrame resourceFrame = (ResourceFrame) commonFrame.getValue();
            List<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame.getOrganisations().getOrganisation_();
            for(JAXBElement element : organisations){
                if(element.getValue() instanceof Authority){
                    Authority authority = (Authority) element.getValue();
                    dao.getAuthorities().put(authority.getId(), authority);
                    dao.getAllAgencies().add(mapAgency(authority));
                }
            }
        }
    }

    private TripPattern mapTripPattern(JourneyPattern journeyPattern){

        List<Trip> trips = new ArrayList<>();

        //find matching journey pattern
        List<ServiceJourney> serviceJourneys = dao.getServiceJourneyById().get(journeyPattern.getId());

        StopPattern stopPattern = null;

        Route route = dao.getRouteById().get(journeyPattern.getRouteRef().getRef());
        String lineRef = route.getLineRef().getValue().getRef();

        for(ServiceJourney serviceJourney : serviceJourneys){
            Trip trip = mapServiceJourney(serviceJourney);
            trips.add(trip);

            TimetabledPassingTimes_RelStructure passingTimes = serviceJourney.getPassingTimes();
            List<TimetabledPassingTime> timetabledPassingTime = passingTimes.getTimetabledPassingTime();
            List<StopTime> stopTimes = new ArrayList<>();

            for(TimetabledPassingTime passingTime : timetabledPassingTime){
                JAXBElement<? extends PointInJourneyPatternRefStructure> pointInJourneyPatternRef = passingTime.getPointInJourneyPatternRef();
                String ref = pointInJourneyPatternRef.getValue().getRef();
                Stop stop = findStopPlace(ref, journeyPattern);
                if(stop != null){
                    stop.getId().setAgencyId(getAgencyIdForLine(lineRef));
                }
                StopTime stopTime = new StopTime();
                stopTime.setTrip(trip);

                if(passingTime.getArrivalTime() != null){
                    int secondOfDay = passingTime.getArrivalTime().toLocalTime().toSecondOfDay();
                    if(passingTime.getArrivalDayOffset() != null && passingTime.getArrivalDayOffset().intValue() == 1){
                        secondOfDay = secondOfDay + (3600 * 24);
                    }
                    stopTime.setArrivalTime(secondOfDay);
                }else if(passingTime.getDepartureTime() != null) {
                    int arrivalTime = passingTime.getDepartureTime().toLocalTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        arrivalTime = arrivalTime + (3600 * 24);
                    }
                    stopTime.setArrivalTime(arrivalTime);
                }

                if(passingTime.getDepartureTime() != null) {
                    int departureTime = passingTime.getDepartureTime().toLocalTime().toSecondOfDay();
                    if(passingTime.getDepartureDayOffset() != null && passingTime.getDepartureDayOffset().intValue() == 1){
                        departureTime = departureTime + (3600 * 24);
                    }
                    stopTime.setDepartureTime(departureTime);
                }
                StopPointInJourneyPattern stopPoint = findStopPoint(ref, journeyPattern);
                if(stopPoint != null){
                    stopTime.setDropOffType(stopPoint.isForAlighting() != null && !stopPoint.isForAlighting() ? 1 : 0);
                    stopTime.setPickupType(stopPoint.isForBoarding() != null && !stopPoint.isForBoarding() ? 1 : 0);
                }

                stopTime.setStop(stop);
                stopTimes.add(stopTime);
            }

            if(stopPattern == null){
                stopPattern = new StopPattern(stopTimes);
            }
            dao.getStopTimesForTrip().put(trip, stopTimes);
        }

        Line line = dao.getLineById().get(lineRef);

        TripPattern tripPattern = new TripPattern(dao.getOtpRouteById().get(line.getId()), stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();

        Deduplicator deduplicator = new Deduplicator();

        for(Trip trip : trips){
            TripTimes tripTimes = new TripTimes(trip, dao.getStopTimesForTrip().get(trip), deduplicator);
            tripPattern.add(tripTimes);
        }
        return tripPattern;

    }

    private Stop findStopPlace(String pointInJourneyPatterRef, JourneyPattern journeyPattern){
        List<PointInLinkSequence_VersionedChildStructure> points =
                journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stop = (StopPointInJourneyPattern) point;
                if(stop.getId().equals(pointInJourneyPatterRef)){
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) point).getScheduledStopPointRef();
                    String stopId = dao.getStopPointStopPlaceMap().get(scheduledStopPointRef.getValue().getRef());
                    return dao.getStopsById().get(stopId);
                }
            }
        }
        return null;
    }

    private StopPointInJourneyPattern findStopPoint(String pointInJourneyPatterRef, JourneyPattern journeyPattern){
        List<PointInLinkSequence_VersionedChildStructure> points =
                journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        for(PointInLinkSequence_VersionedChildStructure point : points){
            if(point instanceof StopPointInJourneyPattern){
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) point;
                if(stopPoint.getId().equals(pointInJourneyPatterRef)){
                    return stopPoint;
                }
            }
        }
        return null;
    }

    /**
     * Agency id must be added when the stop is related to a line
     */
    private List<Stop> mapStopPlace(StopPlace stopPlace){
        List<Stop> stops = new ArrayList<>();
        Stop stop = new Stop();
        stop.setName(stopPlace.getName().getValue());
        if(stopPlace.getCentroid() != null){
            stop.setLat(stopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            stop.setLon(stopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.error("Stop place without coordinates");
        }

        stop.setId(new AgencyAndId("", stopPlace.getId()));
        stops.add(stop);
        List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
        for(Object quayObject : quayRefOrQuay){
            if(quayObject instanceof Quay){
                Quay quay = (Quay) quayObject;
                Stop stopQuay = new Stop();
                stopQuay.setName(stop.getName());
                stopQuay.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());
                stopQuay.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
                stopQuay.setId(new AgencyAndId("", quay.getId()));
                stopQuay.setParentStation(stop.getId().getId());
                stops.add(stopQuay);
            }
        }
        return stops;
    }

    private Trip mapServiceJourney(ServiceJourney serviceJourney){

        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
            JourneyPattern journeyPattern = dao.getJourneyPatternsById().get(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = dao.getRouteById().get(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        String agencyIdForLine = getAgencyIdForLine(lineRef);
        trip.setId(new AgencyAndId(agencyIdForLine, serviceJourney.getId()));

        trip.setRoute(dao.getOtpRouteById().get(lineRef));
        DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();

        StringBuilder serviceId = new StringBuilder();
        boolean first = true;
        for(JAXBElement dt : dayTypes.getDayTypeRef()){
            if(!first){
                serviceId.append("+");
            }
            first = false;
            if(dt.getValue() instanceof DayTypeRefStructure){
                DayTypeRefStructure dayType = (DayTypeRefStructure) dt.getValue();
                serviceId.append(dayType.getRef());
            }
        }

        AgencyAndId key = new AgencyAndId(agencyIdForLine, serviceId.toString());
        trip.setServiceId(key);
        dao.getServiceIds().add(key);
        return trip;
    }

    private org.onebusaway2.gtfs.model.Route mapRoute(Line line){
        org.onebusaway2.gtfs.model.Route mapped = new org.onebusaway2.gtfs.model.Route();
        AuthorityRefStructure authorityRefStruct = line.getAuthorityRef();
        String authorityRef;
        if(authorityRefStruct == null){
            authorityRef = dao.getAuthoritiesByGroupOfLinesId().get(line.getRepresentedByGroupRef().getRef());
        }else{
            authorityRef = authorityRefStruct.getRef();
        }
        Authority authority = dao.getAuthorities().get(authorityRef);
        Agency agency = mapAgency(authority);
        mapped.setId(new AgencyAndId(agency.getName(), line.getId()));
        mapped.setAgency(agency);
        mapped.setLongName(line.getName().toString());
        mapped.setShortName(line.getPublicCode());
        mapped.setType(mapTransportType(line.getTransportMode().value()));
        return mapped;
    }

    private int mapTransportType(String type){
        if("bus".equals(type)){
            return 3;
        }
        return 0;
    }

    private Agency mapAgency(Authority authority){
        Agency agency = new Agency();
        agency.setId(authority.getId());
        agency.setName(authority.getName().getValue());
        agency.setTimezone(dao.getTimeZone());
        return agency;
    }

    private String getAgencyIdForLine(String lineRef){
        Line line = dao.getLineById().get(lineRef);
        return dao.getAuthoritiesByGroupOfLinesId().get(line.getRepresentedByGroupRef().getRef());
    }
}
