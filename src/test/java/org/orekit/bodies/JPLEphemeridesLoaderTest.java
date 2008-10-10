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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TTScale;
import org.orekit.time.TimeStamped;

public class JPLEphemeridesLoaderTest extends TestCase {

    public void testConstants() throws OrekitException, ParseException {
         AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TTScale.getInstance());
        JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.EphemerisType.MOON, date);
        loader.loadEphemerides();
        assertEquals(149597870691.0, loader.getAstronomicalUnit(), 0.1);
        assertEquals(81.30056, loader.getEarthMoonMassRatio(), 1.0e-8);
    }

    public void testDerivative405() throws OrekitException, ParseException {
        checkDerivative(new AbsoluteDate(1969, 6, 28, TTScale.getInstance()));
    }

    public void testDerivative406() throws OrekitException, ParseException {
        checkDerivative(new AbsoluteDate(2964, 9, 26, TTScale.getInstance()));
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

        assertEquals(0, getVelocity(set, date).subtract(estimatedV).getNorm(), 1.0e-5);

    }

    private Vector3D getPosition(SortedSet<TimeStamped> set, AbsoluteDate date) {
        PosVelChebyshev pv = (PosVelChebyshev) set.headSet(date).last();
        return pv.getPositionVelocity(date).getPosition();        
    }

    private Vector3D getVelocity(SortedSet<TimeStamped> set, AbsoluteDate date) {
        PosVelChebyshev pv = (PosVelChebyshev) set.headSet(date).last();
        return pv.getPositionVelocity(date).getVelocity();        
    }

    public void setUp() {
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataDirectoryCrawler.OREKIT_DATA_PATH, root);
    }

    public static Test suite() {
        return new TestSuite(JPLEphemeridesLoaderTest.class);
    }

}
