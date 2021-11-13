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
package org.orekit.forces.maneuvers;


import java.util.Locale;

import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class ManeuverTest {

    @Test
    public void testDerivativeWrtStartTime() {
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
                               FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        final OrbitType     orbitType = OrbitType.KEPLERIAN;
        final PositionAngle angleType = PositionAngle.TRUE;
        double[][] tol = NumericalPropagator.tolerances(0.001, orbit, orbitType);
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        // compute gradient with respect to firing date, keeping stop date constant
        // finite differences method
        final AbsoluteDate firing = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                     new TimeComponents(04, 15, 34.080),
                                                     TimeScalesFactory.getUTC());
        final PropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM");
        final AbsoluteDate finalDate = firing.shiftedBy(3800);
        final UnivariateDifferentiableVectorFunction function =
                        new FiniteDifferencesDifferentiator(8, 1.0).differentiate((UnivariateVectorFunction) dt -> {
                            final double[] array = new double[6];
                            integrator.setInitialStepSize(60);
                            final NumericalPropagator propagator = new NumericalPropagator(integrator);
                            propagator.setOrbitType(orbitType);
                            propagator.setPositionAngleType(angleType);
                            propagator.setAttitudeProvider(law);
                            propagator.addForceModel(new Maneuver(null,
                                                                  new DateBasedManeuverTriggers(firing.shiftedBy(dt), duration - dt),
                                                                  propulsionModel));
                            propagator.setInitialState(initialState);
                            orbitType.mapOrbitToArray(propagator.propagate(finalDate).getOrbit(), angleType, array, null);
                            return array;
                        });
        final UnivariateDerivative1[] reference = function.value(new UnivariateDerivative1(0.0, 1.0));

        // compute gradient with respect to firing date, keeping stop date constant
        // direct computation method
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(angleType);
        propagator.setAttitudeProvider(law);
        DateBasedManeuverTriggers trigger = new DateBasedManeuverTriggers("MAN_0", firing, duration);
        trigger.getFiringIntervalDetector().getStartDriver().setSelected(true);
        propagator.addForceModel(new Maneuver(null, trigger, propulsionModel));
        PartialDerivativesEquations pde = new PartialDerivativesEquations("pde", propagator);
        propagator.setInitialState(pde.setInitialJacobians(initialState));
        SpacecraftState finalState = propagator.propagate(finalDate);
        final double[] orbitArray = new double[6];
        orbitType.mapOrbitToArray(propagator.propagate(finalDate).getOrbit(), angleType, orbitArray, null);
        double[][] jacobian = new double[6][1];
        pde.getMapper().getParametersJacobian(finalState, jacobian);

        for (int n = 0; n < reference.length; ++n) {
            final double f0 = reference[n].getValue();
            final double f1 = reference[n].getFirstDerivative();
            System.out.format(Locale.US, "o[%d] = %.6f %c %.9f Δt / %.6f %c %.9f Δt%n",
                              n,
                              f0, f1 < 0 ? '-' : '+', FastMath.abs(f1),
                              orbitArray[n], jacobian[n][0] < 0 ? '-' : '+', FastMath.abs(jacobian[n][0]));
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
