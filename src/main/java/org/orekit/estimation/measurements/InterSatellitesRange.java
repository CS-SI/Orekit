/* Copyright 2002-2026 CS GROUP
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTravelTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way or two-way range measurements between two satellites.
 * <p>
 * For one-way measurements, a signal is emitted by a remote satellite and received
 * by local satellite. The measurement value is the elapsed time between emission
 * and reception multiplied by c where c is the speed of light.
 * </p>
 * <p>
 * For two-way measurements, a signal is emitted by local satellite, reflected on
 * remote satellite, and received back by local satellite. The measurement value
 * is the elapsed time between emission and reception multiplied by c/2 where c
 * is the speed of light.
 * </p>
 * <p>
 * Since 9.3, this class also uses the clock offsets of both satellites,
 * which manage the value that must be added to each satellite reading of time to
 * compute the real physical date. In this measurement, these offsets have two effects:
 * </p>
 * <ul>
 *   <li>as measurement date is evaluated at reception time, the real physical date
 *   of the measurement is the observed date to which the local satellite clock
 *   offset is subtracted</li>
 *   <li>as range is evaluated using the total signal time of flight, for one-way
 *   measurements the observed range is the real physical signal time of flight to
 *   which (Δtl - Δtr) ⨯ c is added, where Δtl (resp. Δtr) is the clock offset for the
 *   local satellite (resp. remote satellite). A similar effect exists in
 *   two-way measurements but it is computed as (Δtl - Δtl) ⨯ c / 2 as the local satellite
 *   clock is used for both initial emission and final reception and therefore it evaluates
 *   to zero.</li>
 * </ul>
 * <p>
 * The motion of both satellites during the signal flight time is
 * taken into account. The date of the measurement corresponds to
 * the reception of the signal by satellite 1.
 * </p>
 * @author Luc Maisonobe
 * @since 9.0
 */
