/*
 * Copyright 2002-2011 CS Communication & Systèmes Licensed to CS Communication & Systèmes (CS)
 * under one or more contributor license agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership. CS licenses this file to You under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.orekit.propagation.semianalytical.dsst;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

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
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.SimpleExponentialAtmosphere;
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
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.OrbitFactory;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.ResonantCouple;
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
    private static String                 format = new String("%14.10f %14.10f %14.10f %14.10f %14.10f %14.10f %14.10f");

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
    public void testAccumulator() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        // Propagation of the initial state at t + 2*dt then back to t + dt, back to t - 2*dt and
        // forth to t - dt
        final double dt = 3200;
        propaDSST.propagate(initDate.shiftedBy(2 * dt));
        final SpacecraftState finalState1 = propaDSST.propagate(initDate.shiftedBy(dt));

        // Check results
        Assert.assertEquals(initialState.getA(), finalState1.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState1.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState1.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), finalState1.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), finalState1.getHy(), 0.);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState1.getLM(), 1.e-14);

        // Continue propagation of the initial state back to t - 2*dt and forth to t - dt
        propaDSST.propagate(initDate.shiftedBy(-2 * dt));
        final SpacecraftState finalState2 = propaDSST.propagate(initDate.shiftedBy(-dt));
        // Check results
        Assert.assertEquals(initialState.getA(), finalState2.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState2.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState2.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), finalState2.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), finalState2.getHy(), 0.);
        Assert.assertEquals(initialState.getLM() - n * dt, finalState2.getLM(), 1.e-12);

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
        DSSTPropagator propagator = new DSSTPropagator(integrator, initialOrbit, new LofOffset(initialOrbit.getFrame(), LOFType.VVLH));
        propagator.addEventDetector(new ImpulseManeuver(new NodeDetector(initialOrbit, FramesFactory.getEME2000()), new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    // @Test
    // public void testPropagationWithCentralBody2() throws OrekitException, IOException,
    // ParseException {
    //
    // // DSST Propagation with no force (reference Keplerian motion)
    // final double tMax = 86400.;
    // final double step = 1000.;
    // // PVCoordinates pv = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
    //
    // // Central Body Force Model
    // PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
    // double[][] CnmUnNormalized = provider.getC(2, 2, false);
    // double[][] SnmUnNormalized = provider.getS(2, 2, false);
    //
    // double[][] Cnm = provider.getC(2, 2, true);
    // double[][] Snm = provider.getS(2, 2, true);
    //
    // DSSTForceModel force = new DSSTCentralBody(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Cnm, Snm,
    // null, 1e-4);
    //
    // ForceModel nForce = new CunninghamAttractionModel(FramesFactory.getITRF2005(),
    // Constants.WGS84_EARTH_EQUATORIAL_RADIUS, mu, CnmUnNormalized, SnmUnNormalized);
    //
    // // Define initial state
    // Orbit orbit = OrbitFactory.getHeliosynchronousOrbit(800000.,
    // 1e-4, Math.PI / 2., 0., Math.PI / 2.,
    // PositionAngle.MEAN,
    // provider.getMu());
    // SpacecraftState state = new SpacecraftState(orbit);
    //
    // setDSSTProp(state);
    // propaDSST.addForceModel(force);
    //
    // // Numerical Propagation
    // setNumProp(initialState);
    // numProp.addForceModel(nForce);
    //
    // AbsoluteDate current = initialState.getDate();
    //
    // while (current.compareTo(initialState.getDate().shiftedBy(tMax)) < 0) {
    // current = current.shiftedBy(step);
    // PVCoordinates pvd = propaDSST.propagate(current).getPVCoordinates();
    // PVCoordinates pvn = numProp.propagate(current).getPVCoordinates();
    // KeplerianOrbit orbNum = new KeplerianOrbit(pvn, FramesFactory.getEME2000(), current, mu);
    // KeplerianOrbit orbDsst = new KeplerianOrbit(pvd, FramesFactory.getEME2000(), current, mu);
    // // Anomaly
    // final double anNum = orbNum.getAnomaly(PositionAngle.TRUE);
    // final double anDsst = orbDsst.getAnomaly(PositionAngle.TRUE);
    //
    // printShift(anNum, anDsst);
    //
    // }
    // }

    // private void printShift(double val1, double val2) {
    // System.out.println(val1 + "  " + val2 + "  " + Math.abs(val1 - val2));
    // }

   
    /**
     * 
     * @throws Exception
     */
    @Test
    public void testPropagationWithCentralBody() throws Exception {
        final double dt = 365*86400.;
        final int printStep = 1000;
        boolean printPV = true;
        boolean meanOrbit = false;
        
        /**
         * FORCES :
         */
        // Central Body Force Model
        double[][] CnmNotNorm = provider.getC(5, 5, false);
        double[][] SnmNotNorm = provider.getS(5, 5, false);
        
        List<ResonantCouple> listCouple = new ArrayList<ResonantCouple>();
        

        DSSTForceModel force = new DSSTCentralBody(ae, mu,CnmNotNorm, CnmNotNorm, listCouple, 1e-4);
        ForceModel nForce = new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu, CnmNotNorm, SnmNotNorm);
        
