/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.conversion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class DSSTPropagatorBuilderTest {

    private static final double eps  = 2.0e-10;

    private double minStep;
    private double maxStep;
    private double dP;
    private double[][] tolerance;

    private AbsoluteDate initDate;
    private EquinoctialOrbit orbit;
    private DSSTPropagator propagator;
    private DSSTForceModel moon;
    private DSSTForceModel sun;

    @Test
    public void testIntegrators01() {

        ODEIntegratorBuilder abBuilder = new AdamsBashforthIntegratorBuilder(2, minStep, maxStep, dP);
        doTestBuildPropagator(abBuilder);
    }

    @Test
    public void testIntegrators02() {

        ODEIntegratorBuilder amBuilder = new AdamsMoultonIntegratorBuilder(2, minStep, maxStep, dP);
        doTestBuildPropagator(amBuilder);
    }

    @Test
    public void testIntegrators03() {

        final double stepSize = 100.;

        ODEIntegratorBuilder crkBuilder = new ClassicalRungeKuttaIntegratorBuilder(stepSize);
        doTestBuildPropagator(crkBuilder);
    }

    @Test
    public void testIntegrators04() {

        final double stepSize = 100.;

        ODEIntegratorBuilder lBuilder = new LutherIntegratorBuilder(stepSize);
        doTestBuildPropagator(lBuilder);
    }

    @Test
    public void testIntegrators05() {

        ODEIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        doTestBuildPropagator(dp54Builder);
    }

    @Test
    public void testIntegrators06() {

        final double stepSize = 100.;

        ODEIntegratorBuilder eBuilder = new EulerIntegratorBuilder(stepSize);
        doTestBuildPropagator(eBuilder);
    }

    @Test
    public void testIntegrators07() {

        final double stepSize = 100.;

        ODEIntegratorBuilder gBuilder = new GillIntegratorBuilder(stepSize);
        doTestBuildPropagator(gBuilder);
    }

    @Test
    public void testIntegrators08() {

        ODEIntegratorBuilder gbsBuilder = new GraggBulirschStoerIntegratorBuilder(minStep, maxStep, dP);
        doTestBuildPropagator(gbsBuilder);
    }

    @Test
    public void testIntegrators09() {

        ODEIntegratorBuilder hh54Builder = new HighamHall54IntegratorBuilder(minStep, maxStep, dP);
        doTestBuildPropagator(hh54Builder);
    }

    @Test
    public void testIntegrators10() {

        final double stepSize = 100.;

        ODEIntegratorBuilder mBuilder = new MidpointIntegratorBuilder(stepSize);
        doTestBuildPropagator(mBuilder);
    }

    @Test
    public void testIntegrators11() {

        final double stepSize = 100.;

        ODEIntegratorBuilder teBuilder = new ThreeEighthesIntegratorBuilder(stepSize);
        doTestBuildPropagator(teBuilder);
    }

    private void doTestBuildPropagator(final ODEIntegratorBuilder foiBuilder) {
        
        // We propagate using directly the propagator of the set up
        final Orbit orbitWithPropagator = propagator.propagate(initDate.shiftedBy(600)).getOrbit();
        
        // We propagate using a build version of the propagator
        // We shall have the same results than before
        DSSTPropagatorBuilder builder = new DSSTPropagatorBuilder(orbit,
                                                                  foiBuilder,
                                                                  1.0,
                                                                  PropagationType.MEAN,
                                                                  PropagationType.MEAN);
        
        builder.addForceModel(moon);
        builder.setMass(1000.);
        
        final DSSTPropagator prop = builder.buildPropagator(builder.getSelectedNormalizedParameters());
        
        final Orbit orbitWithBuilder = prop.propagate(initDate.shiftedBy(600)).getOrbit();
        
        // Verify
        Assert.assertEquals(orbitWithPropagator.getA(),             orbitWithBuilder.getA(), 1.e-1);
        Assert.assertEquals(orbitWithPropagator.getEquinoctialEx(), orbitWithBuilder.getEquinoctialEx(), eps);
        Assert.assertEquals(orbitWithPropagator.getEquinoctialEy(), orbitWithBuilder.getEquinoctialEy(), eps);
        Assert.assertEquals(orbitWithPropagator.getHx(),            orbitWithBuilder.getHx(), eps);
        Assert.assertEquals(orbitWithPropagator.getHy(),            orbitWithBuilder.getHy(), eps);
        Assert.assertEquals(orbitWithPropagator.getLM(),            orbitWithBuilder.getLM(), 8.0e-10);
        
    }

    @Test
    public void testIssue598() {
        // Integrator builder
        final ODEIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        // Propagator builder
        final DSSTPropagatorBuilder builder = new DSSTPropagatorBuilder(orbit,
                                                                  dp54Builder,
                                                                  1.0,
                                                                  PropagationType.MEAN,
                                                                  PropagationType.MEAN);
        builder.addForceModel(moon);
        // Verify that there is no Newtonian attration force model
        assertFalse(hasNewtonianAttraction(builder.getAllForceModels()));
        // Build the DSST propagator (not used here)
        builder.buildPropagator(builder.getSelectedNormalizedParameters());
        // Verify the addition of the Newtonian attraction force model
        assertTrue(hasNewtonianAttraction(builder.getAllForceModels()));
        // Add a new force model to ensure the Newtonian attraction stay at the last position
        builder.addForceModel(sun);
        assertTrue(hasNewtonianAttraction(builder.getAllForceModels()));
    }

    @Test
    public void testAdditionalEquations() {
        // Integrator builder
        final ODEIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        // Propagator builder
        final DSSTPropagatorBuilder builder = new DSSTPropagatorBuilder(orbit,
                                                                  dp54Builder,
                                                                  1.0,
                                                                  PropagationType.MEAN,
                                                                  PropagationType.MEAN);
        builder.addForceModel(moon);
        builder.addForceModel(sun);

        // Add additional equations
        builder.addAdditionalEquations(new AdditionalEquations() {

            public String getName() {
                return "linear";
            }

            public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
                pDot[0] = 1.0;
                return new double[7];
            }
        });

        builder.addAdditionalEquations(new AdditionalEquations() {

    	    public String getName() {
    	        return "linear";
    	    }

    	    public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
    	        pDot[0] = 1.0;
    	        return new double[7];
    	    }
        });

        try {
    	    // Build the propagator
    	    builder.buildPropagator(builder.getSelectedNormalizedParameters());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
    }
    
    @Before
    public void setUp() throws IOException, ParseException {
        
        Utils.setDataRoot("regular-data");

        minStep = 1.0;
        maxStep = 600.0;
        dP      = 10.0;

        final Frame earthFrame = FramesFactory.getEME2000();
        initDate = new AbsoluteDate(2003, 07, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        
        final double mu = 3.986004415E14;
        // a    = 42163393.0 m
        // ex =  -0.25925449177598586
        // ey =  -0.06946703170551687
        // hx =   0.15995912655021305
        // hy =  -0.5969755874197339
        // lM   = 15.47576793123677 rad
        orbit = new EquinoctialOrbit(4.2163393E7,
                                     -0.25925449177598586,
                                     -0.06946703170551687,
                                     0.15995912655021305,
                                     -0.5969755874197339,
                                     15.47576793123677,
                                     PositionAngle.MEAN,
                                     earthFrame,
                                     initDate,
                                     mu);

        moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), mu);
        sun = new DSSTThirdBody(CelestialBodyFactory.getSun(), mu);

        tolerance  = NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
        propagator = new DSSTPropagator(new DormandPrince853Integrator(minStep, maxStep, tolerance[0], tolerance[1]));
        propagator.setInitialState(new SpacecraftState(orbit, 1000.), PropagationType.MEAN);
        propagator.addForceModel(moon);

    }

    private boolean hasNewtonianAttraction(final List<DSSTForceModel> forceModels) {
        final int last = forceModels.size() - 1;
        return last >= 0 && forceModels.get(last) instanceof DSSTNewtonianAttraction;
    }

}
