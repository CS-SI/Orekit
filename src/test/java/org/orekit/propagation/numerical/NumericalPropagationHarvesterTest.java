/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.numerical;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.DoubleArrayDictionary;

public class NumericalPropagationHarvesterTest {

    @Test
    public void testNullStmName() {
         try {
            propagator.setupMatricesComputation(null, null, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NULL_ARGUMENT, oe.getSpecifier());
            Assert.assertEquals("stmName", oe.getParts()[0]);
        }
    }

    @Test
    public void testUnknownStmName() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assert.assertNull(harvester.getStateTransitionMatrix(propagator.getInitialState()));
    }

    @Test
    public void testUnknownColumnName() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assert.assertNull(harvester.getParametersJacobian(propagator.getInitialState()));
    }

    @Test
    public void testDefaultNonNullInitialJacobian() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assert.assertNotNull(harvester.getInitialJacobianColumn("xyz"));
    }

    @Test
    public void testInitialStmCartesian() {
        doTestInitialStm(OrbitType.CARTESIAN, 0.0);
    }

    @Test
    public void testInitialStmKeplerian() {
        doTestInitialStm(OrbitType.KEPLERIAN, 2160.746);
    }

    @Test
    public void testInitialStmAbsPV() {
        SpacecraftState state = propagator.getInitialState();
        SpacecraftState absPV =
                        new SpacecraftState(new AbsolutePVCoordinates(state.getFrame(),
                                                                      state.getPVCoordinates()));
        propagator.setInitialState(absPV);
        doTestInitialStm(null, 0.0);
    }

    private void doTestInitialStm(OrbitType type, double deltaId) {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm", null, null);
        harvester.setOrbitType(type);
        harvester.setPositionAngleType(PositionAngle.TRUE);
        double[] p = new double[36];
        for (int i = 0; i < p.length; i += 7) {
            p[i] = 1.0;
        }
        SpacecraftState s = propagator.getInitialState().addAdditionalState(harvester.getStmName(), p);
        RealMatrix stm = harvester.getStateTransitionMatrix(s);
        Assert.assertEquals(deltaId, stm.subtract(MatrixUtils.createRealIdentityMatrix(6)).getNorm1(), 1.0e-3);
    }

    @Before
    public void setUp() {
        Orbit initialOrbit =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           Constants.EIGEN5C_EARTH_MU);
        double minStep = 0.0001;
        double maxStep = 60;
        double[][] tolerances = NumericalPropagator.tolerances(0.001, initialOrbit, initialOrbit.getType());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(1.0);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
    }

    @After
    public void tearDown() {
        propagator = null;
    }

    private NumericalPropagator propagator;

}
