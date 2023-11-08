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
package org.orekit.propagation.analytical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
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
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldElevationDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldNodeDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.FieldDSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class FieldEcksteinHechlerPropagatorTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    private double mu;
    private double ae;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        double[][] cnm = new double[][] {
            { 0 }, { 0 }, { -1.08263e-3 }, { 2.54e-6 }, { 1.62e-6 }, { 2.3e-7 }, { -5.5e-7 }
           };
        double[][] snm = new double[][] {
            { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
           };

        provider = GravityFieldFactory.getUnnormalizedProvider(ae, mu, TideSystem.UNKNOWN, cnm, snm);
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
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<>(initialOrbit, provider);

        // Extrapolation at the initial date
        // ---------------------------------
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPosition(),
                                              finalOrbit.getPosition()).getReal(),
                            1.0e-8);

        // velocity and circular parameters do *not* match, this is EXPECTED!
        // the reason is that we ensure position/velocity are consistent with the
        // evolution of the orbit, and this includes the non-Keplerian effects,
        // whereas the initial orbit is Keplerian only. The implementation of the
        // model is such that rather than having a perfect match at initial point
        // (either in velocity or in circular parameters), we have a propagated orbit
        // that remains close to a numerical reference throughout the orbit.
        // This is shown in the testInitializationCorrectness() where a numerical
        // fit is used to check initialization

        Assertions.assertEquals(0.137,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-3);
        Assertions.assertEquals(125.2, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.1);

    }

    @Test
    public void sameDateKeplerian() {
        doSameDateKeplerian(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doSameDateKeplerian(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // Definition of initial conditions with Keplerian parameters
        // -----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add( 2.1), zero.add( 2.9),
                                                               zero.add(6.2), PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<>(initialOrbit, zero.add(Propagator.DEFAULT_MASS), provider);

        // Extrapolation at the initial date
        // ---------------------------------
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPosition(),
                                              finalOrbit.getPosition()).getReal(),
                            3.0e-8);

        // velocity and circular parameters do *not* match, this is EXPECTED!
        // the reason is that we ensure position/velocity are consistent with the
        // evolution of the orbit, and this includes the non-Keplerian effects,
        // whereas the initial orbit is Keplerian only. The implementation of the
        // model is such that rather than having a perfect match at initial point
        // (either in velocity or in circular parameters), we have a propagated orbit
        // that remains close to a numerical reference throughout the orbit.
        // This is shown in the testInitializationCorrectness() where a numerical
        // fit is used to check initialization
        Assertions.assertEquals(0.137,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-3);
        Assertions.assertEquals(126.8, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.1);

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
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
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
        FieldEcksteinHechlerPropagator<T> extrapolatorAna =
            new FieldEcksteinHechlerPropagator<>(initialOrbit, kepProvider);
        FieldKeplerianPropagator<T> extrapolatorKep = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

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
    public void propagatedCartesian() {
        doPropagatedCartesian(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doPropagatedCartesian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<>(initialOrbit,
                                                 new LofOffset(initialOrbit.getFrame(),
                                                               LOFType.VNC, RotationOrder.XYZ, 0, 0, 0),
                                                 provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        Assertions.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate).getReal(), 1.0e-9);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(finalOrbit.getEquinoctialEx().multiply(
        finalOrbit.getLE().sin())).add(finalOrbit.getEquinoctialEy()
        .multiply(finalOrbit.getLE().cos()));

        Assertions.assertEquals(LM.getReal(), finalOrbit.getLM().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbit.getLM().getReal()));

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()).getReal(), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin()).subtract(
                 initialOrbit.getEquinoctialEx().multiply(initialOrbit.getLE().sin())).subtract(
                 finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos())).add(
                 initialOrbit.getEquinoctialEy().multiply(initialOrbit.getLE().cos()));

        Assertions.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle
                     * FastMath.abs(deltaE.getReal() - delta.getReal()));

        // for final orbit
        T ex = finalOrbit.getEquinoctialEx();
        T ey = finalOrbit.getEquinoctialEy();
        T hx = finalOrbit.getHx();
        T hy = finalOrbit.getHy();
        T LE = finalOrbit.getLE();

        T ex2 = ex.multiply(ex);
        T ey2 = ey.multiply(ey);
        T hx2 = hx.multiply(hx);
        T hy2 = hy.multiply(hy);
        T h2p1 = hx2.add(1.).add(hy2);
        T beta = ex2.negate().add(1.).subtract(ey2).sqrt().add(1.).reciprocal();

        T x3 = ex.negate().add(ey2.multiply(beta).negate().add(1.).multiply(LE.cos())).add(beta.multiply(ex).multiply(ey).multiply(
        LE.sin()));
        T y3 = ey.negate().add(ex2.negate().multiply(beta).add(1).multiply(LE.sin())).add(beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<>(hx2.add(1).subtract(hy2).divide(h2p1),
                                                 hx.multiply(hy).multiply(2).divide(h2p1),
                                                 hy.multiply(-2).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<>(hx.multiply(2).multiply(hy).divide(h2p1),
                                                 hy2.add(1).subtract(hx2).divide(h2p1),
                                                 hx.multiply(2).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<>(finalOrbit.getA(), new FieldVector3D<>(x3, U, y3, V));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm().getReal(),
                            r.getNorm().getReal(),
                            Utils.epsilonTest * r.getNorm().getReal());

    }

    @Test
    public void propagatedKeplerian() {

        doPropagatedKeplerian(Binary64Field.getInstance());

    }

    private <T extends CalculusFieldElement<T>> void doPropagatedKeplerian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // Definition of initial conditions with Keplerian parameters
        // -----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                               zero.add(6.2), PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<>(initialOrbit,
                                                 new LofOffset(initialOrbit.getFrame(), LOFType.VNC),
                                                 zero.add(2000.0), provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        Assertions.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate).getReal(), 1.0e-9);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(finalOrbit.getEquinoctialEx().multiply(
        finalOrbit.getLE().sin())).add(finalOrbit.getEquinoctialEy().multiply(
        finalOrbit.getLE().cos()));

        Assertions.assertEquals(LM.getReal(), finalOrbit.getLM().getReal(), Utils.epsilonAngle);

        // test of tan((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()).getReal(), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin()).subtract(
                initialOrbit.getEquinoctialEx().multiply(initialOrbit.getLE().sin())).subtract(
                  finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos())).add(
                initialOrbit.getEquinoctialEy().multiply(initialOrbit.getLE().cos()));

        Assertions.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle
                     * FastMath.abs(deltaE.getReal() - delta.getReal()));

        // for final orbit
        T ex = finalOrbit.getEquinoctialEx();
        T ey = finalOrbit.getEquinoctialEy();
        T hx = finalOrbit.getHx();
        T hy = finalOrbit.getHy();
        T LE = finalOrbit.getLE();

        T ex2 = ex.multiply( ex);
        T ey2 = ey.multiply( ey);
        T hx2 = hx.multiply( hx);
        T hy2 = hy.multiply( hy);
        T h2p1 = hx2.add(1).add(hy2);
        T beta = ex2.negate().add(1.).subtract(ey2).sqrt().add(1).reciprocal();

        T x3 = ex.negate().add(beta.negate().multiply(ey2).add(1).multiply(LE.cos())).add(
               beta.multiply(ex).multiply(ey).multiply(LE.sin()));
        T y3 = ey.negate().add(beta.negate().multiply(ex2).add(1).multiply(LE.sin())).add(
              beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<>(hx2.subtract(hy2).add(1.).divide(h2p1),
                                                 hx.multiply(2).multiply(hy).divide(h2p1),
                                                 hy.multiply(-2.).divide(h2p1));
        FieldVector3D<T> V = new FieldVector3D<>(hx.multiply(2.).multiply(hy).divide(h2p1),
                                                 hy2.subtract(hx2).add(1.).divide(h2p1),
                                                 hx.multiply(2).divide(h2p1));
        FieldVector3D<T> r = new FieldVector3D<>(finalOrbit.getA(), new FieldVector3D<>(x3, U, y3, V));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm().getReal(), r.getNorm().getReal(),
                     Utils.epsilonTest * r.getNorm().getReal());

    }

    @Test
    public void undergroundOrbit() {
        doUndergroundOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doUndergroundOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // for a semi major axis < equatorial radius
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add( 1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(800.0), zero.add(100.0));
        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }

    @Test
    public void equatorialOrbit() {
        doEquatorialOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doEquatorialOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldCircularOrbit<>(zero.add(7000000), zero.add(1.0e-4), zero.add(-1.5e-4),
                                                              zero, zero.add(1.2), zero.add(2.3), PositionAngleType.MEAN,
                                                              FramesFactory.getEME2000(),
                                                              initDate, zero.add(provider.getMu()));
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.ALMOST_EQUATORIAL_ORBIT, oe.getSpecifier());
        }
    }

    @Test
    public void criticalInclination() {
        doCriticalInclination(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCriticalInclination(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field);
        FieldOrbit<T> initialOrbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(-3862363.8474653554),
                                                                                                           zero.add(-3521533.9758022362),
                                                                                                           zero.add(4647637.852558916)),
                                                                                       new FieldVector3D<>(zero.add(65.36170817232278),
                                                                                                           zero.add(-6056.563439401233),
                                                                                                           zero.add(-4511.1247889782757))),
                                                              FramesFactory.getEME2000(),
                                                              initDate, zero.add(provider.getMu()));

        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.ALMOST_CRITICALLY_INCLINED_ORBIT, oe.getSpecifier());
        }
    }

    @Test
    public void tooEllipticalOrbit() {
        doTooEllipticalOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTooEllipticalOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // for an eccentricity too big for the model
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add( 4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add( 8000.0), zero.add(1000.0));
        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(provider.getMu()));
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, oe.getSpecifier());
        }
    }

    @Test
    public void hyperbolic() {
        doHyperbolic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doHyperbolic(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldKeplerianOrbit<T> hyperbolic =
            new FieldKeplerianOrbit<>(zero.add(-1.0e10), zero.add(2), zero, zero, zero, zero, PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        try {
            FieldEcksteinHechlerPropagator<T> propagator =
                            new FieldEcksteinHechlerPropagator<>(hyperbolic, provider);
            propagator.propagate(date.shiftedBy(10.0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }

    @Test
    public void wrongAttitude() {
        doWrongAttitude(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doWrongAttitude(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(1.0e10), zero.add(1.0e-4), zero.add(1.0e-2), zero, zero, zero, PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        final DummyLocalizable gasp = new DummyLocalizable("gasp");
        AttitudeProvider wrongLaw = new AttitudeProvider() {

            @Override
            public Attitude getAttitude(PVCoordinatesProvider pvProv,
                                        AbsoluteDate date, Frame frame)
                {
                throw new OrekitException(gasp, new RuntimeException());
            }

            @Override
            public <Q extends CalculusFieldElement<Q>> FieldAttitude<Q>
                getAttitude(FieldPVCoordinatesProvider<Q> pvProv,
                            FieldAbsoluteDate<Q> date, Frame frame)
                    {
                throw new OrekitException(gasp, new RuntimeException());
            }
        };
        try {
            FieldEcksteinHechlerPropagator<T> propagator =
                            new FieldEcksteinHechlerPropagator<>(orbit, wrongLaw, provider);
            propagator.propagate(date.shiftedBy(10.0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertSame(gasp, oe.getSpecifier());
        }
    }

    @Test
    public void testAcceleration() {
        doTestAcceleration(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestAcceleration(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        FieldAbsoluteDate<T> target = date.shiftedBy(10000.0);
        List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<TimeStampedFieldPVCoordinates<T>>();
        for (double dt : Arrays.asList(-0.5, 0.0, 0.5)) {
            sample.add(propagator.propagate(target.shiftedBy(dt)).getPVCoordinates());
        }

        // create interpolator
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<T>, T> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedFieldPVCoordinates<T> interpolated = interpolator.interpolate(target, sample);
        FieldVector3D<T> computedP     = sample.get(1).getPosition();
        FieldVector3D<T> computedV     = sample.get(1).getVelocity();
        FieldVector3D<T> referenceP    = interpolated.getPosition();
        FieldVector3D<T> referenceV    = interpolated.getVelocity();
        FieldVector3D<T> computedA     = sample.get(1).getAcceleration();
        FieldVector3D<T> referenceA    = interpolated.getAcceleration();
        final FieldCircularOrbit<T> propagated = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(propagator.propagateOrbit(target, null));
        final FieldCircularOrbit<T> keplerian =
                new FieldCircularOrbit<>(propagated.getA(),
                                         propagated.getCircularEx(),
                                         propagated.getCircularEy(),
                                         propagated.getI(),
                                         propagated.getRightAscensionOfAscendingNode(),
                                         propagated.getAlphaM(), PositionAngleType.MEAN,
                                         propagated.getFrame(),
                                         propagated.getDate(),
                                         propagated.getMu());
        FieldVector3D<T> keplerianP    = keplerian.getPosition();
        FieldVector3D<T> keplerianV    = keplerian.getPVCoordinates().getVelocity();
        FieldVector3D<T> keplerianA    = keplerian.getPVCoordinates().getAcceleration();

        // perturbed orbit position should be similar to Keplerian orbit position
        Assertions.assertEquals(0.0, FieldVector3D.distance(referenceP, computedP).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(referenceP, keplerianP).getReal(), 4.0e-9);

        // perturbed orbit velocity should be equal to Keplerian orbit because
        // it was in fact reconstructed from Cartesian coordinates
        T computationErrorV   = FieldVector3D.distance(referenceV, computedV);
        T nonKeplerianEffectV = FieldVector3D.distance(referenceV, keplerianV);
        Assertions.assertEquals(0.0, nonKeplerianEffectV.getReal() - computationErrorV.getReal(), 9.0e-12);
        Assertions.assertEquals(2.2e-4, computationErrorV.getReal(), 3.0e-6);

        // perturbed orbit acceleration should be different from Keplerian orbit because
        // Keplerian orbit doesn't take orbit shape changes into account
        // perturbed orbit acceleration should be consistent with position evolution
        T computationErrorA   = FieldVector3D.distance(referenceA, computedA);
        T nonKeplerianEffectA = FieldVector3D.distance(referenceA, keplerianA);
        Assertions.assertEquals(1.0e-7,  computationErrorA.getReal(), 6.0e-9);
        Assertions.assertEquals(6.37e-3, nonKeplerianEffectA.getReal(), 7.0e-6);
        Assertions.assertTrue(computationErrorA.getReal() < nonKeplerianEffectA.getReal() / 60000);

    }

    @Test
    public void ascendingNode() {
        doAscendingNode(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doAscendingNode(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        FieldNodeDetector<T> detector = new FieldNodeDetector<>(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(FramesFactory.getITRF(IERSConventions.IERS_2010, true) == detector.getFrame());
        propagator.addEventDetector(detector);

        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);

        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);

        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3500.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 4000.0);

        Assertions.assertEquals(0, pv.getPosition().getZ().getReal(), 1.0e-6);
        Assertions.assertTrue(pv.getVelocity().getZ().getReal() > 0);
        Collection<FieldEventDetector<T>> detectors = propagator.getEventsDetectors();
        Assertions.assertEquals(1, detectors.size());
        propagator.clearEventsDetectors();
        Assertions.assertEquals(0, propagator.getEventsDetectors().size());
    }

    @Test
    public void stopAtTargetDate() {
        doStopAtTargetDate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doStopAtTargetDate(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        Frame itrf =  FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new FieldNodeDetector<>(orbit, itrf).
                                    withHandler(new FieldContinueOnEvent<T>()));
        FieldAbsoluteDate<T> farTarget = orbit.getDate().shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate()).getReal()), 1.0e-3);
    }

    @Test
    public void perigee() {
        doPerigee(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doPerigee(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        propagator.addEventDetector(new FieldApsideDetector<>(orbit));
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldVector3D<T> position = propagated.getPosition(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3000.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 3500.0);
        Assertions.assertEquals(orbit.getA().getReal() * (1.0 - orbit.getE().getReal()), position.getNorm().getReal(), 410);
    }

    @Test
    public void date() {
        doDate(Binary64Field.getInstance());

    }

    private <T extends CalculusFieldElement<T>> void doDate(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        final FieldAbsoluteDate<T> stopDate = date.shiftedBy(500.0);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> detector = new FieldDateDetector<>(field, stopDate);
        propagator.addEventDetector(detector);
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0, stopDate.durationFrom(propagated.getDate()).getReal(), 1.0e-10);
    }

    @Test
    public void fixedStep() {

        doFixedStep(Binary64Field.getInstance());


    }

    private <T extends CalculusFieldElement<T>> void doFixedStep(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        final T step = zero.add(100.0);
        propagator.setStepHandler(step, new FieldOrekitFixedStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            @Override
            public void handleStep(FieldSpacecraftState<T> currentState) {
                if (previous != null) {
                    Assertions.assertEquals(step.getReal(), currentState.getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void setting() {

        doSetting(Binary64Field.getInstance());

    }

    private <T extends CalculusFieldElement<T>> void doSetting(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, provider);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        FieldElevationDetector<T> detector = new FieldElevationDetector<>(zero.add(60), zero.add(1.0e-9), topo).withConstantElevation(0.09);
        Assertions.assertEquals(0.09, detector.getMinElevation(), 1.0e-12);
        Assertions.assertTrue(topo == detector.getTopocentricFrame());
        propagator.addEventDetector(detector);

        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        final double elevation = topo.
                                 getTrackingCoordinates(propagated.getPosition().toVector3D(),
                                                        propagated.getFrame(),
                                                        propagated.getDate().toAbsoluteDate()).
                                 getElevation();
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ().getReal();
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 7800.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 7900.0,
                "Incorrect value " + farTarget.durationFrom(propagated.getDate()) + " !< 7900");
        Assertions.assertEquals(0.09, elevation, 1.0e-11);
        Assertions.assertTrue(zVelocity < 0);
    }

    @Test
    public void testIssue504() {
        doTestIssue504(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue504(Field<T> field) {
        final T zero = field.getZero();
        // LEO orbit
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2018, 07, 15), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final FieldSpacecraftState<T> initialState =  new FieldSpacecraftState<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                                                             FramesFactory.getEME2000(),
                                                                                                             initDate,
                                                                                                             zero.add(provider.getMu())));

        // Mean state computation
        final List<DSSTForceModel> models = new ArrayList<>();
        models.add(new DSSTZonal(provider));
        final FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(initialState, DEFAULT_LAW, models);

        // Initialize Eckstein-Hechler model with mean state
        final FieldEcksteinHechlerPropagator<T> propagator = new FieldEcksteinHechlerPropagator<>(meanState.getOrbit(), provider, PropagationType.MEAN);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate);

        // Verify
        Assertions.assertEquals(initialState.getA().getReal(),             finalState.getA().getReal(),             18.0);
        Assertions.assertEquals(initialState.getEquinoctialEx().getReal(), finalState.getEquinoctialEx().getReal(), 1.0e-6);
        Assertions.assertEquals(initialState.getEquinoctialEy().getReal(), finalState.getEquinoctialEy().getReal(), 5.0e-6);
        Assertions.assertEquals(initialState.getHx().getReal(),            finalState.getHx().getReal(),            1.0e-6);
        Assertions.assertEquals(initialState.getHy().getReal(),            finalState.getHy().getReal(),            2.0e-6);
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialState.getPosition(),
                                                   finalState.getPosition()).getReal(),
                            11.4);
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialState.getPVCoordinates().getVelocity(),
                                                   finalState.getPVCoordinates().getVelocity()).getReal(),
                            4.2e-2);
    }

    @Test
    public void testIssue504Bis() {
        doTestIssue504Bis(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue504Bis(Field<T> field) {
        final T zero = field.getZero();
        // LEO orbit
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2018, 07, 15), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final FieldSpacecraftState<T> initialState =  new FieldSpacecraftState<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                                                             FramesFactory.getEME2000(),
                                                                                                             initDate,
                                                                                                             zero.add(provider.getMu())));

        // Mean state computation
        final List<DSSTForceModel> models = new ArrayList<>();
        models.add(new DSSTZonal(provider));
        final FieldSpacecraftState<T> meanState = FieldDSSTPropagator.computeMeanState(initialState, DEFAULT_LAW, models);

        // Initialize Eckstein-Hechler model with mean state
        final FieldEcksteinHechlerPropagator<T> propagator = new FieldEcksteinHechlerPropagator<>(meanState.getOrbit(), DEFAULT_LAW, zero.add(498.5), provider, PropagationType.MEAN);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate);

        // Verify
        Assertions.assertEquals(initialState.getA().getReal(),             finalState.getA().getReal(),             18.0);
        Assertions.assertEquals(initialState.getEquinoctialEx().getReal(), finalState.getEquinoctialEx().getReal(), 1.0e-6);
        Assertions.assertEquals(initialState.getEquinoctialEy().getReal(), finalState.getEquinoctialEy().getReal(), 5.0e-6);
        Assertions.assertEquals(initialState.getHx().getReal(),            finalState.getHx().getReal(),            1.0e-6);
        Assertions.assertEquals(initialState.getHy().getReal(),            finalState.getHy().getReal(),            2.0e-6);
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialState.getPosition(),
                                                   finalState.getPosition()).getReal(),
                            11.4);
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(initialState.getPVCoordinates().getVelocity(),
                                                   finalState.getPVCoordinates().getVelocity()).getReal(),
                            4.2e-2);
    }

    @Test
    public void testMeanOrbit() throws IOException {
        doTestMeanOrbit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMeanOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> initialOsculating =
            new FieldKeplerianOrbit<>(zero.newInstance(7.8e6), zero.newInstance(0.032), zero.newInstance(0.4),
                                      zero.newInstance(0.1), zero.newInstance(0.2), zero.newInstance(0.3),
                                      PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), date, zero.add(provider.getMu()));
        final UnnormalizedSphericalHarmonics ush = provider.onDate(initialOsculating.getDate().toAbsoluteDate());

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = FieldNumericalPropagator.tolerances(zero.newInstance(0.1), initialOsculating, OrbitType.CIRCULAR);
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> num = new FieldNumericalPropagator<>(field, integrator);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(provider)));
        num.setInitialState(new FieldSpacecraftState<>(initialOsculating));
        num.setOrbitType(OrbitType.CIRCULAR);
        final StorelessUnivariateStatistic oscMin  = new Min();
        final StorelessUnivariateStatistic oscMax  = new Max();
        final StorelessUnivariateStatistic meanMin = new Min();
        final StorelessUnivariateStatistic meanMax = new Max();
        num.getMultiplexer().add(zero.newInstance(60), state -> {
            final FieldOrbit<T> osc = state.getOrbit();
            oscMin.increment(osc.getA().getReal());
            oscMax.increment(osc.getA().getReal());
            // compute mean orbit at current date (this is what we test)
            final FieldOrbit<T> mean = FieldEcksteinHechlerPropagator.computeMeanOrbit(state.getOrbit(), provider, ush);
            meanMin.increment(mean.getA().getReal());
            meanMax.increment(mean.getA().getReal());
        });
        num.propagate(initialOsculating.getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(3190.029, oscMax.getResult()  - oscMin.getResult(),  1.0e-3);
        Assertions.assertEquals(  49.638, meanMax.getResult() - meanMin.getResult(), 1.0e-3);

    }

    private <T extends CalculusFieldElement<T>> T tangLEmLv(T Lv, T ex, T ey) {
        // tan ((LE - Lv) /2)) =
        return ey.multiply(Lv.cos()).subtract(ex.multiply(Lv.sin())).divide(
         ex.multiply(Lv.cos()).add(1.0).add(ey.multiply(Lv.sin()).add(ex.negate().multiply(ex).add(1.0).subtract(
                                                                 ey.multiply(ey)).sqrt())));

    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    private UnnormalizedSphericalHarmonicsProvider provider;

}
