package org.opentripplanner.graph_builder.triptransformer.transform;

import org.opentripplanner.model.AgencyAndId;

class NewServiceIds {
    final AgencyAndId targetId;
    final AgencyAndId otherDaysId;

    NewServiceIds(AgencyAndId targetId, AgencyAndId otherDaysId) {
        this.targetId = targetId;
        this.otherDaysId = otherDaysId;
    }
}
