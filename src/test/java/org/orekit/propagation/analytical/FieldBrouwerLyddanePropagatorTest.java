/* Copyright 2002-2024 CS GROUP
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

package org.orekit.propagation.analytical;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.stat.descriptive.StorelessUnivariateStatistic;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class FieldBrouwerLyddanePropagatorTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @Test
    public void testIssue947() {
        doTestIssue947(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue947(Field<T> field) {
        // Error case from gitlab issue#947: negative eccentricity when calculating mean orbit
        TLE tleOrbit = new TLE("1 43196U 18015E   21055.59816856  .00000894  00000-0  38966-4 0  9996",
                               "2 43196  97.4662 188.8169 0016935 299.6845  60.2706 15.24746686170319");
        Propagator propagator = TLEPropagator.selectExtrapolator(tleOrbit);

        //Get state at initial date and 3 days before
        SpacecraftState tleState = propagator.getInitialState();
        FieldOrbit<T> orbIni = OrbitType.KEPLERIAN.convertToFieldOrbit(field, tleState.getOrbit());
        SpacecraftState tleStateAtDate = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(3,  TimeUnit.DAYS));
        FieldOrbit<T> orbAtDate = OrbitType.KEPLERIAN.convertToFieldOrbit(field, tleStateAtDate.getOrbit());

        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 0);

        Assertions.assertDoesNotThrow(() -> {
            FieldBrouwerLyddanePropagator.computeMeanOrbit(orbIni, provider,
                                                           provider.onDate(tleState.getDate()),
                                                           BrouwerLyddanePropagator.M2);
        });

        Assertions.assertDoesNotThrow(() -> {
            FieldBrouwerLyddanePropagator.computeMeanOrbit(orbAtDate, provider,
                                                           provider.onDate(tleStateAtDate.getDate()),
                                                           BrouwerLyddanePropagator.M2);
        });
    }

    @Test
    public void sameDateCartesian() {
        doSameDateCartesian(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doSameDateCartesian(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // e = 0.04152500499523033   and   i = 1.705015527659039

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6149822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolation at the initial date
        // ---------------------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
                new FieldBrouwerLyddanePropagator<T>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPosition(),
                                                       finalOrbit.getPosition()).getReal(),
                                1.9e-8);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                                       finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                                1.2e-11);
        Assertions.assertEquals(0.0, 
                                finalOrbit.getA().getReal() - initialOrbit.getA().getReal(),
                                9.4e-10);

    }

    @Test
    public void sameDateKeplerian() {
        doSameDateKeplerian(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doSameDateKeplerian(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(6767924.41), zero.add(.005),  zero.add(1.7),zero.add( 2.1),
                                                               zero.add(2.9), zero.add(6.2), PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        FieldBrouwerLyddanePropagator<T> extrapolator =
                new FieldBrouwerLyddanePropagator<T>(initialOrbit, DEFAULT_LAW, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                                       finalOrbit.getPVCoordinates().getPosition()).getReal(),
                                6.7e-7);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                                       finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                                4.2e-10);
        Assertions.assertEquals(0.0,
                                finalOrbit.getA().getReal() - initialOrbit.getA().getReal(),
                                9.4e-9);
    }

    @Test
    public void almostSphericalBody() {
        doAlmostSphericalBody(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doAlmostSphericalBody(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(8449822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));



        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Initialisation to simulate a Keplerian extrapolation
        // To be noticed: in order to simulate a Keplerian extrapolation with the
        // analytical
        // extrapolator, one should put the zonal coefficients to 0. But due to
        // numerical pbs
        // one must put a non 0 value.
        UnnormalizedSphericalHarmonicsProvider kepProvider =
                GravityFieldFactory.getUnnormalizedProvider(6.378137e6, 3.9860047e14,
                                                            TideSystem.UNKNOWN,
                                                            new double[][] {
                                                                { 0 }, { 0 }, { 0.1e-10 }, { 0.1e-13 }, { 0.1e-13 }, { 0.1e-14 }, { 0.1e-14 }
                                                            }, new double[][] {
                                                                { 0 }, { 0 },  { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
                                                            });

        // Extrapolators definitions
        // -------------------------
        FieldBrouwerLyddanePropagator<T> extrapolatorAna =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, zero.add(1000.0), kepProvider, BrouwerLyddanePropagator.M2);
        FieldKeplerianPropagator<T> extrapolatorKep = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = date.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        FieldSpacecraftState<T> finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assertions.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate).getReal(), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assertions.assertEquals(finalOrbitAna.getA().getReal(), finalOrbitKep.getA().getReal(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA().getReal());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEx().getReal(), finalOrbitKep.getEquinoctialEx().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEy().getReal(), finalOrbitKep.getEquinoctialEy().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx().getReal(), finalOrbitKep.getHx().getReal()),
                     finalOrbitKep.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy().getReal(), finalOrbitKep.getHy().getReal()),
                     finalOrbitKep.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv().getReal(), finalOrbitKep.getLv().getReal()),
                     finalOrbitKep.getLv().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE().getReal(), finalOrbitKep.getLE().getReal()),
                     finalOrbitKep.getLE().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM().getReal(), finalOrbitKep.getLM().getReal()),
                     finalOrbitKep.getLM().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM().getReal()));

    }

    @Test
    public void compareToNumericalPropagation() {
        doCompareToNumericalPropagation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagation(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Initial state definition
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, initialOrbit.toOrbit(), propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState.toSpacecraftState());

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.toAbsoluteDate().shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator =
                new FieldBrouwerLyddanePropagator<T>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        FieldSpacecraftState<T> BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());

        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.175);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 3.2e-6);
        Assertions.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 6.9e-8);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.0053);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 1.2e-6);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.0052);
    }

    @Test
    public void compareToNumericalPropagationWithDrag() {
        doCompareToNumericalPropagationWithDrag(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagationWithDrag(Field<T> field) {

        T zero = field.getZero();
        final Frame inertialFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 1, 00, 00, 00.000, utc);
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(date, zero);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));
        // Initial state definition
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, initialOrbit.toOrbit(), propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        // Atmosphere
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        MarshallSolarActivityFutureEstimation msafe =
                        new MarshallSolarActivityFutureEstimation("Jan2000F10-edited-data\\.txt",
                                                                  MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        DTM2000 atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), earth);

        // Force model
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        final ForceModel drag = new DragForce(atmosphere, new IsotropicDrag(1.0, 1.0));
        NumPropagator.addForceModel(holmesFeatherstone);
        NumPropagator.addForceModel(drag);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState.toSpacecraftState());

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.toAbsoluteDate().shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION WITHOUT DRAG
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator =
                        new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        FieldSpacecraftState<T> BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());

        // Verify a and e differences without the drag effect on Brouwer-Lyddane
        final double deltaSmaBefore = 15.81;
        final double deltaEccBefore = 9.2681e-5;
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), deltaSmaBefore);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), deltaEccBefore);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION WITH DRAG
        //_______________________________________________________________________________________________

        double M2 = 1.0e-14;
        BLextrapolator = new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), M2);
        BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());

        // Verify a and e differences without the drag effect on Brouwer-Lyddane
        final double deltaSmaAfter = 11.04;
        final double deltaEccAfter = 9.2639e-5;
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), deltaSmaAfter);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), deltaEccAfter);
        Assertions.assertTrue(deltaSmaAfter < deltaSmaBefore);
        Assertions.assertTrue(deltaEccAfter < deltaEccBefore);
        Assertions.assertEquals(M2, BLextrapolator.getM2(), Double.MIN_VALUE);

    }

    @Test
    public void compareToNumericalPropagationMeanInitialOrbit() {
        doCompareToNumericalPropagationMeanInitialOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagationMeanInitialOrbit(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        FieldBrouwerLyddanePropagator<T> BLextrapolator =
                new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider),
                                             PropagationType.MEAN, BrouwerLyddanePropagator.M2);
        FieldSpacecraftState<T> initialOsculatingState = BLextrapolator.propagate(initDate);
        final KeplerianOrbit InitOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(initialOsculatingState.getOrbit().toOrbit());

        FieldSpacecraftState<T> BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit().toOrbit());

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, InitOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialOsculatingState.toSpacecraftState());

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.toAbsoluteDate().shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        // Asserts
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.174);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 3.2e-6);
        Assertions.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 6.9e-8);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.0053);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 1.2e-6);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.0052);
    }

    @Test
    public void compareToNumericalPropagationResetInitialIntermediate() {
        doCompareToNumericalPropagationResetInitialIntermediate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareToNumericalPropagationResetInitialIntermediate(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                      zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                      inertialFrame, initDate, zero.add(provider.getMu()));
        // Initial state definition
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator1 =
                new FieldBrouwerLyddanePropagator<>(initialOrbit, DEFAULT_LAW, zero.add(Propagator.DEFAULT_MASS), GravityFieldFactory.getUnnormalizedProvider(provider),
                                             PropagationType.OSCULATING, BrouwerLyddanePropagator.M2);
        //_______________________________________________________________________________________________
        // SET UP ANOTHER BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        FieldBrouwerLyddanePropagator<T> BLextrapolator2 =
                new FieldBrouwerLyddanePropagator<>( new FieldKeplerianOrbit<>(zero.add(a + 3000), zero.add(e + 0.001),
                                                                               zero.add(i - FastMath.toRadians(12.0)), zero.add(omega),
                                                                               zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                     inertialFrame, initDate, zero.add(provider.getMu())),DEFAULT_LAW,
                                                     zero.add(Propagator.DEFAULT_MASS),
                                                     GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);
        // Reset BL2 with BL1 initial state
        BLextrapolator2.resetInitialState(initialState);

        FieldSpacecraftState<T> BLFinalState1 = BLextrapolator1.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit().toOrbit());
        FieldSpacecraftState<T> BLFinalState2 = BLextrapolator2.propagate(initDate.shiftedBy(timeshift));
        BLextrapolator2.resetIntermediateState(BLFinalState1, true);
        final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit().toOrbit());

        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);

    }

    @Test
    public void compareConstructors() {
        doCompareConstructors(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareConstructors(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 600. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));


        FieldBrouwerLyddanePropagator<T> BLPropagator1 = new FieldBrouwerLyddanePropagator<T>(initialOrbit, DEFAULT_LAW,
                provider.getAe(), zero.add(provider.getMu()), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
        FieldBrouwerLyddanePropagator<T> BLPropagator2 = new FieldBrouwerLyddanePropagator<>(initialOrbit,
                provider.getAe(), zero.add(provider.getMu()), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
        FieldBrouwerLyddanePropagator<T> BLPropagator3 = new FieldBrouwerLyddanePropagator<>(initialOrbit,
                zero.add(Propagator.DEFAULT_MASS), provider.getAe(), zero.add(provider.getMu()), -1.08263e-3,
                2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);

        FieldSpacecraftState<T> BLFinalState1 = BLPropagator1.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit().toOrbit());
        FieldSpacecraftState<T> BLFinalState2 = BLPropagator2.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit().toOrbit());
        FieldSpacecraftState<T> BLFinalState3 = BLPropagator3.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit3 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState3.getOrbit().toOrbit());

        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);

        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit3.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit3.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit3.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getTrueAnomaly(), FastMath.PI), 0.0);

    }

    @Test
    public void undergroundOrbit() {
        doUndergroundOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doUndergroundOrbit(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);

        // for a semi major axis < equatorial radius
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(800.0), zero.add(100.0));

        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        // Extrapolator definition
        // -----------------------
        try {

            FieldBrouwerLyddanePropagator<T> extrapolator =
                new FieldBrouwerLyddanePropagator<>(initialOrbit, DEFAULT_LAW, provider.getAe(), zero.add(provider.getMu()),
                        -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }

    @Test
    public void tooEllipticalOrbit() {
        doTooEllipticalOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTooEllipticalOrbit(Field<T> field) {
        // for an eccentricity too big for the model
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(67679244.0), zero.add(1.0), zero.add(1.85850),
                                                               zero.add(2.1), zero.add(2.9), zero.add(6.2), PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        try {
        // Extrapolator definition
        // -----------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, provider.getAe(), zero.add(provider.getMu()),
                    -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0;
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, oe.getSpecifier());
        }
    }

    @Test
    public void criticalInclination() {
        doCriticalInclination(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCriticalInclination(Field<T> field) {

        final Frame inertialFrame = FramesFactory.getEME2000();

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.acos(1.0 / FastMath.sqrt(5.0)); // critical inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lV = 0; // true anomaly

        T zero = field.getZero();
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field);
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lV), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldBrouwerLyddanePropagator<T> extrapolator =
            new FieldBrouwerLyddanePropagator<>(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        // Extrapolation at the initial date
        // ---------------------------------
        final FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // Asserts
        // -------
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPosition(),
                                                       finalOrbit.getPosition()).getReal(),
                                1.5e-8);
        Assertions.assertEquals(0.0,
                                FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                                       finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                                2.7e-12);
        Assertions.assertEquals(0.0,
                                finalOrbit.getA().getReal() - initialOrbit.getA().getReal(),
                                7.5e-9);
    }

    @Test
    public void testUnableToComputeBLMeanParameters() {
        OrekitException oe = Assertions.assertThrows(OrekitException.class, () -> {
            doTestUnableToComputeBLMeanParameters(Binary64Field.getInstance());
        });
        Assertions.assertTrue(oe.getMessage().contains("unable to compute Brouwer-Lyddane mean parameters after"));
    }

    private <T extends CalculusFieldElement<T>> void doTestUnableToComputeBLMeanParameters(Field<T> field) {
        final Frame eci = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.9; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double pa = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = FastMath.toRadians(0); // mean anomaly
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(field,
                                                              new KeplerianOrbit(a, e, i, pa, raan, lM,
                                                                                 PositionAngleType.MEAN,
                                                                                 eci, date, provider.getMu()));
        // Mean orbit computation
        // ----------------------
        final UnnormalizedSphericalHarmonicsProvider up = GravityFieldFactory.getUnnormalizedProvider(provider);
        final UnnormalizedSphericalHarmonics uh = up.onDate(date);
        final double eps  = 1.e-13;
        final int maxIter = 10;
        FieldBrouwerLyddanePropagator.computeMeanOrbit(orbit, up, uh, BrouwerLyddanePropagator.M2,
                                                       eps, maxIter);
    }

    @Test
    public void testMeanComparisonWithNonField() {
        doTestMeanComparisonWithNonField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMeanComparisonWithNonField(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = -59400.0;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.9; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = FastMath.toRadians(0); // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Initial state definition
        final FieldSpacecraftState<T> initialStateField = new FieldSpacecraftState<>(initialOrbit);
        final SpacecraftState         initialState      = initialStateField.toSpacecraftState();

        // Field propagation
        final FieldBrouwerLyddanePropagator<T> blField = new FieldBrouwerLyddanePropagator<>(initialStateField.getOrbit(),
                                                                                             GravityFieldFactory.getUnnormalizedProvider(provider),
                                                                                             PropagationType.MEAN, BrouwerLyddanePropagator.M2);
        final FieldSpacecraftState<T> finalStateField     = blField.propagate(initialStateField.getDate().shiftedBy(timeshift));
        final FieldKeplerianOrbit<T>  finalOrbitField     = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(finalStateField.getOrbit());
        final KeplerianOrbit          finalOrbitFieldReal = finalOrbitField.toOrbit();

        // Classical propagation
        final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(initialState.getOrbit(),
                                                                         GravityFieldFactory.getUnnormalizedProvider(provider),
                                                                         PropagationType.MEAN, BrouwerLyddanePropagator.M2);
        final SpacecraftState finalState = bl.propagate(initialState.getDate().shiftedBy(timeshift));
        final KeplerianOrbit  finalOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(finalState.getOrbit());

        Assertions.assertEquals(finalOrbitFieldReal.getA(), finalOrbit.getA(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getE(), finalOrbit.getE(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getI(), finalOrbit.getI(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getRightAscensionOfAscendingNode(), finalOrbit.getRightAscensionOfAscendingNode(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getPerigeeArgument(), finalOrbit.getPerigeeArgument(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getMeanAnomaly(), finalOrbit.getMeanAnomaly(), Double.MIN_VALUE);
        Assertions.assertEquals(0.0, finalOrbitFieldReal.getPosition().distance(finalOrbit.getPosition()), Double.MIN_VALUE);
        Assertions.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getVelocity().distance(finalOrbit.getPVCoordinates().getVelocity()), Double.MIN_VALUE);
    }

    @Test
    public void testOsculatingComparisonWithNonField() {
        doTestOsculatingComparisonWithNonField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestOsculatingComparisonWithNonField(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final Frame inertialFrame = FramesFactory.getEME2000();
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        double timeshift = 60000.0;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = FastMath.toRadians(0); // mean anomaly
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(a), zero.add(e), zero.add(i), zero.add(omega),
                                                                     zero.add(raan), zero.add(lM), PositionAngleType.TRUE,
                                                                     inertialFrame, initDate, zero.add(provider.getMu()));

        // Initial state definition
        final FieldSpacecraftState<T> initialStateField = new FieldSpacecraftState<>(initialOrbit);
        final SpacecraftState         initialState      = initialStateField.toSpacecraftState();

        // Field propagation
        final FieldBrouwerLyddanePropagator<T> blField = new FieldBrouwerLyddanePropagator<>(initialStateField.getOrbit(),
                                                                                             GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);
        final FieldSpacecraftState<T> finalStateField     = blField.propagate(initialStateField.getDate().shiftedBy(timeshift));
        final FieldKeplerianOrbit<T>  finalOrbitField     = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(finalStateField.getOrbit());
        final KeplerianOrbit          finalOrbitFieldReal = finalOrbitField.toOrbit();

        // Classical propagation
        final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(initialState.getOrbit(),
                                                                         GravityFieldFactory.getUnnormalizedProvider(provider),
                                                                         BrouwerLyddanePropagator.M2);
        final SpacecraftState finalState = bl.propagate(initialState.getDate().shiftedBy(timeshift));
        final KeplerianOrbit  finalOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(finalState.getOrbit());

        Assertions.assertEquals(finalOrbitFieldReal.getA(), finalOrbit.getA(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getE(), finalOrbit.getE(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getI(), finalOrbit.getI(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getRightAscensionOfAscendingNode(), finalOrbit.getRightAscensionOfAscendingNode(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getPerigeeArgument(), finalOrbit.getPerigeeArgument(), Double.MIN_VALUE);
        Assertions.assertEquals(finalOrbitFieldReal.getMeanAnomaly(), finalOrbit.getMeanAnomaly(), Double.MIN_VALUE);
        Assertions.assertEquals(0.0, finalOrbitFieldReal.getPosition().distance(finalOrbit.getPosition()), Double.MIN_VALUE);
        Assertions.assertEquals(0.0, finalOrbitFieldReal.getPVCoordinates().getVelocity().distance(finalOrbit.getPVCoordinates().getVelocity()), Double.MIN_VALUE);
    }

    @Test
    public void testMeanOrbit() throws IOException {
        doTestMeanOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMeanOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final UnnormalizedSphericalHarmonicsProvider ushp = GravityFieldFactory.getUnnormalizedProvider(provider);
        final FieldKeplerianOrbit<T> initialOsculating =
            new FieldKeplerianOrbit<>(zero.newInstance(7.8e6), zero.newInstance(0.032), zero.newInstance(0.4),
                                      zero.newInstance(0.1), zero.newInstance(0.2), zero.newInstance(0.3),
                                      PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        final UnnormalizedSphericalHarmonics ush = ushp.onDate(initialOsculating.getDate().toAbsoluteDate());

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = FieldNumericalPropagator.tolerances(zero.newInstance(0.1), initialOsculating, OrbitType.KEPLERIAN);
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> num = new FieldNumericalPropagator<>(field, integrator);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, provider));
        num.setInitialState(new FieldSpacecraftState<>(initialOsculating));
        num.setOrbitType(OrbitType.KEPLERIAN);
        num.setPositionAngleType(initialOsculating.getCachedPositionAngleType());
        final StorelessUnivariateStatistic oscMin  = new Min();
        final StorelessUnivariateStatistic oscMax  = new Max();
        final StorelessUnivariateStatistic meanMin = new Min();
        final StorelessUnivariateStatistic meanMax = new Max();
        num.getMultiplexer().add(zero.newInstance(60), state -> {
            final FieldOrbit<T> osc = state.getOrbit();
            oscMin.increment(osc.getA().getReal());
            oscMax.increment(osc.getA().getReal());
            // compute mean orbit at current date (this is what we test)
            final FieldOrbit<T> mean = FieldBrouwerLyddanePropagator.computeMeanOrbit(state.getOrbit(), 
                                                                                      ushp, ush,
                                                                                      BrouwerLyddanePropagator.M2);
            meanMin.increment(mean.getA().getReal());
            meanMax.increment(mean.getA().getReal());
        });
        num.propagate(initialOsculating.getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(3188.347, oscMax.getResult()  - oscMin.getResult(),  1.0e-3);
        Assertions.assertEquals(  25.794, meanMax.getResult() - meanMin.getResult(), 1.0e-3);

    }

    @Test
    public void testGeostationaryOrbit() {
        doTestGeostationaryOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGeostationaryOrbit(Field<T> field) {

        final Frame eci = FramesFactory.getEME2000();
        final UnnormalizedSphericalHarmonicsProvider ushp = GravityFieldFactory.getUnnormalizedProvider(provider);

        T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // geostationary orbit
        final double sma = FastMath.cbrt(Constants.IERS2010_EARTH_MU /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY);
        final double ecc  = 0.0;
        final double inc  = 0.0;
        final double pa   = 0.0;
        final double raan = 0.0;
        final double lV   = 0.0;
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(sma), zero.add(ecc), zero.add(inc),
                                                              zero.add(pa), zero.add(raan), zero.add(lV),
                                                              PositionAngleType.TRUE,
                                                              eci, date, zero.add(provider.getMu()));

        // set up a BL propagator from mean orbit
        FieldBrouwerLyddanePropagator<T> fbl =
            new FieldBrouwerLyddanePropagator<>(orbit, ushp, PropagationType.MEAN,
                                                BrouwerLyddanePropagator.M2);

        // propagate
        final FieldSpacecraftState<T> state = fbl.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        final KeplerianOrbit orbOsc = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit().toOrbit());

        // Check all elements are defined, i.e. no singularity
        Assertions.assertTrue(Double.isFinite(orbOsc.getA()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getE()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getI()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getPerigeeArgument()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getRightAscensionOfAscendingNode()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getTrueAnomaly()));

        // set up a BL propagator from mean orbit
        FieldBrouwerLyddanePropagator<T> fbl2 =
            new FieldBrouwerLyddanePropagator<>(orbit, ushp, PropagationType.OSCULATING,
                                                BrouwerLyddanePropagator.M2);

        // propagate
        final FieldSpacecraftState<T> state2 = fbl2.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        final KeplerianOrbit orbOsc2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state2.getOrbit().toOrbit());

        // Check all elements are defined, i.e. no singularity
        Assertions.assertTrue(Double.isFinite(orbOsc2.getA()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getE()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getI()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getPerigeeArgument()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getRightAscensionOfAscendingNode()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getTrueAnomaly()));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere:potential/icgem-format");
        provider = GravityFieldFactory.getNormalizedProvider(5, 0);
    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    private NormalizedSphericalHarmonicsProvider provider;
}
