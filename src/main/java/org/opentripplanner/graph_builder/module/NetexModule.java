package org.opentripplanner.graph_builder.module;

import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.triptransformer.TripTransformService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexLoader;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;

import java.util.HashMap;
import java.util.List;

public class NetexModule implements GraphBuilderModule {

    private List<NetexBundle> netexBundles;

    private FareServiceFactory _fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(List<NetexBundle> netexBundles) {
        this.netexBundles = netexBundles;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();

        try {
            for (NetexBundle netexBundle : netexBundles) {
                OtpTransitBuilder daoBuilder = new NetexLoader(netexBundle,graph).loadBundle();

                TripTransformService.runTripTransform(daoBuilder, netexBundle.fileDir());

                calendarService.addData(daoBuilder);

                if (netexBundle.removeStopsNotInUse) {
                    daoBuilder.removeStopsNotInUse();
                }

                TripTransformService.printTimeTable(daoBuilder, netexBundle.fileDir());


                PatternHopFactory hf = new PatternHopFactory(
                        new GtfsFeedId.Builder().id(netexBundle.netexParameters.netexFeedId).build(),
                        daoBuilder.build(),
                        _fareServiceFactory,
                        netexBundle.getMaxStopToShapeSnapDistance(),
                        netexBundle.subwayAccessTime,
                        netexBundle.maxInterlineDistance);
                hf.setStopContext(stopContext);
                hf.run(graph);

                if (netexBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                }
                if (netexBundle.linkMultiModalStopsToParentStations) {
                    hf.linkMultiModalStops(graph);
                }
                if (netexBundle.linkMultiModalStopsToParentStations) {
                    hf.linkGroupsOfStopPlaces(graph);
                }
                if (netexBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }

                if (netexBundle.parkAndRideFromTransitData) {
                    hf.createParkAndRide(graph);
                }
            }
        } catch (Exception e) {
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
}