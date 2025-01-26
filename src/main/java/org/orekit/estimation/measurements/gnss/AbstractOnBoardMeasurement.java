/* Copyright 2022-2025 Luc Maisonobe
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.estimation.measurements.QuadraticFieldClockModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ClockOffset;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldClockOffset;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Base class modeling a measurement where receiver is a satellite.
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class AbstractOnBoardMeasurement<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Constructor.
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     */
    public AbstractOnBoardMeasurement(final AbsoluteDate date, final double observed,
                                      final double sigma, final double baseWeight,
                                      final List<ObservableSatellite> satellites) {
        // Call to super constructor
        super(date, observed, sigma, baseWeight, satellites);

        // Add parameter drivers
        satellites.forEach(s -> {
            addParameterDriver(s.getClockOffsetDriver());
            addParameterDriver(s.getClockDriftDriver());
            addParameterDriver(s.getClockAccelerationDriver());
        });

    }

    /** Get emitting satellite clock provider.
     * @return emitting satellite clock provider
     */
    protected abstract QuadraticClockModel getRemoteClock();

    /** Get emitting satellite position/velocity provider.
     * @param states states of all spacecraft involved in the measurement
     * @return emitting satellite position/velocity provider
     */
    protected abstract PVCoordinatesProvider getRemotePV(SpacecraftState[] states);

    /** Get emitting satellite position/velocity provider.
     * @param states states of all spacecraft involved in the measurement
     * @param freeParameters total number of free parameters in the gradient
     * @return emitting satellite position/velocity provider
     */
    protected abstract FieldPVCoordinatesProvider<Gradient> getRemotePV(SpacecraftState[] states,
                                                                        int freeParameters);

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

    /** Compute common estimation parameters.
     * @param states states of all spacecraft involved in the measurement
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected OnBoardCommonParametersWithoutDerivatives computeCommonParametersWithout(final SpacecraftState[] states,
                                                                                       final boolean clockOffsetAlreadyApplied) {

        // local and remote satellites
        final Frame                    frame            = states[0].getFrame();
        final TimeStampedPVCoordinates pvaLocal         = states[0].getPVCoordinates(frame);
        final ClockOffset              localClock       = getSatellites().
                                                          get(0).
                                                          getQuadraticClockModel().
            getOffset(getDate());
        final double                   localClockOffset = localClock.getOffset();
        final double                   localClockRate   = localClock.getRate();
        final PVCoordinatesProvider    remotePV         = getRemotePV(states);

        // take clock offset into account
        final AbsoluteDate arrivalDate = clockOffsetAlreadyApplied ? getDate() : getDate().shiftedBy(-localClockOffset);

        // Downlink delay
        final double deltaT = arrivalDate.durationFrom(states[0]);
        final TimeStampedPVCoordinates pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final double tauD = signalTimeOfFlightAdjustableEmitter(remotePV, arrivalDate, pvaDownlink.getPosition(),
                                                                arrivalDate, frame);

        // Remote satellite at signal emission
        final AbsoluteDate        emissionDate      = arrivalDate.shiftedBy(-tauD);
        final ClockOffset         remoteClock       = getRemoteClock().getOffset(emissionDate);
        final double              remoteClockOffset = remoteClock.getOffset();
        final double              remoteClockRate   = remoteClock.getRate();
        return new OnBoardCommonParametersWithoutDerivatives(states[0],
                                                             localClockOffset, localClockRate,
                                                             remoteClockOffset, remoteClockRate,
                                                             tauD, pvaDownlink,
                                                             remotePV.getPVCoordinates(emissionDate, frame));

    }

    /** Compute common estimation parameters.
     * @param states states of all spacecraft involved in the measurement
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected OnBoardCommonParametersWithDerivatives computeCommonParametersWith(final SpacecraftState[] states,
                                                                                 final boolean clockOffsetAlreadyApplied) {

        final Frame frame = states[0].getFrame();

        // measurement derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 6k..6k+2 - Position of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6k+3..6k+5 - Velocity of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6nbSat..n - measurements parameters (clock offset, etc)
        int nbEstimatedParams = 6 * states.length;
        final Map<String, Integer> parameterIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : getParametersDrivers()) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    parameterIndices.put(span.getData(), nbEstimatedParams++);
                }
            }
        }
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbEstimatedParams),
                                                                          getDate());

        // local and remote satellites
        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal         = getCoordinates(states[0], 0, nbEstimatedParams);
        final QuadraticFieldClockModel<Gradient>      localClock       = getSatellites().get(0).getQuadraticClockModel().
                                                                         toGradientModel(nbEstimatedParams, parameterIndices, getDate());
        final FieldClockOffset<Gradient>              localClockOffset = localClock.getOffset(gDate);
        final FieldPVCoordinatesProvider<Gradient>    remotePV         = getRemotePV(states, nbEstimatedParams);

        // take clock offset into account
        final FieldAbsoluteDate<Gradient> arrivalDate = clockOffsetAlreadyApplied ?
                                                        gDate : gDate.shiftedBy(localClockOffset.getOffset().negate());

        // Downlink delay
        final Gradient deltaT = arrivalDate.durationFrom(states[0].getDate());
        final TimeStampedFieldPVCoordinates<Gradient> pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final Gradient tauD = signalTimeOfFlightAdjustableEmitter(remotePV, arrivalDate,
                                                                  pvaDownlink.getPosition(), arrivalDate,
                                                                  frame);

        // Remote satellite at signal emission
        final FieldAbsoluteDate<Gradient>        emissionDate      = arrivalDate.shiftedBy(tauD.negate());
        final QuadraticFieldClockModel<Gradient> remoteClock       = getRemoteClock(nbEstimatedParams, parameterIndices);
        final FieldClockOffset<Gradient>         remoteClockOffset = remoteClock.getOffset(emissionDate);
        return new OnBoardCommonParametersWithDerivatives(states[0], parameterIndices,
                                                          localClockOffset.getOffset(), localClockOffset.getRate(),
                                                          remoteClockOffset.getOffset(), remoteClockOffset.getRate(),
                                                          tauD, pvaDownlink,
                                                          remotePV.getPVCoordinates(emissionDate, frame));

    }

}
