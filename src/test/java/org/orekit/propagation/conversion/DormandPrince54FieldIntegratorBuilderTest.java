package org.orekit.propagation.conversion;

import org.hipparchus.complex.Complex;
import org.hipparchus.ode.AbstractFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
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

class DormandPrince54FieldIntegratorBuilderTest {

    @Test
    void testToODEIntegratorBuilder() {
        // GIVEN
        final DormandPrince54FieldIntegratorBuilder<Complex> fieldIntegratorBuilder = new DormandPrince54FieldIntegratorBuilder<>(1., 10.,
                Mockito.mock(ToleranceProvider.class));
        // WHEN
        final DormandPrince54IntegratorBuilder integratorBuilder = fieldIntegratorBuilder.toODEIntegratorBuilder();
        // THEN
        Assertions.assertNotNull(integratorBuilder);
    }

    @Test
    void testBuildIntegrator() {
        // GIVEN
        final double minStep = 1;
        final ToleranceProvider mockedProvider = Mockito.mock(ToleranceProvider.class);
        final FieldAbsolutePVCoordinates<Binary64> fieldAbsolutePVCoordinates = new FieldAbsolutePVCoordinates<>(
                Binary64Field.getInstance(), new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates())));
        Mockito.when(mockedProvider.getTolerances(fieldAbsolutePVCoordinates)).thenReturn(new double[6][6]);
        // WHEN
        final DormandPrince54FieldIntegratorBuilder<Binary64> builder = new DormandPrince54FieldIntegratorBuilder<>(minStep,
                minStep, mockedProvider);
        final AbstractFieldIntegrator<Binary64> integrator = builder.buildIntegrator(fieldAbsolutePVCoordinates);
        // THEN
        Assertions.assertInstanceOf(DormandPrince54FieldIntegrator.class, integrator);
    }

}
