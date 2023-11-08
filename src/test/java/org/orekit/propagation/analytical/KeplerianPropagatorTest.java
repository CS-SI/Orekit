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

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
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
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
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
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;


public class KeplerianPropagatorTest {

    // Body mu
    private double mu;

    /**
     * Check that the date returned by {@link KeplerianPropagator#propagate(AbsoluteDate)}
     * is the same as the date passed to propagate().
     */
    @Test
    public void testPropagationDate() {
        // setup
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        // date s.t. target - date rounds down when represented as a double.
        AbsoluteDate target =
                initDate.shiftedBy(20.0).shiftedBy(FastMath.ulp(20.0) / 4);
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                PositionAngleType.TRUE, FramesFactory.getGCRF(), initDate, mu);
        Propagator propagator = new KeplerianPropagator(ic);

        // action
        SpacecraftState actual = propagator.propagate(target);

        // verify
        Assertions.assertEquals(target, actual.getDate());
    }

    @Test
    public void testEphemerisModeWithHandler() {
        // setup
        AbsoluteDate initDate = AbsoluteDate.GPS_EPOCH;
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                PositionAngleType.TRUE, FramesFactory.getGCRF(), initDate, mu);
        Propagator propagator = new KeplerianPropagator(ic);
        AbsoluteDate end = initDate.shiftedBy(90 * 60);

        // action
        final List<SpacecraftState> states = new ArrayList<>();
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.setStepHandler(interpolator -> {
            states.add(interpolator.getCurrentState());
            states.add(interpolator.getPreviousState());
        });
        propagator.propagate(end);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        //verify
        Assertions.assertTrue(states.size() > 1); // got some data
        for (SpacecraftState state : states) {
            PVCoordinates actual =
                    ephemeris.propagate(state.getDate()).getPVCoordinates();
            MatcherAssert.assertThat(actual, OrekitMatchers.pvIs(state.getPVCoordinates()));
        }
    }

    @Test
    public void testAdditionalState() {
        AbsoluteDate initDate = AbsoluteDate.GPS_EPOCH;
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0, PositionAngleType.TRUE, FramesFactory.getGCRF(), initDate, mu);
        Propagator propagator = new KeplerianPropagator(ic);
        SpacecraftState initialState = propagator.getInitialState().addAdditionalState("myState", 4.2);
        propagator.resetInitialState(initialState);
        AbsoluteDate end = initDate.shiftedBy(90 * 60);
        EphemerisGenerator generator = propagator.getEphemerisGenerator();
        SpacecraftState finalStateKeplerianPropagator = propagator.propagate(end);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        SpacecraftState ephemerisInitialState = ephemeris.getInitialState();
        SpacecraftState finalStateBoundedPropagator = ephemeris.propagate(end);
        Assertions.assertEquals(4.2, finalStateKeplerianPropagator.getAdditionalState("myState")[0], 1.0e-15);
        Assertions.assertEquals(4.2, ephemerisInitialState.getAdditionalState("myState")[0], 1.0e-15);
        Assertions.assertEquals(4.2, finalStateBoundedPropagator.getAdditionalState("myState")[0], 1.0e-15);
    }

    @Test
    public void sameDateCartesian() {

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

        Assertions.assertEquals(n*delta_t,
                            finalOrbit.getLM() - initialOrbit.getLM(),
                            Utils.epsilonTest * FastMath.abs(n*delta_t));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(), initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM()));

        Assertions.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assertions.assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI()));

    }

    @Test
    public void sameDateKeplerian() {
        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngleType.TRUE,
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

        Assertions.assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * FastMath.max(100., FastMath.abs(n*delta_t)));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(), initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM()));

        Assertions.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assertions.assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI()));

    }

    @Test
    public void propagatedCartesian() {

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

        Assertions.assertEquals(n * delta_t,
                            finalOrbit.getLM() - initialOrbit.getLM(),
                            Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*FastMath.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*FastMath.cos(finalOrbit.getLE());

        Assertions.assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (FastMath.sin(finalOrbit.getLE()) - FastMath.sin(initialOrbit.getLE()))
        - finalOrbit.getEquinoctialEy() * (FastMath.cos(finalOrbit.getLE()) - FastMath.cos(initialOrbit.getLE()));

        Assertions.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assertions.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assertions.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assertions.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

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

        Vector3D r = new Vector3D(finalOrbit.getA(), new Vector3D(x3, U, y3, V));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() {

        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngleType.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);
        Assertions.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod(), 1.0e-12);
        Assertions.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion(), 1.0e-16);

        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = FastMath.sqrt(finalOrbit.getMu()/FastMath.pow(a, 3));

        Assertions.assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*FastMath.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*FastMath.cos(finalOrbit.getLE());

        Assertions.assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (FastMath.sin(finalOrbit.getLE()) - FastMath.sin(initialOrbit.getLE())) - finalOrbit.getEquinoctialEy() * (FastMath.cos(finalOrbit.getLE()) - FastMath.cos(initialOrbit.getLE()));

        Assertions.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assertions.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assertions.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assertions.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

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

        Vector3D r = new Vector3D(finalOrbit.getA(), new Vector3D(x3, U, y3, V));

        Assertions.assertEquals(finalOrbit.getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void wrongAttitude() {
        Assertions.assertThrows(OrekitException.class, () -> {
            KeplerianOrbit orbit =
                    new KeplerianOrbit(1.0e10, 1.0e-4, 1.0e-2, 0, 0, 0, PositionAngleType.TRUE,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
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
            KeplerianPropagator propagator = new KeplerianPropagator(orbit, wrongLaw);
            propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
        });
    }

    @Test
    public void testStepException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final KeplerianOrbit orbit =
                    new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
            KeplerianPropagator propagator = new KeplerianPropagator(orbit);
            propagator.getMultiplexer().add(new OrekitStepHandler() {
                public void handleStep(OrekitStepInterpolator interpolator) {
                }
                public void finish(SpacecraftState finalState) {
                    throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            });

            propagator.propagate(orbit.getDate().shiftedBy(-3600));
        });
    }

    @Test
    public void tesWrapedAttitudeException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final KeplerianOrbit orbit =
                    new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
            KeplerianPropagator propagator = new KeplerianPropagator(orbit,
                    new AttitudeProvider() {
                        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date,
                                Frame frame)
                        {
                            throw new OrekitException((Throwable) null,
                                    new DummyLocalizable("dummy error"));
                        }
                        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                                FieldAbsoluteDate<T> date, Frame frame)
                        {
                            throw new OrekitException((Throwable) null,
                                    new DummyLocalizable("dummy error"));
                        }
                    });
            propagator.propagate(orbit.getDate().shiftedBy(10.09));
        });
    }

    @Test
    public void ascendingNode() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new NodeDetector(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assertions.assertEquals(0, pv.getPosition().getZ(), 2.0e-6);
        Assertions.assertTrue(pv.getVelocity().getZ() > 0);
    }

    @Test
    public void stopAtTargetDate() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new NodeDetector(orbit, itrf).withHandler(new ContinueOnEvent()));
        AbsoluteDate farTarget = orbit.getDate().shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate())), 1.0e-3);
    }

    @Test
    public void perigee() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new ApsideDetector(orbit));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Vector3D pos = propagated.getPosition(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assertions.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pos.getNorm(), 1.0e-6);
    }

    @Test
    public void altitude() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        BodyShape bodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        AltitudeDetector detector =
            new AltitudeDetector(0.05 * orbit.getKeplerianPeriod(),
                                 1500000, bodyShape);
        Assertions.assertEquals(1500000, detector.getAltitude(), 1.0e-12);
        propagator.addEventDetector(detector);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 5400.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 5500.0);
        GeodeticPoint gp = bodyShape.transform(propagated.getPosition(),
                                               propagated.getFrame(), propagated.getDate());
        Assertions.assertEquals(1500000, gp.getAltitude(), 0.1);
    }

    @Test
    public void date() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void setting() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        propagator.addEventDetector(new ElevationDetector(60, AbstractDetector.DEFAULT_THRESHOLD, topo).withConstantElevation(0.09));
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.
                                 getTrackingCoordinates(propagated.getPosition(),
                                                        propagated.getFrame(),
                                                        propagated.getDate()).
                                 getElevation();
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0);
        Assertions.assertEquals(0.09, elevation, 1.0e-9);
        Assertions.assertTrue(zVelocity < 0);
    }

    @Test
    public void fixedStep() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = 100.0;
        final int[] counter = new int[] {0};  // mutable int
        propagator.setStepHandler(step, new OrekitFixedStepHandler() {
            private AbsoluteDate previous;
            public void handleStep(SpacecraftState currentState) {
                if (previous != null) {
                    Assertions.assertEquals(step, currentState.getDate().durationFrom(previous), 1.0e-10);
                }
                // check state is accurate
                PVCoordinates expected = new KeplerianPropagator(orbit)
                        .propagate(currentState.getDate()).getPVCoordinates();
                MatcherAssert.assertThat(
                        currentState.getPVCoordinates(),
                        OrekitMatchers.pvIs(expected));
                previous = currentState.getDate();
                counter[0]++;
            }
        });
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
        // check the step handler was executed
        Assertions.assertEquals(
                counter[0],
                (int) (farTarget.durationFrom(orbit.getDate()) / step) + 1);
    }

    @Test
    public void variableStep() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = orbit.getKeplerianPeriod() / 100;
        final int[] counter = new int[] {0};  // mutable int
        propagator.setStepHandler(new OrekitStepHandler() {
            private AbsoluteDate t = orbit.getDate();
            @Override
            public void handleStep(OrekitStepInterpolator interpolator) {
                // check the states provided by the interpolator are accurate.
                do {
                    PVCoordinates expected = new KeplerianPropagator(orbit)
                            .propagate(t).getPVCoordinates();
                    MatcherAssert.assertThat(
                            interpolator.getInterpolatedState(t).getPVCoordinates(),
                            OrekitMatchers.pvIs(expected));
                    t = t.shiftedBy(step);
                    counter[0]++;
                } while (t.compareTo(interpolator.getCurrentState().getDate()) <= 0);
            }
        });
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
        // check the step handler was executed
        Assertions.assertEquals(
                counter[0],
                (int) (farTarget.durationFrom(orbit.getDate()) / step) + 1);
    }

    @Test
    public void ephemeris() {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(farTarget);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Assertions.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()), 1.0e10);
        Assertions.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget), 1.0e10);
    }

    @Test
    public void testIssue14() {
        AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit initialOrbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), initialDate, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        propagator.getEphemerisGenerator();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        PVCoordinates pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        PVCoordinates pv2 = generator.getGeneratedEphemeris().getPVCoordinates(initialDate, FramesFactory.getEME2000());

        Assertions.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-15);
        Assertions.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-15);

    }

    @Test
    public void testIssue107() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);

        Propagator propagator = new KeplerianPropagator(orbit) {
            AbsoluteDate lastDate = AbsoluteDate.PAST_INFINITY;

            protected SpacecraftState basicPropagate(final AbsoluteDate date) {
                if (date.compareTo(lastDate) < 0) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                   "no backward propagation allowed");
                }
                lastDate = date;
                return super.basicPropagate(date);
            }
        };

        SpacecraftState finalState = propagator.propagate(date.shiftedBy(3600.0));
        Assertions.assertEquals(3600.0, finalState.getDate().durationFrom(date), 1.0e-15);

    }

    @Test
    public void testMu() {
        final KeplerianOrbit orbit1 =
                new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.WGS84_EARTH_MU);
        final KeplerianOrbit orbit2 =
                new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                   Constants.EIGEN5C_EARTH_MU);
        final AbsoluteDate target = orbit1.getDate().shiftedBy(10000.0);
        PVCoordinates pv1       = new KeplerianPropagator(orbit1).propagate(target).getPVCoordinates();
        PVCoordinates pv2       = new KeplerianPropagator(orbit2).propagate(target).getPVCoordinates();
        PVCoordinates pvWithMu1 = new KeplerianPropagator(orbit2, orbit1.getMu()).propagate(target).getPVCoordinates();
        Assertions.assertEquals(0.026054, Vector3D.distance(pv1.getPosition(), pv2.getPosition()),       1.0e-6);
        Assertions.assertEquals(0.0,      Vector3D.distance(pv1.getPosition(), pvWithMu1.getPosition()), 1.0e-15);
    }

    @Test
    public void testResetStateForward() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, new LofOffset(eme2000, LOFType.LVLH));

        // maneuver along Z in attitude aligned with LVLH will change orbital plane
        final AbsoluteDate maneuverDate = date.shiftedBy(1000.0);
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(maneuverDate),
                                                        new Vector3D(0.0, 0.0, -100.0),
                                                        350.0));

        final Vector3D initialNormal = orbit.getPVCoordinates().getMomentum();
        propagator.setStepHandler(60.0, state -> {
            final Vector3D currentNormal = state.getPVCoordinates().getMomentum();
            if (state.getDate().isBefore(maneuverDate)) {
                Assertions.assertEquals(0.000, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            } else {
                Assertions.assertEquals(0.014, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(1500.0));

    }

    @Test
    public void testResetStateBackward() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, new LofOffset(eme2000, LOFType.LVLH));

        // maneuver along Z in attitude aligned with LVLH will change orbital plane
        final AbsoluteDate maneuverDate = date.shiftedBy(-1000.0);
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(maneuverDate),
                                                        new Vector3D(0.0, 0.0, -100.0),
                                                        350.0));

        final Vector3D initialNormal = orbit.getPVCoordinates().getMomentum();
        propagator.setStepHandler(60.0, state -> {
            final Vector3D currentNormal = state.getPVCoordinates().getMomentum();
            if (state.getDate().isAfter(maneuverDate)) {
                Assertions.assertEquals(0.000, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            } else {
                Assertions.assertEquals(0.014, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-1500.0));

    }

    @Test
    public void testNoDerivatives() {
        for (OrbitType type : OrbitType.values()) {

            // create an initial orbit with non-Keplerian acceleration
            final AbsoluteDate date         = new AbsoluteDate(2003, 9, 16, TimeScalesFactory.getUTC());
            final Vector3D     position     = new Vector3D(-6142438.668, 3492467.56, -25767.257);
            final Vector3D     velocity     = new Vector3D(505.848, 942.781, 7435.922);
            final Vector3D     keplerAcceleration = new Vector3D(-mu / position.getNormSq(), position.normalize());
            final Vector3D     nonKeplerAcceleration = new Vector3D(0.001, 0.002, 0.003);
            final Vector3D     acceleration = keplerAcceleration.add(nonKeplerAcceleration);
            final TimeStampedPVCoordinates pva = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
            final Orbit initial = type.convertType(new CartesianOrbit(pva, FramesFactory.getEME2000(), mu));
            Assertions.assertEquals(type, initial.getType());

            // the derivatives are available at this stage
            checkDerivatives(initial, true);

            KeplerianPropagator propagator = new KeplerianPropagator(initial);
            Assertions.assertEquals(type, propagator.getInitialState().getOrbit().getType());

            // non-Keplerian derivatives are explicitly removed when building the Keplerian-only propagator
            checkDerivatives(propagator.getInitialState().getOrbit(), false);

            PVCoordinates initPV = propagator.getInitialState().getOrbit().getPVCoordinates();
            Assertions.assertEquals(nonKeplerAcceleration.getNorm(), Vector3D.distance(acceleration, initPV.getAcceleration()), 2.0e-15);
            Assertions.assertEquals(0.0,
                                Vector3D.distance(keplerAcceleration, initPV.getAcceleration()),
                                4.0e-15);

            double dt = 0.2 * initial.getKeplerianPeriod();
            Orbit orbit = propagator.propagateOrbit(initial.getDate().shiftedBy(dt));
            Assertions.assertEquals(type, orbit.getType());

            // at the end, we don't have non-Keplerian derivatives
            checkDerivatives(orbit, false);

            // using shiftedBy on the initial orbit, non-Keplerian derivatives would have been preserved
            checkDerivatives(initial.shiftedBy(dt), true);

        }
    }

    @Test
    public void testStackableGenerators() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, new LofOffset(eme2000, LOFType.LVLH));

        // we have A → B → C → D → E → F but register them in a different order
        propagator.addAdditionalStateProvider(new DependentGenerator("F", "E"));
        propagator.addAdditionalStateProvider(new DependentGenerator("B", "A"));
        propagator.addAdditionalStateProvider(new DependentGenerator("E", "D"));
        propagator.addAdditionalStateProvider(new DependentGenerator("C", "B"));
        propagator.addAdditionalStateProvider(new DependentGenerator("A", null));
        propagator.addAdditionalStateProvider(new DependentGenerator("D", "C"));

        final SpacecraftState finalState = propagator.propagate(orbit.getDate().shiftedBy(3600.0));
        Assertions.assertEquals(1,   finalState.getAdditionalState("A").length);
        Assertions.assertEquals(1.0, finalState.getAdditionalState("A")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("B").length);
        Assertions.assertEquals(2.0, finalState.getAdditionalState("B")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("C").length);
        Assertions.assertEquals(3.0, finalState.getAdditionalState("C")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("D").length);
        Assertions.assertEquals(4.0, finalState.getAdditionalState("D")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("E").length);
        Assertions.assertEquals(5.0, finalState.getAdditionalState("E")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("F").length);
        Assertions.assertEquals(6.0, finalState.getAdditionalState("F")[0], 1.0e-15);

    }

    @Test
    public void testCircularDependency() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, new LofOffset(eme2000, LOFType.LVLH));

        // here, the dependency creates a loop, which is detected and its adders ignored
        propagator.addAdditionalStateProvider(new DependentGenerator("F", "E"));
        propagator.addAdditionalStateProvider(new DependentGenerator("B", "A"));
        propagator.addAdditionalStateProvider(new DependentGenerator("E", "D"));
        propagator.addAdditionalStateProvider(new DependentGenerator("C", "B"));
        propagator.addAdditionalStateProvider(new DependentGenerator("A", null));
        propagator.addAdditionalStateProvider(new DependentGenerator("D", "F"));

        final SpacecraftState finalState = propagator.propagate(orbit.getDate().shiftedBy(3600.0));
        Assertions.assertEquals(1,   finalState.getAdditionalState("A").length);
        Assertions.assertEquals(1.0, finalState.getAdditionalState("A")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("B").length);
        Assertions.assertEquals(2.0, finalState.getAdditionalState("B")[0], 1.0e-15);
        Assertions.assertEquals(1,   finalState.getAdditionalState("C").length);
        Assertions.assertEquals(3.0, finalState.getAdditionalState("C")[0], 1.0e-15);
        Assertions.assertFalse(finalState.hasAdditionalState("D"));
        Assertions.assertFalse(finalState.hasAdditionalState("E"));
        Assertions.assertFalse(finalState.hasAdditionalState("F"));

    }

    private void checkDerivatives(final Orbit orbit, final boolean expectedDerivatives) {
        Assertions.assertEquals(expectedDerivatives, orbit.hasDerivatives());
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getADot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getEquinoctialExDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getEquinoctialEyDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getHxDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getHyDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getLEDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getLvDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getLMDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getEDot()));
        Assertions.assertNotEquals(expectedDerivatives, Double.isNaN(orbit.getIDot()));
    }

    private static double tangLEmLv(double Lv, double ex, double ey){
        // tan ((LE - Lv) /2)) =
        return (ey*FastMath.cos(Lv) - ex*FastMath.sin(Lv)) /
        (1 + ex*FastMath.cos(Lv) + ey*FastMath.sin(Lv) + FastMath.sqrt(1 - ex*ex - ey*ey));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
    }

    @AfterEach
    public void tearDown() {
        mu   = Double.NaN;
    }

    private static class DependentGenerator implements AdditionalStateProvider {

        private final String name;
        private final String dependency;

        DependentGenerator(final String name, final String dependency) {
            this.name       = name;
            this.dependency = dependency;
        }

        public String getName() {
            return name;
        }

        public boolean yields(final SpacecraftState state) {
            return dependency != null && state.getAdditionalStatesValues().getEntry(dependency) == null;
        }

        public double[] getAdditionalState(final SpacecraftState state) {
            return new double[] {
                dependency == null ? 1.0 : state.getAdditionalState(dependency)[0] + 1.0
            };
        }

    }

}

