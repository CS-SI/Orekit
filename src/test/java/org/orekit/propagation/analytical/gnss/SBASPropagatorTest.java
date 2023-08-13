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
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASOrbitalElements;
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


public class SBASPropagatorTest {

    /** Threshold for test validation. */
    private static double eps = 1.0e-15;

    /** SBAS orbital elements. */
    private SBASNavigationMessage soe;
    private Frames frames;

    @BeforeEach
    public void setUp() {
        // Reference data are taken from IGS file brdm0370.17p
        soe = new SBASNavigationMessage();
        soe.setPRN(127);
        soe.setTime(1.23303e+05);
        soe.setDate(new GNSSDate(1935, 123303.0, SatelliteSystem.SBAS).getDate());
        soe.setX(2.406022248000e+07);
        soe.setXDot(-2.712500000000e-01);
        soe.setXDotDot(3.250000000000e-04);
        soe.setY(3.460922568000e+07);
        soe.setYDot(3.063125000000e-00);
        soe.setYDotDot(-1.500000000000e-04);
        soe.setZ(1.964040000000e+04);
        soe.setZDot(1.012000000000e-00);
        soe.setZDotDot(-1.250000000000e-04);
        frames = DataContext.getDefault().getFrames();
    }

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testPropagationAtReferenceTime() {
        // SBAS propagator
        final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).
                        attitudeProvider(Utils.defaultLaw()).
                        mu(GNSSConstants.SBAS_MU).
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
        Assertions.assertEquals(soe.getX(),       position.getX(),     eps);
        Assertions.assertEquals(soe.getY(),       position.getY(),     eps);
        Assertions.assertEquals(soe.getZ(),       position.getZ(),     eps);
        Assertions.assertEquals(soe.getXDot(),    velocity.getX(),     eps);
        Assertions.assertEquals(soe.getYDot(),    velocity.getY(),     eps);
        Assertions.assertEquals(soe.getZDot(),    velocity.getZ(),     eps);
        Assertions.assertEquals(soe.getXDotDot(), acceleration.getX(), eps);
        Assertions.assertEquals(soe.getYDotDot(), acceleration.getY(), eps);
        Assertions.assertEquals(soe.getZDotDot(), acceleration.getZ(), eps);
    }

    @Test
    public void testPropagation() {
        // SBAS propagator
        final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();
        // Propagation
        final PVCoordinates pv = propagator.propagateInEcef(soe.getDate().shiftedBy(1.0));
        // Position/Velocity/Acceleration
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();
        final Vector3D acceleration = pv.getAcceleration();
        // Verify
        Assertions.assertEquals(24060222.2089125, position.getX(),     eps);
        Assertions.assertEquals(34609228.7430500, position.getY(),     eps);
        Assertions.assertEquals(19641.4119375,    position.getZ(),     eps);
        Assertions.assertEquals(-0.270925,        velocity.getX(),     eps);
        Assertions.assertEquals(3.062975,         velocity.getY(),     eps);
        Assertions.assertEquals(1.011875,         velocity.getZ(),     eps);
        Assertions.assertEquals(soe.getXDotDot(), acceleration.getX(), eps);
        Assertions.assertEquals(soe.getYDotDot(), acceleration.getY(), eps);
        Assertions.assertEquals(soe.getZDotDot(), acceleration.getZ(), eps);
    }

    @Test
    public void testFrames() {
        // Builds the SBAS propagator from the ephemeris
        final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986005e+14, propagator.getMU(), 1.0e6);
        Assertions.assertEquals(propagator.getECI().getName(), propagator.getFrame().getName());
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2017, 2, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 1.6e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.8e-12);
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();
        SBASOrbitalElements elements = propagator.getSBASOrbitalElements();
        AbsoluteDate t0 = new GNSSDate(elements.getWeek(), elements.getTime(), SatelliteSystem.SBAS).getDate();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate central = t0.shiftedBy(dt);
            final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
            final double h = 10.0;
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
        Assertions.assertEquals(0.0, errorP, 1.5e-11);
        Assertions.assertEquals(0.0, errorV, 7.3e-3);
        Assertions.assertEquals(0.0, errorA, 1.7e-3);

    }

    @Test
    public void testNoReset() {
        try {
            final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();
            propagator.resetInitialState(propagator.getInitialState());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    /** Error with specific propagators & additional state provider throwing a NullPointerException when propagating */
    @Test
    public void testIssue949() {
        // GIVEN
        // Setup propagator
        final SBASPropagator propagator = new SBASPropagatorBuilder(soe, frames).build();

        // Setup additional state provider which use the initial state in its init method
        final AdditionalStateProvider additionalStateProvider = TestUtils.getAdditionalProviderWithInit();
        propagator.addAdditionalStateProvider(additionalStateProvider);

        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> propagator.propagate(new AbsoluteDate()), "No error should have been thrown");

    }
}
