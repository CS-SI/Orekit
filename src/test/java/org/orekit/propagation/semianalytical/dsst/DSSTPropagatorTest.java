/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class DSSTPropagatorTest {

    private AbsoluteDate                  initDate;
    private SpacecraftState               initialState;
    private DSSTPropagator                propaDSST;
    private NumericalPropagator           numProp;
    private boolean                       gotHere;
    private double                        mu;
    private double                        ae;
    private PotentialCoefficientsProvider provider;

    @Test
    public void testNoExtrapolation() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        // Propagation of the initial state at the initial date
        final SpacecraftState finalState = propaDSST.propagate(initDate);

        // Initial orbit definition
        final Vector3D initialPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX(), finalPosition.getX(), 0.0);
        Assert.assertEquals(initialPosition.getY(), finalPosition.getY(), 0.0);
        Assert.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 0.0);
        Assert.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 0.0);
        Assert.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 0.0);
        Assert.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 0.0);
    }

    @Test
    public void testKepler() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = propaDSST.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(), finalState.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 0.);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 1.e-14);

    }

    @Test
    public void testEphemeris() throws OrekitException {
        SpacecraftState state = getGEOrbit();
        setDSSTProp(state);

        // Set ephemeris mode
        propaDSST.setEphemerisMode();

        // Propagation of the initial state at t + 10 days
        final double dt = 2. * Constants.JULIAN_DAY;
        propaDSST.propagate(initDate.shiftedBy(5. * dt));

        // Get ephemeris
        BoundedPropagator ephem = propaDSST.getGeneratedEphemeris();

        // Propagation of the initial state with ephemeris at t + 2 days
        final SpacecraftState s = ephem.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(), s.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), s.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), s.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), s.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), s.getHy(), 0.);
        Assert.assertEquals(initialState.getLM() + n * dt, s.getLM(), 1.5e-14);

    }

    @Test
    public void testImpulseManeuver() throws OrekitException {
        final Orbit initialOrbit = new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0, PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(new DateComponents(2008, 06, 23), new TimeComponents(14, 18, 37), TimeScalesFactory.getUTC()), 3.986004415e14);
        final double a = initialOrbit.getA();
        final double e = initialOrbit.getE();
        final double i = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;

        final double[][] tol = DSSTPropagator.tolerances(1.0, initialOrbit);
        FirstOrderIntegrator integrator = new DormandPrince853Integrator(10., 1000., tol[0], tol[1]);
        DSSTPropagator propagator = new DSSTPropagator(integrator, initialOrbit, false, new LofOffset(initialOrbit.getFrame(), LOFType.VVLH));
        propagator.addEventDetector(new ImpulseManeuver(new NodeDetector(initialOrbit, FramesFactory.getEME2000()), new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    @Test
    public void testPropagationWithCentralBody() throws Exception {
        // 1 day extrapolation time
        final double dt = 86400.;
        final double checkStep = 500;

        SpacecraftState orbit = getLEOrbit();

        // First test the propagation without any force model :
        setNumProp(orbit);
        setDSSTProp(orbit);
        double[] tolerance = new double[] { 0d, 0d, 0d, 0d, 0d, 6e-12 };
        // Check extrapolation process via the step handler :
        propaDSST.setMasterMode(checkStep, new StepChecker(numProp, tolerance));
        propaDSST.propagate(initDate.shiftedBy(dt));

        // Now add force model :

        // Central Body Force Model 5x0
        double[][] Cnm = provider.getC(5, 0, false);
        double[][] Snm = provider.getS(5, 0, false);

        // force expression :
        DSSTForceModel force = new DSSTCentralBody(Constants.WGS84_EARTH_ANGULAR_VELOCITY, ae, mu, Cnm, Cnm);
        ForceModel nForce = new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu, Cnm, Snm);

        // Reset propagators
        setNumProp(orbit);
        numProp.addForceModel(nForce);
        setDSSTProp(orbit);
        propaDSST.addForceModel(force);

        // Max difference between DSST and numerical propagator
        tolerance = new double[] { 20000,  0.002, 0.002, 0.1, 0.1, 1.0 };
        // Check extrapolation process via the step handler :
        propaDSST.setMasterMode(checkStep, new StepChecker(numProp, tolerance));
        propaDSST.propagate(initDate.shiftedBy(dt));
    }

    @Test
    public void testPropagationWithThirdBody() throws OrekitException, IOException {
        // 10 days Extrapolation time
        final double dt = 10 * 86400.;
        final double checkStep = 500;

        SpacecraftState state = getGEOrbit();

        /** Third Body Force Model (Moon) */
        DSSTForceModel force = new DSSTThirdBody(CelestialBodyFactory.getMoon());
        ForceModel nForce = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());

        // DSST Propagation
        setDSSTProp(state);
        propaDSST.addForceModel(force);

        // Numerical Propagation
        setNumProp(state);
        numProp.addForceModel(nForce);

        // Tolerance established thanks to a step control every 10 seconds over 50 days : should not
        // exceed :
        double[] tolerance = new double[] { 1476, 5.2E-5, 7.8E-5, 5.5E-6, 5E-6, 9.4E-4 };

        propaDSST.setMasterMode(checkStep, new StepChecker(numProp, tolerance));

        propaDSST.propagate(initDate.shiftedBy(dt));

        /** Third Body Force Model (Sun) */
        force = new DSSTThirdBody(CelestialBodyFactory.getSun());
        nForce = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        setDSSTProp(state);
        propaDSST.addForceModel(force);

        setNumProp(state);
        numProp.addForceModel(nForce);

        tolerance = new double[] { 507.29, 2.3E-5, 2.5E-5, 1.6E-6, 1.55E-6, 2.4e-4 };
        propaDSST.setMasterMode(checkStep, new StepChecker(numProp, tolerance));

        propaDSST.propagate(initDate.shiftedBy(dt));

    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);
        setNumProp(state);

        // Propagation duration
        final double dt = 864000.;

        // Drag Force Model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF2005());
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        Atmosphere atm = new HarrisPriester(sun, earth);
//        Atmosphere atm = new SimpleExponentialAtmosphere(earth, 4.e-13, 500000.0, 60000.0);
        final double cd = 2.0;
        final double sf = 5.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, sf);
        ForceModel ndrag = new DragForce(atm, new SphericalSpacecraft(sf, cd, 0., 0.));

        // DSST Propagation
        propaDSST.addForceModel(drag);
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Numerical Propagation
        numProp.addForceModel(ndrag);
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 10.e+3);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(),  2.e+3);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 22.e+3);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 20.);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 13.);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 10.);
    }

    @Test
    public void testPropagationWithSolarRadiationPressure() throws OrekitException {
        SpacecraftState state = getGEOrbit();
        setDSSTProp(state);

        // Propagation duration
        final double dt = 86400.;

        // DSST Propagation with no force (reference Keplerian motion)
        PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Solar Radiation Pressure Force Model
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        double sf = 5.0;
        double kA = 0.5;
        double kR = 0.5;
        double cR =  (1. + (1. - kA) * (1. - kR) * 4. / 9.);
        DSSTForceModel srp = new DSSTSolarRadiationPressure(cR, sf, sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        SphericalSpacecraft spc = new SphericalSpacecraft(sf, 0., kA, kR);
        ForceModel nsrp = new SolarRadiationPressure(sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spc);

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(srp);
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Numerical Propagation
        setNumProp(initialState);
        numProp.addForceModel(nsrp);
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " "
        // + (pvn.getPosition().getX() - pvd.getPosition().getX()));
        // System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " "
        // + (pvn.getPosition().getY() - pvd.getPosition().getY()));
        // System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " "
        // + (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        // System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " "
        // + (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        // System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " "
        // + (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        // System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " "
        // + (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));
        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 40.0);
        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 50.0);
        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 0.2);
        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 3.e-3);
        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 5.e-3);
        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 1.e-5);

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 7.0);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 55.0);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 0.1);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 4.e-3);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 5.e-4);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 2.e-5);
    }

    @Test
    public void testStopEvent() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final AbsoluteDate stopDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(stopDate) {
            private static final long serialVersionUID = -5024861864672841095L;

            public EventDetector.Action eventOccurred(SpacecraftState s,
                                                      boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.STOP;
            }

            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = propaDSST.propagate(initDate.shiftedBy(3200));
        Assert.assertTrue(gotHere);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
    }

    @Test
    public void testContinueEvent() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 5959523015368708867L;

            public EventDetector.Action eventOccurred(SpacecraftState s,
                                                      boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.CONTINUE;
            }
        });
        final double dt = 3200;
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = propaDSST.propagate(initDate.shiftedBy(dt));
        Assert.assertTrue(gotHere);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    private void setGotHere(boolean gotHere) {
        this.gotHere = gotHere;
    }

    private SpacecraftState getGEOrbit() throws IllegalArgumentException, OrekitException {
        // No shadow at this date
        initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3), FastMath.tan(0.001745329)
                                                                                                                                        * FastMath.sin(2 * FastMath.PI / 3), 0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), initDate, mu);
        return new SpacecraftState(orbit);
    }

    private SpacecraftState getLEOrbit() throws IllegalArgumentException, OrekitException {
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        // Spring equinoxe 21st mars 2003 1h00m
        initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        return new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), initDate, mu));
    }

    private void setNumProp(SpacecraftState initialState) throws PropagationException {
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialState.getOrbit(), initialState.getOrbit().getType());
        final double minStep = 10.;
        final double maxStep = 1000.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        numProp = new NumericalPropagator(integrator);
        numProp.setInitialState(initialState);
    }

    private void setDSSTProp(SpacecraftState initialState) throws OrekitException {
        this.initialState = initialState;
        this.initDate = initialState.getDate();
        final double minStep = 1.e3;
        final double maxStep = 1.e5;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        propaDSST = new DSSTPropagator(integrator, initialState.getOrbit(), false, 864000.);

    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        provider = GravityFieldFactory.getPotentialProvider();
        mu = provider.getMu();
        ae = provider.getAe();
        gotHere = false;
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propaDSST = null;
        gotHere = false;
    }

    /**
     * Specialized step handler.
     * <p>
     * This class extends the step handler in order to print on the output stream at the given step.
     * <p>
     */
    private static class StepChecker implements OrekitFixedStepHandler {

        /** Reference propagator */
        private final AbstractIntegratedPropagator reference;

        /** Relative tolerances between DSST propagator and the numerical propagator
         *  (da, dex, dey, dhx, dhy, dlm)
         */
        private final double[] tolerance;

        /** Serializable UID. */
        private static final long serialVersionUID = -8909135870522456848L;

        /** Constructor
         *
         *  @param propagatorRef Reference propagator
         *  @param tolerance relative tolerances (da, dex, dey, dhx, dhy, dlm)
         */
        private StepChecker(final AbstractIntegratedPropagator propagatorRef,
                            double[] tolerance) {
            this.reference = propagatorRef;
            this.tolerance = tolerance;
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        /** {@inheritDoc} */
        public void handleStep(SpacecraftState currentState, boolean isLast) {

            EquinoctialOrbit orb = new EquinoctialOrbit(currentState.getOrbit());
            EquinoctialOrbit orbRef = null;
            try {
                orbRef = new EquinoctialOrbit(reference.propagate(currentState.getDate()).getOrbit());
            } catch (PropagationException e) {
                e.printStackTrace();
            }

            Assert.assertEquals(orbRef.getA(), orb.getA(), tolerance[0]);
            Assert.assertEquals(orbRef.getEquinoctialEx(), orb.getEquinoctialEx(), tolerance[1]);
            Assert.assertEquals(orbRef.getEquinoctialEy(), orb.getEquinoctialEy(), tolerance[2]);
            Assert.assertEquals(orbRef.getHx(), orb.getHx(), tolerance[3]);
            Assert.assertEquals(orbRef.getHy(), orb.getHy(), tolerance[4]);
            Assert.assertEquals(orbRef.getLM(), orb.getLM(), tolerance[5]);

        }
    }

}
