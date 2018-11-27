/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;


/** Base class for {@link MeasurementBuilder measurements builders}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public abstract class AbstractMeasurementBuilder<T extends ObservedMeasurement<T>> implements MeasurementBuilder<T> {

    /** Noise source (may be null). */
    private final CorrelatedRandomVectorGenerator noiseSource;

    /** Modifiers that apply to the measurement.*/
    private final List<EstimationModifier<T>> modifiers;

    /** Theoretical standard deviation. */
    private final double[] sigma;

    /** Base weight. */
    private final double[] baseWeight;

    /** Indices of the propagators related to this measurement. */
    private final int[] propagatorsIndices;

    /** Simmple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorsIndices indices of the propagators related to this measurement
     */
    protected AbstractMeasurementBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                         final double sigma, final double baseWeight,
                                         final int... propagatorsIndices) {
        this(noiseSource,
             new double[] {
                 sigma
             }, new double[] {
                 baseWeight
             }, propagatorsIndices);
    }

    /** Simmple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorsIndices indices of the propagators related to this measurement
     */
    protected AbstractMeasurementBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                                         final double[] sigma, final double[] baseWeight,
                                         final int... propagatorsIndices) {
        this.noiseSource        = noiseSource;
        this.modifiers          = new ArrayList<>();
        this.sigma              = sigma.clone();
        this.baseWeight         = baseWeight.clone();
        this.propagatorsIndices = propagatorsIndices.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void addModifier(final EstimationModifier<T> modifier) {
        modifiers.add(modifier);
    }

    /** {@inheritDoc} */
    @Override
    public List<EstimationModifier<T>> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    /** Generate a noise vector.
     * @return noise vector (null if we generate perfect measurements)
     */
    protected double[] getNoise() {
        return noiseSource == null ? null : noiseSource.nextVector();
    }

    /** Get the theoretical standard deviation.
     * <p>
     * The theoretical standard deviation is a theoretical value
     * used for normalizing the residuals. It acts as a weighting
     * factor to mix appropriately measurements with different units
     * and different accuracy. The value has the same dimension as
     * the measurement itself (i.e. when a residual is divided by
     * this value, it becomes dimensionless).
     * </p>
     * @return expected standard deviation
     * @see #getBaseWeight()
     */
    protected double[] getTheoreticalStandardDeviation() {
        return sigma.clone();
    }

    /** Get the base weight associated with the measurement
     * <p>
     * The base weight is used on residuals already normalized thanks to
     * {@link #getTheoreticalStandardDeviation()} to increase or
     * decrease relative effect of some measurements with respect to
     * other measurements. It is a dimensionless value, typically between
     * 0 and 1 (but it can really have any non-negative value).
     * </p>
     * @return base weight
     * @see #getTheoreticalStandardDeviation()
     * @see EstimatedMeasurement#getCurrentWeight()
     */
    protected double[] getBaseWeight() {
        return baseWeight.clone();
    }

    /** Get the indices of the {@link org.orekit.propagation.Propagator propagators}
     * related to this measurement.
     * <p>
     * The propagators are indexed starting from 0 and ordered according to
     * the order of the {@link org.orekit.propagation.conversion.PropagatorBuilder
     * propagators builders} in the orbit determination engine used.
     * </p>
     * @return indices of the {@link org.orekit.propagation.Propagator propagators}
     * related to this measurement
     * @since 9.0
     */
    protected int[] getPropagatorsIndices() {
        return propagatorsIndices.clone();
    }

}
