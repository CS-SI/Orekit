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
import org.orekit.propagation.analytical.gnss.data.BeidouAlmanac;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
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

public class BeidouPropagatorTest {

    private static BeidouAlmanac almanac;

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");

        // Almanac for satellite 18 for May 28th 2019
        almanac = new BeidouAlmanac();
        almanac.setPRN(18);
        almanac.setWeek(694);
        almanac.setTime(4096.0);
        almanac.setSqrtA(6493.3076);
        almanac.setE(0.00482368);
        almanac.setI0(0.0, -0.01365602);
        almanac.setOmega0(1.40069711);
        almanac.setOmegaDot(-2.11437379e-9);
        almanac.setPa(3.11461541);
        almanac.setM0(-2.53029382);
        almanac.setAf0(0.0001096725);
        almanac.setAf1(7.27596e-12);
        almanac.setHealth(0);
        almanac.setDate(new GNSSDate(almanac.getWeek(), almanac.getTime(), SatelliteSystem.BEIDOU).getDate());
    }

    @Test
    public void testBeidouCycle() {
        // Builds the BeiDou propagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        // Intermediate verification
        Assertions.assertEquals(18,           almanac.getPRN());
        Assertions.assertEquals(0,            almanac.getHealth());
        Assertions.assertEquals(0.0001096725, almanac.getAf0(), 1.0e-15);
        Assertions.assertEquals(7.27596e-12,  almanac.getAf1(), 1.0e-15);
        // Propagate at the BeiDou date and one BeiDou cycle later
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
        // Builds the BeiDou propagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986004418e+14, almanac.getMu(), 1.0e6);
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
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), elements.getTime(), SatelliteSystem.BEIDOU).getDate();
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
        Assertions.assertEquals(0.0, errorV, 8.0e-8);
        Assertions.assertEquals(0.0, errorA, 2.0e-8);

    }

    @Test
    public void testPosition() {
        // Initial BeiDou orbital elements (Ref: IGS)
        final BeidouLegacyNavigationMessage boe = new BeidouLegacyNavigationMessage();
        boe.setPRN(7);
        boe.setWeek(713);
        boe.setTime(284400.0);
        boe.setSqrtA(6492.84515953064);
        boe.setE(0.00728036486543715);
        boe.setDeltaN(2.1815194404696853E-9);
        boe.setI0(0.9065628903946735);
        boe.setIDot(0.0);
        boe.setOmega0(-0.6647664535282437);
        boe.setOmegaDot(-3.136916379444212E-9);
        boe.setPa(-2.6584351442773304);
        boe.setM0(0.9614806010234702);
        boe.setCuc(7.306225597858429E-6);
        boe.setCus(-6.314832717180252E-6);
        boe.setCrc(406.96875);
        boe.setCrs(225.9375);
        boe.setCic(-7.450580596923828E-9);
        boe.setCis(-1.4062970876693726E-7);
        boe.setDate(new GNSSDate(boe.getWeek(), boe.getTime(), SatelliteSystem.BEIDOU).getDate());
        // Date of the BeiDou orbital elements (GPStime - BDTtime = 14s)
        final AbsoluteDate target = boe.getDate().shiftedBy(-14.0);
        // Build the BeiDou propagator
        final GNSSPropagator propagator = boe.getPropagator();
        // Compute the PV coordinates at the date of the BeiDou orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from sp3 file WUM0MGXULA_20192470700_01D_05M_ORB.SP33)
        final Vector3D expectedPos = new Vector3D(-10260690.520, 24061180.795, -32837341.074);
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 3.1);
    }

    @Test
    public void testIssue544() {
        // Builds the BeidouPropagator from the almanac
        final GNSSPropagator propagator = almanac.getPropagator();
        // In order to test the issue, we voluntary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());

    }

}
