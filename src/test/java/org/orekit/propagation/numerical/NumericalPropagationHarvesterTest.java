/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.DoubleArrayDictionary;

import java.util.List;

public class NumericalPropagationHarvesterTest {

    @Test
    public void testNullStmName() {
         try {
            propagator.setupMatricesComputation(null, null, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NULL_ARGUMENT, oe.getSpecifier());
            Assertions.assertEquals("stmName", oe.getParts()[0]);
        }
    }

    @Test
    public void testUnknownStmName() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assertions.assertNull(harvester.getStateTransitionMatrix(propagator.getInitialState()));
    }

    @Test
    public void testUnknownColumnName() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assertions.assertNull(harvester.getParametersJacobian(propagator.getInitialState()));
    }

    @Test
    public void testDefaultNonNullInitialJacobian() {
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assertions.assertNotNull(harvester.getInitialJacobianColumn("xyz"));
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

    @Test
    public void testColumnsNames() {

        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm",
                                                                                            MatrixUtils.createRealIdentityMatrix(6),
                                                                                            new DoubleArrayDictionary());
        Assertions.assertTrue(harvester.getJacobiansColumnsNames().isEmpty());

        DateBasedManeuverTriggers triggers = new DateBasedManeuverTriggers("apogee_boost", propagator.getInitialState().getDate().shiftedBy(60.0), 120.0);
        PropulsionModel propulsion = new BasicConstantThrustPropulsionModel(400.0, 350.0, Vector3D.PLUS_I, "ABM-");
        propagator.addForceModel(new Maneuver(null, triggers, propulsion));
        Assertions.assertTrue(harvester.getJacobiansColumnsNames().isEmpty());

        triggers.getParametersDrivers().get(1).setSelected(true);
        propulsion.getParametersDrivers().get(0).setSelected(true);
        List<String> columnsNames = harvester.getJacobiansColumnsNames();
        Assertions.assertEquals(2, columnsNames.size());
        Assertions.assertEquals("SpanABM-" + BasicConstantThrustPropulsionModel.THRUST + Integer.toString(0), columnsNames.get(0));
        Assertions.assertEquals("Spanapogee_boost_STOP" + Integer.toString(0), columnsNames.get(1));

    }

    private void doTestInitialStm(OrbitType type, double deltaId) {
        PositionAngleType angle = PositionAngleType.TRUE;
        NumericalPropagationHarvester harvester =
                        (NumericalPropagationHarvester) propagator.setupMatricesComputation("stm", null, null);
        propagator.setOrbitType(type);
        propagator.setPositionAngleType(angle);
        double[] p = new double[36];
        for (int i = 0; i < p.length; i += 7) {
            p[i] = 1.0;
        }
        SpacecraftState s = propagator.getInitialState().addAdditionalState(harvester.getStmName(), p);
        RealMatrix stm = harvester.getStateTransitionMatrix(s);
        Assertions.assertEquals(deltaId, stm.subtract(MatrixUtils.createRealIdentityMatrix(6)).getNorm1(), 1.0e-3);
        Assertions.assertEquals(type, harvester.getOrbitType());
        Assertions.assertEquals(angle, harvester.getPositionAngleType());
    }

    @BeforeEach
    public void setUp() {
        Orbit initialOrbit =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
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

    @AfterEach
    public void tearDown() {
        propagator = null;
    }

    private NumericalPropagator propagator;

}