public class InterSatellitesRange extends SignalBasedMeasurement<InterSatellitesRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "InterSatellitesRange";

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @since 9.3
     */
    public InterSatellitesRange(final ObservableSatellite local, final ObservableSatellite remote,
                                final boolean twoWay, final AbsoluteDate date, final double range,
                                final double sigma, final double baseWeight) {
        this(local, remote, twoWay, date, range, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel());
    }

    /** Simple constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote satellite which simply emits the signal in the one-way case,
     * or reflects the signal in the two-way case
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @since 14.0
     */
    public InterSatellitesRange(final ObservableSatellite local, final ObservableSatellite remote,
                                final boolean twoWay, final AbsoluteDate date, final double range,
                                final MeasurementQuality measurementQuality,
                                final SignalTravelTimeModel signalTravelTimeModel) {
        super(date, twoWay, new double[] {range}, measurementQuality, signalTravelTimeModel,
                Arrays.asList(local, remote));
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<InterSatellitesRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                     final int evaluation,
                                                                                                     final SpacecraftState[] states) {
        // compute actual reception date
        final double dtl = getSatellites().get(0).getClockBiasDriver().getValue(getDate());
        final AbsoluteDate receptionDate = getDate().shiftedBy(-dtl);

        if (isTwoWay()) {
            return theoreticalTwoWayEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states);
        } else {
            return theoreticalOneWayEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states);
        }
    }

    /**
     * Estimate two-way measurement without derivatives.
     * @param iteration iteration
     * @param evaluation evaluation
     * @param receptionDate actual reception date
     * @param states states
     * @return estimated measurement
     * @since 14.0
     */
    private EstimatedMeasurementBase<InterSatellitesRange> theoreticalTwoWayEvaluationWithoutDerivatives(final int iteration,
                                                                                                         final int evaluation,
                                                                                                         final AbsoluteDate receptionDate,
                                                                                                         final SpacecraftState[] states) {
        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final SpacecraftState remote = states[1];

        // compute transit and emission dates
        final Frame           frame = local.getFrame();
        final TwoLeggedSignalTravelTimer travelTimer = new TwoLeggedSignalTravelTimer(getSignalTravelTimeModel());
        final SpacecraftState localAtReception = local.shiftedBy(receptionDate.durationFrom(local));
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, localAtReception.getPosition(),
                frame);
        final double[] delays = travelTimer.computeDelays(receptionCondition,
                AbstractParticipant.extractPVCoordinatesProvider(remote, remote.getPVCoordinates()),
                AbstractParticipant.extractPVCoordinatesProvider(local, local.getPVCoordinates()));
        final AbsoluteDate transitDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(-delays[0]);

        // form participants
        final SpacecraftState remoteAtTransit = remote.shiftedBy(transitDate.durationFrom(remote));
        final SpacecraftState localAtEmission = local.shiftedBy(emissionDate.durationFrom(local));
        final EstimatedMeasurementBase<InterSatellitesRange> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { local.shiftedBy(transitDate.durationFrom(local)), remoteAtTransit }, new TimeStampedPVCoordinates[] {
                localAtEmission.getPVCoordinates(), remoteAtTransit.getPVCoordinates(frame), localAtReception.getPVCoordinates()});

        // range value
        final double range = (delays[0] + delays[1]) / 2. * Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue(range);
        return estimated;
    }

    /**
     * Estimate one-way measurement without derivatives.
     * @param iteration iteration
     * @param evaluation evaluation
     * @param receptionDate actual reception date
     * @param states states
     * @return estimated measurement
     * @since 14.0
     */
    private EstimatedMeasurementBase<InterSatellitesRange> theoreticalOneWayEvaluationWithoutDerivatives(final int iteration,
                                                                                                         final int evaluation,
                                                                                                         final AbsoluteDate receptionDate,
                                                                                                         final SpacecraftState[] states) {
        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final SpacecraftState remote = states[1];

        // compute emission date
        final Frame           frame = local.getFrame();
        final SpacecraftState localAtReception = local.shiftedBy(receptionDate.durationFrom(local));
        final SignalTravelTimeAdjustableEmitter adjustableEmitterComputer = getSignalTravelTimeModel()
                .getAdjustableEmitterComputer(AbstractParticipant.extractPVCoordinatesProvider(remote, remote.getPVCoordinates()));
        final double delay = adjustableEmitterComputer.computeDelay(new SignalReceptionCondition(receptionDate,
                localAtReception.getPosition(), frame));
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-delay);

        // form participants
        final SpacecraftState remoteAtEmission = remote.shiftedBy(emissionDate.durationFrom(remote));
        final EstimatedMeasurementBase<InterSatellitesRange> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { local.shiftedBy(emissionDate.durationFrom(local)), remoteAtEmission }, new TimeStampedPVCoordinates[] {
                remoteAtEmission.getPVCoordinates(frame), localAtReception.getPVCoordinates()});

        // range value
        final double dtl = getSatellites().get(0).getClockBiasDriver().getValue(getDate());
        final double dtr = getSatellites().get(1).getClockBiasDriver().getValue(remoteAtEmission.getDate());
        final double range  = (delay + dtl - dtr) * Constants.SPEED_OF_LIGHT;

        estimated.setEstimatedValue(range);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesRange> theoreticalEvaluation(final int iteration,
                                                                               final int evaluation,
                                                                               final SpacecraftState[] states) {
        // Range derivatives are computed with respect to spacecraft states in inertial frame
        // ----------------------
        //
        // Parameters:
        //  - 0..2  - Position of the receiver satellite in inertial frame
        //  - 3..5  - Velocity of the receiver satellite in inertial frame
        //  - 6..8  - Position of the remote satellite in inertial frame
        //  - 9..11 - Velocity of the remote satellite in inertial frame
        //  - 12..  - Measurement parameters: local clock offset, remote clock offset...
        int nbParams = 12;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    if (!indices.containsKey(span.getData())) {
                        indices.put(span.getData(), nbParams++);
                    }
                }
            }
        }

        // Position-velocity for automatic differentiation
        final TimeStampedFieldPVCoordinates<Gradient> pvaL = getCoordinates(states[0], 0, nbParams);
        final Frame frame = states[0].getFrame();
        final TimeStampedFieldPVCoordinates<Gradient> pvaR = states[1].getFrame().
                getTransformTo(frame, states[1].getDate()).transformPVCoordinates(getCoordinates(states[1], 6, nbParams));

        if (isTwoWay()) {
            return theoreticalTwoWayEvaluation(iteration, evaluation, states, pvaL, pvaR, indices);
        } else {
            return theoreticalOneWayEvaluation(iteration, evaluation, states, pvaL, pvaR, indices);
        }
    }

    /**
     * Estimate two-way measurement.
     * @param iteration iteration
     * @param evaluation evaluation
     * @param states states
     * @param pvaL position-velocity coordinate of local for automatic differentiation
     * @param pvaR position-velocity coordinate of remote for automatic differentiation
     * @param indices mapping between parameters' name and derivatives' index
     * @return estimated measurement
     * @since 14.0
     */
    private EstimatedMeasurement<InterSatellitesRange> theoreticalTwoWayEvaluation(final int iteration, final int evaluation,
                                                                                   final SpacecraftState[] states,
                                                                                   final TimeStampedFieldPVCoordinates<Gradient> pvaL,
                                                                                   final TimeStampedFieldPVCoordinates<Gradient> pvaR,
                                                                                   final Map<String, Integer> indices) {
        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final SpacecraftState remote = states[1];
        final Frame frame = states[0].getFrame();

        // compute actual reception date
        final int nbParams = pvaL.getDate().getField().getZero().getFreeParameters();
        final Gradient dtl = getSatellites().get(0).getClockBiasDriver().getValue(nbParams, indices, getDate());
        final FieldAbsoluteDate<Gradient> receptionDate = new FieldAbsoluteDate<>(getDate(), dtl.negate());

        // compute transit and emission dates
        final TwoLeggedSignalTravelTimer travelTimer = new TwoLeggedSignalTravelTimer(getSignalTravelTimeModel());
        final FieldPVCoordinatesProvider<Gradient> localPVProvider = AbstractParticipant.extractFieldPVCoordinatesProvider(local, pvaL);
        final FieldPVCoordinatesProvider<Gradient> remotePVProvider = AbstractParticipant.extractFieldPVCoordinatesProvider(remote, pvaR);
        final TimeStampedFieldPVCoordinates<Gradient> localPVAtReception = localPVProvider.getPVCoordinates(receptionDate, frame);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                localPVAtReception.getPosition(), frame);
        final Gradient[] delays = travelTimer.computeDelays(receptionCondition, remotePVProvider, localPVProvider);
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(delays[1].negate());
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(delays[0].negate());

        // form participants
        final SpacecraftState remoteAtTransit = remote.shiftedBy(transitDate.toAbsoluteDate().durationFrom(remote));
        final SpacecraftState localAtEmission = local.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(local));
        final EstimatedMeasurement<InterSatellitesRange> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { local.shiftedBy(transitDate.toAbsoluteDate().durationFrom(local)), remoteAtTransit }, new TimeStampedPVCoordinates[] {
                localAtEmission.getPVCoordinates(), remoteAtTransit.getPVCoordinates(frame), localPVAtReception.toTimeStampedPVCoordinates()});

        // Range value
        final Gradient range = delays[0].add(delays[1]).multiply(0.5 * Constants.SPEED_OF_LIGHT);
        fillDerivatives(range, indices, estimated);
        return estimated;
    }

    /**
     * Estimate one-way measurement.
     * @param iteration iteration
     * @param evaluation evaluation
     * @param states states
     * @param pvaL position-velocity coordinate of local for automatic differentiation
     * @param pvaR position-velocity coordinate of remote for automatic differentiation
     * @param indices mapping between parameters' name and derivatives' index
     * @return estimated measurement
     * @since 14.0
     */
    private EstimatedMeasurement<InterSatellitesRange> theoreticalOneWayEvaluation(final int iteration, final int evaluation,
                                                                                   final SpacecraftState[] states,
                                                                                   final TimeStampedFieldPVCoordinates<Gradient> pvaL,
                                                                                   final TimeStampedFieldPVCoordinates<Gradient> pvaR,
                                                                                   final Map<String, Integer> indices) {
        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final SpacecraftState remote = states[1];
        final Frame frame = local.getFrame();

        // compute actual reception date
        final int nbParams = pvaL.getDate().getField().getZero().getFreeParameters();
        final Gradient dtl = getSatellites().get(0).getClockBiasDriver().getValue(nbParams, indices, getDate());
        final FieldAbsoluteDate<Gradient> receptionDate = new FieldAbsoluteDate<>(getDate(), dtl.negate());

        // compute emission date
        final FieldPVCoordinatesProvider<Gradient> remotePVProvider = AbstractParticipant.extractFieldPVCoordinatesProvider(remote, pvaR);
        final TimeStampedFieldPVCoordinates<Gradient> localPVAtReception = AbstractParticipant.extractFieldPVCoordinatesProvider(local, pvaL)
                .getPVCoordinates(receptionDate, frame);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> adjustableEmitterComputer = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(dtl.getField(), remotePVProvider);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                localPVAtReception.getPosition(), frame);
        final Gradient delay = adjustableEmitterComputer.computeDelay(receptionCondition);
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(delay.negate());

        // form participants
        final SpacecraftState remoteAtEmission = remote.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(remote));
        final SpacecraftState localAtReception = local.shiftedBy(receptionDate.toAbsoluteDate().durationFrom(local));
        final EstimatedMeasurement<InterSatellitesRange> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { local.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(local)), remoteAtEmission },
                new TimeStampedPVCoordinates[] { remoteAtEmission.getPVCoordinates(frame), localAtReception.getPVCoordinates() });

        // Range value
        final Gradient dtr = getSatellites().get(1).getClockBiasDriver().getValue(nbParams, indices, remoteAtEmission.getDate());
        final Gradient range = delay.add(dtl).subtract(dtr).multiply(Constants.SPEED_OF_LIGHT);
        fillDerivatives(range, indices, estimated);
        return estimated;
    }

    /**
     * Fill estimated measurement with derivatives.
     * @param range range evaluated with automatic differentiation
     * @param indices mapping between parameters' name and derivatives' index
     * @param estimated estimation
     */
    private void fillDerivatives(final Gradient range, final Map<String, Integer> indices,
                                 final EstimatedMeasurement<InterSatellitesRange> estimated) {
        estimated.setEstimatedValue(range.getValue());

        // Range first order derivatives with respect to states
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));
        estimated.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 6, 12));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }
    }
}
