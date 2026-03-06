/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class AngularRaDecBuilderTest extends AbstractGroundMeasurementBuilderTest<AngularRaDec> {

    private static final double SIGMA = 1.0e-3;
    private static final double BIAS  = 1.0e-4;

    protected MeasurementBuilder<AngularRaDec> getBuilder(final RandomGenerator random,
                                                          final GroundStation groundStation,
                                                          final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA, SIGMA * SIGMA });
        MeasurementBuilder<AngularRaDec> ab =
                        new AngularRaDecBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                            1.0e-10,
                                                                                                            new GaussianRandomGenerator(random)),
                                                groundStation, FramesFactory.getEME2000(),
                                                new double[] { SIGMA, SIGMA}, new double[] { 1.0, 1.0 },
                                                satellite);
        ab.addModifier(new Bias<>(new String[] { "aBias", "eBias" },
                        new double[] { BIAS, BIAS },
                        new double[] { 1.0, 1.0 },
                        new double[] { -FastMath.PI, -0.5 * FastMath.PI },
                        new double[] { +FastMath.PI, +0.5 * FastMath.PI }));
        return ab;
    }

    @Test
    public void testForward() {
        doTest(0x5c845a8e6a11f7b3l, 0.4, 0.9, 128, 3.1 * SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0x24f750901da8cd2cl, -0.2, -0.6, 100, 2.7 * SIGMA);
    }

    @Test
    void testBuildCovariance() {
        // GIVEN
        final GroundStation station = new GroundStation(new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(0., 0., 0), ""));
        final double[][] covarianceCoefficients = MatrixUtils.createRealIdentityMatrix(2).getData();
        covarianceCoefficients[0][1] = 0.1;
        covarianceCoefficients[1][0] = covarianceCoefficients[0][1];
        final AngularRaDecBuilder builder = new AngularRaDecBuilder(null, station, FramesFactory.getEME2000(),
                new MeasurementQuality(covarianceCoefficients, 1.), new SignalTravelTimeModel(), new ObservableSatellite(0));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(date));
        builder.init(date, date);
        // WHEN
        final EstimatedMeasurementBase<AngularRaDec> angularRaDec = builder.build(date, new SpacecraftState[] {state});
        // THEN
        assertArrayEquals(covarianceCoefficients, angularRaDec.getObservedMeasurement().getMeasurementQuality().getCovarianceMatrix().getData());
    }
}
