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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.GPSAlmanac;
import org.orekit.gnss.SEMParser;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class GPSPropagatorTest {

    private static List<GPSAlmanac> almanacs;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.GPS_EPOCH, 7 * 512));
        // Get the parser to read a SEM file
        SEMParser reader = new SEMParser(null);
        // Reads the SEM file
        reader.loadData();
        // Gets the first SEM almanac
        almanacs = reader.getAlmanacs();
    }

    @Test
    public void testClockCorrections() {
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        propagator.addAdditionalStateProvider(new ClockCorrectionsProvider(almanacs.get(0)));
        // Propagate at the GPS date and one GPS cycle later
        final AbsoluteDate date0 = almanacs.get(0).getDate();
        double dtRelMin = 0;
        double dtRelMax = 0;
        for (double dt = 0; dt < 0.5 * Constants.JULIAN_DAY; dt += 1.0) {
            SpacecraftState state = propagator.propagate(date0.shiftedBy(dt));
            double[] corrections = state.getAdditionalState(ClockCorrectionsProvider.CLOCK_CORRECTIONS);
            Assert.assertEquals(3, corrections.length);
            Assert.assertEquals(1.33514404296875E-05, corrections[0], 1.0e-19);
            dtRelMin = FastMath.min(dtRelMin, corrections[1]);
            dtRelMax = FastMath.max(dtRelMax, corrections[1]);
            Assert.assertEquals(0.0, corrections[2], Precision.SAFE_MIN);
        }
        Assert.assertEquals(-1.1679e-8, dtRelMin, 1.0e-12);
        Assert.assertEquals(+1.1679e-8, dtRelMax, 1.0e-12);
    }

    @Test
    public void testGPSCycle() {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        // Propagate at the GPS date and one GPS cycle later
        final AbsoluteDate date0 = almanacs.get(0).getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = GPSOrbitalElements.GPS_WEEK_IN_SECONDS * GPSOrbitalElements.GPS_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        Assert.assertEquals("EME2000", propagator.getFrame().getName());
        Assert.assertEquals(3.986005e14, GPSOrbitalElements.GPS_MU, 1.0e6);
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
            GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testTLE() {

        List<GPSPropagator> gpsPropagators = new ArrayList<GPSPropagator>();
        for (final GPSAlmanac almanac : almanacs) {
            gpsPropagators.add(new GPSPropagator.Builder(almanac).build());
        }

        // the following map corresponds to the GPS constellation status in early 2016
        final Map<Integer, TLE> prnToTLE = new HashMap<Integer, TLE>();
        prnToTLE.put( 1,
                     new TLE("1 37753U 11036A   16059.51505483 -.00000016  00000-0  00000+0 0  9995",
                             "2 37753  55.2230 119.7200 0049958  23.9363 306.3749  2.00566105 33828"));
        prnToTLE.put( 2,
                     new TLE("1 28474U 04045A   16059.68518942 -.00000018 +00000-0 +00000-0 0  9992",
                             "2 28474 054.0048 117.2153 0156693 236.8152 092.4773 02.00556694082981"));
        prnToTLE.put( 3,
                     new TLE("1 40294U 14068A   16059.64183862 +.00000012 +00000-0 +00000-0 0  9990",
                             "2 40294 054.9632 179.3690 0003634 223.4011 136.5596 02.00553679009768"));
        prnToTLE.put( 4,
                      new TLE("1 34661U 09014A   16059.51658765 -.00000072  00000-0  00000+0 0  9997",
                              "2 34661  56.3471   0.6400 0080177  48.0430 129.2467  2.00572451 50844"));
        prnToTLE.put( 5,
                      new TLE("1 35752U 09043A   16059.39819238  .00000011  00000-0  00000+0 0  9993",
                              "2 35752  54.2243 178.7652 0044753  24.8598  29.4422  2.00555538 47893"));
        prnToTLE.put( 6,
                      new TLE("1 39741U 14026A   16059.18044747 -.00000016  00000-0  00000+0 0  9990",
                              "2 39741  55.2140 119.2493 0005660 259.2190 100.7882  2.00566982 13061"));
        prnToTLE.put( 7,
                      new TLE("1 32711U 08012A   16059.36304856 -.00000033 +00000-0 +00000-0 0  9998",
                              "2 32711 055.4269 300.7399 0091867 207.6311 151.9340 02.00564257058321"));
        prnToTLE.put( 8,
                      new TLE("1 40730U 15033A   16059.44106931 -.00000026 +00000-0 +00000-0 0  9994",
                              "2 40730 055.1388 059.0069 0020452 282.1769 077.6168 02.00566073004562"));
        prnToTLE.put( 9,
                      new TLE("1 40105U 14045A   16059.27451329  .00000045  00000-0  00000+0 0  9996",
                              "2 40105  54.7529 238.9873 0004485 121.4766 238.5557  2.00568637 11512"));
        prnToTLE.put(10,
                     new TLE("1 41019U 15062A   16059.49433942  .00000013  00000-0  00000+0 0  9991",
                             "2 41019  54.9785 179.1399 0012328 204.9013 155.0292  2.00561967  2382"));
        prnToTLE.put(11,
                     new TLE("1 25933U 99055A   16059.51073770 -.00000024  00000-0  00000+0 0  9997",
                             "2 25933  51.3239  98.4815 0159812  86.1576 266.7718  2.00565163120122"));
        prnToTLE.put(12,
                     new TLE("1 29601U 06052A   16059.62966898 -.00000070 +00000-0 +00000-0 0  9994",
                             "2 29601 056.7445 002.2755 0057667 037.0706 323.3313 02.00552237067968"));
        prnToTLE.put(13,
                     new TLE("1 24876U 97035A   16059.41696335  .00000046  00000-0  00000+0 0  9998",
                             "2 24876  55.6966 245.8203 0044339 114.8899 245.5712  2.00562657136305"));
        prnToTLE.put(14,
                     new TLE("1 26605U 00071A   16059.56211888  .00000047  00000-0  00000+0 0  9997",
                             "2 26605  55.2663 243.7251 0085518 248.7231  95.5323  2.00557009112094"));
        prnToTLE.put(15,
                     new TLE("1 32260U 07047A   16059.45678257 +.00000044 +00000-0 +00000-0 0  9994",
                             "2 32260 053.3641 236.0940 0079746 026.4105 333.9774 02.00547771061402"));
        prnToTLE.put(16,
                     new TLE("1 27663U 03005A   16059.14440417 -.00000071  00000-0  00000+0 0  9996",
                             "2 27663  56.7743   3.3691 0085346  17.6322 214.4333  2.00559487 95843"));
        prnToTLE.put(17,
                     new TLE("1 28874U 05038A   16059.21070933 -.00000024  00000-0  00000+0 0  9997",
                             "2 28874  55.8916  61.9596 0112077 248.9647 205.7384  2.00567116 76379"));
        prnToTLE.put(18,
                     new TLE("1 26690U 01004A   16059.51332910  .00000008  00000-0  00000+0 0  9990",
                             "2 26690  52.9999 177.6630 0169501 250.8579 153.6293  2.00563995110499"));
        prnToTLE.put(19,
                     new TLE("1 28190U 04009A   16058.12363503 -.00000030  00000-0  00000+0 0  9999",
                             "2 28190  55.7230  64.8110 0105865  40.0254 321.4519  2.00572212 87500"));
        prnToTLE.put(20,
                     new TLE("1 26360U 00025A   16059.44770263  .00000005  00000-0  00000+0 0  9992",
                             "2 26360  53.0712 174.6895 0046205  76.0615 334.4302  2.00559931115818"));
        prnToTLE.put(21,
                     new TLE("1 27704U 03010A   16059.50719524 -.00000019  00000-0  00000+0 0  9998",
                             "2 27704  53.6134 117.9454 0234081 255.6874 199.2128  2.00564673 94659"));
        prnToTLE.put(22,
                     new TLE("1 28129U 03058A   16059.06680941  .00000008  00000-0  00000+0 0  9990",
                             "2 28129  52.8771 177.7253 0079127 245.1376 114.0279  2.00398763 89357"));
        prnToTLE.put(23,
                     new TLE("1 28361U 04023A   16059.54310021  .00000046  00000-0  00000+0 0  9995",
                             "2 28361  54.2347 239.3240 0106509 211.5355  11.7648  2.00557932 85613"));
        prnToTLE.put(24,
                     new TLE("1 38833U 12053A   16059.04618549 -.00000032  00000-0  00000+0 0  9999",
                             "2 38833  54.4591 298.1383 0042253  18.7074 341.5041  2.00568407 24895"));
        prnToTLE.put(25,
                     new TLE("1 36585U 10022A   16059.29300735 -.00000074  00000-0  00000+0 0  9993",
                             "2 36585  56.0738 359.4320 0050768  38.3425  49.1794  2.00578535 42134"));
        prnToTLE.put(26,
                     new TLE("1 40534U 15013A   16059.28299301 -.00000076  00000-0  00000+0 0  9994",
                             "2 40534  55.0430 359.0082 0009349 342.4081  17.5685  2.00558853  6801"));
        prnToTLE.put(27,
                     new TLE("1 39166U 13023A   16059.40401153 -.00000025  00000-0  00000+0 0  9990",
                             "2 39166  55.6020  59.1224 0032420   7.7969 352.2759  2.00568484 20414"));
        prnToTLE.put(28,
                     new TLE("1 26407U 00040A   16059.80383354 -.00000069 +00000-0 +00000-0 0  9994",
                             "2 26407 056.6988 003.6328 0201499 267.0948 317.6209 02.00569902114508"));
        prnToTLE.put(29,
                     new TLE("1 32384U 07062A   16059.44770263 -.00000021  00000-0  00000+0 0  9992",
                             "2 32384  55.9456  62.5022 0011922 319.9531 172.6730  2.00571577 60128"));
        prnToTLE.put(30,
                     new TLE("1 39533U 14008A   16059.40267873 -.00000038 +00000-0 +00000-0 0  9996",
                             "2 39533 054.6126 303.3404 0017140 179.4267 180.6311 02.00568364014251"));
        prnToTLE.put(31,
                     new TLE("1 29486U 06042A   16059.50651990 -.00000032  00000-0  00000+0 0  9992",
                             "2 29486  55.7041 301.2472 0084115 334.2804 254.9897  2.00560606 69098"));
        prnToTLE.put(32,
                     new TLE("1 41328U 16007A   16059.56873502  .00000049  00000-0  00000+0 0  9991",
                             "2 41328  55.0137 239.0304 0002157 298.9074  61.0768  1.99172830   453"));

        for (final GPSPropagator gpsPropagator : gpsPropagators) {
            final int prn = gpsPropagator.getGPSOrbitalElements().getPRN();
            TLE tle = prnToTLE.get(prn);
            TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
            for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
                final AbsoluteDate date = tlePropagator.getInitialState().getDate().shiftedBy(dt);
                final PVCoordinates gpsPV = gpsPropagator.getPVCoordinates(date, gpsPropagator.getECI());
                final PVCoordinates tlePV = tlePropagator.getPVCoordinates(date, gpsPropagator.getECI());
                Assert.assertEquals(0.0,
                                    Vector3D.distance(gpsPV.getPosition(), tlePV.getPosition()),
                                    8400.0);
            }
        }
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        for (final GPSAlmanac almanac : almanacs) {
            GPSPropagator propagator = new GPSPropagator.Builder(almanac).build();
            GPSOrbitalElements elements = propagator.getGPSOrbitalElements();
            AbsoluteDate t0 = new GNSSDate(elements.getWeek(), 0.001 * elements.getTime(), SatelliteSystem.GPS).getDate();
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
        }
        Assert.assertEquals(0.0, errorP, 3.8e-9);
        Assert.assertEquals(0.0, errorV, 3.5e-8);
        Assert.assertEquals(0.0, errorA, 1.1e-8);

    }

    @Test
    public void testPosition() {
        // Initial GPS orbital elements (Ref: IGS)
        GPSOrbitalElements goe = new GPSEphemeris(7, 0, 288000, 5153.599830627441,
                                                  0.012442796607501805, 4.419469802942352E-9,0.9558937988021613,
                                                  -2.4608167886110235E-10, 1.0479401362158658, -7.967117576712062E-9,
                                                  -2.4719019944000538, -1.0899023379614294, 4.3995678424835205E-6,
                                                  1.002475619316101E-5, 183.40625, 87.03125, 3.203749656677246E-7,
                                                  4.0978193283081055E-8);

        // Date of the GPS orbital elements
        final AbsoluteDate target = goe.getDate();
        // Build the GPS propagator
        final GPSPropagator propagator = new GPSPropagator.Builder(goe).build();
        // Compute the PV coordinates at the date of the GPS orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from IGS file igu20484_00.sp3)
        final Vector3D expectedPos = new Vector3D(-4920705.292, 24248099.200, 9236130.101);

        Assert.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 3.2);
    }

    @Test
    public void testIssue544() {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        // In order to test the issue, we volontary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assert.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assert.assertEquals(Vector3D.NaN, pv0.getVelocity());
        
    }

    private class GPSEphemeris implements GPSOrbitalElements {

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
        GPSEphemeris(int prn, int week, double toe, double sqa, double ecc,
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
            return FastMath.sqrt(GPS_MU / absA) / absA + deltaN;
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
            return new GNSSDate(week, toe * 1000., SatelliteSystem.GPS).getDate();
        }
        
    }

}
