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

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.TimeStamped;


/** Interface for measurements used for orbit determination.
 * <p>
 * The most important methods of this interface allow to:
 * <ul>
 *   <li>get the observed value,</li>
 *   <li>compute the theoretical value of a measurement,</li>
 *   <li>compute the corresponding partial derivatives (with respect to state and parameters)</li>
 * </ul>
 * </p>
 * <p>
 * The theoretical values can be modified by registering one or several {@link
 * EvaluationModifier MeasurementModifier} objects. These objects will manage notions
 * like tropospheric delays, biases, ...
 * </p>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 7.2
 */
public interface Measurement<T extends Measurement<T>> extends TimeStamped {

    /** Enable or disable a measurement.
     * <p>
     * Disabling a measurement allow to not consider it at
     * one stage of the orbit determination (for example when
     * it appears to be an outlier as per current estimated
     * covariance).
     * </p>
     * @param enabled if true the measurement will be enabled,
     * otherwise it will be disabled
     */
    void setEnabled(boolean enabled);

    /** Check if a measurement is enabled.
     * @return true if the measurement is enabled
     */
    boolean isEnabled();

    /** Get the dimension of the measurement.
     * <p>
     * Dimension is the size of the array containing the
     * value. It will be one for a scalar measurement like
     * a range or range-rate, but 6 for a position-velocity
     * measurement.
     * </p>
     * @return dimension of the measurement
     */
    int getDimension();

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
    double[] getTheoreticalStandardDeviation();

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
     * @see Evaluation#getCurrentWeight()
     */
    double[] getBaseWeight();

    /** Get the observed value.
     * <p>
     * The observed value is the value that was measured by the instrument.
     * </p>
     * @return observed value (array of size {@link #getDimension()}
     */
    double[] getObservedValue();

    /** Add a modifier.
     * <p>
     * The modifiers are applied in the order in which they are added in order to
     * {@link #evaluate(SpacecraftState) evaluate} the measurement.
     * </p>
     * @param modifier modifier to add
     * @see #getModifiers()
     */
    void addModifier(EvaluationModifier<T> modifier);

    /** Get the modifiers that apply to a measurement.
     * @return modifiers that apply to a measurement
     * @see #addModifier(EvaluationModifier)
     */
    List<EvaluationModifier<T>> getModifiers();

    /** Get the parameters supported by this measurement, including its modifiers.
     * @return parameters supported by this measurement, including its modifiers
     */
    List<Parameter> getSupportedParameters();

    /** Evaluate the measurement.
     * <p>
     * The evaluated value is the <em>combination</em> of the raw theoretical
     * value and all the modifiers that apply to the measurement.
     * </p>
     * @param iteration iteration number
     * @param count evaluations counter
     * @param state orbital state at measurement date
     * @return evaluated measurement
     * @exception OrekitException if value cannot be computed
     */
    Evaluation<T> evaluate(int iteration, int count, SpacecraftState state)
        throws OrekitException;

}
