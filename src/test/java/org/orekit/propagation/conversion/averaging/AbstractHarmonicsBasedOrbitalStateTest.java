package org.orekit.propagation.conversion.averaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.elements.AveragedOrbitalElements;
import org.orekit.time.AbsoluteDate;

class AbstractHarmonicsBasedOrbitalStateTest {

    @Test
    void testGetMu() {
        // GIVEN
        final double expectedMu = 1.;
        final UnnormalizedSphericalHarmonicsProvider mockedProvider = Mockito
                .mock(UnnormalizedSphericalHarmonicsProvider.class);
        Mockito.when(mockedProvider.getMu()).thenReturn(expectedMu);
        final TestHarmonicsBasedOrbitalSTate elements = new TestHarmonicsBasedOrbitalSTate(mockedProvider);
        // WHEN
        final double actualMu = elements.getMu();
        // THEN
        Assertions.assertEquals(expectedMu, actualMu);
    }

    private static class TestHarmonicsBasedOrbitalSTate
            extends AbstractHarmonicsBasedOrbitalState {

        protected TestHarmonicsBasedOrbitalSTate(UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
            super(AbsoluteDate.ARBITRARY_EPOCH, FramesFactory.getGCRF(), harmonicsProvider);
        }

        @Override
        public AveragedOrbitalElements getAveragedElements() {
            return null;
        }

        @Override
        public OrbitType getOrbitType() {
            return null;
        }

        @Override
        public PositionAngleType getPositionAngleType() {
            return null;
        }

        @Override
        public Orbit toOsculatingOrbit() {
            return null;
        }
    }

}
