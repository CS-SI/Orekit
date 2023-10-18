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
package org.orekit.forces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class BoxAndSolarArraySpacecraftTest {

    @Test
    public void testCoefficients() {
        final double drag = 1.1;
        final double lift = 2.2;
        final double abso = 3.3;
        final double refl = 4.4;

        final double tol = 1.0e-15;

        // build box
        List<Panel> box = BoxAndSolarArraySpacecraft.buildBox(1.5, 3.5, 2.5,
            drag, lift, abso, refl);

        // check
        for (Panel panel : box) {
            Assertions.assertEquals(drag, panel.getDrag(), tol);
            Assertions.assertEquals(lift, panel.getLiftRatio(), tol);
            Assertions.assertEquals(refl, panel.getReflection(), tol);
            Assertions.assertEquals(abso, panel.getAbsorption(), tol);
        }

        // build panels
        List<Panel> panels = BoxAndSolarArraySpacecraft.buildPanels(1.5, 3.5, 2.5,
            CelestialBodyFactory.getSun(), 0.4, Vector3D.PLUS_J,
            drag, lift, abso, refl);

        // check
        for (Panel panel : panels) {
            Assertions.assertEquals(drag, panel.getDrag(), tol);
            Assertions.assertEquals(lift, panel.getLiftRatio(), tol);
            Assertions.assertEquals(refl, panel.getReflection(), tol);
            Assertions.assertEquals(abso, panel.getAbsorption(), tol);
        }

    }

    @Test
    public void testParametersDrivers() {

        CelestialBody sun = CelestialBodyFactory.getSun();
        List<Panel> cube = new ArrayList<>();
        cube.add(new FixedPanel(Vector3D.MINUS_I, 3.0, false, 2.0, 0.0, 0.8, 0.1));
        cube.add(new FixedPanel(Vector3D.PLUS_I,  3.0, false, 2.0, 0.0, 0.8, 0.1));
        cube.add(new FixedPanel(Vector3D.MINUS_J, 3.0, false, 2.0, 0.0, 0.8, 0.1));
        cube.add(new FixedPanel(Vector3D.PLUS_J,  3.0, false, 2.0, 0.0, 0.8, 0.1));
        cube.add(new FixedPanel(Vector3D.MINUS_K, 3.0, false, 2.0, 0.0, 0.8, 0.1));
        cube.add(new FixedPanel(Vector3D.PLUS_K,  3.0, false, 2.0, 0.0, 0.8, 0.1));
        List<Panel> boxNoLift = BoxAndSolarArraySpacecraft.buildBox(1.5, 3.5, 2.5, 2.0, 0.0, 0.8, 0.1);
        List<Panel> boxLift   = BoxAndSolarArraySpacecraft.buildBox(1.5, 3.5, 2.5, 2.0, 0.4, 0.8, 0.1);

        BoxAndSolarArraySpacecraft s1 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.0, 0.8, 0.1);
        Assertions.assertEquals(1, s1.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s1.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s1.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s1.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                s1.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1, s1.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s2 =
                        new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 2.0, 0.4, 0.8, 0.1);
        Assertions.assertEquals(1, s2.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s2.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s2.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s2.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                s2.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s2.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        PointingPanel pointingNoLift = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 2.0, 0.0, 0.8, 0.1);
        BoxAndSolarArraySpacecraft s3 =
                        new BoxAndSolarArraySpacecraft(Stream.concat(cube.stream(), Stream.of(pointingNoLift)).
                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s3.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s3.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s3.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s3.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                s3.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s3.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        PointingPanel pointingLift = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 2.0, 0.4, 0.8, 0.1);
        BoxAndSolarArraySpacecraft s4 =
                        new BoxAndSolarArraySpacecraft(Stream.concat(cube.stream(), Stream.of(pointingLift)).
                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s4.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s4.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s4.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s4.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                s4.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s4.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        SlewingPanel slewingNoLift = new SlewingPanel(Vector3D.PLUS_J, 7.292e-5, AbsoluteDate.J2000_EPOCH,
                                                      Vector3D.PLUS_I, 20.0, 2.0, 0.0, 0.8, 0.1);
        BoxAndSolarArraySpacecraft s5 = new BoxAndSolarArraySpacecraft(Stream.concat(boxNoLift.stream(), Stream.of(slewingNoLift)).
                                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s5.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s5.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s5.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s5.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                s5.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s5.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        SlewingPanel slewingLift = new SlewingPanel(Vector3D.PLUS_J, 7.292e-5, AbsoluteDate.J2000_EPOCH,
                                                    Vector3D.PLUS_I, 20.0, 2.0, 0.4, 0.8, 0.1);
        BoxAndSolarArraySpacecraft s6 = new BoxAndSolarArraySpacecraft(Stream.concat(boxLift.stream(), Stream.of(slewingLift)).
                                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s6.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s6.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s6.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s6.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                s6.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s6.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s7 =
                        new BoxAndSolarArraySpacecraft(Stream.concat(cube.stream(), Stream.of(slewingNoLift)).
                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s7.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s7.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s7.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s7.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                s7.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s7.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

        BoxAndSolarArraySpacecraft s8 =
                        new BoxAndSolarArraySpacecraft(Stream.concat(cube.stream(), Stream.of(slewingLift)).
                                                       collect(Collectors.toList()));
        Assertions.assertEquals(1, s8.getDragParametersDrivers().size());
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR, s8.getDragParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s8.getDragParametersDrivers().get(0).getValue(), 1.0e-15);
        Assertions.assertEquals(1, s8.getRadiationParametersDrivers().size());
        Assertions.assertEquals(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                s8.getRadiationParametersDrivers().get(0).getName());
        Assertions.assertEquals(1.0, s8.getRadiationParametersDrivers().get(0).getValue(), 1.0e-15);

    }

    @Test
    public void testNoLiftWithoutReflection() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 0.0, 1.0, 0.0);

        Vector3D earthRot = new Vector3D(0.0, 0.0, 7.292115e-4);
        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // simple Earth fixed atmosphere
            Vector3D p = state.getPosition();
            Vector3D v = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, p);
            Vector3D relativeVelocity = vAtm.subtract(v);

            Vector3D drag = s.dragAcceleration(state, 0.001, relativeVelocity, getDragParameters(s));
            Assertions.assertEquals(0.0, Vector3D.angle(relativeVelocity, drag), 1.0e-15);

            Vector3D sunDirection = sun.getPosition(date, state.getFrame()).normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D radiation = s.radiationPressureAcceleration(state, flux, getRadiationParameters(s));
            Assertions.assertEquals(0.0, Vector3D.angle(flux, radiation), 1.0e-9);

        }

    }

    @Test
    public void testOnlyLiftWithoutReflection() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 1.0, 1.0, 0.0);

        Vector3D earthRot = new Vector3D(0.0, 0.0, 7.292115e-4);
        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // simple Earth fixed atmosphere
            Vector3D p = state.getPosition();
            Vector3D v = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, p);
            Vector3D relativeVelocity = vAtm.subtract(v);

            Vector3D drag = s.dragAcceleration(state, 0.001, relativeVelocity, getDragParameters(s));
            Assertions.assertTrue(Vector3D.angle(relativeVelocity, drag) > 0.167);
            Assertions.assertTrue(Vector3D.angle(relativeVelocity, drag) < 0.736);

            Vector3D sunDirection = sun.getPosition(date, state.getFrame()).normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D radiation = s.radiationPressureAcceleration(state, flux, getRadiationParameters(s));
            Assertions.assertEquals(0.0, Vector3D.angle(flux, radiation), 1.0e-9);

        }

    }

    @Test
    public void testLiftVsNoLift()
        throws NoSuchFieldException, SecurityException,
               IllegalArgumentException, IllegalAccessException {

        // older implementation did not consider lift, so it really worked
        // only for symmetrical shapes. For testing purposes, we will use a
        // basic cubic shape without solar arrays and a relative atmosphere
        // velocity either *exactly* facing a side or *exactly* along a main diagonal
        List<Panel> facets = new ArrayList<>(7);
        facets.add(new FixedPanel(Vector3D.MINUS_I, 3.0, false, 1.0, 1.0, 1.0, 0.0));
        facets.add(new FixedPanel(Vector3D.PLUS_I,  3.0, false, 1.0, 1.0, 1.0, 0.0));
        facets.add(new FixedPanel(Vector3D.MINUS_J, 3.0, false, 1.0, 1.0, 1.0, 0.0));
        facets.add(new FixedPanel(Vector3D.PLUS_J,  3.0, false, 1.0, 1.0, 1.0, 0.0));
        facets.add(new FixedPanel(Vector3D.MINUS_K, 3.0, false, 1.0, 1.0, 1.0, 0.0));
        facets.add(new FixedPanel(Vector3D.PLUS_K,  3.0, false, 1.0, 1.0, 1.0, 0.0));
        BoxAndSolarArraySpacecraft cube = new BoxAndSolarArraySpacecraft(facets);

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getEME2000();
        Vector3D position = new Vector3D(1234567.8, 9876543.21, 121212.3434);
        double density = 0.001;
        SpacecraftState state = new SpacecraftState(new CartesianOrbit(new TimeStampedPVCoordinates(date, position, Vector3D.ZERO),
                                                                       frame, Constants.EIGEN5C_EARTH_MU),
                                                    new Attitude(frame,
                                                                new TimeStampedAngularCoordinates(date,
                                                                                                  Rotation.IDENTITY,
                                                                                                  Vector3D.ZERO,
                                                                                                  Vector3D.ZERO)),
                                                    1000.0);

        // head-on, there acceleration with lift should be twice acceleration without lift
        Vector3D headOnVelocity = new Vector3D(2000, 0.0, 0.0);
        Vector3D newHeadOnDrag  = cube.dragAcceleration(state, density, headOnVelocity, getDragParameters(cube));
        Vector3D oldHeadOnDrag  = oldDragAcceleration(cube, state, density, headOnVelocity);
        MatcherAssert.assertThat(newHeadOnDrag, OrekitMatchers.vectorCloseTo(oldHeadOnDrag.scalarMultiply(2), 1));

        // on an angle, the no lift implementation applies drag to the velocity direction
        // instead of to the facet normal direction. In the symmetrical case, this implies
        // it applied a single cos(θ) coefficient (projected surface reduction) instead
        // of using cos²(θ) (projected surface reduction *and* normal component projection)
        // and since molecule is reflected backward with the same velocity, this implies a
        // factor 2 in linear momentum differences
        Vector3D diagonalVelocity = new Vector3D(2000, 2000, 2000);
        Vector3D newDiagDrag= cube.dragAcceleration(state, density, diagonalVelocity, getDragParameters(cube));
        Vector3D oldDiagDrag = oldDragAcceleration(cube, state, density, diagonalVelocity);
        double oldMissingCoeff = 2.0 / FastMath.sqrt(3.0);
        Vector3D fixedOldDrag = new Vector3D(oldMissingCoeff, oldDiagDrag);
        MatcherAssert.assertThat(newDiagDrag, OrekitMatchers.vectorCloseTo(fixedOldDrag, 1));

    }

    // this is a slightly adapted version of the pre-9.0 implementation
    // Beware that this implementation is WRONG
    private Vector3D oldDragAcceleration(final BoxAndSolarArraySpacecraft boxWithoutSolarArray,
                                         final SpacecraftState state,
                                         final double density, final Vector3D relativeVelocity)
         throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, SecurityException {

        final double dragCoeff = boxWithoutSolarArray.getDragParametersDrivers().get(0).getValue();

        // relative velocity in spacecraft frame
        final Vector3D v = state.getAttitude().getRotation().applyTo(relativeVelocity);

        // body facets contribution
        double sv = 0;
        for (final Panel panel : boxWithoutSolarArray.getPanels()) {
            final double dot = Vector3D.dotProduct(panel.getNormal(state), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= panel.getArea() * dot;
            }
        }

        return new Vector3D(sv * density * dragCoeff / (2.0 * state.getMass()), relativeVelocity);

    }

    @Test
    public void testPlaneSpecularReflection() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel reflectingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 0.0, 1.0);
        BoxAndSolarArraySpacecraft s = new BoxAndSolarArraySpacecraft(Collections.singletonList(reflectingSolarArray));

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPosition(date, state.getFrame()).normalize();
            Vector3D flux         = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration = s.radiationPressureAcceleration(state, flux, getRadiationParameters(s));
            Vector3D normal       = state.getAttitude().getRotation().applyInverseTo(reflectingSolarArray.getNormal(state));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assertions.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to solar array normal as there is only specular reflection
            Assertions.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, normal)), 1.0e-3);

        }

    }

    @Test
    public void testPlaneAbsorption() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel absorbingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 1.0, 0.0);
        BoxAndSolarArraySpacecraft s = new BoxAndSolarArraySpacecraft(Collections.singletonList(absorbingSolarArray));

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPosition(date, state.getFrame()).normalize();
            Vector3D flux         = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration = s.radiationPressureAcceleration(state, flux, getRadiationParameters(s));
            Vector3D normal       = state.getAttitude().getRotation().applyInverseTo(absorbingSolarArray.getNormal(state));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assertions.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to Sun direction as there is only absorption
            Assertions.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, sunDirection)), 1.0e-3);

        }

    }

    /** Test solar array radiation acceleration with zero flux. */
    @Test
    public void testNullIllumination() {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel absorbingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 1.0, 0.0);
        BoxAndSolarArraySpacecraft s = new BoxAndSolarArraySpacecraft(Collections.singletonList(absorbingSolarArray));

        // "Field" the inputs using Binary64
        Field<Binary64> field = Binary64Field.getInstance();
        Binary64[] srpParam = getRadiationParameters(s, field);

        FieldSpacecraftState<Binary64> fState = new FieldSpacecraftState<>(field, state);
        FieldVector3D<Binary64> flux = new FieldVector3D<Binary64>(field.getOne(),
                        new Vector3D(Precision.SAFE_MIN / 2, Vector3D.PLUS_I));


        FieldVector3D<Binary64> a = s.radiationPressureAcceleration(fState, flux, srpParam);
        Assertions.assertEquals(0.0, a.getNorm().getReal(), Double.MIN_VALUE);
    }

    /** Test forward/backward acceleration due to solar array radiation pressure. */
    @Test
    public void testBackwardIllumination() {
        SpacecraftState state = propagator.getInitialState();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel absorbingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 1.0, 0.0);
        BoxAndSolarArraySpacecraft s = new BoxAndSolarArraySpacecraft(Collections.singletonList(absorbingSolarArray));

        // "Field" the inputs using Binary64
        Field<Binary64> field = Binary64Field.getInstance();
        Binary64[] srpParam = getRadiationParameters(s, field);

        FieldSpacecraftState<Binary64> fState = new FieldSpacecraftState<>(field, state);

        // Flux equal to SA normal
        FieldVector3D<Binary64> flux = absorbingSolarArray.getNormal(fState);

        // Forward flux
        FieldVector3D<Binary64> aPlus = s.radiationPressureAcceleration(fState, flux, srpParam);
        // Backward flux
        FieldVector3D<Binary64> aMinus = s.radiationPressureAcceleration(fState, flux.negate(), srpParam);

        Assertions.assertEquals(0.0, aPlus.add(aMinus).getNorm().getReal(), Double.MIN_VALUE);
    }

    /** Test the functions computing drag and SRP acceleration and giving FieldVector3D outputs.
     *  By comparing the "double" value with a "Binary64" implementation.
     */
    @Test
    public void testFieldAcceleration() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();

        // Assuming simple Earth rotation model, constant density and flux
        Vector3D earthRot = new Vector3D(0., 0., Constants.GRIM5C1_EARTH_ANGULAR_VELOCITY);
        double density = 1.e-3;
        double refFlux = 4.56e-6;


        // Build a S/C box with non-nil coefficients so that the computation of the acceleration does not
        // avoid any line of code
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1., 2., 3., sun, 20.0, Vector3D.PLUS_J, 2.0, 0.3, 0.5, 0.4);

        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // Data used in acceleration computation
            Vector3D position = state.getPosition();
            Vector3D velocity = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, position);
            Vector3D relativeVelocity = vAtm.subtract(velocity);

            Frame frame = state.getFrame();
            Vector3D flux = position.subtract(sun.getPosition(date, frame)).normalize().scalarMultiply(refFlux);

            // Acceleration in double
            Vector3D aDrag = s.dragAcceleration(state, density, relativeVelocity, getDragParameters(s));
            Vector3D aSrp = s.radiationPressureAcceleration(state, flux, getRadiationParameters(s));

            // "Field" the inputs using Binary64
            Field<Binary64> field = Binary64Field.getInstance();
            FieldSpacecraftState<Binary64> fState = new FieldSpacecraftState<>(field, state);

            FieldVector3D<Binary64> fluxF = new FieldVector3D<Binary64>(field.getOne(), flux);
            Binary64 densityF = new Binary64(density);
            FieldVector3D<Binary64> relativeVelocityF = new FieldVector3D<Binary64>(field.getOne(), relativeVelocity);


            // Acceleration in Binary64
            FieldVector3D<Binary64> aDragF = s.dragAcceleration(fState, densityF, relativeVelocityF, getDragParameters(s, field));
            FieldVector3D<Binary64> aSrpF  = s.radiationPressureAcceleration(fState, fluxF, getRadiationParameters(s, field));
            // Compare double and Binary64 accelerations
            Assertions.assertEquals(0.0, Vector3D.distance(aDrag, aDragF.toVector3D()), Precision.EPSILON);
            Assertions.assertEquals(0.0, Vector3D.distance(aSrp,  aSrpF.toVector3D()), Precision.EPSILON);
        }
    }

    /** Get drag parameters as double[]. */
    private double[] getDragParameters(final BoxAndSolarArraySpacecraft basa) {
        final List<ParameterDriver> drivers = basa.getDragParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get radiation parameters as double[]. */
    private double[] getRadiationParameters(final BoxAndSolarArraySpacecraft basa) {
        final List<ParameterDriver> drivers = basa.getRadiationParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get drag parameters as field[]. */
    private <T extends CalculusFieldElement<T>> T[] getDragParameters(final BoxAndSolarArraySpacecraft basa,
                                                                  final Field<T> field) {
        final List<ParameterDriver> drivers = basa.getDragParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue());
        }
        return parameters;
    }

    /** Get radiation parameters as field[]. */
    private <T extends CalculusFieldElement<T>> T[] getRadiationParameters(final BoxAndSolarArraySpacecraft basa,
                                                                  final Field<T> field) {
        final List<ParameterDriver> drivers = basa.getRadiationParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue());
        }
        return parameters;
    }

    @BeforeEach
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
                                   FastMath.toRadians(10.0), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        propagator =
            new EcksteinHechlerPropagator(circ,
                                          new LofOffset(circ.getFrame(), LOFType.LVLH_CCSDS),
                                          ae, mu, c20, c30, c40, c50, c60);
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    private double mu;
    private Propagator propagator;

}
