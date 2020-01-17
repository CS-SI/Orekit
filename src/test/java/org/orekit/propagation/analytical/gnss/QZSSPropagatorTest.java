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
import org.orekit.gnss.QZSSAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class QZSSPropagatorTest {

    private static QZSSOrbitalElements almanac;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");

        // Almanac for satellite 193 for May 27th 2019 (q201914.alm)
        almanac = new QZSSAlmanac(null, 193, 7, 348160.0, 6493.145996,
                                  7.579761505E-02, 0.7201680272, -1.643310999,
                                  -3.005839491E-09, -1.561775201, -4.050903957E-01,
                                  -2.965927124E-04, 7.275957614E-12, 0);
    }

    @Test
    public void testQZSSCycle() {
        // Builds the QZSS propagator from the almanac
        final QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
        // Propagate at the QZSS date and one QZSS cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double bdtCycleDuration = QZSSOrbitalElements.QZSS_WEEK_IN_SECONDS * QZSSOrbitalElements.QZSS_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(bdtCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the QZSS propagator from the almanac
        final QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
        Assert.assertEquals("EME2000", propagator.getFrame().getName());
        Assert.assertEquals(3.986005e+14, QZSSOrbitalElements.QZSS_MU, 1.0e6);
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
            QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
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
        QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
        QZSSOrbitalElements elements = propagator.getQZSSOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), 0.001 * elements.getTime(), SatelliteSystem.QZSS).getDate();
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
        Assert.assertEquals(0.0, errorV, 8.4e-8);
        Assert.assertEquals(0.0, errorA, 2.1e-8);

    }

    @Test
    public void testPosition() {
        // Initial QZSS orbital elements (Ref: IGS)
        final QZSSOrbitalElements qoe = new QZSSEphemeris(195, 21, 226800.0, 6493.226968765259, 0.07426900835707784,
                                                          4.796628370253418E-10, 0.7116940567084221, 4.835915721014987E-10,
                                                          0.6210371871830609, -8.38963517626603E-10, -1.5781555771543598,
                                                          1.077008903618136, -8.8568776845932E-6, 1.794286072254181E-5,
                                                          -344.03125, -305.6875, 1.2032687664031982E-6, -2.6728957891464233E-6);
        // Date of the QZSS orbital elements
        final AbsoluteDate target = qoe.getDate();
        // Build the QZSS propagator
        final QZSSPropagator propagator = new QZSSPropagator.Builder(qoe).build();
        // Compute the PV coordinates at the date of the QZSS orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from QZSS sp3 file qzu20693_00.sp3)
        final Vector3D expectedPos = new Vector3D(-35047225.493, 18739632.916, -9522204.569);
        Assert.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 0.7);
    }

    @Test
    public void testIssue544() {
        // Builds the QZSSPropagator from the almanac
        final QZSSPropagator propagator = new QZSSPropagator.Builder(almanac).build();
        // In order to test the issue, we volontary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assert.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assert.assertEquals(Vector3D.NaN, pv0.getVelocity());
        
    }

    private class QZSSEphemeris implements QZSSOrbitalElements {

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
        QZSSEphemeris(int prn, int week, double toe, double sqa, double ecc,
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
            return FastMath.sqrt(QZSS_MU / absA) / absA + deltaN;
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
            return new GNSSDate(week, toe * 1000., SatelliteSystem.QZSS).getDate();
        }
        
    }

}
