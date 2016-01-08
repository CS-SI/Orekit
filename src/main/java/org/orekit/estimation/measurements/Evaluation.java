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

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;

/** Class holding a theoretical evaluation of one {@link Measurement measurement}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 7.1
 */
public class Evaluation<T extends Measurement<T>> {

    /** Associated measurement. */
    private final T measurement;

    /** Iteration number. */
    private final int iteration;

    /** State of the spacecraft. */
    private final SpacecraftState state;

    /** Simulated value. */
    private double[] value;

    /** Current weight. */
    private double[] currentWeight;

    /** Partial derivatives with respect to state. */
    private double[][] stateDerivatives;

    /** Partial derivatives with respect to parameters. */
    private final Map<String, double[][]> parametersDerivatives;

    /** Simple constructor.
     * @param measurement associated measurement
     * @param iteration iteration number
     * @param state state of the spacecraft
     */
    public Evaluation(final T measurement, final int iteration,
                      final SpacecraftState state) {
        this.measurement           = measurement;
        this.iteration             = iteration;
        this.state                 = state;
        this.parametersDerivatives = new HashMap<String, double[][]>();
    }

    /** Get the associated measurement.
     * @return associated measurement
     */
    public T getMeasurement() {
        return measurement;
    }

    /** Get the iteration number.
     * @return iteration number
     */
    public int getIteration() {
        return iteration;
    }

    /** Get the state of the spacecraft.
     * @return state of the spacecraft
     */
    public SpacecraftState getState() {
        return state;
    }

    /** Get the time offset from state date to measurement date.
     * @return time offset from state date to measurement date
     */
    public double getTimeOffset() {
        return measurement.getDate().durationFrom(state.getDate());
    }

    /** Get the simulated value.
     * @return simulated value
     */
    public double[] getValue() {
        return value.clone();
    }

    /** Set the simulated value.
     * @param value simulated value
     */
    public void setValue(final double ... value) {
        this.value = value.clone();
    }

    /** Get the current weight.
     * <p>
     * By default, the current weight is measurement {@link
     * Measurement#getBaseWeight() base weight}.
     * </p>
     * @return current weight
     */
    public double[] getCurrentWeight() {
        return currentWeight == null ? measurement.getBaseWeight() : currentWeight.clone();
    }

    /** Set the current weight.
     * @param currentWeight current weight
     */
    public void setCurrentWeight(final double ... currentWeight) {
        this.currentWeight = currentWeight.clone();
    }

    /** Get the partial derivatives of the {@link #getValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @return partial derivatives of the simulated value (array of size
     * {@link dimension x 6}
     */
    public double[][] getStateDerivatives() {
        final double[][] sd = new double[measurement.getDimension()][];
        for (int i = 0; i < measurement.getDimension(); ++i) {
            sd[i] = stateDerivatives[i].clone();
        }
        return sd;
    }

    /** Set the partial derivatives of the {@link #getValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param stateDerivatives partial derivatives with respect to state
     */
    public void setStateDerivatives(final double[] ... stateDerivatives) {
        this.stateDerivatives = new double[measurement.getDimension()][];
        for (int i = 0; i < measurement.getDimension(); ++i) {
            this.stateDerivatives[i] = stateDerivatives[i].clone();
        }
    }

    /** Get the partial derivatives of the {@link #getValue()
     * simulated measurement} with respect to a parameter.
     * @param name name of the parameter
     * @return partial derivatives of the simulated value
     * @exception OrekitIllegalArgumentException if parameter is unknown
     */
    public double[][] getParameterDerivatives(final String name)
        throws OrekitIllegalArgumentException {
        final double[][] p = parametersDerivatives.get(name);
        if (p == null) {
            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<String, double[][]> entry : parametersDerivatives.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey());
            }
            throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                     name,
                                                     builder.length() > 0 ? builder.toString() : "<none>");
        }
        final double[][] sd = new double[measurement.getDimension()][];
        for (int i = 0; i < measurement.getDimension(); ++i) {
            sd[i] = p[i].clone();
        }
        return sd;
    }

    /** Set the partial derivatives of the {@link #getValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param name name of the parameter
     * @param parameterDerivatives partial derivatives with respect to parameter
     */
    public void setParameterDerivatives(final String name, final double[] ... parameterDerivatives) {
        final double[][] p = new double[measurement.getDimension()][];
        for (int i = 0; i < measurement.getDimension(); ++i) {
            p[i] = parameterDerivatives[i].clone();
        }
        parametersDerivatives.put(name, p);
    }

}
