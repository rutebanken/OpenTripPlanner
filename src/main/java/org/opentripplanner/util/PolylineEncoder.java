package org.opentripplanner.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.LatLng;
import org.opentripplanner.util.model.EncodedPolylineBean;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class PolylineEncoder {

    public static EncodedPolylineBean createEncodings(Iterable<Coordinate> points) {
        List<LatLng> coordinates = new ArrayList<>();
        for (Coordinate coordinate : points) {
            coordinates.add(new LatLng(coordinate.y, coordinate.x));
        }
        return new EncodedPolylineBean(PolylineEncoding.encode(coordinates), coordinates.size());
    }

    public static EncodedPolylineBean createEncodings(Geometry geometry) {
        if (geometry instanceof LineString) {
            LineString string = (LineString) geometry;
            Coordinate[] coordinates = string.getCoordinates();
            return createEncodings(new CoordinateList(coordinates));
        } else if (geometry instanceof MultiLineString) {
            MultiLineString mls = (MultiLineString) geometry;
            return createEncodings(new CoordinateList(mls.getCoordinates()));
        } else {
            throw new IllegalArgumentException(geometry.toString());
        }
    }

    public static List<Coordinate> decode(EncodedPolylineBean polyline) {
        List<LatLng> coordinates = PolylineEncoding.decode(polyline.getPoints());

        return coordinates
            .stream()
            .map(c -> new Coordinate(c.lng, c.lat))
            .collect(Collectors.toList());
    }

    private static class CoordinateList extends AbstractList<Coordinate> {

        private final Coordinate[] coordinates;

        public CoordinateList(Coordinate[] coordinates) {
            this.coordinates = coordinates;
        }

        @Override
        public Coordinate get(int index) {
            return coordinates[index];
        }

        @Override
        public int size() {
            return coordinates.length;
        }
    }
}
