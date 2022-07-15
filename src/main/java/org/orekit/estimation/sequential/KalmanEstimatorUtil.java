/* Copyright 2002-2022 CS GROUP
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/**
 * Utility class for Kalman Filter.
 */
public class KalmanEstimatorUtil {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private KalmanEstimatorUtil() {
    }

    /** Decorate an observed measurement.
     * <p>
     * The "physical" measurement noise matrix is the covariance matrix of the measurement.
     * Normalizing it consists in applying the following equation: Rn[i,j] =  R[i,j]/σ[i]/σ[j]
     * Thus the normalized measurement noise matrix is the matrix of the correlation coefficients
     * between the different components of the measurement.
     * </p>
     * @param observedMeasurement the measurement
     * @param referenceDate reference date
     * @return decorated measurements
     */
    public static MeasurementDecorator decorate(final ObservedMeasurement<?> observedMeasurement,
                                                final AbsoluteDate referenceDate) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // Indeed, the "physical" measurement noise matrix is the covariance matrix of the measurement
        // Normalizing it leaves us with the matrix of the correlation coefficients
        final RealMatrix covariance;
        if (observedMeasurement instanceof PV) {
            // For PV measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final PV pv = (PV) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(pv.getCorrelationCoefficientsMatrix());
        } else if (observedMeasurement instanceof Position) {
            // For Position measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final Position position = (Position) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(position.getCorrelationCoefficientsMatrix());
        } else {
            // For other measurements we do not have a covariance matrix.
            // Thus the correlation coefficients matrix is an identity matrix.
            covariance = MatrixUtils.createRealIdentityMatrix(observedMeasurement.getDimension());
        }

        return new MeasurementDecorator(observedMeasurement, covariance, referenceDate);

    }

    /** Check dimension.
     * @param dimension dimension to check
     * @param orbitalParameters orbital parameters
     * @param propagationParameters propagation parameters
     * @param measurementParameters measurements parameters
     */
    public static void checkDimension(final int dimension,
                                      final ParameterDriversList orbitalParameters,
                                      final ParameterDriversList propagationParameters,
                                      final ParameterDriversList measurementParameters) {

        // count parameters, taking care of counting all orbital parameters
        // regardless of them being estimated or not
        int requiredDimension = orbitalParameters.getNbParams();
        for (final ParameterDriver driver : propagationParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++requiredDimension;
            }
        }
        for (final ParameterDriver driver : measurementParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++requiredDimension;
            }
        }

        if (dimension != requiredDimension) {
            // there is a problem, set up an explicit error message
            final StringBuilder sBuilder = new StringBuilder();
            for (final ParameterDriver driver : orbitalParameters.getDrivers()) {
                if (sBuilder.length() > 0) {
                    sBuilder.append(", ");
                }
                sBuilder.append(driver.getName());
            }
            for (final ParameterDriver driver : propagationParameters.getDrivers()) {
                if (driver.isSelected()) {
                    sBuilder.append(driver.getName());
                }
            }
            for (final ParameterDriver driver : measurementParameters.getDrivers()) {
                if (driver.isSelected()) {
                    sBuilder.append(driver.getName());
                }
            }
            throw new OrekitException(OrekitMessages.DIMENSION_INCONSISTENT_WITH_PARAMETERS,
                                      dimension, sBuilder.toString());
        }

    }

}
