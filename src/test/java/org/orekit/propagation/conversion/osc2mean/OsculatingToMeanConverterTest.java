/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class OsculatingToMeanConverterTest {

    private Orbit osculating;

    private LeastSquaresOptimizer optimizer;

    private Binary64Field field;

    private boolean doPrint;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        osculating = getOsculatingOrbit();
        optimizer  = new GaussNewtonOptimizer();
        field      = Binary64Field.getInstance();
        doPrint    = false;
    }

    @Test
    void testBrouwerLyddane() {
        // GIVEN
        // Mean theory
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(getProvider(5),
                                                                     BrouwerLyddanePropagator.M2);
        // THEN
        compareAlgorithms(theory, 1e-6, 1e-13, 1e-12, 1e-13, 1e-14, 1e-12);
        compareFieldVersions(theory, 1e-8, 1e-15, 1e-16, 1e-15, 1e-15, 1e-15);
        miscellaneous(theory);
    }

    @Test
    void testEcksteinHechler() {
        // GIVEN
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(6);
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(getOsculatingOrbit().getDate());
        final EcksteinHechlerTheory theory = new EcksteinHechlerTheory(provider.getAe(), provider.getMu(),
                                                                       harmonics.getUnnormalizedCnm(2, 0),
                                                                       harmonics.getUnnormalizedCnm(3, 0),
                                                                       harmonics.getUnnormalizedCnm(4, 0),
                                                                       harmonics.getUnnormalizedCnm(5, 0),
                                                                       harmonics.getUnnormalizedCnm(6, 0));
        // THEN
        compareAlgorithms(theory, 1e-5, 1e-12, 1e-13, 1e-12, 1e-12, 1e-12);
        compareFieldVersions(theory, 1e-15, 1e-15, 1e-15, 1e-15, 1e-15, 1e-15);
        miscellaneous(theory);
    }

    @Test
    void testDSST() {
        // GIVEN
        // Mean theory
        final DSSTTheory theory0 = new DSSTTheory(createDSSTForces());
        final DSSTTheory theory1 = new DSSTTheory(createDSSTForces());
        final DSSTTheory theory2 = new DSSTTheory(createDSSTForces());
        // THEN
        compareAlgorithms(theory0, 1e-6, 1e-13, 1e-13, 1e-14, 1e-14, 1e-15);
        compareFieldVersions(theory1, 1e-8, 1e-15, 1e-15, 1e-15, 1e-15, 1e-15);
        miscellaneous(theory2);
    }

    @Test
    void testTLE() {
        // GIVEN
        // Mean theory
        final TLETheory theory = new TLETheory();
        final TLETheory fieldTheory = new TLETheory(new FieldTLE<>(field, TLETheory.TMP_L1, TLETheory.TMP_L2));
        // THEN
        compareAlgorithms(theory, 1e-5, 1e-12, 1e-12, 1e-12, 1e-13, 1e-12);
        compareFieldVersions(fieldTheory, 1e-8, 1e-15, 1e-15, 1e-15, 1e-15, 1e-15);
    }

    @Test
    void testTLEMisc() {
        // GIVEN
        final String  l1 = "1 00000U 00000A   00001.00000000  .00000000  00000+0  00000+0 0    02";
        final String  l2 = "2 00000   0.0000   0.0000 0000000   0.0000   0.0000  0.00000000    02";
        final TLE tmpTle = new TLE(l1, l2);
        // Mean theory
        final TLETheory theory = new TLETheory(tmpTle);
        // Mean orbit converters
        final FixedPointConverter   fpConverter = new FixedPointConverter(theory);
        final LeastSquaresConverter lsConverter = new LeastSquaresConverter(theory, optimizer);
        // WHEN
        final Orbit fpMean = fpConverter.convertToMean(osculating);
        final Orbit lsMean = lsConverter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(fpConverter.getMeanTheory().getTheoryName(),
                                lsConverter.getMeanTheory().getTheoryName());
        Assertions.assertEquals(FixedPointConverter.DEFAULT_DAMPING,          fpConverter.getDamping(), 0.);
        Assertions.assertEquals(FixedPointConverter.DEFAULT_MAX_ITERATIONS,   fpConverter.getMaxIterations(), 0);
        Assertions.assertEquals(FixedPointConverter.DEFAULT_THRESHOLD,        fpConverter.getThreshold(), 0.);
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_MAX_ITERATIONS, lsConverter.getMaxIterations(), 0);
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_THRESHOLD,      lsConverter.getThreshold(), 0.);
        Assertions.assertEquals(fpMean.getType(),  OrbitType.KEPLERIAN);
        Assertions.assertEquals(fpMean.getDate(),  osculating.getDate());
        Assertions.assertEquals(fpMean.getFrame(), FramesFactory.getTEME());
        Assertions.assertEquals(fpMean.getMu(),    TLEConstants.MU);
        Assertions.assertEquals(lsMean.getType(),  OrbitType.KEPLERIAN);
        Assertions.assertEquals(lsMean.getDate(),  osculating.getDate());
        Assertions.assertEquals(lsMean.getFrame(), FramesFactory.getTEME());
        Assertions.assertEquals(lsMean.getMu(),    TLEConstants.MU);
    }

    @Test
    void testErrors() {
        // GIVEN
        final Orbit orbit = new KeplerianOrbit(1e4, 0.1, 1., 1., 2., -3.,
                                               PositionAngleType.MEAN,
                                               FramesFactory.getEME2000(),
                                               AbsoluteDate.J2000_EPOCH,
                                               Constants.EGM96_EARTH_MU);
        // Field orbit
        final FieldOrbit<?> fieldOrbit = orbit.getType().convertToFieldOrbit(field, orbit);
        // Fixed-point converter
        final FixedPointConverter fpConvert = new FixedPointConverter(new TLETheory());
        // THEN
        // Try converting simple orbit
        Assertions.assertThrows(OrekitException.class, () -> {
            fpConvert.convertToMean(orbit);
        });
        // Try converting field orbit
        Assertions.assertThrows(OrekitException.class, () -> {
            fpConvert.convertToMean(fieldOrbit);
        });
    }

    /**
     * Compares fixed-point and lest-squares algorithms for a given theory.
     * @param theory the theory to consider
     * @param dA  expected deviation on the semi-major axis
     * @param dEx expected deviation on Ex
     * @param dEy expected deviation on Ey
     * @param dHx expected deviation on Hx
     * @param dHy expected deviation on Hy
     * @param dLv expected deviation on true latitude argument
     */
    private void compareAlgorithms(final MeanTheory theory,
                                   final double dA, final double dEx, final double dEy,
                                   final double dHx, final double dHy, final double dLv) {
        // GIVEN
        // Mean orbit converters
        final FixedPointConverter   fpConvert = new FixedPointConverter(theory);
        final LeastSquaresConverter lsConvert = new LeastSquaresConverter(theory, optimizer);
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        if (doPrint) {
            System.out.println(fpConvert.getIterationsNb() + " " + lsConvert.getIterationsNb());
            System.out.println();
        }
        // THEN
        compareOrbits(fpMean, lsMean, dA, dEx, dEy, dHx, dHy, dLv);
    }

    /**
     * Compares field versions of fixed-point and lest-squares algorithms for a given theory.
     * @param theory the theory to consider
     * @param dA  expected deviation on the semi-major axis
     * @param dEx expected deviation on Ex
     * @param dEy expected deviation on Ey
     * @param dHx expected deviation on Hx
     * @param dHy expected deviation on Hy
     * @param dLv expected deviation on true latitude argument
     */
    private void compareFieldVersions(final MeanTheory theory,
                                      final double dA, final double dEx, final double dEy,
                                      final double dHx, final double dHy, final double dLv) {
        // GIVEN
        // Field orbit
        final FieldOrbit<?> fieldOrbit = osculating.getType().convertToFieldOrbit(field, osculating);
        // Mean orbit converters
        final FixedPointConverter   fpConvert = new FixedPointConverter(theory);
        final LeastSquaresConverter lsConvert = new LeastSquaresConverter(theory, optimizer);
        // THEN
        // Try conversion using fixed point algorithm
        Assertions.assertDoesNotThrow(() -> {
            final FieldOrbit<?> fpMean = fpConvert.convertToMean(fieldOrbit);
            compareOrbits(fpMean.toOrbit(), fpConvert.convertToMean(osculating),
                          dA, dEx, dEy, dHx, dHy, dLv);
        });
        // Try conversion using least-squares algorithm
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            lsConvert.convertToMean(fieldOrbit);
        });
    }

    /**
     * Miscellaneous coverage tests.
     * @param theory the mean theory to consider
     */
    private void miscellaneous(final MeanTheory theory) {
        // Check for FixedPointConverter
        fixedPointMisc(theory);
        // Check for LeastSquaresConverter
        leastSquaresMisc(theory);
    }

    /**
     * Miscellaneous coverage tests for FixedPointConverter.
     * @param theory the theory to consider
     */
    private void fixedPointMisc(final MeanTheory theory) {
        FixedPointConverter fpConvert = new FixedPointConverter(1.e-10, 1000, 0.75);
        Assertions.assertNull(fpConvert.getMeanTheory());
        Assertions.assertEquals(1.e-10, fpConvert.getThreshold());
        Assertions.assertEquals(1000, fpConvert.getMaxIterations());
        Assertions.assertEquals(0.75, fpConvert.getDamping());

        fpConvert = new FixedPointConverter();
        Assertions.assertNull(fpConvert.getMeanTheory());
        Assertions.assertEquals(FixedPointConverter.DEFAULT_THRESHOLD, fpConvert.getThreshold());
        Assertions.assertEquals(FixedPointConverter.DEFAULT_MAX_ITERATIONS, fpConvert.getMaxIterations());
        Assertions.assertEquals(FixedPointConverter.DEFAULT_DAMPING, fpConvert.getDamping());

        fpConvert.setMeanTheory(theory);
        Assertions.assertEquals(theory.getTheoryName(), fpConvert.getMeanTheory().getTheoryName());

        Assertions.assertEquals(0, fpConvert.getIterationsNb());
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        // THEN
        Assertions.assertNotEquals(0, fpConvert.getIterationsNb());
        if (theory.getTheoryName().contentEquals(EcksteinHechlerTheory.THEORY)) {
            Assertions.assertEquals(OrbitType.CIRCULAR, fpMean.getType());
        } else {
            Assertions.assertEquals(fpMean.getType(),  osculating.getType());
        }
        Assertions.assertEquals(fpMean.getDate(),  osculating.getDate());
        Assertions.assertEquals(fpMean.getMu(),    osculating.getMu());
        Assertions.assertEquals(fpMean.getFrame(), osculating.getFrame());
    }

    /**
     * Miscellaneous coverage tests for LeastSquaresConverter.
     * @param theory the theory to consider
     */
    private void leastSquaresMisc(final MeanTheory theory) {
        LeastSquaresConverter lsConvert = new LeastSquaresConverter(1.e-3, 100);
        Assertions.assertNull(lsConvert.getMeanTheory());
        Assertions.assertNull(lsConvert.getOptimizer());
        Assertions.assertEquals(1.e-3, lsConvert.getThreshold());
        Assertions.assertEquals(100, lsConvert.getMaxIterations());

        lsConvert = new LeastSquaresConverter();
        Assertions.assertNull(lsConvert.getMeanTheory());
        Assertions.assertNull(lsConvert.getOptimizer());
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_THRESHOLD, lsConvert.getThreshold());
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_MAX_ITERATIONS, lsConvert.getMaxIterations());

        lsConvert = new LeastSquaresConverter(theory);
        Assertions.assertEquals(theory.getTheoryName(), lsConvert.getMeanTheory().getTheoryName());
        Assertions.assertNull(lsConvert.getOptimizer());
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_THRESHOLD, lsConvert.getThreshold());
        Assertions.assertEquals(LeastSquaresConverter.DEFAULT_MAX_ITERATIONS, lsConvert.getMaxIterations());
        lsConvert.setOptimizer(optimizer);
        Assertions.assertEquals(optimizer, lsConvert.getOptimizer());

        Assertions.assertEquals(0, lsConvert.getIterationsNb());
        Assertions.assertEquals(0, lsConvert.getRMS());
        // WHEN
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        // THEN
        Assertions.assertNotEquals(0, lsConvert.getIterationsNb());
        Assertions.assertNotEquals(0, lsConvert.getRMS());
        if (theory.getTheoryName().contentEquals(EcksteinHechlerTheory.THEORY)) {
            Assertions.assertEquals(OrbitType.CIRCULAR, lsMean.getType());
        } else {
            Assertions.assertEquals(lsMean.getType(),  osculating.getType());
        }
        Assertions.assertEquals(lsMean.getDate(),  osculating.getDate());
        Assertions.assertEquals(lsMean.getMu(),    osculating.getMu());
        Assertions.assertEquals(lsMean.getFrame(), osculating.getFrame());
    }

    /**
     * Compares 2 orbits.
     * @param orbit1 the 1st orbit to consider
     * @param orbit2 the 2nd orbit to consider
     * @param dA  expected deviation on the semi-major axis
     * @param dEx expected deviation on Ex
     * @param dEy expected deviation on Ey
     * @param dHx expected deviation on Hx
     * @param dHy expected deviation on Hy
     * @param dLv expected deviation on true latitude argument
     */
    private void compareOrbits(final Orbit orbit1, final Orbit orbit2,
                               final double dA, final double dEx, final double dEy,
                               final double dHx, final double dHy, final double dLv) {
        if (doPrint) {
            System.out.println(orbit1.getA() - orbit2.getA());
            System.out.println(orbit1.getEquinoctialEx() - orbit2.getEquinoctialEx());
            System.out.println(orbit1.getEquinoctialEy() - orbit2.getEquinoctialEy());
            System.out.println(orbit1.getHx() - orbit2.getHx());
            System.out.println(orbit1.getHy() - orbit2.getHy());
            System.out.println(orbit1.getLv() - orbit2.getLv());
            System.out.println();
        }
        Assertions.assertEquals(orbit1.getA(), orbit2.getA(), dA);
        Assertions.assertEquals(orbit1.getEquinoctialEx(), orbit2.getEquinoctialEx(), dEx);
        Assertions.assertEquals(orbit1.getEquinoctialEy(), orbit2.getEquinoctialEy(), dEy);
        Assertions.assertEquals(orbit1.getHx(), orbit2.getHx(), dHx);
        Assertions.assertEquals(orbit1.getHy(), orbit2.getHy(), dHy);
        Assertions.assertEquals(orbit1.getLv(), orbit2.getLv(), dLv);
    }

    /**
     * Builds an orbit.
     * @return the orbit
     */
    private Orbit getOsculatingOrbit() {
        return new KeplerianOrbit(1e7, 0.1, 1., 1., 2., -3.,
                                  PositionAngleType.MEAN,
                                  FramesFactory.getEME2000(),
                                  AbsoluteDate.J2000_EPOCH,
                                  Constants.EGM96_EARTH_MU);
    }

    /**
     * Gets an harmonic provider.
     * @param maxDegree the max degree to set (order is set to 0)
     * @return harmonic provider
     */
    private UnnormalizedSphericalHarmonicsProvider getProvider(final int maxDegree) {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(maxDegree, 0);
    }

    /**
     * Create list of first 6 zonal DSST forces.
     * @return six first zonal forces
     */
    private List<DSSTForceModel> createDSSTForces() {
        final List<DSSTForceModel> forceModels = new ArrayList<>();
        final DSSTZonal zonal = new DSSTZonal(getProvider(6));
        forceModels.add(zonal);
        return forceModels;
    }

}
