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

import java.util.concurrent.TimeUnit;

import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.stat.descriptive.StorelessUnivariateStatistic;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class BrouwerLyddaneMeanConversionTest {

    @Test
    void testIssue947() {
        // Error case from gitlab issue#947: negative eccentricity when calculating mean orbit
        final TLE tleOrbit = new TLE("1 43196U 18015E   21055.59816856  .00000894  00000-0  38966-4 0  9996",
                                     "2 43196  97.4662 188.8169 0016935 299.6845  60.2706 15.24746686170319");
        final Propagator propagator = TLEPropagator.selectExtrapolator(tleOrbit);

        //Get state at initial date and 3 days before
        SpacecraftState tleState = propagator.getInitialState();
        final KeplerianOrbit osculating0 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(tleState.getOrbit());
        SpacecraftState tleStateAtDate = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(3,  TimeUnit.DAYS));
        final KeplerianOrbit osculating1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(tleStateAtDate.getOrbit());

        // BL theory
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);

        // Converts osculating to mean using both algorithms
        final Orbit mean0fp = fpConvert.convertToMean(osculating0);
        final Orbit mean0ls = lsConvert.convertToMean(osculating0);

        if (doPrint) {
            System.out.println("osculating : " + osculating0);
            System.out.println("mean (fp)  : " + mean0fp);
            System.out.println("mean (ls)  : " + mean0ls);
            System.out.println("iter : " + lsConvert.getIterationsNb() + " ; rms : " + lsConvert.getRMS());
            System.out.println("écart (m)  : " + Vector3D.distance(mean0ls.getPosition(mean0ls.getFrame()),
                                                                   mean0fp.getPosition(mean0ls.getFrame())));
            System.out.println();
        }

        final Orbit mean1fp1 = fpConvert.convertToMean(osculating1);
        final Orbit mean1ls1 = lsConvert.convertToMean(osculating1);

        if (doPrint) {
            System.out.println("osculating : " + osculating1);
            System.out.println("mean (fp)  : " + mean1fp1);
            System.out.println("mean (ls)  : " + mean1ls1);
            System.out.println("iter : " + lsConvert.getIterationsNb() + " ; rms : " + lsConvert.getRMS());
            System.out.println("écart (m)  : " + Vector3D.distance(mean1ls1.getPosition(mean1ls1.getFrame()),
                                                                   mean1fp1.getPosition(mean1ls1.getFrame())));
            System.out.println();
        }
    }

    @Test
    void testIssue1558() {

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit(67679244.0, 0.0, 1.85850, 2.1, 2.9, 3.2,
                                                PositionAngleType.TRUE, FramesFactory.getEME2000(),
                                                initDate, Constants.EGM96_EARTH_MU);
        if (doPrint) {
            System.out.println(initialOrbit);
            System.out.println();
        }

        // Get BL propagator
        BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialOrbit, provider,
                                                                           PropagationType.MEAN,
                                                                           BrouwerLyddanePropagator.M2);

        // Get orbit at initial date and 3 days after
        final double shift = 3 * Constants.JULIAN_DAY;
        final Orbit initialState = propagator.propagate(initDate).getOrbit();
        final Orbit futureState  = propagator.propagate(initDate.shiftedBy(shift)).getOrbit();

        // BL theory
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);

        // Try using fixed-point algorithm
        OrekitException oei = Assertions.assertThrows(OrekitException.class, () -> {
            fpConvert.convertToMean(initialState);
        });
        Assertions.assertTrue(oei.getMessage().contains("unable to compute Brouwer-Lyddane mean parameters after"));
        OrekitException oef = Assertions.assertThrows(OrekitException.class, () -> {
            fpConvert.convertToMean(futureState);
        });
        Assertions.assertTrue(oef.getMessage().contains("unable to compute Brouwer-Lyddane mean parameters after"));

        // Try using least-squares algorithm
        final Orbit mean0ls = lsConvert.convertToMean(initialState);

        if (doPrint) {
            System.out.println("iter : " + lsConvert.getIterationsNb() + " ; rms : " + lsConvert.getRMS());
            System.out.println(initialState);
            System.out.println(mean0ls);
            System.out.println();
        }

        final Orbit mean1ls = lsConvert.convertToMean(futureState);

        if (doPrint) {
            System.out.println("iter : " + lsConvert.getIterationsNb() + " ; rms : " + lsConvert.getRMS());
            System.out.println(futureState);
            System.out.println(mean1ls);
        }
    }

    @Test
    void testMeanOrbit() {
        final KeplerianOrbit initialOsculating =
                        new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());

        // BL theory
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = NumericalPropagator.tolerances(0.1, initialOsculating, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator num = new NumericalPropagator(integrator);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(provider)));
        num.setInitialState(new SpacecraftState(initialOsculating));
        num.setOrbitType(OrbitType.KEPLERIAN);
        num.setPositionAngleType(initialOsculating.getCachedPositionAngleType());
        final StorelessUnivariateStatistic oscMin  = new Min();
        final StorelessUnivariateStatistic oscMax  = new Max();
        final StorelessUnivariateStatistic meanFpMin = new Min();
        final StorelessUnivariateStatistic meanFpMax = new Max();
        final StorelessUnivariateStatistic meanLsMin = new Min();
        final StorelessUnivariateStatistic meanLsMax = new Max();
        num.getMultiplexer().add(60, state -> {
            final Orbit osc = state.getOrbit();
            oscMin.increment(osc.getA());
            oscMax.increment(osc.getA());
            // compute mean orbit at current date (this is what we test)
            final Orbit mean0 = fpConvert.convertToMean(state.getOrbit());
            final Orbit mean1 = lsConvert.convertToMean(state.getOrbit());
            meanFpMin.increment(mean0.getA());
            meanFpMax.increment(mean0.getA());
            meanLsMin.increment(mean1.getA());
            meanLsMax.increment(mean1.getA());
            if (doPrint) {
                System.out.println("iter : " + lsConvert.getIterationsNb() + " ; rms : " + lsConvert.getRMS());
                System.out.println("écart (m)  : " + Vector3D.distance(mean0.getPosition(mean0.getFrame()),
                                                                       mean1.getPosition(mean0.getFrame())));
                System.out.println();
            }
        });
        num.propagate(initialOsculating.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Asserts
        Assertions.assertEquals(3188.347, oscMax.getResult()  - oscMin.getResult(),  1.0e-3);
        Assertions.assertEquals(  25.794, meanFpMax.getResult() - meanFpMin.getResult(), 1.0e-3);
        Assertions.assertEquals(  25.794, meanLsMax.getResult() - meanLsMin.getResult(), 1.0e-3);

    }

    @Test
    void testGeostationaryOrbit() {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame eci = FramesFactory.getEME2000();

        // geostationary orbit
        final double sma = FastMath.cbrt(Constants.IERS2010_EARTH_MU /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY);
        final double ecc  = 0.0;
        final double inc  = 0.0;
        final double pa   = 0.0;
        final double raan = 0.0;
        final double lV   = 0.0;
        final Orbit orbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, lV,
                                               PositionAngleType.TRUE,
                                               eci, date, provider.getMu());

        // set up a BL propagator from mean orbit
        final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(orbit, provider, PropagationType.MEAN,
                                                                         BrouwerLyddanePropagator.M2);

        // propagate
        final SpacecraftState state = bl.propagate(date);
        final KeplerianOrbit orbOsc = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit());

        if (doPrint) {
            System.out.println(orbit);
            System.out.println();
            System.out.println(orbOsc);
        }

        // BL theory
        final BrouwerLyddaneTheory theory = new BrouwerLyddaneTheory(provider,
                                                                     BrouwerLyddanePropagator.M2);
        // Mean orbit converters
        final FixedPointAlgorithm fpConvert = new FixedPointAlgorithm(theory,
                                                                      FixedPointAlgorithm.DEFAULT_EPSILON,
                                                                      FixedPointAlgorithm.DEFAULT_MAX_ITERATIONS,
                                                                      0.75);
        final LeastSquaresAlgorithm lsConvert = new LeastSquaresAlgorithm(theory);

        // Recover mean orbit
        Assertions.assertDoesNotThrow(() -> {
            Orbit orbMean = fpConvert.convertToMean(orbOsc);
            if (doPrint) {
                System.out.println(orbMean);
                System.out.println();
            }
        });

        MathIllegalStateException mise = Assertions.assertThrows(MathIllegalStateException.class, () -> {
            lsConvert.convertToMean(orbOsc);
        });
        Assertions.assertTrue(mise.getMessage().contains("unable to perform Q.R decomposition"));

        // set up a BL propagator from osculating orbit
        final BrouwerLyddanePropagator bl2 = new BrouwerLyddanePropagator(orbit, provider,
                                                                          PropagationType.OSCULATING,
                                                                          BrouwerLyddanePropagator.M2);

        // propagate
        final SpacecraftState state2 = bl2.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        final KeplerianOrbit orbOsc2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state2.getOrbit());
        if (doPrint) {
            System.out.println(orbOsc2);
        }
        Assertions.assertDoesNotThrow(() -> {
            Orbit orbMean2 = fpConvert.convertToMean(orbOsc2);
            if (doPrint) {
                System.out.println(orbMean2);
                System.out.println();
            }
        });

        mise = Assertions.assertThrows(MathIllegalStateException.class, () -> {
            lsConvert.convertToMean(orbOsc2);
        });
        Assertions.assertTrue(mise.getMessage().contains("unable to perform Q.R decomposition"));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere:potential/icgem-format");
        provider = GravityFieldFactory.getUnnormalizedProvider(5, 0);
        doPrint = false;
    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    private UnnormalizedSphericalHarmonicsProvider provider;

    private boolean doPrint;

}
