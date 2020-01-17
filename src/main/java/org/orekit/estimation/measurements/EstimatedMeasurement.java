/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class holding an estimated theoretical value associated to an {@link ObservedMeasurement observed measurement}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public class EstimatedMeasurement<T extends ObservedMeasurement<T>> implements ComparableMeasurement {

    /** Associated observed measurement. */
    private final T observedMeasurement;

    /** Iteration number. */
    private final int iteration;

    /** Evaluations counter. */
    private final int count;

    /** States of the spacecrafts. */
    private final SpacecraftState[] states;

    /** Coordinates of the participants in signal travel order. */
    private final TimeStampedPVCoordinates[] participants;

    /** Estimated value. */
    private double[] estimatedValue;

    /** Measurement status. */
    private Status status;

    /** Partial derivatives with respect to states. */
    private double[][][] stateDerivatives;

    /** Partial derivatives with respect to parameters. */
    private final Map<ParameterDriver, double[]> parametersDerivatives;

    /** Simple constructor.
     * @param observedMeasurement associated observed measurement
     * @param iteration iteration number
     * @param count evaluations counter
     * @param states states of the spacecrafts
     * @param participants coordinates of the participants in signal travel order
     * in inertial frame
     */
    public EstimatedMeasurement(final T observedMeasurement,
                                final int iteration, final int count,
                                final SpacecraftState[] states,
                                final TimeStampedPVCoordinates[] participants) {
        this.observedMeasurement   = observedMeasurement;
        this.iteration             = iteration;
        this.count                 = count;
        this.states                = states.clone();
        this.participants          = participants.clone();
        this.status                = Status.PROCESSED;
        this.stateDerivatives      = new double[states.length][][];
        this.parametersDerivatives = new IdentityHashMap<ParameterDriver, double[]>();
    }

    /** Get the associated observed measurement.
     * @return associated observed measurement
     */
    public T getObservedMeasurement() {
        return observedMeasurement;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return observedMeasurement.getDate();
    }

    /** Get the iteration number.
     * @return iteration number
     */
    public int getIteration() {
        return iteration;
    }

    /** Get the evaluations counter.
     * @return evaluations counter
     */
    public int getCount() {
        return count;
    }

    /** Get the states of the spacecrafts.
     * @return states of the spacecrafts
     */
    public SpacecraftState[] getStates() {
        return states.clone();
    }

    /** Get the coordinates of the measurements participants in signal travel order.
     * <p>
     * First participant (at index 0) emits the signal (it is for example a ground
     * station for two-way range measurement). Last participant receives the signal
     * (it is also the ground station for two-way range measurement, but a few
     * milliseconds later). Intermediate participants relfect the signal (it is the
     * spacecraft for two-way range measurement).
     * </p>
     * @return coordinates of the measurements participants in signal travel order
     * in inertial frame
     */
    public TimeStampedPVCoordinates[] getParticipants() {
        return participants.clone();
    }

    /** Get the time offset from first state date to measurement date.
     * @return time offset from first state date to measurement date
     */
    public double getTimeOffset() {
        return observedMeasurement.getDate().durationFrom(states[0].getDate());
    }

    /** {@inheritDoc} */
    @Override
    public double[] getObservedValue() {
        return observedMeasurement.getObservedValue();
    }

    /** Get the estimated value.
     * @return estimated value
     */
    public double[] getEstimatedValue() {
        return estimatedValue.clone();
    }

    /** Set the estimated value.
     * @param estimatedValue estimated value
     */
    public void setEstimatedValue(final double... estimatedValue) {
        this.estimatedValue = estimatedValue.clone();
    }

    /** Get the status.
     * <p>
     * The status is set to {@link Status#PROCESSED PROCESSED} at construction, and
     * can be reset to {@link Status#REJECTED REJECTED} later on, typically by
     * {@link org.orekit.estimation.measurements.modifiers.OutlierFilter OutlierFilter}
     * or {@link org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter DynamicOutlierFilter}
     * </p>
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /** Set the status.
     * @param status status to set
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /** Get state size.
     * <p>
     * Warning, the {@link #setStateDerivatives(int, double[][])}
     * method must have been called before this method is called.
     * </p>
     * @return state size
     * @since 10.1
     */
    public int getStateSize() {
        return stateDerivatives[0][0].length;
    }

    /** Get the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param index index of the state, according to the {@code states}
     * passed at construction
     * @return partial derivatives of the simulated value (array of size
     * {@link ObservedMeasurement#getDimension() dimension} x 6)
     */
    public double[][] getStateDerivatives(final int index) {
        final double[][] sd = new double[observedMeasurement.getDimension()][];
        for (int i = 0; i < observedMeasurement.getDimension(); ++i) {
            sd[i] = stateDerivatives[index][i].clone();
        }
        return sd;
    }

    /** Set the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param index index of the state, according to the {@code states}
     * passed at construction
     * @param derivatives partial derivatives with respect to state
     */
    public void setStateDerivatives(final int index, final double[]... derivatives) {
        this.stateDerivatives[index] = new double[observedMeasurement.getDimension()][];
        for (int i = 0; i < observedMeasurement.getDimension(); ++i) {
            this.stateDerivatives[index][i] = derivatives[i].clone();
        }
    }

    /** Get all the drivers with set derivatives.
     * @return all the drivers with set derivatives
     * @since 9.0
     */
    public Stream<ParameterDriver> getDerivativesDrivers() {
        return parametersDerivatives.entrySet().stream().map(entry -> entry.getKey());
    }

    /** Get the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to a parameter.
     * @param driver driver for the parameter
     * @return partial derivatives of the simulated value
     * @exception OrekitIllegalArgumentException if parameter is unknown
     */
    public double[] getParameterDerivatives(final ParameterDriver driver)
        throws OrekitIllegalArgumentException {
        final double[] p = parametersDerivatives.get(driver);
        if (p == null) {
            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<ParameterDriver, double[]> entry : parametersDerivatives.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey().getName());
            }
            throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                     driver.getName(),
                                                     builder.length() > 0 ? builder.toString() : "<none>");
        }
        return p;
    }

    /** Set the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to parameter.
     * @param driver driver for the parameter
     * @param parameterDerivatives partial derivatives with respect to parameter
     */
    public void setParameterDerivatives(final ParameterDriver driver, final double... parameterDerivatives) {
        parametersDerivatives.put(driver, parameterDerivatives);
    }

    /** Enumerate for the status of the measurement. */
    public enum Status {

        /** Status for processed measurements. */
        PROCESSED,

        /** Status for rejected measurements. */
        REJECTED;

    }

}
