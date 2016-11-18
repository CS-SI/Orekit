/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.drag.atmosphere;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.atmosphere.NRLMSISE00.Output;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;


public class NRLMSISE00Test {

    @Test
    public void testLegacy() throws OrekitException {
        // Build the model
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING, itrf);
        NRLMSISE00 atm = new NRLMSISE00(null, null, earth);

        // Common data for all cases
        final int doy = 172;
        final double sec   = 29000.;
        final double alt   = 400.;
        final double lat   =  60.;
        final double lon   = -70.;
        final double hl    =  16.;
        final double f107a = 150.;
        final double f107  = 150.;
        double[] ap  = {4., 100., 100., 100., 100., 100., 100.};
        final boolean print = false;

        // Case #1
        final NRLMSISE00.Output out1 = atm.gtd7(doy, sec, alt, lat, lon, hl, f107a, f107, ap);
        checkLegacy(1, out1, print);

        // Case #2
        final int doy2 = 81;
        final NRLMSISE00.Output out2 = atm.gtd7(doy2, sec, alt, lat, lon, hl, f107a, f107, ap);
        checkLegacy(2, out2, print);

        // Case #3
        final double sec3 = 75000.;
        final double alt3 = 1000.;
        final NRLMSISE00.Output out3 = atm.gtd7(doy, sec3, alt3, lat, lon, hl, f107a, f107, ap);
        checkLegacy(3, out3, print);

        // Case #4
        final double alt4 = 100.;
        final NRLMSISE00.Output out4 = atm.gtd7(doy, sec, alt4, lat, lon, hl, f107a, f107, ap);
        checkLegacy(4, out4, print);

        // Case #5
        final double lat5 = 0.;
        final NRLMSISE00.Output out5 = atm.gtd7(doy, sec, alt, lat5, lon, hl, f107a, f107, ap);
        checkLegacy(5, out5, print);

        // Case #6
        final double lon6 = 0.;
        final NRLMSISE00.Output out6 = atm.gtd7(doy, sec, alt, lat, lon6, hl, f107a, f107, ap);
        checkLegacy(6, out6, print);

        // Case #7
        final double hl7 = 4.;
        final NRLMSISE00.Output out7 = atm.gtd7(doy, sec, alt, lat, lon, hl7, f107a, f107, ap);
        checkLegacy(7, out7, print);

        // Case #8
        final double f107a8 = 70.;
        final NRLMSISE00.Output out8 = atm.gtd7(doy, sec, alt, lat, lon, hl, f107a8, f107, ap);
        checkLegacy(8, out8, print);

        // Case #9
        final double f1079 = 180.;
        final NRLMSISE00.Output out9 = atm.gtd7(doy, sec, alt, lat, lon, hl, f107a, f1079, ap);
        checkLegacy(9, out9, print);

        // Case #10
        ap[0] = 40.;
        final NRLMSISE00.Output out10 = atm.gtd7(doy, sec, alt, lat, lon, hl, f107a, f107, ap);
        checkLegacy(10, out10, print);
        ap[0] = 4.;

        // Case #11
        final double alt11 =  0.;
        final NRLMSISE00.Output out11 = atm.gtd7(doy, sec, alt11, lat, lon, hl, f107a, f107, ap);
        checkLegacy(11, out11, print);

        // Case #12
        final double alt12 = 10.;
        final NRLMSISE00.Output out12 = atm.gtd7(doy, sec, alt12, lat, lon, hl, f107a, f107, ap);
        checkLegacy(12, out12, print);

        // Case #13
        final double alt13 = 30.;
        final NRLMSISE00.Output out13 = atm.gtd7(doy, sec, alt13, lat, lon, hl, f107a, f107, ap);
        checkLegacy(13, out13, print);

        // Case #14
        final double alt14 = 50.;
        final NRLMSISE00.Output out14 = atm.gtd7(doy, sec, alt14, lat, lon, hl, f107a, f107, ap);
        checkLegacy(14, out14, print);

        // Case #15
        final double alt15 = 70.;
        final NRLMSISE00.Output out15 = atm.gtd7(doy, sec, alt15, lat, lon, hl, f107a, f107, ap);
        checkLegacy(15, out15, print);

        // Case #16
        atm.setSwitch(9, -1);
        final NRLMSISE00.Output out16 = atm.gtd7(doy, sec, alt, lat, lon, hl, f107a, f107, ap);
        checkLegacy(16, out16, print);

