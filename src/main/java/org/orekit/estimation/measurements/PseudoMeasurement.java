/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements;

import java.util.Collections;

import org.orekit.time.AbsoluteDate;

/** Abstract class modeling a position(-velocity) measurement,
 * referred to as pseudo because it is not based on any signals.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class PseudoMeasurement<T extends PseudoMeasurement<T>> extends AbstractMeasurement<T> {

    /** Constructor with full covariance matrix and all inputs.
     * <p>The fact that the covariance matrix is symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param observed measurement value
     * @param measurementQuality measurement quality data
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    protected PseudoMeasurement(final AbsoluteDate date, final double[] observed,
                                final MeasurementQuality measurementQuality, final ObservableSatellite satellite) {
        super(date, observed, measurementQuality, Collections.singletonList(satellite));
    }

    /** Get the covariance matrix.
     * @return the covariance matrix
     */
    public double[][] getCovarianceMatrix() {
        return getMeasurementQuality().getCovarianceMatrix();
    }

    /** Get the correlation coefficients matrix.
     * <p>This is the square, symmetric matrix M such that:
     * <p>Mij = Pij/(σi.σj)
     * <p>Where:
     * <ul>
     * <li>P is the covariance matrix
     * <li>σi is the i-th standard deviation (σi² = Pii)
     * </ul>
     * @return the correlation coefficient matrix (3x3)
     */
    public double[][] getCorrelationCoefficientsMatrix() {
        return getMeasurementQuality().getCorrelationCoefficientsMatrix();
    }

}
