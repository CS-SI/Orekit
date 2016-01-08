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
package org.orekit.forces.gravity;


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class ThirdBodyAttractionTest extends AbstractForceModelTest {

    private double mu;

    @Test(expected= OrekitException.class)
    public void testSunContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                           FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3),
                                           0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        double period = 2 * FastMath.PI * orbit.getA() * FastMath.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));

        // set up step handler to perform checks
        calc.setMasterMode(FastMath.floor(period), new ReferenceChecker(date) {
            protected double hXRef(double t) {
                return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 *
                FastMath.cos(3.9820426e-7*t) - 7.59983e-5 * FastMath.sin(3.9820426e-7*t);
            }
            protected double hYRef(double t) {
                return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 *
                FastMath.cos(3.9820426e-7*t) + 17.6083e-5 * FastMath.sin(3.9820426e-7*t);
            }
        });
        AbsoluteDate finalDate = date.shiftedBy(365 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    @Test
    public void testMoonContrib() throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit =
            new EquinoctialOrbit(42164000,10e-3,10e-3,
                                      FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                      FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3),
                                      0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        double period = 2 * FastMath.PI * orbit.getA() * FastMath.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        NumericalPropagator calc =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0, 1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // set up step handler to perform checks
        calc.setMasterMode(FastMath.floor(period), new ReferenceChecker(date) {
            protected double hXRef(double t) {
                return  -0.000906173 + 1.93933e-11 * t +
                         1.0856e-06  * FastMath.cos(5.30637e-05 * t) -
                         1.22574e-06 * FastMath.sin(5.30637e-05 * t);
            }
            protected double hYRef(double t) {
                return 0.00151973 + 1.88991e-10 * t -
                       1.25972e-06  * FastMath.cos(5.30637e-05 * t) -
                       1.00581e-06 * FastMath.sin(5.30637e-05 * t);
            }
        });
        AbsoluteDate finalDate = date.shiftedBy(31 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    private static abstract class ReferenceChecker implements OrekitFixedStepHandler {

        private final AbsoluteDate reference;

        protected ReferenceChecker(AbsoluteDate reference) {
            this.reference = reference;
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            double t = currentState.getDate().durationFrom(reference);
            Assert.assertEquals(hXRef(t), currentState.getHx(), 1e-4);
            Assert.assertEquals(hYRef(t), currentState.getHy(), 1e-4);
        }

        protected abstract double hXRef(double t);

        protected abstract double hYRef(double t);

    }

    @Test
    public void testParameterDerivative() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        final String name = moon.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX;
        checkParameterDerivative(state, forceModel, name, 1.0, 7.0e-15);

    }

    @Test
    public void testStateJacobian()
        throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(moon);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e4, tolerances[0], 2.0e-9);

    }

    @Before
    public void setUp() {
        mu = 3.986e14;
        Utils.setDataRoot("regular-data");
    }

}
