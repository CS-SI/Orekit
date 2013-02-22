/* Copyright 2002-2013 CS Systèmes d'Information
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
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class DragForceTest extends AbstractForceModelTest {

    @Test
    public void testParameterDerivative() throws OrekitException {

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
                                                                      FramesFactory.getITRF2008())),
                              new SphericalSpacecraft(2.5, 1.2, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 4.0e-13);

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
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF2008())),
                              new SphericalSpacecraft(2.5, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           4, 1e3, tolerances[0], 3.0e-4);

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}


