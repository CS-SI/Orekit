package org.orekit.control.heuristics;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.osc2mean.BrouwerLyddaneTheory;
import org.orekit.propagation.conversion.osc2mean.FixedPointConverter;
import org.orekit.propagation.conversion.osc2mean.MeanTheory;
import org.orekit.propagation.conversion.osc2mean.OsculatingToMeanConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class MeanOsculatingSmaChangeImpulseProviderTest {

    @Test
    void testGetImpulse() {
        // GIVEN
        final double semiMajorAxis = 1e7;
        final OsculatingToMeanConverter converter = new FixedPointConverter(new BrouwerLyddaneTheory(Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                Constants.EGM96_EARTH_MU, Constants.EGM96_EARTH_C20, 0., 0., 0., 0.));
        final MeanSmaChangeImpulseProvider impulseProvider = new MeanSmaChangeImpulseProvider(semiMajorAxis, converter);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(semiMajorAxis + 1e5, 0.01, 0., 0.1, 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        // WHEN
        final Vector3D impulse = impulseProvider.getUnconstrainedImpulse(state, true);
        // THEN
        final Vector3D velocity = orbit.getVelocity().add(impulse);
        final Orbit changedOrbit = new CartesianOrbit(new PVCoordinates(orbit.getPosition(), velocity), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final Orbit meanOrbit = converter.convertToMean(changedOrbit);
        assertEquals(semiMajorAxis, meanOrbit.getA(), 1e-6);
    }

    @Test
    void testGetImpulseOsculating() {
        // GIVEN
        final double semiMajorAxis = 1e7;
        final OsculatingToMeanConverter converter = new DummyMeanConverter();
        final MeanSmaChangeImpulseProvider impulseProvider = new MeanSmaChangeImpulseProvider(semiMajorAxis, converter);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(semiMajorAxis + 1e5, 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        // WHEN
        final Vector3D impulse = impulseProvider.getUnconstrainedImpulse(state, true);
        // THEN
        final OsculatingSmaChangeImpulseProvider osculatingSmaChangeImpulseProvider = new OsculatingSmaChangeImpulseProvider(semiMajorAxis);
        final Vector3D expected = osculatingSmaChangeImpulseProvider.getUnconstrainedImpulse(state, true);
        assertArrayEquals(expected.toArray(), impulse.toArray(), 1e-6);
    }

    private static class DummyMeanConverter implements OsculatingToMeanConverter {

        @Override
        public void setMeanTheory(MeanTheory theory) {
            // not used
        }

        @Override
        public MeanTheory getMeanTheory() {
            return null;
        }

        @Override
        public Orbit convertToMean(Orbit osculating) {
            return osculating;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldOrbit<T> convertToMean(FieldOrbit<T> osculating) {
            return osculating;
        }
    }
}
