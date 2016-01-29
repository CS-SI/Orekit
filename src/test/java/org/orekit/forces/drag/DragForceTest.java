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
package org.orekit.forces.drag;


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
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
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class DragForceTest extends AbstractForceModelTest {

    @Test
    public void testParameterDerivativeSphere() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testStateJacobianSphere()
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
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-8);

    }

    @Test
    public void testParameterDerivativeBox() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testStateJacobianBox()
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
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.0e-8);

    }

    @Test
    public void testIssue229() throws OrekitException {
        AbsoluteDate initialDate = new AbsoluteDate(2004, 1, 1, 0, 0, 0., TimeScalesFactory.getUTC());
        Frame frame       = FramesFactory.getEME2000();
        double rpe         = 160.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double rap         = 2000.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double inc         = FastMath.toRadians(0.);
        double aop         = FastMath.toRadians(0.);
        double raan        = FastMath.toRadians(0.);
        double mean        = FastMath.toRadians(180.);
        double mass        = 100.;
        KeplerianOrbit orbit = new KeplerianOrbit(0.5 * (rpe + rap), (rap - rpe) / (rpe + rap),
                                                  inc, aop, raan, mean, PositionAngle.MEAN,
                                                  frame, initialDate, Constants.EIGEN5C_EARTH_MU);

        IsotropicDrag shape = new IsotropicDrag(10., 2.2);

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 2.6e-10, 200000, 26000);

        double[][]          tolerance  = NumericalPropagator.tolerances(0.1, orbit, OrbitType.CARTESIAN);
        AbstractIntegrator  integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerance[0], tolerance[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMu(orbit.getMu());
        propagator.addForceModel(new DragForce(atmosphere, shape));
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(orbit, mass), 6, 0));

        SpacecraftState state = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));

        double delta = 0.1;
        Orbit shifted = new CartesianOrbit(new TimeStampedPVCoordinates(orbit.getDate(),
                                                                        orbit.getPVCoordinates().getPosition().add(new Vector3D(delta, 0, 0)),
                                                                        orbit.getPVCoordinates().getVelocity()),
                                           orbit.getFrame(), orbit.getMu());
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(shifted, mass), 6, 0));
        SpacecraftState newState = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));
        double[] dPVdX = new double[] {
            (newState.getPVCoordinates().getPosition().getX() - state.getPVCoordinates().getPosition().getX()) / delta,
            (newState.getPVCoordinates().getPosition().getY() - state.getPVCoordinates().getPosition().getY()) / delta,
            (newState.getPVCoordinates().getPosition().getZ() - state.getPVCoordinates().getPosition().getZ()) / delta,
            (newState.getPVCoordinates().getVelocity().getX() - state.getPVCoordinates().getVelocity().getX()) / delta,
            (newState.getPVCoordinates().getVelocity().getY() - state.getPVCoordinates().getVelocity().getY()) / delta,
            (newState.getPVCoordinates().getVelocity().getZ() - state.getPVCoordinates().getVelocity().getZ()) / delta,
        };

        double[][] dYdY0 = new double[6][6];
        partials.getMapper().getStateJacobian(state, dYdY0);
        for (int i = 0; i < 6; ++i) {
            Assert.assertEquals(dPVdX[i], dYdY0[i][0], 4.5e-6 * FastMath.abs(dPVdX[i]));
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}


