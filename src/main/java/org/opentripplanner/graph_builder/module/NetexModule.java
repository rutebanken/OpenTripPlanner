package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.onebusaway2.gtfs.impl.GtfsDaoImpl;
import org.onebusaway2.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.model.NetexBundle;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.graph_builder.model.NetexStopDao;
import org.opentripplanner.graph_builder.model.NetexStopPlaceBundle;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.netex.mapping.NetexMapper;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
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

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarSrvDataWithoutDatesForLocalizedSrvId;

public class NetexModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private List<NetexBundle> netexBundles;

    private FareServiceFactory _fareServiceFactory = new DefaultFareServiceFactory();

    private NetexStopPlaceBundle netexStopPlaceBundle;

    public NetexModule(List<NetexBundle> netexBundles, NetexStopPlaceBundle netexStopPlaceBundle) {
        this.netexBundles = netexBundles;
        this.netexStopPlaceBundle = netexStopPlaceBundle;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();

        try {
            NetexStopDao netexStopDao = loadBundle(netexStopPlaceBundle);

            for(NetexBundle netexBundle : netexBundles){
                NetexDao netexDao = loadBundle(netexBundle, netexStopDao);

                NetexMapper otpMapper = new NetexMapper();
                GtfsDao otpDao = otpMapper.mapNetexToOtp(netexDao);
                calendarService.addData(
                        createCalendarSrvDataWithoutDatesForLocalizedSrvId(otpDao),
                        otpDao
                );

                GTFSPatternHopFactory hf = new GTFSPatternHopFactory(new GtfsFeedId.Builder().id("RB").build(),
                        otpDao,
                        _fareServiceFactory,
                        netexBundle.getMaxStopToShapeSnapDistance(),
                        netexBundle.subwayAccessTime,
                        netexBundle.maxInterlineDistance);
                hf.setStopContext(stopContext);
                hf.run(graph);

                if (netexBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                }
                if (netexBundle.linkStopsToParentStations) {
                    hf.linkMultiModalStops(graph);
                }
                if (netexBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }
                if (netexBundle.parentStationTransfers) {
                    hf.createMultiModalStationTransfers();
                }
            }
        } catch (Exception e){
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

    private NetexDao loadBundle(NetexBundle netexBundle, NetexStopDao netexStopDao) throws Exception {
        NetexDao netexDao = new NetexDao();
        LOG.info("Loading common file...");
        Unmarshaller unmarshaller = getUnmarshaller();
        loadFile(netexBundle.getCommonFile(), unmarshaller, netexDao, netexStopDao);
        List<ZipEntry> entries = netexBundle.getFileEntries();
        for(ZipEntry entry : entries){
            LOG.info("Loading line file " + entry.getName());
            InputStream fileInputStream = netexBundle.getFileInputStream(entry);
            loadFile(fileInputStream, unmarshaller, netexDao, netexStopDao);
        }
        return netexDao;
    }

    private NetexStopDao loadBundle(NetexStopPlaceBundle netexBundle) throws Exception {
        NetexStopDao netexStopDao = new NetexStopDao();
        LOG.info("Loading stop place file...");
        Unmarshaller unmarshaller = getUnmarshaller();
        loadFile(netexBundle.getStopPlaceFile(), unmarshaller, netexStopDao);
        return netexStopDao;
    }

    public org.onebusaway2.gtfs.services.GtfsDao getOtpDao() throws Exception {
        org.onebusaway2.gtfs.services.GtfsDao otpDao = new GtfsDaoImpl();

        NetexStopDao netexStopDao = loadBundle(netexStopPlaceBundle);

        for(NetexBundle bundle : netexBundles) {
            NetexDao netexDao = loadBundle(bundle, netexStopDao);

            NetexMapper otpMapper = new NetexMapper();
            otpDao = otpMapper.mapNetexToOtp(netexDao);
        }

        return otpDao;
    }

    private Unmarshaller getUnmarshaller() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller;
    }

    private void loadFile(InputStream is, Unmarshaller unmarshaller, NetexStopDao netexStopDao) throws Exception {
        byte[] bytesArray = IOUtils.toByteArray(is);

        @SuppressWarnings("unchecked")
        JAXBElement<PublicationDeliveryStructure> jaxbElement = (JAXBElement<PublicationDeliveryStructure>) unmarshaller
                .unmarshal(new ByteArrayInputStream(bytesArray));

        PublicationDeliveryStructure value = jaxbElement.getValue();
        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrames = value.getDataObjects().getCompositeFrameOrCommonFrame();
        for(JAXBElement frame : compositeFrameOrCommonFrames){
            if (frame.getValue() instanceof SiteFrame) {
                loadSiteFrames(frame, netexStopDao);
            }
        }
    }

    private void loadFile(InputStream is, Unmarshaller unmarshaller, NetexDao netexDao, NetexStopDao netexStopDao) throws Exception {
        byte[] bytesArray = IOUtils.toByteArray(is);

        @SuppressWarnings("unchecked")
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

                netexDao.setTimeZone(timeZone);
                List<JAXBElement<? extends Common_VersionFrameStructure>> commonFrames = cf.getFrames().getCommonFrame();
                for (JAXBElement commonFrame : commonFrames) {
                    loadResourceFrames(commonFrame, netexDao);
                    loadServiceCalendarFrames(commonFrame, netexDao);
                    loadTimeTableFrames(commonFrame, netexDao);
                    loadServiceFrames(commonFrame, netexDao, netexStopDao);
                }
            }
        }
    }

    // Stop places and quays
    private void loadSiteFrames(JAXBElement commonFrame, NetexStopDao netexStopDao) {
        if (commonFrame.getValue() instanceof SiteFrame) {
            SiteFrame sf = (SiteFrame) commonFrame.getValue();
            StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
            List<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
            for (StopPlace stopPlace : stopPlaceList) {
                if (stopPlace.getKeyList().getKeyValue().stream().anyMatch(keyValueStructure ->
                        keyValueStructure.getKey().equals("IS_PARENT_STOP_PLACE") && keyValueStructure.getValue().equals("true"))) {
                    netexStopDao.multimodalStopPlaceById.put(stopPlace.getId(), stopPlace);
                } else {
                    netexStopDao.getStopsById().put(stopPlace.getId(), stopPlace);
                    if (stopPlace.getQuays() == null) {
                        LOG.warn(stopPlace.getId() + " does not contain any quays");
                    } else {
                        List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                        for (Object quayObject : quayRefOrQuay) {
                            if (quayObject instanceof Quay) {
                                Quay quay = (Quay) quayObject;
                                netexStopDao.getQuayById().put(quay.getId(), quay);
                                netexStopDao.getStopPlaceByQuay().put(quay, stopPlace);
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame, NetexDao netexDao, NetexStopDao netexStopDao) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //stop assignments
            StopAssignmentsInFrame_RelStructure stopAssignments = sf.getStopAssignments();
            if(stopAssignments != null){
                List<JAXBElement<? extends StopAssignment_VersionStructure>> assignments = stopAssignments.getStopAssignment();
                for (JAXBElement assignment : assignments) {
                    if(assignment.getValue() instanceof PassengerStopAssignment) {
                        PassengerStopAssignment passengerStopAssignment = (PassengerStopAssignment) assignment.getValue();
                        if (passengerStopAssignment.getQuayRef() != null) {
                            if (netexStopDao.getQuayById().containsKey(passengerStopAssignment.getQuayRef().getRef())) {
                                Quay quay = netexStopDao.getQuayById().get(passengerStopAssignment.getQuayRef().getRef());
                                StopPlace stopPlace = netexStopDao.getStopPlaceByQuay().get(quay);
                                netexDao.getStopPointStopPlaceMap().put(passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(), stopPlace.getId());
                                netexDao.getStopPointQuayMap().put(passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(), quay.getId());

                                // Load stopPlace and quay from netexStopDao into netexDao
                                if (!netexDao.getStopPlaceMap().containsKey(stopPlace.getId())) {
                                    netexDao.getStopPlaceMap().put(stopPlace.getId(), stopPlace);
                                }
                                if (!netexDao.getQuayMap().containsKey(quay.getId())) {
                                    netexDao.getQuayMap().put(quay.getId(), quay);
                                }
                            } else {
                                LOG.warn("Quay " + passengerStopAssignment.getQuayRef().getRef() + " not found in stop place file.");
                            }
                        }
                    }
                }
            }

            // Load parent stops from NetexStopDao into NetexDao

            for (StopPlace stopPlace : netexStopDao.getAllStopPlaces()) {
                if (!netexDao.getParentStopPlaceById().containsKey(stopPlace.getId())) {
                    netexDao.getParentStopPlaceById().put(stopPlace.getId(), stopPlace);
                }
            }

            // Load multimodal stops from NetexStopDao into NetexDao

            for (StopPlace stopPlace : netexStopDao.multimodalStopPlaceById.values()) {
                if (!netexDao.getMultimodalStopPlaceById().containsKey(stopPlace.getId())) {
                    netexDao.getMultimodalStopPlaceById().put(stopPlace.getId(), stopPlace);
                }
            }

            //routes
            RoutesInFrame_RelStructure routes = sf.getRoutes();
            if(routes != null){
                List<JAXBElement<? extends LinkSequence_VersionStructure>> route_ = routes.getRoute_();
                for (JAXBElement element : route_) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        netexDao.getRouteById().put(route.getId(), route);
                    }
                }
            }

            //network
            Network network = sf.getNetwork();
            if(network != null){
                OrganisationRefStructure orgRef = network.getTransportOrganisationRef().getValue();
                netexDao.getAuthoritiesByNetworkId().put(network.getId(), orgRef.getRef());
                if (network.getGroupsOfLines() != null) {
                    GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();
                    List<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
                    for (GroupOfLines group : groupOfLines) {
                        netexDao.getAuthoritiesByGroupOfLinesId().put(group.getId(), orgRef.getRef());
                    }
                }
            }


            //lines
            LinesInFrame_RelStructure lines = sf.getLines();
            if(lines != null){
                List<JAXBElement<? extends DataManagedObjectStructure>> line_ = lines.getLine_();
                for (JAXBElement element : line_) {
                    if (element.getValue() instanceof Line) {
                        Line line = (Line) element.getValue();
                        netexDao.getLineById().put(line.getId(), line);
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
                        netexDao.getJourneyPatternsById().put(journeyPattern.getId(), journeyPattern);
                    }
                }
            }

            if (sf.getNotices() != null) {
                for (Notice notice : sf.getNotices().getNotice()) {
                    netexDao.getNoticeMap().put(notice.getId(), notice);
                }
            }
        }
    }

    // ServiceJourneys
    private void loadTimeTableFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if(commonFrame.getValue() instanceof TimetableFrame){
            TimetableFrame timetableFrame = (TimetableFrame) commonFrame.getValue();

            JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
            List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys.getDatedServiceJourneyOrDeadRunOrServiceJourney();
            for(Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney){
                if(jStructure instanceof ServiceJourney){
                    loadServiceIds((ServiceJourney)jStructure, netexDao);
                    ServiceJourney sj = (ServiceJourney) jStructure;
                    String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();
                    if (netexDao.getJourneyPatternsById().get(journeyPatternId).getPointsInSequence().
                            getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size()
                            == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                        if (netexDao.getServiceJourneyById().get(journeyPatternId) != null) {
                            netexDao.getServiceJourneyById().get(journeyPatternId).add(sj);
                        } else {
                            netexDao.getServiceJourneyById().put(journeyPatternId, Lists.newArrayList(sj));
                        }
                    } else {
                        LOG.warn("Mismatch between ServiceJourney and JourneyPattern. ServiceJourney will be skipped. - " + sj.getId());
                    }
                }
            }



            if (timetableFrame.getNoticeAssignments() != null) {
                for (JAXBElement<? extends DataManagedObjectStructure> noticeAssignmentElement : timetableFrame.getNoticeAssignments()
                        .getNoticeAssignment_()) {
                    NoticeAssignment noticeAssignment = (NoticeAssignment) noticeAssignmentElement.getValue();

                    if (noticeAssignment.getNoticeRef() != null && noticeAssignment.getNoticedObjectRef() != null) {
                        netexDao.getNoticeAssignmentMap().put(noticeAssignment.getId(), noticeAssignment);
                    }
                }
            }

            if (timetableFrame.getJourneyInterchanges() != null) {
                for (Interchange_VersionStructure interchange_versionStructure : timetableFrame.getJourneyInterchanges()
                        .getServiceJourneyPatternInterchangeOrServiceJourneyInterchange()) {
                    if (interchange_versionStructure instanceof  ServiceJourneyInterchange) {
                        ServiceJourneyInterchange interchange = (ServiceJourneyInterchange) interchange_versionStructure;
                        netexDao.getInterchanges().put(interchange.getId(), interchange);
                    }
                }
            }
        }
    }

    private void loadServiceIds (ServiceJourney serviceJourney, NetexDao netexDao) {
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

        // Add all unique service ids to map. Used when mapping calendars later.
        if (!netexDao.getServiceIds().containsKey(serviceId.toString())) {
            netexDao.getServiceIds().put(serviceId.toString(), serviceId.toString());
        }
    }

    // ServiceCalendar
    private void loadServiceCalendarFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame){
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();

            if (scf.getServiceCalendar() != null) {
                DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
                for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        netexDao.getDayTypeById().put(dayType.getId(), dayType);
                    }
                }
            }

            if (scf.getDayTypes() != null) {
                List<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes().getDayType_();
                for (JAXBElement dt : dayTypes) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        netexDao.getDayTypeById().put(dayType.getId(), dayType);
                    }
                }
            }

            if (scf.getOperatingPeriods() != null) {
                for (OperatingPeriod_VersionStructure operatingPeriodStruct : scf.getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()) {
                    OperatingPeriod operatingPeriod = (OperatingPeriod) operatingPeriodStruct;
                    netexDao.getOperatingPeriodById().put(operatingPeriod.getId(), operatingPeriod);
                }
            }

            List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments().getDayTypeAssignment();
            for(DayTypeAssignment dayTypeAssignment : dayTypeAssignments){
                String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
                netexDao.getDayTypeAvailable().put(dayTypeAssignment.getId(), dayTypeAssignment.isIsAvailable() == null ? true : dayTypeAssignment.isIsAvailable());

                if (netexDao.getDayTypeAssignment().containsKey(ref)) {
                    netexDao.getDayTypeAssignment().get(ref).add(dayTypeAssignment);
                } else {
                    netexDao.getDayTypeAssignment().put(ref, new ArrayList<DayTypeAssignment>() {
                        {
                            add(dayTypeAssignment);
                        }
                    });
                }
            }
        }
    }

    // Authorities and operators
    private void loadResourceFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if(commonFrame.getValue() instanceof ResourceFrame){
            ResourceFrame resourceFrame = (ResourceFrame) commonFrame.getValue();
            List<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame.getOrganisations().getOrganisation_();
            for(JAXBElement element : organisations){
                if(element.getValue() instanceof Authority){
                    Authority authority = (Authority) element.getValue();
                    netexDao.getAuthorities().put(authority.getId(), authority);
                }
                if(element.getValue() instanceof Operator){
                    Operator operator = (Operator) element.getValue();
                    netexDao.getOperators().put(operator.getId(), operator);
                }
            }
        }
    }
}