/* Copyright 2002-2024 CS GROUP
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
import org.orekit.utils.TimeSpanMap.Span;

/**
 * Base class for phase ambiguity modifier.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 10.3
 * @deprecated as of 12.1 ambiguity is managed directly by raw measurements
 * {@link org.orekit.estimation.measurements.gnss.Phase},
 * {@link org.orekit.estimation.measurements.gnss.OneWayGNSSPhase}
 * and {@link org.orekit.estimation.measurements.gnss.InterSatellitesPhase}
 */
@Deprecated
public class AbstractAmbiguityModifier {

    /** Ambiguity scale factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double AMBIGUITY_SCALE = FastMath.scalb(1.0, 26);

    /** Ambiguity parameter. */
    private final ParameterDriver ambiguity;

    /** Constructor.
     * @param key key to identify the ambiguity
     * @param ambiguity initial value of ambiguity
     */
    public AbstractAmbiguityModifier(final int key, final double ambiguity) {
        this.ambiguity = new ParameterDriver("ambiguity-" + key, ambiguity, AMBIGUITY_SCALE,
                                             Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Get the drivers for this modifier.
     * @return drivers for this modifier
     */
    protected List<ParameterDriver> getDrivers() {
        return Collections.singletonList(ambiguity);
    }

    /** Modify measurement.
     * @param <T> type of the measurements
     * @param modifier applied modifier
     * @param estimated measurement to modify
     * @since 12.1
     */
    protected <T extends ObservedMeasurement<T>> void doModifyWithoutDerivatives(final EstimationModifier<T> modifier,
                                                                                 final EstimatedMeasurementBase<T> estimated) {
        // Apply the ambiguity to the measurement value
        for (Span<String> span = ambiguity.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
            final double[] value = estimated.getEstimatedValue();
            value[0] += ambiguity.getValue(span.getStart());
            estimated.modifyEstimatedValue(modifier, value);
        }
    }

    /** Modify measurement.
     * @param <T> type of the measurements
     * @param modifier applied modifier
     * @param estimated measurement to modify
     * @since 12.1
     */
    protected <T extends ObservedMeasurement<T>> void doModify(final EstimationModifier<T> modifier,
                                                               final EstimatedMeasurement<T> estimated) {

        // apply the ambiguity to the measurement derivatives
        for (Span<String> span = ambiguity.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
            if (ambiguity.isSelected()) {
            // add the partial derivatives
                estimated.setParameterDerivatives(ambiguity, span.getStart(), 1.0);
            }
        }

        // apply the ambiguity to the measurement value
        doModifyWithoutDerivatives(modifier, estimated);

    }

}
