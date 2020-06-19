package org.opentripplanner.standalone.config;

import com.csvreader.CsvReader;
import org.opentripplanner.model.modes.TransitMode;
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

public class SubmodesConfig {

  private static final String DEFAULT_FILE =
      "org/opentripplanner/submodes/submodes.csv";

  private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;

  private static final char CSV_DELIMITER = ',';

  private static final String LIST_DELIMITER = ",";

  private final List<ConfigItem> configItems = new ArrayList<>();

  private static final Logger LOG = LoggerFactory.getLogger(SubmodesConfig.class);

  public SubmodesConfig(File file) {
    try {
      CsvReader csvReader = new CsvReader(file.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
      csvReader.readHeaders(); // Skip header
      while (csvReader.readRecord()) {
        configItems.add(new ConfigItem(
            csvReader.get("name"),
            TransitMode.valueOf(csvReader.get("mode")),
            csvReader.get("description"),
            Arrays.asList(csvReader.get("netexSubmodes").split(LIST_DELIMITER)),
            Arrays.asList(csvReader.get("gtfsExtendedRouteTypes").split(LIST_DELIMITER))
        ));
      }
    }
    catch (NullPointerException | IOException e) {
      LOG.error("Could not read submodes from file", e);
    }
  }

  public List<ConfigItem> getConfig() {
    return configItems;
  }

  public static File getDefault() {
    return new File(
        Objects.requireNonNull(
            SubmodesConfig.class.getClassLoader().getResource(DEFAULT_FILE)).getFile()
    );
  }

  public static class ConfigItem {

    public final String name;
    public final TransitMode mode;
    public final String description;
    public final List<String> netexSubmodes;
    public final List<String> gtfsExtendRouteTypes;

    public ConfigItem(
        String name,
        TransitMode mode,
        String description,
        List<String> netexSubmodes,
        List<String> gtfsExtendRouteTypes
    ) {
      this.name = name;
      this.mode = mode;
      this.description = description;
      this.netexSubmodes = netexSubmodes;
      this.gtfsExtendRouteTypes = gtfsExtendRouteTypes;
    }
  }
}
