/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
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
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;


public class EcksteinHechlerPropagatorTest {

    @Test
    public void sameDateCartesian() throws OrekitException {

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
        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()),
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
        Assert.assertEquals(0.137,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-3);
        Assert.assertEquals(125.2, finalOrbit.getA() - initialOrbit.getA(), 0.1);

    }

    @Test
    public void sameDateKeplerian() throws OrekitException {

        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit, Propagator.DEFAULT_MASS, provider);

        // Extrapolation at the initial date
        // ---------------------------------
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assert.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()),
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
        Assert.assertEquals(0.137,
                            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()),
                            1.0e-3);
        Assert.assertEquals(126.8, finalOrbit.getA() - initialOrbit.getA(), 0.1);

    }

    @Test
    public void almostSphericalBody() throws OrekitException {

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Initialisation to simulate a keplerian extrapolation
        // To be noticed: in order to simulate a keplerian extrapolation with the
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

        Assert.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assert.assertEquals(finalOrbitAna.getA(), finalOrbitKep.getA(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEx(), finalOrbitKep.getEquinoctialEx(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEy(), finalOrbitKep.getEquinoctialEy(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx(), finalOrbitKep.getHx()),
                     finalOrbitKep.getHx(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy(), finalOrbitKep.getHy()),
                     finalOrbitKep.getHy(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv(), finalOrbitKep.getLv()),
                     finalOrbitKep.getLv(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE(), finalOrbitKep.getLE()),
                     finalOrbitKep.getLE(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM(), finalOrbitKep.getLM()),
                     finalOrbitKep.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM()));

    }

    @Test
    public void propagatedCartesian() throws OrekitException {
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

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
        * FastMath.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
        * FastMath.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbit.getLM()));

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta = finalOrbit.getEquinoctialEx() * FastMath.sin(finalOrbit.getLE())
        - initialOrbit.getEquinoctialEx() * FastMath.sin(initialOrbit.getLE())
        - finalOrbit.getEquinoctialEy() * FastMath.cos(finalOrbit.getLE())
        + initialOrbit.getEquinoctialEy() * FastMath.cos(initialOrbit.getLE());

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
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

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
                     Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() throws OrekitException {
        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                              6.2, PositionAngle.TRUE,
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

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
        * FastMath.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
        * FastMath.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
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

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
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

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
                     Utils.epsilonTest * r.getNorm());

    }

    @Test(expected = OrekitException.class)
    public void undergroundOrbit() throws OrekitException {

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
    }

    @Test(expected = OrekitException.class)
    public void equatorialOrbit() throws OrekitException {
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new CircularOrbit(7000000, 1.0e-4, -1.5e-4,
                                               0.0, 1.2, 2.3, PositionAngle.MEAN,
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
    }

    @Test(expected = OrekitException.class)
    public void criticalInclination() throws OrekitException {
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
    }

    @Test(expected = OrekitException.class)
    public void tooEllipticalOrbit() throws OrekitException {
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
    }

    @Test(expected = OrekitException.class)
    public void hyperbolic() throws OrekitException {
        KeplerianOrbit hyperbolic =
            new KeplerianOrbit(-1.0e10, 2, 0, 0, 0, 0, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(hyperbolic, provider);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test(expected = OrekitException.class)
    public void wrongAttitude() throws OrekitException {
        KeplerianOrbit orbit =
            new KeplerianOrbit(1.0e10, 1.0e-4, 1.0e-2, 0, 0, 0, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        AttitudeProvider wrongLaw = new AttitudeProvider() {
            private static final long serialVersionUID = 5918362126173997016L;
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) throws OrekitException {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
        };
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, wrongLaw, provider);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test
    public void testAcceleration() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        AbsoluteDate target = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt : Arrays.asList(-0.5, 0.0, 0.5)) {
            sample.add(propagator.propagate(target.shiftedBy(dt)).getPVCoordinates());
        }
        TimeStampedPVCoordinates interpolated =
                TimeStampedPVCoordinates.interpolate(target, CartesianDerivativesFilter.USE_P, sample);
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
                                  propagated.getAlphaM(), PositionAngle.MEAN,
                                  propagated.getFrame(),
                                  propagated.getDate(),
                                  propagated.getMu());
        Vector3D keplerianP    = keplerian.getPVCoordinates().getPosition();
        Vector3D keplerianV    = keplerian.getPVCoordinates().getVelocity();
        Vector3D keplerianA    = keplerian.getPVCoordinates().getAcceleration();

        // perturbed orbit position should be similar to Keplerian orbit position
        Assert.assertEquals(0.0, Vector3D.distance(referenceP, computedP), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(referenceP, keplerianP), 4.0e-9);

        // perturbed orbit velocity should be equal to Keplerian orbit because
        // it was in fact reconstructed from Cartesian coordinates
        double computationErrorV   = Vector3D.distance(referenceV, computedV);
        double nonKeplerianEffectV = Vector3D.distance(referenceV, keplerianV);
        Assert.assertEquals(nonKeplerianEffectV, computationErrorV, 9.0e-13);
        Assert.assertEquals(2.2e-4, computationErrorV, 3.0e-6);

        // perturbed orbit acceleration should be different from Keplerian orbit because
        // Keplerian orbit doesn't take orbit shape changes into account
        // perturbed orbit acceleration should be consistent with position evolution
        double computationErrorA   = Vector3D.distance(referenceA, computedA);
        double nonKeplerianEffectA = Vector3D.distance(referenceA, keplerianA);
        Assert.assertEquals(1.0e-7,  computationErrorA, 6.0e-9);
        Assert.assertEquals(6.37e-3, nonKeplerianEffectA, 7.0e-6);
        Assert.assertTrue(computationErrorA < nonKeplerianEffectA / 60000);

    }

    @Test
    public void ascendingNode() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        NodeDetector detector = new NodeDetector(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(FramesFactory.getITRF(IERSConventions.IERS_2010, true) == detector.getFrame());
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ(), 1.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ() > 0);
        Collection<EventDetector> detectors = propagator.getEventsDetectors();
        Assert.assertEquals(1, detectors.size());
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());
    }

    @Test
    public void stopAtTargetDate() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        Frame itrf =  FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new NodeDetector(orbit, itrf).withHandler(new ContinueOnEvent<NodeDetector>()));
        AbsoluteDate farTarget = orbit.getDate().shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate())), 1.0e-3);
    }

    @Test
    public void perigee() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, provider.getMu());
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        propagator.addEventDetector(new ApsideDetector(orbit));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assert.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 410);
    }

    @Test
    public void date() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void fixedStep() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final double step = 100.0;
        propagator.setMasterMode(step, new OrekitFixedStepHandler() {
            private AbsoluteDate previous;
            public void handleStep(SpacecraftState currentState, boolean isLast)
            throws OrekitException {
                if (previous != null) {
                    Assert.assertEquals(step, currentState.getDate().durationFrom(previous), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void setting() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, provider);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        ElevationDetector detector = new ElevationDetector(60, 1.0e-9, topo).withConstantElevation(0.09);
        Assert.assertEquals(0.09, detector.getMinElevation(), 1.0e-12);
        Assert.assertTrue(topo == detector.getTopocentricFrame());
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition(),
                                                   propagated.getFrame(),
                                                   propagated.getDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assert.assertTrue("Incorrect value " + farTarget.durationFrom(propagated.getDate()) + " !< 7900",farTarget.durationFrom(propagated.getDate()) < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-11);
        Assert.assertTrue(zVelocity < 0);
    }

    @Test
    public void testInitializationCorrectness()
        throws OrekitException, IOException {

        //  Definition of initial conditions
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                       FastMath.toRadians(97.40236024565775),
                                                       FastMath.toRadians(166.15873160992115),
                                                       FastMath.toRadians(90.1282370098961), PositionAngle.MEAN,
                                                       poleAligned, date, provider.getMu());

        // find the default Eckstein-Hechler propagator initialized from the initial orbit
        EcksteinHechlerPropagator defaultEH = new EcksteinHechlerPropagator(initial, provider);

        // the osculating parameters recomputed by the default Eckstein-Hechler propagator are quite different
        // from initial orbit
        CircularOrbit defaultOrbit = (CircularOrbit) OrbitType.CIRCULAR.convertType(defaultEH.propagateOrbit(initial.getDate()));
        Assert.assertEquals(267.4, defaultOrbit.getA() - initial.getA(), 0.1);

        // the position on the other hand match perfectly
        Assert.assertEquals(0.0,
                            Vector3D.distance(defaultOrbit.getPVCoordinates().getPosition(),
                                              initial.getPVCoordinates().getPosition()),
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
                                                                                             PositionAngle.TRUE,
                                                                                             1.0),
                                                        1.0e-6, 100);
        EcksteinHechlerPropagator fittedEH =
                (EcksteinHechlerPropagator) converter.convert(num, 3 * initial.getKeplerianPeriod(), 300);

        // the default Eckstein-Hechler propagator did however quite a good job, as it found
        // an orbit close to the best fitting
        CircularOrbit fittedOrbit  = (CircularOrbit) OrbitType.CIRCULAR.convertType(fittedEH.propagateOrbit(initial.getDate()));
        Assert.assertEquals(0.623, defaultOrbit.getA() - fittedOrbit.getA(), 0.1);

        // the position on the other hand are slightly different
        // because the fitted orbit minimizes the residuals over a complete time span,
        // not on a single point
        Assert.assertEquals(58.0,
                            Vector3D.distance(defaultOrbit.getPVCoordinates().getPosition(),
                                              fittedOrbit.getPVCoordinates().getPosition()),
                            0.1);

    }

    @Test
    public void testNonSerializableStateProvider() throws OrekitException, IOException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                       FastMath.toRadians(97.40236024565775),
                                                       FastMath.toRadians(166.15873160992115),
                                                       FastMath.toRadians(90.1282370098961), PositionAngle.MEAN,
                                                       poleAligned, date, provider.getMu());

        EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initial, provider);

        // this serialization should work
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(propagator);

        propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() {
                return "not serializable";
            }
            public double[] getAdditionalState(SpacecraftState state) {
                return new double[] { 0 };
            }
        });

        try {
            // this serialization should not work
            new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(propagator);
            Assert.fail("an exception should have been thrown");
        } catch (NotSerializableException nse) {
            // expected
        }

    }

    @Test
    public void testIssue223()
        throws OrekitException, IOException, ClassNotFoundException {

        //  Definition of initial conditions
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                       FastMath.toRadians(97.40236024565775),
                                                       FastMath.toRadians(166.15873160992115),
                                                       FastMath.toRadians(90.1282370098961), PositionAngle.MEAN,
                                                       poleAligned, date, provider.getMu());

        EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initial, provider);

        propagator.addAdditionalStateProvider(new SevenProvider());
        propagator.setEphemerisMode();
        propagator.propagate(initial.getDate().shiftedBy(40000));

        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        Assert.assertSame(poleAligned, ephemeris.getFrame());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        Assert.assertTrue(bos.size() > 2450);
        Assert.assertTrue(bos.size() < 2550);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BoundedPropagator deserialized  = (BoundedPropagator) ois.readObject();
        Assert.assertEquals(initial.getA(), deserialized.getInitialState().getA(), 1.0e-10);
        Assert.assertEquals(initial.getEquinoctialEx(), deserialized.getInitialState().getEquinoctialEx(), 1.0e-10);
        SpacecraftState s = deserialized.propagate(initial.getDate().shiftedBy(20000));
        Map<String, double[]> additional = s.getAdditionalStates();
        Assert.assertEquals(1, additional.size());
        Assert.assertEquals(1, additional.get("seven").length);
        Assert.assertEquals(7, additional.get("seven")[0], 1.0e-15);


    }

    @Test
    public void testIssue224Forward()
        throws OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                  FastMath.toRadians(97.40236024565775),
                                                  FastMath.toRadians(166.15873160992115),
                                                  FastMath.toRadians(90.1282370098961), PositionAngle.MEAN,
                                                  poleAligned, date, provider.getMu());

        EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initial,
                                                                             new LofOffset(poleAligned,
                                                                                           LOFType.VVLH),
                                                                             1000.0,
                                                                             provider);
        propagator.addAdditionalStateProvider(new SevenProvider());
        propagator.setEphemerisMode();

        // Impulsive burns
        final AbsoluteDate burn1Date = initial.getDate().shiftedBy(200);
        ImpulseManeuver<DateDetector> impulsiveBurn1 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn1Date), new Vector3D(0.0, 500.0, 0.0), 320);
        propagator.addEventDetector(impulsiveBurn1);
        final AbsoluteDate burn2Date = initial.getDate().shiftedBy(300);
        ImpulseManeuver<DateDetector> impulsiveBurn2 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn2Date), new Vector3D(0.0, 500.0, 0.0), 320);
        propagator.addEventDetector(impulsiveBurn2);

        propagator.propagate(initial.getDate().shiftedBy(400));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(propagator.getGeneratedEphemeris());

        Assert.assertTrue(bos.size() > 2950);
        Assert.assertTrue(bos.size() < 3050);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BoundedPropagator ephemeris  = (BoundedPropagator) ois.readObject();

        ephemeris.setMasterMode(10, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState, boolean isLast) {
                if (currentState.getDate().durationFrom(burn1Date) < -0.001) {
                    Assert.assertEquals(97.402, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn1Date) >  0.001 &&
                           currentState.getDate().durationFrom(burn2Date) < -0.001) {
                    Assert.assertEquals(98.183, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn2Date) > 0.001) {
                    Assert.assertEquals(99.310, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                }
            }
        });
        ephemeris.propagate(ephemeris.getMaxDate());

    }

    @Test
    public void testIssue224Backward()
        throws OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(154.);
        Frame itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000     = FramesFactory.getEME2000();
        Vector3D pole     = itrf.getTransformTo(eme2000, date).transformVector(Vector3D.PLUS_K);
        Frame poleAligned = new Frame(FramesFactory.getEME2000(),
                                      new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                      "pole aligned", true);
        CircularOrbit initial = new CircularOrbit(7208669.8179538045, 1.3740461966386876E-4, -3.2364250248363356E-5,
                                                  FastMath.toRadians(97.40236024565775),
                                                  FastMath.toRadians(166.15873160992115),
                                                  FastMath.toRadians(90.1282370098961), PositionAngle.MEAN,
                                                  poleAligned, date, provider.getMu());

        EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initial,
                                                                             new LofOffset(poleAligned,
                                                                                           LOFType.VVLH),
                                                                             1000.0,
                                                                             provider);
        propagator.addAdditionalStateProvider(new SevenProvider());
        propagator.setEphemerisMode();

        // Impulsive burns
        final AbsoluteDate burn1Date = initial.getDate().shiftedBy(-200);
        ImpulseManeuver<DateDetector> impulsiveBurn1 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn1Date), new Vector3D(0.0, 500.0, 0.0), 320);
        propagator.addEventDetector(impulsiveBurn1);
        final AbsoluteDate burn2Date = initial.getDate().shiftedBy(-300);
        ImpulseManeuver<DateDetector> impulsiveBurn2 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn2Date), new Vector3D(0.0, 500.0, 0.0), 320);
        propagator.addEventDetector(impulsiveBurn2);

        propagator.propagate(initial.getDate().shiftedBy(-400));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(propagator.getGeneratedEphemeris());

        Assert.assertTrue(bos.size() > 2950);
        Assert.assertTrue(bos.size() < 3050);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BoundedPropagator ephemeris  = (BoundedPropagator) ois.readObject();

        ephemeris.setMasterMode(10, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState, boolean isLast) {
                if (currentState.getDate().durationFrom(burn1Date) > 0.001) {
                    Assert.assertEquals(97.402, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn1Date) < -0.001 &&
                           currentState.getDate().durationFrom(burn2Date) >  0.001) {
                    Assert.assertEquals(98.164, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn2Date) < -0.001) {
                    Assert.assertEquals(99.273, FastMath.toDegrees(currentState.getI()), 1.0e-3);
                }
            }
        });
        ephemeris.propagate(ephemeris.getMinDate());

    }

    private static class SevenProvider implements AdditionalStateProvider, Serializable {
        private static final long serialVersionUID = 1L;
        public String getName() {
            return "seven";
        }
        public double[] getAdditionalState(final SpacecraftState state) {
            return new double[] { 7 };
        }
    }

    private static double tangLEmLv(double Lv, double ex, double ey) {
        // tan ((LE - Lv) /2)) =
        return (ey * FastMath.cos(Lv) - ex * FastMath.sin(Lv))
        / (1 + ex * FastMath.cos(Lv) + ey * FastMath.sin(Lv) + FastMath.sqrt(1 - ex * ex
                                                                 - ey * ey));
    }

    @Before
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

    @After
    public void tearDown() {
        provider = null;
    }

    private UnnormalizedSphericalHarmonicsProvider provider;

}
