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
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;


/** Interface for measurements used for orbit determination.
 * <p>
 * The most important methods of this interface allow to:
 * <ul>
 *   <li>get the observed value,</li>
 *   <li>estimate the theoretical value of a measurement,</li>
 *   <li>compute the corresponding partial derivatives (with respect to state and parameters)</li>
 * </ul>
 *
 * <p>
 * The estimated theoretical values can be modified by registering one or several {@link
 * EstimationModifier EstimationModifier} objects. These objects will manage notions
 * like tropospheric delays, biases, ...
 * </p>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface ObservedMeasurement<T extends ObservedMeasurement<T>> extends TimeStamped {

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
     * @see EstimatedMeasurement#getCurrentWeight()
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
     * {@link #estimate(int, int, SpacecraftState) evaluate} the measurement.
     * </p>
     * @param modifier modifier to add
     * @see #getModifiers()
     * @exception OrekitException if there is a conflict between measurement and
     * modifiers parameters
     */
    void addModifier(EstimationModifier<T> modifier) throws OrekitException;

    /** Get the modifiers that apply to a measurement.
     * @return modifiers that apply to a measurement
     * @see #addModifier(EstimationModifier)
     */
    List<EstimationModifier<T>> getModifiers();

    /** Get the drivers for this measurement parameters, including its modifiers parameters.
     * @return drivers for this measurement parameters, including its modifiers parameters
     */
    List<ParameterDriver> getParametersDrivers();

    /** Estimate the theoretical value of the measurement.
     * <p>
     * The estimated value is the <em>combination</em> of the raw estimated
     * value and all the modifiers that apply to the measurement.
     * </p>
     * @param iteration iteration number
     * @param evaluation evaluations number
     * @param state orbital state at measurement date
     * @return estimated measurement
     * @exception OrekitException if value cannot be computed
     */
    EstimatedMeasurement<T> estimate(int iteration, int evaluation, SpacecraftState state)
        throws OrekitException;

}
