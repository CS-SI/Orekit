/* Copyright 2022-2026 Luc Maisonobe
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
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.CommonParametersWithDerivatives;
import org.orekit.estimation.measurements.CommonParametersWithoutDerivatives;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Base class for measurement between two satellites that are both estimated.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a remote satellite and received by a local satellite.
 * Its value is the number of cycles between emission and reception.
 * The motion of both spacecraft during the signal flight time
 * are taken into account. The date of the measurement corresponds to the
 * reception on ground of the emitted signal.
 * </p>
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class AbstractInterSatellitesMeasurement<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Constructor.
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param local satellite which receives the signal and performs the measurement
     * @param remote remote satellite which simply emits the signal
     * @since 14.0
     */
    protected AbstractInterSatellitesMeasurement(final AbsoluteDate date, final double observed,
                                                 final double sigma, final double baseWeight,
                                                 final SignalTravelTimeModel signalTravelTimeModel,
                                                 final ObservableSatellite local,
                                                 final ObservableSatellite remote) {
        // Call to super constructor
        super(date, false, new double[] {observed}, new double[] {sigma}, new double[] {baseWeight},
                signalTravelTimeModel, Arrays.asList(local, remote));
    }

    /** Retrieves the clock of the satellite being treated as "remote"
     * in this function (i.e. sat number 2).
     * @return ObservableSatellite clock
     */
    protected QuadraticClockModel getRemoteClock() {
        return getSatellites().get(1).getQuadraticClockModel();
    }

    /** Get emitting satellite clock provider.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @return emitting satellite clock provider
     */
    protected QuadraticFieldClockModel<Gradient> getRemoteClock(final int freeParameters,
                                                                final Map<String, Integer> indices) {
        return getRemoteClock().toGradientModel(freeParameters, indices, getDate());
    }

    /** Return the FieldPVCoordinatesProvider.
     * @param state ObservableSatellite spacecraft state
     * @return pos/vel coordinates provider for values with Gradient field
     * @since 14.0
     */
    protected PVCoordinatesProvider getRemotePV(final SpacecraftState state) {
        return new AbsolutePVCoordinates(state.getFrame(), state.getPVCoordinates());
    }

    /** Return the FieldPVCoordinatesProvider.
     * @param state ObservableSatellite spacecraft state
     * @param freeParameters number of free parameters
     * @return pos/vel coordinates provider for values with Gradient field
     * @since 14.0
     */
    protected FieldPVCoordinatesProvider<Gradient> getRemotePV(final SpacecraftState state,
                                                               final int freeParameters) {
        // convert the SpacecraftState to a FieldPVCoordinatesProvider<Gradient>
        return (date, frame) -> {

            // set up the derivatives with respect to remote state at its date
            final TimeStampedFieldPVCoordinates<Gradient> pv0 = getCoordinates(state, 6, freeParameters);

            // shift to desired date
            final TimeStampedFieldPVCoordinates<Gradient> shifted = pv0.shiftedBy(date.durationFrom(state.getDate()));

            // transform to desired frame
            return state.getFrame().getTransformTo(frame, state.getDate()).transformPVCoordinates(shifted);

        };
    }

    /** Compute common estimation parameters.
     * @param states states of all spacecraft involved in the measurement
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected CommonParametersWithoutDerivatives computeCommonParametersWithout(final SpacecraftState[] states,
                                                                                final boolean clockOffsetAlreadyApplied) {

        // local and remote satellites
        final Frame                    frame            = states[0].getFrame();
        final TimeStampedPVCoordinates pvaLocal         = states[0].getPVCoordinates(frame);
        final ClockOffset              localClock       = getSatellites().get(0).
                                                          getQuadraticClockModel().getOffset(getDate());
        final double                   localClockOffset = localClock.getOffset();
        final PVCoordinatesProvider    remotePV         = getRemotePV(states[1]);

        // take clock offset into account
        final AbsoluteDate arrivalDate = clockOffsetAlreadyApplied ? getDate() : getDate().shiftedBy(-localClockOffset);

        // Downlink delay
        final double deltaT = arrivalDate.durationFrom(states[0]);
        final TimeStampedPVCoordinates pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = getSignalTravelTimeModel().getAdjustableEmitterComputer(remotePV);
        final double tauD = signalTimeOfFlight.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote satellite at signal emission
        final AbsoluteDate emissionDate = arrivalDate.shiftedBy(-tauD);
        final ClockOffset  remoteClock  = getRemoteClock().getOffset(emissionDate);

        return new CommonParametersWithoutDerivatives(states[0], tauD,
                                                      localClock, remoteClock,
                                                      states[0],
                                                      pvaDownlink,
                                                      remotePV.getPVCoordinates(emissionDate, frame));

    }

    /** Compute common estimation parameters.
     * @param states states of all spacecraft involved in the measurement
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected CommonParametersWithDerivatives computeCommonParametersWith(final SpacecraftState[] states,
                                                                          final boolean clockOffsetAlreadyApplied) {

        final Frame frame = states[0].getFrame();

        // measurement derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 6k..6k+2 - Position of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6k+3..6k+5 - Velocity of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6nbSat..n - measurements parameters (clock offset, etc)
        int nbParams = 6 * states.length;
        final Map<String, Integer> paramIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : getParametersDrivers()) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    paramIndices.put(span.getData(), nbParams++);
                }
            }
        }
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams),
                                                                          getDate());

        // local and remote satellites
        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal         = getCoordinates(states[0], 0, nbParams);
        final QuadraticFieldClockModel<Gradient>      localClock       = getSatellites().get(0).getQuadraticClockModel().
                                                                         toGradientModel(nbParams, paramIndices, getDate());
        final FieldClockOffset<Gradient>              localClockOffset = localClock.getOffset(gDate);
        final FieldPVCoordinatesProvider<Gradient>    remotePV         = getRemotePV(states[1], nbParams);

        // take clock offset into account
        final FieldAbsoluteDate<Gradient> arrivalDate = clockOffsetAlreadyApplied ?
                                                        gDate : gDate.shiftedBy(localClockOffset.getOffset().negate());

        // Downlink delay
        final Gradient deltaT = arrivalDate.durationFrom(states[0].getDate());
        final TimeStampedFieldPVCoordinates<Gradient> pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputer = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(deltaT.getField(), remotePV);
        final Gradient tauD = fieldComputer.computeDelay(arrivalDate, pvaDownlink.getPosition(), arrivalDate, frame);

        // Remote satellite at signal emission
        final FieldAbsoluteDate<Gradient>        emissionDate      = arrivalDate.shiftedBy(tauD.negate());
        final QuadraticFieldClockModel<Gradient> remoteClock       = getRemoteClock(nbParams, paramIndices);
        final FieldClockOffset<Gradient>         remoteClockOffset = remoteClock.getOffset(emissionDate);

        return new CommonParametersWithDerivatives(states[0], paramIndices, tauD,
                                                   localClockOffset, remoteClockOffset,
                                                   states[0], pvaDownlink,
                                                   remotePV.getPVCoordinates(emissionDate, frame));

    }

}
