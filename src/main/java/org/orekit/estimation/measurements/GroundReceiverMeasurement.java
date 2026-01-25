/* Copyright 2002-2025 CS GROUP
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

import java.util.Collections;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/** Base class modeling a measurement where receiver is a ground station.
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 12.0
 * @param <T> type of the measurement
 */
public abstract class GroundReceiverMeasurement<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Ground station that receives signal from satellite. */
    private final GroundStation station;

    /** Simple constructor for scalar measurements.
     * @param station ground station from which measurement is performed
     * @param isTwoWay flag indicating whether it is a two-way measurement
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param observedValue observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    protected GroundReceiverMeasurement(final GroundStation station, final boolean isTwoWay, final AbsoluteDate date,
                                        final double[] observedValue, final double[] sigma, final double[] baseWeight,
                                        final SignalTravelTimeModel signalTravelTimeModel,
                                        final ObservableSatellite satellite) {
        super(date, isTwoWay, observedValue, sigma, baseWeight, signalTravelTimeModel, Collections.singletonList(satellite));

        addParametersDrivers(station.getParametersDrivers());

        this.station = station;
    }

    /** Get the ground station that receives the signal.
     * @return ground station
     */
    public final GroundStation getReceiverStation() {
        return station;
    }

    /**
     * Form the mapping between parameters' names and derivatives' indices.
     * @param states observables
     * @return map
     */
    protected Map<String, Integer> getParameterIndices(final SpacecraftState[] states) {
        return getReceiverStation().getParamaterIndices(states, getParametersDrivers());
    }

    /**
     * Compute actual reception date taking into account clock offset.
     * @return reception date
     */
    protected AbsoluteDate getCorrectedReceptionDate() {
        final ClockOffset localClock = getReceiverStation().getQuadraticClockModel().getOffset(getDate());
        return getDate().shiftedBy(-localClock.getOffset());
    }

    /**
     * Compute actual reception date taking into account clock offset.
     * @param nbParams number of independent variables for automatic differentiation
     * @param paramIndices mapping between parameter name and variable index
     * @return reception date
     */
    protected FieldAbsoluteDate<Gradient> getCorrectedReceptionDateField(final int nbParams,
                                                                         final Map<String, Integer> paramIndices) {
        final QuadraticFieldClockModel<Gradient> quadraticClockModel = getReceiverStation().getQuadraticFieldClock(nbParams,
                getDate(), paramIndices);
        final GradientField field = GradientField.getField(nbParams);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, getDate());
        final FieldClockOffset<Gradient> localClock = quadraticClockModel.getOffset(fieldDate);
        return fieldDate.shiftedBy(localClock.getOffset().negate());
    }

    /**
     * Compute the signal emission date.
     * @param frame frame where to perform signal propagation
     * @param receiver signal receiver
     * @param receptionDate reception date
     * @param emitter signal emitter
     * @return emission date
     */
    protected AbsoluteDate computeEmissionDate(final Frame frame, final PVCoordinatesProvider receiver,
                                               final AbsoluteDate receptionDate, final PVCoordinatesProvider emitter) {
        final double signalTravelTime = getSignalTravelTimeModel().getAdjustableEmitterComputer(emitter)
                .computeDelay(receptionDate, receiver.getPosition(receptionDate, frame), receptionDate, frame);
        return receptionDate.shiftedBy(-signalTravelTime);
    }

    /**
     * Compute the signal emission date.
     * @param frame frame where to perform signal propagation
     * @param receiver signal receiver
     * @param receptionDate reception date
     * @param emitter signal emitter
     * @return emission date
     */
    protected FieldAbsoluteDate<Gradient> computeEmissionDateField(final Frame frame,
                                                                   final FieldPVCoordinatesProvider<Gradient> receiver,
                                                                   final FieldAbsoluteDate<Gradient> receptionDate,
                                                                   final FieldPVCoordinatesProvider<Gradient> emitter) {
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldSignalTravelTimeAdjustableEmitter = getSignalTravelTimeModel().
                getFieldAdjustableEmitterComputer(receptionDate.getField(), emitter);
        final Gradient signalTravelTime = fieldSignalTravelTimeAdjustableEmitter.computeDelay(receptionDate,
                receiver.getPosition(receptionDate, frame), receptionDate, frame);
        return receptionDate.shiftedBy(signalTravelTime.negate());
    }
}
