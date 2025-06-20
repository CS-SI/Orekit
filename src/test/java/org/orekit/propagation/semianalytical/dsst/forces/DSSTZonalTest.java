/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

class DSSTZonalTest {

    /** Test mean elements rates computation.
     * 
     * First without setting the body-fixed frame, then by setting it to the inertial propagation frame.
     * Both should give the same results.
     */
    @Test
    void testGetMeanElementRate() {
        doTestGetMeanElementRate(false);
        doTestGetMeanElementRate(true);
    }

    private void doTestGetMeanElementRate(final boolean testIssue1104) {

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);

        final Frame earthFrame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 26559890 m
        // ex = 2.719455286199036E-4
        // ey = 0.0041543085910249414
        // hx = -0.3412974060023717
        // hy = 0.3960084733107685
        // lM = 8.566537840341699 rad
        final Orbit orbit = new EquinoctialOrbit(2.655989E7,
                                                 2.719455286199036E-4,
                                                 0.0041543085910249414,
                                                 -0.3412974060023717,
                                                 0.3960084733107685,
                                                 8.566537840341699,
                                                 PositionAngleType.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 3.986004415E14);

        final SpacecraftState state = new SpacecraftState(orbit).withMass(1000.0);

        final DSSTForceModel zonal;
        if (testIssue1104) {
            // Non regression for issue 1104, pass-on the inertial propagation frame as body-fixed frame
            zonal = new DSSTZonal(state.getFrame(), provider, 4, 3, 9);
        } else {
            // Classical way of doing the same thing
            zonal = new DSSTZonal(provider, 4, 3, 9);
        }

