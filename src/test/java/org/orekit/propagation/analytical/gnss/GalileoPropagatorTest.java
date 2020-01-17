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
package org.orekit.propagation.analytical.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.GalileoAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class GalileoPropagatorTest {

    private GalileoOrbitalElements goe;

    @Before
    public void setUp() {
        // Input parameters (reference IGS, 12 April 2019 at 09:30:00 UTC)
        goe = new GalileoEphemeris(4, 1024, 293400.0, 5440.602949142456,
                                   3.7394414770330066E-9, 2.4088891223073006E-4, 0.9531656087278083,
                                   -2.36081262303612E-10, -0.36639513583951266, -5.7695260382035525E-9,
                                   -1.6870064194345724, -0.38716557650888, -8.903443813323975E-7,
                                   6.61797821521759E-6, 194.0625, -18.78125,
                                   3.166496753692627E-8, -1.862645149230957E-8);
    }
    
    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testGalileoCycle() {
        // Reference for the almanac: 2019-05-28T09:40:01.0Z
        final GalileoAlmanac almanac = new GalileoAlmanac(1, 1024, 293400.0, 0.013671875,
                                                          0.000152587890625, 0.003356933593, 4,
                                                          0.2739257812499857891, -1.74622982740407E-9,
                                                          0.7363586425, 0.27276611328124, -0.0006141662597,
                                                          -7.275957614183E-12, 0, 0, 0);
        // Intermediate verification
        Assert.assertEquals(1,                   almanac.getPRN());
        Assert.assertEquals(1024,                almanac.getWeek());
        Assert.assertEquals(4,                   almanac.getIOD());
        Assert.assertEquals(0,                   almanac.getHealthE1());
        Assert.assertEquals(0,                   almanac.getHealthE5a());
        Assert.assertEquals(0,                   almanac.getHealthE5b());
        Assert.assertEquals(-0.0006141662597,    almanac.getAf0(), 1.0e-15);
        Assert.assertEquals(-7.275957614183E-12, almanac.getAf1(), 1.0e-15);

        // Builds the GalileoPropagator from the almanac
        GalileoPropagator propagator = new GalileoPropagator.Builder(almanac).build();
        // Propagate at the Galileo date and one Galileo cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double galCycleDuration = GalileoOrbitalElements.GALILEO_WEEK_IN_SECONDS * GalileoOrbitalElements.GALILEO_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(galCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the GalileoPropagator from the ephemeris
        GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
        Assert.assertEquals("EME2000", propagator.getFrame().getName());
        Assert.assertEquals(3.986004418e+14, GalileoOrbitalElements.GALILEO_MU, 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 2.4e-8);
        Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 2.7e-12);
    }

    @Test
    public void testNoReset() {
        try {
            GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
        GalileoOrbitalElements elements = propagator.getGalileoOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), 0.001 * elements.getTime(), SatelliteSystem.GALILEO).getDate();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate central = t0.shiftedBy(dt);
            final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
            final double h = 10.0;
            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (int i = -3; i <= 3; ++i) {
                sample.add(propagator.getPVCoordinates(central.shiftedBy(i * h), eme2000));
            }
            final PVCoordinates interpolated =
                            TimeStampedPVCoordinates.interpolate(central,
                                                                 CartesianDerivativesFilter.USE_P,
                                                                 sample);
            errorP = FastMath.max(errorP, Vector3D.distance(pv.getPosition(), interpolated.getPosition()));
            errorV = FastMath.max(errorV, Vector3D.distance(pv.getVelocity(), interpolated.getVelocity()));
            errorA = FastMath.max(errorA, Vector3D.distance(pv.getAcceleration(), interpolated.getAcceleration()));
        }
        Assert.assertEquals(0.0, errorP, 1.5e-11);
        Assert.assertEquals(0.0, errorV, 2.2e-7);
        Assert.assertEquals(0.0, errorA, 4.9e-8);

    }

    @Test
    public void testPosition() {
        // Date of the Galileo orbital elements, 10 April 2019 at 09:30:00 UTC
        final AbsoluteDate target = goe.getDate();
        // Build the Galileo propagator
        final GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
        // Compute the PV coordinates at the date of the Galileo orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from IGS file WUM0MGXULA_20191010500_01D_15M_ORB.sp3)
        final Vector3D expectedPos = new Vector3D(10487480.721, 17867448.753, -21131462.002);
        Assert.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 2.1);
    }

    @Test
    public void testIssue544() {
        // Builds the GalileoPropagator from the almanac
        final GalileoPropagator propagator = new GalileoPropagator.Builder(goe).build();
        // In order to test the issue, we volontary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assert.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assert.assertEquals(Vector3D.NaN, pv0.getVelocity()); 
    }

    private class GalileoEphemeris implements GalileoOrbitalElements {

        private int satID;
        private int week;
        private double toe;
        private double sma;
        private double deltaN;
        private double ecc;
        private double inc;
        private double iDot;
        private double om0;
        private double dom;
        private double aop;
        private double anom;
        private double cuc;
        private double cus;
        private double crc;
        private double crs;
        private double cic;
        private double cis;

        /**
         * Build a new instance.
         */
        public GalileoEphemeris(int satID, int week, double toe, double sqa,
                                double deltaN, double ecc, double inc,
                                double iDot, double om0, double dom, double aop,
                                double anom, double cuc, double cus, double crc,
                                double crs, double cic, double cis) {
            this.satID = satID;
            this.week = week;
            this.toe = toe;
            this.sma = sqa * sqa;
            this.deltaN = deltaN;
            this.ecc = ecc;
            this.inc = inc;
            this.iDot = iDot;
            this.om0 = om0;
            this.dom = dom;
            this.aop = aop;
            this.anom = anom;
            this.cuc = cuc;
            this.cus = cus;
            this.crc = crc;
            this.crs = crs;
            this.cic = cic;
            this.cis = cis;
        }

        @Override
        public int getPRN() {
            return satID;
        }

        @Override
        public int getWeek() {
            return week;
        }

        @Override
        public double getTime() {
            return toe;
        }

        @Override
        public double getSma() {
            return sma;
        }

        @Override
        public double getMeanMotion() {
            final double absA = FastMath.abs(sma);
            return FastMath.sqrt(GALILEO_MU / absA) / absA + deltaN;
        }

        @Override
        public double getE() {
            return ecc;
        }

        @Override
        public double getI0() {
            return inc;
        }

        @Override
        public double getIDot() {
            return iDot;
        }

        @Override
        public double getOmega0() {
            return om0;
        }

        @Override
        public double getOmegaDot() {
            return dom;
        }

        @Override
        public double getPa() {
            return aop;
        }

        @Override
        public double getM0() {
            return anom;
        }

        @Override
        public double getCuc() {
            return cuc;
        }

        @Override
        public double getCus() {
            return cus;
        }

        @Override
        public double getCrc() {
            return crc;
        }

        @Override
        public double getCrs() {
            return crs;
        }

        @Override
        public double getCic() {
            return cic;
        }

        @Override
        public double getCis() {
            return cis;
        }

        @Override
        public AbsoluteDate getDate() {
            return new GNSSDate(week, toe * 1000., SatelliteSystem.GALILEO).getDate();
        }
        
    }

}
