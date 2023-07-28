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
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

/** Class modeling a range measurement from a ground station.
 * <p>
 * For one-way measurements, a signal is emitted by the satellite
 * and received by the ground station. The measurement value is the
 * elapsed time between emission and reception multiplied by c where
 * c is the speed of light.
 * </p>
 * <p>
 * For two-way measurements, the measurement is considered to be a signal
 * emitted from a ground station, reflected on spacecraft, and received
 * on the same ground station. Its value is the elapsed time between
 * emission and reception multiplied by c/2 where c is the speed of light.
 * </p>
 * <p>
 * The motion of both the station and the spacecraft during the signal
 * flight time are taken into account. For two way measurements the date of the measurement
 * corresponds to either the time of signal reception at ground station,
 * the time of signal bounce arrival or the date of transmission dependent on the time tag specification
 * enumerated value. If not specified, the time tag specification defaults to receive time.
 * For one way measurements the date of the measurement is assumed to be a time of signal reception.
 * </p>
 * <p>
 * The clock offsets of both the ground station and the satellite are taken
 * into account. These offsets correspond to the values that must be subtracted
 * from station (resp. satellite) reading of time to compute the real physical
 * date. These offsets have two effects:
 * </p>
 * <ul>
 *   <li>If measurement date is evaluated at reception time, the real physical date
 *   of the measurement is the observed date to which the receiving ground station
 *   clock offset is subtracted</li>
 *   <li>as range is evaluated using the total signal time of flight, for one-way
 *   measurements the observed range is the real physical signal time of flight to
 *   which (Δtg - Δts) ⨉ c is added, where Δtg (resp. Δts) is the clock offset for the
 *   receiving ground station (resp. emitting satellite). A similar effect exists in
 *   two-way measurements but it is computed as (Δtg - Δtg) ⨉ c / 2 as the same ground
 *   station clock is used for initial emission and final reception and therefore it evaluates
 *   to zero.</li>
 * </ul>
 * <p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 8.0
 */