        // Force model parameters
        final double[] parameters = zonal.getParameters(orbit.getDate());

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Initialize force model
        zonal.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, parameters);

        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);

        final double[] daidt = zonal.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(0.0,                     elements[0], 1.e-25);
        Assertions.assertEquals(1.3909396722346468E-11,  elements[1], 3.e-26);
        Assertions.assertEquals(-2.0275977261372793E-13, elements[2], 3.e-27);
        Assertions.assertEquals(3.087141512018238E-9,    elements[3], 1.e-24);
        Assertions.assertEquals(2.6606317310148797E-9,   elements[4], 4.e-24);
        Assertions.assertEquals(-3.659904725206694E-9,   elements[5], 1.e-24);

    }

    @Test
    void testShortPeriodTerms() {
        final SpacecraftState meanState = getGEOState();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(meanState.getOrbit(), 1);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        zonal.registerAttitudeProvider(null);
        shortPeriodTerms.addAll(zonal.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, zonal.getParameters(meanState.getDate())));
        zonal.updateShortPeriodTerms(zonal.getParametersAllValues(), meanState);

        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }

        Assertions.assertEquals(35.005618980090276,     y[0], 1.e-15);
        Assertions.assertEquals(3.75891551882889E-5,    y[1], 1.e-20);
        Assertions.assertEquals(3.929119925563796E-6,   y[2], 1.e-21);
        Assertions.assertEquals(-1.1781951949124315E-8, y[3], 1.e-24);
        Assertions.assertEquals(-3.2134924513679615E-8, y[4], 1.e-24);
        Assertions.assertEquals(-1.1607392915997098E-6, y[5], 1.e-21);
    }

    private SpacecraftState getGEOState() {
        // No shadow at this date
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        return new SpacecraftState(orbit);
    }
    
    @Test
    void testIssue625() {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        final Frame earthFrame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = new AbsoluteDate(2007, 04, 16, 0, 46, 42.400, TimeScalesFactory.getUTC());

        // a  = 2.655989E6 m
        // ex = 2.719455286199036E-4
        // ey = 0.0041543085910249414
        // hx = -0.3412974060023717
        // hy = 0.3960084733107685
        // lM = 8.566537840341699 rad
        final Orbit orbit = new EquinoctialOrbit(2.655989E6,
                                                 2.719455286199036E-4,
                                                 0.0041543085910249414,
                                                 -0.3412974060023717,
                                                 0.3960084733107685,
                                                 8.566537840341699,
                                                 PositionAngleType.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 3.986004415E14);

        final SpacecraftState state = new SpacecraftState(orbit).withMass(1000.0);

        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Central Body geopotential 32x32
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(32, 32);

        // Zonal force model
        final DSSTZonal zonal = new DSSTZonal(provider, 32, 4, 65);
        zonal.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, zonal.getParameters(orbit.getDate()));

        // Zonal force model with default constructor
        final DSSTZonal zonalDefault = new DSSTZonal(provider);
        zonalDefault.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, zonalDefault.getParameters(orbit.getDate()));

        // Compute mean element rate for the zonal force model
        final double[] elements = zonal.getMeanElementRate(state, auxiliaryElements, zonal.getParameters(orbit.getDate()));

        // Compute mean element rate for the "default" zonal force model
        final double[] elementsDefault = zonalDefault.getMeanElementRate(state, auxiliaryElements, zonalDefault.getParameters(orbit.getDate()));

        // Verify
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(elements[i], elementsDefault[i], Double.MIN_VALUE);
        }

    }

    @Test
    void testOutOfRangeException() {
        try {
            @SuppressWarnings("unused")
            final DSSTZonal zonal = new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(1, 0));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
        }
    }
    
    /** Test for issue 1104.
     * <p>Only J2 is used
     * <p>Comparisons to a numerical propagator are done, with different frames as "body-fixed frames": GCRF, ITRF, TOD
     */
    @Test
    void testIssue1104() {
        
        final boolean printResults = false;
        
        // Frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame tod = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        
        // GCRF/GCRF test
        // ---------
        
        // Using GCRF as both inertial and body frame (behaviour before the fix)
        double diMax  = 9.615e-5;
        double dOmMax = 3.374e-3;
        double dLmMax = 1.128e-2;
        doTestIssue1104(gcrf, gcrf, printResults, diMax, dOmMax, dLmMax);
        
        // TOD/TOD test
        // --------
        
        // Before fix, using TOD was the best choice to reduce the errors DSST vs Numerical
        // INC is one order of magnitude better compared to GCRF/GCRF (and not diverging anymore but it's not testable here)
        // RAAN and LM are only slightly better
        diMax  = 1.059e-5;
        dOmMax = 2.789e-3;
        dLmMax = 1.040e-2;
        doTestIssue1104(tod, tod, printResults, diMax, dOmMax, dLmMax);
        
        // GCRF/ITRF test
        // ---------
        
        // Using ITRF as body-fixed frame and GCRF as inertial frame
        // Results are on par with TOD/TOD
        diMax  = 1.067e-5;
        dOmMax = 2.789e-3;
        dLmMax = 1.040e-2;
        doTestIssue1104(gcrf, itrf, printResults, diMax, dOmMax, dLmMax);
        
        // GCRF/TOD test
        // ---------
        
        // Using TOD as body-fixed frame and GCRF as inertial frame
        // Results are identical to TOD/TOD
        diMax  = 1.059e-5;
        dOmMax = 2.789e-3;
        dLmMax = 1.040e-2;
        doTestIssue1104(tod, itrf, printResults, diMax, dOmMax, dLmMax);
        
        // Since ITRF is longer to compute, if another inertial frame than TOD is used,
        // the best balance performance vs accuracy is to use TOD as body-fixed frame
    }

    /** Implements the comparison between DSST osculating and numerical. */
    private void doTestIssue1104(final Frame inertialFrame,
                                 final Frame bodyFixedFrame,
                                 final boolean printResults,
                                 final double diMax,
                                 final double dOmMax,
                                 final double dLmMax) {
        
        // GIVEN
        // -----
        
        // Parameters
        final double step = 60.;
        final double nOrb = 50.;
        
        final AbsoluteDate t0 = new AbsoluteDate();
        
        // Frames
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        
        // Potential coefficients providers
        final int degree = 2;
        final int order = 0;
        final UnnormalizedSphericalHarmonicsProvider unnormalized =
                        GravityFieldFactory.getConstantUnnormalizedProvider(degree, order, t0);
        final NormalizedSphericalHarmonicsProvider normalized =
                        GravityFieldFactory.getConstantNormalizedProvider(degree, order, t0);

        // Initial LEO osculating orbit
        final double mass = 150.;
        final double a  = 6906780.35;
        final double ex = 5.09E-4;
        final double ey = 1.24e-3;
        final double i  = FastMath.toRadians(97.49);
        final double raan   = FastMath.toRadians(-94.607);
        final double alphaM = FastMath.toRadians(0.);
        final CircularOrbit oscCircOrbit0 = new CircularOrbit(a, ex, ey, i, raan, alphaM, PositionAngleType.MEAN,
                                                              inertialFrame, t0, unnormalized.getMu());
        
        final Orbit oscOrbit0 = new EquinoctialOrbit(oscCircOrbit0);
        final SpacecraftState oscState0 = new SpacecraftState(oscOrbit0).withMass(mass);
        final AttitudeProvider attProvider = new FrameAlignedProvider(inertialFrame);

        // Propagation duration
        final double duration = nOrb * oscOrbit0.getKeplerianPeriod();
        final AbsoluteDate tf = t0.shiftedBy(duration);
        
        // Numerical prop
        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        final NumericalPropagator numProp = new NumericalPropagator(integrator);
        numProp.setOrbitType(oscOrbit0.getType());
        numProp.setInitialState(oscState0);
        numProp.setAttitudeProvider(attProvider);
        numProp.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, normalized)); // J2-only gravity field
        final EphemerisGenerator numEphemGen = numProp.getEphemerisGenerator();

        // DSST prop: max step could be much higher but made explicitly equal to numerical to rule out a step difference
        final ClassicalRungeKuttaIntegrator integratorDsst = new ClassicalRungeKuttaIntegrator(step);
        final DSSTPropagator dsstProp = new DSSTPropagator(integratorDsst, PropagationType.OSCULATING);
        dsstProp.setInitialState(oscState0, PropagationType.OSCULATING); // Initial state is OSCULATING
        dsstProp.setAttitudeProvider(attProvider);
        final DSSTForceModel zonal = new DSSTZonal(bodyFixedFrame, unnormalized); // J2-only with custom Earth-fixed frame
        dsstProp.addForceModel(zonal);
        final EphemerisGenerator dsstEphemGen = dsstProp.getEphemerisGenerator();
        
        // WHEN
        // ----
        
        // Statistics containers: compare on INC, RAAN and anomaly since that's where there is
        // improvement brought by fixing 1104. The in-plane parameters (a, ex, ey) are almost equal
        final StreamingStatistics dI  = new StreamingStatistics();
        final StreamingStatistics dOm = new StreamingStatistics();
        final StreamingStatistics dLM = new StreamingStatistics();

        // Propagate and get ephemeris
        numProp.propagate(t0, tf);
        dsstProp.propagate(t0, tf);
        
        final BoundedPropagator numEphem  = numEphemGen.getGeneratedEphemeris();
        final BoundedPropagator dsstEphem = dsstEphemGen.getGeneratedEphemeris();
        
        // Compare and fill statistics
        for (double dt = 0; dt < duration; dt += step) {

            // Date
            final AbsoluteDate t = t0.shiftedBy(dt);

            // Orbits and comparison
            final CircularOrbit num  = new CircularOrbit(numEphem.propagate(t).getOrbit());
            final CircularOrbit dsst = new CircularOrbit(dsstEphem.propagate(t).getOrbit());
            dI.addValue(FastMath.toDegrees(dsst.getI() - num.getI()));
            dOm.addValue(FastMath.toDegrees(dsst.getRightAscensionOfAscendingNode() - num.getRightAscensionOfAscendingNode()));
            dLM.addValue(FastMath.toDegrees(dsst.getLM() - num.getLM()));
        }
        
        // THEN
        // ----
        
        // Optional: print the statistics
        if (printResults) {
            System.out.println("Inertial frame  : " + inertialFrame.toString());
            System.out.println("Body-Fixed frame: " + bodyFixedFrame.toString());
            System.out.println("\ndi\n" + dI.toString());
            System.out.println("\ndΩ\n" + dOm.toString());
            System.out.println("\ndLM\n" + dLM.toString());
        }
        
        // Compare to reference
        Assertions.assertEquals(diMax, FastMath.max(FastMath.abs(dI.getMax()), FastMath.abs(dI.getMin())), 1.e-8);
        Assertions.assertEquals(dOmMax, FastMath.max(FastMath.abs(dOm.getMax()), FastMath.abs(dOm.getMin())), 1.e-6);
        Assertions.assertEquals(dLmMax, FastMath.max(FastMath.abs(dLM.getMax()), FastMath.abs(dLM.getMin())), 1.e-5);
    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}
