/* Copyright 2002-2020 CS GROUP
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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.ThirdBodyAttractionEpoch;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.EpochDerivativesEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class MultipleShooterTest {

    private static final double eps = 1.0e-6;

    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** arbitrary inertial frame */
    private static final Frame eci = FramesFactory.getGCRF();

    /** unused propagator */
    private NumericalPropagator propagator;
    /** mock force model */
    private ForceModel forceModel;
    /** arbitrary PV */
    private PVCoordinates pv;
    /** arbitrary state */
    private SpacecraftState state;
    /** subject under test */
    private EpochDerivativesEquations pde;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        propagator = new NumericalPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new ThirdBodyAttractionEpoch(CelestialBodyFactory.getSun());
        propagator.addForceModel(forceModel);
        propagator.addForceModel(new NewtonianAttraction(Constants.WGS84_EARTH_MU));
        pde = new EpochDerivativesEquations("ede", propagator);
        Vector3D p = new Vector3D(7378137, 0, 0);
        Vector3D v = new Vector3D(0, 7500, 0);
        pv = new PVCoordinates(p, v);
        state = new SpacecraftState(new AbsolutePVCoordinates(eci,date,pv))
                .addAdditionalState("ede", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state);
    }

    @Test
    public void testMultipleShooting() {

        // Time settings
        // -------------
        final AbsoluteDate initialDate =
                        new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                         TimeScalesFactory.getUTC());
        final double arcDuration = 10000;
       
        final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(1.25E10, 1.450E11, -7.5E9),
                                                           new Vector3D(-30000.0, 2500.0, -3500.0));

        // Integration parameters
        // ---------------------------
        // Adaptive stepsize boundaries
        final double minStep = 1;
        final double maxstep = 30000;

        // Integrator tolerances
        final double positionTolerance = 1000;
        final double velocityTolerance = 1;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = {positionTolerance, positionTolerance, positionTolerance, velocityTolerance, velocityTolerance, velocityTolerance, massTolerance };
        final double[] vecRelativeTolerances =
                        new double[vecAbsoluteTolerances.length];

        // Integrator definition
        final AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(minStep, maxstep,
                                                       vecAbsoluteTolerances,
                                                       vecRelativeTolerances);

        // Load Celestial bodies
        // ---------------------
        final CelestialBody   sun     = CelestialBodyFactory.getSun();
        final CelestialBody   earth    = CelestialBodyFactory.getEarthMoonBarycenter();
        final Frame primaryFrame = sun.getInertiallyOrientedFrame();

        // Trajectory definition
        // ---------------------
        final int narcs = 5;
        final List<SpacecraftState> correctedList = new ArrayList<SpacecraftState>(narcs + 1);
        final AbsolutePVCoordinates firstGuessAPV = new AbsolutePVCoordinates(primaryFrame, initialDate, firstGuess);
        List<SpacecraftState> firstGuessList2 = generatePatchPointsEphemeris(sun, earth, firstGuessAPV, arcDuration, narcs, integrator);
        final List<NumericalPropagator> propagatorList  = initializePropagators(sun, earth, integrator, narcs);
        final List<AdditionalEquations> additionalEquations = addAdditionalEquations(propagatorList);

        for (int i = 0; i < narcs + 1; i++) {
            final SpacecraftState sp = firstGuessList2.get(i);
            correctedList.add(new SpacecraftState(sp.getAbsPVA(), sp.getAttitude()));
        }

        // Perturbation on a patch point
        // -----------------------------

        final int nP = 1; // Perturbated patch point
        final Vector3D deltaP = new Vector3D(-50000,1000,0);
        final Vector3D deltaV = new Vector3D(0.1,0,1.0);
        final double deltaEpoch = 1000;

        final SpacecraftState firstGuessSP = correctedList.get(nP);
        final AttitudeProvider attPro = propagatorList.get(nP).getAttitudeProvider();

        // Small change of the a patch point
        final Vector3D newPos = firstGuessSP.getAbsPVA().getPosition().add(deltaP); 
        final Vector3D newVel = firstGuessSP.getAbsPVA().getVelocity().add(deltaV);
        final AbsoluteDate newDate = firstGuessSP.getDate().shiftedBy(deltaEpoch);
        AbsolutePVCoordinates absPva = new AbsolutePVCoordinates(firstGuessSP.getFrame(), newDate, newPos, newVel);
        final Attitude attitude = attPro.getAttitude(absPva, newDate, absPva.getFrame());
        SpacecraftState newSP = new SpacecraftState(absPva , attitude);
        correctedList.set(1, newSP);

        final double tolerance = 1.0;

        MultipleShooter multipleShooting = new MultipleShooter(correctedList, propagatorList, additionalEquations, arcDuration, tolerance);
        multipleShooting.setPatchPointComponentFreedom(1, 0, false);
        multipleShooting.setPatchPointComponentFreedom(1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(1, 2, false);
        multipleShooting.setPatchPointComponentFreedom(1, 3, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 0, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 2, false);
        multipleShooting.setEpochFreedom(1, false);

        multipleShooting.compute();

        // Verify
        Assert.assertEquals(0.0,        Vector3D.distance(firstGuessList2.get(0).getAbsPVA().getPosition(), correctedList.get(0).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.018029,   Vector3D.distance(firstGuessList2.get(0).getAbsPVA().getVelocity(), correctedList.get(0).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(677.097822, Vector3D.distance(firstGuessList2.get(1).getAbsPVA().getPosition(), correctedList.get(1).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.017816,   Vector3D.distance(firstGuessList2.get(1).getAbsPVA().getVelocity(), correctedList.get(1).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(863.676399, Vector3D.distance(firstGuessList2.get(2).getAbsPVA().getPosition(), correctedList.get(2).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.092103,   Vector3D.distance(firstGuessList2.get(2).getAbsPVA().getVelocity(), correctedList.get(2).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(576.396708, Vector3D.distance(firstGuessList2.get(3).getAbsPVA().getPosition(), correctedList.get(3).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.092099,   Vector3D.distance(firstGuessList2.get(3).getAbsPVA().getVelocity(), correctedList.get(3).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(288.575119, Vector3D.distance(firstGuessList2.get(4).getAbsPVA().getPosition(), correctedList.get(4).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.092113,   Vector3D.distance(firstGuessList2.get(4).getAbsPVA().getVelocity(), correctedList.get(4).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(0.000000,   Vector3D.distance(firstGuessList2.get(5).getAbsPVA().getPosition(), correctedList.get(5).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.092129,   Vector3D.distance(firstGuessList2.get(5).getAbsPVA().getVelocity(), correctedList.get(5).getAbsPVA().getVelocity()), eps);

    }

    @Test
    public void testMultipleShootingWithEstimatedParameters() {

        // Time settings
        // -------------
        final AbsoluteDate initialDate =
                        new AbsoluteDate(2000, 01, 01, 0, 0, 00.000,
                                         TimeScalesFactory.getUTC());
        final double arcDuration = 10000;
       
        final PVCoordinates firstGuess = new PVCoordinates(new Vector3D(1.25E10, 1.450E11, -7.5E9),
                                                           new Vector3D(-30000.0, 2500.0, -3500.0));

        // Integration parameters
        // ---------------------------
        // Adaptive stepsize boundaries
        final double minStep = 1;
        final double maxstep = 30000;

        // Integrator tolerances
        final double positionTolerance = 1000;
        final double velocityTolerance = 1;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances = {positionTolerance, positionTolerance, positionTolerance, velocityTolerance, velocityTolerance, velocityTolerance, massTolerance };
        final double[] vecRelativeTolerances =
                        new double[vecAbsoluteTolerances.length];

        // Integrator definition
        final AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(minStep, maxstep,
                                                       vecAbsoluteTolerances,
                                                       vecRelativeTolerances);

        // Load Celestial bodies
        // ---------------------
        final CelestialBody   sun     = CelestialBodyFactory.getSun();
        final CelestialBody   earth    = CelestialBodyFactory.getEarthMoonBarycenter();
        final Frame primaryFrame = sun.getInertiallyOrientedFrame();

        // Trajectory definition
        // ---------------------
        final int narcs = 5;
        final List<SpacecraftState> correctedList = new ArrayList<SpacecraftState>(narcs + 1);
        final AbsolutePVCoordinates firstGuessAPV = new AbsolutePVCoordinates(primaryFrame, initialDate, firstGuess);
        List<SpacecraftState> firstGuessList2 = generatePatchPointsEphemeris(sun, earth, firstGuessAPV, arcDuration, narcs, integrator);
        final List<NumericalPropagator> propagatorList  = initializePropagatorsWithEstimated(sun, earth, integrator, narcs);
        final List<AdditionalEquations> additionalEquations = addAdditionalEquations(propagatorList);

        for (int i = 0; i < narcs + 1; i++) {
            final SpacecraftState sp = firstGuessList2.get(i);
            correctedList.add(new SpacecraftState(sp.getAbsPVA(), sp.getAttitude()));
        }

        // Perturbation on a patch point
        // -----------------------------

        final int nP = 1; // Perturbated patch point
        final Vector3D deltaP = new Vector3D(-50000,1000,0);
        final Vector3D deltaV = new Vector3D(0.1,0,1.0);
        final double deltaEpoch = 1000;

        final SpacecraftState firstGuessSP = correctedList.get(nP);
        final AttitudeProvider attPro = propagatorList.get(nP).getAttitudeProvider();

        // Small change of the a patch point
        final Vector3D newPos = firstGuessSP.getAbsPVA().getPosition().add(deltaP); 
        final Vector3D newVel = firstGuessSP.getAbsPVA().getVelocity().add(deltaV);
        final AbsoluteDate newDate = firstGuessSP.getDate().shiftedBy(deltaEpoch);
        AbsolutePVCoordinates absPva = new AbsolutePVCoordinates(firstGuessSP.getFrame(), newDate, newPos, newVel);
        final Attitude attitude = attPro.getAttitude(absPva, newDate, absPva.getFrame());
        SpacecraftState newSP = new SpacecraftState(absPva , attitude);
        correctedList.set(1, newSP);

        final double tolerance = 1.0;

        MultipleShooter multipleShooting = new MultipleShooter(correctedList, propagatorList, additionalEquations, arcDuration, tolerance);
        multipleShooting.setPatchPointComponentFreedom(1, 0, false);
        multipleShooting.setPatchPointComponentFreedom(1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(1, 2, false);
        multipleShooting.setPatchPointComponentFreedom(1, 3, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 0, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 1, false);
        multipleShooting.setPatchPointComponentFreedom(narcs + 1, 2, false);
        multipleShooting.setEpochFreedom(1, false);

        multipleShooting.compute();

        // Verify
        Assert.assertEquals(0.0,        Vector3D.distance(firstGuessList2.get(0).getAbsPVA().getPosition(), correctedList.get(0).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.007568,   Vector3D.distance(firstGuessList2.get(0).getAbsPVA().getVelocity(), correctedList.get(0).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(231.922890, Vector3D.distance(firstGuessList2.get(1).getAbsPVA().getPosition(), correctedList.get(1).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.007547,   Vector3D.distance(firstGuessList2.get(1).getAbsPVA().getVelocity(), correctedList.get(1).getAbsPVA().getVelocity()), eps);
        Assert.assertEquals(233.233939, Vector3D.distance(firstGuessList2.get(2).getAbsPVA().getPosition(), correctedList.get(2).getAbsPVA().getPosition()), eps);
        Assert.assertEquals(0.028078,   Vector3D.distance(firstGuessList2.get(2).getAbsPVA().getVelocity(), correctedList.get(2).getAbsPVA().getVelocity()), eps);
    }

    @Test(expected=OrekitException.class)
    public void testNotInitialized() {
        new EpochDerivativesEquations("partials", propagator).getMapper();
    }

    @Test(expected=OrekitException.class)
    public void testTooSmallDimension() {
        final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(state, new double[5][6], new double[6][2]);
    }

    @Test(expected=OrekitException.class)
    public void testTooLargeDimension() {
        final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(state, new double[8][6], new double[6][2]);
    }

    @Test(expected=OrekitException.class)
    public void testMismatchedDimensions() {
        final EpochDerivativesEquations partials = new EpochDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(state, new double[6][6], new double[7][2]);
    }

    private static List<SpacecraftState> generatePatchPointsEphemeris(final CelestialBody primary, final CelestialBody secondary,
                                                                      final AbsolutePVCoordinates initialAPV, final double arcDuration,
                                                                      final int narcs, final ODEIntegrator integrator) {

        List<SpacecraftState> firstGuessList = new ArrayList<SpacecraftState>(narcs+1);

        final double integrationTime = arcDuration;

        // Creating the initial spacecraftstate that will be given to the propagator

        final SpacecraftState initialState2 = new SpacecraftState(initialAPV);

        firstGuessList.add(initialState2);

        NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.addForceModel(new NewtonianAttraction(primary.getGM()));
        propagator.addForceModel(new ThirdBodyAttraction(secondary));
        propagator.setOrbitType(null);


        propagator.setInitialState(initialState2);

        AbsoluteDate currentDate = initialAPV.getDate();
        for(int i = 0; i < narcs; i++) {

            currentDate = currentDate.shiftedBy(integrationTime);
            final SpacecraftState sp = propagator.propagate(currentDate);

            firstGuessList.add(sp);
        }
        return firstGuessList;
    }

    private static List<NumericalPropagator> initializePropagators(final CelestialBody primary, final CelestialBody secondary, final ODEIntegrator integrator,
                                                                   final int propagationNumber) {
        final List<NumericalPropagator> propagatorList = new ArrayList<NumericalPropagator>(propagationNumber);

        // Definition of the propagator
        for(int i = 0; i < propagationNumber; i++) {

            NumericalPropagator propagator = new NumericalPropagator(integrator);

            propagator.addForceModel(new NewtonianAttraction(primary.getGM()));
            propagator.addForceModel(new ThirdBodyAttractionEpoch(secondary));

            propagator.setOrbitType(null);
            propagatorList.add(propagator);
        }        
        return propagatorList;
    }

    private static List<NumericalPropagator> initializePropagatorsWithEstimated(final CelestialBody primary, final CelestialBody secondary, final ODEIntegrator integrator,
                                                                                final int propagationNumber) {
        final List<NumericalPropagator> propagatorList = new ArrayList<NumericalPropagator>(propagationNumber);

        // Definition of the propagator
        for(int i = 0; i < propagationNumber; i++) {

            NumericalPropagator propagator = new NumericalPropagator(integrator);

            propagator.addForceModel(new NewtonianAttraction(primary.getGM()));
            final ForceModel model = new ThirdBodyAttractionEpoch(secondary);
            model.getParametersDrivers()[0].setSelected(true);
            propagator.addForceModel(model);

            propagator.setOrbitType(null);
            propagatorList.add(propagator);
        }        
        return propagatorList;
    }

    private static List<AdditionalEquations> addAdditionalEquations(List<NumericalPropagator> propagatorList){
        final int narcs = propagatorList.size();
        final List<AdditionalEquations> additionalEquations = new ArrayList<AdditionalEquations>(narcs) ;
        for(int i = 0; i < narcs; i++) {
            additionalEquations.add(new EpochDerivativesEquations("derivatives", propagatorList.get(i)));
        }
        return additionalEquations;

    }

}
