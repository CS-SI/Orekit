/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.QZSSAlmanac;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;

public class QZSSPropagatorTest {

    private static QZSSAlmanac almanac;

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");

        // Almanac for satellite 193 for May 27th 2019 (q201914.alm)
        almanac = new QZSSAlmanac();
        almanac.setPRN(193);
        almanac.setWeek(7);
        almanac.setTime(348160.0);
        almanac.setSqrtA(6493.145996);
        almanac.setE(7.579761505E-02);
        almanac.setI0(0.7201680272);
        almanac.setOmega0(-1.643310999);
        almanac.setOmegaDot(-3.005839491E-09);
        almanac.setPa(-1.561775201);
        almanac.setM0(-4.050903957E-01);
        almanac.setAf0(-2.965927124E-04);
        almanac.setAf1(7.275957614E-12);
        almanac.setHealth(0);
        almanac.setDate(new GNSSDate(almanac.getWeek(), almanac.getTime(), SatelliteSystem.QZSS).getDate());

    }

    @Test
    public void testQZSSCycle() {
        // Builds the QZSS propagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        // Propagate at the QZSS date and one QZSS cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double bdtCycleDuration = almanac.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(bdtCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the QZSS propagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986005e+14, almanac.getMu(), 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 3.3e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.9e-12);
    }

    @Test
    public void testNoReset() {
        try {
            final GNSSPropagator propagator = almanac.getPropagator();
            propagator.resetInitialState(propagator.getInitialState());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            GNSSPropagator propagator = new GNSSPropagatorBuilder(almanac).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final GNSSPropagator propagator = almanac.getPropagator();
        GNSSOrbitalElements elements = propagator.getOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), elements.getTime(), SatelliteSystem.QZSS).getDate();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate central = t0.shiftedBy(dt);
            final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
            final double h = 60.0;
            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (int i = -3; i <= 3; ++i) {
                sample.add(propagator.getPVCoordinates(central.shiftedBy(i * h), eme2000));
            }

            // create interpolator
            final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                    new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

            final PVCoordinates interpolated = interpolator.interpolate(central, sample);
            errorP = FastMath.max(errorP, Vector3D.distance(pv.getPosition(), interpolated.getPosition()));
            errorV = FastMath.max(errorV, Vector3D.distance(pv.getVelocity(), interpolated.getVelocity()));
            errorA = FastMath.max(errorA, Vector3D.distance(pv.getAcceleration(), interpolated.getAcceleration()));
        }

        Assertions.assertEquals(0.0, errorP, 3.8e-9);
        Assertions.assertEquals(0.0, errorV, 8.4e-8);
        Assertions.assertEquals(0.0, errorA, 2.1e-8);

    }

    @Test
    public void testPosition() {
        // Initial QZSS orbital elements (Ref: IGS)
        final QZSSLegacyNavigationMessage qoe = new QZSSLegacyNavigationMessage();
        qoe.setPRN(195);
        qoe.setWeek(21);
        qoe.setTime(226800.0);
        qoe.setSqrtA(6493.226968765259);
        qoe.setE(0.07426900835707784);
        qoe.setDeltaN(4.796628370253418E-10);
        qoe.setI0(0.7116940567084221);
        qoe.setIDot(4.835915721014987E-10);
        qoe.setOmega0(0.6210371871830609);
        qoe.setOmegaDot(-8.38963517626603E-10);
        qoe.setPa(-1.5781555771543598);
        qoe.setM0(1.077008903618136);
        qoe.setCuc(-8.8568776845932E-6);
        qoe.setCus(1.794286072254181E-5);
        qoe.setCrc(-344.03125);
        qoe.setCrs(-305.6875);
        qoe.setCic(1.2032687664031982E-6);
        qoe.setCis(-2.6728957891464233E-6);
        qoe.setDate(new GNSSDate(qoe.getWeek(), qoe.getTime(), SatelliteSystem.QZSS).getDate());
        // Date of the QZSS orbital elements
        final AbsoluteDate target = qoe.getDate();
        // Build the QZSS propagator
        final GNSSPropagator propagator = qoe.getPropagator();
        // Compute the PV coordinates at the date of the QZSS orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from QZSS sp3 file qzu20693_00.sp3)
        final Vector3D expectedPos = new Vector3D(-35047225.493, 18739632.916, -9522204.569);
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 0.7);
    }

    @Test
    public void testIssue544() {
        // Builds the QZSSPropagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        // In order to test the issue, we voluntary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());

    }
}
