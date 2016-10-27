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
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandlerMultiplexer;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class KeplerianPropagatorTest {

    // Body mu
    private double mu;

    /**
     * Check that the date returned by {@link KeplerianPropagator#propagate(AbsoluteDate)}
     * is the same as the date passed to propagate().
     *
     * @throws OrekitException on error.
     */
    @Test
    public void testPropagationDate() throws OrekitException {
        // setup
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        // date s.t. target - date rounds down when represented as a double.
        AbsoluteDate target =
                initDate.shiftedBy(20.0).shiftedBy(FastMath.ulp(20.0) / 4);
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, mu);
        Propagator propagator = new KeplerianPropagator(ic);

        // action
        SpacecraftState actual = propagator.propagate(target);

        // verify
        Assert.assertEquals(target, actual.getDate());
    }

    @Test
    public void testEphemerisModeWithHandler() throws OrekitException {
        // setup
        AbsoluteDate initDate = AbsoluteDate.GPS_EPOCH;
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, mu);
        Propagator propagator = new KeplerianPropagator(ic);
        AbsoluteDate end = initDate.shiftedBy(90 * 60);

        // action
        final List<SpacecraftState> states = new ArrayList<>();
        propagator.setEphemerisMode((interpolator, isLast) -> {
            states.add(interpolator.getCurrentState());
            states.add(interpolator.getPreviousState());
        });
        propagator.propagate(end);
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        //verify
        Assert.assertTrue(states.size() > 1); // got some data
        for (SpacecraftState state : states) {
            PVCoordinates actual =
                    ephemeris.propagate(state.getDate()).getPVCoordinates();
            Assert.assertThat(actual, OrekitMatchers.pvIs(state.getPVCoordinates()));
        }
    }

    @Test
    public void sameDateCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = FastMath.sqrt(finalOrbit.getMu()/FastMath.pow(a, 3));

        Assert.assertEquals(n*delta_t,
                            finalOrbit.getLM() - initialOrbit.getLM(),
                            Utils.epsilonTest * FastMath.abs(n*delta_t));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM()));

        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI()));

    }

    @Test
    public void sameDateKeplerian() throws OrekitException {
        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = FastMath.sqrt(finalOrbit.getMu()/FastMath.pow(a, 3));

        Assert.assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * FastMath.max(100.,FastMath.abs(n*delta_t)));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM()));

        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(),initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI()));

    }

    @Test
    public void propagatedCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);


        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = FastMath.sqrt(finalOrbit.getMu()/FastMath.pow(a, 3));

        Assert.assertEquals(n * delta_t,
                            finalOrbit.getLM() - initialOrbit.getLM(),
                            Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*FastMath.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*FastMath.cos(finalOrbit.getLE());

        Assert.assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (FastMath.sin(finalOrbit.getLE()) - FastMath.sin(initialOrbit.getLE()))
        - finalOrbit.getEquinoctialEy() * (FastMath.cos(finalOrbit.getLE()) - FastMath.cos(initialOrbit.getLE()));

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex*ex;
        double ey2 = ey*ey;
        double hx2 = hx*hx;
        double hy2 = hy*hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + FastMath.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*FastMath.cos(LE) + beta*ex*ey*FastMath.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*FastMath.sin(LE) + beta*ex*ey*FastMath.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() throws OrekitException {

        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);
        Assert.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion(), 1.0e-16);

        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = FastMath.sqrt(finalOrbit.getMu()/FastMath.pow(a, 3));

        Assert.assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*FastMath.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*FastMath.cos(finalOrbit.getLE());

        Assert.assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (FastMath.sin(finalOrbit.getLE()) - FastMath.sin(initialOrbit.getLE())) - finalOrbit.getEquinoctialEy() * (FastMath.cos(finalOrbit.getLE()) - FastMath.cos(initialOrbit.getLE()));

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex*ex;
        double ey2 = ey*ey;
        double hx2 = hx*hx;
        double hy2 = hy*hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + FastMath.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*FastMath.cos(LE) + beta*ex*ey*FastMath.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*FastMath.sin(LE) + beta*ex*ey*FastMath.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

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
        KeplerianPropagator propagator = new KeplerianPropagator(orbit, wrongLaw);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test(expected = OrekitException.class)
    public void testStepException() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        OrekitStepHandlerMultiplexer multiplexer = new OrekitStepHandlerMultiplexer();
        propagator.setMasterMode(multiplexer);
        multiplexer.add(new OrekitStepHandler() {
            public void init(SpacecraftState s0, AbsoluteDate t) {
            }
            public void handleStep(OrekitStepInterpolator interpolator,
                                   boolean isLast) throws OrekitException {
                if (isLast) {
                    throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-3600));

    }

    @Test(expected = OrekitException.class)
    public void tesWrapedAttitudeException() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit,
                                                                 new AttitudeProvider() {
                                                                    private static final long serialVersionUID = 1L;
                                                                    public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date,
                                                                                                Frame frame)
                                                                        throws OrekitException {
                                                                        throw new OrekitException((Throwable) null,
                                                                                                  new DummyLocalizable("dummy error"));
                                                                    }
                                                                });
        propagator.propagate(orbit.getDate().shiftedBy(10.09));
    }

    @Test
    public void ascendingNode() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new NodeDetector(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ(), 2.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ() > 0);
    }

    @Test
    public void stopAtTargetDate() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new NodeDetector(orbit, itrf).withHandler(new ContinueOnEvent<NodeDetector>()));
        AbsoluteDate farTarget = orbit.getDate().shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate())), 1.0e-3);
    }

    @Test
    public void perigee() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new ApsideDetector(orbit));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assert.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 1.0e-6);
    }

    @Test
    public void altitude() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        BodyShape bodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        AltitudeDetector detector =
            new AltitudeDetector(0.05 * orbit.getKeplerianPeriod(),
                                 1500000, bodyShape);
        Assert.assertEquals(1500000, detector.getAltitude(), 1.0e-12);
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 5400.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 5500.0);
        GeodeticPoint gp = bodyShape.transform(propagated.getPVCoordinates().getPosition(),
                                               propagated.getFrame(), propagated.getDate());
        Assert.assertEquals(1500000, gp.getAltitude(), 0.1);
    }

    @Test
    public void date() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void setting() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        propagator.addEventDetector(new ElevationDetector(60, AbstractDetector.DEFAULT_THRESHOLD, topo).withConstantElevation(0.09));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition(),
                                                   propagated.getFrame(),
                                                   propagated.getDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-9);
        Assert.assertTrue(zVelocity < 0);
    }

    @Test
    public void fixedStep() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
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
    public void variableStep() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = orbit.getKeplerianPeriod() / 100;
        propagator.setMasterMode(new OrekitStepHandler() {
            private AbsoluteDate previous;
            public void handleStep(OrekitStepInterpolator interpolator,
                                   boolean isLast) throws OrekitException {
                if ((previous != null) && !isLast) {
                    Assert.assertEquals(step, interpolator.getCurrentState().getDate().durationFrom(previous), 1.0e-10);
                }
                previous = interpolator.getCurrentState().getDate();
            }
        });
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void ephemeris() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.setEphemerisMode();
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.setEphemerisMode();
        propagator.propagate(farTarget);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        Assert.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()), 1.0e10);
        Assert.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget), 1.0e10);
    }

    @Test
    public void testIssue14() throws OrekitException {
        AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit initialOrbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initialDate, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        PVCoordinates pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        PVCoordinates pv2 = propagator.getGeneratedEphemeris().getPVCoordinates(initialDate, FramesFactory.getEME2000());

        Assert.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-15);

    }

    @Test
    public void testIssue107() throws OrekitException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);

        Propagator propagator = new KeplerianPropagator(orbit) {
            private static final long serialVersionUID = 1L;
            AbsoluteDate lastDate = AbsoluteDate.PAST_INFINITY;

            protected SpacecraftState basicPropagate(final AbsoluteDate date) throws OrekitException {
                if (date.compareTo(lastDate) < 0) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                   "no backward propagation allowed");
                }
                lastDate = date;
                return super.basicPropagate(date);
            }
        };

        SpacecraftState finalState = propagator.propagate(date.shiftedBy(3600.0));
        Assert.assertEquals(3600.0, finalState.getDate().durationFrom(date), 1.0e-15);

    }

    @Test
    public void testMu() throws OrekitException {
        final KeplerianOrbit orbit1 =
                new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.WGS84_EARTH_MU);
        final KeplerianOrbit orbit2 =
                new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);
        final AbsoluteDate target = orbit1.getDate().shiftedBy(10000.0);
        PVCoordinates pv1       = new KeplerianPropagator(orbit1).propagate(target).getPVCoordinates();
        PVCoordinates pv2       = new KeplerianPropagator(orbit2).propagate(target).getPVCoordinates();
        PVCoordinates pvWithMu1 = new KeplerianPropagator(orbit2, orbit1.getMu()).propagate(target).getPVCoordinates();
        Assert.assertEquals(0.026054, Vector3D.distance(pv1.getPosition(), pv2.getPosition()),       1.0e-6);
        Assert.assertEquals(0.0,      Vector3D.distance(pv1.getPosition(), pvWithMu1.getPosition()), 1.0e-15);
    }

    @Test
    public void testIssue223()
        throws OrekitException, IOException, ClassNotFoundException {

        // Inertial frame
        Frame inertialFrame = FramesFactory.getEME2000();

        // Initial date
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,utc);

        // Central attraction coefficient
        double mu =  3.986004415e+14;

        // Initial orbit
        double a = 42100;                       // semi major axis in meters
        double e = 0.01;                        // eccentricity
        double i = FastMath.toRadians(6);       // inclination
        double omega = FastMath.toRadians(180); // perigee argument
        double raan = FastMath.toRadians(261);  // right ascention of ascending node
        double lM = 0;                          // mean anomaly
        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, initialDate, mu);

        // Initial state definition
        SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addAdditionalStateProvider(new SevenProvider());
        propagator.setEphemerisMode();
        propagator.propagate(initialState.getDate().shiftedBy(40000));

        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        Assert.assertSame(inertialFrame, ephemeris.getFrame());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        Assert.assertTrue(bos.size() > 2250);
        Assert.assertTrue(bos.size() < 2350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BoundedPropagator deserialized  = (BoundedPropagator) ois.readObject();
        Assert.assertEquals(initialOrbit.getA(), deserialized.getInitialState().getA(), 1.0e-10);
        Assert.assertEquals(initialOrbit.getEquinoctialEx(), deserialized.getInitialState().getEquinoctialEx(), 1.0e-10);
        SpacecraftState s = deserialized.propagate(initialState.getDate().shiftedBy(20000));
        Map<String, double[]> additional = s.getAdditionalStates();
        Assert.assertEquals(1, additional.size());
        Assert.assertEquals(1, additional.get("seven").length);
        Assert.assertEquals(7, additional.get("seven")[0], 1.0e-15);


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

    @Test
    public void testIssue224()
        throws OrekitException, IOException, ClassNotFoundException {

        // Inertial frame
        Frame inertialFrame = FramesFactory.getEME2000();

        // Initial date
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

        // Central attraction coefficient
        double mu =  3.986004415e+14;

        // Initial orbit
        double a = 42100;                       // semi major axis in meters
        double e = 0.01;                        // eccentricity
        double i = FastMath.toRadians(6);       // inclination
        double omega = FastMath.toRadians(180); // perigee argument
        double raan = FastMath.toRadians(261);  // right ascention of ascending node
        double lM = 0;                          // mean anomaly
        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, initialDate, mu);

        // Initial state definition
        SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit,
                                                                 new LofOffset(inertialFrame,
                                                                               LOFType.VVLH));
        propagator.addAdditionalStateProvider(new SevenProvider());
        propagator.setEphemerisMode();

        // Impulsive burn 1
        final AbsoluteDate burn1Date = initialState.getDate().shiftedBy(200);
        ImpulseManeuver<DateDetector> impulsiveBurn1 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn1Date), new Vector3D(1000, 0, 0), 320);
        propagator.addEventDetector(impulsiveBurn1);

        // Impulsive burn 2
        final AbsoluteDate burn2Date = initialState.getDate().shiftedBy(300);
        ImpulseManeuver<DateDetector> impulsiveBurn2 =
                new ImpulseManeuver<DateDetector>(new DateDetector(burn2Date), new Vector3D(1000, 0, 0), 320);
        propagator.addEventDetector(impulsiveBurn2);

        propagator.propagate(initialState.getDate().shiftedBy(400));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(propagator.getGeneratedEphemeris());

        Assert.assertTrue(bos.size() > 2300);
        Assert.assertTrue(bos.size() < 2400);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BoundedPropagator ephemeris  = (BoundedPropagator) ois.readObject();

        ephemeris.setMasterMode(10, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState, boolean isLast) {
                if (currentState.getDate().durationFrom(burn1Date) < -0.001) {
                    Assert.assertEquals(42100.0, currentState.getA(), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn1Date) > 0.001 &&
                        currentState.getDate().durationFrom(burn2Date) < -0.001) {
                    Assert.assertEquals(42979.962, currentState.getA(), 1.0e-3);
                } else if (currentState.getDate().durationFrom(burn2Date) > 0.001) {
                    Assert.assertEquals(43887.339, currentState.getA(), 1.0e-3);
                }
            }
        });
        ephemeris.propagate(ephemeris.getMaxDate());

    }

    @Test
    public void testNonSerializableStateProvider() throws OrekitException, IOException {
        KeplerianPropagator propagator =
                        new KeplerianPropagator(new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                                                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                                   Constants.WGS84_EARTH_MU));

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

    private static double tangLEmLv(double Lv,double ex,double ey){
        // tan ((LE - Lv) /2)) =
        return (ey*FastMath.cos(Lv) - ex*FastMath.sin(Lv)) /
        (1 + ex*FastMath.cos(Lv) + ey*FastMath.sin(Lv) + FastMath.sqrt(1 - ex*ex - ey*ey));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
    }

    @After
    public void tearDown() {
        mu   = Double.NaN;
    }

}

