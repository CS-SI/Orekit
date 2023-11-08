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
package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

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

    @Test
    @DisplayName("Test copy method")
    void testCopyMethod() {

        // Given
        final ODEIntegratorBuilder integratorBuilder = Mockito.mock(ODEIntegratorBuilder.class);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(),
                                               new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);

        final double               positionScale     = 1;
        final PropagationType      propagationType   = PropagationType.OSCULATING;
        final PropagationType      stateType         = PropagationType.OSCULATING;

        final DSSTPropagatorBuilder builder =
                new DSSTPropagatorBuilder(orbit, integratorBuilder, positionScale, propagationType, stateType);

        builder.addForceModel(Mockito.mock(DSSTForceModel.class));

        // When
        final DSSTPropagatorBuilder copyBuilder = builder.copy();

        // Then
        assertDSSTPropagatorBuilderIsACopy(builder, copyBuilder);

    }

    private void assertDSSTPropagatorBuilderIsACopy(final DSSTPropagatorBuilder expected,
                                                    final DSSTPropagatorBuilder actual) {
        assertPropagatorBuilderIsACopy(expected, actual);

        Assertions.assertEquals(expected.getIntegratorBuilder(), actual.getIntegratorBuilder());
        Assertions.assertEquals(expected.getPropagationType(), actual.getPropagationType());
        Assertions.assertEquals(expected.getStateType(), actual.getStateType());
        Assertions.assertEquals(expected.getMass(), actual.getMass());
        Assertions.assertEquals(expected.getAllForceModels(), actual.getAllForceModels());
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
        Assertions.assertEquals(orbitWithPropagator.getA(),             orbitWithBuilder.getA(), 1.e-1);
        Assertions.assertEquals(orbitWithPropagator.getEquinoctialEx(), orbitWithBuilder.getEquinoctialEx(), eps);
        Assertions.assertEquals(orbitWithPropagator.getEquinoctialEy(), orbitWithBuilder.getEquinoctialEy(), eps);
        Assertions.assertEquals(orbitWithPropagator.getHx(),            orbitWithBuilder.getHx(), eps);
        Assertions.assertEquals(orbitWithPropagator.getHy(),            orbitWithBuilder.getHy(), eps);
        Assertions.assertEquals(orbitWithPropagator.getLM(),            orbitWithBuilder.getLM(), 8.0e-10);

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
        // Verify that there is no Newtonian attraction force model
        Assertions.assertFalse(hasNewtonianAttraction(builder.getAllForceModels()));
        // Build the DSST propagator (not used here)
        builder.buildPropagator(builder.getSelectedNormalizedParameters());
        // Verify the addition of the Newtonian attraction force model
        Assertions.assertTrue(hasNewtonianAttraction(builder.getAllForceModels()));
        // Add a new force model to ensure the Newtonian attraction stay at the last position
        builder.addForceModel(sun);
        Assertions.assertTrue(hasNewtonianAttraction(builder.getAllForceModels()));
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
        builder.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {

            public String getName() {
                return "linear";
            }

            public int getDimension() {
                return 1;
            }

            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[] { 1.0 }, null);
            }

        });

        builder.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {

            public String getName() {
                return "linear";
            }

            public int getDimension() {
                return 1;
            }

            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[] { 1.0 }, null);
            }

        });

        try {
            // Build the propagator
            builder.buildPropagator(builder.getSelectedNormalizedParameters());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
    }

    @Test
    public void testDeselectOrbitals() {
        // Integrator builder
        final ODEIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        // Propagator builder
        final DSSTPropagatorBuilder builder = new DSSTPropagatorBuilder(orbit,
                                                                  dp54Builder,
                                                                  1.0,
                                                                  PropagationType.MEAN,
                                                                  PropagationType.MEAN);
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assertions.assertTrue(driver.isSelected());
        }
        builder.deselectDynamicParameters();
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            Assertions.assertFalse(driver.isSelected());
        }
    }

    @BeforeEach
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
                                     PositionAngleType.MEAN,
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
