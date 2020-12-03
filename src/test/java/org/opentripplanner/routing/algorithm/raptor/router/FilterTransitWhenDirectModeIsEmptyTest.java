package org.opentripplanner.routing.algorithm.raptor.router;

import org.junit.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

import java.util.List;

import static org.junit.Assert.*;

public class FilterTransitWhenDirectModeIsEmptyTest {

  @Test
  public void directModeIsExistAndIsNotWalking() {
    var modes = new RequestModes(null,null, StreetMode.BIKE, List.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.BIKE, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.BIKE, subject.originalDirectMode());
  }

  @Test
  public void directModeIsExistAndIsWalking() {
    var modes = new RequestModes(null,null, StreetMode.WALK, List.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.WALK, subject.originalDirectMode());
  }

  @Test
  public void directModeIsEmpty() {
    var modes = new RequestModes(null,null, null, List.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertTrue(subject.removeWalkAllTheWayResults());
    assertNull(subject.originalDirectMode());
  }
}