package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

class AdditionalDataProviderTest {
    private static final double DURATION = 600.0;
    private static final String STRING_BEFORE = "Let's go!";
    private static final String STRING_AFTER = "Good job!";
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
    public void testModifyMainState() {

        // Create propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        // Create state modifier
        final MainStateModifier modifier = new MainStateModifier();

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(modifier);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initDate.shiftedBy(dt));

        // Verify
        Assertions.assertEquals(2 * SpacecraftState.DEFAULT_MASS, propagated.getMass(), 1.0e-12);
        Assertions.assertEquals(FastMath.PI,
                propagated.getAttitude().getRotation().getAngle(),
                1.0e-15);

    }

    @Test
    public void testIssue900Numerical() {

        // Create propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initDate.shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    @Test
    public void testIssue900Dsst() {

        // Initialize propagator
        final DSSTPropagator propagator = new DSSTPropagator(integrator);
        propagator.setInitialState(initialState, PropagationType.MEAN);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    @Test
    public void testIssue900BrouwerLyddane() {

        // Initialize propagator
        BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialState.getOrbit(), Utils.defaultLaw(),
                GravityFieldFactory.getUnnormalizedProvider(5, 0), BrouwerLyddanePropagator.M2);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    @Test
    public void testPropagateAdditionalStringData() {
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        final MainStringDataModifier modifier = new MainStringDataModifier();
        propagator.addAdditionalDataProvider(modifier);

        final SpacecraftState propagated = propagator.propagate(initDate.shiftedBy(DURATION));
        Assertions.assertEquals(STRING_AFTER, propagated.getAdditionalData(MainStringDataModifier.class.getSimpleName()));
    }

    @Test
    public void testInterpolationAdditionalStringData() {
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        final MainStringDataModifier modifier = new MainStringDataModifier();
        propagator.addAdditionalDataProvider(modifier);
        EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initDate.shiftedBy(DURATION));
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        Assertions.assertEquals(STRING_BEFORE, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2 - 0.1)));
        Assertions.assertEquals(STRING_AFTER, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2)));
        Assertions.assertEquals(STRING_AFTER, getAdditionalDataAt(ephemeris, initDate.shiftedBy(DURATION / 2 + 0.1)));
    }

    private Object getAdditionalDataAt(Propagator propagator, AbsoluteDate date) {
        return propagator.propagate(date).getAdditionalData(MainStringDataModifier.class.getSimpleName());
    }

    private class MainStringDataModifier implements AdditionalDataProvider<String> {

        private String value;

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
            value = target.getDate().isBefore(initDate.shiftedBy(DURATION / 2)) ? STRING_BEFORE : STRING_AFTER;
        }

        @Override
        public String getAdditionalData(SpacecraftState state) {
            return value;
        }

        @Override
        public String getName() {
            return MainStringDataModifier.class.getSimpleName();
        }
    }

    private static class MainStateModifier extends AbstractStateModifier {
        /** {@inheritDoc} */
        @Override
        public SpacecraftState change(final SpacecraftState state) {
            return new SpacecraftState(state.getOrbit(),
                    new Attitude(state.getDate(),
                            state.getFrame(),
                            new Rotation(0, 0, 0, 1, false),
                            Vector3D.ZERO,
                            Vector3D.ZERO)).withMass(2 * SpacecraftState.DEFAULT_MASS);
        }
    }

    private static class TimeDifferenceProvider implements AdditionalDataProvider<double[]> {

        private final String  name;
        private boolean called;
        private double  dt;

        public TimeDifferenceProvider(final String name) {
            this.name   = name;
            this.called = false;
            this.dt     = 0.0;
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
            this.called = true;
            this.dt     = target.durationFrom(initialState.getDate());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double[] getAdditionalData(SpacecraftState state) {
            return new double[] {
                    dt
            };
        }

        public boolean wasCalled() {
            return called;
        }

    }

}