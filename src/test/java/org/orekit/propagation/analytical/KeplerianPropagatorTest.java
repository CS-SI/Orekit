/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class KeplerianPropagatorTest extends TestCase {

    // Body mu
    private double mu;
    
    public KeplerianPropagatorTest(String name) {
        super(name);
    }

    public void testSameDateCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * Math.abs(n*delta_t));
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

    }

    public void testSameDateKeplerian() throws OrekitException {
        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * Math.max(100.,Math.abs(n*delta_t)));
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(),initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

    }


    public void testPropagatedCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);


        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*Math.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*Math.cos(finalOrbit.getLE());

        assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE()))
        - finalOrbit.getEquinoctialEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

        assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Excentric latitude arguments are the same
        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

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
        double beta = 1. / (1. + Math.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*Math.cos(LE) + beta*ex*ey*Math.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*Math.sin(LE) + beta*ex*ey*Math.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    public void testPropagatedKeplerian() throws OrekitException {

        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);
        assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod(), 1.0e-12);
        assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion(), 1.0e-16);

        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*Math.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*Math.cos(finalOrbit.getLE());

        assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE())) - finalOrbit.getEquinoctialEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

        assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Excentric latitude arguments are the same
        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

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
        double beta = 1. / (1. + Math.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*Math.cos(LE) + beta*ex*ey*Math.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*Math.sin(LE) + beta*ex*ey*Math.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    public void testWrongAttitude() {
        try {
            KeplerianOrbit orbit =
                new KeplerianOrbit(1.0e10, 1.0e-4, 1.0e-2, 0, 0, 0, KeplerianOrbit.TRUE_ANOMALY,
                                   Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
            AttitudeLaw wrongLaw = new AttitudeLaw() {
                private static final long serialVersionUID = 5918362126173997016L;
                public Attitude getState(AbsoluteDate date, PVCoordinates pv,
                                         Frame frame) throws OrekitException {
                    throw new OrekitException("gasp", new Object[0], new RuntimeException());
                }
            };
            KeplerianPropagator propagator = new KeplerianPropagator(orbit, wrongLaw);
            propagator.propagate(new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10.0));
            fail("an exception should have been thrown");
        } catch (PropagationException pe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testAscendingNode() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new NodeDetector(orbit, Frame.getITRF2000B()));
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(Frame.getITRF2000B());
        assertTrue(farTarget.minus(propagated.getDate()) > 3500.0);
        assertTrue(farTarget.minus(propagated.getDate()) < 4000.0);
        assertEquals(0, pv.getPosition().getZ(), 1.0e-6);
        assertTrue(pv.getVelocity().getZ() > 0);
    }

    public void testPerigee() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new ApsideDetector(orbit));
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        PVCoordinates pv = propagated.getPVCoordinates(Frame.getITRF2000B());
        assertTrue(farTarget.minus(propagated.getDate()) > 3000.0);
        assertTrue(farTarget.minus(propagated.getDate()) < 3500.0);
        assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 1.0e-6);
    }

    public void testDate() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AbsoluteDate stopDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        assertEquals(0, stopDate.minus(propagated.getDate()), 1.0e-10);
    }

    public void testSetting() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, Frame.getITRF2000B());
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(-2.962, 0.389, 0), null);
        propagator.addEventDetector(new ElevationDetector(60, 0.09, topo));
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition(),
                                                   propagated.getFrame(),
                                                   propagated.getDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        assertTrue(farTarget.minus(propagated.getDate()) > 7800.0);
        assertTrue(farTarget.minus(propagated.getDate()) < 7900.0);
        assertEquals(0.09, elevation, 1.0e-11);
        assertTrue(zVelocity < 0);
    }

    public void testFixedStep() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = 100.0;
        propagator.setMasterMode(step, new OrekitFixedStepHandler() {
            private static final long serialVersionUID = 5343978335581094125L;
            private AbsoluteDate previous;
            public void handleStep(SpacecraftState currentState, boolean isLast)
            throws PropagationException {
                if (previous != null) {
                    assertEquals(step, currentState.getDate().minus(previous), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        propagator.propagate(farTarget);
    }

    public void testVariableStep() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = orbit.getKeplerianPeriod() / 100;
        propagator.setMasterMode(new OrekitStepHandler() {
            private static final long serialVersionUID = -7257691813065811595L;
            private AbsoluteDate previous;
            public void handleStep(OrekitStepInterpolator interpolator,
                                   boolean isLast) throws PropagationException {
                if (previous != null) {
                    assertEquals(step, interpolator.getCurrentDate().minus(previous), 1.0e-10);
                }
                previous = interpolator.getCurrentDate();
            }
            public boolean requiresDenseOutput() {
                return false;
            }
            public void reset() {
            }
        });
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        propagator.propagate(farTarget);
    }

    public void testEphemeris() throws OrekitException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.setEphemerisMode();
        AbsoluteDate farTarget = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 10000.0);
        propagator.setEphemerisMode();
        propagator.propagate(farTarget);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        assertTrue(Double.isInfinite(ephemeris.getMinDate().minus(AbsoluteDate.J2000_EPOCH)));
        assertTrue(Double.isInfinite(ephemeris.getMaxDate().minus(AbsoluteDate.J2000_EPOCH)));
    }

    private static double tangLEmLv(double Lv,double ex,double ey){
        // tan ((LE - Lv) /2)) =
        return (ey*Math.cos(Lv) - ex*Math.sin(Lv)) /
        (1 + ex*Math.cos(Lv) + ey*Math.sin(Lv) + Math.sqrt(1 - ex*ex - ey*ey));
    }

    public void setUp() {
        mu  = 3.9860047e14;
    }

    public void tearDown() {
        mu   = Double.NaN;
    }
    
    public static Test suite() {
        return new TestSuite(KeplerianPropagatorTest.class);
    }

}

