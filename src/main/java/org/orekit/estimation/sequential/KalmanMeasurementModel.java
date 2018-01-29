/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;

/** Basic model handling measurement related functions to use with a {@link KalmanEstimator}
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanMeasurementModel {


    /** Default constructor. */
    public KalmanMeasurementModel() {}

    /** Get the status of a measurement.<p>
     *   - true : the measurement will be processed by the Kalman filter <p>
     *   - false: the measurement will be rejected by the Kalman filter
     * @param estimatedMeasurement measurement to test for its status
     * @return the status of the measurement
     */
    <T extends ObservedMeasurement<T>> boolean getMeasurementStatus(final EstimatedMeasurement<T> estimatedMeasurement) {

        // Init status to false
        boolean measurementStatus = false;
        
        // Get measurement weights. They were set to 0 if the measurement was rejected
        // by an outlier filter
        final double[] weights = estimatedMeasurement.getCurrentWeight();

        // Loop on the weights, if they're all nil the measurement is rejected
        for (int i = 0; i < weights.length; i++) {
            measurementStatus = measurementStatus || (weights[i] > 0.);
        }
        return measurementStatus;
    }

    /** Return the normalized residuals of an estimated measurement.
     * @param estimatedMeasurement the measurement
     * @return the residuals
     */
    <T extends ObservedMeasurement<T>> RealVector getResiduals(final EstimatedMeasurement<T> estimatedMeasurement) {

        final double[] observed  = estimatedMeasurement.getObservedMeasurement().getObservedValue();
        final double[] estimated = estimatedMeasurement.getEstimatedValue();
        final double[] sigma     = estimatedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();
        final double[] residuals = new double[observed.length];

        for (int i = 0; i < observed.length; i++) {
            residuals[i] = (observed[i] - estimated[i]) / sigma[i];
        }
        return MatrixUtils.createRealVector(residuals);
    }

 
    /** Return the normalized measurement noise matrix of a measurement. <p>
     * The "physical" measurement noise matrix is the covariance matrix of the measurement.<p>
     * Normalizing it consists in applying the following equation: Rn[i,j] =  R[i,j]/σ[i]/σ[j]<p>
     * Thus the normalized measurement noise matrix is the matrix of the correlation coefficients 
     * between the different components of the measurement.
     * @param observedMeasurement the measurement
     * @return the normalized measurement noise matrix
     */
    <T extends ObservedMeasurement<T>> RealMatrix getMeasurementNoiseMatrix(final ObservedMeasurement<T> observedMeasurement) {

        // Normalized measurement noise matrix contains 1 on its diagonal and correlation coefficients
        // of the measurement on its non-diagonal elements.
        // TODO: Introduce the correlation coefficients for PV in Orekit
        // Here we have 0 for all correlation coefficients
        // The "physical" measurement noise matrix is the covariance matrix of the measurement
        // Normalizing it leaves us with the matrix of the correlation coefficients

        // Set up an identity matrix of proper size
        return MatrixUtils.createRealIdentityMatrix(observedMeasurement.getDimension());
    }
    
    /** Set and apply a dynamic outlier filter on a measurement.<p>
     * Loop on the modifiers to see if a dynamic outlier filter needs to be applied.<p>
     * Compute the sigma array using the matrix in input and set the filter.<p>
     * Apply the filter by calling the modify method on the estimated measurement.<p>
     * Reset the filter.
     * @param estimatedMeasurement measurement to filter
     * @param innovationCovarianceMatrix So called innovation covariance matrix S, with:<p>
     *        S = H.Ppred.Ht + R<p>
     *        Where:<p>
     *         - H is the normalized measurement matrix (Ht its transpose)<p>
     *         - Ppred is the normalized predicted covariance matrix<p>
     *         - R is the normalized measurement noise matrix
     * @throws OrekitException if modifier cannot be applied
     */
    <T extends ObservedMeasurement<T>> void applyDynamicOutlierFilter(final EstimatedMeasurement<T> estimatedMeasurement,
                                                                      final RealMatrix innovationCovarianceMatrix) 
        throws OrekitException {

        // Observed measurement associated to the predicted measurement
        final ObservedMeasurement<T> observedMeasurement = estimatedMeasurement.getObservedMeasurement();

        // Check if a dynamic filter was added to the measurement
        // If so, update its sigma value and apply it
        for (EstimationModifier<T> modifier : observedMeasurement.getModifiers()) {
            if (modifier instanceof DynamicOutlierFilter<?>) {
                final DynamicOutlierFilter<T> dynamicOutlierFilter = (DynamicOutlierFilter<T>) modifier;

                // Initialize the values of the sigma array used in the dynamic filter
                final double[] sigmaDynamic     = new double[innovationCovarianceMatrix.getColumnDimension()];
                final double[] sigmaMeasurement = observedMeasurement.
                                getTheoreticalStandardDeviation();

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
                    sigmaDynamic[i] = FastMath.sqrt(innovationCovarianceMatrix.getEntry(i, i)) *
                                    sigmaMeasurement[i];
                }
                dynamicOutlierFilter.setSigma(sigmaDynamic);

                // Apply the modifier on the estimated measurement
                modifier.modify(estimatedMeasurement);

                // Re-initialize the value of the filter for the next measurement of the same type
                dynamicOutlierFilter.setSigma(null);
            }
        }
    }
}
