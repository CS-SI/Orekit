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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.FieldGalileoAlmanac;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.GalileoAlmanac;
import org.orekit.propagation.analytical.gnss.data.GalileoAlmanacFactory;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessageFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;

public class GalileoPropagatorTest {

    private DataContext context;
    private GalileoNavigationMessageFactory factory;

    @DefaultDataContext
    @BeforeEach
    public void setUp() {
        context = DataContext.getDefault();
        factory = new GalileoNavigationMessageFactory(context.getTimeScales(),
                                                      SatelliteSystem.GALILEO,
                                                      GalileoNavigationMessage.FNAV,
                                                      context.getFrames().getEME2000(),
                                                      context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        factory.setPrn(4);
        factory.setWeekAndTime(1024, 293400.0);
        final double sqrtA = 5440.602949142456;
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        factory.getDeltaN0Driver().setValue(3.7394414770330066E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(2.4088891223073006E-4);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(0.9531656087278083);
        factory.getIDotDriver().setValue(-2.36081262303612E-10);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(-0.36639513583951266);
        factory.getOmegaDotDriver().setValue(-5.7695260382035525E-9);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(-1.6870064194345724);
        factory.getOrbitalParametersDrivers().findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(-0.38716557650888);
        factory.getCucDriver().setValue(-8.903443813323975E-7);
        factory.getCusDriver().setValue(6.61797821521759E-6);
        factory.getCrcDriver().setValue(194.0625);
        factory.getCrsDriver().setValue(-18.78125);
        factory.getCicDriver().setValue(3.166496753692627E-8);
        factory.getCisDriver().setValue(-1.862645149230957E-8);
        factory.setToc(new GNSSDate(1024, 0.0, SatelliteSystem.GALILEO).getDate());
    }

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testGalileoCycle() {
        // Reference for the almanac: 2019-05-28T09:40:01.0Z
        final GalileoAlmanacFactory almanacFactory =
            new GalileoAlmanacFactory(context.getTimeScales(),
                                      SatelliteSystem.GALILEO,
                                      context.getFrames().getEME2000(),
                                      context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        final ParameterDriversList orb = factory.getOrbitalParametersDrivers();
        almanacFactory.setPrn(1);
        almanacFactory.setWeekAndTime(1024, 293400.0);
        final double sqrtA = FastMath.sqrt(GalileoAlmanac.A0) + 0.013671875;
        orb.findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        orb.findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(0.000152587890625);
        orb.findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(GalileoAlmanac.I0 + 0.003356933593);
        almanacFactory.setIOD(4);
        orb.findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(0.2739257812499857891);
        almanacFactory.getOmegaDotDriver().setValue(-1.74622982740407E-9);
        orb.findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(0.7363586425);
        orb.findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(0.27276611328124);
        almanacFactory.getAf0Driver().setValue(-0.0006141662597);
        almanacFactory.getAf1Driver().setValue(-7.275957614183E-12);
        almanacFactory.setHealthE1(0);
        almanacFactory.setHealthE5a(0);
        almanacFactory.setHealthE5b(0);

        // Intermediate verification
        Assertions.assertEquals(1,                   almanacFactory.getPrn());
        Assertions.assertEquals(1024,                almanacFactory.getWeek());
        Assertions.assertEquals(4,                   almanacFactory.getIOD());
        Assertions.assertEquals(0,                   almanacFactory.getHealthE1());
        Assertions.assertEquals(0,                   almanacFactory.getHealthE5a());
        Assertions.assertEquals(0,                   almanacFactory.getHealthE5b());
        Assertions.assertEquals(-0.0006141662597,    almanacFactory.getAf0Driver().getValue(), 1.0e-15);
        Assertions.assertEquals(-7.275957614183E-12, almanacFactory.getAf1Driver().getValue(), 1.0e-15);

        // Builds the GalileoPropagator from the almanac
        final GNSSPropagator<GalileoAlmanac> propagator = new GNSSPropagator<>(almanacFactory);
        // Propagate at the Galileo date and one Galileo cycle later
        final AbsoluteDate date0 = almanacFactory.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final AbsoluteDate date1 = date0.shiftedBy(propagator.getOrbitalElements().getCycleDuration());
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFieldGalileoCycle() {
        // Reference for the almanac: 2019-05-28T09:40:01.0Z
        final GalileoAlmanacFactory almanacFactory =
            new GalileoAlmanacFactory(context.getTimeScales(),
                                      SatelliteSystem.GALILEO,
                                      context.getFrames().getEME2000(),
                                      context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        final ParameterDriversList orb = almanacFactory.getOrbitalParametersDrivers();
        almanacFactory.setPrn(1);
        almanacFactory.setWeekAndTime(1024, 293400.0);
        final double sqrtA = GalileoAlmanac.A0 + 0.013671875;
        orb.findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).setValue(sqrtA * sqrtA);
        orb.findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).setValue(0.000152587890625);
        orb.findByName(GNSSOrbitalElementsFactory.INCLINATION).setValue(GalileoAlmanac.I0 + 0.003356933593);
        almanacFactory.setIOD(4);
        orb.findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).setValue(0.2739257812499857891);
        almanacFactory.getOmegaDotDriver().setValue(-1.74622982740407E-9);
        orb.findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).setValue(0.7363586425);
        orb.findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).setValue(0.27276611328124);
        almanacFactory.getAf0Driver().setValue(-0.0006141662597);
        almanacFactory.getAf1Driver().setValue(-7.275957614183E-12);
        almanacFactory.setHealthE1(0);
        almanacFactory.setHealthE5a(0);
        almanacFactory.setHealthE5b(0);
        final FieldGalileoAlmanac<Binary64> almanac =
            almanacFactory.createFromDrivers().toField(Binary64Field.getInstance());

        // Intermediate verification
        Assertions.assertEquals(1,                   almanacFactory.getPrn());
        Assertions.assertEquals(1024,                almanacFactory.getWeek());
        Assertions.assertEquals(4,                   almanac.getIOD());
        Assertions.assertEquals(0,                   almanac.getHealthE1());
        Assertions.assertEquals(0,                   almanac.getHealthE5a());
        Assertions.assertEquals(0,                   almanac.getHealthE5b());
        Assertions.assertEquals(-0.0006141662597,    almanac.getAf0().getReal(), 1.0e-15);
        Assertions.assertEquals(-7.275957614183E-12, almanac.getAf1().getReal(), 1.0e-15);

        // Builds the GalileoPropagator from the almanac
        final FieldGnssPropagator<Binary64, GalileoAlmanac, FieldGalileoAlmanac<Binary64>> propagator =
            new FieldGnssPropagator<>(Binary64Field.getInstance(), almanacFactory);
        // Propagate at the Galileo date and one Galileo cycle later
        final FieldAbsoluteDate<Binary64> date0 = almanac.getOrbit().getDate();
        final FieldVector3D<Binary64> p0 =
                propagator.propagateInEcef(date0, propagator.getParameters(Binary64Field.getInstance())).
                getPosition();
        final FieldAbsoluteDate<Binary64> date1 = date0.shiftedBy(propagator.getOrbitalElements().getCycleDuration());
        final FieldVector3D<Binary64> p1 =
                propagator.propagateInEcef(date1, propagator.getParameters(Binary64Field.getInstance())).
                getPosition();

        // Checks
        Assertions.assertEquals(0., p0.distance(p1).getReal(), 0.);
    }

