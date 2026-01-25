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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a turn-around range measurement using a primary ground station and a secondary ground station.
 * <p>
 * The measurement is considered to be a signal:
 * - Emitted from the primary ground station
 * - Reflected on the spacecraft
 * - Reflected on the secondary ground station
 * - Reflected on the spacecraft again
 * - Received on the primary ground station
 * Its value is the elapsed time between emission and reception
 * divided by 2c were c is the speed of light.
 * The motion of the stations and the spacecraft
 * during the signal flight time are taken into account.
 * The date of the measurement corresponds to the
 * reception on ground of the reflected signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 *
 * @since 9.0
 */
public class TurnAroundRange extends AbstractMeasurement<TurnAroundRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "TurnAroundRange";

    /** Station from which measurement is performed. */
    private final GroundStation primaryStation;

    /** Secondary ground station reflecting the signal. */
    private final GroundStation secondaryStation;

    /** Simple constructor.
     * @param primaryStation ground station from which measurement is performed
     * @param secondaryStation ground station reflecting the signal
     * @param date date of the measurement
     * @param turnAroundRange observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public TurnAroundRange(final GroundStation primaryStation, final GroundStation secondaryStation,
                           final AbsoluteDate date, final double turnAroundRange,
                           final double sigma, final double baseWeight,
                           final ObservableSatellite satellite) {
        this(primaryStation, secondaryStation, date, turnAroundRange, sigma, baseWeight, new SignalTravelTimeModel(),
                satellite);
    }

    /** Constructor.
     * @param primaryStation ground station from which measurement is performed
     * @param secondaryStation ground station reflecting the signal
     * @param date date of the measurement
     * @param turnAroundRange observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public TurnAroundRange(final GroundStation primaryStation, final GroundStation secondaryStation,
                           final AbsoluteDate date, final double turnAroundRange,
                           final double sigma, final double baseWeight,
                           final SignalTravelTimeModel signalTravelTimeModel,
                           final ObservableSatellite satellite) {
        super(date, true, new double[] {turnAroundRange}, new double[] {sigma}, new double[] {baseWeight},
                signalTravelTimeModel, Collections.singletonList(satellite));

        // Add parameter drivers
        addParametersDrivers(primaryStation.getParametersDrivers());
        addParametersDrivers(secondaryStation.getParametersDrivers());

        this.primaryStation   = primaryStation;
        this.secondaryStation = secondaryStation;

    }

    /** Get the primary ground station from which measurement is performed.
     * @return primary ground station from which measurement is performed
     */
    public GroundStation getPrimaryStation() {
        return primaryStation;
    }

    /** Get the secondary ground station reflecting the signal.
     * @return secondary ground station reflecting the signal
     */
    public GroundStation getSecondaryStation() {
        return secondaryStation;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<TurnAroundRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                final int evaluation,
                                                                                                final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Time-stamped PV
        final TimeStampedPVCoordinates pva = state.getPVCoordinates();

        // The path of the signal is divided in two legs.
        // Leg1: Emission from primary station to satellite in primaryTauU seconds
        //     + Reflection from satellite to secondary station in secondaryTauD seconds
        // Leg2: Reflection from secondary station to satellite in secondaryTauU seconds
        //     + Reflection from satellite to primary station in primaryTaudD seconds
        // The measurement is considered to be time stamped at reception on ground
        // by the primary station. All times are therefore computed as backward offsets
        // with respect to this reception time.
        //
        // Two intermediate spacecraft states are defined:
        //  - transitStateLeg2: State of the satellite when it bounced back the signal
        //                      from secondary station to primary station during the 2nd leg
        //  - transitStateLeg1: State of the satellite when it bounced back the signal
        //                      from primary station to secondary station during the 1st leg

        // Compute propagation time for the 2nd leg of the signal path
        // --

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = primaryTauD + secondaryTauU)
        final double delta = getDate().durationFrom(state.getDate());

        // Solve for PV coords at signal arrival time at first station
        final double primeClockOffset = getPrimaryStation().getQuadraticClockModel().getOffset(getDate()).getOffset();
        final AbsoluteDate measurementDate = getDate().shiftedBy(-primeClockOffset);
        final TimeStampedPVCoordinates primaryArrival = getPrimaryStation().getPVCoordinatesProvider()
                .getPVCoordinates(measurementDate, state.getFrame());

        // Compute propagation times
        final PVCoordinatesProvider localCoordsProvider = new AbsolutePVCoordinates(state.getFrame(), pva);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = getSignalTravelTimeModel().getAdjustableEmitterComputer(localCoordsProvider);
        final double primaryTauD = signalTimeOfFlight.computeDelay(pva.getDate(), primaryArrival.getPosition(), measurementDate, state.getFrame());

        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final double dtLeg2 = delta - primaryTauD;

        // Transit state where the satellite reflected the signal from secondary to primary station
        final SpacecraftState transitStateLeg2 = state.shiftedBy(dtLeg2);

        // Transit state pv of leg2 (re)computed with gradient
        final TimeStampedPVCoordinates transitStateLeg2PV = pva.shiftedBy(dtLeg2);

        // transform between secondary station topocentric frame (east-north-zenith) and inertial frame expressed as gradients
        // The components of secondary station's position in offset frame are the 3 last derivative parameters

        // Uplink time of flight from secondary station to transit state of leg2
        final AbsoluteDate approxReboundDate = measurementDate.shiftedBy(-delta);
        final PVCoordinatesProvider secondPVCoordinatesProvider = getSecondaryStation().getPVCoordinatesProvider();
        final TimeStampedPVCoordinates QSecondaryApprox = secondPVCoordinatesProvider.getPVCoordinates(approxReboundDate, state.getFrame());
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlightQSecondary =
                            getSignalTravelTimeModel().getAdjustableEmitterComputer(new AbsolutePVCoordinates(state.getFrame(), QSecondaryApprox));
        final double secondaryTauU = signalTimeOfFlightQSecondary.computeDelay(transitStateLeg2PV.getPosition(), transitStateLeg2PV.getDate(), state.getFrame());

        // Total time of flight for leg 2
        final double tauLeg2 = primaryTauD + secondaryTauU;

        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of rebound of the signal to secondary station
        final AbsoluteDate reboundDate = measurementDate.shiftedBy(-tauLeg2);

        // Secondary station PV in inertial frame at rebound date on secondary station
        final TimeStampedPVCoordinates secondaryRebound = getSecondaryStation().getPVCoordinatesProvider().getPVCoordinates(reboundDate, state.getFrame());

        // Downlink time of flight from transitStateLeg1 to secondary station at rebound date
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlightLeg2 =
                    getSignalTravelTimeModel().getAdjustableEmitterComputer(new AbsolutePVCoordinates(state.getFrame(), transitStateLeg2PV));
        final double secondaryTauD = signalTimeOfFlightLeg2.computeDelay(transitStateLeg2PV.getDate(), secondaryRebound.getPosition(), reboundDate, state.getFrame());

        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final double dtLeg1 = dtLeg2 - secondaryTauU - secondaryTauD;

        // Transit state pv of leg2 (re)computed
        final TimeStampedPVCoordinates transitStateLeg1PV = pva.shiftedBy(dtLeg1);

        // Primary station PV in inertial frame at approximate emission date
        final AbsoluteDate approxEmissionDate = measurementDate.shiftedBy(-2 * (secondaryTauU + primaryTauD));
        final TimeStampedPVCoordinates QPrimaryApprox = getPrimaryStation().getPVCoordinatesProvider().getPVCoordinates(approxEmissionDate, state.getFrame());

        // Uplink time of flight from primary station to transit state of leg1
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlightQPrimary =
                        getSignalTravelTimeModel().getAdjustableEmitterComputer(new AbsolutePVCoordinates(state.getFrame(), QPrimaryApprox));
        final double primaryTauU = signalTimeOfFlightQPrimary.computeDelay(QPrimaryApprox.getDate(),
                                                                      transitStateLeg1PV.getPosition(),
                                                                      transitStateLeg1PV.getDate(),
                                                                      state.getFrame());

        // Primary station PV in inertial frame at exact emission date
        final AbsoluteDate emissionDate = transitStateLeg1PV.getDate().shiftedBy(-primaryTauU);
        final TimeStampedPVCoordinates checkPrimaryDeparture =
                        getPrimaryStation().getPVCoordinatesProvider().getPVCoordinates(emissionDate, state.getFrame());

        // Total time of flight for leg 1
        final double tauLeg1 = secondaryTauD + primaryTauU;


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of rebound of the signal at the secondary station
        // Or t" = t -primaryTauD -secondaryTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +secondaryTauU to get transitStateLeg2
        //  - -secondaryTauD to get transitStateLeg1
        final EstimatedMeasurementBase<TurnAroundRange> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitStateLeg2.shiftedBy(-secondaryTauU)
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       checkPrimaryDeparture,
                                                       transitStateLeg1PV,
                                                       secondaryRebound,
                                                       transitStateLeg2.getPVCoordinates(),
                                                       primaryArrival
                                                   });

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final double turnAroundRange = (tauLeg2 + tauLeg1) * cOver2;
        estimated.setEstimatedValue(turnAroundRange);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                          final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Turn around range derivatives are computed with respect to:
        // - Spacecraft state in inertial frame
        // - Primary station parameters
        // - Secondary station parameters
        // --------------------------
        //
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - stations' parameters (clock offset, station offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // we have to check for duplicate keys because primary and secondary station share
            // pole and prime meridian parameters names that must be considered
            // as one set only (they are combined together by the estimation engine)
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    if (!indices.containsKey(span.getData())) {
                        indices.put(span.getData(), nbParams++);
                    }
                }
            }
        }

        // Place the gradient in a time-stamped PV
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // The path of the signal is divided in two legs.
        // Leg1: Emission from primary station to satellite in primaryTauU seconds
        //     + Reflection from satellite to secondary station in secondaryTauD seconds
        // Leg2: Reflection from secondary station to satellite in secondaryTauU seconds
        //     + Reflection from satellite to primary station in primaryTaudD seconds
        // The measurement is considered to be time stamped at reception on ground
        // by the primary station. All times are therefore computed as backward offsets
        // with respect to this reception time.
        //
        // Two intermediate spacecraft states are defined:
        //  - transitStateLeg2: State of the satellite when it bounced back the signal
        //                      from secondary station to primary station during the 2nd leg
        //  - transitStateLeg1: State of the satellite when it bounced back the signal
        //                      from primary station to secondary station during the 1st leg

        // Compute propagation time for the 2nd leg of the signal path
        // --

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = primaryTauD + secondaryTauU)
        final double delta = getDate().durationFrom(state.getDate());

        // Primary station PV in inertial frame at measurement date
        final Field<Gradient> field = GradientField.getField(nbParams);
        final FieldAbsoluteDate<Gradient> measurementDateDS = new FieldAbsoluteDate<>(field, getDate());
        final TimeStampedFieldPVCoordinates<Gradient> primaryArrival = getPrimaryStation().
                                                                       getFieldPVCoordinatesProvider(nbParams, indices).
                                                                       getPVCoordinates(measurementDateDS, states[0].getFrame());

        // Compute propagation times
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputer = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, new FieldAbsolutePVCoordinates<>(state.getFrame(), pvaDS));
        final Gradient primaryTauD = fieldComputer.computeDelay(pvaDS.getDate(), primaryArrival.getPosition(), measurementDateDS, state.getFrame());

        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final Gradient dtLeg2 = primaryTauD.negate().add(delta);

        // Transit state where the satellite reflected the signal from secondary to primary station
        final SpacecraftState transitStateLeg2 = state.shiftedBy(dtLeg2.getValue());

        // Transit state pv of leg2 (re)computed with gradient
        final TimeStampedFieldPVCoordinates<Gradient> transitStateLeg2PV = pvaDS.shiftedBy(dtLeg2);

        // Uplink time of flight from secondary station to transit state of leg2
        final FieldPVCoordinatesProvider<Gradient> secondaryCoordsProvider = getSecondaryStation().getFieldPVCoordinatesProvider(nbParams, indices);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputerSecondaryU = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, secondaryCoordsProvider);
        final Gradient secondaryTauU = fieldComputerSecondaryU.computeDelay(transitStateLeg2PV.getPosition(), transitStateLeg2PV.getDate(), state.getFrame());

        // Total time of flight for leg 2
        final Gradient tauLeg2 = primaryTauD.add(secondaryTauU);

        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of rebound of the signal to secondary station
        final FieldAbsoluteDate<Gradient> reboundDateDS = measurementDateDS.shiftedBy(tauLeg2.negate());
        final TimeStampedFieldPVCoordinates<Gradient> secondaryRebound = secondaryCoordsProvider.getPVCoordinates(reboundDateDS, state.getFrame());

        // Downlink time of flight from transitStateLeg1 to secondary station at rebound date
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputerD = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, new FieldAbsolutePVCoordinates<>(state.getFrame(), transitStateLeg2PV));
        final Gradient secondaryTauD = fieldComputerD.computeDelay(transitStateLeg2PV.getDate(), secondaryRebound.getPosition(), reboundDateDS, state.getFrame());


        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final Gradient dtLeg1 = dtLeg2.subtract(secondaryTauU).subtract(secondaryTauD);

        // Transit state pv of leg2 (re)computed with gradients
        final TimeStampedFieldPVCoordinates<Gradient> transitStateLeg1PV = pvaDS.shiftedBy(dtLeg1);

        // Primary station PV in inertial frame at approximate emission date
        final FieldAbsoluteDate<Gradient> approxEmissionDate =
                        measurementDateDS.shiftedBy(-2 * (secondaryTauU.getValue() + primaryTauD.getValue()));
        final FieldPVCoordinatesProvider<Gradient> primaryCoordsProvider = getPrimaryStation().getFieldPVCoordinatesProvider(nbParams, indices);
        final TimeStampedFieldPVCoordinates<Gradient> QPrimaryApprox = primaryCoordsProvider.getPVCoordinates(approxEmissionDate, state.getFrame());

        // Uplink time of flight from primary station to transit state of leg1
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldComputerPrimaryU = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, new FieldAbsolutePVCoordinates<>(state.getFrame(), QPrimaryApprox));
        final Gradient primaryTauU = fieldComputerPrimaryU.computeDelay(transitStateLeg1PV.getPosition(), transitStateLeg1PV.getDate(), state.getFrame());

        // Primary station PV in inertial frame at exact emission date
        final AbsoluteDate emissionDate = transitStateLeg1PV.getDate().toAbsoluteDate().shiftedBy(-primaryTauU.getValue());
        final TimeStampedPVCoordinates primaryDeparture = getPrimaryStation().getPVCoordinatesProvider()
                .getPVCoordinates(emissionDate, state.getFrame());

        // Total time of flight for leg 1
        final Gradient tauLeg1 = secondaryTauD.add(primaryTauU);


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of rebound of the signal at the secondary station
        // Or t" = t -primaryTauD -secondaryTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +secondaryTauU to get transitStateLeg2
        //  - -secondaryTauD to get transitStateLeg1
        final EstimatedMeasurement<TurnAroundRange> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitStateLeg2.shiftedBy(-secondaryTauU.getValue())
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       primaryDeparture,
                                                       transitStateLeg1PV.toTimeStampedPVCoordinates(),
                                                       secondaryRebound.toTimeStampedPVCoordinates(),
                                                       transitStateLeg2.getPVCoordinates(),
                                                       primaryArrival.toTimeStampedPVCoordinates()
                                                   });

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final Gradient turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());

        // Turn-around range first order derivatives with respect to state
        final double[] derivatives = turnAroundRange.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
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
