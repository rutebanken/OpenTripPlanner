package org.opentripplanner.model.modes;

import org.opentripplanner.model.OtpExtention;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransitSubmodeMappingExtension implements Serializable {

  private final OtpExtention extension;
  private final List<String> input;
  private final String output;

  public TransitSubmodeMappingExtension(
      OtpExtention extension, List<String> input, String output
  ) {
    this.extension = extension;
    this.input = List.copyOf(input);
    this.output = output;
  }

  public OtpExtention extension() { return extension; }
  public List<String> input() { return input; }
  public String output() { return output; }


  public static Map<OtpExtention, TransitSubmodeMappingExtension> toMap(Collection<TransitSubmodeMappingExtension> list) {
    return Map.copyOf(list.stream().collect(Collectors.toMap(TransitSubmodeMappingExtension::extension, it -> it)));
  }
}
