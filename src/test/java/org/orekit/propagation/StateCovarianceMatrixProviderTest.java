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
package org.orekit.propagation;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTHarvester;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTJ2SquaredClosedForm;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.forces.ZeisModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class StateCovarianceMatrixProviderTest {

    /** Initial S/C state. */
    private SpacecraftState initialState;
    
    /** Initial covariance. */
    private RealMatrix  initCov;
    
    /** Reference covariance after 60s prop with a J2 numerical model (computed with another solution). */
    private RealMatrix  refCovAfter60s;
    

    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
        Orbit initialOrbit = new CartesianOrbit(
                new PVCoordinates(new Vector3D(7526993.581890527, -9646310.10026971, 1464110.4928112086),
                                  new Vector3D(3033.79456099698, 1715.265069098717, -4447.658745923895)),
                FramesFactory.getEME2000(),
                new AbsoluteDate("2016-02-13T16:00:00.000", TimeScalesFactory.getUTC()),
                Constants.WGS84_EARTH_MU);
        initialState = new SpacecraftState(initialOrbit);
        initCov = MatrixUtils.createRealMatrix( new double[][] {
            { 8.651816029e+01, 5.689987127e+01, -2.763870764e+01, -2.435617201e-02, 2.058274137e-02, -5.872883051e-03 },
            { 5.689987127e+01, 7.070624321e+01, 1.367120909e+01, -6.112622013e-03, 7.623626008e-03, -1.239413190e-02 },
            { -2.763870764e+01, 1.367120909e+01, 1.811858898e+02, 3.143798992e-02, -4.963106559e-02, -7.420114385e-04 },
            { -2.435617201e-02, -6.112622013e-03, 3.143798992e-02, 4.657077389e-05, 1.469943634e-05, 3.328475593e-05 },
            { 2.058274137e-02, 7.623626008e-03, -4.963106559e-02, 1.469943634e-05, 3.950715934e-05, 2.516044258e-05 },
            { -5.872883051e-03, -1.239413190e-02, -7.420114385e-04, 3.328475593e-05, 2.516044258e-05, 3.547466120e-05 }
        });
        
        // Reference covariance after 60s prop with a J2 numerical model (computed with another solution)
        refCovAfter60s = MatrixUtils.createRealMatrix( new double[][] {
            { 5.770543135e+02, 2.316979550e+02, -5.172369105e+02, -2.585893247e-01, 2.113809017e-01, -1.759509343e-01 },
            { 2.316979550e+02, 1.182942930e+02, -1.788422178e+02, -9.570305681e-02, 7.792155309e-02, -7.435822327e-02 },
            { -5.172369105e+02, -1.788422178e+02, 6.996248500e+02, 2.633605389e-01, -2.480144888e-01, 1.908427233e-01 },
            { -2.585893247e-01, -9.570305681e-02, 2.633605389e-01, 1.419148897e-04, -8.715858320e-05, 1.024944399e-04 },
            { 2.113809017e-01, 7.792155309e-02, -2.480144888e-01, -8.715858320e-05, 1.069566588e-04, -5.667563856e-05 },
            { -1.759509343e-01, -7.435822327e-02, 1.908427233e-01, 1.024944399e-04, -5.667563856e-05, 8.178356868e-05 }
        });
    }

    /**
     * Absolute comparison of two covariance matrices.
     *
     * @param reference reference covariance
     * @param computed  computed covariance
     * @param threshold threshold for comparison
     */
    private void compareCovariance(final RealMatrix reference, final RealMatrix computed, final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }
    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithNumericalPropagatorCartesian() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final OrbitType           propType   = OrbitType.CARTESIAN;
        final PositionAngleType angleType  = PositionAngleType.MEAN;
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        
        // Finalize setting
        propagator.setOrbitType(propType);
        propagator.setPositionAngleType(angleType);
        propagator.setInitialState(initialState);

        // Configure covariance propagation
        final StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);
        

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = propagatedStateCov.getMatrix();

        // Verify
        compareCovariance(refCovAfter60s, propagatedCov, 4.0e-7);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());
        Assertions.assertEquals(OrbitType.CARTESIAN, propagatedStateCov.getOrbitType());
        Assertions.assertNull(propagatedStateCov.getLOF());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB).getMatrix();

        // Second transformation
        RealMatrix transformedCovB =
        		propagatedStateCov.changeCovarianceFrame(propagated.getOrbit(), frameB).getMatrix();

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        ///////////
        // Test the orbit type transformation
        ///////////

        // Define a new output frame
        final OrbitType     outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngleType outAngleType = PositionAngleType.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType).getMatrix();

        // Second transformation
        RealMatrix transformedCovD =
        		propagatedStateCov.changeCovarianceType(propagated.getOrbit(), outOrbitType, outAngleType).getMatrix();

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements. The difference here is that the propagator uses its
     * default orbit type: EQUINOCTIAL
     */
    @Test
    public void testWithNumericalPropagatorDefault() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        propagator.setInitialState(initialState);

        // Configure covariance propagation
        final StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = propagatedStateCov.getMatrix();

        // Verify
        compareCovariance(refCovAfter60s, propagatedCov, 3.0e-5);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB).getMatrix();

        // Second transformation
        RealMatrix transformedCovB =
        		propagatedStateCov.changeCovarianceFrame(propagated.getOrbit(), frameB).getMatrix();

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        ///////////
        // Test the orbit type transformation
        ///////////

        // Define a new output frame
        final OrbitType     outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngleType outAngleType = PositionAngleType.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType).getMatrix();

        // Second transformation
        RealMatrix transformedCovD =
        		propagatedStateCov.changeCovarianceType(propagated.getOrbit(), outOrbitType, outAngleType).getMatrix();

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Keplerian elements. The difference here is that the propagator uses its
     * default orbit type: EQUINOCTIAL.
     * <p>
     * The additional purpose of this test is to make sure that the propagated state covariance is expressed in the right
     * orbit type.
     */
    @Test
    public void testWithNumericalPropagatorDefaultAndKeplerianOrbitType() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final String              stmName    = "STM";
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final String     additionalName = "cartCov";
        final StateCovariance initialStateCovariance = new StateCovariance(initCov, initialState.getDate(), initialState.getFrame(), OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final StateCovariance initialStateCovarianceInKep = initialStateCovariance.changeCovarianceType(initialState.getOrbit(), OrbitType.KEPLERIAN, PositionAngleType.MEAN);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester, initialStateCovarianceInKep);
        propagator.setInitialState(initialState);
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final StateCovariance propagatedStateCovInCart = propagatedStateCov.changeCovarianceType(propagated.getOrbit(), OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final RealMatrix propagatedCovInCart = propagatedStateCovInCart.getMatrix();

        // Verify
        compareCovariance(refCovAfter60s, propagatedCovInCart, 3.0e-5);
        Assertions.assertEquals(OrbitType.KEPLERIAN, provider.getCovarianceOrbitType());
        Assertions.assertEquals(OrbitType.KEPLERIAN, propagatedStateCov.getOrbitType());

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithAnalyticalPropagator() {

        // Initialization
        setUp();

        // Numerical propagator
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialState.getOrbit(),
                                                                                   GravityFieldFactory.getUnnormalizedProvider(6, 0));

        // Configure covariance propagation
        final StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = propagatedStateCov.getMatrix();

        // Verify
        compareCovariance(refCovAfter60s, propagatedCov, 5.0e-4);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB).getMatrix();

        // Second transformation
        RealMatrix transformedCovB =
        		propagatedStateCov.changeCovarianceFrame(propagated.getOrbit(), frameB).getMatrix();

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        ///////////
        // Test the orbit type transformation
        ///////////

        // Define a new output frame
        final OrbitType     outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngleType outAngleType = PositionAngleType.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType).getMatrix();

        // Second transformation
        RealMatrix transformedCovD =
        		propagatedStateCov.changeCovarianceType(propagated.getOrbit(), outOrbitType, outAngleType).getMatrix();

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation with DSST propagator.
     */
    @Test
    public void testWithDSSTPropagatorDefault() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 3600.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // DSST propagator
        final String         stmName    = "STM";
        final DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        // Add a force model
        final UnnormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal = new DSSTZonal(gravity);
        propagator.addForceModel(zonal);
        propagator.addForceModel(new DSSTJ2SquaredClosedForm(new ZeisModel(), gravity));
        // Finalize setting
        final DSSTHarvester harvester = (DSSTHarvester) propagator.setupMatricesComputation(stmName, null, null);
        harvester.initializeFieldShortPeriodTerms(DSSTPropagator.computeMeanState(initialState, propagator.getAttitudeProvider(), Arrays.asList(zonal)));

        // Create additional state
        final String     additionalName = "cartCov";
        final StateCovariance initialStateCovariance = new StateCovariance(initCov, initialState.getDate(), initialState.getFrame(), OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester, initialStateCovariance);
        propagator.setInitialState(initialState);
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = propagatedStateCov.getMatrix();

        // Verify (3% error with respect to reference)
        compareCovariance(refCovAfter60s, propagatedCov, 0.03);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());
    }

    /**
     * Unit test for shiftedBy() method.
     * The method is compared to covariance propagation using the Keplerian propagator.
     */
    @Test
    public void testCovarianceShift() {

        // Initialization
        setUp();

        // Keplerian propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialState.getOrbit());
        final double dt = 60.0;

        // Configure covariance propagation
        final StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(dt));

        // Get the propagated covariance
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = propagatedStateCov.getMatrix();

        // Use of shiftedBy
        final StateCovariance initialStateCovariance = new StateCovariance(initCov, initialState.getDate(), initialState.getFrame(), OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final StateCovariance shiftedStateCov = initialStateCovariance.shiftedBy(initialState.getOrbit(), dt);
        final RealMatrix shiftedCov = shiftedStateCov.getMatrix();

        // Verify
        compareCovariance(propagatedCov, shiftedCov, 4.0e-12);
        Assertions.assertEquals(propagatedStateCov.getDate(), shiftedStateCov.getDate());
        Assertions.assertEquals(propagatedStateCov.getOrbitType(), shiftedStateCov.getOrbitType());
        Assertions.assertEquals(propagatedStateCov.getPositionAngleType(), shiftedStateCov.getPositionAngleType());
    }

    /**
     * Unit test for issue 1253: incorrect covariance propagation with a bounded propagator.
     *
     * <p>Bug detection courtesy of Christophe Le Bris
     *
     * <p>The problem was that, when using ephemeris mode with an integrated propagator, the covariance propagated with the
     * output BoundedPropagator was wrong. Actually, it was always equal to the initial covariance matrix.
     */
    @Test
    public void testIssue1253_IntegratedPropagator() {

        // GIVEN
        // -----
        
        // Initialization
        setUp();

        // dt
        final double dt = 3600.0;
        final AbsoluteDate propDate = initialState.getDate().shiftedBy(dt);

        // Numerical propagator
        final OrbitType           propType   = OrbitType.CARTESIAN;
        final PositionAngleType angleType  = PositionAngleType.MEAN;
        NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(60.));
        propagator.setOrbitType(propType);
        propagator.setPositionAngleType(angleType);
        propagator.setInitialState(initialState);
        StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);

        // Propagate to dt/2
        SpacecraftState propagated = propagator.propagate(propDate);

        // Get the reference propagated covariance
        final StateCovariance refPropagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix refPropagatedCov = refPropagatedStateCov.getMatrix();
        
        // WHEN
        // ----
        
        // 2. Ephemeris on [t0, t0 + dt]
        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(60.));
        propagator.setOrbitType(propType);
        propagator.setPositionAngleType(angleType);
        propagator.setInitialState(initialState);
        provider = setUpCovariancePropagation(propagator);

        // Propagate to dt and store ephemeris
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(propDate);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        
        // 3. Propagate ephemeris to dt
        propagated = ephemeris.propagate(propDate);
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = provider.getStateCovariance(propagated).getMatrix();
        
        
        // THEN
        // ----

        // Verify that both covariances are equal
        Assertions.assertEquals(refPropagatedStateCov.getDate(), propagatedStateCov.getDate());
        Assertions.assertEquals(refPropagatedStateCov.getOrbitType(), propagatedStateCov.getOrbitType());
        Assertions.assertEquals(refPropagatedStateCov.getPositionAngleType(), propagatedStateCov.getPositionAngleType());
        compareCovariance(refPropagatedCov, propagatedCov, 0.);
    }

    /**
     * Unit test for issue 1253: incorrect covariance propagation with a bounded propagator.
     * <p>
     * With analytical propagators, the former code worked. Test if it still works with the new version of the code.
     */
    @Test
    public void testIssue1253_AnalyticalPropagator() {

        // GIVEN
        // -----
        
        // Initialization
        setUp();

        // dt
        final double dt = 3600.0;
        final AbsoluteDate propDate = initialState.getDate().shiftedBy(dt);

        // Keplerian propagator
        Propagator propagator = new KeplerianPropagator(initialState.getOrbit());
        StateCovarianceMatrixProvider provider = setUpCovariancePropagation(propagator);

        // Propagate to dt/2
        SpacecraftState propagated = propagator.propagate(propDate);

        // Get the reference propagated covariance
        final StateCovariance refPropagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix refPropagatedCov = refPropagatedStateCov.getMatrix();
        
        // WHEN
        // ----
        
        // 2. Ephemeris on [t0, t0 + dt]
        propagator = new KeplerianPropagator(initialState.getOrbit());
        provider = setUpCovariancePropagation(propagator);

        // Propagate to dt and store ephemeris
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(propDate);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        
        // 3. Propagate ephemeris to dt
        propagated = ephemeris.propagate(propDate);
        final StateCovariance propagatedStateCov = provider.getStateCovariance(propagated);
        final RealMatrix propagatedCov = provider.getStateCovariance(propagated).getMatrix();
        
        
        // THEN
        // ----

        // Verify that both covariances are equal
        Assertions.assertEquals(refPropagatedStateCov.getDate(), propagatedStateCov.getDate());
        Assertions.assertEquals(refPropagatedStateCov.getOrbitType(), propagatedStateCov.getOrbitType());
        Assertions.assertEquals(refPropagatedStateCov.getPositionAngleType(), propagatedStateCov.getPositionAngleType());
        compareCovariance(refPropagatedCov, propagatedCov, 0.);
    }
    
    /** Setup a StateCovarianceMatrixProvider without changing orbit type.
     * 
     * @param propagator input propagator
     * @return configured StateCovarianceMatrixProvider
     */
    private StateCovarianceMatrixProvider setUpCovariancePropagation(final Propagator propagator) {
        
        final String stmName = "STM";
        final String     additionalName = "cartCov";
        
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final StateCovariance initialStateCovariance = new StateCovariance(initCov, initialState.getDate(), initialState.getFrame(), OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester, initialStateCovariance);
        propagator.addAdditionalStateProvider(provider);
        
        return provider;
    }

}
