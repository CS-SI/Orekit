/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.forces;


import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class BoxAndSolarArraySpacecraftTest {

    @Test
    public void testParametersDrivers() throws OrekitException {
        
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft.Facet[] facets = new BoxAndSolarArraySpacecraft.Facet[] {
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_I, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_I,  3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_J, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_J,  3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_K, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_K,  3.0)
        };

        BoxAndSolarArraySpacecraft s1 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.8, 0.1);
        Assert.assertEquals(1, s1.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s1.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s1.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(2, s1.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s1.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s1.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s1.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s1.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s2 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.4, 0.8, 0.1);
        Assert.assertEquals(2, s2.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s2.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s2.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(DragSensitive.LIFT_RATIO, s2.getDragParametersDrivers()[1].getName());
        Assert.assertEquals(0.4, s2.getDragParametersDrivers()[1].getValue(), 1.0e-15);
        Assert.assertEquals(2, s2.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s2.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s2.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s2.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s2.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s3 =
                        new BoxAndSolarArraySpacecraft(facets, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.8, 0.1);
        Assert.assertEquals(1, s3.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s3.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s3.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(2, s3.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s3.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s3.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s3.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s3.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s4 =
                        new BoxAndSolarArraySpacecraft(facets, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.4, 0.8, 0.1);
        Assert.assertEquals(2, s4.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s4.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s4.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(DragSensitive.LIFT_RATIO, s4.getDragParametersDrivers()[1].getName());
        Assert.assertEquals(0.4, s4.getDragParametersDrivers()[1].getValue(), 1.0e-15);
        Assert.assertEquals(2, s4.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s4.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s4.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s4.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s4.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s5 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                                       AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, 7.292e-5,
                                                       2.0, 0.8, 0.1);
        Assert.assertEquals(1, s5.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s5.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s5.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(2, s5.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s5.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s5.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s5.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s5.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s6 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                                       AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, 7.292e-5,
                                                       2.0, 0.4, 0.8, 0.1);
        Assert.assertEquals(2, s6.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s6.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s6.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(DragSensitive.LIFT_RATIO, s6.getDragParametersDrivers()[1].getName());
        Assert.assertEquals(0.4, s6.getDragParametersDrivers()[1].getValue(), 1.0e-15);
        Assert.assertEquals(2, s6.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s6.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s6.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s6.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s6.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s7 =
                        new BoxAndSolarArraySpacecraft(facets, sun, 20.0, Vector3D.PLUS_J,
                                                       AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, 7.292e-5,
                                                       2.0, 0.8, 0.1);
        Assert.assertEquals(1, s7.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s7.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s7.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(2, s7.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s7.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s7.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s7.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s7.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s8 =
                        new BoxAndSolarArraySpacecraft(facets, sun, 20.0, Vector3D.PLUS_J,
                                                       AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, 7.292e-5,
                                                       2.0, 0.4, 0.8, 0.1);
        Assert.assertEquals(2, s8.getDragParametersDrivers().length);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, s8.getDragParametersDrivers()[0].getName());
        Assert.assertEquals(2.0, s8.getDragParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(DragSensitive.LIFT_RATIO, s8.getDragParametersDrivers()[1].getName());
        Assert.assertEquals(0.4, s8.getDragParametersDrivers()[1].getValue(), 1.0e-15);
        Assert.assertEquals(2, s8.getRadiationParametersDrivers().length);
        Assert.assertEquals(RadiationSensitive.ABSORPTION_COEFFICIENT, s8.getRadiationParametersDrivers()[0].getName());
        Assert.assertEquals(0.8, s8.getRadiationParametersDrivers()[0].getValue(), 1.0e-15);
        Assert.assertEquals(RadiationSensitive.REFLECTION_COEFFICIENT, s8.getRadiationParametersDrivers()[1].getName());
        Assert.assertEquals(0.1, s8.getRadiationParametersDrivers()[1].getValue(), 1.0e-15);

    }

    @Test
    public void testBestPointing() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 0.0, 0.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
            Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

            Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                     state.getPVCoordinates().getPosition(),
                                     state.getAttitude().getRotation());
            Assert.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
            double misAlignment = Vector3D.angle(sunSat, n);
            Assert.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
    public void testCorrectFixedRate() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                           initialDate,
                                           new Vector3D(0.46565509814462996, 0.0,  0.884966287251619),
                                           propagator.getInitialState().getKeplerianMeanMotion(),
                                           0.0, 0.0, 0.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
            Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

            Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                     state.getPVCoordinates().getPosition(),
                                     state.getAttitude().getRotation());
            Assert.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
            double misAlignment = Vector3D.angle(sunSat, n);
            Assert.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
    public void testTooSlowFixedRate() throws OrekitException {

            AbsoluteDate initialDate = propagator.getInitialState().getDate();
            CelestialBody sun = CelestialBodyFactory.getSun();
            BoxAndSolarArraySpacecraft s =
                new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                               initialDate,
                                               new Vector3D(0.46565509814462996, 0.0,  0.884966287251619),
                                               0.1 * propagator.getInitialState().getKeplerianMeanMotion(),
                                               0.0, 0.0, 0.0);

            double maxDelta = 0;
            for (double dt = 0; dt < 4000; dt += 60) {

                SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

                Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
                Vector3D momentum = state.getPVCoordinates().getMomentum();
                double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
                Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

                Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                         state.getPVCoordinates().getPosition(),
                                         state.getAttitude().getRotation());
                Assert.assertEquals(0.0, n.getY(), 1.0e-10);

                // normal misalignment should become very large as solar array rotation is plain wrong
                Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
                double misAlignment = Vector3D.angle(sunSat, n);
                maxDelta = FastMath.max(maxDelta, FastMath.abs(sunElevation - misAlignment));

            }
            Assert.assertTrue(FastMath.toDegrees(maxDelta) > 120.0);

    }

    @Test
    public void testNoLiftWithoutReflection() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 0.0, 1.0, 0.0);

        Vector3D earthRot = new Vector3D(0.0, 0.0, 7.292115e-4);
        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // simple Earth fixed atmosphere
            Vector3D p = state.getPVCoordinates().getPosition();
            Vector3D v = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, p);
            Vector3D relativeVelocity = vAtm.subtract(v);

            Vector3D drag = s.dragAcceleration(state.getDate(), state.getFrame(),
                                               state.getPVCoordinates().getPosition(),
                                               state.getAttitude().getRotation(),
                                               state.getMass(), 0.001, relativeVelocity,
                                               getDragParameters(s));
            Assert.assertEquals(0.0, Vector3D.angle(relativeVelocity, drag), 1.0e-15);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D radiation = s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                                 state.getPVCoordinates().getPosition(),
                                                                 state.getAttitude().getRotation(),
                                                                 state.getMass(), flux,
                                                                 getRadiationParameters(s));
            Assert.assertEquals(0.0, Vector3D.angle(flux, radiation), 1.0e-9);

        }

    }

    @Test
    public void testOnlyLiftWithoutReflection() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 1.0, 1.0, 0.0);

        Vector3D earthRot = new Vector3D(0.0, 0.0, 7.292115e-4);
        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // simple Earth fixed atmosphere
            Vector3D p = state.getPVCoordinates().getPosition();
            Vector3D v = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, p);
            Vector3D relativeVelocity = vAtm.subtract(v);

            Vector3D drag = s.dragAcceleration(state.getDate(), state.getFrame(),
                                               state.getPVCoordinates().getPosition(),
                                               state.getAttitude().getRotation(),
                                               state.getMass(), 0.001, relativeVelocity,
                                               getDragParameters(s));
            Assert.assertTrue(Vector3D.angle(relativeVelocity, drag) > 0.167);
            Assert.assertTrue(Vector3D.angle(relativeVelocity, drag) < 0.736);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D radiation = s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                                 state.getPVCoordinates().getPosition(),
                                                                 state.getAttitude().getRotation(),
                                                                 state.getMass(), flux,
                                                                 getRadiationParameters(s));
            Assert.assertEquals(0.0, Vector3D.angle(flux, radiation), 1.0e-9);

        }

    }

    @Test
    public void testLiftVsNoLift()
        throws OrekitException, NoSuchFieldException, SecurityException,
               IllegalArgumentException, IllegalAccessException {

        CelestialBody sun = CelestialBodyFactory.getSun();

        // older implementation did not consider lift, so it really worked
        // only for symmetrical shapes. For testing purposes, we will use a
        // basic cubic shape without solar arrays and a relative atmosphere
        // velocity either *exactly* facing a side or *exactly* along a main diagonal
        BoxAndSolarArraySpacecraft.Facet[] facets = new BoxAndSolarArraySpacecraft.Facet[] {
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_I, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_I,  3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_J, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_J,  3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.MINUS_K, 3.0),
            new BoxAndSolarArraySpacecraft.Facet(Vector3D.PLUS_K,  3.0)
        };
        BoxAndSolarArraySpacecraft cube =
                        new BoxAndSolarArraySpacecraft(facets, sun, 0.0, Vector3D.PLUS_J, 1.0, 1.0, 1.0, 0.0);

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        Vector3D position = new Vector3D(1234567.8, 9876543.21, 121212.3434);
        double mass = 1000.0;
        double density = 0.001;
        Rotation rotation = Rotation.IDENTITY;

        // head-on, there acceleration with lift should be twice acceleration without lift
        Vector3D headOnVelocity = new Vector3D(2000, 0.0, 0.0);
        Vector3D newHeadOnDrag  = cube.dragAcceleration(date, frame, position, rotation, mass, density, headOnVelocity,
                                                        getDragParameters(cube));
        Vector3D oldHeadOnDrag  = oldDragAcceleration(cube, date, frame, position, rotation, mass, density, headOnVelocity);
        Assert.assertThat(newHeadOnDrag, OrekitMatchers.vectorCloseTo(oldHeadOnDrag.scalarMultiply(2), 1));

        // on an angle, the no lift implementation applies drag to the velocity direction
        // instead of to the facet normal direction. In the symmetrical case, this implies
        // it applied a single cos(θ) coefficient (projected surface reduction) instead
        // of using cos²(θ) (projected surface reduction *and* normal component projection)
        // and since molecule is reflected backward with the same velocity, this implies a
        // factor 2 in linear momentum differences
        Vector3D diagonalVelocity = new Vector3D(2000, 2000, 2000);
        Vector3D newDiagDrag= cube.dragAcceleration(date, frame, position, rotation, mass, density, diagonalVelocity,
                                                    getDragParameters(cube));
        Vector3D oldDiagDrag = oldDragAcceleration(cube, date, frame, position, rotation, mass, density, diagonalVelocity);
        double oldMissingCoeff = 2.0 / FastMath.sqrt(3.0);
        Vector3D fixedOldDrag = new Vector3D(oldMissingCoeff, oldDiagDrag);
        Assert.assertThat(newDiagDrag, OrekitMatchers.vectorCloseTo(fixedOldDrag, 1));

    }

    // this is a slightly adapted version of the pre-9.0 implementation
    // (changes are only related to retrieve the fields using reflection)
    // Beware that this implementation is WRONG
    private Vector3D oldDragAcceleration(final BoxAndSolarArraySpacecraft bsa,
                                         final AbsoluteDate date, final Frame frame, final Vector3D position,
                                         final Rotation rotation, final double mass,
                                         final double density, final Vector3D relativeVelocity)
         throws OrekitException, IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, SecurityException {

        java.lang.reflect.Field facetsField = BoxAndSolarArraySpacecraft.class.getDeclaredField("facets");
        facetsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<BoxAndSolarArraySpacecraft.Facet> facets = (List<BoxAndSolarArraySpacecraft.Facet>) facetsField.get(bsa);
        
        java.lang.reflect.Field saAreaField = BoxAndSolarArraySpacecraft.class.getDeclaredField("solarArrayArea");
        saAreaField.setAccessible(true);
        final double solarArrayArea = (Double) saAreaField.get(bsa);
        
        final double dragCoeff = bsa.getDragParametersDrivers()[0].getValue();
        
        // relative velocity in spacecraft frame
        final Vector3D v = rotation.applyTo(relativeVelocity);

        // solar array contribution
        final Vector3D solarArrayFacet = new Vector3D(solarArrayArea, bsa.getNormal(date, frame, position, rotation));
        double sv = FastMath.abs(Vector3D.dotProduct(solarArrayFacet, v));

        // body facets contribution
        for (final BoxAndSolarArraySpacecraft.Facet facet : facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= facet.getArea() * dot;
            }
        }

        return new Vector3D(sv * density * dragCoeff / (2.0 * mass), relativeVelocity);

    }

    @Test
    public void testPlaneSpecularReflection() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 0.0, 1.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration = s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                                    state.getPVCoordinates().getPosition(),
                                                                    state.getAttitude().getRotation(),
                                                                    state.getMass(), flux,
                                                                    getRadiationParameters(s));
            Vector3D normal = state.getAttitude().getRotation().applyInverseTo(s.getNormal(state.getDate(), state.getFrame(),
                                                                                           state.getPVCoordinates().getPosition(),
                                                                                           state.getAttitude().getRotation()));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assert.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to solar array normal as there is only specular reflection
            Assert.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, normal)), 1.0e-3);

        }

    }

    @Test
    public void testPlaneAbsorption() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration =
                    s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                    state.getPVCoordinates().getPosition(),
                                                    state.getAttitude().getRotation(),
                                                    state.getMass(), flux,
                                                    getRadiationParameters(s));
            Vector3D normal = state.getAttitude().getRotation().applyInverseTo(s.getNormal(state.getDate(), state.getFrame(),
                                                                                           state.getPVCoordinates().getPosition(),
                                                                                           state.getAttitude().getRotation()));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assert.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to Sun direction as there is only absorption
            Assert.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, sunDirection)), 1.0e-3);

        }

    }

    @Test
    public void testWrongParameterDrag() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        try {
            s.dragAcceleration(state.getDate(), state.getFrame(),
                               state.getPVCoordinates().getPosition(),
                               state.getAttitude().getRotation(),
                               state.getMass(), 1.0e-6, Vector3D.PLUS_I,
                               getDragParameters(s),
                               "wrong");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                oe.getSpecifier());
            Assert.assertEquals("wrong", (String) oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingParameterLift() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 2.0, 1.0, 0.0);
        try {
            s.dragAcceleration(state.getDate(), state.getFrame(),
                               state.getPVCoordinates().getPosition(),
                               state.getAttitude().getRotation(),
                               state.getMass(), 1.0e-6, Vector3D.PLUS_I,
                               getDragParameters(s),
                               DragSensitive.LIFT_RATIO);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                oe.getSpecifier());
            Assert.assertEquals(2, oe.getParts().length);
            Assert.assertEquals(DragSensitive.LIFT_RATIO, (String) oe.getParts()[0]);
            Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, (String) oe.getParts()[1]);
        }
    }

    @Test
    public void testPresentParameterLift() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.4, 1.0, 0.0);
        FieldVector3D<DerivativeStructure> a = s.dragAcceleration(state.getDate(), state.getFrame(),
                                                                  state.getPVCoordinates().getPosition(),
                                                                  state.getAttitude().getRotation(),
                                                                  state.getMass(), 1.0e-6, Vector3D.PLUS_I,
                                                                  getDragParameters(s),
                                                                  DragSensitive.LIFT_RATIO);
        Assert.assertEquals(5.58e-10, a.getNorm().getReal(), 1.0e-12);
    }

    @Test
    public void testWrongParameterLift() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.4, 1.0, 0.0);
        try {
            s.dragAcceleration(state.getDate(), state.getFrame(),
                               state.getPVCoordinates().getPosition(),
                               state.getAttitude().getRotation(),
                               state.getMass(), 1.0e-6, Vector3D.PLUS_I,
                               getDragParameters(s),
                               "wrong");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                oe.getSpecifier());
            Assert.assertEquals(3, oe.getParts().length);
            Assert.assertEquals("wrong", (String) oe.getParts()[0]);
            Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, (String) oe.getParts()[1]);
            Assert.assertEquals(DragSensitive.LIFT_RATIO, (String) oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongParameterRadiation() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        try {
            s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation(),
                                            state.getMass(), Vector3D.PLUS_I,
                                            getRadiationParameters(s),
                                            "wrong");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                oe.getSpecifier());
            Assert.assertEquals("wrong", (String) oe.getParts()[0]);
        }
    }

    @Test
    public void testNullIllumination() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        FieldVector3D<DerivativeStructure> a =
                        s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation(),
                                            state.getMass(), new Vector3D(Precision.SAFE_MIN / 2,
                                                                          Vector3D.PLUS_I),
                                            getRadiationParameters(s),
                                            RadiationSensitive.ABSORPTION_COEFFICIENT);
        Assert.assertEquals(0.0, a.getNorm().getReal(), Double.MIN_VALUE);
    }

    @Test
    public void testBackwardIllumination() throws OrekitException {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                 state.getPVCoordinates().getPosition(),
                                 state.getAttitude().getRotation());
        FieldVector3D<DerivativeStructure> aPlus =
                        s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation(),
                                            state.getMass(), n,
                                            getRadiationParameters(s),
                                            RadiationSensitive.ABSORPTION_COEFFICIENT);
        FieldVector3D<DerivativeStructure> aMinus =
                        s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation(),
                                            state.getMass(), n.negate(),
                                            getRadiationParameters(s),
                                            RadiationSensitive.ABSORPTION_COEFFICIENT);
        Assert.assertEquals(0.0, aPlus.add(aMinus).getNorm().getReal(), Double.MIN_VALUE);
    }

    @Test
    public void testNormalOptimalRotationDouble() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            Vector3D normal = s.getNormal(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation());
            Assert.assertEquals(0, Vector3D.dotProduct(normal, Vector3D.PLUS_J), 1.0e-16);
        }
    }

    @Test
    public void testNormalOptimalRotationField() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        Field<Decimal64> field = Decimal64Field.getInstance();
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            FieldVector3D<Decimal64> normal = s.getNormal(new FieldAbsoluteDate<>(field, state.getDate()),
                                                          state.getFrame(),
                                                          new FieldVector3D<>(field, state.getPVCoordinates().getPosition()),
                                                          new FieldRotation<>(field, state.getAttitude().getRotation()));
            Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
        }
    }

    @Test
    public void testNormalOptimalRotationDS() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        DSFactory factory = new DSFactory(1, 2);
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            FieldVector3D<DerivativeStructure> normal = s.getNormal(state.getDate(),
                                                                    state.getFrame(),
                                                                    new FieldVector3D<>(factory.getDerivativeField(), state.getPVCoordinates().getPosition()),
                                                                    new FieldRotation<>(factory.getDerivativeField(), state.getAttitude().getRotation()));
            Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
        }
    }

    @Test
    public void testNormalFixedRateDouble() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J,
                                           initialDate, Vector3D.PLUS_K, 1.0e-3,
                                           0.0, 1.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            Vector3D normal = s.getNormal(state.getDate(), state.getFrame(),
                                            state.getPVCoordinates().getPosition(),
                                            state.getAttitude().getRotation());
            Assert.assertEquals(0, Vector3D.dotProduct(normal, Vector3D.PLUS_J), 1.0e-16);
        }
    }

    @Test
    public void testNormalFixedRateField() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J,
                                           initialDate, Vector3D.PLUS_K, 1.0e-3,
                                           0.0, 1.0, 0.0);
        Field<Decimal64> field = Decimal64Field.getInstance();
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            FieldVector3D<Decimal64> normal = s.getNormal(new FieldAbsoluteDate<>(field, state.getDate()),
                                                          state.getFrame(),
                                                          new FieldVector3D<>(field, state.getPVCoordinates().getPosition()),
                                                          new FieldRotation<>(field, state.getAttitude().getRotation()));
            Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
        }
    }

    @Test
    public void testNormalFixedRateDS() throws OrekitException {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J,
                                           initialDate, Vector3D.PLUS_K, 1.0e-3,
                                           0.0, 1.0, 0.0);
        DSFactory factory = new DSFactory(1, 2);
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            FieldVector3D<DerivativeStructure> normal = s.getNormal(state.getDate(),
                                                                    state.getFrame(),
                                                                    new FieldVector3D<>(factory.getDerivativeField(), state.getPVCoordinates().getPosition()),
                                                                    new FieldRotation<>(factory.getDerivativeField(), state.getAttitude().getRotation()));
            Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
        }
    }

    @Test
    public void testNormalSunAlignedDouble() throws OrekitException {
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0,
                                           (date, frame) -> new TimeStampedPVCoordinates(date, new Vector3D(0, 1e6, 0), Vector3D.ZERO),
                                           20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        Vector3D normal = s.getNormal(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                      Vector3D.ZERO, Rotation.IDENTITY);
        Assert.assertEquals(0, Vector3D.dotProduct(normal, Vector3D.PLUS_J), 1.0e-16);
    }

    @Test
    public void testNormalSunAlignedField() throws OrekitException {
        BoxAndSolarArraySpacecraft s =
                        new BoxAndSolarArraySpacecraft(0, 0, 0,
                                                       (date, frame) -> new TimeStampedPVCoordinates(date, new Vector3D(0, 1e6, 0), Vector3D.ZERO),
                                                       20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        Field<Decimal64> field = Decimal64Field.getInstance();
        FieldVector3D<Decimal64> normal = s.getNormal(FieldAbsoluteDate.getJ2000Epoch(field),
                                                      FramesFactory.getEME2000(),
                                                      FieldVector3D.getZero(field),
                                                      FieldRotation.getIdentity(field));
        Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
    }

    @Test
    public void testNormalSunAlignedDS() throws OrekitException {
        BoxAndSolarArraySpacecraft s =
                        new BoxAndSolarArraySpacecraft(0, 0, 0,
                                                       (date, frame) -> new TimeStampedPVCoordinates(date, new Vector3D(0, 1e6, 0), Vector3D.ZERO),
                                                       20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);
        DSFactory factory = new DSFactory(1, 2);
        FieldVector3D<DerivativeStructure> normal = s.getNormal(AbsoluteDate.J2000_EPOCH,
                                                                FramesFactory.getEME2000(),
                                                                FieldVector3D.getZero(factory.getDerivativeField()),
                                                                FieldRotation.getIdentity(factory.getDerivativeField()));
        Assert.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
    }

    private double[] getDragParameters(final BoxAndSolarArraySpacecraft basa) {
        final ParameterDriver[] drivers = basa.getDragParametersDrivers();
        final double[] parameters = new double[drivers.length];
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].getValue();
        }
        return parameters;
    }

    private double[] getRadiationParameters(final BoxAndSolarArraySpacecraft basa) {
        final ParameterDriver[] drivers = basa.getRadiationParametersDrivers();
        final double[] parameters = new double[drivers.length];
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].getValue();
        }
        return parameters;
    }

    @Before
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());

        // Satellite position as circular parameters, raan chosen to have sun elevation with
        // respect to orbit plane roughly evolving roughly from 15 to 15.2 degrees in the test range
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(280),
                                   FastMath.toRadians(10.0), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        propagator =
            new EcksteinHechlerPropagator(circ,
                                          new LofOffset(circ.getFrame(), LOFType.VVLH),
                                          ae, mu, c20, c30, c40, c50, c60);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    private double mu;
    private Propagator propagator;

}
