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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GalileoAlmanac;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
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

public class GalileoPropagatorTest {

    private GalileoNavigationMessage goe;

    @BeforeEach
    public void setUp() {
        goe = new GalileoNavigationMessage();
        goe.setPRN(4);
        goe.setWeek(1024);
        goe.setTime(293400.0);
        goe.setSqrtA(5440.602949142456);
        goe.setDeltaN(3.7394414770330066E-9);
        goe.setE(2.4088891223073006E-4);
        goe.setI0(0.9531656087278083);
        goe.setIDot(-2.36081262303612E-10);
        goe.setOmega0(-0.36639513583951266);
        goe.setOmegaDot(-5.7695260382035525E-9);
        goe.setPa(-1.6870064194345724);
        goe.setM0(-0.38716557650888);
        goe.setCuc(-8.903443813323975E-7);
        goe.setCus(6.61797821521759E-6);
        goe.setCrc(194.0625);
        goe.setCrs(-18.78125);
        goe.setCic(3.166496753692627E-8);
        goe.setCis(-1.862645149230957E-8);
        goe.setDate(new GNSSDate(goe.getWeek(), goe.getTime(), SatelliteSystem.GALILEO).getDate());
    }

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testGalileoCycle() {
        // Reference for the almanac: 2019-05-28T09:40:01.0Z
        final GalileoAlmanac almanac = new GalileoAlmanac();
        almanac.setPRN(1);
        almanac.setWeek(1024);
        almanac.setTime(293400.0);
        almanac.setDeltaSqrtA(0.013671875);
        almanac.setE(0.000152587890625);
        almanac.setDeltaInc(0.003356933593);
        almanac.setIOD(4);
        almanac.setOmega0(0.2739257812499857891);
        almanac.setOmegaDot(-1.74622982740407E-9);
        almanac.setPa(0.7363586425);
        almanac.setM0(0.27276611328124);
        almanac.setAf0(-0.0006141662597);
        almanac.setAf1(-7.275957614183E-12);
        almanac.setHealthE1(0);
        almanac.setHealthE5a(0);
        almanac.setHealthE5b(0);
        almanac.setDate(new GNSSDate(almanac.getWeek(), almanac.getTime(), SatelliteSystem.GALILEO).getDate());

        // Intermediate verification
        Assertions.assertEquals(1,                   almanac.getPRN());
        Assertions.assertEquals(1024,                almanac.getWeek());
        Assertions.assertEquals(4,                   almanac.getIOD());
        Assertions.assertEquals(0,                   almanac.getHealthE1());
        Assertions.assertEquals(0,                   almanac.getHealthE5a());
        Assertions.assertEquals(0,                   almanac.getHealthE5b());
        Assertions.assertEquals(-0.0006141662597,    almanac.getAf0(), 1.0e-15);
        Assertions.assertEquals(-7.275957614183E-12, almanac.getAf1(), 1.0e-15);

        // Builds the GalileoPropagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        // Propagate at the Galileo date and one Galileo cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double galCycleDuration = almanac.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(galCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the GalileoPropagator from the ephemeris
        final GNSSPropagator propagator = goe.getPropagator();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986004418e+14, goe.getMu(), 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 2.4e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 2.8e-12);
    }

    @Test
    public void testNoReset() {
        try {
            final GNSSPropagator propagator = goe.getPropagator();
            propagator.resetInitialState(propagator.getInitialState());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            GNSSPropagator propagator = new GNSSPropagatorBuilder(goe).build();
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
        final GNSSPropagator propagator = goe.getPropagator();
        GNSSOrbitalElements elements = propagator.getOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), elements.getTime(), SatelliteSystem.GALILEO).getDate();
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
        Assertions.assertEquals(0.0, errorP, 1.9e-9);
        Assertions.assertEquals(0.0, errorV, 4.4e-8);
        Assertions.assertEquals(0.0, errorA, 1.8e-9);

    }

    @Test
    public void testPosition() {
        // Date of the Galileo orbital elements, 10 April 2019 at 09:30:00 UTC
        final AbsoluteDate target = goe.getDate();
        // Build the Galileo propagator
        final GNSSPropagator propagator = goe.getPropagator();
        // Compute the PV coordinates at the date of the Galileo orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from IGS file WUM0MGXULA_20191010500_01D_15M_ORB.sp3)
        final Vector3D expectedPos = new Vector3D(10487480.721, 17867448.753, -21131462.002);
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 2.1);
    }

    @Test
    public void testIssue544() {
        // Builds the GalileoPropagator from the almanac
        final GNSSPropagator propagator = goe.getPropagator();
        // In order to test the issue, we voluntary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());
    }

}
