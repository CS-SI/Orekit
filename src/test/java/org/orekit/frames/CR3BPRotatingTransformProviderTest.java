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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

/**Unit tests for {@link CR3BPRotatingTransformProvider}.
 * @author Vincent Mouraux
 */
public class CR3BPRotatingTransformProviderTest {

    @Test
    public void testTransformationOrientationForEarthMoon() {

        // Load Bodies
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Set frames
        final Frame eme2000 = FramesFactory.getEME2000();
        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Moon position in EME2000
        Vector3D posMoon = moon.getPosition(date, eme2000);

        // Compute barycenter position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getStaticTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Moon are aligned as seen from Earth
        Assertions.assertEquals(0.0, Vector3D.angle(posMoon, posBary), 1.0e-10);
    }

    @Test
    public void testFieldTransformationOrientationForEarthMoon() {
        doTestFieldTransformationOrientationForEarthMoon(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldTransformationOrientationForEarthMoon(final Field<T> field) {

        // Load Bodies
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Set frames
        final Frame eme2000 = FramesFactory.getEME2000();
        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  TimeScalesFactory.getUTC());

        // Compute Moon position in EME2000
        FieldPVCoordinates<T> pvMoon = moon.getPVCoordinates(date, eme2000);
        FieldVector3D<T> posMoon = pvMoon.getPosition();

        // Compute barycenter position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posBary   = baryFrame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Moon are aligned as seen from Earth
        Assertions.assertEquals(0.0, FieldVector3D.angle(posMoon, posBary).getReal(), 1.0e-10);
    }


    @Test
    public void testSunEarth() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Time settings
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   timeScale);

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date, timeScale);
        final Frame baryFrame = syst.getRotatingFrame();

        // Compute Earth position in Sun centered frame
        Vector3D posEarth = earth.getPosition(date, sunFrame);

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L1 and Earth are aligned as seen from Sun
        Assertions.assertEquals(0.0, Vector3D.angle(posEarth, posBary), 3.0e-5);
    }

    @Test
    public void testFieldSunEarth() {
        doTestFieldSunEarth(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSunEarth(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Time settings
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  timeScale);

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date.toAbsoluteDate(), timeScale);
        final Frame baryFrame = syst.getRotatingFrame();

        // Compute Earth position in Sun centered frame
        FieldPVCoordinates<T> pvEarth = earth.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posEarth = pvEarth.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posBary   = baryFrame.getStaticTransformTo(sunFrame,date).transformPosition(FieldVector3D.getZero(field));

        // check L2 and Earth are aligned as seen from Sun
        Assertions.assertEquals(0.0, FieldVector3D.angle(posEarth, posBary).getReal(), 3.0e-5);
    }

    @Test
    public void testSunJupiter() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Time settings
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   timeScale);

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunJupiterCR3BP(date, timeScale);
        final Frame baryFrame = syst.getRotatingFrame();

        // Compute Jupiter position in Sun centered frame
        Vector3D posJupiter = jupiter.getPosition(date, sunFrame);

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Jupiter are aligned as seen from Sun
        Assertions.assertEquals(0.0, Vector3D.angle(posJupiter, posBary), 1.0e-10);
    }

    @Test
    public void testFieldSunJupiter() {
        doTestFieldSunJupiter(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSunJupiter(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Time settings
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  timeScale);

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunJupiterCR3BP(date.toAbsoluteDate(), timeScale);
        final Frame baryFrame = syst.getRotatingFrame();

        // Compute Jupiter position in Sun centered frame
        FieldPVCoordinates<T> pvJupiter = jupiter.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posJupiter = pvJupiter.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posBary   = baryFrame.getStaticTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Jupiter are aligned as seen from Sun
        Assertions.assertEquals(0.0, FieldVector3D.angle(posJupiter, posBary).getReal(), 1.0e-10);
    }

    @Test
    public void testBaryOrientation() {

        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate date0 = new AbsoluteDate(2000, 01, 1, 11, 58, 20.000,
                                                    timeScale);
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date0, timeScale);
        final Frame baryFrame = syst.getRotatingFrame();
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final AbsoluteDate date              = date0.shiftedBy(dt);
            final Vector3D     sunPositionInBary   = sun.getPosition(date, baryFrame);
            final Vector3D     earthPositionInBary = earth.getPosition(date, baryFrame);
            Assertions.assertEquals(0.0, Vector3D.angle(sunPositionInBary,   Vector3D.MINUS_I), 1.0e-10);
            Assertions.assertEquals(FastMath.PI, Vector3D.angle(earthPositionInBary, Vector3D.MINUS_I), 1.0e-4);
        }
    }

    @Test
    public void testFieldBaryOrientation() {
        doTestFieldBaryOrientation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldBaryOrientation(final Field<T> field) {

        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 01, 1, 11, 58, 20.000,
                                                                   timeScale);
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date0.toAbsoluteDate(), timeScale);
        final Frame baryFrame = syst.getRotatingFrame();
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final FieldAbsoluteDate<T> date              = date0.shiftedBy(dt);
            final FieldVector3D<T>     sunPositionInBary   = sun.getPosition(date, baryFrame);
            final FieldVector3D<T>     earthPositionInBary = earth.getPosition(date, baryFrame);
            Assertions.assertEquals(0.0, FieldVector3D.angle(sunPositionInBary,   Vector3D.MINUS_I).getReal(), 1.0e-10);
            Assertions.assertEquals(FastMath.PI, FieldVector3D.angle(earthPositionInBary, Vector3D.MINUS_I).getReal(), 1.0e-4);
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}