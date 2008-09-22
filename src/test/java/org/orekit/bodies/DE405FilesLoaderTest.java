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
import org.orekit.utils.PVCoordinates;

public class DE405FilesLoaderTest extends TestCase {

    public void testConstants() throws OrekitException, ParseException {
         AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TTScale.getInstance());
        DE405FilesLoader loader =
            new DE405FilesLoader(DE405FilesLoader.EphemerisType.MOON, date);
        loader.loadEphemerides();
        assertEquals(149597870691.0, loader.getAstronomicalUnit(), 0.1);
        assertEquals(81.30056, loader.getEarthMoonMassRatio(), 1.0e-8);
    }

    public void testDerivative() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TTScale.getInstance());
        DE405FilesLoader loader =
            new DE405FilesLoader(DE405FilesLoader.EphemerisType.MERCURY, date);
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

    public void testGeocentricPV() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TTScale.getInstance());
        checkGeocentricPV(DE405FilesLoader.EphemerisType.MOON, date,
                          new Vector3D(-0.0008081773279115, -0.0019946300016204, -0.0010872626608381),
                          new Vector3D( 0.0006010848166591, -0.0001674454606152, -0.0000855621449740));
    }

    private void checkGeocentricPV(DE405FilesLoader.EphemerisType type, AbsoluteDate date,
                                   Vector3D position, Vector3D velocity)
    throws OrekitException {

        DE405FilesLoader planetLoader = new DE405FilesLoader(type, date);
        SortedSet<TimeStamped> planetSet = planetLoader.loadEphemerides();
        PosVelChebyshev planetPV = (PosVelChebyshev) planetSet.headSet(date).last();
        PVCoordinates computed = planetPV.getPositionVelocity(date);

        final double au = 149597870691.0;
        final double auPerDay = au / 86400.0;
        PVCoordinates reference =
            new PVCoordinates(new Vector3D(au, position), new Vector3D(auPerDay, velocity));

        PVCoordinates error = new PVCoordinates(1.0, computed, -1.0, reference);
        assertEquals(0, error.getPosition().getNorm(), 2.0e-3);
        assertEquals(0, error.getVelocity().getNorm(), 2.0e-10);

    }

    public void testHeliocentricPV() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TTScale.getInstance());
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.MERCURY, date,
                            new Vector3D( 0.3572602064472754,   -0.0915490424305184, -0.0859810399869404),
                            new Vector3D( 0.0033678456621938,    0.0248893428422493,  0.0129440715867960));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.VENUS, date,
                            new Vector3D( 0.6082494331856039,   -0.3491324431959005, -0.1955443457854069),
                            new Vector3D( 0.0109524201099088,    0.0156125067398625,  0.0063288764517467));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.EARTH_MOON, date,
                            new Vector3D( 0.1160149091391665,   -0.9266055536403852, -0.4018062776069879),
                            new Vector3D( 0.0168116200522023,    0.0017431316879820,  0.0007559737671361));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.MARS, date,
                            new Vector3D(-0.1146885824390927,   -1.3283665308334880, -0.6061551894193808),
                            new Vector3D( 0.0144820048079447,    0.0002372854923607, -0.0002837498361024));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.JUPITER, date,
                            new Vector3D(-5.3842094069921451,   -0.8312476561610838, -0.2250947570335498),
                            new Vector3D( 0.0010923632912185,   -0.0065232941911923, -0.0028230122672194));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.SATURN, date,
                            new Vector3D( 7.8898899338228166,    4.5957107269260122,  1.558431516725089),
                            new Vector3D(-0.0032172034910937,    0.0043306322335557,  0.0019264174637995));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.URANUS, date,
                            new Vector3D(-18.2699008149782607,  -1.1627115802190469, -0.2503695407425549),
                            new Vector3D(  0.0002215401656274,  -0.0037676535582462, -0.0016532438049224));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.NEPTUNE, date,
                            new Vector3D(-16.0595450919244627, -23.9429482908794995, -9.4004227803540061),
                            new Vector3D(  0.0026431227915766,  -0.0015034920807588, -0.0006812710048723));
        checkHeliocentricPV(DE405FilesLoader.EphemerisType.PLUTO, date,
                            new Vector3D(-30.4878221121555448,  -0.8732454301967293,  8.9112969841847551),
                            new Vector3D(  0.0003225621959332,  -0.0031487479275516, -0.0010801779315937));
    }

    private void checkHeliocentricPV(DE405FilesLoader.EphemerisType type, AbsoluteDate date,
                                     Vector3D position, Vector3D velocity)
    throws OrekitException {

        DE405FilesLoader planetLoader = new DE405FilesLoader(type, date);
        SortedSet<TimeStamped> planetSet = planetLoader.loadEphemerides();
        PosVelChebyshev planetPV = (PosVelChebyshev) planetSet.headSet(date).last();

        DE405FilesLoader sunLoader = new DE405FilesLoader(DE405FilesLoader.EphemerisType.SUN, date);
        SortedSet<TimeStamped> sunSet = sunLoader.loadEphemerides();
        PosVelChebyshev sunPV = (PosVelChebyshev) sunSet.headSet(date).last();

        PVCoordinates computed =
            new PVCoordinates( 1.0, planetPV.getPositionVelocity(date),
                              -1.0, sunPV.getPositionVelocity(date));

        final double au = 149597870691.0;
        final double auPerDay = au / 86400.0;
        PVCoordinates reference =
            new PVCoordinates(new Vector3D(au, position), new Vector3D(auPerDay, velocity));

        PVCoordinates error = new PVCoordinates(1.0, computed, -1.0, reference);
        assertEquals(0, error.getPosition().getNorm(), 2.0e-3);
        assertEquals(0, error.getVelocity().getNorm(), 2.0e-10);

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
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(DE405FilesLoaderTest.class);
    }

}
