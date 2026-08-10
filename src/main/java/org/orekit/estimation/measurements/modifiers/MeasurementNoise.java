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
package org.orekit.estimation.measurements.modifiers;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriver;

/** Class modeling a measurement noise.
 * @param <T> the type of the measurement
 * @author Romain Serra
 * @since 14.0
 */
public class MeasurementNoise<T extends ObservedMeasurement<T>> implements EstimationModifier<T> {

    /** Random vector generator. */
    private final CorrelatedRandomVectorGenerator randomVectorGenerator;

    /** Simple constructor.
     * @param randomVectorGenerator noise generator assumed to have a consistent dimension with measurement
     */
    public MeasurementNoise(final CorrelatedRandomVectorGenerator randomVectorGenerator) {
        this.randomVectorGenerator = randomVectorGenerator;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "noise";
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated) {
        final double[] value = estimated.getEstimatedValue();
        final double[] noise = randomVectorGenerator.nextVector();
        for (int i = 0; i < value.length; ++i) {
            value[i] += noise[i];
        }
        estimated.modifyEstimatedValue(this, value);
    }
}
