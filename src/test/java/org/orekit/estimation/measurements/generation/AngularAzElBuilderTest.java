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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.MeasurementNoise;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.signal.SignalTravelTimeModel;
import static org.mockito.Mockito.mock;

class AngularAzElBuilderTest extends AbstractGroundMeasurementBuilderTest<AngularAzEl> {

    private static final double SIGMA = 1.0e-3;
    private static final double BIAS  = 1.0e-4;

    protected MeasurementBuilder<AngularAzEl> getBuilder(final RandomGenerator random,
                                                         final GroundStation groundStation,
                                                         final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA, SIGMA * SIGMA });
        MeasurementBuilder<AngularAzEl> ab =
                        new AngularAzElBuilder(groundStation, new double[] { SIGMA, SIGMA}, new double[] { 1.0, 1.0 },
                                               satellite);
        if (random != null) {
            ab.addModifier(new MeasurementNoise<>(new CorrelatedRandomVectorGenerator(covariance,
                    1.0e-10,
                    new GaussianRandomGenerator(random))));
        }
        ab.addModifier(new Bias<>(new String[] { "aBias", "eBias" },
                        new double[] { BIAS, BIAS },
                        new double[] { 1.0, 1.0 },
                        new double[] { -FastMath.PI, -0.5 * FastMath.PI },
                        new double[] { +FastMath.PI, +0.5 * FastMath.PI }));
        return ab;
    }

    @Test
    void testGetSignalTravelTimeModel() {
        // GIVEN
        final GroundStation station = new GroundStation(new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(0., 0., 0), ""));
        final SignalTravelTimeModel signalTravelTimeModel = mock();
        final AngularAzElBuilder builder = new AngularAzElBuilder(station,
                new MeasurementQuality(1., 1.), signalTravelTimeModel, new ObservableSatellite(0));
        // WHEN
        final SignalTravelTimeModel actualModel = builder.getSignalTravelTimeModel();
        // THEN
        Assertions.assertEquals(signalTravelTimeModel, actualModel);
    }

    @Test
    void testForward() {
        doTest(0x527ebeb15d630624l, 0.4, 0.9, 128, 6. * SIGMA);
    }

    @Test
    void testBackward() {
        doTest(0x5300b1314adab8cbl, -0.2, -0.6, 100, 6. * SIGMA);
    }

}
