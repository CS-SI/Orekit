/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.bodies;


import java.text.ParseException;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class SolarBodyTest {

    @Test
    public void geocentricPV() throws OrekitException, ParseException {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        Frame geocentricFrame = FramesFactory.getGCRF();
        checkPV(CelestialBodyFactory.getMoon(), date, geocentricFrame,
                new Vector3D(-0.0022350411591597575, -0.0010106334699928434, -5.658291803646671E-4),
                new Vector3D(3.1279236468844985E-4, -4.526815459166321E-4, -2.428841016970333E-4));
        checkPV(CelestialBodyFactory.getEarth(), date, geocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
    }

    @Test
    public void heliocentricPV() throws OrekitException, ParseException {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        final Frame gcrf = FramesFactory.getGCRF();
        Frame heliocentricFrame = new Frame(gcrf, new TransformProvider() {
            private static final long serialVersionUID = 1L;
            public Transform getTransform(AbsoluteDate date)
                throws OrekitException {
                return new Transform(date, CelestialBodyFactory.getSun().getPVCoordinates(date, gcrf).negate());
            }
        }, "heliocentric/aligned GCRF", true);
        checkPV(CelestialBodyFactory.getSun(), date, heliocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
        checkPV(CelestialBodyFactory.getMercury(), date, heliocentricFrame,
                new Vector3D(0.3388866970713254, -0.16350851403469605, -0.12250815624343761),
                new Vector3D(0.008716751907934464, 0.02294287010530833, 0.011349219084264612));
        checkPV(CelestialBodyFactory.getVenus(), date, heliocentricFrame,
                new Vector3D(0.5733328682513444, -0.3947124128748959, -0.21383496742544283),
                new Vector3D(0.012311818929592546, 0.014756722625966128, 0.005857890214695866));
        checkPV(CelestialBodyFactory.getMars(), date, heliocentricFrame,
        new Vector3D(-0.15808000178306866, -1.3285167111540124, -0.6050478023304016),
        new Vector3D(0.014443621048367267, -1.3669889027283553E-4, -4.542404441793112E-4));
        checkPV(CelestialBodyFactory.getJupiter(), date, heliocentricFrame,
        new Vector3D(-5.387442227958154, -0.8116709870422928, -0.21662388956102652),
        new Vector3D(0.0010628473875341506, -0.006527800816267844, -0.0028242250304474767));
        checkPV(CelestialBodyFactory.getSaturn(), date, heliocentricFrame,
        new Vector3D(7.89952834654684, 4.582711147265509, 1.552649660593234),
        new Vector3D(-0.003208403682518813, 0.004335751536569781, 0.001928152129122073));
        checkPV(CelestialBodyFactory.getUranus(), date, heliocentricFrame,
        new Vector3D(-18.2705614311796, -1.151408356279009, -0.24540975062356502),
        new Vector3D(2.1887052624725852E-4, -0.0037678288699642877, -0.0016532828516810242));
        checkPV(CelestialBodyFactory.getNeptune(), date, heliocentricFrame,
        new Vector3D(-16.06747366050193, -23.938436657940095, -9.39837851302005),
        new Vector3D(0.0026425894813251684, -0.0015042632480101307, -6.815738977894145E-4));
        checkPV(CelestialBodyFactory.getPluto(), date, heliocentricFrame,
        new Vector3D(-30.488788499360652, -0.8637991387172488, 8.914537151982762),
        new Vector3D(3.21695873843002E-4, -0.0031487797507673814, -0.0010799339515148705));
    }

    @Test(expected = OrekitException.class)
    public void noMercury() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMercury();
    }

    @Test(expected = OrekitException.class)
    public void noVenus() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getVenus();
    }

    @Test(expected = OrekitException.class)
    public void noEarthMoonBarycenter() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getEarthMoonBarycenter();
    }

    @Test(expected = OrekitException.class)
    public void noMars() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMars();
    }

    @Test(expected = OrekitException.class)
    public void noJupiter() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getJupiter();
    }

    @Test(expected = OrekitException.class)
    public void noSaturn() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSaturn();
    }

    @Test(expected = OrekitException.class)
    public void noUranus() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getUranus();
    }

    @Test(expected = OrekitException.class)
    public void noNeptune() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getNeptune();
    }

    @Test(expected = OrekitException.class)
    public void noPluto() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getPluto();
    }

    @Test(expected = OrekitException.class)
    public void noMoon() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMoon();
    }

    @Test(expected = OrekitException.class)
    public void noSun() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSun();
    }

    private void checkPV(PVCoordinatesProvider body, AbsoluteDate date, Frame frame,
                         Vector3D position, Vector3D velocity)
    throws OrekitException {

        PVCoordinates pv = body.getPVCoordinates(date, frame);

        final double posScale = 149597870691.0;
        final double velScale = posScale / Constants.JULIAN_DAY;
        PVCoordinates reference =
            new PVCoordinates(new Vector3D(posScale, position), new Vector3D(velScale, velocity));

        PVCoordinates error = new PVCoordinates(reference, pv);
        Assert.assertEquals(0, error.getPosition().getNorm(), 2.0e-3);
        Assert.assertEquals(0, error.getVelocity().getNorm(), 5.0e-10);

    }

    @Test
    public void testFrameShift() throws OrekitException {
        Utils.setDataRoot("regular-data");
        final Frame moon  = CelestialBodyFactory.getMoon().getBodyOrientedFrame();
        final Frame earth = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate date0 = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());

        for (double t = 0; t < Constants.JULIAN_DAY; t += 3600) {
            final AbsoluteDate date = date0.shiftedBy(t);
            final Transform transform = earth.getTransformTo(moon, date);
            for (double dt = -10; dt < 10; dt += 0.125) {
                final Transform shifted  = transform.shiftedBy(dt);
                final Transform computed = earth.getTransformTo(moon, transform.getDate().shiftedBy(dt));
                final Transform error    = new Transform(computed.getDate(), computed, shifted.getInverse());
                Assert.assertEquals(0.0, error.getTranslation().getNorm(),   100.0);
                Assert.assertEquals(0.0, error.getVelocity().getNorm(),       20.0);
                Assert.assertEquals(0.0, error.getRotation().getAngle(),    4.0e-8);
                Assert.assertEquals(0.0, error.getRotationRate().getNorm(), 8.0e-10);
            }
        }
    }

    @Test
    public void testPropagationVsEphemeris() throws OrekitException {

        Utils.setDataRoot("regular-data");

        //Creation of the celestial bodies of the solar system
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody mercury = CelestialBodyFactory.getMercury();
        final CelestialBody venus   = CelestialBodyFactory.getVenus();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();  
        final CelestialBody mars    = CelestialBodyFactory.getMars();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();
        final CelestialBody saturn  = CelestialBodyFactory.getSaturn();
        final CelestialBody uranus  = CelestialBodyFactory.getUranus();
        final CelestialBody neptune = CelestialBodyFactory.getNeptune();
        final CelestialBody pluto   = CelestialBodyFactory.getPluto();

        //Starting and end dates
        final AbsoluteDate startingDate = new AbsoluteDate(2000, 1, 2, TimeScalesFactory.getUTC());
        AbsoluteDate endDate = startingDate.shiftedBy(30 * Constants.JULIAN_DAY);

        final Frame icrf = FramesFactory.getICRF();

        // fake orbit around negligible point mass at solar system barycenter
        double negligibleMu = 1.0e-3;
        SpacecraftState initialState = new SpacecraftState(new CartesianOrbit(venus.getPVCoordinates(startingDate, icrf),
                                                           icrf, startingDate, negligibleMu));

        //Creation of the numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1000, initialState.getOrbit(), OrbitType.CARTESIAN);
        AbstractIntegrator dop1 = new DormandPrince853Integrator(1.0, 1.0e5, tol[0], tol[1]);
        NumericalPropagator propag = new NumericalPropagator(dop1);
        propag.setOrbitType(OrbitType.CARTESIAN);
        propag.setInitialState(initialState);
        propag.setMu(negligibleMu);

        //Creation of the ForceModels
        propag.addForceModel(new BodyAttraction(sun)); 
        propag.addForceModel(new BodyAttraction(mercury));
        propag.addForceModel(new BodyAttraction(earth)); 
        propag.addForceModel(new BodyAttraction(mars)); 
        propag.addForceModel(new BodyAttraction(jupiter));
        propag.addForceModel(new BodyAttraction(saturn));
        propag.addForceModel(new BodyAttraction(uranus));
        propag.addForceModel(new BodyAttraction(neptune));
        propag.addForceModel(new BodyAttraction(pluto)); 

        // checks are done within the step handler
        propag.setMasterMode(1000.0, new OrekitFixedStepHandler() {
            public void init(SpacecraftState s0, AbsoluteDate t) {
            }
            public void handleStep(SpacecraftState currentState, boolean isLast)
                throws PropagationException {
                try {
                    // propagated position should remain within 1400m of ephemeris for one month
                    Vector3D propagatedP = currentState.getPVCoordinates(icrf).getPosition();
                    Vector3D ephemerisP  = venus.getPVCoordinates(currentState.getDate(), icrf).getPosition();
                    Assert.assertEquals(0, Vector3D.distance(propagatedP, ephemerisP), 1400.0);
                } catch (OrekitException oe) {
                    throw new PropagationException(oe);
                }
            }
        });

        propag.propagate(startingDate,endDate);

    }

    private static class BodyAttraction extends AbstractParameterizable implements ForceModel {

        /** Suffix for parameter name for attraction coefficient enabling jacobian processing. */
        public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

        /** The body to consider. */
        private final CelestialBody body;

        /** Local value for body attraction coefficient. */
        private double gm;

        /** Simple constructor.
         * @param body the third body to consider
         * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
         * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
         */
        public BodyAttraction(final CelestialBody body) {
            super(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX);
            this.body = body;
            this.gm   = body.getGM();
        }

        /** {@inheritDoc} */
        public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
            throws OrekitException {

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
            final double r2Sat           = satToBody.getNormSq();

            // compute relative acceleration
            final Vector3D gamma =
                new Vector3D(gm / (r2Sat * FastMath.sqrt(r2Sat)), satToBody);

            // add contribution to the ODE second member
            adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());

        }

        /** {@inheritDoc} */
        public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                          final FieldVector3D<DerivativeStructure> position,
                                                                          final FieldVector3D<DerivativeStructure> velocity,
                                                                          final FieldRotation<DerivativeStructure> rotation,
                                                                          final DerivativeStructure mass)
            throws OrekitException {

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody    = body.getPVCoordinates(date, frame).getPosition();
            final FieldVector3D<DerivativeStructure> satToBody = position.subtract(centralToBody).negate();
            final DerivativeStructure r2Sat = satToBody.getNormSq();

            // compute absolute acceleration
            return new FieldVector3D<DerivativeStructure>(r2Sat.pow(-1.5).multiply(gm), satToBody);

        }

        /** {@inheritDoc} */
        public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
            throws OrekitException {

            complainIfNotSupported(paramName);

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
            final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
            final double r2Sat           = Vector3D.dotProduct(satToBody, satToBody);

            final DerivativeStructure gmds = new DerivativeStructure(1, 1, 0, gm);

            // compute relative acceleration
            return new FieldVector3D<DerivativeStructure>(gmds.multiply(FastMath.pow(r2Sat, -1.5)), satToBody);

        }

        /** {@inheritDoc} */
        public EventDetector[] getEventsDetectors() {
            return new EventDetector[0];
        }

        /** {@inheritDoc} */
        public double getParameter(final String name)
            throws IllegalArgumentException {
            complainIfNotSupported(name);
            return gm;
        }

        /** {@inheritDoc} */
        public void setParameter(final String name, final double value)
            throws IllegalArgumentException {
            complainIfNotSupported(name);
            gm = value;
        }

    }

    @Test
    public void testKepler() throws OrekitException {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TimeScalesFactory.getTT());
        final double au = 149597870691.0;
        checkKepler(CelestialBodyFactory.getMoon(),    CelestialBodyFactory.getEarth(), date, 3.844e8, 0.012);
        checkKepler(CelestialBodyFactory.getMercury(), CelestialBodyFactory.getSun(),   date,  0.387 * au, 4.0e-9);
        checkKepler(CelestialBodyFactory.getVenus(),   CelestialBodyFactory.getSun(),   date,  0.723 * au, 8.0e-9);
        checkKepler(CelestialBodyFactory.getEarth(),   CelestialBodyFactory.getSun(),   date,  1.000 * au, 2.0e-5);
        checkKepler(CelestialBodyFactory.getMars(),    CelestialBodyFactory.getSun(),   date,  1.52  * au, 2.0e-7);
        checkKepler(CelestialBodyFactory.getJupiter(), CelestialBodyFactory.getSun(),   date,  5.20  * au, 2.0e-6);
        checkKepler(CelestialBodyFactory.getSaturn(),  CelestialBodyFactory.getSun(),   date,  9.58  * au, 8.0e-7);
        checkKepler(CelestialBodyFactory.getUranus(),  CelestialBodyFactory.getSun(),   date, 19.20  * au, 6.0e-7);
        checkKepler(CelestialBodyFactory.getNeptune(), CelestialBodyFactory.getSun(),   date, 30.05  * au, 4.0e-7);
        checkKepler(CelestialBodyFactory.getPluto(),   CelestialBodyFactory.getSun(),   date, 39.24  * au, 3.0e-7);
    }

    private void checkKepler(final PVCoordinatesProvider orbiting, final CelestialBody central,
                             final AbsoluteDate start, final double a, final double epsilon)
        throws OrekitException {

        // set up Keplerian orbit of orbiting body around central body
        Orbit orbit = new KeplerianOrbit(orbiting.getPVCoordinates(start, central.getInertiallyOrientedFrame()),
                                         central.getInertiallyOrientedFrame(),start, central.getGM());
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Assert.assertEquals(a, orbit.getA(), 0.02 * a);
        double duration = FastMath.min(50 * Constants.JULIAN_DAY, 0.01 * orbit.getKeplerianPeriod());

        double max = 0;
        for (AbsoluteDate date = start; date.durationFrom(start) < duration; date = date.shiftedBy(duration / 100)) {
            PVCoordinates ephemPV = orbiting.getPVCoordinates(date, central.getInertiallyOrientedFrame());
            PVCoordinates keplerPV = propagator.propagate(date).getPVCoordinates();
            Vector3D error = keplerPV.getPosition().subtract(ephemPV.getPosition());
            max = FastMath.max(max, error.getNorm());
        }
        Assert.assertTrue(max < epsilon * a);
    }

}
