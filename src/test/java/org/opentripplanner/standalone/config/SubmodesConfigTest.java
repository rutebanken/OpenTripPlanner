package org.opentripplanner.standalone.config;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SubmodesConfigTest {

  private static final String SUBMODES_TEST_FILE =
      "src/main/resources/org/opentripplanner/submodes/submodes.csv";

  @Test
  public void testLoadSubmodesConfig() {
    File file = new File(SUBMODES_TEST_FILE);
    SubmodesConfig submodesConfig = new SubmodesConfig(file);
    assertEquals(30, submodesConfig.getConfig().size());
  }
}
