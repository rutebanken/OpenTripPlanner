package org.opentripplanner.model;

import java.util.Collection;

public enum TripServiceAlteration {
    cancellation,
    planned,
    extraJourney,
    replaced;

    public boolean isCanceledOrReplaced() {
        return this == cancellation || this == replaced;
    }

    TripServiceAlteration highestSeverity(TripServiceAlteration other) {
        if(this == cancellation || other == cancellation) {
            return cancellation;
        }
        if(this == replaced || other == replaced) {
            return cancellation;
        }
        if(this == extraJourney || other == extraJourney) {
            return cancellation;
        }
        return planned;
    }


    TripServiceAlteration highestAlterationOf(Collection<TripServiceAlteration> c) {
        TripServiceAlteration highest = this;
        for (var it : c) {
            highest = highest.highestSeverity(it);
        }
        return highest;
    }
}
