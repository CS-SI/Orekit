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
package org.orekit.estimation.measurements;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class holding an estimated theoretical value associated to an {@link ObservedMeasurement observed measurement}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public class EstimatedMeasurementBase<T extends ObservedMeasurement<T>> implements ComparableMeasurement {

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

    /** Simple constructor.
     * @param observedMeasurement associated observed measurement
     * @param iteration iteration number
     * @param count evaluations counter
     * @param states states of the spacecrafts
     * @param participants coordinates of the participants in signal travel order
     * in inertial frame
     */
    public EstimatedMeasurementBase(final T observedMeasurement,
                                final int iteration, final int count,
                                final SpacecraftState[] states,
                                final TimeStampedPVCoordinates[] participants) {
        this.observedMeasurement = observedMeasurement;
        this.iteration           = iteration;
        this.count               = count;
        this.states              = states.clone();
        this.participants        = participants.clone();
        this.status              = Status.PROCESSED;
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

    /** Enumerate for the status of the measurement. */
    public enum Status {

        /** Status for processed measurements. */
        PROCESSED,

        /** Status for rejected measurements. */
        REJECTED;

    }

}
