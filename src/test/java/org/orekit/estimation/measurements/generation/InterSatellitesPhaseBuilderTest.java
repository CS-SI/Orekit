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
package org.orekit.estimation.measurements.generation;

import java.util.SortedSet;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.gnss.InterSatellitesPhase;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.gnss.Frequency;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.InterSatDirectViewDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class InterSatellitesPhaseBuilderTest {

    private static final double SIGMA =  0.5;
    private static final double BIAS  = -0.01;
    private static final double WAVELENGTH = Frequency.G01.getWavelength();

    private MeasurementBuilder<InterSatellitesPhase> getBuilder(final RandomGenerator random,
                                                                final ObservableSatellite receiver,
                                                                final ObservableSatellite remote) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA });
        MeasurementBuilder<InterSatellitesPhase> isrb =
                        new InterSatellitesPhaseBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                                    1.0e-10,
                                                                                                                    new GaussianRandomGenerator(random)),
                                                        receiver, remote, WAVELENGTH, SIGMA, 1.0);
        isrb.addModifier(new Bias<>(new String[] { "bias" },
                         new double[] { BIAS },
                         new double[] { 1.0 },
                         new double[] { Double.NEGATIVE_INFINITY },
                         new double[] { Double.POSITIVE_INFINITY }));
        return isrb;
    }

    @Test
    public void testForward() {
        doTest(0xc82a56322345dc25l, 0.0, 1.2, 2.8 * SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0x95c10149c4891232l, 0.0, -1.0, 2.6 * SIGMA);
    }

    private Propagator buildPropagator() {
        return EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
    }

    private void doTest(long seed, double startPeriod, double endPeriod, double tolerance) {
        Generator generator = new Generator();
        generator.addPropagator(buildPropagator()); // dummy first propagator
        generator.addPropagator(buildPropagator()); // dummy second propagator
        ObservableSatellite receiver = generator.addPropagator(buildPropagator()); // useful third propagator
        generator.addPropagator(buildPropagator()); // dummy fourth propagator
        final Orbit o1 = context.initialOrbit;
        // for the second satellite, we simply reverse velocity
        final Orbit o2 = new KeplerianOrbit(new PVCoordinates(o1.getPVCoordinates().getPosition(),
                                                              o1.getPVCoordinates().getVelocity().negate()),
                                            o1.getFrame(), o1.getDate(), o1.getMu());
        ObservableSatellite remote = generator.addPropagator(new KeplerianPropagator(o2)); // useful sixth propagator
        final double step = 60.0;

        // beware that in order to avoid deadlocks, the slave PV coordinates provider
        // in InterSatDirectViewDetector must be *different* from the second propagator
        // added to generator above! The reason is the event detector will be bound
        // to the first propagator, so it cannot also refer to the second one at the same time
        // this is the reason why we create a *new* KeplerianPropagator below
        generator.addScheduler(new EventBasedScheduler<>(getBuilder(new Well19937a(seed), receiver, remote),
                                                         new FixedStepSelector(step, TimeScalesFactory.getUTC()),
                                                         generator.getPropagator(receiver),
                                                         new InterSatDirectViewDetector(context.earth, new KeplerianPropagator(o2)),
                                                         SignSemantic.FEASIBLE_MEASUREMENT_WHEN_POSITIVE));

        final double period = o1.getKeplerianPeriod();
        AbsoluteDate t0     = o1.getDate().shiftedBy(startPeriod * period);
        AbsoluteDate t1     = o1.getDate().shiftedBy(endPeriod   * period);
        SortedSet<ObservedMeasurement<?>> measurements = generator.generate(t0, t1);

        // and yet another set of propagators for reference
        Propagator propagator1 = buildPropagator();
        Propagator propagator2 = new KeplerianPropagator(o2);

        double maxError = 0;
        AbsoluteDate previous = null;
        AbsoluteDate tInf = t0.compareTo(t1) < 0 ? t0 : t1;
        AbsoluteDate tSup = t0.compareTo(t1) < 0 ? t1 : t0;
        for (ObservedMeasurement<?> measurement : measurements) {
            AbsoluteDate date = measurement.getDate();
            double[] m = measurement.getObservedValue();
            Assert.assertTrue(date.compareTo(tInf) >= 0);
            Assert.assertTrue(date.compareTo(tSup) <= 0);
            if (previous != null) {
                // measurements are always chronological, even with backward propagation,
                // due to the SortedSet (which is intended for combining several
                // measurements types with different builders and schedulers)
                Assert.assertTrue(date.durationFrom(previous) >= 0.999999 * step);
            }
            previous = date;
            double[] e = measurement.estimate(0, 0,
                                              new SpacecraftState[] {
                                                  propagator1.propagate(date),
                                                  propagator2.propagate(date)
                                              }).getEstimatedValue();
            for (int i = 0; i < m.length; ++i) {
                maxError = FastMath.max(maxError, FastMath.abs(e[i] - m[i]));
            }
        }
        Assert.assertEquals(0.0, maxError, tolerance);
     }

     @Before
     public void setUp() {
         context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

         propagatorBuilder = context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                                   1.0e-6, 300.0, 0.001, Force.POTENTIAL,
                                                   Force.THIRD_BODY_SUN, Force.THIRD_BODY_MOON);
     }

     Context context;
     NumericalPropagatorBuilder propagatorBuilder;

}
