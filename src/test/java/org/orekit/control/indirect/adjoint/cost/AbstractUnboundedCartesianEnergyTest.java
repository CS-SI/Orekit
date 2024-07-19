package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;
import java.util.stream.Stream;

class AbstractUnboundedCartesianEnergyTest {

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final AbstractUnboundedCartesianEnergy mockedEnergy = Mockito.mock(AbstractUnboundedCartesianEnergy.class);
        Mockito.when(mockedEnergy.getEventDetectors()).thenCallRealMethod();
        // WHEN
        final Stream<?> detectors = mockedEnergy.getEventDetectors();
        // THEN
        Assertions.assertTrue(detectors.collect(Collectors.toSet()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFieldGetEventDetectors() {
        // GIVEN
        final AbstractUnboundedCartesianEnergy mockedEnergy = Mockito.mock(AbstractUnboundedCartesianEnergy.class);
        Mockito.when(mockedEnergy.getFieldEventDetectors(Mockito.any(Field.class))).thenCallRealMethod();
        // WHEN
        final Stream<?> detectors = mockedEnergy.getFieldEventDetectors(Mockito.mock(Field.class));
        // THEN
        Assertions.assertTrue(detectors.collect(Collectors.toSet()).isEmpty());
    }

}
