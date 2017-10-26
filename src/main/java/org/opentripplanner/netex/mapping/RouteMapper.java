package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.AuthorityRefStructure;
import org.rutebanken.netex.model.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    TransportTypeMapper transportTypeMapper = new TransportTypeMapper();

    public org.onebusaway2.gtfs.model.Route mapRoute(Line line, GtfsDao gtfsDao){

        org.onebusaway2.gtfs.model.Route otpRoute = new org.onebusaway2.gtfs.model.Route();

        if (line.getOperatorRef() == null) {
            LOG.warn("Line " + line.getId() + " does not have an operator.");
        } else {
            String agencyId = line.getOperatorRef().getRef();

            AgencyAndId agencyAndId = AgencyAndIdFactory.getAgencyAndId(agencyId);
            int i = 0;

            Agency agency = gtfsDao.getAllAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        }
        otpRoute.setId(AgencyAndIdFactory.getAgencyAndId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportTypeMapper.mapTransportType(line.getTransportMode().value()));
        return otpRoute;
    }
}