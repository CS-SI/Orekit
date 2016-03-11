/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.utils.ParameterDriver;

/** Modifier that sets evaluation weight to 0 if residual is too far from expected domain.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 7.2
 */
public class OutlierFilter<T extends Measurement<T>> implements EvaluationModifier<T> {

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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final Evaluation<T> evaluation) {

        if (evaluation.getIteration() > warmup) {

            // check if evaluation is far to observed value
            final double[] observed    = evaluation.getMeasurement().getObservedValue();
            final double[] theoretical = evaluation.getValue();
            final double[] sigma       = evaluation.getMeasurement().getTheoreticalStandardDeviation();
            for (int i = 0; i < observed.length; ++i) {
                if (FastMath.abs(observed[i] - theoretical[i]) > maxSigma * sigma[i]) {
                    // evaluation is too far
                    // set current weight to 0.0
                    evaluation.setCurrentWeight(new double[observed.length]);
                }
            }
        }

    }

}
