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

import java.util.Arrays;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/**
 * Utility class for Kalman Filter.
 * <p>
 * This class includes common methods used by the different Kalman
 * models in Orekit (i.e., Extended, Unscented, and Semi-analytical)
 * </p>
 * @since 11.3
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
     * @return decorated measurement
     */
    public static MeasurementDecorator decorate(final ObservedMeasurement<?> observedMeasurement,
                                                final AbsoluteDate referenceDate) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // Indeed, the "physical" measurement noise matrix is the covariance matrix of the measurement
        // Normalizing it leaves us with the matrix of the correlation coefficients
        final RealMatrix covariance;
        if (observedMeasurement.getMeasurementType().equals(PV.MEASUREMENT_TYPE)) {
            // For PV measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final PV pv = (PV) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(pv.getCorrelationCoefficientsMatrix());
        } else if (observedMeasurement.getMeasurementType().equals(Position.MEASUREMENT_TYPE)) {
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

    /** Decorate an observed measurement for an Unscented Kalman Filter.
     * <p>
     * This method uses directly the measurement's covariance matrix, without any normalization.
     * </p>
     * @param observedMeasurement the measurement
     * @param referenceDate reference date
     * @return decorated measurement
     * @since 11.3.2
     */
    public static MeasurementDecorator decorateUnscented(final ObservedMeasurement<?> observedMeasurement,
                                                         final AbsoluteDate referenceDate) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // Indeed, the "physical" measurement noise matrix is the covariance matrix of the measurement

        final RealMatrix covariance;
        if (observedMeasurement.getMeasurementType().equals(PV.MEASUREMENT_TYPE)) {
            // For PV measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final PV pv = (PV) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(pv.getCovarianceMatrix());
        } else if (observedMeasurement.getMeasurementType().equals(Position.MEASUREMENT_TYPE)) {
            // For Position measurements we do have a covariance matrix and thus a correlation coefficients matrix
            final Position position = (Position) observedMeasurement;
            covariance = MatrixUtils.createRealMatrix(position.getCovarianceMatrix());
        } else {
            // For other measurements we do not have a covariance matrix.
            // Thus the correlation coefficients matrix is an identity matrix.
            covariance = MatrixUtils.createRealIdentityMatrix(observedMeasurement.getDimension());
            final double[] sigma = observedMeasurement.getTheoreticalStandardDeviation();
            for (int i = 0; i < sigma.length; i++) {
                covariance.setEntry(i, i, sigma[i] * sigma[i]);
            }

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

        // count parameters
        int requiredDimension = 0;
        for (final ParameterDriver driver : orbitalParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++requiredDimension;
            }
        }
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

    /** Filter relevant states for a measurement.
     * @param observedMeasurement measurement to consider
     * @param allStates all states
     * @return array containing only the states relevant to the measurement
     */
    public static SpacecraftState[] filterRelevant(final ObservedMeasurement<?> observedMeasurement,
                                                   final SpacecraftState[] allStates) {
        final List<ObservableSatellite> satellites = observedMeasurement.getSatellites();
        final SpacecraftState[] relevantStates = new SpacecraftState[satellites.size()];
        for (int i = 0; i < relevantStates.length; ++i) {
            relevantStates[i] = allStates[satellites.get(i).getPropagatorIndex()];
        }
        return relevantStates;
    }

    /** Set and apply a dynamic outlier filter on a measurement.<p>
     * Loop on the modifiers to see if a dynamic outlier filter needs to be applied.<p>
     * Compute the sigma array using the matrix in input and set the filter.<p>
     * Apply the filter by calling the modify method on the estimated measurement.<p>
     * Reset the filter.
     * @param measurement measurement to filter
     * @param innovationCovarianceMatrix So called innovation covariance matrix S, with:<p>
     *        S = H.Ppred.Ht + R<p>
     *        Where:<p>
     *         - H is the normalized measurement matrix (Ht its transpose)<p>
     *         - Ppred is the normalized predicted covariance matrix<p>
     *         - R is the normalized measurement noise matrix
     * @param <T> the type of measurement
     */
    public static <T extends ObservedMeasurement<T>> void applyDynamicOutlierFilter(final EstimatedMeasurement<T> measurement,
                                                                                    final RealMatrix innovationCovarianceMatrix) {

        // Observed measurement associated to the predicted measurement
        final ObservedMeasurement<T> observedMeasurement = measurement.getObservedMeasurement();

        // Check if a dynamic filter was added to the measurement
        // If so, update its sigma value and apply it
        for (EstimationModifier<T> modifier : observedMeasurement.getModifiers()) {
            if (modifier instanceof DynamicOutlierFilter<?>) {
                final DynamicOutlierFilter<T> dynamicOutlierFilter = (DynamicOutlierFilter<T>) modifier;

                // Initialize the values of the sigma array used in the dynamic filter
                final double[] sigmaDynamic     = new double[innovationCovarianceMatrix.getColumnDimension()];
                final double[] sigmaMeasurement = observedMeasurement.getTheoreticalStandardDeviation();

                // Set the sigma value for each element of the measurement
                // Here we do use the value suggested by David A. Vallado (see [1]§10.6):
                // sigmaDynamic[i] = sqrt(diag(S))*sigma[i]
                // With S = H.Ppred.Ht + R
                // Where:
                //  - S is the measurement error matrix in input
                //  - H is the normalized measurement matrix (Ht its transpose)
                //  - Ppred is the normalized predicted covariance matrix
                //  - R is the normalized measurement noise matrix
                //  - sigma[i] is the theoretical standard deviation of the ith component of the measurement.
                //    It is used here to un-normalize the value before it is filtered
                for (int i = 0; i < sigmaDynamic.length; i++) {
                    sigmaDynamic[i] = FastMath.sqrt(innovationCovarianceMatrix.getEntry(i, i)) * sigmaMeasurement[i];
                }
                dynamicOutlierFilter.setSigma(sigmaDynamic);

                // Apply the modifier on the estimated measurement
                modifier.modify(measurement);

                // Re-initialize the value of the filter for the next measurement of the same type
                dynamicOutlierFilter.setSigma(null);
            }
        }
    }

    /**
     * Compute the unnormalized innovation vector from the given predicted measurement.
     * @param predicted predicted measurement
     * @return the innovation vector
     */
    public static RealVector computeInnovationVector(final EstimatedMeasurement<?> predicted) {
        final double[] sigmas = new double[predicted.getObservedMeasurement().getDimension()];
        Arrays.fill(sigmas, 1.0);
        return computeInnovationVector(predicted, sigmas);
    }

    /**
     * Compute the normalized innovation vector from the given predicted measurement.
     * @param predicted predicted measurement
     * @param sigma measurement standard deviation
     * @return the innovation vector
     */
    public static RealVector computeInnovationVector(final EstimatedMeasurement<?> predicted, final double[] sigma) {

        if (predicted.getStatus() == EstimatedMeasurement.Status.REJECTED)  {
            // set innovation to null to notify filter measurement is rejected
            return null;
        } else {
            // Normalized innovation of the measurement (Nx1)
            final double[] observed  = predicted.getObservedMeasurement().getObservedValue();
            final double[] estimated = predicted.getEstimatedValue();
            final double[] residuals = new double[observed.length];

            for (int i = 0; i < observed.length; i++) {
                residuals[i] = (observed[i] - estimated[i]) / sigma[i];
            }
            return MatrixUtils.createRealVector(residuals);
        }

    }

}
