package org.opentripplanner.standalone.config;

import com.csvreader.CsvReader;
import org.opentripplanner.model.OtpExtention;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitSubmodeMappingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubmodesConfig {

  private static final String DEFAULT_FILE = "org/opentripplanner/submodes/submodes.csv";

  private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;

  private static final char CSV_DELIMITER = ',';

  private static final String LIST_DELIMITER = " ";

  private final List<ConfigItem> configItems = new ArrayList<>();

  private static final Logger LOG = LoggerFactory.getLogger(SubmodesConfig.class);

  public SubmodesConfig(File file) {
    try {
      CsvReader csvReader = new CsvReader(file.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
      csvReader.readHeaders(); // Skip header
      while (csvReader.readRecord()) {
        configItems.add(new ConfigItem(csvReader.get("name"),
            TransitMainMode.valueOf(csvReader.get("mode")),
            csvReader.get("description"),
            asList(csvReader.get("netexSubmodes")),
            asList(csvReader.get("gtfsExtendedRouteTypes")),
            csvReader.get("netexOutputSubmode"),
            csvReader.get("gtfsOutputExtendedRouteType")
        ));
      }
    }
    catch (NullPointerException | IOException e) {
      LOG.error("Could not read submodes from file", e);
    }
  }

  public static SubmodesConfig getDefault() {
    return new SubmodesConfig(new File(Objects
        .requireNonNull(SubmodesConfig.class.getClassLoader().getResource(DEFAULT_FILE))
        .getFile()));
  }

  public List<TransitMode> getSubmodes() {
    return configItems
        .stream()
        .map(c -> new TransitMode(
            c.mode,
            c.name,
            c.description,
            c.extensions()
        ))
        .collect(Collectors.toList());
  }

  private static class ConfigItem {

    public final String name;
    public final TransitMainMode mode;
    public final String description;
    public final List<String> netexSubmodes;
    public final List<String> gtfsExtendRouteTypes;
    public final String netexOutputSubmode;
    public final String gtfsOutputExtendedRouteType;

    public ConfigItem(
        String name, TransitMainMode mode, String description, List<String> netexSubmodes,
        List<String> gtfsExtendRouteTypes, String netexOutputSubmode, String gtfsOutputExtendedRouteType
    ) {
      this.name = name;
      this.mode = mode;
      this.description = description;
      this.netexSubmodes = netexSubmodes;
      this.gtfsExtendRouteTypes = gtfsExtendRouteTypes;
      this.netexOutputSubmode = netexOutputSubmode;
      this.gtfsOutputExtendedRouteType = gtfsOutputExtendedRouteType;
    }

    List<TransitSubmodeMappingExtension> extensions() {
      List<TransitSubmodeMappingExtension> list = new ArrayList<>();

      if(!gtfsExtendRouteTypes.isEmpty()) {
        list.add(
            new TransitSubmodeMappingExtension(
                OtpExtention.GTFS,
                gtfsExtendRouteTypes,
                gtfsOutputExtendedRouteType
            )
        );
      }
      if(!netexOutputSubmode.isEmpty()) {
        list.add(
            new TransitSubmodeMappingExtension(
                OtpExtention.NETEX,
                netexSubmodes,
                netexOutputSubmode
            )
        );
      }
      return list;
    }
  }

  private List<String> asList(String input) {
    return Arrays.asList(input.split(LIST_DELIMITER));
  }
}
