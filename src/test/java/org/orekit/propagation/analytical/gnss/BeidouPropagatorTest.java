/* Copyright 2002-2019 CS Systèmes d'Information
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

}
