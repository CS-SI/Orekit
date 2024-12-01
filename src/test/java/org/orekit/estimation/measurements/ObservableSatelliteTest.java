package org.orekit.estimation.measurements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ObservableSatelliteTest {

    @Test
    public void testIssue1039() {
        Assertions.assertFalse(new ObservableSatellite(0).getSatelliteName().isPresent());
        ObservableSatellite satellite = new ObservableSatellite(0, "satellite");
        Assertions.assertTrue(satellite.getSatelliteName().isPresent());
        Assertions.assertEquals("satellite", satellite.getSatelliteName().get())    ;
    }
}