        // Case #17
        final double alt17 = 100.;
        final NRLMSISE00.Output out17 = atm.gtd7(doy, sec, alt17, lat, lon, hl, f107a, f107, ap);
        checkLegacy(17, out17, print);
    }

    @Test
    public void testDensity() throws OrekitException {
        // Build the iput params provider
        final InputParams ip = new InputParams();
        // Get Sun
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Get Earth body shape
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Build the model
        final NRLMSISE00 atm = new NRLMSISE00(ip, sun, earth);
        // Build the date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 172),
                                                   new TimeComponents(29000.),
                                                   TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true));
        // Build the position
        final double alt = 400.;
        final double lat =  60.;
        final double lon = -70.;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(lat),
                                                      FastMath.toRadians(lon),
                                                      alt * 1000.);
        final Vector3D pos = earth.transform(point);

        // Run
        atm.setSwitch(9, -1);
        final double rho = atm.getDensity(date, pos, itrf);
        final double lst = 29000. / 3600. - 70. / 15.;
        final double[] ap  = {4., 100., 100., 100., 100., 100., 100.};
        final Output out = atm.gtd7d(172, 29000., 400., 60., -70, lst, 150., 150., ap);
        Assert.assertEquals(rho, out.getDensity(5), rho * 1.e-3);
   }

    private void checkLegacy(final int nb, final NRLMSISE00.Output out, final boolean print) {
        final double[] tInfRef = {1.250540E+03, 1.166754E+03, 1.239892E+03, 1.027318E+03,
                                  1.212396E+03, 1.220146E+03, 1.116385E+03, 1.031247E+03,
                                  1.306052E+03, 1.361868E+03, 1.027318E+03, 1.027318E+03,
                                  1.027318E+03, 1.027318E+03, 1.027318E+03, 1.426412E+03,
                                  1.027318E+03};
        final double[] tAltRef = {1.241416E+03, 1.161710E+03, 1.239891E+03, 2.068878E+02,
                                  1.208135E+03, 1.212712E+03, 1.112999E+03, 1.024848E+03,
                                  1.293374E+03, 1.347389E+03, 2.814648E+02, 2.274180E+02,
                                  2.374389E+02, 2.795551E+02, 2.190732E+02, 1.408608E+03,
                                  1.934071E+02};
        final double[] dHeRef  = {6.665177E+05, 3.407293E+06, 1.123767E+05, 5.411554E+07,
                                  1.851122E+06, 8.673095E+05, 5.776251E+05, 3.740304E+05,
                                  6.748339E+05, 5.528601E+05, 1.375488E+14, 4.427443E+13,
                                  2.127829E+12, 1.412184E+11, 1.254884E+10, 5.196477E+05,
                                  4.260860E+07};
        final double[] dORef   = {1.138806E+08, 1.586333E+08, 6.934130E+04, 1.918893E+11,
                                  1.476555E+08, 1.278862E+08, 6.979139E+07, 4.782720E+07,
                                  1.245315E+08, 1.198041E+08, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 1.274494E+08,
                                  1.241342E+11};
        final double[] dN2Ref  = {1.998211E+07, 1.391117E+07, 4.247105E+01, 6.115826E+12,
                                  1.579356E+07, 1.822577E+07, 1.236814E+07, 5.240380E+06,
                                  2.369010E+07, 3.495798E+07, 2.049687E+19, 6.597567E+18,
                                  3.170791E+17, 2.104370E+16, 1.874533E+15, 4.850450E+07,
                                  4.929562E+12};
        final double[] dO2Ref  = {4.022764E+05, 3.262560E+05, 1.322750E-01, 1.225201E+12,
                                  2.633795E+05, 2.922214E+05, 2.492868E+05, 1.759875E+05,
                                  4.911583E+05, 9.339618E+05, 5.498695E+18, 1.769929E+18,
                                  8.506280E+16, 5.645392E+15, 4.923051E+14, 1.720838E+06,
                                  1.048407E+12};
        final double[] dARRef  = {3.557465E+03, 1.559618E+03, 2.618848E-05, 6.023212E+10,
                                  1.588781E+03, 2.402962E+03, 1.405739E+03, 5.501649E+02,
                                  4.578781E+03, 1.096255E+04, 2.451733E+17, 7.891680E+16,
                                  3.792741E+15, 2.517142E+14, 2.239685E+13, 2.354487E+04,
                                  4.993465E+10};
        final double[] dHRef   = {3.475312E+04, 4.854208E+04, 2.016750E+04, 1.059880E+07,
                                  5.816167E+04, 3.686389E+04, 5.291986E+04, 8.896776E+04,
                                  3.244595E+04, 2.686428E+04, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 2.500078E+04,
                                  8.831229E+06};
        final double[] dNRef   = {4.095913E+06, 4.380967E+06, 5.741256E+03, 2.615737E+05,
                                  5.478984E+06, 3.897276E+06, 1.069814E+06, 1.979741E+06,
                                  5.370833E+06, 4.889974E+06, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 6.279210E+06,
                                  2.252516E+05};
        final double[] dAnORef = {2.667273E+04, 6.956682E+03, 2.374394E+04, 2.819879E-42,
                                  1.264446E+03, 2.667273E+04, 2.667273E+04, 9.121815E+03,
                                  2.667273E+04, 2.805445E+04, 0.000000E+00, 0.000000E+00,
                                  0.000000E+00, 0.000000E+00, 0.000000E+00, 2.667273E+04,
                                  2.415246E-42};
        final double[] rhoRef  = {4.074714E-15, 5.001846E-15, 2.756772E-18, 3.584426E-10,
                                  4.809630E-15, 4.355866E-15, 2.470651E-15, 1.571889E-15,
                                  4.564420E-15, 4.974543E-15, 1.261066E-03, 4.059139E-04,
                                  1.950822E-05, 1.294709E-06, 1.147668E-07, 5.881940E-15,
                                  2.914304E-10};
        final double deltaT = 1.e-2;
        final double deltaD = 5.e-7;
        final int id = nb - 1;
        if (print) {
            System.out.printf("Case #%d\n", nb);
            System.out.printf("Tinf: %E  %E\n", tInfRef[id], out.getTemperature(0));
            System.out.printf("Talt: %E  %E\n", tAltRef[id], out.getTemperature(1));
            System.out.printf("He:   %E  %E\n", dHeRef[id], out.getDensity(0) * 1e-6);
            System.out.printf("O:    %E  %E\n", dORef[id], out.getDensity(1) * 1e-6);
            System.out.printf("N2:   %E  %E\n", dN2Ref[id], out.getDensity(2) * 1e-6);
            System.out.printf("O2:   %E  %E\n", dO2Ref[id], out.getDensity(3) * 1e-6);
            System.out.printf("Ar:   %E  %E\n", dARRef[id], out.getDensity(4) * 1e-6);
            System.out.printf("H:    %E  %E\n", dHRef[id], out.getDensity(6) * 1e-6);
            System.out.printf("N:    %E  %E\n", dNRef[id], out.getDensity(7) * 1e-6);
            System.out.printf("AnO:  %E  %E\n", dAnORef[id], out.getDensity(8) * 1e-6);
            System.out.printf("Rho:  %E  %E\n\n", rhoRef[id], out.getDensity(5) * 1e-3);
        } else {
            Assert.assertEquals(tInfRef[id], out.getTemperature(0), deltaT);
            Assert.assertEquals(tAltRef[id], out.getTemperature(1), deltaT);
            Assert.assertEquals(dHeRef[id],  out.getDensity(0) * 1e-6, dHeRef[id] * deltaD);
            Assert.assertEquals(dORef[id],   out.getDensity(1) * 1e-6, dORef[id] * deltaD);
            Assert.assertEquals(dN2Ref[id],  out.getDensity(2) * 1e-6, dN2Ref[id] * deltaD);
            Assert.assertEquals(dO2Ref[id],  out.getDensity(3) * 1e-6, dO2Ref[id] * deltaD);
            Assert.assertEquals(dARRef[id],  out.getDensity(4) * 1e-6, dARRef[id] * deltaD);
            Assert.assertEquals(dHRef[id],   out.getDensity(6) * 1e-6, dHRef[id] * deltaD);
            Assert.assertEquals(dNRef[id],   out.getDensity(7) * 1e-6, dNRef[id] * deltaD);
            Assert.assertEquals(dAnORef[id], out.getDensity(8) * 1e-6, dAnORef[id] * deltaD);
            Assert.assertEquals(rhoRef[id],  out.getDensity(5) * 1e-3, rhoRef[id] * deltaD);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private static class InputParams implements NRLMSISE00InputParameters {

        /** Serializable UID. */
        private static final long serialVersionUID = 1L;

        /** Constructor. */
        public InputParams() {

        }

        @Override
        public AbsoluteDate getMinDate() throws OrekitException {
            return new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC());
        }

        @Override
        public AbsoluteDate getMaxDate() throws OrekitException {
            return new AbsoluteDate(2003, 12, 31, TimeScalesFactory.getUTC());
        }

        @Override
        public double getDailyFlux(AbsoluteDate date) throws OrekitException {
            return 150.;
        }

        @Override
        public double getAverageFlux(AbsoluteDate date) throws OrekitException {
            return 150.;
        }

        @Override
        public double[] getAp(AbsoluteDate date) throws OrekitException {
            return new double[] {4., 100., 100., 100., 100., 100., 100.};
        }
    }
}
