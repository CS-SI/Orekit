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

import java.io.IOException;
import java.text.ParseException;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Unit tests for {@link IntegrableJacobianColumnGenerator}. */
public class IntegrableJacobianColumnGeneratorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
    }

    @Test
    public void testDragParametersDerivatives() throws ParseException, IOException {
        doTestParametersDerivatives(DragSensitive.DRAG_COEFFICIENT, 2.4e-3, OrbitType.values());
    }

    @Test
    public void testMuParametersDerivatives() throws ParseException, IOException {
        // TODO: for an unknown reason, derivatives with respect to central attraction
        // coefficient currently (June 2016) do not work in non-Cartesian orbits
        // we don't even know if the test is badly written or if the library code is wrong ...
        doTestParametersDerivatives(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, 5e-7,
                                    OrbitType.CARTESIAN);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance,
                                             OrbitType... orbitTypes) {

        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        ForceModel drag = new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(), earth),
                                        new IsotropicDrag(2.5, 1.2));

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        Orbit baseOrbit =
                new KeplerianOrbit(7000000.0, 0.01, 0.1, 0.7, 0, 1.2, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   provider.getMu());

        double dt = 900;
        double dP = 1.0;
        for (OrbitType orbitType : orbitTypes) {
            final Orbit initialOrbit = orbitType.convertType(baseOrbit);
            for (PositionAngleType angleType : PositionAngleType.values()) {

                NumericalPropagator propagator =
                                setUpPropagator(initialOrbit, dP, orbitType, angleType, drag, gravityField);
                propagator.setMu(provider.getMu());
                ParameterDriver selected = null;
                for (final ForceModel forceModel : propagator.getAllForceModels()) {
                    for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                        driver.setValue(driver.getReferenceValue(), initialOrbit.getDate());
                        if (driver.getName().equals(parameterName)) {
                            driver.setSelected(true);
                            selected = driver;
                        } else {
                            driver.setSelected(false);
                        }
                    }
                }

                SpacecraftState initialState = new SpacecraftState(initialOrbit);
                propagator.setInitialState(initialState);
                PickUpHandler pickUp = new PickUpHandler(propagator, null, null, selected.getNameSpan(new AbsoluteDate()));
                propagator.setStepHandler(pickUp);
                propagator.propagate(initialState.getDate().shiftedBy(dt));
                RealMatrix dYdP = pickUp.getdYdP();

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
                ParameterDriver selected2 = bound.getDrivers().get(0);
                double p0 = selected2.getReferenceValue();
                double h  = selected2.getScale();
                selected2.setValue(p0 - 4 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 - 3 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 - 2 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 - 1 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sM1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 + 1 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP1h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 + 2 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP2h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 + 3 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP3h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                selected2.setValue(p0 + 4 * h);
                propagator2.resetInitialState(arrayToState(stateToArray(initialState, orbitType, angleType, true),
                                                           orbitType, angleType,
                                                           initialState.getFrame(), initialState.getDate(),
                                                           propagator2.getMu(), // the mu may have been reset above
                                                           initialState.getAttitude()));
                SpacecraftState sP4h = propagator2.propagate(initialOrbit.getDate().shiftedBy(dt));
                fillJacobianColumn(dYdPRef, 0, orbitType, angleType, h,
                                   sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

                for (int i = 0; i < 6; ++i) {
                    Assertions.assertEquals(dYdPRef[i][0], dYdP.getEntry(i, 0), FastMath.abs(dYdPRef[i][0] * tolerance));
                }

            }
        }

    }

    @Test
    public void testJacobianIssue18() {

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
        final AttitudeProvider law = new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta),
                                                                           Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        SpacecraftState initialState =
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
        ParameterDriver selected = maneuver.getParameterDriver("thrust");
        selected.setSelected(true);

        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngleType angleType = PositionAngleType.TRUE;
        propagator.setOrbitType(orbitType);
        StateTransitionMatrixGenerator stmGenerator =
                        new StateTransitionMatrixGenerator("stm",
                                                           propagator.getAllForceModels(),
                                                           propagator.getAttitudeProvider());
        IntegrableJacobianColumnGenerator columnGenerator = new IntegrableJacobianColumnGenerator(stmGenerator, selected.getName());
        propagator.addAdditionalDerivativesProvider(columnGenerator);
        propagator.addAdditionalDerivativesProvider(stmGenerator);

        initialState = stmGenerator.setInitialStateTransitionMatrix(initialState, null, orbitType, angleType);
        initialState = initialState.addAdditionalState(columnGenerator.getName(), new double[6]);
        propagator.setInitialState(initialState);

        final AbsoluteDate finalDate = fireDate.shiftedBy(3800);
        final SpacecraftState finalorb = propagator.propagate(finalDate);
        Assertions.assertEquals(0, finalDate.durationFrom(finalorb.getDate()), 1.0e-11);

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngleType angleType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        boolean withMass = jacobian.length > 6;
        double[] aM4h = stateToArray(sM4h, orbitType, angleType, withMass)[0];
        double[] aM3h = stateToArray(sM3h, orbitType, angleType, withMass)[0];
        double[] aM2h = stateToArray(sM2h, orbitType, angleType, withMass)[0];
        double[] aM1h = stateToArray(sM1h, orbitType, angleType, withMass)[0];
        double[] aP1h = stateToArray(sP1h, orbitType, angleType, withMass)[0];
        double[] aP2h = stateToArray(sP2h, orbitType, angleType, withMass)[0];
        double[] aP3h = stateToArray(sP3h, orbitType, angleType, withMass)[0];
        double[] aP4h = stateToArray(sP4h, orbitType, angleType, withMass)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                  boolean withMass) {
        double[][] array = new double[2][withMass ? 7 : 6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        if (withMass) {
            array[0][6] = state.getMass();
        }
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngleType angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[0][6]);
    }

    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngleType angleType,
                                                ForceModel... models)
        {

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

}
