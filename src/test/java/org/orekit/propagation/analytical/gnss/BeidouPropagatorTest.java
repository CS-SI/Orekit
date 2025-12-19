/* Copyright 2002-2025 CS GROUP
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
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.attitude.GenericGNSS;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.BeidouAlmanac;
import org.orekit.propagation.analytical.gnss.data.BeidouAlmanacFactory;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessageFactory;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.time.AbsoluteDate;
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

    private static DataContext          context;
    private static BeidouAlmanacFactory factory;

    @DefaultDataContext
    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        context = DataContext.getDefault();

        // Almanac for satellite 18 for May 28th 2019
        factory = new BeidouAlmanacFactory(context.getTimeScales(),
                                           SatelliteSystem.BEIDOU,
                                           context.getFrames().getEME2000(),
                                           context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        factory.setPrn(18);
        factory.setWeekAndTime(694, 4096.0);
        final double sqrtA = 6493.3076;
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(0.00482368);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(-0.01365602);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(1.40069711);
        factory.getOmegaDotDriver().setValue(-2.11437379e-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(3.11461541);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(-2.53029382);
        factory.getAf0Driver().setValue(0.0001096725);
        factory.getAf1Driver().setValue(7.27596e-12);
        factory.setHealth(0);
    }

    @Test
    void testBeidouCycle() {
        // Builds the BeiDou propagator from the almanac
        final GNSSPropagator<BeidouAlmanac> propagator =
            new GNSSPropagator<>(new BeidouAlmanacFactory(context.getTimeScales(),
                                                          SatelliteSystem.BEIDOU,
                                                          context.getFrames().getEME2000(),
                                                          context.getFrames().getITRF(IERSConventions.IERS_2010, false)));
        // Intermediate verification
        Assertions.assertEquals(18, factory.getPrn());
        Assertions.assertEquals(0, factory.getHealth());
        Assertions.assertEquals(0.0001096725, factory.getAf0Driver().getValue(), 1.0e-15);
        Assertions.assertEquals(7.27596e-12, factory.getAf1Driver().getValue(), 1.0e-15);
        // Propagate at the BeiDou date and one BeiDou cycle later
        final AbsoluteDate date0 = factory.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final AbsoluteDate date1 = date0.shiftedBy(propagator.getOrbitalElements().getCycleDuration());
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    void testFrames() {
        // Builds the BeiDou propagator from the almanac
        final GNSSPropagator<BeidouAlmanac> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        Assertions.assertEquals("EME2000", propagator.getFrame().getName());
        Assertions.assertEquals(3.986004418e+14, factory.getMu(), 1.0e6);
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 4.6e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.9e-12);
    }

    @Test
    void testResetInitialState() {
        final GNSSPropagator<BeidouAlmanac> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetInitialState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000));
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    void testResetIntermediateState() {
        GNSSPropagator<BeidouAlmanac> propagator =  new GNSSPropagator<>(factory);
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetIntermediateState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000),
                                          true);
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    void testDerivativesConsistency() {

        final Frame eme2000 = context.getFrames().getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final GNSSPropagator<BeidouAlmanac> propagator =
            new GNSSPropagator<>(factory.createFromDrivers(),
                                 eme2000,
                                 context.getFrames().getITRF(IERSConventions.IERS_2010, true),
                                 new GenericGNSS(AbsoluteDate.PAST_INFINITY,
                                                 AbsoluteDate.FUTURE_INFINITY,
                                                 context.getCelestialBodies().getSun(),
                                                 factory.getInertial()),
                                 Propagator.DEFAULT_MASS);
        GNSSOrbitalElements<?> elements = propagator.getOrbitalElements();
        AbsoluteDate t0 = elements.getDate();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate central = t0.shiftedBy(dt);
            final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
            final double h = 60.0;
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
        Assertions.assertEquals(0.0, errorV, 8.0e-8);
        Assertions.assertEquals(0.0, errorA, 2.0e-8);

    }

    @Test
    void testPosition() {
        // Initial BeiDou orbital elements (Ref: IGS)
        final BeidouLegacyNavigationMessageFactory factory1 =
            new BeidouLegacyNavigationMessageFactory(context.getTimeScales(),
                                                     SatelliteSystem.BEIDOU,
                                                     BeidouLegacyNavigationMessage.D1,
                                                     context.getFrames().getEME2000(),
                                                     context.getFrames().getITRF(IERSConventions.IERS_2010, false),
                                                     false);
        factory1.setPrn(7);
        factory1.setWeekAndTime(713, 284400.0);
        final double sqrtA = 6492.84515953064;
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(0.00728036486543715);
        factory1.getDeltaN0Driver().setValue(2.1815194404696853E-9);
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(0.9065628903946735);
        factory1.getIDotDriver().setValue(0.0);
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(-0.6647664535282437);
        factory1.getOmegaDotDriver().setValue(-3.136916379444212E-9);
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(-2.6584351442773304);
        factory1.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(0.9614806010234702);
        factory1.getCucDriver().setValue(7.306225597858429E-6);
        factory1.getCusDriver().setValue(-6.314832717180252E-6);
        factory1.getCrcDriver().setValue(406.96875);
        factory1.getCrsDriver().setValue(225.9375);
        factory1.getCicDriver().setValue(-7.450580596923828E-9);
        factory1.getCisDriver().setValue(-1.4062970876693726E-7);
        // Date of the BeiDou orbital elements (GPStime - BDTtime = 14s)
        final AbsoluteDate target = factory1.getDate().shiftedBy(-14.0);
        // Build the BeiDou propagator
        final GNSSPropagator<BeidouLegacyNavigationMessage> propagator = new GNSSPropagator<>(factory1);
        // Compute the PV coordinates at the date of the BeiDou orbital elements
        final PVCoordinates pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from sp3 file WUM0MGXULA_20192470700_01D_05M_ORB.SP33)
        final Vector3D expectedPos = new Vector3D(-10260690.520, 24061180.795, -32837341.074);
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 3.1);
    }

    @Test
    void testIssue544() {
        // Builds the BeidouPropagator from the almanac
        final GNSSPropagator<BeidouAlmanac> propagator = new GNSSPropagator<>(factory);
        // In order to test the issue, we voluntarily set a Double.NaN value in the date.
        final AbsoluteDate date0 = new AbsoluteDate(2010, 5, 7, 7, 50, Double.NaN, TimeScalesFactory.getUTC());
        final PVCoordinates pv0 = propagator.propagateInEcef(date0);
        // Verify that an infinite loop did not occur
        Assertions.assertEquals(Vector3D.NaN, pv0.getPosition());
        Assertions.assertEquals(Vector3D.NaN, pv0.getVelocity());

    }

    @Test
    void testConversion() {
        GnssTestUtils.checkFieldConversion(factory.createFromDrivers());
    }

}
