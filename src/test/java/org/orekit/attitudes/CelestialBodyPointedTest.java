/* Copyright 2002-2025 CS GROUP
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

class CelestialBodyPointedTest {

    @Test
    void templateTestGetAttitudeRotationSameFrame() {
        templateTestGetAttitudeRotation(FramesFactory.getEME2000());
    }

    @Test
    void templateTestGetAttitudeRotationDifferentFrame() {
        templateTestGetAttitudeRotation(FramesFactory.getGCRF());
    }

    void templateTestGetAttitudeRotation(final Frame frame) {
        // GIVEN
        final Frame celestialFrame = FramesFactory.getEME2000();
        final CelestialBodyPointed celestialBodyPointed = new CelestialBodyPointed(celestialFrame,
                CelestialBodyFactory.getSun(), Vector3D.PLUS_K, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final PVCoordinates pv =
                new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                        new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new CartesianOrbit(pv, frame, AbsoluteDate.ARBITRARY_EPOCH, Constants.EIGEN5C_EARTH_MU);
        // WHEN
        final Rotation actualRotation = celestialBodyPointed.getAttitudeRotation(orbit, orbit.getDate(), frame);
        // THEN
        final Rotation expectedRotation = celestialBodyPointed.getAttitude(orbit, orbit.getDate(), frame).getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    void testFieldGetAttitudeRotationSameFrame() {
        templateTestFieldGetAttitudeRotation(FramesFactory.getEME2000());
    }

    @Test
    void testFieldGetAttitudeRotationDifferentFrame() {
        templateTestFieldGetAttitudeRotation(FramesFactory.getGCRF());
    }

    void templateTestFieldGetAttitudeRotation(final Frame frame) {
        // GIVEN
        final Frame celestialFrame = FramesFactory.getEME2000();
        final CelestialBodyPointed celestialBodyPointed = new CelestialBodyPointed(celestialFrame,
                CelestialBodyFactory.getSun(), Vector3D.PLUS_K, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final PVCoordinates pv =
                new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                        new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new CartesianOrbit(pv, frame, AbsoluteDate.ARBITRARY_EPOCH, Constants.EIGEN5C_EARTH_MU);
        final FieldOrbit<Complex> fieldOrbit = new FieldCartesianOrbit<>(ComplexField.getInstance(), orbit);
        // WHEN
        final FieldRotation<Complex> actualRotation = celestialBodyPointed.getAttitudeRotation(fieldOrbit, fieldOrbit.getDate(), frame);
        // THEN
        final FieldRotation<Complex> expectedRotation = celestialBodyPointed.getAttitude(fieldOrbit, fieldOrbit.getDate(), frame).getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    @Test
    void testSunPointing() {
        CelestialBody sun = CelestialBodyFactory.getSun();

        final Frame frame = FramesFactory.getGCRF();
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getTAI());
        AttitudeProvider sunPointing =
            new CelestialBodyPointed(frame, sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        Attitude attitude   = sunPointing.getAttitude(orbit, date, frame);

        checkField(Binary64Field.getInstance(), sunPointing, orbit, date, frame);

        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D zDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assertions.assertEquals(0,
                     Vector3D.dotProduct(zDirection, Vector3D.crossProduct(xDirection, Vector3D.PLUS_K)),
                     1.0e-15);

        // the following statement checks we take parallax into account
        // Sun-Earth-Sat are in quadrature, with distance (Earth, Sat) == distance(Sun, Earth) / 5000
        Assertions.assertEquals(FastMath.atan(1.0 / 5000.0),
                            Vector3D.angle(xDirection,
                                           sun.getPosition(date, frame)),
                                           1.0e-15);

        double h = 0.1;
        Attitude aMinus = sunPointing.getAttitude(orbit.shiftedBy(-h), date.shiftedBy(-h), frame);
        Attitude a0     = sunPointing.getAttitude(orbit, date, frame);
        Attitude aPlus  = sunPointing.getAttitude(orbit.shiftedBy(h), date.shiftedBy(h), frame);

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(aMinus.shiftedBy(h).getRotation(),
                                                       a0.getRotation());
        double evolutionAngleMinus = Rotation.distance(aMinus.getRotation(),
                                                       a0.getRotation());
        Assertions.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(a0.getRotation(),
                                                       aPlus.shiftedBy(-h).getRotation());
        double evolutionAnglePlus  = Rotation.distance(a0.getRotation(),
                                                       aPlus.getRotation());
        Assertions.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final AttitudeProvider provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame)
        {
        Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

