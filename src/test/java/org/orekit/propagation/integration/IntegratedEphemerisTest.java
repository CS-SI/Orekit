/* Copyright 2002-2022 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.integration;

import java.util.Collections;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.DenseOutputModel;
import org.hipparchus.ode.ExpandableODE;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.SecondaryODE;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class IntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() {

        // Keplerian propagator definition
        KeplerianPropagator keplerEx = new KeplerianPropagator(initialOrbit);

        // Integrated ephemeris

        // Propagation
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final EphemerisGenerator generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(i);
            SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            SpacecraftState numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 0.06);
        }

        // test inv
        AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(41589);
        SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        SpacecraftState state = keplerEx.propagate(finalDate);
        numericalPropagator.setInitialState(state);
        final EphemerisGenerator generator2 = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.propagate(initialOrbit.getDate());
        BoundedPropagator invEphemeris = generator2.getGeneratedEphemeris();
        SpacecraftState numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 10e-2);

    }

    @Test
    public void testPartialDerivativesIssue16() {

        final String eqName = "derivatives";
        final EphemerisGenerator generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setOrbitType(OrbitType.CARTESIAN);
        final MatricesHarvester harvester = numericalPropagator.setupMatricesComputation(eqName, null, null);
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        ephemeris.setStepHandler(new OrekitStepHandler() {

            public void handleStep(OrekitStepInterpolator interpolator) {
                SpacecraftState state = interpolator.getCurrentState();
                RealMatrix dYdY0 = harvester.getStateTransitionMatrix(state);
                harvester.getParametersJacobian(state); // no parameters, this is a no-op and should work
                RealMatrix deltaId = dYdY0.subtract(MatrixUtils.createRealIdentityMatrix(6));
                Assert.assertTrue(deltaId.getNorm1() >  100);
                Assert.assertTrue(deltaId.getNorm1() < 3100);
            }

        });

        ephemeris.propagate(initialOrbit.getDate().shiftedBy(1800.0));

    }

    @Test
    public void testGetFrame() {
        // setup
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final EphemerisGenerator generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        //action
        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
    }

    @Test
    public void testIssue766() {

        // setup
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final EphemerisGenerator generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // verify
        Assert.assertTrue(ephemeris.getAttitudeProvider() instanceof InertialProvider);

        // action
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        ephemeris.setAttitudeProvider(new CelestialBodyPointed(FramesFactory.getEME2000(), sun, Vector3D.PLUS_K,
                                                               Vector3D.PLUS_I, Vector3D.PLUS_K));
        Assert.assertTrue(ephemeris.getAttitudeProvider() instanceof CelestialBodyPointed);

    }

    @Deprecated
    @Test
    public void testDeprecated() {

        EulerIntegrator integ = new EulerIntegrator(1.0);
        DenseOutputModel dom = new DenseOutputModel();
        integ.addStepHandler(dom);
        ExpandableODE eode = new ExpandableODE(new OrdinaryDifferentialEquation() {
            public int getDimension() { return 1; }
            public double[] computeDerivatives(double t, double[] y) { return y; }
        });
        eode.addSecondaryEquations(new SecondaryODE() {
            public int getDimension() { return 1; }
            public double[] computeDerivatives(double t, double[] primary,
                                               double[] primaryDot, double[] secondary) { return secondary; }
        });
        integ.integrate(eode, new ODEState(0.0, new double[1], new double[1][1]), 1.0);

        StateMapper mapper = new StateMapper(AbsoluteDate.ARBITRARY_EPOCH, Constants.EIGEN5C_EARTH_MU,
                                             OrbitType.CARTESIAN, PositionAngle.TRUE,
                                             new InertialProvider(FramesFactory.getEME2000()),
                                             FramesFactory.getEME2000()) {
            public void mapStateToArray(SpacecraftState state, double[] y, double[] yDot) {}
            public SpacecraftState mapArrayToState(AbsoluteDate date, double[] y, double[] yDot, PropagationType type) {
                return null;
            }
        };

        try {
            new IntegratedEphemeris(AbsoluteDate.ARBITRARY_EPOCH,
                                    AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                    mapper, PropagationType.OSCULATING,
                                    dom, Collections.emptyMap(), Collections.emptyList(),
                                    new String[] { "equation-1", "equation-2" });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            // expected as only one equation could be handled properly by this deprecated constructor
        }

        IntegratedEphemeris ie = new IntegratedEphemeris(AbsoluteDate.ARBITRARY_EPOCH,
                                                         AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                         mapper, PropagationType.OSCULATING,
                                                         dom, Collections.emptyMap(), Collections.emptyList(),
                                                         new String[] { "equation-1" });
        Assert.assertNotNull(ie);

    }

    @Test
    public void testAdditionalDerivatives() {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(10.0);
        double[][] tolerances = NumericalPropagator.tolerances(1.0e-3, initialOrbit, OrbitType.CARTESIAN);
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(1.0e-6, 10.0, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(1.0e-3);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        final DerivativesProvider provider1 = new DerivativesProvider("provider-1", 3);
        propagator.addAdditionalDerivativesProvider(provider1);
        final DerivativesProvider provider2 = new DerivativesProvider("provider-2", 1);
        propagator.addAdditionalDerivativesProvider(provider2);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.setInitialState(new SpacecraftState(initialOrbit).
                                   addAdditionalState(provider1.getName(), new double[provider1.getDimension()]).
                                   addAdditionalState(provider2.getName(), new double[provider2.getDimension()]));
        propagator.propagate(finalDate);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        for (double dt = 0; dt < ephemeris.getMaxDate().durationFrom(ephemeris.getMinDate()); dt += 0.1) {
            SpacecraftState state = ephemeris.propagate(ephemeris.getMinDate().shiftedBy(dt));
            checkState(dt, state, provider1);
            checkState(dt, state, provider2);
        }

    }

    private void checkState(final double dt, final SpacecraftState state, final DerivativesProvider provider) {

        Assert.assertTrue(state.hasAdditionalState(provider.getName()));
        Assert.assertEquals(provider.getDimension(), state.getAdditionalState(provider.getName()).length);
        for (int i = 0; i < provider.getDimension(); ++i) {
            Assert.assertEquals(i * dt, state.getAdditionalState(provider.getName())[i], 4.0e-15 * i * dt);
        }

        Assert.assertTrue(state.hasAdditionalStateDerivative(provider.getName()));
        Assert.assertEquals(provider.getDimension(), state.getAdditionalStateDerivative(provider.getName()).length);
        for (int i = 0; i < provider.getDimension(); ++i) {
            Assert.assertEquals(i, state.getAdditionalStateDerivative(provider.getName())[i], 2.0e-14 * i);
        }

    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Definition of initial conditions with position and velocity
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
            new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                 FramesFactory.getEME2000(), initDate, mu);

        // Numerical propagator definition
        double[] absTolerance = {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
        };
        double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        numericalPropagator = new NumericalPropagator(integrator);

    }

    private Orbit initialOrbit;
    private NumericalPropagator numericalPropagator;

    private static class DerivativesProvider implements AdditionalDerivativesProvider {
        private final String name;
        private final double[] derivatives;
        DerivativesProvider(final String name, final int dimension) {
            this.name        = name;
            this.derivatives = new double[dimension];
            for (int i = 0; i < dimension; ++i) {
                derivatives[i] = i;
            }
        }
        public String getName() {
            return name;
        }
        public int getDimension() {
            return derivatives.length;
        }
        public double[] derivatives(final SpacecraftState s) {
            return derivatives;
        }
    }

}
