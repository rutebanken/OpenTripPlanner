package org.opentripplanner.model.modes;

import java.util.Objects;

public class TransitMode {

  private final TransitMainMode mainMode;

  private final String name;

  // For serialization
  public TransitMode() {
    mainMode = null;
    name = null;
  }

  public TransitMode(String name, TransitMainMode mainMode) {
    this.name = name;
    this.mainMode = mainMode;
  }

  public String getSubmode() {
    return name;
  }

  public TransitMainMode getMainMode() {
    return mainMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    TransitMode that = (TransitMode) o;
    return mainMode == that.mainMode && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, name);
  }
}
