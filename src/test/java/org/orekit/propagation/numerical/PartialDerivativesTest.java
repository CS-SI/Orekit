/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class PartialDerivativesTest {

    @Test
    public void testPropagationTypesElliptical() throws OrekitException, ParseException, IOException {

        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double mu = provider.getMu();
        ForceModel gravityField =
            new CunninghamAttractionModel(FramesFactory.getITRF2005(), 6378136.460, mu,
                                          provider.getC(5, 5, true), provider.getS(5, 5, true));
        SpacecraftState initialState =
            new SpacecraftState(new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, mu));

        double dt = 3200;
        double dP = 0.001;
        for (OrbitType orbitType : OrbitType.values()) {
            for (PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using PartialDerivatives
                NumericalPropagator propagator =
                    setUpPropagator(initialState, dP, orbitType, angleType, gravityField);
                PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
                partials.setInitialJacobians(initialState, 6, 0);
                final JacobiansMapper mapper = partials.getMapper();
                PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                double[][] dYdY0 = pickUp.getdYdY0();

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = new double[6][6];
                AbstractIntegratedPropagator propagator2 = setUpPropagator(initialState, dP, orbitType, angleType, gravityField);
                double[] steps = NumericalPropagator.tolerances(1000000 * dP, initialState.getOrbit(), orbitType)[0];
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
                        double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 3.0e-4);

                    }
                }

            }
        }

    }

    @Test
    public void testPropagationTypesHyperbolic() throws OrekitException, ParseException, IOException {

        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double mu = provider.getMu();
        ForceModel gravityField =
            new CunninghamAttractionModel(FramesFactory.getITRF2005(), 6378136.460, mu,
                                          provider.getC(5, 5, true), provider.getS(5, 5, true));
        SpacecraftState initialState =
            new SpacecraftState(new KeplerianOrbit(new PVCoordinates(new Vector3D(-1551946.0, 708899.0, 6788204.0),
                                                                     new Vector3D(-9875.0, -3941.0, -1845.0)),
                                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, mu));

        double dt = 3200;
        double dP = 0.001;
        for (OrbitType orbitType : new OrbitType[] { OrbitType.KEPLERIAN, OrbitType.CARTESIAN }) {
            for (PositionAngle angleType : PositionAngle.values()) {

                // compute state Jacobian using PartialDerivatives
                NumericalPropagator propagator =
                    setUpPropagator(initialState, dP, orbitType, angleType, gravityField);
                PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
                partials.setInitialJacobians(initialState, 6, 0);
                final JacobiansMapper mapper = partials.getMapper();
                PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                double[][] dYdY0 = pickUp.getdYdY0();

                // compute reference state Jacobian using finite differences
                double[][] dYdY0Ref = new double[6][6];
                AbstractIntegratedPropagator propagator2 = setUpPropagator(initialState, dP, orbitType, angleType, gravityField);
                double[] steps = NumericalPropagator.tolerances(1000000 * dP, initialState.getOrbit(), orbitType)[0];
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
                        double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 9.0e-4);

                    }
                }

            }
        }

    }

    @Test
    public void testJacobianIssue18() throws OrekitException {

        // Body mu
        final double mu = 3.9860047e14;

        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);

        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);




        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);



        propagator.setOrbitType(OrbitType.CARTESIAN);
        PartialDerivativesEquations PDE = new PartialDerivativesEquations("derivatives", propagator);
        PDE.selectParamAndStep("thrust", Double.NaN);
        PDE.setInitialJacobians(initialState, 7, 1);

        final AbsoluteDate finalDate = fireDate.shiftedBy(3800);
        final SpacecraftState finalorb = propagator.propagate(finalDate);
        Assert.assertEquals(0, finalDate.durationFrom(finalorb.getDate()), 1.0e-11);

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngle angleType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        boolean withMass = jacobian.length > 6;
        double[] aM4h = stateToArray(sM4h, orbitType, angleType, withMass);
        double[] aM3h = stateToArray(sM3h, orbitType, angleType, withMass);
        double[] aM2h = stateToArray(sM2h, orbitType, angleType, withMass);
        double[] aM1h = stateToArray(sM1h, orbitType, angleType, withMass);
        double[] aP1h = stateToArray(sP1h, orbitType, angleType, withMass);
        double[] aP2h = stateToArray(sP2h, orbitType, angleType, withMass);
        double[] aP3h = stateToArray(sP3h, orbitType, angleType, withMass);
        double[] aP4h = stateToArray(sP4h, orbitType, angleType, withMass);
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                       double delta, int column) {

        double[] array = stateToArray(state, orbitType, angleType, true);
        array[column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                  boolean withMass) {
        double[] array = new double[withMass ? 7 : 6];
        switch (orbitType) {
        case CARTESIAN : {
            CartesianOrbit cart = (CartesianOrbit) orbitType.convertType(state.getOrbit());
            array[0] = cart.getPVCoordinates().getPosition().getX();
            array[1] = cart.getPVCoordinates().getPosition().getY();
            array[2] = cart.getPVCoordinates().getPosition().getZ();
            array[3] = cart.getPVCoordinates().getVelocity().getX();
            array[4] = cart.getPVCoordinates().getVelocity().getY();
            array[5] = cart.getPVCoordinates().getVelocity().getZ();
        }
        break;
        case CIRCULAR : {
            CircularOrbit circ = (CircularOrbit) orbitType.convertType(state.getOrbit());
            array[0] = circ.getA();
            array[1] = circ.getCircularEx();
            array[2] = circ.getCircularEy();
            array[3] = circ.getI();
            array[4] = circ.getRightAscensionOfAscendingNode();
            array[5] = circ.getAlpha(angleType);
        }
        break;
        case EQUINOCTIAL : {
            EquinoctialOrbit equ = (EquinoctialOrbit) orbitType.convertType(state.getOrbit());
            array[0] = equ.getA();
            array[1] = equ.getEquinoctialEx();
            array[2] = equ.getEquinoctialEy();
            array[3] = equ.getHx();
            array[4] = equ.getHy();
            array[5] = equ.getL(angleType);
        }
        break;
        case KEPLERIAN : {
            KeplerianOrbit kep = (KeplerianOrbit) orbitType.convertType(state.getOrbit());
            array[0] = kep.getA();
            array[1] = kep.getE();
            array[2] = kep.getI();
            array[3] = kep.getPerigeeArgument();
            array[4] = kep.getRightAscensionOfAscendingNode();
            array[5] = kep.getAnomaly(angleType);
        }
        break;
        }

        if (withMass) {
            array[6] = state.getMass();
        }

        return array;

    }

    private SpacecraftState arrayToState(double[] array, OrbitType orbitType, PositionAngle angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = null;
        switch (orbitType) {
        case CARTESIAN :
            orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(array[0], array[1], array[2]),
                                                         new Vector3D(array[3], array[4], array[5])),
                                       frame, date, mu);
            break;
        case CIRCULAR :
            orbit = new CircularOrbit(array[0], array[1], array[2], array[3], array[4], array[5],
                                      angleType, frame, date, mu);
            break;
        case EQUINOCTIAL :
            orbit = new EquinoctialOrbit(array[0], array[1], array[2], array[3], array[4], array[5],
                                         angleType, frame, date, mu);
            break;
        case KEPLERIAN :
            orbit = new KeplerianOrbit(array[0], array[1], array[2], array[3], array[4], array[5],
                                       angleType, frame, date, mu);
            break;
        }

        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[6]);

    }

    private NumericalPropagator setUpPropagator(SpacecraftState state, double dP,
                                                OrbitType orbitType, PositionAngle angleType,
                                                ForceModel ... models)
        throws OrekitException {

        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = NumericalPropagator.tolerances(dP, state.getOrbit(), orbitType);
        NumericalPropagator propagator =
            new NumericalPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(angleType);
        for (ForceModel model : models) {
            propagator.addForceModel(model);
        }
        propagator.setInitialState(state);
        return propagator;
    }

    private static class PickUpHandler implements OrekitStepHandler {

        private static final long serialVersionUID = 8040284226089555027L;
        private final JacobiansMapper mapper;
        private final AbsoluteDate pickUpDate;
        private final double[][] dYdY0;
        private final double[][] dYdP;

        public PickUpHandler(JacobiansMapper mapper, AbsoluteDate pickUpDate) {
            this.mapper = mapper;
            this.pickUpDate = pickUpDate;
            dYdY0 = new double[mapper.getStateDimension()][mapper.getStateDimension()];
            dYdP = new double[mapper.getStateDimension()][mapper.getParameters()];
        }

        public double[][] getdYdY0() {
            return dYdY0;
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
        }

        public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
        throws PropagationException {
            try {
                if (pickUpDate == null) {
                    // we want to pick up the Jacobians at the end of last step
                    if (isLast) {
                        interpolator.setInterpolatedDate(interpolator.getCurrentDate());
                    }
                } else {
                    // we want to pick up some intermediate Jacobians
                    double dt0 = pickUpDate.durationFrom(interpolator.getPreviousDate());
                    double dt1 = pickUpDate.durationFrom(interpolator.getCurrentDate());
                    if (dt0 * dt1 > 0) {
                        // the current step does not cover the pickup date
                        return;
                    } else {
                        interpolator.setInterpolatedDate(pickUpDate);
                    }
                }

                SpacecraftState state = interpolator.getInterpolatedState();
                double[] p = interpolator.getInterpolatedAdditionalState(mapper.getName());
                mapper.getStateJacobian(state, p, dYdY0);
                mapper.getParametersJacobian(state, p, dYdP);

            } catch (PropagationException pe) {
                throw pe;
            } catch (OrekitException oe) {
                throw new PropagationException(oe);
            }
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}

