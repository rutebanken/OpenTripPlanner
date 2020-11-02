package org.opentripplanner.model;


import org.opentripplanner.model.calendar.ServiceDate;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a temporary HACK to support {@link TripServiceAlteration} on a given day. Hopefully
 * we can live with this until OTP1 is replaced with OTP2.
 */
public class TripAlterationSchedule {

  private final Map<ServiceDate, TripServiceAlteration> alterations = new HashMap<>();

  private final TripServiceAlteration defaultAlteration;
  private final TripServiceAlteration highestSeverityAlteration;


  public TripAlterationSchedule(
      TripServiceAlteration defaultAlteration,
      Map<ServiceDate, TripServiceAlteration> schedule
  ) {
    this.defaultAlteration = defaultAlteration;
    this.highestSeverityAlteration =  defaultAlteration.highestAlterationOf(schedule.values());
    for (Map.Entry<ServiceDate, TripServiceAlteration> it : schedule.entrySet()) {
      add(it.getKey(), it.getValue());
    }
  }

  public TripServiceAlteration getHighestSeverity() {
    return highestSeverityAlteration;
  }

  public TripServiceAlteration get(ServiceDate date) {
    return alterations.getOrDefault(date, defaultAlteration);
  }

  private void add(ServiceDate key, TripServiceAlteration value) {
    if(value == null || value == TripServiceAlteration.planned) {
      alterations.remove(key);
    }
    else {
      alterations.put(key, value);
    }
  }
}
