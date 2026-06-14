/* Copyright 2002-2026 CS GROUP
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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.NavICAlmanac;
import org.orekit.propagation.analytical.gnss.data.NavICAlmanacFactory;
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

public class NavICPropagatorTest {

    private static NavICAlmanacFactory factory;
    private static Frames frames;

    @DefaultDataContext
    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        final DataContext context = DataContext.getDefault();

        // Almanac for satellite 1 for April 1st 2014 (Source: Rinex 3.04 format - Table A19)
        factory = new NavICAlmanacFactory(context.getTimeScales(),
                                          SatelliteSystem.NAVIC,
                                          context.getFrames().getEME2000(),
                                          context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        factory.setPrn(1);
        factory.setTimeOfEphemeris(new GNSSDate(1786, 172800.0, SatelliteSystem.NAVIC));
        final double sqrtA = 6.493487739563E03;
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(2.257102518342E-03);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(4.758105460020e-01);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(-8.912102146884E-01);
        factory.getOmegaDotDriver().setValue(-4.414469594664e-09);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(-2.999907424014);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(-1.396094758025);
        factory.getAf0Driver().setValue(-9.473115205765e-04);
        factory.getAf1Driver().setValue(1.250555214938e-12);
        factory.setTimeOfClock(factory.getTimeOfEphemeris());

        frames = context.getFrames();
    }

    @Test
    public void testNavICCycle() {
        // Builds the NavIC propagator from the almanac
        final GNSSPropagator<NavICAlmanac> propagator =
            new GNSSPropagator<>(factory);
        // Propagate at the NavIC date and one NavIC cycle later
        final AbsoluteDate date0 = factory.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final AbsoluteDate date1 = date0.shiftedBy(propagator.getOrbitalElements().getCycleDuration());
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the NavIC propagator from the almanac
        final GNSSPropagator<NavICAlmanac> propagator = new GNSSPropagator<>(factory);
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986005e+14, factory.getMu(), 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 3.4e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.9e-12);
    }

    @Test
    public void testResetInitialState() {
        final GNSSPropagator<NavICAlmanac> propagator = new GNSSPropagator<>(factory);
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetInitialState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000));
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    public void testResetIntermediateState() {
        GNSSPropagator<NavICAlmanac> propagator = new GNSSPropagator<>(factory);
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetIntermediateState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000),
                                          true);
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = frames.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final GNSSPropagator<NavICAlmanac> propagator =
            new GNSSPropagator<>(factory.createFromDrivers(),
                                 eme2000,
                                 frames.getITRF(IERSConventions.IERS_2010, true),
                                 Propagator.getDefaultLaw(frames),
                                 Propagator.DEFAULT_MASS);
        GNSSOrbitalElements<?> elements = propagator.getOrbitalElements();
        AbsoluteDate t0 = elements.getDate();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate central = t0.shiftedBy(dt);
            final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
            final double h = 10.0;
            List<TimeStampedPVCoordinates> sample = new ArrayList<>();
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
        Assertions.assertEquals(0.0, errorV, 2.6e-7);
        Assertions.assertEquals(0.0, errorA, 6.5e-8);

    }

    @Test
    public void testIssue544() {
        // Builds the NavICPropagator from the almanac
        final GNSSPropagator<NavICAlmanac> propagator = new GNSSPropagator<>(factory);
        // In order to test the issue, we voluntarily set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());
    }

    @Test
    public void testConversion() {
        GnssTestUtils.checkFieldConversion(factory.createFromDrivers());
    }

}
