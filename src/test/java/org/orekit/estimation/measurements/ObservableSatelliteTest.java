package org.orekit.estimation.measurements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ObservableSatelliteTest {

    @Test
    public void testIssue1039() {
        Assertions.assertEquals("sat-0", new ObservableSatellite(0).getName());
        Assertions.assertEquals("satellite", new ObservableSatellite(0, "satellite").getName())    ;
    }
}
