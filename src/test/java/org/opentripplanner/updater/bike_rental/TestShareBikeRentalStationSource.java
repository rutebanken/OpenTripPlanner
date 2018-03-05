package org.opentripplanner.updater.bike_rental;

import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestShareBikeRentalStationSource {

    @Ignore("This test do not run on Ruter's Windows TeamCity instance, most likly due to the '?' in the url.")
    public void testShareBike() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeSource = new ShareBikeRentalDataSource();
        shareBikeSource.setUrl("file:src/test/resources/bike/share-bike.json?SystemID=dummyid");
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        assertEquals(17, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }
        BikeRentalStation prinsen = rentalStations.get(0);
        
        assertTrue(prinsen.networks.contains("dummyid"));
        
        assertEquals("01", prinsen.name.toString());
        assertEquals("dummyid_1", prinsen.id);
        assertEquals(10.392981, prinsen.x);
        assertEquals(63.426637, prinsen.y);
        assertEquals(9, prinsen.spacesAvailable);
        assertEquals(6, prinsen.bikesAvailable);
    }

    @Test
    public void testShareBikeMissingSystemIDParameter() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeSource = new ShareBikeRentalDataSource();
        shareBikeSource.setUrl("file:src/test/resources/bike/share-bike.json");
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        BikeRentalStation prinsen = rentalStations.get(0);
        
        //  Should be random value
        assertFalse(prinsen.networks.contains("dummyid"));
    }
}
