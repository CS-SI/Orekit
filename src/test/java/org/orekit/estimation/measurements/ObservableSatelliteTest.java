package org.orekit.estimation.measurements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

class ObservableSatelliteTest {

    @Test
    void testIssue1039() {
        Assertions.assertEquals("sat-0", new ObservableSatellite(0).getName());
        Assertions.assertEquals("satellite", new ObservableSatellite(0, "satellite").getName())    ;
    }

    @Test
    void testEquals() {
        // GIVEN
        final ObservableSatellite sat0a = new ObservableSatellite(0);
        final ObservableSatellite sat0b = new ObservableSatellite(0);
        final ObservableSatellite sat1  = new ObservableSatellite(1);

        // WHEN / THEN – not even same class
        Assertions.assertNotEquals(sat0a, mock(Object.class));

        // WHEN / THEN – same propagator index
        Assertions.assertEquals(sat0a, sat0b);

        // WHEN / THEN – different propagator index
        Assertions.assertNotEquals(sat0a, sat1);
    }
}
