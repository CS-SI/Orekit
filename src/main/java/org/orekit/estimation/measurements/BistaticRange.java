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

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
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
public class BistaticRange extends BistaticRangeRelatedMeasurement<BistaticRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "BistaticRange";

    /**
     * Simple constructor.
     *
     * @param emitter     emitter object
     * @param receiver    receiver object
     * @param date        date of the measurement
     * @param range       observed value
     * @param sigma       theoretical standard deviation
     * @param baseWeight  base weight
     * @param satellite   satellite related to this measurement
     * @since 11.2
     */
    public BistaticRange(final Observer emitter, final Observer receiver, final AbsoluteDate date,
                         final double range, final double sigma, final double baseWeight,
                         final ObservableSatellite satellite) {
        this(emitter, receiver, date, range, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), satellite);
    }

    /**
     * Simple constructor.
     *
     * @param emitter     emitter object
     * @param receiver    receiver object
     * @param date        date of the measurement
     * @param range       observed value
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal travel time model
     * @param satellite   satellite related to this measurement
     * @since 14.0
     */
    public BistaticRange(final Observer emitter, final Observer receiver, final AbsoluteDate date,
                         final double range, final MeasurementQuality measurementQuality,
                         final SignalTravelTimeModel signalTravelTimeModel,
                         final ObservableSatellite satellite) {
        super(emitter, receiver, date, new double[] { range }, measurementQuality, signalTravelTimeModel, satellite);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EstimatedMeasurementBase<BistaticRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final SpacecraftState[] states) {
        // Compute participants (position-velocities at signal transmissions)
        final SpacecraftState state = states[0];
        final TimeStampedPVCoordinates[] participants = getParticipants(state);

        // Extract dates
        final AbsoluteDate emissionDate = participants[0].getDate();
        final AbsoluteDate transitDate = participants[1].getDate();
        final AbsoluteDate receptionDate = participants[2].getDate();

        // Prepare the evaluation
        final double shift = transitDate.durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final EstimatedMeasurementBase<BistaticRange> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState }, participants);

        // Clock offsets
        final double dte = getEmitter().getClockBiasDriver().getValue(emissionDate);
        final double dtr = getReceiver().getClockBiasDriver().getValue(receptionDate);

        // Range value
        final double firstLegDelay = transitDate.durationFrom(emissionDate);
        final double secondLegDelay = receptionDate.durationFrom(transitDate);
        final double tau = firstLegDelay + secondLegDelay + dtr - dte;
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
        // Compute time shifts w.r.t. recorded observation date
        final Gradient[] shifts = getFieldShifts(states);
        final GradientField field = shifts[0].getField();
        final FieldAbsoluteDate<Gradient> receptionDate = new FieldAbsoluteDate<>(field, getDate()).shiftedBy(shifts[0]);
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(shifts[1]);
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(shifts[2]);

        // Prepare the evaluation
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final double shift = transitDate.toAbsoluteDate().durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final EstimatedMeasurement<BistaticRange> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {
                        getEmitter().getPVCoordinatesProvider().getPVCoordinates(emissionDate.toAbsoluteDate(), frame),
                        transitState.getPVCoordinates(),
                        getReceiver().getPVCoordinatesProvider().getPVCoordinates(receptionDate.toAbsoluteDate(), frame)});

        // Clock offsets
        final int nbParams = field.getZero().getFreeParameters();
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final Gradient dte = getEmitter().getClockBiasDriver().getValue(nbParams, paramIndices, emissionDate.toAbsoluteDate());
        final Gradient dtr = getReceiver().getClockBiasDriver().getValue(nbParams, paramIndices, receptionDate.toAbsoluteDate());

        // Range value
        final Gradient tau   = (shifts[1].add(shifts[2])).negate().add(dtr).subtract(dte);
        final Gradient range = tau.multiply(Constants.SPEED_OF_LIGHT);

        fillEstimation(range, paramIndices, estimated);
        return estimated;
    }

}