//        SpacecraftState orbitOsc = new SpacecraftState(OrbitFactory.getHeliosynchronousOrbit(provider.getAe(), 800000, 1e-3, 0, Math.PI / 2d, Math.PI, provider.getMu(), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH));
        SpacecraftState orbitOsc = new SpacecraftState(OrbitFactory.getGeostationnaryOrbit(mu, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH));

        
        /**
         * Numerical propagator initialization for averaging
         */
        setNumProp(orbitOsc);
        numProp.addForceModel(nForce);
        
        /** Initialisation of osculating and mean orbit, if needed */
        SpacecraftState[] orbits;
        
        SpacecraftState mean = null;
        SpacecraftState osc = null;

        if (meanOrbit) {
            // Create a fake mean orbit from an osculating one
            orbits = OrbitFactory.getMeanOrbitFromOsculating(numProp, 2*86400, 28, 1);
            
            mean = orbits[0];
            osc = orbits[1];

            setDSSTProp(mean);
        } else {
            setDSSTProp(orbitOsc);
        }

        // DSST Propagation with no force (reference Keplerian motion)
//        SpacecraftState finalState0 = propaDSST.propagate(initDate.shiftedBy(dt));
//        PVCoordinates pv = finalState0.getPVCoordinates();



        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(force);
        if (printPV) {
            propaDSST.setMasterMode(printStep, new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/CentralBody/"), new String("CentralBody_DSST_PV"), PrintStepHandler.PrintEnum.PV, format, initDate));
        } else {
            propaDSST.setMasterMode(printStep, new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/CentralBody/"), new String("CentralBody_DSST_equinoxial"), PrintStepHandler.PrintEnum.EQUINOCIAL, format, initDate));
        }
        
        double dsstStart = System.currentTimeMillis();
        SpacecraftState finalStateDSST = propaDSST.propagate(initDate.shiftedBy(dt));
        double dsstEnd = System.currentTimeMillis();
        
        System.out.println("execution time DSST : " + (dsstEnd - dsstStart)/1000.);

        PVCoordinates pvd = finalStateDSST.getPVCoordinates();

        // Numerical Propagation
        if (meanOrbit) {
            setNumProp(osc);
        } else {
            setNumProp(orbitOsc);
        }
        numProp.addForceModel(nForce);
        if (printPV) {
            numProp.setMasterMode(printStep, new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/CentralBody/"), new String("CentralBody_NUM_PV"), PrintStepHandler.PrintEnum.PV, format, initDate));
        } else {
            numProp.setMasterMode(printStep, new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/CentralBody/"), new String("CentralBody_NUM_equinoxial"), PrintStepHandler.PrintEnum.EQUINOCIAL, format, initDate));
        }
        
        double NUMStart = System.currentTimeMillis();
        SpacecraftState finalStateNum = numProp.propagate(initDate.shiftedBy(dt));
        double NUMEnd = System.currentTimeMillis();

        System.out.println("execution time NUM : " + (NUMEnd - NUMStart)/1000.);

        PVCoordinates pvn = finalStateNum.getPVCoordinates();

        // System.out.println((pv.getPosition().getX() - pvd.getPosition().getX()) + " "
        // + (pv.getPosition().getX() - pvn.getPosition().getX()) + " "
        // + (pvn.getPosition().getX() - pvd.getPosition().getX()));
        // System.out.println((pv.getPosition().getY() - pvd.getPosition().getY()) + " "
        // + (pv.getPosition().getY() - pvn.getPosition().getY()) + " "
        // + (pvn.getPosition().getY() - pvd.getPosition().getY()));
        // System.out.println((pv.getPosition().getZ() - pvd.getPosition().getZ()) + " "
        // + (pv.getPosition().getZ() - pvn.getPosition().getZ()) + " "
        // + (pvn.getPosition().getZ() - pvd.getPosition().getZ()));
        // System.out.println((pv.getVelocity().getX() - pvd.getVelocity().getX()) + " "
        // + (pv.getVelocity().getX() - pvn.getVelocity().getX()) + " "
        // + (pvn.getVelocity().getX() - pvd.getVelocity().getX()));
        // System.out.println((pv.getVelocity().getY() - pvd.getVelocity().getY()) + " "
        // + (pv.getVelocity().getY() - pvn.getVelocity().getY()) + " "
        // + (pvn.getVelocity().getY() - pvd.getVelocity().getY()));
        // System.out.println((pv.getVelocity().getZ() - pvd.getVelocity().getZ()) + " "
        // + (pv.getVelocity().getZ() - pvn.getVelocity().getZ())+ " "
        // + (pvn.getVelocity().getZ() - pvd.getVelocity().getZ()));

//        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 1100.0);
//        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 9000.0);
//        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 20.0);
//        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 1.0);
//        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 0.1);
//        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 5.e-3);

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 1200.);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 11000.);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 23.);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 1.0);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 0.11);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 5.e-3);
    }

    @Test
    public void testPropagationWithThirdBody() throws OrekitException, IOException {
        SpacecraftState state = getGEOrbit();
        setDSSTProp(state);

        boolean printPV = true;

        // DSST Propagation with no force (reference Keplerian motion)
        final double dt = 864000;
        SpacecraftState finalState0 = propaDSST.propagate(initDate.shiftedBy(dt));
        PVCoordinates pv = finalState0.getPVCoordinates();

        // Third Body Force Model
        DSSTForceModel force = new DSSTThirdBody(CelestialBodyFactory.getMoon());
        ForceModel nForce = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(force);
        if (printPV) {
            propaDSST.setMasterMode(100., new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/ThirdBody/"), new String("ThirdBody_DSST_PV"), PrintStepHandler.PrintEnum.PV, format,initDate));
        } else {
            propaDSST.setMasterMode(100., new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/ThirdBody/"), new String("ThirdBody_DSST_equinoxial"), PrintStepHandler.PrintEnum.EQUINOCIAL, format,initDate));
        }

        SpacecraftState finalState1 = propaDSST.propagate(initDate.shiftedBy(dt));
        PVCoordinates pvd = finalState1.getPVCoordinates();

        // Numerical Propagation
        setNumProp(initialState);
        numProp.addForceModel(nForce);
        if (printPV) {
            numProp.setMasterMode(100., new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/ThirdBody/"), new String("ThirdBody_NUM_PV"), PrintStepHandler.PrintEnum.PV, format,initDate));
        } else {
            numProp.setMasterMode(100., new PrintStepHandler(new String("D:/rdicosta/EUMETSAT/DSSTvalidation/ThirdBody/"), new String("ThirdBody_NUM_equinoxial"), PrintStepHandler.PrintEnum.EQUINOCIAL, format,initDate));
        }
        SpacecraftState finalState2 = numProp.propagate(initDate.shiftedBy(dt));
        PVCoordinates pvn = finalState2.getPVCoordinates();
        //
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

        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 8000.0);
        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 30000.0);
        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 8000.0);
        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 3.0);
        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 1.0);
        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 5.e-3);

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 12000.);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 45000.);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 400.);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 3.0);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 1.0);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 1.e-2);
    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        // Propagation duration
        final double dt = 864000.;

        // DSST Propagation with no force (reference Keplerian motion)
        SpacecraftState finalState0 = propaDSST.propagate(initDate.shiftedBy(dt));
        PVCoordinates pv = finalState0.getPVCoordinates();

        // Drag Force Model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF2005());
        earth.setAngularThreshold(1.e-6);
        // PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        // Atmosphere atm = new HarrisPriester(sun, earth);
        Atmosphere atm = new SimpleExponentialAtmosphere(earth, 4.e-13, 500000.0, 60000.0);
        final double cd = 2.0;
        final double sf = 5.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, sf);
        ForceModel ndrag = new DragForce(atm, new SphericalSpacecraft(sf, cd, 0., 0.));

        // DSST Propagation
        propaDSST.resetInitialState(initialState);
        propaDSST.addForceModel(drag);
        PVCoordinates pvd = propaDSST.propagate(initDate.shiftedBy(dt)).getPVCoordinates();

        // Numerical Propagation
        setNumProp(initialState);
        numProp.addForceModel(ndrag);
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

        Assert.assertEquals(pv.getPosition().getX(), pvd.getPosition().getX(), 2000.0);
        Assert.assertEquals(pv.getPosition().getY(), pvd.getPosition().getY(), 400.0);
        Assert.assertEquals(pv.getPosition().getZ(), pvd.getPosition().getZ(), 5000.0);
        Assert.assertEquals(pv.getVelocity().getX(), pvd.getVelocity().getX(), 4.);
        Assert.assertEquals(pv.getVelocity().getY(), pvd.getVelocity().getY(), 3.);
        Assert.assertEquals(pv.getVelocity().getZ(), pvd.getVelocity().getZ(), 2.);

        Assert.assertEquals(pvn.getPosition().getX(), pvd.getPosition().getX(), 0.1);
        Assert.assertEquals(pvn.getPosition().getY(), pvd.getPosition().getY(), 0.01);
        Assert.assertEquals(pvn.getPosition().getZ(), pvd.getPosition().getZ(), 0.2);
        Assert.assertEquals(pvn.getVelocity().getX(), pvd.getVelocity().getX(), 2.e-4);
        Assert.assertEquals(pvn.getVelocity().getY(), pvd.getVelocity().getY(), 1.e-4);
        Assert.assertEquals(pvn.getVelocity().getZ(), pvd.getVelocity().getZ(), 1.e-4);
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
        double cR = 2. * (1. + (1. - kA) * (1. - kR) * 4. / 9.);
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
    public void testResetStateEvent() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        propaDSST.addEventDetector(new DateDetector(resetDate) {
            private static final long serialVersionUID = 6453983658076746705L;

            public EventDetector.Action eventOccurred(SpacecraftState s,
                                                      boolean increasing) throws OrekitException {
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

    private void setNumProp(SpacecraftState initialState) {
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialState.getOrbit(), initialState.getOrbit().getType());
        final double minStep = 10.;
        final double maxStep = 1000.;
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);
        numProp = new NumericalPropagator(integrator);
        numProp.setInitialState(initialState);
        // numProp.setMasterMode(300., new PrintStepHandler());
    }

    private void setDSSTProp(SpacecraftState initialState) throws PropagationException {
        this.initialState = initialState;
        this.initDate = initialState.getDate();
        final double minStep = 10000.;
        final double maxStep = 86400.;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(minStep);
        propaDSST = new DSSTPropagator(integrator, initialState.getOrbit());

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
    private static class PrintStepHandler implements OrekitFixedStepHandler {

        public enum PrintEnum {
            PV,
            EQUINOCIAL;
        }

        /**
         * Output format.
         */
        private final PrintEnum      printType;

        private final String         format;

        /**
         * Buffer
         */
        private final BufferedWriter buffer;

        /** Starting date */
        private AbsoluteDate dateIni;

        /** Serializable UID. */
        private static final long    serialVersionUID = -8909135870522456848L;

        private PrintStepHandler(final String outputPath,
                                 final String name,
                                 final PrintEnum printType,
                                 final String format,
                                 final AbsoluteDate initDate)
                                               throws IOException {
            this.buffer = new BufferedWriter(new FileWriter(outputPath + name));
            this.format = format;
            this.printType = printType;
            this.dateIni = initDate;
        }

        public void handleStep(SpacecraftState currentState,
                               boolean isLast) {
            final StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.ENGLISH);

            if (this.printType == PrintEnum.PV) {
                final Vector3D pos = currentState.getOrbit().getPVCoordinates().getPosition();
                final Vector3D vel = currentState.getOrbit().getPVCoordinates().getVelocity();
                final double px = pos.getX();
                final double py = pos.getY();
                final double pz = pos.getZ();
                final double vx = vel.getX();
                final double vy = vel.getY();
                final double vz = vel.getZ();
                formatter.format(this.format, currentState.getDate().durationFrom(dateIni)/86400d, px, py, pz, vx, vy, vz);
                try {
                    this.buffer.write(formatter.toString());
                    this.buffer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (this.printType == PrintEnum.EQUINOCIAL) {
                EquinoctialOrbit orb = new EquinoctialOrbit(currentState.getOrbit());
                formatter.format(this.format, orb.getDate().durationFrom(dateIni)/86400d, orb.getA(), orb.getEquinoctialEx(), orb.getEquinoctialEy(), orb.getHx(), orb.getHy(), orb.getLM());
                try {
                    this.buffer.write(formatter.toString());
                    this.buffer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (isLast) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
