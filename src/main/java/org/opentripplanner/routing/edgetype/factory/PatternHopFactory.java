package org.opentripplanner.routing.edgetype.factory;


import com.beust.jcommander.internal.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.util.FastMath;
import org.onebusaway2.gtfs.model.*;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.*;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class PatternHopFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PatternHopFactory.class);

    private static final int SECONDS_IN_HOUR = 60 * 60; // rename to seconds in hour

    private static GeometryFactory _geometryFactory = GeometryUtils.getGeometryFactory();
    public int maxInterlineDistance = 200;

    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<>();

    private double maxStopToShapeSnapDistance = 150;

    private static final int subwayAccessTime = 0;

    private static final int STOP_LOCATION_TYPE = 0;
    private static final int PARENT_STATION_LOCATION_TYPE = 1;


    abstract protected Collection<Agency> getAllAgencies();
    abstract protected Collection<Stop> getAllStops();
    abstract protected List<ShapePoint> getShapePointsForShapeId(AgencyAndId agencyAndId);
    abstract protected List<Stop> getStopsForStation(Stop stop);

    /**
     * Creates a set of geometries for a single trip, considering the GTFS shapes.txt,
     * The geometry is broken down into one geometry per inter-stop segment ("hop"). We also need a shape for the entire
     * trip and tripPattern, but given the complexity of the existing code for generating hop geometries, we will create
     * the full-trip geometry by simply concatenating the hop geometries.
     *
     * This geometry will in fact be used for an entire set of trips in a trip pattern. Technically one of the trips
     * with exactly the same sequence of stops could follow a different route on the streets, but that's very uncommon.
     */
    LineString[] createGeometry(Graph graph, Trip trip, List<StopTime> stopTimes) {
        AgencyAndId shapeId = trip.getShapeId();

        // One less geometry than stoptime as array indexes represetn hops not stops (fencepost problem).
        LineString[] geoms = new LineString[stopTimes.size() - 1];

        // Detect presence or absence of shape_dist_traveled on a per-trip basis
        StopTime st0 = stopTimes.get(0);
        boolean hasShapeDist = st0.isShapeDistTraveledSet();
        if (hasShapeDist) {
            // this trip has shape_dist in stop_times
            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                geoms[i] = getHopGeometryViaShapeDistTraveled(graph, shapeId, st0, st1);
            }
            return geoms;
        }
        LineString shape = getLineStringForShapeId(shapeId);
        if (shape == null) {
            // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
            // create straight line segments between stops for each hop
            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
                geoms[i] = geometry;
            }
            return geoms;
        }
        // This trip does not have shape_dist in stop_times, but does have an associated shape.
        ArrayList<IndexedLineSegment> segments = new ArrayList<IndexedLineSegment>();
        for (int i = 0 ; i < shape.getNumPoints() - 1; ++i) {
            segments.add(new IndexedLineSegment(i, shape.getCoordinateN(i), shape.getCoordinateN(i + 1)));
        }
        // Find possible segment matches for each stop.
        List<List<IndexedLineSegment>> possibleSegmentsForStop = new ArrayList<List<IndexedLineSegment>>();
        int minSegmentIndex = 0;
        for (int i = 0; i < stopTimes.size() ; ++i) {
            Stop stop = stopTimes.get(i).getStop();
            Coordinate coord = new Coordinate(stop.getLon(), stop.getLat());
            List<IndexedLineSegment> stopSegments = new ArrayList<>();
            double bestDistance = Double.MAX_VALUE;
            IndexedLineSegment bestSegment = null;
            int maxSegmentIndex = -1;
            int index = -1;
            int minSegmentIndexForThisStop = -1;
            for (IndexedLineSegment segment : segments) {
                index ++;
                if (segment.index < minSegmentIndex) {
                    continue;
                }
                double distance = segment.distance(coord);
                if (distance < maxStopToShapeSnapDistance) {
                    stopSegments.add(segment);
                    maxSegmentIndex = index;
                    if (minSegmentIndexForThisStop == -1)
                        minSegmentIndexForThisStop = index;
                } else if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSegment = segment;
                    if (maxSegmentIndex != -1) {
                        maxSegmentIndex = index;
                    }
                }
            }
            if (stopSegments.size() == 0) {
                //no segments within 150m
                //fall back to nearest segment
                stopSegments.add(bestSegment);
                minSegmentIndex = bestSegment.index;
            } else {
                minSegmentIndex = minSegmentIndexForThisStop;
                Collections.sort(stopSegments, new IndexedLineSegmentComparator(coord));
            }

            for (int j = i - 1; j >= 0; j --) {
                for (Iterator<IndexedLineSegment> it = possibleSegmentsForStop.get(j).iterator(); it.hasNext(); ) {
                    IndexedLineSegment segment = it.next();
                    if (segment.index > maxSegmentIndex) {
                        it.remove();
                    }
                }
            }
            possibleSegmentsForStop.add(stopSegments);
        }

        List<LinearLocation> locations = getStopLocations(possibleSegmentsForStop, stopTimes, 0, -1);

        if (locations == null) {
            // this only happens on shape which have points very far from
            // their stop sequence. So we'll fall back to trivial stop-to-stop
            // linking, even though theoretically we could do better.

            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
                geoms[i] = geometry;
                //this warning is not strictly correct, but will do
                LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
            }
            return geoms;
        }

        Iterator<LinearLocation> locationIt = locations.iterator();
        LinearLocation endLocation = locationIt.next();
        double distanceSoFar = 0;
        int last = 0;
        for (int i = 0; i < stopTimes.size() - 1; ++i) {
            LinearLocation startLocation = endLocation;
            endLocation = locationIt.next();

            //convert from LinearLocation to distance
            //advance distanceSoFar up to start of segment containing startLocation;
            //it does not matter at all if this is accurate so long as it is consistent
            for (int j = last; j < startLocation.getSegmentIndex(); ++j) {
                Coordinate from = shape.getCoordinateN(j);
                Coordinate to = shape.getCoordinateN(j + 1);
                double xd = from.x - to.x;
                double yd = from.y - to.y;
                distanceSoFar += FastMath.sqrt(xd * xd + yd * yd);
            }
            last = startLocation.getSegmentIndex();

            double startIndex = distanceSoFar + startLocation.getSegmentFraction() * startLocation.getSegmentLength(shape);
            //advance distanceSoFar up to start of segment containing endLocation
            for (int j = last; j < endLocation.getSegmentIndex(); ++j) {
                Coordinate from = shape.getCoordinateN(j);
                Coordinate to = shape.getCoordinateN(j + 1);
                double xd = from.x - to.x;
                double yd = from.y - to.y;
                distanceSoFar += FastMath.sqrt(xd * xd + yd * yd);
            }
            last = startLocation.getSegmentIndex();
            double endIndex = distanceSoFar + endLocation.getSegmentFraction() * endLocation.getSegmentLength(shape);

            ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startIndex, endIndex);
            LineString geometry = _geometriesByShapeSegmentKey.get(key);

            if (geometry == null) {
                LocationIndexedLine locationIndexed = new LocationIndexedLine(shape);
                geometry = (LineString) locationIndexed.extractLine(startLocation, endLocation);

                // Pack the resulting line string
                CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                        .getCoordinates(), 2);
                geometry = _geometryFactory.createLineString(sequence);
            }
            geoms[i] = geometry;
        }

        return geoms;
    }



    /**
     * Find a consistent, increasing list of LinearLocations along a shape for a set of stops.
     * Handles loops routes.
     * @return
     */
    private List<LinearLocation> getStopLocations(List<List<IndexedLineSegment>> possibleSegmentsForStop,
                                                  List<StopTime> stopTimes, int index, int prevSegmentIndex) {

        if (index == stopTimes.size()) {
            return new LinkedList<>();
        }

        StopTime st = stopTimes.get(index);
        Stop stop = st.getStop();
        Coordinate stopCoord = new Coordinate(stop.getLon(), stop.getLat());

        for (IndexedLineSegment segment : possibleSegmentsForStop.get(index)) {
            if (segment.index < prevSegmentIndex) {
                //can't go backwards along line
                continue;
            }
            List<LinearLocation> locations = getStopLocations(possibleSegmentsForStop, stopTimes, index + 1, segment.index);
            if (locations != null) {
                LinearLocation location = new LinearLocation(0, segment.index, segment.fraction(stopCoord));
                locations.add(0, location);
                return locations; //we found one!
            }
        }

        return null;
    }

    /**
     * Scan through the given list, looking for clearly incorrect series of stoptimes and unsetting
     * them. This includes duplicate times (0-time hops), as well as negative, fast or slow hops.
     * Unsetting the arrival/departure time of clearly incorrect stoptimes will cause them to be
     * interpolated in the next step. Annotations are also added to the graph to reveal the problems
     * to the user.
     *
     * @param stopTimes the stoptimes to be filtered (from a single trip)
     * @param graph the graph where annotations will be registered
     */
    void filterStopTimes(List<StopTime> stopTimes, Graph graph) {

        if (stopTimes.size() < 2) return;
        StopTime st0 = stopTimes.get(0);

        /* Set departure time if it is missing */
        if (!st0.isDepartureTimeSet() && st0.isArrivalTimeSet()) {
            st0.setDepartureTime(st0.getArrivalTime());
        }

        /* If the feed does not specify any timepoints, we want to mark all times that are present as timepoints. */
        boolean hasTimepoints = false;
        for (StopTime stopTime : stopTimes) {
            if (stopTime.getTimepoint() == 1) {
                hasTimepoints = true;
                break;
            }
        }
        // TODO verify that the first (and last?) stop should always be considered a timepoint.
        if (!hasTimepoints) st0.setTimepoint(1);

        /* Indicates that stop times in this trip are being shifted forward one day. */
        boolean midnightCrossed = false;

        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);

            /* If the feed did not specify any timepoints, mark all times that are present as timepoints. */
            if ( !hasTimepoints && (st1.isDepartureTimeSet() || st1.isArrivalTimeSet())) {
                st1.setTimepoint(1);
            }

            if (midnightCrossed) {
                if (st1.isDepartureTimeSet())
                    st1.setDepartureTime(st1.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                if (st1.isArrivalTimeSet())
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
            }
            /* Set departure time if it is missing. */
            // TODO: doc: what if arrival time is missing?
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* Do not process (skip over) non-timepoint stoptimes, leaving them in place for interpolation. */
            // All non-timepoint stoptimes in a series will have identical arrival and departure values of MISSING_VALUE.
            if ( ! (st1.isArrivalTimeSet() && st1.isDepartureTimeSet())) {
                continue;
            }
            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            if (dwellTime < 0) {
                LOG.warn(graph.addBuilderAnnotation(new NegativeDwellTime(st0)));
                if (st0.getArrivalTime() > 23 * SECONDS_IN_HOUR && st0.getDepartureTime() < 1 * SECONDS_IN_HOUR) {
                    midnightCrossed = true;
                    st0.setDepartureTime(st0.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                } else {
                    st0.setDepartureTime(st0.getArrivalTime());
                }
            }
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

            if (runningTime < 0) {
                LOG.warn(graph.addBuilderAnnotation(new NegativeHopTime(new StopTime(st0), new StopTime(st1))));
                // negative hops are usually caused by incorrect coding of midnight crossings
                midnightCrossed = true;
                if (st0.getDepartureTime() > 23 * SECONDS_IN_HOUR && st1.getArrivalTime() < 1 * SECONDS_IN_HOUR) {
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
                } else {
                    st1.setArrivalTime(st0.getDepartureTime());
                }
            }
            double hopDistance = SphericalDistanceLibrary.fastDistance(
                    st0.getStop().getLat(), st0.getStop().getLon(),
                    st1.getStop().getLat(), st1.getStop().getLon());
            double hopSpeed = hopDistance/runningTime;
            /* zero-distance hops are probably not harmful, though they could be better
             * represented as dwell times
            if (hopDistance == 0) {
                LOG.warn(GraphBuilderAnnotation.register(graph,
                        Variety.HOP_ZERO_DISTANCE, runningTime,
                        st1.getTrip().getId(),
                        st1.getStopSequence()));
            }
            */
            // sanity-check the hop
            if (st0.getArrivalTime() == st1.getArrivalTime() ||
                    st0.getDepartureTime() == st1.getDepartureTime()) {
                LOG.trace("{} {}", st0, st1);
                // series of identical stop times at different stops
                LOG.trace(graph.addBuilderAnnotation(new HopZeroTime((float) hopDistance,
                        st1.getTrip(), st1.getStopSequence())));
                // clear stoptimes that are obviously wrong, causing them to later be interpolated
/* FIXME (lines commented out because they break routability in multi-feed NYC for some reason -AMB) */
//                st1.clearArrivalTime();
//                st1.clearDepartureTime();
                st1bogus = true;
            } else if (hopSpeed > 45) {
                // 45 m/sec ~= 100 miles/hr
                // elapsed time of 0 will give speed of +inf
                LOG.trace(graph.addBuilderAnnotation(new HopSpeedFast((float) hopSpeed,
                        (float) hopDistance, st0.getTrip(), st0.getStopSequence())));
            } else if (hopSpeed < 0.1) {
                // 0.1 m/sec ~= 0.2 miles/hr
                LOG.trace(graph.addBuilderAnnotation(new HopSpeedSlow((float) hopSpeed,
                        (float) hopDistance, st0.getTrip(), st0.getStopSequence())));
            }
            // st0 should reflect the last stoptime that was not clearly incorrect
            if ( ! st1bogus)
                st0 = st1;
        } // END for loop over stop times
    }

    /**
     * Scan through the given list of stoptimes, interpolating the missing (unset) ones.
     * This is currently done by assuming equidistant stops and constant speed.
     * While we may not be able to improve the constant speed assumption, we can
     * TODO: use route matching (or shape distance etc.) to improve inter-stop distances
     *
     * @param stopTimes the stoptimes (from a single trip) to be interpolated
     */
    void interpolateStopTimes(List<StopTime> stopTimes) {
        int lastStop = stopTimes.size() - 1;
        int numInterpStops = -1;
        int departureTime = -1, prevDepartureTime = -1;
        int interpStep = 0;

        int i;
        for (i = 0; i < lastStop; i++) {
            StopTime st0 = stopTimes.get(i);

            prevDepartureTime = departureTime;
            departureTime = st0.getDepartureTime();

            /* Interpolate, if necessary, the times of non-timepoint stops */
            /* genuine interpolation needed */
            if (!(st0.isDepartureTimeSet() && st0.isArrivalTimeSet())) {
                // figure out how many such stops there are in a row.
                int j;
                StopTime st = null;
                for (j = i + 1; j < lastStop + 1; ++j) {
                    st = stopTimes.get(j);
                    if ((st.isDepartureTimeSet() && st.getDepartureTime() != departureTime)
                            || (st.isArrivalTimeSet() && st.getArrivalTime() != departureTime)) {
                        break;
                    }
                }
                if (j == lastStop + 1) {
                    throw new RuntimeException(
                            "Could not interpolate arrival/departure time on stop " + i
                                    + " (missing final stop time) on trip " + st0.getTrip());
                }
                numInterpStops = j - i;
                int arrivalTime;
                if (st.isArrivalTimeSet()) {
                    arrivalTime = st.getArrivalTime();
                } else {
                    arrivalTime = st.getDepartureTime();
                }
                interpStep = (arrivalTime - prevDepartureTime) / (numInterpStops + 1);
                if (interpStep < 0) {
                    throw new RuntimeException(
                            "trip goes backwards for some reason");
                }
                for (j = i; j < i + numInterpStops; ++j) {
                    //System.out.println("interpolating " + j + " between " + prevDepartureTime + " and " + arrivalTime);
                    departureTime = prevDepartureTime + interpStep * (j - i + 1);
                    st = stopTimes.get(j);
                    if (st.isArrivalTimeSet()) {
                        departureTime = st.getArrivalTime();
                    } else {
                        st.setArrivalTime(departureTime);
                    }
                    if (!st.isDepartureTimeSet()) {
                        st.setDepartureTime(departureTime);
                    }
                }
                i = j - 1;
            }
        }
    }

    void clearCachedData() {
        LOG.debug("shapes=" + _geometriesByShapeId.size());
        LOG.debug("segments=" + _geometriesByShapeSegmentKey.size());
        _geometriesByShapeId.clear();
        _distancesByShapeId.clear();
        _geometriesByShapeSegmentKey.clear();
    }

    private LineString getHopGeometryViaShapeDistTraveled(Graph graph, AgencyAndId shapeId, StopTime st0, StopTime st1) {

        double startDistance = st0.getShapeDistTraveled();
        double endDistance = st1.getShapeDistTraveled();

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
        LineString geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry != null)
            return geometry;

        double[] distances = getDistanceForShapeId(shapeId);

        if (distances == null) {
            LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometry(shapeId)));
            return null;
        } else {
            LinearLocation startIndex = getSegmentFraction(distances, startDistance);
            LinearLocation endIndex = getSegmentFraction(distances, endDistance);

            if (equals(startIndex, endIndex)) {
                //bogus shape_dist_traveled
                graph.addBuilderAnnotation(new BogusShapeDistanceTraveled(st1));
                return createSimpleGeometry(st0.getStop(), st1.getStop());
            }
            LineString line = getLineStringForShapeId(shapeId);
            LocationIndexedLine lol = new LocationIndexedLine(line);

            geometry = getSegmentGeometry(graph, shapeId, lol, startIndex, endIndex, startDistance,
                    endDistance, st0, st1);

            return geometry;
        }
    }

    private static boolean equals(LinearLocation startIndex, LinearLocation endIndex) {
        return startIndex.getSegmentIndex() == endIndex.getSegmentIndex()
                && startIndex.getSegmentFraction() == endIndex.getSegmentFraction()
                && startIndex.getComponentIndex() == endIndex.getComponentIndex();
    }

    /** create a 2-point linestring (a straight line segment) between the two stops */
    private LineString createSimpleGeometry(Stop s0, Stop s1) {

        Coordinate[] coordinates = new Coordinate[] {
                new Coordinate(s0.getLon(), s0.getLat()),
                new Coordinate(s1.getLon(), s1.getLat())
        };
        CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);

        return _geometryFactory.createLineString(sequence);
    }

    boolean isValid(Geometry geometry, Stop s0, Stop s1) {
        Coordinate[] coordinates = geometry.getCoordinates();
        if (coordinates.length < 2) {
            return false;
        }
        if (geometry.getLength() == 0) {
            return false;
        }
        for (Coordinate coordinate : coordinates) {
            if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
                return false;
            }
        }
        Coordinate geometryStartCoord = coordinates[0];
        Coordinate geometryEndCoord = coordinates[coordinates.length - 1];

        Coordinate startCoord = new Coordinate(s0.getLon(), s0.getLat());
        Coordinate endCoord = new Coordinate(s1.getLon(), s1.getLat());
        if (SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord) > maxStopToShapeSnapDistance) {
            return false;
        } else if (SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance) {
            return false;
        }
        return true;
    }

    private LineString getSegmentGeometry(Graph graph, AgencyAndId shapeId,
                                          LocationIndexedLine locationIndexedLine, LinearLocation startIndex,
                                          LinearLocation endIndex, double startDistance, double endDistance,
                                          StopTime st0, StopTime st1) {

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

        LineString geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry == null) {

            geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

            // Pack the resulting line string
            CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                    .getCoordinates(), 2);
            geometry = _geometryFactory.createLineString(sequence);

            if (!isValid(geometry, st0.getStop(), st1.getStop())) {
                LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
                //fall back to trivial geometry
                geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
            }
            _geometriesByShapeSegmentKey.put(key, (LineString) geometry);
        }

        return geometry;
    }

    /*
     * If a shape appears in more than one feed, the shape points will be loaded several
     * times, and there will be duplicates in the DAO. Filter out duplicates and repeated
     * coordinates because 1) they are unnecessary, and 2) they define 0-length line segments
     * which cause JTS location indexed line to return a segment location of NaN,
     * which we do not want.
     */
    List<ShapePoint> getUniqueShapePointsForShapeId(AgencyAndId shapeId) {
        List<ShapePoint> points = getShapePointsForShapeId(shapeId);
        ArrayList<ShapePoint> filtered = new ArrayList<ShapePoint>(points.size());
        ShapePoint last = null;
        for (ShapePoint sp : points) {
            if (last == null || last.getSequence() != sp.getSequence()) {
                if (last != null &&
                        last.getLat() == sp.getLat() &&
                        last.getLon() == sp.getLon()) {
                    LOG.trace("pair of identical shape points (skipping): {} {}", last, sp);
                } else {
                    filtered.add(sp);
                }
            }
            last = sp;
        }
        if (filtered.size() != points.size()) {
            filtered.trimToSize();
            return filtered;
        } else {
            return points;
        }
    }

    LineString getLineStringForShapeId(AgencyAndId shapeId) {

        LineString geometry = _geometriesByShapeId.get(shapeId);

        if (geometry != null)
            return geometry;

        List<ShapePoint> points = getUniqueShapePointsForShapeId(shapeId);
        if (points.size() < 2) {
            return null;
        }
        Coordinate[] coordinates = new Coordinate[points.size()];
        double[] distances = new double[points.size()];

        boolean hasAllDistances = true;

        int i = 0;
        for (ShapePoint point : points) {
            coordinates[i] = new Coordinate(point.getLon(), point.getLat());
            distances[i] = point.getDistTraveled();
            if (!point.isDistTraveledSet())
                hasAllDistances = false;
            i++;
        }

        /**
         * If we don't have distances here, we can't calculate them ourselves because we can't
         * assume the units will match
         */

        if (!hasAllDistances) {
            distances = null;
        }

        CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
        geometry = _geometryFactory.createLineString(sequence);
        _geometriesByShapeId.put(shapeId, geometry);
        _distancesByShapeId.put(shapeId, distances);

        return geometry;
    }

    private double[] getDistanceForShapeId(AgencyAndId shapeId) {
        getLineStringForShapeId(shapeId);
        return _distancesByShapeId.get(shapeId);
    }

    private LinearLocation getSegmentFraction(double[] distances, double distance) {
        int index = Arrays.binarySearch(distances, distance);
        if (index < 0)
            index = -(index + 1);
        if (index == 0)
            return new LinearLocation(0, 0.0);
        if (index == distances.length)
            return new LinearLocation(distances.length, 0.0);

        double prevDistance = distances[index - 1];
        if (prevDistance == distances[index]) {
            return new LinearLocation(index - 1, 1.0);
        }
        double indexPart = (distance - distances[index - 1])
                / (distances[index] - prevDistance);
        return new LinearLocation(index - 1, indexPart);
    }

    /**
     * Filter out any series of stop times that refer to the same stop. This is very inefficient in
     * an array-backed list, but we are assuming that this is a rare occurrence. The alternative is
     * to copy every list of stop times during filtering.
     *
     * TODO: OBA GFTS makes the stoptime lists unmodifiable, so this will not work.
     * We need to copy any modified list.
     *
     * @return whether any repeated stops were filtered out.
     */
    TIntList removeRepeatedStops (List<StopTime> stopTimes) {
        boolean filtered = false;
        StopTime prev = null;
        Iterator<StopTime> it = stopTimes.iterator();
        TIntList stopSequencesRemoved = new TIntArrayList();
        while (it.hasNext()) {
            StopTime st = it.next();
            if (prev != null) {
                if (prev.getStop().equals(st.getStop())) {
                    // OBA gives us unmodifiable lists, but we have copied them.

                    // Merge the two stop times, making sure we're not throwing out a stop time with times in favor of an
                    // interpolated stop time
                    // keep the arrival time of the previous stop, unless it didn't have an arrival time, in which case
                    // replace it with the arrival time of this stop time
                    // This is particularly important at the last stop in a route (see issue #2220)
                    if (prev.getArrivalTime() == StopTime.MISSING_VALUE) prev.setArrivalTime(st.getArrivalTime());

                    // prefer to replace with the departure time of this stop time, unless this stop time has no departure time
                    if (st.getDepartureTime() != StopTime.MISSING_VALUE) prev.setDepartureTime(st.getDepartureTime());

                    it.remove();
                    stopSequencesRemoved.add(st.getStopSequence());
                }
            }
            prev = st;
        }
        return stopSequencesRemoved;
    }


    Collection<Transfer> expandTransfer (Transfer source) {
        Stop fromStop = source.getFromStop();
        Stop toStop = source.getToStop();

        if (fromStop.getLocationType() == STOP_LOCATION_TYPE && toStop.getLocationType() == STOP_LOCATION_TYPE) {
            // simple, no need to copy anything
            return Arrays.asList(source);
        } else {
            // at least one of the stops is a parent station
            // all the stops this transfer originates with
            List<Stop> fromStops;

            // all the stops this transfer terminates with
            List<Stop> toStops;

            if (fromStop.getLocationType() == PARENT_STATION_LOCATION_TYPE) {
                fromStops = getStopsForStation(fromStop);
            } else {
                fromStops = Arrays.asList(fromStop);
            }

            if (toStop.getLocationType() == PARENT_STATION_LOCATION_TYPE) {
                toStops = getStopsForStation(toStop);
            } else {
                toStops = Arrays.asList(toStop);
            }

            List<Transfer> expandedTransfers = new ArrayList<>(fromStops.size() * toStops.size());

            for (Stop expandedFromStop : fromStops) {
                for (Stop expandedToStop : toStops) {
                    Transfer expanded = new Transfer(source);
                    expanded.setFromStop(expandedFromStop);
                    expanded.setToStop(expandedToStop);
                    expandedTransfers.add(expanded);
                }
            }

            LOG.info(
                    "Expanded transfer between stations \"{} ({})\" and \"{} ({})\" to {} transfers between {} and {} stops",
                    fromStop.getName(),
                    fromStop.getId(),
                    toStop.getName(),
                    toStop.getId(),
                    expandedTransfers.size(),
                    fromStops.size(),
                    toStops.size()
            );

            return expandedTransfers;
        }
    }

    /**
     * Identify interlined trips (where a physical vehicle continues on to another logical trip)
     * and update the TripPatterns accordingly. This must be called after all the pattern edges and vertices
     * are already created, because it creates interline dwell edges between existing pattern arrive/depart vertices.
     */
    void interline(Collection<TripPattern> tripPatterns, Graph graph) {

        /* Record which Pattern each interlined TripTimes belongs to. */
        Map<TripTimes, TripPattern> patternForTripTimes = Maps.newHashMap();

        /* TripTimes grouped by the block ID and service ID of their trips. Must be a ListMultimap to allow sorting. */
        ListMultimap<BlockIdAndServiceId, TripTimes> tripTimesForBlock = ArrayListMultimap.create();

        LOG.info("Finding interlining trips based on block IDs.");
        for (TripPattern pattern : tripPatterns) {
            Timetable timetable = pattern.scheduledTimetable;
            /* TODO: Block semantics seem undefined for frequency trips, so skip them? */
            for (TripTimes tripTimes : timetable.tripTimes) {
                Trip trip = tripTimes.trip;
                if ( ! Strings.isNullOrEmpty(trip.getBlockId())) {
                    tripTimesForBlock.put(new BlockIdAndServiceId(trip), tripTimes);
                    // For space efficiency, only record times that are part of a block.
                    patternForTripTimes.put(tripTimes, pattern);
                }
            }
        }

        /* Associate pairs of TripPatterns with lists of trips that continue from one pattern to the other. */
        Multimap<P2<TripPattern>, P2<Trip>> interlines = ArrayListMultimap.create();

        /*
          Sort trips within each block by first departure time, then iterate over trips in this block and service,
          linking them. Has no effect on single-trip blocks.
         */
        SERVICE_BLOCK :
        for (BlockIdAndServiceId block : tripTimesForBlock.keySet()) {
            List<TripTimes> blockTripTimes = tripTimesForBlock.get(block);
            Collections.sort(blockTripTimes);
            TripTimes prev = null;
            for (TripTimes curr : blockTripTimes) {
                if (prev != null) {
                    if (prev.getDepartureTime(prev.getNumStops() - 1) > curr.getArrivalTime(0)) {
                        LOG.error("Trip times within block {} are not increasing on service {} after trip {}.",
                                block.blockId, block.serviceId, prev.trip.getId());
                        continue SERVICE_BLOCK;
                    }
                    TripPattern prevPattern = patternForTripTimes.get(prev);
                    TripPattern currPattern = patternForTripTimes.get(curr);
                    Stop fromStop = prevPattern.getStop(prevPattern.getStops().size() - 1);
                    Stop toStop   = currPattern.getStop(0);
                    double teleportationDistance = SphericalDistanceLibrary.fastDistance(
                                        fromStop.getLat(), fromStop.getLon(), toStop.getLat(), toStop.getLon());
                    if (teleportationDistance > maxInterlineDistance) {
                        // FIXME Trimet data contains a lot of these -- in their data, two trips sharing a block ID just
                        // means that they are served by the same vehicle, not that interlining is automatically allowed.
                        // see #1654
                        // LOG.error(graph.addBuilderAnnotation(new InterliningTeleport(prev.trip, block.blockId, (int)teleportationDistance)));
                        // Only skip this particular interline edge; there may be other valid ones in the block.
                    } else {
                        interlines.put(new P2<TripPattern>(prevPattern, currPattern), new P2<Trip>(prev.trip, curr.trip));
                    }
                }
                prev = curr;
            }
        }

        /*
          Create the PatternInterlineDwell edges linking together TripPatterns.
          All the pattern vertices and edges must already have been created.
         */
        for (P2<TripPattern> patterns : interlines.keySet()) {
            TripPattern prevPattern = patterns.first;
            TripPattern nextPattern = patterns.second;
            // This is a single (uni-directional) edge which may be traversed forward and backward.
            PatternInterlineDwell edge = new PatternInterlineDwell(prevPattern, nextPattern);
            for (P2<Trip> trips : interlines.get(patterns)) {
                edge.add(trips.first, trips.second);
            }
        }
        LOG.info("Done finding interlining trips and creating the corresponding edges.");
    }

    void recordModeInformation(Graph graph, TripPattern tripPattern) {
    /* Iterate over all stops in this pattern recording mode information. */
        TraverseMode mode = GtfsLibrary.getTraverseMode(tripPattern.route);
        for (TransitStop tstop : tripPattern.stopVertices) {
            tstop.addMode(mode);
            if (mode == TraverseMode.SUBWAY) {
                tstop.setStreetToStopTime(subwayAccessTime);
            }
            graph.addTransitMode(mode);
        }
    }
}
