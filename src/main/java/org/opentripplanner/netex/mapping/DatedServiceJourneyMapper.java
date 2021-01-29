package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.TripAlterationOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.support.HierarchicalMapById;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DatedServiceJourneyRefStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.ServiceJourneyRefStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBElement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DatedServiceJourneyMapper {

  private static final Logger LOG = LoggerFactory.getLogger(DatedServiceJourneyMapper.class);


  /** Map of {@link TripAlteration} by service date, by trip id.  */
  private final Map<String, SjAlt> resultById = new HashMap<>();
  private final Map<String, DatedServiceJourney> dsjs;
  private final HierarchicalMapById<OperatingDay> operatingDaysById;

  public DatedServiceJourneyMapper(
      HierarchicalMapById<DatedServiceJourney> dsjs,
      HierarchicalMapById<OperatingDay> operatingDaysById
  ) {
    this.dsjs = dsjs.values().stream().collect(Collectors.toMap(EntityStructure::getId, Function.identity()));
    this.operatingDaysById = operatingDaysById;
  }

  static Map<String, Map<ServiceDate, TripAlterationOnDate>> map(
      HierarchicalMapById<DatedServiceJourney> dsjs,
      HierarchicalMapById<OperatingDay> operatingDaysById
  ) {
    return new DatedServiceJourneyMapper(dsjs, operatingDaysById).map();
  }

  /**
   * Index of alteration service-journey id and by service date.
   */
  private Map<String, Map<ServiceDate,TripAlterationOnDate>> map() {
    for (String id : dsjs.keySet()) {
      map(id);
    }
    return mapInternalResult();
  }

  private Map<String, Map<ServiceDate, TripAlterationOnDate>> mapInternalResult() {
    Map<String, Map<ServiceDate, TripAlterationOnDate>> m = new HashMap<>();
    for (var e : resultById.entrySet()) {
      TripAlterationOnDate alt = e.getValue().alt;
      String sjId = e.getValue().sjId;
      m.computeIfAbsent(sjId, k -> new HashMap<>()).put(alt.getDate(), alt);
    }
    return m;
  }

  private void map(String id) {
    resultById.computeIfAbsent(id, key -> map(id, dsjs.get(id)));
  }

  @Nullable
  private SjAlt map(@NotNull String id, DatedServiceJourney source) {
    if(source == null) {
      LOG.error("DatedServiceJourney referenced, but do not exist. DSJ id: " + id);
      return null;
    }

    String sjId = null;
    TripAlterationOnDate replaces = null;

    for (JAXBElement<? extends JourneyRefStructure> it : source.getJourneyRef()) {
      JourneyRefStructure ref = it.getValue();
      if(ref instanceof DatedServiceJourneyRefStructure) {
        var e = resultById.get(ref.getRef());
        replaces = e == null ? null : e.alt;
      }
      else if(ref instanceof ServiceJourneyRefStructure) {
        sjId = ref.getRef();
      }
    }
    var date = operatingDaysById
        .lookup(source.getOperatingDayRef().getRef())
        .getCalendarDate().toLocalDate();

    return new SjAlt(
        sjId,
        new TripAlterationOnDate(
          AgencyAndIdFactory.createAgencyAndId(source.getId()),
          TripServiceAlterationMapper.mapAlteration(source.getServiceAlteration()),
          new ServiceDate(date),
          replaces
        )
    );
  }

  private static class SjAlt {
    final String sjId;
    final TripAlterationOnDate alt;

    public SjAlt(String sjId, TripAlterationOnDate alt) {
      this.sjId = sjId;
      this.alt = alt;
    }
  }
}
