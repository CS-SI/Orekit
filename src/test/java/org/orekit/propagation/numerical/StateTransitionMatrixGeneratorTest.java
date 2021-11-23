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

import static org.hamcrest.CoreMatchers.is;

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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Unit tests for {@link StateTransitionMatrixGenerator}. */
public class StateTransitionMatrixGeneratorTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
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
        stmGenerator.generate(state);

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
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 0.001;
        for (OrbitType orbitType : OrbitType.values()) {
            for (PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using StateTransitionMatrixGenerator
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                                 propagator.getAllForceModels(),
                                                                                                 propagator.getAttitudeProvider());
                propagator.addIntegrableGenerator(stmGenerator);
                final SpacecraftState initialState =
                                stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                                             MatrixUtils.createRealIdentityMatrix(StateTransitionMatrixGenerator.STATE_DIMENSION),
                                                                             orbitType, angleType);
                propagator.setInitialState(initialState);
                PickUpHandler pickUp = new PickUpHandler(stmGenerator, orbitType, angleType, null, null, null);
                propagator.setStepHandler(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = new double[6][6];
                AbstractIntegratedPropagator propagator2 = setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
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

                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUp.getStm().getEntry(i, j) - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 6.0e-2);

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
            for (PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using StateTransitionMatrixGenerator
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                                 propagator.getAllForceModels(),
                                                                                                 propagator.getAttitudeProvider());
                propagator.addIntegrableGenerator(stmGenerator);
                final SpacecraftState initialState =
                                stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit), null, orbitType, angleType);
                propagator.setInitialState(initialState);
                PickUpHandler pickUp = new PickUpHandler(stmGenerator, orbitType, angleType, null, null, null);
                propagator.setStepHandler(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = new double[6][6];
                AbstractIntegratedPropagator propagator2 = setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
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

                for (int i = 0; i < 6; ++i) {
                    for (int j = 0; j < 6; ++j) {
                        double error = FastMath.abs((pickUp.getStm().getEntry(i, j) - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 1.0e-3);

                    }
                }

            }
        }

    }

    @Test
    public void testAccelerationPartial() {

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
                        new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        ForceModel newton = new NewtonianAttraction(provider.getMu());
        ParameterDriver gmDriver = newton.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
        gmDriver.setSelected(true);
        Orbit initialOrbit =
                        new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());

        NumericalPropagator propagator =
                        setUpPropagator(initialOrbit, 0.001, OrbitType.EQUINOCTIAL, PositionAngle.MEAN,
                                        gravityField, newton);
        StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                         propagator.getAllForceModels(),
                                                                                         propagator.getAttitudeProvider());
        propagator.addIntegrableGenerator(stmGenerator);
        final SpacecraftState initialState =
                        stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                                     MatrixUtils.createRealIdentityMatrix(StateTransitionMatrixGenerator.STATE_DIMENSION),
                                                                     propagator.getOrbitType(), propagator.getPositionAngleType());
        propagator.setInitialState(initialState);
        AbsoluteDate pickupDate = initialOrbit.getDate().shiftedBy(200);
        PickUpHandler pickUp = new PickUpHandler(stmGenerator, propagator.getOrbitType(),
                                                 propagator.getPositionAngleType(), pickupDate,
                                                 gmDriver.getName(), null);
        propagator.setStepHandler(pickUp);
        propagator.propagate(initialState.getDate().shiftedBy(900.0));
        Assert.assertEquals(0.0, pickUp.getState().getDate().durationFrom(pickupDate), 1.0e-10);
        final Vector3D position = pickUp.getState().getPVCoordinates().getPosition();
        final double   r        = position.getNorm();
        Assert.assertEquals(-position.getX() / (r * r * r), pickUp.getAccPartial()[0], 1.0e-15 / (r * r));
        Assert.assertEquals(-position.getY() / (r * r * r), pickUp.getAccPartial()[1], 1.0e-15 / (r * r));
        Assert.assertEquals(-position.getZ() / (r * r * r), pickUp.getAccPartial()[2], 1.0e-15 / (r * r));

    }

    @Test
    public void testNotInitialized() {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                         propagator.getAllForceModels(),
                                                                                         propagator.getAttitudeProvider());
        propagator.addIntegrableGenerator(stmGenerator);
        Assert.assertTrue(stmGenerator.yield(new SpacecraftState(initialOrbit)));
     }

    @Test
    public void testMismatchedDimensions() {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        StateTransitionMatrixGenerator stmGenerator = new StateTransitionMatrixGenerator("stm",
                                                                                         propagator.getAllForceModels(),
                                                                                         propagator.getAttitudeProvider());
        propagator.addIntegrableGenerator(stmGenerator);
        try {
            stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                         MatrixUtils.createRealMatrix(5,  6),
                                                         propagator.getOrbitType(),
                                                         propagator.getPositionAngleType());
        } catch (OrekitException oe) {
            Assert.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2, oe.getSpecifier());
            Assert.assertEquals(5, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals(6, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(6, ((Integer) oe.getParts()[2]).intValue());
            Assert.assertEquals(6, ((Integer) oe.getParts()[3]).intValue());
        }

        try {
            stmGenerator.setInitialStateTransitionMatrix(new SpacecraftState(initialOrbit),
                                                         MatrixUtils.createRealMatrix(6,  5),
                                                         propagator.getOrbitType(),
                                                         propagator.getPositionAngleType());
        } catch (OrekitException oe) {
            Assert.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2, oe.getSpecifier());
            Assert.assertEquals(6, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals(5, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(6, ((Integer) oe.getParts()[2]).intValue());
            Assert.assertEquals(6, ((Integer) oe.getParts()[3]).intValue());
        }

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngle angleType, double h,
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

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngle angleType) {
        double[][] array = new double[2][6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngle angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }

    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngle angleType,
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

    /** Mock {@link ForceModel}. */
    private static class MockForceModel extends AbstractForceModel {

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
            return s.getPVCoordinates().getPosition();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {
            this.accelerationDerivativesPosition = (FieldVector3D<DerivativeStructure>) s.getPVCoordinates().getPosition();
            this.accelerationDerivativesVelocity = (FieldVector3D<DerivativeStructure>) s.getPVCoordinates().getVelocity();
            return s.getPVCoordinates().getPosition();
        }

        @Override
        public Stream<EventDetector> getEventsDetectors() {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
            return Stream.empty();
        }

    }

}