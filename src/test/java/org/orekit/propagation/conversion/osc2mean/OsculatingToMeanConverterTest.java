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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.tle.TLEConstants;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class OsculatingToMeanConverterTest {

    private boolean doPrint;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        doPrint = false;
    }

    @Test
    void testBrouwerLyddane() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(5);
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        if (doPrint) {
            System.out.println(fpConvert.getIterationsNb() + " " + lsConvert.getIterationsNb());
            System.out.println(fpMean.getA() - lsMean.getA());
            System.out.println(fpMean.getEquinoctialEx() - lsMean.getEquinoctialEx());
            System.out.println(fpMean.getEquinoctialEy() - lsMean.getEquinoctialEy());
            System.out.println(fpMean.getHx() - lsMean.getHx());
            System.out.println(fpMean.getHy() - lsMean.getHy());
            System.out.println(fpMean.getLv() - lsMean.getLv());
        }
        // THEN
        compareOrbits(fpMean, lsMean, 1e-6, 1e-13, 1e-12, 1e-13, 1e-14, 1e-12);
    }

    @Test
    void testBrouwerLyddaneFixedPoint() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(5);
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converter
        final FixedPointAlgorithm converter = new FixedPointAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
        Assertions.assertEquals(mean.getMu(), osculating.getMu());
        Assertions.assertEquals(mean.getFrame(), osculating.getFrame());
    }

    @Test
    void testBrouwerLyddaneLeastSquares() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(5);
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converter
        final LeastSquaresAlgorithm converter = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
        Assertions.assertEquals(mean.getMu(), osculating.getMu());
        Assertions.assertEquals(mean.getFrame(), osculating.getFrame());
    }

    @Test
    void testEcksteinHechler() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(6);
        final EcksteinHechlerTheory theory = new EcksteinHechlerTheory(provider);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        if (doPrint) {
            System.out.println(fpConvert.getIterationsNb() + " " + lsConvert.getIterationsNb());
            System.out.println(fpMean.getA() - lsMean.getA());
            System.out.println(fpMean.getEquinoctialEx() - lsMean.getEquinoctialEx());
            System.out.println(fpMean.getEquinoctialEy() - lsMean.getEquinoctialEy());
            System.out.println(fpMean.getHx() - lsMean.getHx());
            System.out.println(fpMean.getHy() - lsMean.getHy());
            System.out.println(fpMean.getLv() - lsMean.getLv());
        }
        // THEN
        compareOrbits(fpMean, lsMean, 1e-8, 1e-15, 1e-14, 1e-14, 1e-14, 1e-15);
    }

    @Test
    void testEcksteinHechlerFixedPoint() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(6);
        final EcksteinHechlerTheory theory = new EcksteinHechlerTheory(provider);
        // Mean orbit converter
        final FixedPointAlgorithm converter = new FixedPointAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
        Assertions.assertEquals(mean.getMu(), osculating.getMu());
        Assertions.assertEquals(mean.getFrame(), osculating.getFrame());
    }

    @Test
    void testEcksteinHechlerLeastSquares() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(6);
        final EcksteinHechlerTheory theory = new EcksteinHechlerTheory(provider);
        // Mean orbit converter
        final LeastSquaresAlgorithm converter = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
    }

    @Test
    void testDSSTFixedPoint() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final List<DSSTForceModel> forces = createDSSTForces();
        final DSSTTheory theory = new DSSTTheory(forces);
        // Mean orbit converter
        final FixedPointAlgorithm converter = new FixedPointAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
        Assertions.assertEquals(mean.getMu(), osculating.getMu());
        Assertions.assertEquals(mean.getFrame(), osculating.getFrame());
    }

    @Test
    void testDSSTLeastSquares() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final List<DSSTForceModel> forces = createDSSTForces();
        final DSSTTheory theory = new DSSTTheory(forces);
        // Mean orbit converter
        final LeastSquaresAlgorithm converter = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(), osculating.getType());
        Assertions.assertEquals(mean.getDate(), osculating.getDate());
    }

    @Test
    void testDSST() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final List<DSSTForceModel> forces = createDSSTForces();
        final DSSTTheory theory = new DSSTTheory(forces);
        // Mean orbit converters
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        if (doPrint) {
            System.out.println(fpConvert.getIterationsNb() + " " + lsConvert.getIterationsNb());
            System.out.println(fpMean.getA() - lsMean.getA());
            System.out.println(fpMean.getEquinoctialEx() - lsMean.getEquinoctialEx());
            System.out.println(fpMean.getEquinoctialEy() - lsMean.getEquinoctialEy());
            System.out.println(fpMean.getHx() - lsMean.getHx());
            System.out.println(fpMean.getHy() - lsMean.getHy());
            System.out.println(fpMean.getLv() - lsMean.getLv());
        }
        // THEN
        compareOrbits(fpMean, lsMean, 1e-6, 1e-13, 1e-13, 1e-14, 1e-14, 1e-15);
    }

    @Test
    void testTLEFixedPoint() {
        // GIVEN
        // Mean theory
        final TLETheory theory   = new TLETheory();
        // Mean orbit converter
        // Mean orbit converter
        final FixedPointAlgorithm converter = new FixedPointAlgorithm(theory);
        // WHEN
        final Orbit osculating = getOsculatingOrbit();
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(),  OrbitType.KEPLERIAN);
        Assertions.assertEquals(mean.getDate(),  osculating.getDate());
        Assertions.assertEquals(mean.getFrame(), FramesFactory.getTEME());
        Assertions.assertEquals(mean.getMu(), TLEConstants.MU);
    }

    @Test
    void testTLELeastSquares() {
        // GIVEN
        // Mean theory
        final TLETheory theory   = new TLETheory();
        // Mean orbit converter
        final LeastSquaresAlgorithm converter = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit osculating = getOsculatingOrbit();
        final Orbit mean = converter.convertToMean(osculating);
        // THEN
        Assertions.assertEquals(mean.getType(),  OrbitType.KEPLERIAN);
        Assertions.assertEquals(mean.getDate(),  osculating.getDate());
        Assertions.assertEquals(mean.getFrame(), FramesFactory.getTEME());
        Assertions.assertEquals(mean.getMu(), TLEConstants.MU);
    }

    @Test
    void testTLE() {
        // GIVEN
        final Orbit osculating = getOsculatingOrbit();
        // Mean theory
        final TLETheory theory   = new TLETheory();
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);
        // WHEN
        final Orbit fpMean = fpConvert.convertToMean(osculating);
        final Orbit lsMean = lsConvert.convertToMean(osculating);
        if (doPrint) {
            System.out.println(fpConvert.getIterationsNb() + " " + lsConvert.getIterationsNb());
            System.out.println(fpMean.getA() - lsMean.getA());
            System.out.println(fpMean.getEquinoctialEx() - lsMean.getEquinoctialEx());
            System.out.println(fpMean.getEquinoctialEy() - lsMean.getEquinoctialEy());
            System.out.println(fpMean.getHx() - lsMean.getHx());
            System.out.println(fpMean.getHy() - lsMean.getHy());
            System.out.println(fpMean.getLv() - lsMean.getLv());
        }
        // THEN
        compareOrbits(fpMean, lsMean, 1e-5, 1e-12, 1e-12, 1e-12, 1e-13, 1e-12);
    }

    private void compareOrbits(final Orbit orbit1, final Orbit orbit2,
                               final double dA, final double dEx, final double dEy,
                               final double dHx, final double dHy, final double dLv) {
        Assertions.assertEquals(orbit1.getA(), orbit2.getA(), dA);
        Assertions.assertEquals(orbit1.getEquinoctialEx(), orbit2.getEquinoctialEx(), dEx);
        Assertions.assertEquals(orbit1.getEquinoctialEy(), orbit2.getEquinoctialEy(), dEy);
        Assertions.assertEquals(orbit1.getHx(), orbit2.getHx(), dHx);
        Assertions.assertEquals(orbit1.getHy(), orbit2.getHy(), dHy);
        Assertions.assertEquals(orbit1.getLv(), orbit2.getLv(), dLv);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider(final int maxDegree) {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(maxDegree, 0);
    }

    private Orbit getOsculatingOrbit() {
        return new KeplerianOrbit(1e7, 0.1, 1., 1., 2., -3.,
                                  PositionAngleType.MEAN,
                                  FramesFactory.getEME2000(),
                                  AbsoluteDate.J2000_EPOCH,
                                  Constants.EGM96_EARTH_MU);
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
