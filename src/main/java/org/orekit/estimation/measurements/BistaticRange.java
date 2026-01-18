/* Copyright 2002-2026 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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

import java.util.Arrays;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.estimation.measurements.signal.TwoWaySignalTravelTimer;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class modeling a bistatic range measurement using
 * an emitter ground station and a receiver ground station.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted from the emitter ground station</li>
 * <li>Reflected on the spacecraft</li>
 * <li>Received on the receiver ground station</li>
 * </ul>
 * The date of the measurement corresponds to the reception on ground of the reflected signal.
 * <p>
 * The motion of the stations and the spacecraft during the signal flight time are taken into account.
 * </p>
 *
 * @author Mark Rutten
 * @since 11.2
 */
public class BistaticRange extends GroundReceiverMeasurement<BistaticRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "BistaticRange";

    /**
     * Ground station from which transmission is made.
     */
    private final GroundStation emitter;

    /** Two-way signal model .*/
    private final TwoWaySignalTravelTimer twoWaySignal;

    /**
     * Simple constructor.
     *
     * @param emitter     ground station from which transmission is performed
     * @param receiver    ground station from which measurement is performed
     * @param date        date of the measurement
     * @param range       observed value
     * @param sigma       theoretical standard deviation
     * @param baseWeight  base weight
     * @param satellite   satellite related to this measurement
     * @since 11.2
     */
    public BistaticRange(final GroundStation emitter, final GroundStation receiver, final AbsoluteDate date,
                         final double range, final double sigma, final double baseWeight,
                         final ObservableSatellite satellite) {
        this(emitter, receiver, date, range, sigma, baseWeight, new SignalTravelTimeModel(), satellite);
    }

    /**
     * Simple constructor.
     *
     * @param emitter     ground station from which transmission is performed
     * @param receiver    ground station from which measurement is performed
     * @param date        date of the measurement
     * @param range       observed value
     * @param sigma       theoretical standard deviation
     * @param baseWeight  base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite   satellite related to this measurement
     * @since 14.0
     */
    public BistaticRange(final GroundStation emitter, final GroundStation receiver, final AbsoluteDate date,
                         final double range, final double sigma, final double baseWeight,
                         final SignalTravelTimeModel signalTravelTimeModel,
                         final ObservableSatellite satellite) {
        super(receiver, true, date, new double[] { range }, new double[] { sigma }, new double[] { baseWeight },
                signalTravelTimeModel, satellite);

        // Add the parameters for the receiver
        addParametersDrivers(emitter.getParametersDrivers());

        // Set emitter
        this.emitter  = emitter;
        this.twoWaySignal = new TwoWaySignalTravelTimer(signalTravelTimeModel);
    }

    /** Get the emitter ground station.
     * @return emitter ground station
     */
    public GroundStation getEmitterStation() {
        return emitter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EstimatedMeasurementBase<BistaticRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final SpacecraftState[] states) {
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();

        // Compute actual reception date
        final AbsoluteDate receptionDate = getCorrectedReceptionDate();

        // Compute light time delays
        final PVCoordinatesProvider receiverPVProvider = getReceiverStation().getPVCoordinatesProvider();
        final TimeStampedPVCoordinates stationPVAtReception = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final PVCoordinatesProvider satellitePVProvider = MeasurementObject.extractPVCoordinatesProvider(state,
                state.getPVCoordinates());
        final double[] delays = twoWaySignal.computeDelays(frame, stationPVAtReception.getPosition(), receptionDate,
                satellitePVProvider, getEmitterStation().getPVCoordinatesProvider());

        // Compute dates
        final AbsoluteDate transitDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(-delays[0]);

        // Prepare the evaluation
        final double shift = transitDate.durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final EstimatedMeasurementBase<BistaticRange> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] { emitter.getPVCoordinates(emissionDate, frame),
                        transitState.getPVCoordinates(), stationPVAtReception });

        // Clock offsets
        final double dte = getEmitterStation().getClockOffsetDriver().getValue(emissionDate);
        final double dtr = getReceiverStation().getClockOffsetDriver().getValue(receptionDate);

        // Range value
        final double tau = delays[0] + delays[1] + dtr - dte;
        final double range = tau * Constants.SPEED_OF_LIGHT;

        estimated.setEstimatedValue(range);

        return estimated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EstimatedMeasurement<BistaticRange> theoreticalEvaluation(final int iteration,
                                                                        final int evaluation,
                                                                        final SpacecraftState[] states) {
        // Bistatic range derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final Map<String, Integer> paramIndices = getEmitterStation().getParamaterIndices(states,
                getParametersDrivers());
        paramIndices.putAll(getReceiverStation().getParamaterIndices(states, getParametersDrivers()));
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);

        // Compute actual reception date
        final FieldAbsoluteDate<Gradient> receptionDate = getCorrectedReceptionDateField(nbParams, paramIndices);

        // Compute light time delays
        final FieldPVCoordinatesProvider<Gradient> receiverPVProvider = getReceiverStation().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final TimeStampedFieldPVCoordinates<Gradient> stationPVAtReception = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final FieldPVCoordinatesProvider<Gradient> satellitePVProvider = MeasurementObject.extractFieldPVCoordinatesProvider(state, pva);
        final FieldPVCoordinatesProvider<Gradient> emitterPVProvider = getEmitterStation().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final Gradient[] delays = twoWaySignal.computeDelays(frame, stationPVAtReception.getPosition(), receptionDate,
                satellitePVProvider, emitterPVProvider);

        // Compute dates
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(delays[1].negate());
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(delays[0].negate());

        // Prepare the evaluation
        final double shift = transitDate.toAbsoluteDate().durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final EstimatedMeasurement<BistaticRange> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {
                        emitterPVProvider.getPVCoordinates(emissionDate, frame).toTimeStampedPVCoordinates(),
                        transitState.getPVCoordinates(), stationPVAtReception.toTimeStampedPVCoordinates() });

        // Clock offsets
        final Gradient dte = getEmitterStation().getClockOffsetDriver().getValue(nbParams, paramIndices, emissionDate.toAbsoluteDate());
        final Gradient dtr = getReceiverStation().getClockOffsetDriver().getValue(nbParams, paramIndices, receptionDate.toAbsoluteDate());

        // Range value
        final Gradient tau   = delays[0].add(delays[1]).add(dtr).subtract(dte);
        final Gradient range = tau.multiply(Constants.SPEED_OF_LIGHT);

        estimated.setEstimatedValue(range.getValue());

        // Range first order derivatives with respect to state
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        return estimated;
    }

}
