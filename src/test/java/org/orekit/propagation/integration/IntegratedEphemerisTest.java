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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.MatricesHarvester;
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

}
