package org.opentripplanner.util.model;

import com.google.maps.internal.PolylineEncoding;

import java.io.Serializable;

/**
 * A list of coordinates encoded as a string.
 * 
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded
 * polyline algorithm format</a>
 */

public class EncodedPolylineBean implements Serializable {

    private final String polyline;

    private final int length;

    public EncodedPolylineBean(String polyline) {
        this(
            polyline,
            PolylineEncoding.decode(polyline).size()
        );
    }

    public EncodedPolylineBean(String polyline, int length) {
        this.polyline = polyline;
        this.length = length;
    }

    /**
     * The encoded points of the polyline.
     */
    public String getPoints() {
        return polyline;
    }

    public int getLength() {
        return length;
    }
}