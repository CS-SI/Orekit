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
import org.orekit.data.DataContext;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class SBASPropagatorTest {

    /** Threshold for test validation. */
    private static double eps = 1.0e-15;

    /** SBAS orbital elements. */
    private SBASNavigationData soe;
    private Frames frames;
    
    @Before
    public void setUp() {
        // Reference data are taken from IGS file brdm0370.17p
        soe = new SBASNavigationData(127, 1935, 1.23303e+05,
                                     2.406022248000e+07, -2.712500000000e-01, 3.250000000000e-04,
                                     3.460922568000e+07, 3.063125000000e-00, -1.500000000000e-04,
                                     1.964040000000e+04, 1.012000000000e-00, -1.250000000000e-04);
        frames = DataContext.getDefault().getFrames();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testPropagationAtReferenceTime() {
        // SBAS propagator
        final SBASPropagator propagator = new SBASPropagator.
                        Builder(soe, frames).
                        attitudeProvider(InertialProvider.EME2000_ALIGNED).
                        mu(SBASOrbitalElements.SBAS_MU).
                        mass(SBASPropagator.DEFAULT_MASS).
                        eci(FramesFactory.getEME2000()).
                        ecef(FramesFactory.getITRF(IERSConventions.IERS_2010, true)).
                        build();
        // Propagation
        final PVCoordinates pv = propagator.propagateInEcef(soe.getDate());
        // Position/Velocity/Acceleration
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();
        final Vector3D acceleration = pv.getAcceleration();
        // Verify
        Assert.assertEquals(soe.getX(),       position.getX(),     eps);
        Assert.assertEquals(soe.getY(),       position.getY(),     eps);
        Assert.assertEquals(soe.getZ(),       position.getZ(),     eps);
        Assert.assertEquals(soe.getXDot(),    velocity.getX(),     eps);
        Assert.assertEquals(soe.getYDot(),    velocity.getY(),     eps);
        Assert.assertEquals(soe.getZDot(),    velocity.getZ(),     eps);
        Assert.assertEquals(soe.getXDotDot(), acceleration.getX(), eps);
        Assert.assertEquals(soe.getYDotDot(), acceleration.getY(), eps);
        Assert.assertEquals(soe.getZDotDot(), acceleration.getZ(), eps);
    }

    @Test
    public void testPropagation() {
        // SBAS propagator
        final SBASPropagator propagator = new SBASPropagator.Builder(soe, frames).build();
        // Propagation
        final PVCoordinates pv = propagator.propagateInEcef(soe.getDate().shiftedBy(1.0));
        // Position/Velocity/Acceleration
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();
        final Vector3D acceleration = pv.getAcceleration();
        // Verify
        Assert.assertEquals(24060222.2089125, position.getX(),     eps);
        Assert.assertEquals(34609228.7430500, position.getY(),     eps);
        Assert.assertEquals(19641.4119375,    position.getZ(),     eps);
        Assert.assertEquals(-0.270925,        velocity.getX(),     eps);
        Assert.assertEquals(3.062975,         velocity.getY(),     eps);
        Assert.assertEquals(1.011875,         velocity.getZ(),     eps);
        Assert.assertEquals(soe.getXDotDot(), acceleration.getX(), eps);
        Assert.assertEquals(soe.getYDotDot(), acceleration.getY(), eps);
        Assert.assertEquals(soe.getZDotDot(), acceleration.getZ(), eps);
    }

    @Test
    public void testFrames() {
        // Builds the SBAS propagator from the ephemeris
        final SBASPropagator propagator = new SBASPropagator.Builder(soe, frames).build();
        Assert.assertEquals("EME2000", propagator.getFrame().getName());
        Assert.assertEquals(3.986005e+14, propagator.getMU(), 1.0e6);
        Assert.assertEquals(propagator.getECI().getName(), propagator.getFrame().getName());
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2017, 2, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 7.7e-9);
        Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.8e-12);
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final SBASPropagator propagator = new SBASPropagator.Builder(soe, frames).build();
        SBASOrbitalElements elements = propagator.getSBASOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), 0.001 * elements.getTime(), SatelliteSystem.SBAS).getDate();
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
        Assert.assertEquals(0.0, errorV, 6.7e-8);
        Assert.assertEquals(0.0, errorA, 1.8e-8);

    }

    @Test
    public void testNoReset() {
        try {
            final SBASPropagator propagator = new SBASPropagator.Builder(soe, frames).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            final SBASPropagator propagator = new SBASPropagator.Builder(soe, frames).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    /** SBAS orbital elements as read from navigation data files. */
    private class SBASNavigationData implements SBASOrbitalElements {

        private int prn;
        private int week;
        private double time;
        private double x;
        private double xDot;
        private double xDotDot;
        private double y;
        private double yDot;
        private double yDotDot;
        private double z;
        private double zDot;
        private double zDotDot;
        
        /**
         * Constructor.
         * @param prn prn code of the satellote
         * @param week week number
         * @param time reference time (s)
         * @param x ECEF-X component of satellite coordinates (m)
         * @param xDot ECEF-X component of satellite velocity (m/s)
         * @param xDotDot ECEF-X component of satellite acceleration (m/s²)
         * @param y ECEF-Y component of satellite coordinates (m)
         * @param yDot ECEF-Y component of satellite velocity (m/s)
         * @param yDotDot ECEF-Y component of satellite acceleration (m/s²)
         * @param z ECEF-Z component of satellite coordinates (m)
         * @param zDot ECEF-Z component of satellite velocity (m/s)
         * @param zDotDot ECEF-Z component of satellite acceleration (m/s²)
         */
        public SBASNavigationData(final int prn, final int week, final double time,
                                  final double x, final double xDot, final double xDotDot,
                                  final double y, final double yDot, final double yDotDot,
                                  final double z, final double zDot, final double zDotDot) {
            this.prn = prn;
            this.week = week;
            this.time = time;
            this.x = x;
            this.xDot = xDot;
            this.xDotDot = xDotDot;
            this.y = y;
            this.yDot = yDot;
            this.yDotDot = yDotDot;
            this.z = z;
            this.zDot = zDot;
            this.zDotDot = zDotDot;
        }

        @Override
        public AbsoluteDate getDate() {
            return new GNSSDate(week, time * 1000.0, SatelliteSystem.SBAS).getDate();
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
            return time;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getXDot() {
            return xDot;
        }

        @Override
        public double getXDotDot() {
            return xDotDot;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getYDot() {
            return yDot;
        }

        @Override
        public double getYDotDot() {
            return yDotDot;
        }

        @Override
        public double getZ() {
            return z;
        }

        @Override
        public double getZDot() {
            return zDot;
        }

        @Override
        public double getZDotDot() {
            return zDotDot;
        }
        
    }
}
