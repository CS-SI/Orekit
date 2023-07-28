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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;

/** Modifier that sets estimated measurement weight to 0 if residual is too far from expected domain.
 * The "dynamic" aspect comes from the fact that the value of sigma can be changed on demand.
 * This is mainly used when searching for outliers in Kalman filters' prediction phase.
 * The value of sigma is then set to the square root of the diagonal of the matrix (H.Ppred.Ht+R)
 * Note that in the case of the Kalman filter we use the "iteration" word to represent the number of
 * measurements processed by the filter so far.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.2
 */
public class DynamicOutlierFilter<T extends ObservedMeasurement<T>> extends OutlierFilter<T> {
    /** Current value of sigma. */
    private double[] sigma;

    /** Simple constructor.
     * @param warmup number of iterations before with filter is not applied
     * @param maxSigma detection limit for outlier
     */
    public DynamicOutlierFilter(final int warmup,
                                final double maxSigma) {
        super(warmup, maxSigma);
        this.sigma = null;
    }

    /** Get the current value of sigma.
     * @return The current value of sigma
     */
    public double[] getSigma() {
        return sigma == null ? null : sigma.clone();
    }

    /** Set the current value of sigma.
     * @param sigma The value of sigma to set
     */
    public void setSigma(final double[] sigma) {
        this.sigma = sigma == null ? null : sigma.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<T> estimated) {

        // Do not apply the filter if current iteration/measurement is lower than
        // warmup attribute or if the attribute sigma has not been initialized yet
        if (estimated.getIteration() > getWarmup() && sigma != null) {

            final double[] observed    = estimated.getObservedMeasurement().getObservedValue();
            final double[] theoretical = estimated.getEstimatedValue();

            // Check that the dimension of sigma array is consistent with the measurement
            if (observed.length != sigma.length) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                       sigma.length, getSigma().length);
            }

            // Check if observed value is not too far from estimation
            for (int i = 0; i < observed.length; ++i) {
                if (FastMath.abs(observed[i] - theoretical[i]) > getMaxSigma() * sigma[i]) {
                    // observed value is too far, reject measurement
                    estimated.setStatus(EstimatedMeasurement.Status.REJECTED);
                }
            }
        }

    }
}
