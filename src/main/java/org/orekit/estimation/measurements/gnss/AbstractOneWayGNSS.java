/* Copyright 2022-2026 RomainSerra
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
package org.orekit.estimation.measurements.gnss;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.SignalBasedMeasurement;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Abstract class for one-way GNSS measurement.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractOneWayGNSS<T extends ObservedMeasurement<T>> extends SignalBasedMeasurement<T> {

    /** Observer sending measurement data. */
    private final Observer observer;

    /** Simple constructor.
     * @param observer sender of GNSS signal
     * @param date date of the measurement
     * @param observedValue observed value
     * @param measurementQuality measurement quality
     * @param signalTravelTimeModel time delay computer
     * @param local satellite which receives the signal and perform the measurement
     */
    protected AbstractOneWayGNSS(final Observer observer, final AbsoluteDate date,
                                 final double observedValue, final MeasurementQuality measurementQuality,
                                 final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite local) {
        // Call super constructor
        this(observer, date, new double[] { observedValue }, measurementQuality,
             signalTravelTimeModel, local);
    }

    /** Simple constructor.
     * @param observer sender of GNSS signal
     * @param date date of the measurement
     * @param observedValue observed value
     * @param measurementQuality measurement quality
     * @param signalTravelTimeModel time delay computer
     * @param local satellite which receives the signal and perform the measurement
     */
    protected AbstractOneWayGNSS(final Observer observer, final AbsoluteDate date,
                                 final double[] observedValue, final MeasurementQuality measurementQuality,
                                 final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite local) {
        // Call super constructor
        super(date, false, observedValue, measurementQuality,
              signalTravelTimeModel, Collections.singletonList(local));
        this.observer = observer;
    }

    /** Observer object sending signal.
     * @return observer object
     */
    public final Observer getObserver() {
        return observer;
    }

    /** Compute common estimation parameters in case where measured object is the
     * receiver of the signal value (e.g. GNSS to ObservableSatellite).
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    CommonParametersWithoutDerivatives computeLocalParametersWithout(final SpacecraftState[] states,
                                                                               final ObservableSatellite localSat,
                                                                               final AbsoluteDate measurementDate) {

        // Coordinates of the observed spacecraft
        final Frame frame            = states[0].getFrame();
        final TimeStampedPVCoordinates pvaLocal         = states[0].getPVCoordinates(frame);

        // Clock values of the observed spacecraft and signal receiver
        final ClockOffset localClock     = localSat.getQuadraticClockModel().getOffset(measurementDate);
        final double      localClockBias = localClock.getBias();

        // take clock bias of receiver (in this case, ObservableSatellite) into account
        final AbsoluteDate arrivalDate = measurementDate.shiftedBy(-localClockBias);

        // Coordinates provider of the Observer object providing the signal information
        final PVCoordinatesProvider remotePV = getObserver().getPVCoordinatesProvider();

        // Downlink delay / determine time-of-emission of signal information from remote object
        final double deltaT = arrivalDate.durationFrom(states[0]);
        final TimeStampedPVCoordinates pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = getSignalTravelTimeModel()
                .getAdjustableEmitterComputer(remotePV);
        final double tauD = signalTimeOfFlight.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote object pos/vel at time of signal emission
        final AbsoluteDate emissionDate = arrivalDate.shiftedBy(-tauD);
        final ClockOffset  remoteClock  = getObserver().getQuadraticClockModel().getOffset(emissionDate);

        return new CommonParametersWithoutDerivatives(states[0], tauD,
                localClock, remoteClock,
                states[0].shiftedBy(deltaT),
                pvaDownlink,
                remotePV.getPVCoordinates(emissionDate, frame));

    }


    /** Compute common estimation parameters with derivatives when the measured object is the
     * receiver of the signal sent by the Observer.
     * @param states state(s) of all measured spacecraft
     * @param localSat satellite whose state is being estimated
     * @param measurementDate date when measurement was taken
     * @return common parameters
     */
    CommonParametersWithDerivatives computeLocalParametersWith(final SpacecraftState[] states,
                                                               final ObservableSatellite localSat,
                                                               final AbsoluteDate measurementDate)  {
        // Create the parameter indices map
        final Frame                frame        = states[0].getFrame();
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();

        // Turn measurement date into FieldAbsoluteDate<Gradient>
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams), measurementDate);

        // Measured satellite object data
        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal         = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);
        final QuadraticFieldClockModel<Gradient> localClock       = localSat.getQuadraticClockModel().
                toGradientModel(nbParams, paramIndices, measurementDate);
        final FieldClockOffset<Gradient> localClockOffset = localClock.getOffset(gDate);

        // take clock offset into account for arrival date
        final FieldAbsoluteDate<Gradient> arrivalDate = gDate.shiftedBy(localClockOffset.getBias().negate());

        // Coords provider for observer object that is sending signal
        final FieldPVCoordinatesProvider<Gradient> remotePV = getObserver().getFieldPVCoordinatesProvider(nbParams, paramIndices);

        // Downlink delay
        final Gradient deltaT = arrivalDate.durationFrom(states[0].getDate());
        final TimeStampedFieldPVCoordinates<Gradient> pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputer = getSignalTravelTimeModel().
                getFieldAdjustableEmitterComputer(arrivalDate.getField(), remotePV);
        final Gradient tauD = fieldComputer.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote observer at signal emission time
        final FieldAbsoluteDate<Gradient> emissionDate = arrivalDate.shiftedBy(tauD.negate());
        final QuadraticFieldClockModel<Gradient> remoteClock = getObserver().getQuadraticFieldClock(nbParams,
                emissionDate.toAbsoluteDate(), paramIndices);
        final FieldClockOffset<Gradient>  remoteClockOffset = remoteClock.getOffset(emissionDate);

        return new CommonParametersWithDerivatives(states[0], paramIndices, tauD,
                localClockOffset, remoteClockOffset,
                states[0].shiftedBy(deltaT.getValue()),
                pvaDownlink,
                remotePV.getPVCoordinates(emissionDate, frame));

    }

    /**
     * Method filling estimated measurement.
     * @param observedValue theoretical value with automatic differentiation
     * @param indices mapping between parameter name and variable index
     * @param estimated object to fill
     */
    protected void fillDerivatives(final Gradient observedValue, final Map<String, Integer> indices,
                                   final EstimatedMeasurement<T> estimated) {
        final double[] derivatives = observedValue.getGradient();

        // Set value and state first order derivatives of the estimated measurement
        estimated.setEstimatedValue(observedValue.getValue());
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver measurementDriver : getParametersDrivers()) {
            for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(measurementDriver, span.getStart(), derivatives[index]);
                }
            }
        }

    }

}
