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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

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
 *   which (Δtl - Δtr) ⨉ c is added, where Δtl (resp. Δtr) is the clock offset for the
 *   local satellite (resp. remote satellite). A similar effect exists in
 *   two-way measurements but it is computed as (Δtl - Δtl) ⨉ c / 2 as the local satellite
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
public class InterSatellitesRange extends AbstractMeasurement<InterSatellitesRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "InterSatellitesRange";

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

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
    public InterSatellitesRange(final ObservableSatellite local,
                                final ObservableSatellite remote,
                                final boolean twoWay,
                                final AbsoluteDate date, final double range,
                                final double sigma, final double baseWeight) {
        super(date, range, sigma, baseWeight, Arrays.asList(local, remote));
        // for one way and two ways measurements, the local satellite clock offsets affects the measurement
        addParameterDriver(local.getClockOffsetDriver());
        if (!twoWay) {
            // for one way measurements, the remote satellite clock offsets also affects the measurement
            addParameterDriver(remote.getClockOffsetDriver());
        }
        this.twoway = twoWay;
    }

    /** Check if the instance represents a two-way measurement.
     * @return true if the instance represents a two-way measurement
     */
    public boolean isTwoWay() {
        return twoway;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<InterSatellitesRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                     final int evaluation,
                                                                                                     final SpacecraftState[] states) {

        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final TimeStampedPVCoordinates pvaL = local.getPVCoordinates();
        final SpacecraftState remote = states[1];
        final TimeStampedPVCoordinates pvaR = remote.getPVCoordinates();

        // compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // downlink delay
        final double dtl = getSatellites().get(0).getClockOffsetDriver().getValue(local.getDate());
        final AbsoluteDate arrivalDate = getDate().shiftedBy(-dtl);

        final TimeStampedPVCoordinates s1Downlink =
                        pvaL.shiftedBy(arrivalDate.durationFrom(pvaL.getDate()));
        final double tauD = signalTimeOfFlight(pvaR, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double delta      = getDate().durationFrom(remote.getDate());
        final double deltaMTauD = delta - tauD;

        // prepare the evaluation
        final EstimatedMeasurementBase<InterSatellitesRange> estimated;

        final double range;
        if (twoway) {
            // Transit state (re)computed with derivative structures
            final TimeStampedPVCoordinates transitState = pvaR.shiftedBy(deltaMTauD);

            // uplink delay
            final double tauU = signalTimeOfFlight(pvaL,
                                                   transitState.getPosition(),
                                                   transitState.getDate());
            estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           local.shiftedBy(deltaMTauD),
                                                           remote.shiftedBy(deltaMTauD)
                                                       }, new TimeStampedPVCoordinates[] {
                                                           local.shiftedBy(delta - tauD - tauU).getPVCoordinates(),
                                                           remote.shiftedBy(delta - tauD).getPVCoordinates(),
                                                           local.shiftedBy(delta).getPVCoordinates()
                                                       });

            // Range value
            range  = (tauD + tauU) * (0.5 * Constants.SPEED_OF_LIGHT);

        } else {

            estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           local.shiftedBy(deltaMTauD),
                                                           remote.shiftedBy(deltaMTauD)
                                                       }, new TimeStampedPVCoordinates[] {
                                                           remote.shiftedBy(delta - tauD).getPVCoordinates(),
                                                           local.shiftedBy(delta).getPVCoordinates()
                                                       });

            // Clock offsets
            final double dtr = getSatellites().get(1).getClockOffsetDriver().getValue(remote.getDate());

            // Range value
            range  = (tauD + dtl - dtr) * Constants.SPEED_OF_LIGHT;

        }
        estimated.setEstimatedValue(range);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesRange> theoreticalEvaluation(final int iteration,
                                                                               final int evaluation,
                                                                               final SpacecraftState[] states) {

        // Range derivatives are computed with respect to spacecrafts states in inertial frame
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

        // coordinates of both satellites
        final SpacecraftState local = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pvaL = getCoordinates(local, 0, nbParams);
        final SpacecraftState remote = states[1];
        final TimeStampedFieldPVCoordinates<Gradient> pvaR = getCoordinates(remote, 6, nbParams);

        // compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // downlink delay
        final Gradient dtl = getSatellites().get(0).getClockOffsetDriver().getValue(nbParams, indices, local.getDate());
        final FieldAbsoluteDate<Gradient> arrivalDate =
                        new FieldAbsoluteDate<>(getDate(), dtl.negate());

        final TimeStampedFieldPVCoordinates<Gradient> s1Downlink =
                        pvaL.shiftedBy(arrivalDate.durationFrom(pvaL.getDate()));
        final Gradient tauD = signalTimeOfFlight(pvaR, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double              delta      = getDate().durationFrom(remote.getDate());
        final Gradient deltaMTauD = tauD.negate().add(delta);

        // prepare the evaluation
        final EstimatedMeasurement<InterSatellitesRange> estimated;

        final Gradient range;
        if (twoway) {
            // Transit state (re)computed with derivative structures
            final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaR.shiftedBy(deltaMTauD);

            // uplink delay
            final Gradient tauU = signalTimeOfFlight(pvaL,
                                                     transitStateDS.getPosition(),
                                                     transitStateDS.getDate());
            estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       local.shiftedBy(deltaMTauD.getValue()),
                                                       remote.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       local.shiftedBy(delta - tauD.getValue() - tauU.getValue()).getPVCoordinates(),
                                                       remote.shiftedBy(delta - tauD.getValue()).getPVCoordinates(),
                                                       local.shiftedBy(delta).getPVCoordinates()
                                                   });

            // Range value
            range  = tauD.add(tauU).multiply(0.5 * Constants.SPEED_OF_LIGHT);

        } else {

            estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       local.shiftedBy(deltaMTauD.getValue()),
                                                       remote.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       remote.shiftedBy(delta - tauD.getValue()).getPVCoordinates(),
                                                       local.shiftedBy(delta).getPVCoordinates()
                                                   });

            // Clock offsets
            final Gradient dtr = getSatellites().get(1).getClockOffsetDriver().getValue(nbParams, indices, remote.getDate());

            // Range value
            range  = tauD.add(dtl).subtract(dtr).multiply(Constants.SPEED_OF_LIGHT);

        }
        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to states
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));
        estimated.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 6, 12));

        // Set partial derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        return estimated;

    }

}
