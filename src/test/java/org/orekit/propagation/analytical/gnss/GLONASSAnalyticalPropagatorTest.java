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
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.analytical.gnss.data.GLONASSAlmanac;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeComponents;
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

public class GLONASSAnalyticalPropagatorTest {

    private static GLONASSAlmanac almanac;

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        // Reference values for validation are given into Glonass Interface Control Document v1.0 2016
        final double pi = GNSSConstants.GLONASS_PI;
        almanac = new GLONASSAlmanac(0, 1, 22, 12, 2007, 33571.625,
                                     -0.293967247009277 * pi,
                                     -0.00012947082519531 * pi,
                                     0.57867431640625 * pi,
                                     0.000432968139648438,
                                     0.01953124999975,
                                     6.103515625e-5,
                                     0.0, 0.0, 0.0);
    }

    @Test
    public void testPerfectValues() {
        // Build the propagator
        final GLONASSAnalyticalPropagator propagator = almanac.getPropagator(DataContext.getDefault(), Utils.defaultLaw(),
                                                                             FramesFactory.getEME2000(),
                                                                             FramesFactory.getITRF(IERSConventions.IERS_2010, false), 1521.0);

        // Target
        final AbsoluteDate target = new AbsoluteDate(new DateComponents(2007, 12, 23),
                                                     new TimeComponents(51300),
                                                     TimeScalesFactory.getGLONASS());

        Assertions.assertEquals(0.0, almanac.getGlo2UTC(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, almanac.getGloOffset(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, almanac.getGPS2Glo(), Precision.SAFE_MIN);
        Assertions.assertEquals(1,   almanac.getHealth());
        Assertions.assertEquals(0,   almanac.getFrequencyChannel());

        // Compute PVCoordinates at the date in the ECEF
        final PVCoordinates pvFinal    = propagator.propagateInEcef(target);

        // Reference values (see GLONASS ICD)
        final PVCoordinates pvExpected = new PVCoordinates(new Vector3D(10697116.4874360654,
                                                                        21058292.4241863210,
                                                                        -9635679.33963303405),
                                                           new Vector3D(-0686.100809921691084,
                                                                        -1136.54864124521881,
                                                                        -3249.98587740305799));

        // Check
        Assertions.assertEquals(1521.0, propagator.getMass(target), 0.1);
        Assertions.assertEquals(0.0, pvFinal.getPosition().distance(pvExpected.getPosition()), 1.1e-7);
        Assertions.assertEquals(0.0, pvFinal.getVelocity().distance(pvExpected.getVelocity()), 1.1e-5);

        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pvFinal2 = propagator.getPVCoordinates(target, propagator.getECEF());
        Assertions.assertEquals(0., pvFinal.getPosition().distance(pvFinal2.getPosition()), 1.9e-8);
    }

    @Test
    public void testFrames() {
        // Builds the GLONASSAnalyticalPropagator from the almanac
        final GLONASSAnalyticalPropagator propagator = almanac.getPropagator();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals("EME2000", propagator.getECI().getName());
        Assertions.assertEquals(3.986004418e+14, GNSSConstants.GLONASS_MU, 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 2.6e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 2.8e-12);
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final GLONASSAnalyticalPropagator propagator = almanac.getPropagator();
        GLONASSOrbitalElements elements = propagator.getGLONASSOrbitalElements();
        AbsoluteDate t0 = new GLONASSDate(elements.getNa(), elements.getN4(), elements.getTime()).getDate();
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
        Assertions.assertEquals(0.0, errorP, 1.9e-9);
        Assertions.assertEquals(0.0, errorV, 3.2e-3);
        Assertions.assertEquals(0.0, errorA, 7.0e-4);

    }

    @Test
    public void testNoReset() {
        try {
            final GLONASSAnalyticalPropagator propagator = almanac.getPropagator();
            propagator.resetInitialState(propagator.getInitialState());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            GLONASSAnalyticalPropagator propagator = new GLONASSAnalyticalPropagatorBuilder(almanac).build();
            propagator.resetIntermediateState(propagator.getInitialState(), true);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    public void testIssue544() {
        // Builds the GLONASSAnalyticalPropagator from the almanac
        final GLONASSAnalyticalPropagator propagator = almanac.getPropagator(DataContext.getDefault());
        // In order to test the issue, we volontary set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());

    }

    /** Error with specific propagators & additional state provider throwing a NullPointerException when propagating */
    @Test
    public void testIssue949() {
        // GIVEN
        // Setup propagator
        final GLONASSAnalyticalPropagator propagator = almanac.getPropagator();

        // Setup additional state provider which use the initial state in its init method
        final AdditionalStateProvider additionalStateProvider = TestUtils.getAdditionalProviderWithInit();
        propagator.addAdditionalStateProvider(additionalStateProvider);

        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> propagator.propagate(new AbsoluteDate()), "No error should have been thrown");

    }

}
