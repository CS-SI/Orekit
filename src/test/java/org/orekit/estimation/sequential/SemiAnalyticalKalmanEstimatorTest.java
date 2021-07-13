/* Copyright 2002-2021 CS GROUP
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.DSSTRangeMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.Range;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class SemiAnalyticalKalmanEstimatorTest {

    /** Space for residual file. */
    private static final String SPACE = " ";

    /** Format for residuals line. */
    private static String fmt = "%s";

    /** Carrier return. */
    private static String cReturn = "\n";

    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new SemiAnalyticalKalmanEstimatorBuilder().build();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    /**
     * Perfect range measurements with a biased start
     * Keplerian formalism
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;
        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(perfectStart,
                                              minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
                                                               0.0, 6.0, 60.0);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());
        
        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        kalman.setObserver(new Observer(measurements.get(0).getDate(), measurements.size()));

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.0e-15;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.0e-15;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);
    }

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Start epoch. */
        private final AbsoluteDate startEpoch;

        /** Number of measurements. */
        private final int nbOfMeasurement;

        /** Writer. */
        private PrintWriter residuals;

        /**
         * Constructor.
         * @param startEpoch start epoch
         * @param nbOfMeasurement expected number of measurements
         */
        public Observer(final AbsoluteDate startEpoch, final int nbOfMeasurement) {
            this.startEpoch = startEpoch;
            this.nbOfMeasurement = nbOfMeasurement;
            try {
                this.residuals = new PrintWriter(new File("residuals.txt"));
            } catch (FileNotFoundException e) {
                System.out.println("Enable to create residual file");
                this.residuals = null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getCorrectedMeasurement();

//            // Estimated orbital parameters
//            final List<DelegatingDriver> drivers = estimation.getEstimatedOrbitalParameters().getDrivers();

            // Check
            if (estimatedMeasurement.getObservedMeasurement() instanceof Position) {

                final double[] estimated = estimatedMeasurement.getEstimatedValue();
                final double[] observed  = estimatedMeasurement.getObservedValue();
//                final double[] orbital   = new double[] {
//                    drivers.get(0).getValue(), drivers.get(1).getValue(), drivers.get(2).getValue()
//                };

                // Calculate residuals
                final double resX  = estimated[0] - observed[0];
                final double resY  = estimated[1] - observed[1];
                final double resZ  = estimated[2] - observed[2];

//                // Calculate residuals using estimated satellite position (must be identical to estimated measurement value)
//                final double resXOrb  = orbital[0] - observed[0];
//                final double resYOrb  = orbital[1] - observed[1];
//                final double resZOrb  = orbital[2] - observed[2];

                // Time difference in days
                final double dt = estimatedMeasurement.getDate().durationFrom(startEpoch) / Constants.JULIAN_DAY;

                // Add measurement line
                final String line = String.format(fmt, dt + SPACE + resX  + SPACE + resY  + SPACE + resZ);
//                final String line = String.format(fmt, dt + SPACE + resX  + SPACE + resY  + SPACE + resZ + SPACE + resXOrb  + SPACE + resYOrb  + SPACE + resZOrb);
                System.out.println(line);
                if (residuals != null) {
                    residuals.printf(fmt, dt + SPACE + resX  + SPACE + resY  + SPACE + resZ + cReturn);
                }

            } else if (estimatedMeasurement.getObservedMeasurement() instanceof Range) {

            	final double[] estimated = estimatedMeasurement.getEstimatedValue();
                final double[] observed  = estimatedMeasurement.getObservedValue();

                // Calculate residual
                final double res = observed[0] - estimated[0];

                // Time difference in days
                final double dt = estimatedMeasurement.getDate().durationFrom(startEpoch) / Constants.JULIAN_DAY;

                // Add measurement line
                final String line = String.format(fmt, dt + SPACE + res);
                System.out.println(line);
                if (residuals != null) {
                    residuals.printf(fmt, dt + SPACE + res + cReturn);
                }

            }

            // Check if it's the last measurement
            if (estimation.getCurrentMeasurementNumber() == nbOfMeasurement) {
                // Close the file
                residuals.close();
            }

        }

    }

}
