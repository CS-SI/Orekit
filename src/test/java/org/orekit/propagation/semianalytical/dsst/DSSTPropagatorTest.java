/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
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
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class DSSTPropagatorTest {

    private double              mu;
    private AbsoluteDate        initDate;
    private SpacecraftState     initialState;
    private DSSTPropagator      propaDSST;
    private NumericalPropagator numProp;
    private boolean             gotHere;

    @Test
    public void testNoExtrapolation() throws OrekitException {

        // Propagation of the initial state at the initial date
        final SpacecraftState finalState = propaDSST.propagate(initDate);

        // Initial orbit definition
        final Vector3D initialPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition   = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity   = finalState.getPVCoordinates().getVelocity();

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

        // Propagation of the initial state at t + dt
        final double dt = 3200;
        final SpacecraftState finalState = propaDSST.propagate(initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),             finalState.getA(),             0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(),            finalState.getHx(),            0.);
        Assert.assertEquals(initialState.getHy(),            finalState.getHy(),            0.);
        Assert.assertEquals(initialState.getLM() + n * dt,   finalState.getLM(),            2.0e-15);

    }

    @Test
    public void testAccumulator() throws OrekitException {

        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        // Propagation of the initial state at t + 2*dt then back to t + dt, back to t - 2*dt and forth to t - dt
        final double dt = 3200;
        propaDSST.propagate(initDate.shiftedBy(2*dt));
        final SpacecraftState finalState1 = propaDSST.propagate(initDate.shiftedBy(dt));

        // Check results
        Assert.assertEquals(initialState.getA(),             finalState1.getA(),             0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState1.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState1.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(),            finalState1.getHx(),            0.);
        Assert.assertEquals(initialState.getHy(),            finalState1.getHy(),            0.);
        Assert.assertEquals(initialState.getLM() + n * dt,   finalState1.getLM(),            2.0e-15);

        // Continue propagation of the initial state back to t - 2*dt and forth to t - dt
        propaDSST.propagate(initDate.shiftedBy(-2*dt));
        final SpacecraftState finalState2 = propaDSST.propagate(initDate.shiftedBy(-dt));
        // Check results
        Assert.assertEquals(initialState.getA(),             finalState2.getA(),             0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState2.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState2.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(),            finalState2.getHx(),            0.);
        Assert.assertEquals(initialState.getHy(),            finalState2.getHy(),            0.);
        Assert.assertEquals(initialState.getLM() - n * dt,   finalState2.getLM(),            3.0e-15);

    }

    @Test
    public void testPropagationWithCentralBody() throws OrekitException, IOException, ParseException {

        // DSST Propagation with no force (reference Keplerian motion)
        final double dt = 864000;
        PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Central Body Force Model
        PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        double[][] Cnm = provider.getC(5, 5, true);
        double[][] Snm = provider.getS(5, 5, true);
//        DSSTForceModel force = new DSSTCentralBody(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Cnm, Snm, null);
        ForceModel nForce = new CunninghamAttractionModel(FramesFactory.getITRF2005(),
                                                          Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                          mu, Cnm, Snm);

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
//        propaDSST.addForceModel(force);
        long start = System.nanoTime();
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        double elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 1: " + elapsedTimeInSec);

        // Numerical Propagation
        setNumProp();
        numProp.addForceModel(nForce);
        start = System.nanoTime();
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 2: " + elapsedTimeInSec);

        System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " " +
                           (pvn.getPosition().getX() - pvd.getPosition().getX()));
        System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " " +
                           (pvn.getPosition().getY() - pvd.getPosition().getY()));
        System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " " +
                           (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " " +
                           (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " " +
                           (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " " +
                           (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));

//        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 0.0);
//        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 0.0);
//        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 0.0);
//        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 0.e-1);
//        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 0.e-1);
//        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 0.e-1);
//    
//        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 1.5);
//        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 0.2);
//        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 5.0);
//        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 1.e-3);
//        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 1.e-3);
//        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 4.e-3);
    }

    @Test
    public void testPropagationWithThirdBody() throws OrekitException, IOException, ParseException {

        // DSST Propagation with no force (reference Keplerian motion)
        final double dt = 864000;
        PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Third Body Force Model
        DSSTForceModel force = new DSSTThirdBody();
        ForceModel nForce = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(force);
        long start = System.nanoTime();
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        double elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 1: " + elapsedTimeInSec);

        // Numerical Propagation
        setNumProp();
        numProp.addForceModel(nForce);
        start = System.nanoTime();
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 2: " + elapsedTimeInSec);

        System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " " +
                           (pvn.getPosition().getX() - pvd.getPosition().getX()));
        System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " " +
                           (pvn.getPosition().getY() - pvd.getPosition().getY()));
        System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " " +
                           (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " " +
                           (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " " +
                           (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " " +
                           (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));

//        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 0.0);
//        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 0.0);
//        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 0.0);
//        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 0.e-1);
//        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 0.e-1);
//        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 0.e-1);
//    
//        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 1.5);
//        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 0.2);
//        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 5.0);
//        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 1.e-3);
//        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 1.e-3);
//        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 4.e-3);
    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {

        // Propagation duration
        final double dt = 864000;

        // DSST Propagation with no force (reference Keplerian motion)
        long start = System.nanoTime();
        PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        double elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 0: " + elapsedTimeInSec);

        // Drag Force Model
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF2005());
        earth.setAngularThreshold(1.e-10);
        Atmosphere atm = new HarrisPriester(sun, earth);
        final double cd = 2.0;
        final double sf = 5.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, sf);
        ForceModel ndrag = new DragForce(atm, new SphericalSpacecraft(sf, cd, 0., 0.));

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(drag);
        start = System.nanoTime();
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 1: " + elapsedTimeInSec);

        // Numerical Propagation
        setNumProp();
        numProp.addForceModel(ndrag);
        start = System.nanoTime();
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 2: " + elapsedTimeInSec);
        
        System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " " +
                           (pvn.getPosition().getX() - pvd.getPosition().getX()));
        System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " " +
                           (pvn.getPosition().getY() - pvd.getPosition().getY()));
        System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " " +
                           (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " " +
                           (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " " +
                           (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " " +
                           (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));

        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 10000.0);
        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(),  2000.0);
        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 22000.0);
        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 20.);
        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 13.);
        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 10.);

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(),  6.0);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(),  2.0);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 13.0);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 2.e-2);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 1.e-2);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 6.e-3);
    }

    @Test
    public void testPropagationWithSolarRadiationPressure() throws OrekitException {

        // Propagation duration
        final double dt = 3*86400.;

        // DSST Propagation with no force (reference Keplerian motion)
        long start = System.nanoTime();
        PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        double elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 0: " + elapsedTimeInSec);

        // Solar Radiation Pressure Force Model
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        double sf = 5.0;
        double cr = 2.0;
        DSSTForceModel srp  = new DSSTSolarRadiationPressure(cr, sf, sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        SphericalSpacecraft spc = new SphericalSpacecraft(sf, 0., 0., cr);
        ForceModel nsrp = new SolarRadiationPressure(sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spc);

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(srp);
        start = System.nanoTime();
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 1: " + elapsedTimeInSec);

        // Numerical Propagation
        setNumProp();
        numProp.addForceModel(nsrp);
        start = System.nanoTime();
        PVCoordinates pvn = numProp.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        elapsedTimeInSec = (System.nanoTime() - start) * 1.0e-9;
        System.out.println("Prop 2: " + elapsedTimeInSec);

        System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " " +
                           (pvn.getPosition().getX() - pvd.getPosition().getX()));
        System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " " +
                           (pvn.getPosition().getY() - pvd.getPosition().getY()));
        System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " " +
                           (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " " +
                           (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " " +
                           (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " " +
                           (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));

//        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 1.1);
//        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 0.05);
//        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 4.0);
//        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 2.e-4);
//        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 6.e-4);
//        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 3.e-3);
//
//        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 0.5);
//        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 0.3);
//        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 4.0);
//        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 8.e-3);
//        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 4.e-4);
//        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 3.e-3);
    }

    @Test
    public void testImpulseManeuver() throws OrekitException {
        final Orbit initialOrbit =
            new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0,
                               PositionAngle.MEAN, FramesFactory.getEME2000(),
                               new AbsoluteDate(new DateComponents(2008, 06, 23),
                                                new TimeComponents(14, 18, 37),
                                                TimeScalesFactory.getUTC()),
                               3.986004415e14);
        final double a  = initialOrbit.getA();
        final double e  = initialOrbit.getE();
        final double i  = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;

        final double[][] tol = DSSTPropagator.tolerances(1.0, initialOrbit);
        FirstOrderIntegrator integrator = new DormandPrince853Integrator(10., 1000., tol[0], tol[1]);
        DSSTPropagator propagator = new DSSTPropagator(integrator, initialOrbit,
                                                       new LofOffset(initialOrbit.getFrame(), LOFType.VVLH));
        propagator.addEventDetector(new ImpulseManeuver(new NodeDetector(initialOrbit, FramesFactory.getEME2000()),
                                                        new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    @Test
    public void testStopEvent() throws OrekitException {
        final AbsoluteDate stopDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(stopDate) {
            private static final long serialVersionUID = -5024861864672841095L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
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
    public void testResetStateEvent() throws OrekitException {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 6453983658076746705L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.RESET_STATE;
            }
            public SpacecraftState resetState(SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        });
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = propaDSST.propagate(initDate.shiftedBy(3200));
        Assert.assertTrue(gotHere);
        Assert.assertEquals(initialState.getMass() - 200, finalState.getMass(), 1.0e-10);
    }

    @Test
    public void testContinueEvent() throws OrekitException {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 5959523015368708867L;
            public EventDetector.Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
                setGotHere(true);
                return EventDetector.Action.CONTINUE;
            }
        });
        final double dt = 3200;
        Assert.assertFalse(gotHere);
        final SpacecraftState finalState = 
            propaDSST.propagate(initDate.shiftedBy(dt));
        Assert.assertTrue(gotHere);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    private void setGotHere(boolean gotHere) {
        this.gotHere = gotHere;
    }

    private void setNumProp() {
        final double[][] tol = NumericalPropagator.tolerances(0.1, initialState.getOrbit(), OrbitType.EQUINOCTIAL);
        final double minStep = 10.;
        final double maxStep = 1000.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);
        numProp = new NumericalPropagator(integrator);
        numProp.setInitialState(initialState);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        // Equinoxe 21 mars 2003 à 1h00m
        initDate = new AbsoluteDate(new DateComponents(2003, 03, 21),
                                    new TimeComponents(1, 0, 0.),
                                    TimeScalesFactory.getUTC());
        mu  = Constants.WGS84_EARTH_MU;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        final double minStep = 1000.;
        final double maxStep = 100000.;
        final double[][] tol = DSSTPropagator.tolerances(0.1, orbit);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(10000.);
        propaDSST = new DSSTPropagator(integrator, orbit);
        gotHere = false;
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propaDSST = null;
        gotHere = false;
    }

}

