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

import java.util.Collections;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriver;

/** Modifier that sets estimated measurement weight to 0 if residual is too far from expected domain.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public class OutlierFilter<T extends ObservedMeasurement<T>> implements EstimationModifier<T> {

    /** Warmup iterations. */
    private final int warmup;

    /** Outlier detection limit. */
    private final double maxSigma;

    /** Simple constructor.
     * @param warmup number of iterations before with filter is not applied
     * @param maxSigma detection limit for outliers.
     */
    public OutlierFilter(final int warmup, final double maxSigma) {
        this.warmup   = warmup;
        this.maxSigma = maxSigma;
    }

    /** Get the value of warmup iterations.
     * @return the value of warmup iterations
     */
    protected int getWarmup() {
        return warmup;
    }

    /** Get the value of the outlier detection limit.
     *  @return the value of the outlier detection limit
     */
    protected double getMaxSigma() {
        return maxSigma;
    }
    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated) {

        if (estimated.getIteration() > warmup) {

            // check if observed value is far to estimation
            final double[] observed    = estimated.getObservedMeasurement().getObservedValue();
            final double[] theoretical = estimated.getEstimatedValue();
            final double[] sigma       = estimated.getObservedMeasurement().getTheoreticalStandardDeviation();
            for (int i = 0; i < observed.length; ++i) {
                if (FastMath.abs(observed[i] - theoretical[i]) > maxSigma * sigma[i]) {
                    // observed value is too far, reject measurement
                    estimated.setStatus(EstimatedMeasurement.Status.REJECTED);
                }
            }
        }

    }

}