    @Test
    public void testFrames() {
        // Builds the GalileoPropagator from the ephemeris
        final GNSSPropagator<GalileoNavigationMessage> propagator =
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
        Assertions.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 2.5e-8);
        Assertions.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 2.8e-12);
    }

    @Test
    public void testResetInitialState() {
        final GNSSPropagator<GalileoNavigationMessage> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetInitialState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000));
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    public void testResetIntermediateState() {
        GNSSPropagator<GalileoNavigationMessage> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        final SpacecraftState old = propagator.getInitialState();
        propagator.resetIntermediateState(new SpacecraftState(old.getOrbit(), old.getAttitude()).withMass(old.getMass() + 1000),
                                          true);
        Assertions.assertEquals(old.getMass() + 1000, propagator.getInitialState().getMass(), 1.0e-9);
    }

    @Test
    public void testDerivativesConsistency() {

        final Frame eme2000 = context.getFrames().getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        final GNSSPropagator<GalileoNavigationMessage> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        GNSSOrbitalElements<?> elements = propagator.getOrbitalElements();
        AbsoluteDate t0 = elements.getOrbit().getDate();
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
        Assertions.assertEquals(0.0, errorP, 1.9e-9);
        Assertions.assertEquals(0.0, errorV, 4.4e-8);
        Assertions.assertEquals(0.0, errorA, 1.8e-9);

    }

    @Test
    public void testPosition() {
        // Date of the Galileo orbital elements, 10 April 2019 at 09:30:00 UTC
        final AbsoluteDate target = factory.getDate();
        // Build the Galileo propagator
        final GNSSPropagator<GalileoNavigationMessage> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
        // Compute the PV coordinates at the date of the Galileo orbital elements
        final PVCoordinates pv =
            propagator.getPVCoordinates(target,
                                        context.getFrames().getITRF(IERSConventions.IERS_2010, false));
        // Computed position
        final Vector3D computedPos = pv.getPosition();
        // Expected position (reference from IGS file WUM0MGXULA_20191010500_01D_15M_ORB.sp3)
        final Vector3D expectedPos = new Vector3D(10487480.721, 17867448.753, -21131462.002);
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 2.1);
    }

    @Test
    public void testIssue544() {
        // Builds the GalileoPropagator from the almanac
        final GNSSPropagator<GalileoNavigationMessage> propagator =
            new GNSSPropagatorBuilder<>(factory).buildPropagator();
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
