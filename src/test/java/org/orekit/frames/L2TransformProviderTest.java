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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

/**Unit tests for {@link L2TransformProvider}.
 *
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */
public class L2TransformProviderTest {

    @Test
    public void testTransformationOrientationForEarthMoon() {

        // Load Bodies
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Set framesd
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame l2Frame = new L2Frame(earth, moon);

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Moon position in EME2000
        Vector3D posMoon = moon.getPosition(date, eme2000);

        // Compute L2 position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getStaticTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check L2 and Moon are aligned as seen from Earth
        Assertions.assertEquals(0.0, Vector3D.angle(posMoon, posL2), 1.0e-10);

        // check L2 if at least 60 000km farther than Moon
        Assertions.assertTrue(posL2.getNorm() > posMoon.getNorm() + 6.0e7);

    }

    @Test
    public void testFieldTransformationOrientationForEarthMoon() {
        doTestFieldTransformationOrientationForEarthMoon(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldTransformationOrientationForEarthMoon(final Field<T> field) {

        // Load Bodies
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Set framesd
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame l2Frame = new L2Frame(earth, moon);

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  TimeScalesFactory.getUTC());

        // Compute Moon position in EME2000
        FieldPVCoordinates<T> pvMoon = moon.getPVCoordinates(date, eme2000);
        FieldVector3D<T> posMoon = pvMoon.getPosition();

        // Compute L2 position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posL2   = l2Frame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check L2 and Moon are aligned as seen from Earth
        Assertions.assertEquals(0.0, FieldVector3D.angle(posMoon, posL2).getReal(), 1.0e-10);

        // check L2 if at least 60 000km farther than Moon
        Assertions.assertTrue(posL2.getNorm().getReal() > posMoon.getNorm().getReal() + 6.0e7);

    }

    @Test
    public void testSunEarth() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final Frame l2Frame = new L2Frame(sun, earth);

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Earth position in Sun centered frame
        Vector3D posEarth = earth.getPosition(date, sunFrame);

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Earth are aligned as seen from Sun
        Assertions.assertEquals(0.0, Vector3D.angle(posEarth, posL2), 1.0e-10);

        // check L2 if at least 1 000 000km farther than Earth
        Assertions.assertTrue(posL2.getNorm() > posEarth.getNorm() + 1.0e9);
    }

    @Test
    public void testFieldSunEarth() {
        doTestFieldSunEarth(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSunEarth(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final Frame l2Frame = new L2Frame(sun, earth);

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Earth position in Sun centered frame
        FieldPVCoordinates<T> pvEarth = earth.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posEarth = pvEarth.getPosition();

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posL2   = l2Frame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Earth are aligned as seen from Sun
        Assertions.assertEquals(0.0, FieldVector3D.angle(posEarth, posL2).getReal(), 1.0e-10);

        // check L2 if at least 1 000 000km farther than Earth
        Assertions.assertTrue(posL2.getNorm().getReal() > posEarth.getNorm().getReal() + 1.0e9);
    }

    @Test
    public void testSunJupiter() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final Frame l2Frame = new L2Frame(sun, jupiter);

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Jupiter position in Sun centered frame
        Vector3D posJupiter = jupiter.getPosition(date, sunFrame);

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Jupiter are aligned as seen from Sun
        Assertions.assertEquals(0.0, Vector3D.angle(posJupiter, posL2), 1.0e-10);

        // check L2 if at least 50 000 000km farther than Jupiter
        Assertions.assertTrue(posL2.getNorm() > posJupiter.getNorm() + 5.0e10);
    }

    @Test
    public void testFieldSunJupiter() {
        doTestFieldSunJupiter(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSunJupiter(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final Frame l2Frame = new L2Frame(sun, jupiter);

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  TimeScalesFactory.getUTC());

        // Compute Jupiter position in Sun centered frame
        FieldPVCoordinates<T> pvJupiter = jupiter.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posJupiter = pvJupiter.getPosition();

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posL2   = l2Frame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Jupiter are aligned as seen from Sun
        Assertions.assertEquals(0.0, FieldVector3D.angle(posJupiter, posL2).getReal(), 1.0e-10);

        // check L2 if at least 50 000 000km farther than Jupiter
        Assertions.assertTrue(posL2.getNorm().getReal() > posJupiter.getNorm().getReal() + 5.0e10);
    }

    @Test
    public void testL2Orientation() {

        final AbsoluteDate date0 = new AbsoluteDate(2000, 01, 1, 11, 58, 20.000,
                                                   TimeScalesFactory.getUTC());
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final Frame         l2Frame = new L2Frame(sun, earth);
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final AbsoluteDate date              = date0.shiftedBy(dt);
            final Vector3D     sunPositionInL2   = sun.getPosition(date, l2Frame);
            final Vector3D     earthPositionInL2 = earth.getPosition(date, l2Frame);
            Assertions.assertEquals(0.0, Vector3D.angle(sunPositionInL2,   Vector3D.MINUS_I), 3.0e-14);
            Assertions.assertEquals(0.0, Vector3D.angle(earthPositionInL2, Vector3D.MINUS_I), 3.0e-14);
        }
    }

    @Test
    public void testFieldL2Orientation() {
        doTestFieldL2Orientation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldL2Orientation(final Field<T> field) {

        final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 01, 1, 11, 58, 20.000,
                                                                   TimeScalesFactory.getUTC());
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final Frame         l2Frame = new L2Frame(sun, earth);
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final FieldAbsoluteDate<T> date              = date0.shiftedBy(dt);
            final FieldVector3D<T>     sunPositionInL2   = sun.getPosition(date, l2Frame);
            final FieldVector3D<T>     earthPositionInL2 = earth.getPosition(date, l2Frame);
            Assertions.assertEquals(0.0, FieldVector3D.angle(sunPositionInL2,   Vector3D.MINUS_I).getReal(), 3.0e-14);
            Assertions.assertEquals(0.0, FieldVector3D.angle(earthPositionInL2, Vector3D.MINUS_I).getReal(), 3.0e-14);
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
