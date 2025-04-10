package org.orekit.propagation.conversion;

import org.hipparchus.ode.AbstractFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdamsBashforthFieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class AdamsBashforthFieldIntegratorBuilderTest {

    @Test
    void testBuildIntegrator() {
        // GIVEN
        final double minStep = 1;
        final ToleranceProvider mockedProvider = Mockito.mock(ToleranceProvider.class);
        final FieldAbsolutePVCoordinates<Binary64> fieldAbsolutePVCoordinates = new FieldAbsolutePVCoordinates<>(
                Binary64Field.getInstance(), new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates())));
        Mockito.when(mockedProvider.getTolerances(fieldAbsolutePVCoordinates)).thenReturn(new double[6][6]);
        final int nSteps = 2;
        // WHEN
        final AdamsBashforthFieldIntegratorBuilder<Binary64> builder = new AdamsBashforthFieldIntegratorBuilder<>(nSteps, minStep,
                minStep, mockedProvider);
        final AbstractFieldIntegrator<Binary64> integrator = builder.buildIntegrator(fieldAbsolutePVCoordinates);
        Assertions.assertInstanceOf(AdamsBashforthFieldIntegrator.class, integrator);
    }

    @Test
    void testToODEIntegratorBuilder() {
        // GIVEN
        final ToleranceProvider mockedProvider = Mockito.mock(ToleranceProvider.class);
        final int nSteps = 2;
        final AdamsBashforthFieldIntegratorBuilder<Binary64> fieldIntegratorBuilder = new AdamsBashforthFieldIntegratorBuilder<>(nSteps, 1,
                2., mockedProvider);
        // WHEN
        final AdamsBashforthIntegratorBuilder integratorBuilder = fieldIntegratorBuilder.toODEIntegratorBuilder();
        // THEN
        Assertions.assertEquals(integratorBuilder.getMinStep(), fieldIntegratorBuilder.getMinStep());
        Assertions.assertEquals(integratorBuilder.getMaxStep(), fieldIntegratorBuilder.getMaxStep());
    }

}
