package org.opentripplanner.routing.street;

import static org.opentripplanner.PolylineAssert.assertThatPolylinesAreEqual;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.TestUtils;

/*
 * When bus stops are added to graph they split an existing edge in two parts so that an artifical
 * intersection can be added and routes can be found to the station.
 *
 * During this process turn restrictions also need to be propagated from the old edges to the newly
 * created ones. This test makes sure that this happens correctly.
 */
public class SplitEdgeTurnRestrictionsTest {

    // Deufringen
    static GenericLocation hardtheimerWeg = new GenericLocation(48.67765, 8.87212);
    static GenericLocation steinhaldenWeg = new GenericLocation(48.67815, 8.87305);
    static GenericLocation k1022 = new GenericLocation(48.67846, 8.87021);

    // Böblingen
    static GenericLocation paulGerhardtWegEast = new GenericLocation(48.68363, 9.00728);
    static GenericLocation paulGerhardtWegWest = new GenericLocation(48.68297, 9.00520);
    static GenericLocation parkStrasse = new GenericLocation(48.68358, 9.00826);
    static GenericLocation herrenbergerStrasse = new GenericLocation(48.68497, 9.00909);
    static GenericLocation steinbeissWeg = new GenericLocation(48.68172, 9.00599);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 3, 3, 7, 0, 0);

    @Test
    public void shouldTakeDeufringenTurnRestrictionsIntoAccount() throws IOException {
        Graph graph = ConstantsForTests.buildGtfsGraph(
                ConstantsForTests.DEUFRINGEN_OSM,
                ConstantsForTests.VVS_BUS_764_ONLY
        );
        // https://www.openstreetmap.org/relation/10264251 has a turn restriction so when leaving Hardtheimer Weg
        // you must either turn right and take the long way to Steinhaldenweg or go past the intersection with the
        // turn restriction and turn around.
        // on top of this, it has a bus stop so this test also makes sure that the turn restrictions work
        // even when the streets are split.
        var noRightTurnPermitted = computeCarPolyline(graph, hardtheimerWeg, steinhaldenWeg);
        assertThatPolylinesAreEqual(
                noRightTurnPermitted,
                "ijbhHwycu@g@Sq@[g@zBCPGV[vA[xA[z@Wp@Yl@\\n@LBN]?Ya@i@Vq@Z{@ZyAZwACICGD_@j@_DN_A@g@KFE@"
        );

        // when to drive in reverse direction it's fine to go this way
        var leftTurnOk = computeCarPolyline(graph, steinhaldenWeg, hardtheimerWeg);
        assertThatPolylinesAreEqual(leftTurnOk, "mmbhHq_du@DAJGAf@O~@k@~CE^BFBHFWBQf@{Bp@Zf@R");

        // make sure that going straight on a straight-only turn direction also works
        var straightAhead = computeCarPolyline(graph, hardtheimerWeg, k1022);
        assertThatPolylinesAreEqual(straightAhead, "ijbhHwycu@g@Sq@[g@zBCPGV[vA[xAXp@Hb@");

        var straightAheadBack = computeCarPolyline(graph, k1022, hardtheimerWeg);
        assertThatPolylinesAreEqual(straightAheadBack, "kobhHymcu@Ic@Yq@ZyAZwAFWBQf@{Bp@Zf@R");

        // make sure that turning left onto the minor road works even when the opposite direction has a straight-only
        // restriction
        var leftTurnAllowed = computeCarPolyline(graph, k1022, steinhaldenWeg);
        assertThatPolylinesAreEqual(leftTurnAllowed, "kobhHymcu@Ic@Yq@ZyAZwACICGD_@j@_DN_A@g@KFE@");

        var rightTurnAllowed = computeCarPolyline(graph, steinhaldenWeg, k1022);
        assertThatPolylinesAreEqual(
                rightTurnAllowed, "mmbhHq_du@DAJGAf@O~@k@~CE^BFBH[vA[xAXp@Hb@");
    }

    @Test
    public void shouldTakeBoeblingenTurnRestrictionsIntoAccount() throws IOException {
        // this tests that the following turn restriction is transferred correctly to the split edges
        // https://www.openstreetmap.org/relation/299171
        var graph = ConstantsForTests.buildGtfsGraph(
                ConstantsForTests.BOEBLINGEN_OSM,
                ConstantsForTests.VVS_BUS_751_ONLY
        );

        // turning left from the main road onto a residential one
        var turnLeft = computeCarPolyline(graph, parkStrasse, paulGerhardtWegEast);
        assertThatPolylinesAreEqual(
                turnLeft, "kochHul~u@FQN]N_@v@kBDKN]KKMZ{@~BKXYl@KRIPCDYl@DP\\lAJX");

        // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
        // is a bus station along the way, splitting the edge
        var noLeftTurnPermitted = computeCarPolyline(graph, paulGerhardtWegEast, parkStrasse);
        assertThatPolylinesAreEqual(noLeftTurnPermitted, "uochHof~u@KY]mAVk@Rc@FM");

        // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
        // is a bus station along the way, splitting the edge
        var longWay = computeCarPolyline(graph, paulGerhardtWegEast, herrenbergerStrasse);
        assertThatPolylinesAreEqual(
                longWay,
                "uochHof~u@KY]mAVk@Rc@N_@N]N_@v@kBDKN]KKMZ{@~BKXYl@KRIPCDc@`@]ZWPSHQBMAGAOMKMSeAc@sCKe@"
        );

        var longWayBack = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegEast);
        assertThatPolylinesAreEqual(
                longWayBack,
                "axchHyq~u@I]Oc@?WCK?K?SBWRUh@eA@Cb@eANi@Ps@Jq@He@Da@@o@Bs@BUDYJe@Li@DYFDJHt@bADFT\\NN?@d@h@~@lA`@r@Zr@z@vBLZ[R[X[b@e@x@[p@ELMZ{@~BKXYl@KRIPCDYl@DP\\lAJX"
        );

        // test that you can correctly turn right here https://www.openstreetmap.org/relation/415123 when approaching
        // from south
        var fromSouth = computeCarPolyline(graph, steinbeissWeg, paulGerhardtWegWest);
        assertThatPolylinesAreEqual(fromSouth, "ycchHm~}u@Hf@Fj@m@Z}@d@ID{BnAUmA");
        var toSouth = computeCarPolyline(graph, paulGerhardtWegWest, steinbeissWeg);
        assertThatPolylinesAreEqual(toSouth, "okchHoy}u@TlAzBoAHE|@e@l@[Gk@Ig@");

        // test that you cannot turn left here https://www.openstreetmap.org/relation/415123 when approaching
        // from north
        var fromNorth = computeCarPolyline(graph, paulGerhardtWegWest, herrenbergerStrasse);
        assertThatPolylinesAreEqual(
                fromNorth,
                "okchHoy}u@TlA{BjAGDOJEBGFc@}A[gAKa@Ma@K_@Ia@I_@Gc@Ga@Ee@Gi@CYAIKaAEe@COCSIo@SeAc@sCKe@"
        );

        // when you approach you cannot turn left so you have to take a long way
        var toNorth = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegWest);
        assertThatPolylinesAreEqual(
                toNorth,
                "axchHyq~u@I]Oc@?WCK?K?SBWRUh@eA@Cb@eANi@Ps@Jq@He@Da@@o@Bs@BUDYJe@Li@DYFDJHt@bADFT\\NN?@d@h@~@lA`@r@Zr@z@vBLZ[R[X[b@e@x@[p@ELJJBBBFBBBFvAhCZt@Rr@`@jC@Hd@`D@HRpALt@Fj@m@Z}@d@ID{BnAUmA"
        );
    }

    private static String computeCarPolyline(
            Graph graph,
            GenericLocation from,
            GenericLocation to
    ) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.streetSubRequestModes = new TraverseModeSet(TraverseMode.CAR);

        request.setRoutingContext(graph);

        var gpf = new GraphPathFinder(new Router(graph, RouterConfig.DEFAULT));
        var paths = gpf.graphPathFinderEntryPoint(request);

        var itineraries = GraphPathToItineraryMapper.mapItineraries(paths, request);
        // make sure that we only get CAR legs
        itineraries.forEach(
                i -> i.legs.forEach(l -> Assertions.assertEquals(l.mode, TraverseMode.CAR)));
        return itineraries.get(0).legs.get(0).legGeometry.getPoints();
    }
}
