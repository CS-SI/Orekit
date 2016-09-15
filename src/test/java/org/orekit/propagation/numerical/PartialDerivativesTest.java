/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.atmosphere.HarrisPriester;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
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
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class PartialDerivativesTest {

    @Test
    public void testDragParametersDerivatives() throws OrekitException, ParseException, IOException {
        doTestParametersDerivatives(DragSensitive.DRAG_COEFFICIENT, 2.4e-3, OrbitType.values());
    }

    @Test
    public void testMuParametersDerivatives() throws OrekitException, ParseException, IOException {
        // TODO: for an unknown reason, derivatives with respect to central attraction
        // coefficient currently (June 2016) do not work in non-Cartesian orbits
        // we don't even know if the test is badly written or if the library code is wrong ...
        doTestParametersDerivatives(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, 5e-7,
                                    OrbitType.CARTESIAN);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance,
                                             OrbitType ... orbitTypes)
        throws OrekitException {

        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        ForceModel drag = new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(), earth),
                                        new IsotropicDrag(2.5, 1.2));

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        Orbit baseOrbit =
                new KeplerianOrbit(7000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 1.0;
        for (OrbitType orbitType : orbitTypes) {
            final Orbit initialOrbit = orbitType.convertType(baseOrbit);
            for (PositionAngle angleType : PositionAngle.values()) {

                NumericalPropagator propagator =
                                setUpPropagator(initialOrbit, dP, orbitType, angleType, drag, gravityField);
                propagator.setMu(provider.getMu());
                for (final ForceModel forceModel : propagator.getAllForceModels()) {
                    for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                        driver.setValue(driver.getReferenceValue());
                        driver.setSelected(driver.getName().equals(parameterName));
                    }
                }

                PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
                final SpacecraftState initialState =
                        partials.setInitialJacobians(new SpacecraftState(initialOrbit), 6);
                propagator.setInitialState(initialState);
                final JacobiansMapper mapper = partials.getMapper();
                PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                double[][] dYdP = pickUp.getdYdP();

                // compute reference Jacobian using finite differences
                double[][] dYdPRef = new double[6][1];
                NumericalPropagator propagator2 = setUpPropagator(initialOrbit, dP, orbitType, angleType,
                                                                  drag, gravityField);
                propagator2.setMu(provider.getMu());
                ParameterDriversList bound = new ParameterDriversList();
                for (final ForceModel forceModel : propagator2.getAllForceModels()) {
                    for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                        if (driver.getName().equals(parameterName)) {
                            driver.setSelected(true);
                            bound.add(driver);
                        } else {
                            driver.setSelected(false);
                        }
                    }
                }
                ParameterDriver selected = bound.getDrivers().get(0);
                double p0 = selected.getReferenceValue();
                double h  = selected.getScale();
                selected.setValue(p0 - 4 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 - 3 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 - 2 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 - 1 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 + 1 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 + 2 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 + 3 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected.setValue(p0 + 4 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                fillJacobianColumn(dYdPRef, 0, orbitType, angleType, h,
                                   sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

                for (int i = 0; i < 6; ++i) {
                    Assert.assertEquals(dYdPRef[i][0], dYdP[i][0], FastMath.abs(dYdPRef[i][0] * tolerance));
                }

            }
        }

    }

    @Test
    public void testPropagationTypesElliptical() throws OrekitException {

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

                // compute state Jacobian using PartialDerivatives
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
                final SpacecraftState initialState =
                        partials.setInitialJacobians(new SpacecraftState(initialOrbit), 6);
                propagator.setInitialState(initialState);
                final JacobiansMapper mapper = partials.getMapper();
                PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                double[][] dYdY0 = pickUp.getdYdY0();

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
                        double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 6.0e-2);

                    }
                }

            }
        }

    }

    @Test
    public void testPropagationTypesHyperbolic() throws OrekitException {

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

                // compute state Jacobian using PartialDerivatives
                NumericalPropagator propagator =
                    setUpPropagator(initialOrbit, dP, orbitType, angleType, gravityField);
                PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
                final SpacecraftState initialState =
                        partials.setInitialJacobians(new SpacecraftState(initialOrbit), 6);
                propagator.setInitialState(initialState);
                final JacobiansMapper mapper = partials.getMapper();
                PickUpHandler pickUp = new PickUpHandler(mapper, null);
                propagator.setMasterMode(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                double[][] dYdY0 = pickUp.getdYdY0();

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
                        double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / dYdY0Ref[i][j]);
                        Assert.assertEquals(0, error, 1.0e-3);

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

        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        maneuver.getParameterDriver("thrust").setSelected(true);

        propagator.setOrbitType(OrbitType.CARTESIAN);
        PartialDerivativesEquations PDE = new PartialDerivativesEquations("derivatives", propagator);
        Assert.assertEquals(1, PDE.getSelectedParameters().getNbParams());
        propagator.setInitialState(PDE.setInitialJacobians(initialState, 7));

        final AbsoluteDate finalDate = fireDate.shiftedBy(3800);
        final SpacecraftState finalorb = propagator.propagate(finalDate);
        Assert.assertEquals(0, finalDate.durationFrom(finalorb.getDate()), 1.0e-11);

    }

    @Test(expected=OrekitException.class)
    public void testNotInitialized() throws OrekitException {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        new PartialDerivativesEquations("partials", propagator).getMapper();
     }

    @Test(expected=OrekitException.class)
    public void testTooSmallDimension() throws OrekitException {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(new SpacecraftState(initialOrbit),
                                     new double[5][6], new double[6][2]);
     }

    @Test(expected=OrekitException.class)
    public void testTooLargeDimension() throws OrekitException {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(new SpacecraftState(initialOrbit),
                                     new double[8][6], new double[6][2]);
     }

    @Test(expected=OrekitException.class)
    public void testMismatchedDimensions() throws OrekitException {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(new SpacecraftState(initialOrbit),
                                     new double[6][6], new double[7][2]);
     }

    @Test
    public void testWrongParametersDimension() throws OrekitException {
        Orbit initialOrbit =
                new KeplerianOrbit(8000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);

        double dP = 0.001;
        ForceModel sunAttraction  = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        sunAttraction.getParameterDriver("Sun attraction coefficient").setSelected(true);
        ForceModel moonAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
        NumericalPropagator propagator =
                setUpPropagator(initialOrbit, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE,
                                sunAttraction, moonAttraction);
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        try {
            partials.setInitialJacobians(new SpacecraftState(initialOrbit),
                                         new double[6][6], new double[6][3]);
            partials.computeDerivatives(new SpacecraftState(initialOrbit), new double[6]);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                                oe.getSpecifier());
        }
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
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array);
        if (withMass) {
            array[6] = state.getMass();
        }
        return array;
    }

    private SpacecraftState arrayToState(double[] array, OrbitType orbitType, PositionAngle angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array, angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[6]);
    }

    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngle angleType,
                                                ForceModel ... models)
        throws OrekitException {

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

    private static class PickUpHandler implements OrekitStepHandler {

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

        public double[][] getdYdP() {
            return dYdP;
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
        }

        public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
            throws OrekitException {
            final SpacecraftState interpolated;
            if (pickUpDate == null) {
                // we want to pick up the Jacobians at the end of last step
                if (isLast) {
                    interpolated = interpolator.getCurrentState();
                } else {
                    return;
                }
            } else {
                // we want to pick up some intermediate Jacobians
                double dt0 = pickUpDate.durationFrom(interpolator.getPreviousState().getDate());
                double dt1 = pickUpDate.durationFrom(interpolator.getCurrentState().getDate());
                if (dt0 * dt1 > 0) {
                    // the current step does not cover the pickup date
                    return;
                } else {
                    interpolated = interpolator.getInterpolatedState(pickUpDate);
                }
            }

            Assert.assertEquals(1, interpolated.getAdditionalStates().size());
            Assert.assertTrue(interpolated.getAdditionalStates().containsKey(mapper.getName()));
            mapper.getStateJacobian(interpolated, dYdY0);
            mapper.getParametersJacobian(interpolated, dYdP);

        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

}

