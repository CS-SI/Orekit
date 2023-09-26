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

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.jacobians.TriggerDate;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Unit tests for {@link StateTransitionMatrixGenerator}. */
public class StateTransitionMatrixGeneratorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

    @Test
    public void testInterrupt() {
        final AbsoluteDate firing = new AbsoluteDate(new DateComponents(2004, 1, 2),
                                                     new TimeComponents(4, 15, 34.080),
                                                     TimeScalesFactory.getUTC());
        final double duration = 200.0;

        // first propagation, covering the maneuver
        DateBasedManeuverTriggers triggers1 = new DateBasedManeuverTriggers("MAN_0", firing, duration);
        final NumericalPropagator propagator1  = buildPropagator(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, 20,
                                                                 firing, duration, triggers1);
        propagator1.
        getAllForceModels().
        forEach(fm -> fm.
                          getParametersDrivers().
                          stream().
                          filter(d -> d.getName().equals("MAN_0_START") ||
                                      d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                          forEach(d -> d.setSelected(true)));
        final MatricesHarvester   harvester1   = propagator1.setupMatricesComputation("stm", null, null);
        final SpacecraftState     state1       = propagator1.propagate(firing.shiftedBy(2 * duration));
        final RealMatrix          stm1         = harvester1.getStateTransitionMatrix(state1);
        final RealMatrix          jacobian1    = harvester1.getParametersJacobian(state1);

        // second propagation, interrupted during maneuver
        DateBasedManeuverTriggers triggers2 = new DateBasedManeuverTriggers("MAN_0", firing, duration);
                final NumericalPropagator propagator2  = buildPropagator(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, 20,
                                                                         firing, duration, triggers2);
        propagator2.
        getAllForceModels().
        forEach(fm -> fm.
                getParametersDrivers().
                stream().
                filter(d -> d.getName().equals("MAN_0_START") ||
                       d.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT)).
                forEach(d -> d.setSelected(true)));

         // some additional providers for test coverage
        final StateTransitionMatrixGenerator dummyStmGenerator =
                        new StateTransitionMatrixGenerator("dummy-1",
                                                           Collections.emptyList(),
                                                           propagator2.getAttitudeProvider());
        propagator2.addAdditionalDerivativesProvider(dummyStmGenerator);
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState(dummyStmGenerator.getName(), new double[36]));
        propagator2.addAdditionalDerivativesProvider(new IntegrableJacobianColumnGenerator(dummyStmGenerator, "dummy-2"));
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-2", new double[6]));
        propagator2.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {
            public String getName() { return "dummy-3"; }
            public int getDimension() { return 1; }
            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[1], null);
            }
        });
        propagator2.setInitialState(propagator2.getInitialState().addAdditionalState("dummy-3", new double[1]));
        propagator2.addAdditionalStateProvider(new TriggerDate(dummyStmGenerator.getName(), "dummy-4", true,
                                                               (Maneuver) propagator2.getAllForceModels().get(1),
                                                               1.0e-6));
        propagator2.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() { return "dummy-5"; }
            public double[] getAdditionalState(SpacecraftState s) { return new double[1]; }
        });
        final MatricesHarvester   harvester2   = propagator2.setupMatricesComputation("stm", null, null);
        final SpacecraftState     intermediate = propagator2.propagate(firing.shiftedBy(0.5 * duration));
        final RealMatrix          stmI         = harvester2.getStateTransitionMatrix(intermediate);
        final RealMatrix          jacobianI    = harvester2.getParametersJacobian(intermediate);

        // intermediate state has really different matrices, they are still building up
        Assertions.assertEquals(0.1253, stmI.subtract(stm1).getNorm1() / stm1.getNorm1(),                1.0e-4);
        Assertions.assertEquals(0.0225, jacobianI.subtract(jacobian1).getNorm1() / jacobian1.getNorm1(), 1.0e-4);

        // restarting propagation where we left it
        final SpacecraftState     state2       = propagator2.propagate(firing.shiftedBy(2 * duration));
        final RealMatrix          stm2         = harvester2.getStateTransitionMatrix(state2);
        final RealMatrix          jacobian2    = harvester2.getParametersJacobian(state2);

        // after completing the two-stage propagation, we get the same matrices
        Assertions.assertEquals(0.0, stm2.subtract(stm1).getNorm1(), 1.3e-13 * stm1.getNorm1());
        Assertions.assertEquals(0.0, jacobian2.subtract(jacobian1).getNorm1(), 7.0e-11 * jacobian1.getNorm1());

    }

    /**
     * check {@link StateTransitionMatrixGenerator#generate(SpacecraftState)} correctly sets the satellite velocity.
     */
    @Test
    public void testComputeDerivativesStateVelocity() {

        //setup
        /** arbitrary date */
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        /** Earth gravitational parameter */
        final double gm = Constants.EIGEN5C_EARTH_MU;
        /** arbitrary inertial frame */
        Frame eci = FramesFactory.getGCRF();
        NumericalPropagator propagator = new NumericalPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        MockForceModel forceModel = new MockForceModel();
        propagator.addForceModel(forceModel);
        StateTransitionMatrixGenerator stmGenerator =
                        new StateTransitionMatrixGenerator("stm",
                                                           propagator.getAllForceModels(),
                                                           propagator.getAttitudeProvider());
        Vector3D p = new Vector3D(7378137, 0, 0);
        Vector3D v = new Vector3D(0, 7500, 0);
        PVCoordinates pv = new PVCoordinates(p, v);
        SpacecraftState state = stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(new CartesianOrbit(pv, eci, date, gm)),
                                                                             MatrixUtils.createRealIdentityMatrix(6),
                                                                             propagator.getOrbitType(),
                                                                             propagator.getPositionAngleType());

        //action
        stmGenerator.combinedDerivatives(state);

        //verify
        MatcherAssert.assertThat(forceModel.accelerationDerivativesPosition.toVector3D(), is(pv.getPosition()));
        MatcherAssert.assertThat(forceModel.accelerationDerivativesVelocity.toVector3D(), is(pv.getVelocity()));

    }

    @Test
    public void testPropagationTypesElliptical() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 0.001;
        for (OrbitType orbitType : OrbitType.values()) {
            for (PositionAngleType angleType : PositionAngleType.values()) {

                // compute state Jacobian using StateTransitionMatrixGenerator
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                final SpacecraftState initialState = new SpacecraftState(initialOrbit);
                propagator.setInitialState(initialState);
                PickUpHandler pickUp = new PickUpHandler(propagator, null, null, null);
                propagator.getMultiplexer().add(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = finiteDifferencesStm(initialOrbit, orbitType, angleType, dP, dt, gravityField);

                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUp.getStm().getEntry(i, j) - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assertions.assertEquals(0, error, 6.0e-2);

                    }
                }

            }
        }

    }

    @Test
    public void testPropagationTypesHyperbolic() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        Orbit initialOrbit =
                new KeplerianOrbit(new PVCoordinates(new Vector3D(-1551946.0, 708899.0, 6788204.0),
                                                     new Vector3D(-9875.0, -3941.0, -1845.0)),
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 0.001;
        for (OrbitType orbitType : new OrbitType[] { OrbitType.KEPLERIAN, OrbitType.CARTESIAN }) {
            for (PositionAngleType angleType : PositionAngleType.values()) {

                // compute state Jacobian using StateTransitionMatrixGenerator
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                final SpacecraftState initialState = new SpacecraftState(initialOrbit);
                PickUpHandler pickUp = new PickUpHandler(propagator, null, null, null);
                propagator.setInitialState(initialState);
                propagator.setStepHandler(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = finiteDifferencesStm(initialOrbit, orbitType, angleType, dP, dt, gravityField);
                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUp.getStm().getEntry(i, j) - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assertions.assertEquals(0, error, 1.0e-3);

                    }
                }

            }
        }

    }

    @Test
    public void testAccelerationPartialNewtonOnly() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel newton = new NewtonianAttraction(provider.getMu());
        ParameterDriver gmDriver = newton.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
        gmDriver.setSelected(true);
        Orbit initialOrbit =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());

        NumericalPropagator propagator =
                        setUpPropagator(initialOrbit, 0.001, OrbitType.EQUINOCTIAL, PositionAngleType.MEAN,
                                        newton);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        propagator.setInitialState(initialState);
        AbsoluteDate pickupDate = initialOrbit.getDate().shiftedBy(200);
        PickUpHandler pickUp = new PickUpHandler(propagator, pickupDate, gmDriver.getNameSpan(new AbsoluteDate()), gmDriver.getNameSpan(new AbsoluteDate()));
        propagator.setStepHandler(pickUp);
        propagator.propagate(initialState.getDate().shiftedBy(900.0));
        Assertions.assertEquals(0.0, pickUp.getState().getDate().durationFrom(pickupDate), 1.0e-10);
        final Vector3D position = pickUp.getState().getPosition();
        final double   r        = position.getNorm();

        // here, we check that the trivial partial derivative of Newton acceleration is computed correctly
        Assertions.assertEquals(-position.getX() / (r * r * r), pickUp.getAccPartial()[0], 1.0e-15 / (r * r));
        Assertions.assertEquals(-position.getY() / (r * r * r), pickUp.getAccPartial()[1], 1.0e-15 / (r * r));
        Assertions.assertEquals(-position.getZ() / (r * r * r), pickUp.getAccPartial()[2], 1.0e-15 / (r * r));

    }

    @Test
    public void testAccelerationPartialGravity5x5() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
                        new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        ParameterDriver gmDriver = gravityField.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
        gmDriver.setSelected(true);
        ForceModel newton = new NewtonianAttraction(provider.getMu());
        Orbit initialOrbit =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());

        NumericalPropagator propagator =
                        setUpPropagator(initialOrbit, 0.001, OrbitType.EQUINOCTIAL, PositionAngleType.MEAN,
                                        gravityField, newton);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        propagator.setInitialState(initialState);
        AbsoluteDate pickupDate = initialOrbit.getDate().shiftedBy(200);
        PickUpHandler pickUp = new PickUpHandler(propagator, pickupDate, gmDriver.getNameSpan(new AbsoluteDate()), gmDriver.getNameSpan(new AbsoluteDate()));
        propagator.setStepHandler(pickUp);
        propagator.propagate(initialState.getDate().shiftedBy(900.0));
        Assertions.assertEquals(0.0, pickUp.getState().getDate().durationFrom(pickupDate), 1.0e-10);
        final Vector3D position = pickUp.getState().getPosition();
        final double   r        = position.getNorm();
        // here we check that when µ appear is another force model, partial derivatives are not Newton-only anymore
        Assertions.assertTrue(FastMath.abs(-position.getX() / (r * r * r) - pickUp.getAccPartial()[0]) > 2.0e-4 / (r * r));
        Assertions.assertTrue(FastMath.abs(-position.getY() / (r * r * r) - pickUp.getAccPartial()[1]) > 2.0e-4 / (r * r));
        Assertions.assertTrue(FastMath.abs(-position.getZ() / (r * r * r) - pickUp.getAccPartial()[2]) > 2.0e-4 / (r * r));

    }

    @Test
    public void testMultiSat() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Orbit initialOrbitA =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());
        Orbit initialOrbitB =
                        new KeplerianOrbit(7900000.0, 0.015, 0.04, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());

        double dt = 900;
        double dP = 0.001;
        for (OrbitType orbitType : OrbitType.values()) {
            for (PositionAngleType angleType : PositionAngleType.values()) {

                // compute state Jacobian using StateTransitionMatrixGenerator
                NumericalPropagator propagatorA1 = setUpPropagator(initialOrbitA, dP, orbitType, angleType,
                                                                   new HolmesFeatherstoneAttractionModel(itrf, provider));
                final SpacecraftState initialStateA = new SpacecraftState(initialOrbitA);
                propagatorA1.setInitialState(initialStateA);
                final PickUpHandler pickUpA = new PickUpHandler(propagatorA1, null, null, null);
                propagatorA1.setStepHandler(pickUpA);

                NumericalPropagator propagatorB1 = setUpPropagator(initialOrbitB, dP, orbitType, angleType,
                                                                   new HolmesFeatherstoneAttractionModel(itrf, provider));
                final SpacecraftState initialStateB1 = new SpacecraftState(initialOrbitB);
                propagatorB1.setInitialState(initialStateB1);
                final PickUpHandler pickUpB = new PickUpHandler(propagatorB1, null, null, null);
                propagatorB1.setStepHandler(pickUpB);

                PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(Arrays.asList(propagatorA1, propagatorB1),
                                                                                   interpolators -> {});
                parallelizer.propagate(initialStateA.getDate(), initialStateA.getDate().shiftedBy(dt));

                // compute reference state Jacobian using finite differences
                double[][] dYdY0RefA = finiteDifferencesStm(initialOrbitA, orbitType, angleType, dP, dt,
                                                            new HolmesFeatherstoneAttractionModel(itrf, provider));
                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUpA.getStm().getEntry(i, j) - dYdY0RefA[i][j]) / dYdY0RefA[i][j]);
                        Assertions.assertEquals(0, error, 6.0e-2);

                    }
                }

                double[][] dYdY0RefB = finiteDifferencesStm(initialOrbitB, orbitType, angleType, dP, dt,
                                                            new HolmesFeatherstoneAttractionModel(itrf, provider));
                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUpB.getStm().getEntry(i, j) - dYdY0RefB[i][j]) / dYdY0RefB[i][j]);
                        Assertions.assertEquals(0, error, 6.0e-2);

                    }
                }

            }
        }

    }

    @Test
    public void testParallelStm() {

        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                         FramesFactory.getEME2000(), date, Constants.EIGEN5C_EARTH_MU);
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(120.0);
        OrbitType type = OrbitType.CARTESIAN;
        double minStep = 0.0001;
        double maxStep = 60;
        double[][] tolerances = NumericalPropagator.tolerances(0.001, orbit, type);
        AdaptiveStepsizeIntegrator integrator0 = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
        integrator0.setInitialStepSize(1.0);
        NumericalPropagator p0 = new NumericalPropagator(integrator0);
        p0.setInitialState(new SpacecraftState(orbit).addAdditionalState("tmp", new double[1]));
        p0.setupMatricesComputation("stm0", null, null);
        AdaptiveStepsizeIntegrator integrator1 = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
        integrator1.setInitialStepSize(1.0);
        NumericalPropagator p1 = new NumericalPropagator(integrator1);
        p1.setInitialState(new SpacecraftState(orbit));
        p1.setupMatricesComputation("stm1", null, null);
        final List<SpacecraftState> results = new PropagatorsParallelizer(Arrays.asList(p0, p1), interpolators -> {}).
                                              propagate(startDate, endDate);
        Assertions.assertEquals(-0.07953750951271785, results.get(0).getAdditionalState("stm0")[0], 1.0e-10);
        Assertions.assertEquals(-0.07953750951271785, results.get(1).getAdditionalState("stm1")[0], 1.0e-10);
    }

    @Test
    public void testNotInitialized() {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngleType.TRUE);
        StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                         propagator.getAllForceModels(),
                                                                                         propagator.getAttitudeProvider());
        propagator.addAdditionalDerivativesProvider(stmGenerator);
        Assertions.assertTrue(stmGenerator.yields(new SpacecraftState(initialOrbit)));
     }

    @Test
    public void testMismatchedDimensions() {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngleType.TRUE);
        StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                         propagator.getAllForceModels(),
                                                                                         propagator.getAttitudeProvider());
        propagator.addAdditionalDerivativesProvider(stmGenerator);
        try {
            stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                         MatrixUtils.createRealMatrix(5,  6),
                                                         propagator.getOrbitType(),
                                                         propagator.getPositionAngleType());
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2, oe.getSpecifier());
            Assertions.assertEquals(5, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[2]).intValue());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[3]).intValue());
        }

        try {
            stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                         MatrixUtils.createRealMatrix(6,  5),
                                                         propagator.getOrbitType(),
                                                         propagator.getPositionAngleType());
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2, oe.getSpecifier());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals(5, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[2]).intValue());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[3]).intValue());
        }

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngleType angleType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitType, angleType)[0];
        double[] aM3h = stateToArray(sM3h, orbitType, angleType)[0];
        double[] aM2h = stateToArray(sM2h, orbitType, angleType)[0];
        double[] aM1h = stateToArray(sM1h, orbitType, angleType)[0];
        double[] aP1h = stateToArray(sP1h, orbitType, angleType)[0];
        double[] aP2h = stateToArray(sP2h, orbitType, angleType)[0];
        double[] aP3h = stateToArray(sP3h, orbitType, angleType)[0];
        double[] aP4h = stateToArray(sP4h, orbitType, angleType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType) {
        double[][] array = new double[2][6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngleType angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }

    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngleType angleType,
                                                ForceModel... models) {

        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        NumericalPropagator propagator =
            new NumericalPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(angleType);
        for (ForceModel model : models) {
            propagator.addForceModel(model);
        }
        return propagator;
    }

    private double[][] finiteDifferencesStm(final Orbit initialOrbit, final OrbitType orbitType, final PositionAngleType angleType,
                                            final double dP, final double dt, ForceModel... models) {

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        AbstractIntegratedPropagator propagator2 = setUpPropagator(initialOrbit, dP, orbitType, angleType, models);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, initialOrbit, orbitType)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType, -4 * steps[i], i));
            SpacecraftState sM4h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType, -3 * steps[i], i));
            SpacecraftState sM3h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType, -2 * steps[i], i));
            SpacecraftState sM2h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType, -1 * steps[i], i));
            SpacecraftState sM1h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType,  1 * steps[i], i));
            SpacecraftState sP1h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType,  2 * steps[i], i));
            SpacecraftState sP2h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType,  3 * steps[i], i));
            SpacecraftState sP3h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            propagator2.resetInitialState(shiftState(initialState, orbitType, angleType,  4 * steps[i], i));
            SpacecraftState sP4h = propagator2.propagate(initialState.getDate().shiftedBy(dt));
            fillJacobianColumn(dYdY0Ref, i, orbitType, angleType, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        return dYdY0Ref;

    }

    private NumericalPropagator buildPropagator(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                                final int degree, final AbsoluteDate firing, final double duration,
                                                final DateBasedManeuverTriggers triggers) {

        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        SpacecraftState initialState = buildInitialState(attitudeProvider);

        final double isp      = 318;
        final double f        = 420;
        PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM");

        double[][] tol = NumericalPropagator.tolerances(0.01, initialState.getOrbit(), orbitType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(positionAngleType);
        propagator.setAttitudeProvider(attitudeProvider);
        if (degree > 0) {
            propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                           GravityFieldFactory.getNormalizedProvider(degree, degree)));
        }
        final Maneuver maneuver = new Maneuver(null, triggers, propulsionModel);
        propagator.addForceModel(maneuver);
        propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() { return triggers.getName().concat("-acc"); }
            public double[] getAdditionalState(SpacecraftState state) {
                double[] parameters = Arrays.copyOfRange(maneuver.getParameters(initialState.getDate()), 0, propulsionModel.getParametersDrivers().size());
                return new double[] {
                    propulsionModel.getAcceleration(state, state.getAttitude(), parameters).getNorm()
                };
            }
        });
        propagator.setInitialState(initialState);
        return propagator;

    }

    private SpacecraftState buildInitialState(final AttitudeProvider attitudeProvider) {
        final double mass  = 2500;
        final double a     = 24396159;
        final double e     = 0.72831215;
        final double i     = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv    = 0;

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 1, 1), new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit        orbit    = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                         FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        return new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private AttitudeProvider buildAttitudeProvider() {
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        return new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));
    }

    /** Mock {@link ForceModel}. */
    private static class MockForceModel implements ForceModel {

        /**
         * argument for {@link #accelerationDerivatives(AbsoluteDate, Frame,
         * FieldVector3D, FieldVector3D, FieldRotation, DerivativeStructure)}.
         */
        public FieldVector3D<DerivativeStructure> accelerationDerivativesPosition;
        /**
         * argument for {@link #accelerationDerivatives(AbsoluteDate, Frame,
         * FieldVector3D, FieldVector3D, FieldRotation, DerivativeStructure)}.
         */
        public FieldVector3D<DerivativeStructure> accelerationDerivativesVelocity;

        /** {@inheritDoc} */
        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public <T extends CalculusFieldElement<T>> void
            addContribution(FieldSpacecraftState<T> s,
                            FieldTimeDerivativesEquations<T> adder) {
        }

        @Override
        public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
            {
            return s.getPosition();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {
            this.accelerationDerivativesPosition = (FieldVector3D<DerivativeStructure>) s.getPosition();
            this.accelerationDerivativesVelocity = (FieldVector3D<DerivativeStructure>) s.getPVCoordinates().getVelocity();
            return s.getPosition();
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
            return Stream.empty();
        }

    }

}
