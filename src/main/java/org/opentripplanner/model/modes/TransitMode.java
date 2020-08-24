package org.opentripplanner.model.modes;

import org.opentripplanner.model.OtpExtention;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A mode specified either by the TransitMainMode enum or the TransitMainMode enum and a
 * customizable subMode string specified by the TransitModeServiceModule module at graph build time.
 * <p>
 * TransitMainMode is equivalent to GTFS route_type or to NeTEx TransportMode.
 * <p>
 * SubMode is equivalent to either GTFS extended route_type or NeTEx TransportSubMode.
 * <p>
 * This should only be instantiated in the TransitModeService class, to ensure only configured modes
 * are used.
 */
public class TransitMode implements Serializable {

  private final TransitMainMode mainMode;

  private final String subMode;

  private final String description;

  private final Map<OtpExtention, TransitSubmodeMappingExtension> extensions;

  /**
   * Mapping between the old TransitMainMode enum and the new detailed TransitMode class. This
   * ensures that we are reusing TransitMode objects when the fromMainModeEnum method is called.
   */
  private static final Map<TransitMainMode, TransitMode> mainTransitModes = Arrays
      .stream(TransitMainMode.values())
      .map(m -> new TransitMode(m,
          null,
          null,
          List.of()
      ))
      .collect(Collectors.toMap(TransitMode::getMainMode, m -> m));

  public static TransitMode fromMainModeEnum(TransitMainMode mainMode) {
    return mainTransitModes.get(mainMode);
  }

  public TransitMode(
      TransitMainMode mainMode,
      String subMode,
      String description,
      Collection<TransitSubmodeMappingExtension> extensions
  ) {
    this.mainMode = mainMode;
    this.subMode = subMode;
    this.description = description;
    this.extensions = TransitSubmodeMappingExtension.toMap(extensions);
  }

  public TransitSubmodeMappingExtension extension(OtpExtention extension) {
    return extensions.get(extension);
  }

  public String getSubMode() {
    return subMode;
  }

  public TransitMainMode getMainMode() {
    return mainMode;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    TransitMode that = (TransitMode) o;
    return mainMode == that.mainMode && Objects.equals(subMode, that.subMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mainMode, subMode);
  }
}
