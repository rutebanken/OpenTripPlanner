package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgencyMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Agency mapAgency(Operator operator, String timeZone){
        Agency agency = new Agency();
        agency.setId(operator.getId());
        agency.setName(operator.getName().getValue());
        agency.setTimezone(timeZone);
        if (operator.getCustomerServiceContactDetails() != null) {
            agency.setUrl(operator.getCustomerServiceContactDetails().getUrl());
            agency.setPhone(operator.getCustomerServiceContactDetails().getPhone());
        }
        return agency;
    }
}
