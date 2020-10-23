/* Copyright 2002-2020 CS GROUP
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

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
        PVCoordinates pvMoon = moon.getPVCoordinates(date, eme2000);
        Vector3D posMoon = pvMoon.getPosition();

        // Compute barycenter position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Moon are aligned as seen from Earth
        Assert.assertEquals(0.0, Vector3D.angle(posMoon, posBary), 1.0e-10);
    }

    @Test
    public void testFieldTransformationOrientationForEarthMoon() {
        doTestFieldTransformationOrientationForEarthMoon(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldTransformationOrientationForEarthMoon(final Field<T> field) {

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
        Assert.assertEquals(0.0, FieldVector3D.angle(posMoon, posBary).getReal(), 1.0e-10);
    }


    @Test
    public void testSunEarth() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Earth position in Sun centered frame
        PVCoordinates pvEarth = earth.getPVCoordinates(date, sunFrame);
        Vector3D posEarth = pvEarth.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L1 and Earth are aligned as seen from Sun
        Assert.assertEquals(0.0, Vector3D.angle(posEarth, posBary), 3.0e-5);
    }

    @Test
    public void testFieldSunEarth() {
        doTestFieldSunEarth(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldSunEarth(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody earth = CelestialBodyFactory.getEarth();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Earth position in Sun centered frame
        FieldPVCoordinates<T> pvEarth = earth.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posEarth = pvEarth.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posBary   = baryFrame.getTransformTo(sunFrame,date).transformPosition(FieldVector3D.getZero(field));

        // check L2 and Earth are aligned as seen from Sun
        Assert.assertEquals(0.0, FieldVector3D.angle(posEarth, posBary).getReal(), 3.0e-5);
    }

    @Test
    public void testSunJupiter() {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunJupiterCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                                   TimeScalesFactory.getUTC());

        // Compute Jupiter position in Sun centered frame
        PVCoordinates pvJupiter = jupiter.getPVCoordinates(date, sunFrame);
        Vector3D posJupiter = pvJupiter.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posBary   = baryFrame.getTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Jupiter are aligned as seen from Sun
        Assert.assertEquals(0.0, Vector3D.angle(posJupiter, posBary), 1.0e-10);
    }

    @Test
    public void testFieldSunJupiter() {
        doTestFieldSunJupiter(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldSunJupiter(final Field<T> field) {

        // Load Bodies
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();

        // Set frames
        final Frame sunFrame = sun.getInertiallyOrientedFrame();
        final CR3BPSystem syst = CR3BPFactory.getSunJupiterCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();

        // Time settings
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2000, 01, 01, 0, 0, 00.000,
                                                                  TimeScalesFactory.getUTC());

        // Compute Jupiter position in Sun centered frame
        FieldPVCoordinates<T> pvJupiter = jupiter.getPVCoordinates(date, sunFrame);
        FieldVector3D<T> posJupiter = pvJupiter.getPosition();

        // Compute barycenter position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        FieldVector3D<T> posBary   = baryFrame.getTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check barycenter and Jupiter are aligned as seen from Sun
        Assert.assertEquals(0.0, FieldVector3D.angle(posJupiter, posBary).getReal(), 1.0e-10);
    }

    @Test
    public void testBaryOrientation() {

        final AbsoluteDate date0 = new AbsoluteDate(2000, 01, 1, 11, 58, 20.000,
                                                   TimeScalesFactory.getUTC());
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final AbsoluteDate date              = date0.shiftedBy(dt);
            final Vector3D     sunPositionInBary   = sun.getPVCoordinates(date, baryFrame).getPosition();
            final Vector3D     earthPositionInBary = earth.getPVCoordinates(date, baryFrame).getPosition();
            Assert.assertEquals(0.0, Vector3D.angle(sunPositionInBary,   Vector3D.MINUS_I), 1.0e-10);
            Assert.assertEquals(FastMath.PI, Vector3D.angle(earthPositionInBary, Vector3D.MINUS_I), 1.0e-4);
        }
    }

    @Test
    public void testFieldBaryOrientation() {
        doTestFieldBaryOrientation(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldBaryOrientation(final Field<T> field) {

        final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 01, 1, 11, 58, 20.000,
                                                                   TimeScalesFactory.getUTC());
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP();
        final Frame baryFrame = syst.getRotatingFrame();
        for (double dt = -Constants.JULIAN_DAY; dt <= Constants.JULIAN_DAY; dt += 3600.0) {
            final FieldAbsoluteDate<T> date              = date0.shiftedBy(dt);
            final FieldVector3D<T>     sunPositionInBary   = sun.getPVCoordinates(date, baryFrame).getPosition();
            final FieldVector3D<T>     earthPositionInBary = earth.getPVCoordinates(date, baryFrame).getPosition();
            Assert.assertEquals(0.0, FieldVector3D.angle(sunPositionInBary,   Vector3D.MINUS_I).getReal(), 1.0e-10);
            Assert.assertEquals(FastMath.PI, FieldVector3D.angle(earthPositionInBary, Vector3D.MINUS_I).getReal(), 1.0e-4);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}