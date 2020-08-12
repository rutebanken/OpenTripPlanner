package org.opentripplanner.model.modes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllowedTransitModes {
  private final Set<TransitMode> transitModes;

  public AllowedTransitModes(Collection<TransitMode> transitModes) {
    this.transitModes = new HashSet<>(transitModes);
  }

  public boolean isAllowed(TransitMode transitMode) {
    TransitMainMode mainMode1 = transitMode.getMainMode();
    String subMode1 = transitMode.getSubmode();

    List<TransitMode> filteredTransitModes = new ArrayList<>();

    for (TransitMode t : transitModes) {
      TransitMainMode tete = t.getMainMode();

      if (tete.equals(mainMode1)) {
        filteredTransitModes.add(t);
      }
    }


    Set<TransitMode> test2 = transitModes.stream()
        .filter(t -> t.getMainMode().equals(mainMode1)).collect(Collectors.toSet());


    boolean test = transitModes.stream()
        .filter(t -> t.getMainMode().equals(transitMode.getMainMode()))
        .anyMatch(t -> t.getSubmode() == null || t.getSubmode().equals(transitMode.getSubmode()));


    return transitModes.stream()
        .filter(t -> t.getMainMode().equals(transitMode.getMainMode()))
        .anyMatch(t -> t.getSubmode() == null || t.getSubmode().equals(transitMode.getSubmode()));
  }

  public boolean isEmpty() {
    return transitModes.isEmpty();
  }
}
