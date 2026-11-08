package com.jnsub.quanchentrail;

import com.jnsub.quanchentrail.agent.Geo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeoTest {
    @Test
    public void samePointIsZeroMeters() {
        assertEquals(0.0, Geo.distanceMeters(36.65, 117.10, 36.65, 117.10), 0.001);
    }

    @Test
    public void oneHundredMeterBoundaryIsStable() {
        double distance = Geo.distanceMeters(36.6500, 117.1000, 36.6509, 117.1000);
        assertTrue(distance > 99.0 && distance < 101.0);
        assertEquals(100, Geo.roundedMeters(distance));
    }

    @Test
    public void longDistanceIsPositive() {
        double distance = Geo.distanceMeters(36.4983, 116.7896, 36.8508, 117.2047);
        assertTrue(distance > 40000.0);
    }
}
