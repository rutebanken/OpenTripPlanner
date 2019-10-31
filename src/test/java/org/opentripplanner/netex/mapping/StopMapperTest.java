package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.model.SiteRefs_RelStructure;
import org.rutebanken.netex.model.StopPlace;

import javax.xml.bind.JAXBElement;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class StopMapperTest {

    private ObjectFactory netexObjectFactory = new ObjectFactory();

    private StopMapper stopMapper = new StopMapper(gba -> {});

    private OtpTransitBuilder otpTransitBuilder = new OtpTransitBuilder();

    private NetexDao netexDao = mock(NetexDao.class);

    @Test
    public void mapAdjacentSitesForStopPlaces() {
        StopPlace stopPlace = new StopPlace();

        SiteRefs_RelStructure siteRefs_relStructure = new SiteRefs_RelStructure();

        SiteRefStructure siteRefStructure = new SiteRefStructure();
        siteRefStructure.setRef("NSR:StopPlace:1");
        JAXBElement<SiteRefStructure> jaxbSiteRefStructure = netexObjectFactory.createSiteRef(siteRefStructure);
        siteRefs_relStructure.getSiteRef().add(jaxbSiteRefStructure);

        stopPlace.setAdjacentSites(siteRefs_relStructure);

        Collection<Stop> stop = stopMapper.mapParentAndChildStops(Arrays.asList(stopPlace), otpTransitBuilder, netexDao);

        assertThat("Stop places returned", stop.size(), is(1));

        Collection<String> actualAdjacentSites = stop.iterator().next().getAdjacentSites();

        assertThat("Number of adjacent sites for first stop", actualAdjacentSites.size(), is(1));
        assertEquals("Adjacent site ref", actualAdjacentSites.iterator().next(), "NSR:StopPlace:1");
    }
}