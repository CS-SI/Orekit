/* Copyright 2002-2015 CS Systèmes d'Information
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
import java.util.Map;

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
 * MeasurementModifier MeasurementModifier} objects. These objects will manage notions
 * like tropospheric delays, biases, ...
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public interface Measurement extends TimeStamped {

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

    /** Get the simulated value.
     * <p>
     * The simulated value is the <em>addition</em> of the raw theoretical
     * value and all the modifiers that apply to the measurement.
     * </p>
     * @param state orbital state at measurement date
     * @param parameters model parameters map (the map keys are the
     * parameters names)
     * @return simulated value (array of size {@link #getDimension()}
     * @exception OrekitException if value cannot be computed
     */
    double[] getSimulatedValue(SpacecraftState state, Map<String, Parameter> parameters)
        throws OrekitException;

    /** Get the observed value.
     * <p>
     * The observed value is the value that was measured by the instrument.
     * </p>
     * @return observed value (array of size {@link #getDimension()}
     */
    double[] getObservedValue();

    /** Get the partial derivatives of the {@link #getSimulatedValue(SpacecraftState, Map)}
     * simulated measurement with respect to current state Cartesian coordinates.
     * @param state orbital state at measurement date
     * @param parameters model parameters map
     * @return partial derivatives of the simulated value (array of size
     * {@link #getDimension() x 6}
     * @exception OrekitException if derivatives cannot be computed
     */
    double[][] getPartialDerivatives(SpacecraftState state, Map<String, double[]> parameters)
        throws OrekitException;

    /** Add a modifier.
     * <p>
     * The modifiers are applied in the order in which they are added in order to
     * compute the {@link #getSimulatedValue(SpacecraftState, Map) simulated value}.
     * </p>
     * @param modifier modifier to add
     * @see #getModifiers()
     */
    void addModifier(MeasurementModifier modifier);

    /** Get the modifiers that apply to a measurement.
     * @return modifiers that apply to a measurement
     * @see #addModifier(MeasurementModifier)
     */
    List<MeasurementModifier> getModifiers();

}
