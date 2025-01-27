package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

class AdditionalDataProviderTest {
    private static final double DURATION = 600.0;
    private static final String BEFORE = "Let's go!";
    private static final String AFTER = "Good job!";
    private AbsoluteDate initDate;
    private SpacecraftState initialState;
    private AdaptiveStepsizeIntegrator integrator;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        final double mu = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = ToleranceProvider.getDefaultToleranceProvider(0.001).getTolerances(orbit, OrbitType.EQUINOCTIAL);
        integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
    }

    @Test
    public void testPropagate() {
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        final MainDataModifier modifier = new MainDataModifier();
        propagator.addAdditionalDataProvider(modifier);

        final SpacecraftState propagated = propagator.propagate(initDate.shiftedBy(DURATION));
        Assertions.assertEquals(AFTER, propagated.getAdditionalData(MainDataModifier.class.getSimpleName()));
    }

    @Test
    public void testInterpolation() {
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        final MainDataModifier modifier = new MainDataModifier();
        propagator.addAdditionalDataProvider(modifier);
        EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initDate.shiftedBy(DURATION));
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        Assertions.assertEquals(BEFORE, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2 - 0.1)));
        Assertions.assertEquals(AFTER, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2)));
        Assertions.assertEquals(AFTER, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2 + 0.1)));
    }

    private Object getAdditionalDataAt(Propagator propagator, AbsoluteDate date) {
        return propagator.propagate(date).getAdditionalData(MainDataModifier.class.getSimpleName());
    }



    private class MainDataModifier implements AdditionalDataProvider<String> {

        private String value;

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
            value = target.getDate().isBefore(initDate.shiftedBy(DURATION / 2)) ? BEFORE : AFTER;
        }

        @Override
        public String getAdditionalData(SpacecraftState state) {
            return value;
        }

        @Override
        public String getName() {
            return MainDataModifier.class.getSimpleName();
        }
    }
}