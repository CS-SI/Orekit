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
package org.orekit.estimation.sequential;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.BrouwerLyddaneContext;
import org.orekit.estimation.BrouwerLyddaneEstimationTestUtils;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.conversion.BrouwerLyddanePropagatorBuilder;

import java.util.List;

public class BrouwerLyddaneKalmanEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     * Keplerian formalism
     */
    @Test
    public void testKeplerianPV() {

        // Create context
        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final boolean       perfectStart  = true;
        final double        dP            = 1.;
        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(positionAngleType, perfectStart, dP);

        // Create perfect PV measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 3.0, 300.0);
        // Reference propagator for estimation performances
        final BrouwerLyddanePropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Covariance matrix initialization
        final RealMatrix initialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-2, 1e-2, 1e-2, 1e-5, 1e-5, 1e-5
        });

        // Process noise matrix
        RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8, 1.e-8
        });


        // Build the Kalman filter
        final KalmanEstimator kalman = new KalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 2.70e-8;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 6.59e-11;
        final double[] expectedsigmasPos = {0.998881, 0.933800, 0.997357};
        final double   sigmaPosEps       = 1e-6;
        final double[] expectedSigmasVel = {9.475737e-4, 9.904671e-4, 5.060183e-4};
        final double   sigmaVelEps       = 1e-10;
        BrouwerLyddaneEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                                         refOrbit, positionAngleType,
                                                         expectedDeltaPos, posEps,
                                                         expectedDeltaVel, velEps,
                                                         expectedsigmasPos, sigmaPosEps,
                                                         expectedSigmasVel, sigmaVelEps);
    }

}
