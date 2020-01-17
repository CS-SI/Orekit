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
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.BeidouAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class BeidouPropagatorTest {

    private static BeidouAlmanac almanac;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");

        // Almanac for satellite 18 for May 28th 2019
        almanac = new BeidouAlmanac(18, 694, 4096, 6493.3076, 0.00482368,
                                    0.0, -0.01365602, 1.40069711, -2.11437379e-9,
                                    3.11461541, -2.53029382, 0.0001096725, 7.27596e-12, 0);
    }

    @Test
    public void testBeidouCycle() {
        // Builds the BeiDou propagator from the almanac
        final BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
        // Intermediate verification
        Assert.assertEquals(18,           almanac.getPRN());
        Assert.assertEquals(0,            almanac.getHealth());
        Assert.assertEquals(0.0001096725, almanac.getAf0(), 1.0e-15);
        Assert.assertEquals(7.27596e-12,  almanac.getAf1(), 1.0e-15);
        // Propagate at the BeiDou date and one BeiDou cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double bdtCycleDuration = BeidouOrbitalElements.BEIDOU_WEEK_IN_SECONDS * BeidouOrbitalElements.BEIDOU_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(bdtCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the BeiDou propagator from the almanac
        final BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
        Assert.assertEquals("EME2000", propagator.getFrame().getName());
        Assert.assertEquals(3.986004418e+14, BeidouOrbitalElements.BEIDOU_MU, 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 3.3e-8);
        Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.9e-12);
    }

    @Test
    public void testNoReset() {
        try {
            BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
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
        BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
        BeidouOrbitalElements elements = propagator.getBeidouOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), 0.001 * elements.getTime(), SatelliteSystem.BEIDOU).getDate();
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

        Assert.assertEquals(0.0, errorP, 3.8e-9);
        Assert.assertEquals(0.0, errorV, 8.0e-8);
        Assert.assertEquals(0.0, errorA, 2.0e-8);

    }

    @Test
    public void testPosition() {
        // Initial BeiDou orbital elements (Ref: IGS)
        final BeidouOrbitalElements boe = new BeidouEphemeris(7, 713, 284400.0, 6492.84515953064, 0.00728036486543715,
                                                              2.1815194404696853E-9, 0.9065628903946735, 0.0, -0.6647664535282437,
                                                              -3.136916379444212E-9, -2.6584351442773304, 0.9614806010234702,
                                                              7.306225597858429E-6, -6.314832717180252E-6, 406.96875,
                                                              225.9375, -7.450580596923828E-9, -1.4062970876693726E-7);
        // Date of the BeiDou orbital elements (GPStime - BDTtime = 14s)
        final AbsoluteDate target = boe.getDate().shiftedBy(-14.0);
        // Build the BeiDou propagator
        final BeidouPropagator propagator = new BeidouPropagator.Builder(boe).build();
        // Compute the PV coordinates at the date of the BeiDou orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from sp3 file WUM0MGXULA_20192470700_01D_05M_ORB.SP33)
        final Vector3D expectedPos = new Vector3D(-10260690.520, 24061180.795, -32837341.074);
        Assert.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 3.1);
    }

    @Test
    public void testIssue544() {
        // Builds the BeidouPropagator from the almanac
        final BeidouPropagator propagator = new BeidouPropagator.Builder(almanac).build();
        // In order to test the issue, we volontary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assert.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assert.assertEquals(Vector3D.NaN, pv0.getVelocity());
        
    }

    private class BeidouEphemeris implements BeidouOrbitalElements {

        private int prn;
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
        BeidouEphemeris(int prn, int week, double toe, double sqa, double ecc,
                        double deltaN, double inc, double iDot, double om0,
                        double dom, double aop, double anom, double cuc,
                        double cus, double crc, double crs, double cic, double cis) {
            this.prn    = prn;
            this.week   = week;
            this.toe    = toe;
            this.sma    = sqa * sqa;
            this.ecc    = ecc;
            this.deltaN = deltaN;
            this.inc    = inc;
            this.iDot   = iDot;
            this.om0    = om0;
            this.dom    = dom;
            this.aop    = aop;
            this.anom   = anom;
            this.cuc    = cuc;
            this.cus    = cus;
            this.crc    = crc;
            this.crs    = crs;
            this.cic    = cic;
            this.cis    = cis;
        }

        @Override
        public int getPRN() {
            return prn;
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
            return FastMath.sqrt(BEIDOU_MU / absA) / absA + deltaN;
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
            return new GNSSDate(week, toe * 1000., SatelliteSystem.BEIDOU).getDate();
        }
        
    }

}
