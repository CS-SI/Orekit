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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Class modeling a measurement bias.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public class Bias<T extends ObservedMeasurement<T>> implements EstimationModifier<T> {

    /** Parameters holding the bias value components. */
    private final List<ParameterDriver> drivers;

    /** Partial derivatives. */
    private final double[][] derivatives;

    /** Simple constructor.
     * @param name name of the bias
     * @param bias reference value of the bias
     * @param scale scale of the bias, for normalization
     * @param min minimum value of the bias
     * @param max maximum value of the bias
     */
    public Bias(final String[] name, final double[] bias, final double[] scale,
                final double[] min, final double[] max) {

        drivers = new ArrayList<>(bias.length);
        for (int i = 0; i < bias.length; ++i) {
            drivers.add(new ParameterDriver(name[i], bias[i], scale[i], min[i], max[i]));
        }

        derivatives = new double[bias.length][bias.length];
        for (int i = 0; i < bias.length; ++i) {
            // derivatives are computed with respect to the physical parameters,
            // not with respect to the normalized parameters (normalization is
            // performed later on), so the derivative is really 1.0 and not scale[i]
            derivatives[i][i] = 1.0;
        }

    }

    /** {@inheritDoc}
     * <p>
     * For a bias, there are {@link ObservedMeasurement#getDimension()} parameter drivers,
     * sorted in components order.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(drivers);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated) {

        // apply the bias to the measurement value
        final double[] value = estimated.getEstimatedValue();
        int nb = 0;
        for (int i = 0; i < drivers.size(); ++i) {
            final ParameterDriver driver = drivers.get(i);
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                value[nb++] += driver.getValue(span.getStart());
            }
        }
        estimated.setEstimatedValue(value);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<T> estimated) {

        // apply the bias to the measurement value
        int nb = 0;
        for (int i = 0; i < drivers.size(); ++i) {
            final ParameterDriver driver = drivers.get(i);
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                if (driver.isSelected()) {
                    // add the partial derivatives
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[nb++]);
                }
            }
        }

        modifyWithoutDerivatives(estimated);

    }

}
