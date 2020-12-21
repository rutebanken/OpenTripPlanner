package org.opentripplanner.model.calendar;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceDateTest {

  @Test
  public void asIsoString() {
    assertEquals("2020-12-24", new ServiceDate(2020, 12,24).asIsoString());
    assertEquals("0001-01-01", new ServiceDate(1, 1,1).asIsoString());
  }

  @Test
  public void toYyyyMmDd() {
    assertEquals("20201224", new ServiceDate(2020, 12,24).toYyyyMmDd());
    assertEquals("00010101", new ServiceDate(1, 1,1).toYyyyMmDd());
  }
}