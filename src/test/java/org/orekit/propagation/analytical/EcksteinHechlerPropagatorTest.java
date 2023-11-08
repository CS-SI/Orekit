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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.stat.descriptive.StorelessUnivariateStatistic;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
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
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.propagation.conversion.FiniteDifferencePropagatorConverter;
import org.orekit.propagation.conversion.PropagatorConverter;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;


public class EcksteinHechlerPropagatorTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @Test
    public void sameDateCartesian() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit, provider);

        // Extrapolation at the initial date
        // ---------------------------------
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPosition(),
                                              finalOrbit.getPosition()),
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
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-3);
        Assertions.assertEquals(125.2, finalOrbit.getA() - initialOrbit.getA(), 0.1);

    }

    @Test
    public void sameDateKeplerian() {

        // Definition of initial conditions with Keplerian parameters
        // -----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngleType.TRUE,
                                                FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit, Propagator.DEFAULT_MASS, provider);

        // Extrapolation at the initial date
        // ---------------------------------
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPosition(),
                                              finalOrbit.getPosition()),
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
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-3);
        Assertions.assertEquals(126.8, finalOrbit.getA() - initialOrbit.getA(), 0.1);

    }

    @Test
    public void almostSphericalBody() {

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

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
        EcksteinHechlerPropagator extrapolatorAna =
            new EcksteinHechlerPropagator(initialOrbit, 1000.0, kepProvider);
        KeplerianPropagator extrapolatorKep = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        SpacecraftState finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assertions.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assertions.assertEquals(finalOrbitAna.getA(), finalOrbitKep.getA(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEx(), finalOrbitKep.getEquinoctialEx(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEy(), finalOrbitKep.getEquinoctialEy(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx(), finalOrbitKep.getHx()),
                     finalOrbitKep.getHx(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy(), finalOrbitKep.getHy()),
                     finalOrbitKep.getHy(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv(), finalOrbitKep.getLv()),
                     finalOrbitKep.getLv(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE(), finalOrbitKep.getLE()),
                     finalOrbitKep.getLE(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM(), finalOrbitKep.getLM()),
                     finalOrbitKep.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM()));

    }

    @Test
    public void propagatedCartesian() {
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                                          new LofOffset(initialOrbit.getFrame(),
                                                        LOFType.VNC, RotationOrder.XYZ, 0, 0, 0),
                                          provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        Assertions.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
        * FastMath.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
        * FastMath.cos(finalOrbit.getLE());

        Assertions.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbit.getLM()));

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta = finalOrbit.getEquinoctialEx() * FastMath.sin(finalOrbit.getLE())
        - initialOrbit.getEquinoctialEx() * FastMath.sin(initialOrbit.getLE())
        - finalOrbit.getEquinoctialEy() * FastMath.cos(finalOrbit.getLE())
        + initialOrbit.getEquinoctialEy() * FastMath.cos(initialOrbit.getLE());

        Assertions.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
                     * FastMath.abs(deltaE - delta));

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex * ex;
        double ey2 = ey * ey;
        double hx2 = hx * hx;
        double hy2 = hy * hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + FastMath.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1. - beta * ey2) * FastMath.cos(LE) + beta * ex * ey
        * FastMath.sin(LE);
        double y3 = -ey + (1. - beta * ex2) * FastMath.sin(LE) + beta * ex * ey
        * FastMath.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1, (2. * hx * hy) / h2p1,
                                  (-2. * hy) / h2p1);

        Vector3D V = new Vector3D((2. * hx * hy) / h2p1, (1. - hx2 + hy2) / h2p1,
                                  (2. * hx) / h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm(), r.getNorm(),
                     Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() {
        // Definition of initial conditions with Keplerian parameters
        // -----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                              6.2, PositionAngleType.TRUE,
                                              FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                                          new LofOffset(initialOrbit.getFrame(),
                                                        LOFType.VNC, RotationOrder.XYZ, 0, 0, 0),
                                          2000.0, provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        Assertions.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
        * FastMath.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
        * FastMath.cos(finalOrbit.getLE());

        Assertions.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta = finalOrbit.getEquinoctialEx() * FastMath.sin(finalOrbit.getLE())
        - initialOrbit.getEquinoctialEx() * FastMath.sin(initialOrbit.getLE())
        - finalOrbit.getEquinoctialEy() * FastMath.cos(finalOrbit.getLE())
        + initialOrbit.getEquinoctialEy() * FastMath.cos(initialOrbit.getLE());

        Assertions.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
                     * FastMath.abs(deltaE - delta));

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex * ex;
        double ey2 = ey * ey;
        double hx2 = hx * hx;
        double hy2 = hy * hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + FastMath.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1. - beta * ey2) * FastMath.cos(LE) + beta * ex * ey
        * FastMath.sin(LE);
        double y3 = -ey + (1. - beta * ex2) * FastMath.sin(LE) + beta * ex * ey
        * FastMath.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1, (2. * hx * hy) / h2p1,
                                  (-2. * hy) / h2p1);

        Vector3D V = new Vector3D((2. * hx * hy) / h2p1, (1. - hx2 + hy2) / h2p1,
                                  (2. * hx) / h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm(), r.getNorm(),
                     Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void undergroundOrbit() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // for a semi major axis < equatorial radius
            Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            Vector3D velocity = new Vector3D(-500.0, 800.0, 100.0);
            AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
            Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                    FramesFactory.getEME2000(), initDate, provider.getMu());
            // Extrapolator definition
            // -----------------------
            EcksteinHechlerPropagator extrapolator =
                    new EcksteinHechlerPropagator(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
        });
    }

    @Test
    public void equatorialOrbit() {
        Assertions.assertThrows(OrekitException.class, () -> {
            AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
            Orbit initialOrbit = new CircularOrbit(7000000, 1.0e-4, -1.5e-4,
                    0.0, 1.2, 2.3, PositionAngleType.MEAN,
                    FramesFactory.getEME2000(),
                    initDate, provider.getMu());
            // Extrapolator definition
            // -----------------------
            EcksteinHechlerPropagator extrapolator =
                    new EcksteinHechlerPropagator(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
        });
    }

    @Test
    public void criticalInclination() {
        Assertions.assertThrows(OrekitException.class, () -> {
            AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
            Orbit initialOrbit = new CircularOrbit(new PVCoordinates(new Vector3D(-3862363.8474653554,
                    -3521533.9758022362,
                    4647637.852558916),
                    new Vector3D(65.36170817232278,
                            -6056.563439401233,
                            -4511.1247889782757)),
                    FramesFactory.getEME2000(),
                    initDate, provider.getMu());

            // Extrapolator definition
            // -----------------------
            EcksteinHechlerPropagator extrapolator =
                    new EcksteinHechlerPropagator(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
        });
    }

    @Test
    public void tooEllipticalOrbit() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // for an eccentricity too big for the model
            Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
            Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                    FramesFactory.getEME2000(), initDate, provider.getMu());
            // Extrapolator definition
            // -----------------------
            EcksteinHechlerPropagator extrapolator =
                    new EcksteinHechlerPropagator(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
        });
    }

    @Test
    public void hyperbolic() {
        Assertions.assertThrows(OrekitException.class, () -> {
            KeplerianOrbit hyperbolic =
                    new KeplerianOrbit(-1.0e10, 2, 0, 0, 0, 0, PositionAngleType.TRUE,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
            EcksteinHechlerPropagator propagator =
                    new EcksteinHechlerPropagator(hyperbolic, provider);
            propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
        });
    }

    @Test
    public void wrongAttitude() {
        Assertions.assertThrows(OrekitException.class, () -> {
            KeplerianOrbit orbit =
                    new KeplerianOrbit(1.0e10, 1.0e-4, 1.0e-2, 0, 0, 0, PositionAngleType.TRUE,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
            AttitudeProvider wrongLaw = new AttitudeProvider() {
                public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                    throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
                }
                public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                        FieldAbsoluteDate<T> date, Frame frame)
                {
                    throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
                }
            };
            EcksteinHechlerPropagator propagator =
                    new EcksteinHechlerPropagator(orbit, wrongLaw, provider);
            propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
        });
    }

    @Test
    public void testAcceleration() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        AbsoluteDate target = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt : Arrays.asList(-0.5, 0.0, 0.5)) {
            sample.add(propagator.propagate(target.shiftedBy(dt)).getPVCoordinates());
        }

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedPVCoordinates interpolated = interpolator.interpolate(target, sample);
        Vector3D computedP     = sample.get(1).getPosition();
        Vector3D computedV     = sample.get(1).getVelocity();
        Vector3D referenceP    = interpolated.getPosition();
        Vector3D referenceV    = interpolated.getVelocity();
        Vector3D computedA     = sample.get(1).getAcceleration();
        Vector3D referenceA    = interpolated.getAcceleration();
        final CircularOrbit propagated = (CircularOrbit) OrbitType.CIRCULAR.convertType(propagator.propagateOrbit(target));
        final CircularOrbit keplerian =
                new CircularOrbit(propagated.getA(),
                                  propagated.getCircularEx(),
                                  propagated.getCircularEy(),
                                  propagated.getI(),
                                  propagated.getRightAscensionOfAscendingNode(),
                                  propagated.getAlphaM(), PositionAngleType.MEAN,
                                  propagated.getFrame(),
                                  propagated.getDate(),
                                  propagated.getMu());
        Vector3D keplerianP    = keplerian.getPosition();
        Vector3D keplerianV    = keplerian.getPVCoordinates().getVelocity();
        Vector3D keplerianA    = keplerian.getPVCoordinates().getAcceleration();

        // perturbed orbit position should be similar to Keplerian orbit position
        Assertions.assertEquals(0.0, Vector3D.distance(referenceP, computedP), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(referenceP, keplerianP), 4.0e-9);

        // perturbed orbit velocity should be equal to Keplerian orbit because
        // it was in fact reconstructed from Cartesian coordinates
        double computationErrorV   = Vector3D.distance(referenceV, computedV);
        double nonKeplerianEffectV = Vector3D.distance(referenceV, keplerianV);
        Assertions.assertEquals(nonKeplerianEffectV, computationErrorV, 2.0e-12);
        Assertions.assertEquals(2.2e-4, computationErrorV, 3.0e-6);

        // perturbed orbit acceleration should be different from Keplerian orbit because
        // Keplerian orbit doesn't take orbit shape changes into account
        // perturbed orbit acceleration should be consistent with position evolution
        double computationErrorA   = Vector3D.distance(referenceA, computedA);
        double nonKeplerianEffectA = Vector3D.distance(referenceA, keplerianA);
        Assertions.assertEquals(8.0e-8,  computationErrorA, 2.0e-9);
        Assertions.assertEquals(6.37e-3, nonKeplerianEffectA, 7.0e-6);
        Assertions.assertTrue(computationErrorA < nonKeplerianEffectA / 60000);

    }

    @Test
    public void ascendingNode() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        NodeDetector detector = new NodeDetector(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(FramesFactory.getITRF(IERSConventions.IERS_2010, true) == detector.getFrame());
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assertions.assertEquals(0, pv.getPosition().getZ(), 1.0e-6);
        Assertions.assertTrue(pv.getVelocity().getZ() > 0);
        Collection<EventDetector> detectors = propagator.getEventsDetectors();
        Assertions.assertEquals(1, detectors.size());
        propagator.clearEventsDetectors();
        Assertions.assertEquals(0, propagator.getEventsDetectors().size());
    }

    @Test
    public void stopAtTargetDate() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        Frame itrf =  FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new NodeDetector(orbit, itrf).withHandler(new ContinueOnEvent()));
        AbsoluteDate farTarget = orbit.getDate().shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate())), 1.0e-3);
    }

    @Test
    public void perigee() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        propagator.addEventDetector(new ApsideDetector(orbit));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Vector3D pos = propagated.getPosition(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assertions.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pos.getNorm(), 410);
    }

    @Test
    public void date() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void fixedStep() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final double step = 100.0;
        propagator.setStepHandler(step, new OrekitFixedStepHandler() {
            private AbsoluteDate previous;
            public void handleStep(SpacecraftState currentState) {
                if (previous != null) {
                    Assertions.assertEquals(step, currentState.getDate().durationFrom(previous), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void setting() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        ElevationDetector detector = new ElevationDetector(60, 1.0e-9, topo).withConstantElevation(0.09);
        Assertions.assertEquals(0.09, detector.getMinElevation(), 1.0e-12);
        Assertions.assertTrue(topo == detector.getTopocentricFrame());
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getTrackingCoordinates(propagated.getPosition(),
                                                             propagated.getFrame(),
                                                             propagated.getDate()).
                                 getElevation();
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0,"Incorrect value " + farTarget.durationFrom(propagated.getDate()) + " !< 7900");
        Assertions.assertEquals(0.09, elevation, 1.0e-11);
        Assertions.assertTrue(zVelocity < 0);
    }

    @Test
    public void testInitializationCorrectness()
        throws IOException {

        //  Definition of initial conditions
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getStaticTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                       FastMath.toRadians(97.40236024565775),
                                                       FastMath.toRadians(166.15873160992115),
                                                       FastMath.toRadians(90.1282370098961), PositionAngleType.MEAN,
                                                       poleAligned, date, provider.getMu());

        // find the default Eckstein-Hechler propagator initialized from the initial orbit
        EcksteinHechlerPropagator defaultEH = new EcksteinHechlerPropagator(initial, provider);

        // the osculating parameters recomputed by the default Eckstein-Hechler propagator are quite different
        // from initial orbit
        CircularOrbit defaultOrbit = (CircularOrbit) OrbitType.CIRCULAR.convertType(defaultEH.propagateOrbit(initial.getDate()));
        Assertions.assertEquals(267.4, defaultOrbit.getA() - initial.getA(), 0.1);

        // the position on the other hand match perfectly
        Assertions.assertEquals(0.0,
                            Vector3D.distance(defaultOrbit.getPosition(),
                                              initial.getPosition()),
                            1.0e-8);

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = NumericalPropagator.tolerances(0.1, initial, OrbitType.CIRCULAR);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator num = new NumericalPropagator(integrator);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(provider)));
        num.setInitialState(new SpacecraftState(initial));
        num.setOrbitType(OrbitType.CIRCULAR);

        // find the best Eckstein-Hechler propagator that match the orbit evolution
        PropagatorConverter converter =
                new FiniteDifferencePropagatorConverter(new EcksteinHechlerPropagatorBuilder(initial,
                                                                                             provider,
                                                                                             PositionAngleType.TRUE,
                                                                                             1.0),
                                                        1.0e-6, 100);
        EcksteinHechlerPropagator fittedEH =
                (EcksteinHechlerPropagator) converter.convert(num, 3 * initial.getKeplerianPeriod(), 300);

        // the default Eckstein-Hechler propagator did however quite a good job, as it found
        // an orbit close to the best fitting
        CircularOrbit fittedOrbit  = (CircularOrbit) OrbitType.CIRCULAR.convertType(fittedEH.propagateOrbit(initial.getDate()));
        Assertions.assertEquals(0.623, defaultOrbit.getA() - fittedOrbit.getA(), 0.1);

        // the position on the other hand are slightly different
        // because the fitted orbit minimizes the residuals over a complete time span,
        // not on a single point
        Assertions.assertEquals(58.0,
                            Vector3D.distance(defaultOrbit.getPosition(),
                                              fittedOrbit.getPosition()),
                            0.1);

    }

    @Test
    public void testIssue504() {
        // LEO orbit
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2018, 07, 15), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final SpacecraftState initialState =  new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                                                       FramesFactory.getEME2000(),
                                                                                       initDate,
                                                                                       provider.getMu()));

        // Mean state computation
        final List<DSSTForceModel> models = new ArrayList<>();
        models.add(new DSSTZonal(provider));
        final SpacecraftState meanState = DSSTPropagator.computeMeanState(initialState, DEFAULT_LAW, models);

        // Initialize Eckstein-Hechler model with mean state
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(meanState.getOrbit(), provider, PropagationType.MEAN);
        final SpacecraftState finalState = propagator.propagate(initDate);

        // Verify
        Assertions.assertEquals(initialState.getA(),             finalState.getA(),             18.0);
        Assertions.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-6);
        Assertions.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 5.0e-6);
        Assertions.assertEquals(initialState.getHx(),            finalState.getHx(),            1.0e-6);
        Assertions.assertEquals(initialState.getHy(),            finalState.getHy(),            2.0e-6);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialState.getPosition(),
                                              finalState.getPosition()),
                            11.4);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialState.getPVCoordinates().getVelocity(),
                                              finalState.getPVCoordinates().getVelocity()),
                            4.2e-2);
    }

    @Test
    public void testIssue504Bis() {
        // LEO orbit
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2018, 07, 15), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final SpacecraftState initialState =  new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                                                       FramesFactory.getEME2000(),
                                                                                       initDate,
                                                                                       provider.getMu()));

        // Mean state computation
        final List<DSSTForceModel> models = new ArrayList<>();
        models.add(new DSSTZonal(provider));
        final SpacecraftState meanState = DSSTPropagator.computeMeanState(initialState, DEFAULT_LAW, models);

        // Initialize Eckstein-Hechler model with mean state
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(meanState.getOrbit(), DEFAULT_LAW, 458.6, provider, PropagationType.MEAN);
        final SpacecraftState finalState = propagator.propagate(initDate);

        // Verify
        Assertions.assertEquals(initialState.getA(),             finalState.getA(),             18.0);
        Assertions.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-6);
        Assertions.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 5.0e-6);
        Assertions.assertEquals(initialState.getHx(),            finalState.getHx(),            1.0e-6);
        Assertions.assertEquals(initialState.getHy(),            finalState.getHy(),            2.0e-6);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialState.getPosition(),
                                              finalState.getPosition()),
                            11.4);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialState.getPVCoordinates().getVelocity(),
                                              finalState.getPVCoordinates().getVelocity()),
                            4.2e-2);
    }

    @Test
    public void testMeanOrbit() {
        final KeplerianOrbit initialOsculating =
                        new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());
        final UnnormalizedSphericalHarmonics ush = provider.onDate(initialOsculating.getDate());

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = NumericalPropagator.tolerances(0.1, initialOsculating, OrbitType.CIRCULAR);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator num = new NumericalPropagator(integrator);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(provider)));
        num.setInitialState(new SpacecraftState(initialOsculating));
        num.setOrbitType(OrbitType.CIRCULAR);
        final StorelessUnivariateStatistic oscMin  = new Min();
        final StorelessUnivariateStatistic oscMax  = new Max();
        final StorelessUnivariateStatistic meanMin = new Min();
        final StorelessUnivariateStatistic meanMax = new Max();
        num.getMultiplexer().add(60, state -> {
            final Orbit osc = state.getOrbit();
            oscMin.increment(osc.getA());
            oscMax.increment(osc.getA());
            // compute mean orbit at current date (this is what we test)
            final Orbit mean = EcksteinHechlerPropagator.computeMeanOrbit(state.getOrbit(), provider, ush);
            meanMin.increment(mean.getA());
            meanMax.increment(mean.getA());
        });
        num.propagate(initialOsculating.getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(3190.029, oscMax.getResult()  - oscMin.getResult(),  1.0e-3);
        Assertions.assertEquals(  49.638, meanMax.getResult() - meanMin.getResult(), 1.0e-3);

    }

    private static double tangLEmLv(double Lv, double ex, double ey) {
        // tan ((LE - Lv) /2)) =
        return (ey * FastMath.cos(Lv) - ex * FastMath.sin(Lv))
        / (1 + ex * FastMath.cos(Lv) + ey * FastMath.sin(Lv) + FastMath.sqrt(1 - ex * ex
                                                                 - ey * ey));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double[][] cnm = new double[][] {
            { 0 }, { 0 }, { -1.08263e-3 }, { 2.54e-6 }, { 1.62e-6 }, { 2.3e-7 }, { -5.5e-7 }
           };
        double[][] snm = new double[][] {
            { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
           };
        provider = GravityFieldFactory.getUnnormalizedProvider(ae, mu, TideSystem.UNKNOWN, cnm, snm);
    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    private UnnormalizedSphericalHarmonicsProvider provider;

}
