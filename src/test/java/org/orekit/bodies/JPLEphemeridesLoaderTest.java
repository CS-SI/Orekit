/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.bodies;


import java.text.ParseException;
import java.util.SortedSet;

import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;

public class JPLEphemeridesLoaderTest {

    @Test
    public void testConstants() throws OrekitException {
        Assert.assertEquals(149597870691.0, JPLEphemeridesLoader.getAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30056, JPLEphemeridesLoader.getEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testGM() throws OrekitException {
        Assert.assertEquals(22032.080e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
                     1.0e6);
        Assert.assertEquals(324858.599e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
                     1.0e6);
        Assert.assertEquals(42828.314e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
                     1.0e6);
        Assert.assertEquals(126712767.863e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
                     6.0e7);
        Assert.assertEquals(37940626.063e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
                     2.0e6);
        Assert.assertEquals(5794549.007e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
                     1.0e6);
        Assert.assertEquals(6836534.064e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
                     1.0e6);
        Assert.assertEquals(981.601e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
                     1.0e6);
        Assert.assertEquals(132712440017.987e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
                     1.0e6);
        Assert.assertEquals(4902.801e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
                     1.0e6);
        Assert.assertEquals(403503.233e9,
                     JPLEphemeridesLoader.getGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
                     1.0e6);
    }

    @Test
    public void testDerivative405() throws OrekitException, ParseException {
        checkDerivative(new AbsoluteDate(1969, 6, 28, TimeScalesFactory.getTT()));
    }

    @Test
    public void testDerivative406() throws OrekitException, ParseException {
        checkDerivative(new AbsoluteDate(2964, 9, 26, TimeScalesFactory.getTT()));
    }

    private void checkDerivative(AbsoluteDate date) throws OrekitException, ParseException {
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.EphemerisType.MERCURY, date);
        SortedSet<TimeStamped> set = loader.loadEphemerides();
        double h = 10;

        // four points finite differences estimation of the velocity
        Vector3D pm2h = getPosition(set, new AbsoluteDate(date, -2 * h));
        Vector3D pm1h = getPosition(set, new AbsoluteDate(date,     -h));
        Vector3D pp1h = getPosition(set, new AbsoluteDate(date,      h));
        Vector3D pp2h = getPosition(set, new AbsoluteDate(date,  2 * h));
        double c = 1.0 / (12 * h);
        Vector3D estimatedV = new Vector3D(c, pm2h, -8 * c, pm1h, 8 * c, pp1h, -c, pp2h);

        Assert.assertEquals(0, getVelocity(set, date).subtract(estimatedV).getNorm(), 1.0e-5);

    }

    private Vector3D getPosition(SortedSet<TimeStamped> set, AbsoluteDate date) {
        PosVelChebyshev pv = (PosVelChebyshev) set.headSet(date).last();
        return pv.getPositionVelocity(date).getPosition();        
    }

    private Vector3D getVelocity(SortedSet<TimeStamped> set, AbsoluteDate date) {
        PosVelChebyshev pv = (PosVelChebyshev) set.headSet(date).last();
        return pv.getPositionVelocity(date).getVelocity();        
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