public class Range extends GroundReceiverMeasurement<Range> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "Range";

    /** Range measurement constructor for one or two-way measurements with timetag of observed value
     * set to reception time.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Range(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight,
                 final ObservableSatellite satellite) {
        this(station, twoWay, date, range, sigma, baseWeight, satellite, TimeTagSpecificationType.RX);
    }

    /** Range constructor for two-way measurements with a user specified observed value timetag specification.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the timetag configuration of the provided range observation
     * @since xx.xx
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight,
                 final ObservableSatellite satellite, final TimeTagSpecificationType timeTagSpecificationType) {
        this(station, true, date, range, sigma, baseWeight, satellite, timeTagSpecificationType);
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the timetag configuration of the provided range observation
     * @since 12.0
     */
    protected Range(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                    final double range, final double sigma, final double baseWeight,
                    final ObservableSatellite satellite, final TimeTagSpecificationType timeTagSpecificationType) {
        super(station, twoWay, date, range, sigma, baseWeight, satellite, timeTagSpecificationType);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    indices.put(span.getData(), nbParams++);
                }
            }
        }
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // transform between station and inertial frame, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialDownlink =
                        getStation().getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> downlinkDateDS = offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink =
                offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                    zero, zero, zero));

        final Gradient        delta        = downlinkDateDS.durationFrom(state.getDate());
        // prepare the evaluation
        final EstimatedMeasurement<Range> estimated;
        final Gradient range;
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final Gradient tauD;

        if (isTwoWay()) {

            final Gradient tauU;

            if (getTimeTagSpecificationType() == TimeTagSpecificationType.TX) {
                //Date = epoch of transmission.
                //Vary position of receiver -> in case of uplink leg, receiver is satellite
                tauU = signalTimeOfFlightFixedEmission(pvaDS, stationDownlink.getPosition(), stationDownlink.getDate());

                final Gradient deltaMTauU = tauU.add(delta);
                //Get state at transit
                final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(deltaMTauU);
                final SpacecraftState transitState = state.shiftedBy(deltaMTauU.getValue());

                //Get station at transit - although this is effectively an initial seed for fitting the downlink delay
                final TimeStampedFieldPVCoordinates<Gradient> stationTransit = stationDownlink.shiftedBy(tauU);

                //project time of flight forwards with 0 offset.
                tauD = signalTimeOfFlightFixedEmission(stationTransit, transitStateDS.getPosition(), transitStateDS.getDate());

                estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation, new SpacecraftState[] {transitState},
                                                            new TimeStampedPVCoordinates[] {
                                                                    stationDownlink.toTimeStampedPVCoordinates(),
                                                                    transitStateDS.toTimeStampedPVCoordinates(),
                                                                    stationTransit.shiftedBy(tauD).toTimeStampedPVCoordinates()});

                // Range value
                final Gradient tau = tauD.add(tauU);
                range = tau.multiply(cOver2);

            }
            else if (getTimeTagSpecificationType() == TimeTagSpecificationType.TRANSIT) {
                final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(delta);
                final SpacecraftState transitState = state.shiftedBy(delta.getValue());

                //Calculate time of flight for return measurement participants
                tauD = signalTimeOfFlightFixedEmission(stationDownlink, transitStateDS.getPosition(), transitStateDS.getDate());
                tauU = signalTimeOfFlightFixedReception(stationDownlink, transitStateDS.getPosition(), transitStateDS.getDate());
                estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation, new SpacecraftState[] {transitState},
                                                            new TimeStampedPVCoordinates[] {
                                                                    stationDownlink.shiftedBy(tauU.negate()).toTimeStampedPVCoordinates(),
                                                                    transitStateDS.toTimeStampedPVCoordinates(),
                                                                    stationDownlink.shiftedBy(tauD).toTimeStampedPVCoordinates()
                                                            });
                //use transit state as corrected state
                range = stationDownlink.getPosition().distance(transitStateDS.getPosition());
            }
            else {
                // Downlink delay
                tauD = signalTimeOfFlightFixedReception(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

                // Transit state & Transit state (re)computed with gradients
                final Gradient        deltaMTauD   = tauD.negate().add(delta);
                final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());
                final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

                // Station at transit state date (derivatives of tauD taken into account)
                final TimeStampedFieldPVCoordinates<Gradient> stationAtTransitDate = stationDownlink.shiftedBy(tauD.negate());
                // Uplink delay
                tauU = signalTimeOfFlightFixedReception(stationAtTransitDate, transitStateDS.getPosition(),
                                                        transitStateDS.getDate());
                final TimeStampedFieldPVCoordinates<Gradient> stationUplink =
                        stationDownlink.shiftedBy(-tauD.getValue() - tauU.getValue());

                // Prepare the evaluation
                estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation, new SpacecraftState[] {transitState},
                                                            new TimeStampedPVCoordinates[] {
                                                                    stationUplink.toTimeStampedPVCoordinates(),
                                                                    transitStateDS.toTimeStampedPVCoordinates(),
                                                                    stationDownlink.toTimeStampedPVCoordinates()});

                // Range value
                final Gradient tau = tauD.add(tauU);
                range = tau.multiply(cOver2);
            }

        } else {
            // (if state has already been set up to pre-compensate propagation delay,
            //  we will have delta == tauD and transitState will be the same as state)

            // Downlink delay
            tauD = signalTimeOfFlightFixedReception(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

            // Transit state & Transit state (re)computed with gradients
            final Gradient        deltaMTauD   = tauD.negate().add(delta);
            final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());
            final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(deltaMTauD);


            estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation,
                                                        new SpacecraftState[] {transitState},
                                                        new TimeStampedPVCoordinates[] {
                                                                transitStateDS.toTimeStampedPVCoordinates(),
                                                                stationDownlink.toTimeStampedPVCoordinates()
                                                        });

            // Clock offsets
            final ObservableSatellite satellite = getSatellites().get(0);
            final Gradient            dts       = satellite.getClockOffsetDriver().getValue(nbParams, indices);
            final Gradient            dtg       = getStation().getClockOffsetDriver().getValue(nbParams, indices);

            // Range value
            range = tauD.add(dtg).subtract(dts).multiply(Constants.SPEED_OF_LIGHT);

        }

        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
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
