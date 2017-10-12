package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.AgencyAndId;

public class AgencyAndIdFactory {
    public static AgencyAndId getAgencyAndId(String netexId) {
        return new AgencyAndId("RB", netexId);
    }

    private static String getPrefix(String string) {
        return string.split(":")[0];
    }
}
