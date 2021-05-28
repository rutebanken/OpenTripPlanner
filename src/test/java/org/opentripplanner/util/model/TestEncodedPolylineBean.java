package org.opentripplanner.util.model;

import junit.framework.TestCase;

public class TestEncodedPolylineBean extends TestCase {

    public void testEncodedPolylineBean() {
        // test taken from an example usage
        EncodedPolylineBean eplb = new EncodedPolylineBean("wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@", 17);
        assertEquals("wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@", eplb.getPoints());
        assertEquals(17, eplb.getLength());
    }
}
