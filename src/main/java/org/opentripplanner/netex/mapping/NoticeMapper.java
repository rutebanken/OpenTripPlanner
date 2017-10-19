package org.opentripplanner.netex.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoticeMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public org.onebusaway2.gtfs.model.Notice mapNotice(org.rutebanken.netex.model.Notice netexNotice){
        org.onebusaway2.gtfs.model.Notice otpNotice = new org.onebusaway2.gtfs.model.Notice();

        otpNotice.setId(AgencyAndIdFactory.getAgencyAndId(netexNotice.getId()));
        otpNotice.setText(netexNotice.getText().getValue());
        otpNotice.setPublicCode(netexNotice.getPublicCode());

        return otpNotice;
    }
}
