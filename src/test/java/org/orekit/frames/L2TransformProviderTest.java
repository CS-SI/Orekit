/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

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
        PVCoordinates pvMoon = moon.getPVCoordinates(date, eme2000);
        Vector3D posMoon = pvMoon.getPosition();

        // Compute L2 position in EME2000
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);

        // check L2 and Moon are aligned as seen from Earth
        Assert.assertEquals(0.0, Vector3D.angle(posMoon, posL2), 1.0e-10);

        // check L2 if at least 60 000km farther than Moon
        Assert.assertTrue(posL2.getNorm() > posMoon.getNorm() + 6.0e7);

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
        PVCoordinates pvEarth = earth.getPVCoordinates(date, sunFrame);
        Vector3D posEarth = pvEarth.getPosition();

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Earth are aligned as seen from Sun
        Assert.assertEquals(0.0, Vector3D.angle(posEarth, posL2), 1.0e-10);

        // check L2 if at least 1 000 000km farther than Earth
        Assert.assertTrue(posL2.getNorm() > posEarth.getNorm() + 1.0e9);
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
        PVCoordinates pvJupiter = jupiter.getPVCoordinates(date, sunFrame);
        Vector3D posJupiter = pvJupiter.getPosition();

        // Compute L2 position in Sun centered frame
        // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
        // because the test should avoid doing wrong interpretation of the meaning and
        // particularly on the sign of the translation)
        Vector3D posL2   = l2Frame.getTransformTo(sunFrame,date).transformPosition(Vector3D.ZERO);

        // check L2 and Jupiter are aligned as seen from Sun
        Assert.assertEquals(0.0, Vector3D.angle(posJupiter, posL2), 1.0e-10);

        // check L2 if at least 50 000 000km farther than Jupiter
        Assert.assertTrue(posL2.getNorm() > posJupiter.getNorm() + 5.0e10);
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
            final Vector3D     sunPositionInL2   = sun.getPVCoordinates(date, l2Frame).getPosition();
            final Vector3D     earthPositionInL2 = earth.getPVCoordinates(date, l2Frame).getPosition();
            Assert.assertEquals(0.0, Vector3D.angle(sunPositionInL2,   Vector3D.MINUS_I), 3.0e-14);
            Assert.assertEquals(0.0, Vector3D.angle(earthPositionInL2, Vector3D.MINUS_I), 3.0e-14);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
