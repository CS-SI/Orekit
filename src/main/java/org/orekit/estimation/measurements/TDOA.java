/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a Time Difference of Arrival measurement with a satellite as emitter
 * and two ground stations as receivers.
 * <p>
 * TDOA measures the difference in signal arrival time between the emitter and receivers,
 * corresponding to a difference in ranges from the two receivers to the emitter.
 * </p><p>
 * The date of the measurement corresponds to the reception of the signal by the prime station.
 * The measurement corresponds to the date of the measurement minus
 * the date of reception of the signal by the second station:
 * <code>tdoa = tr<sub>1</sub> - tr<sub>2</sub></code>
 * </p><p>
 * The motion of the stations and the satellite during the signal flight time are taken into account.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOA extends GroundReceiverMeasurement<TDOA> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "TDOA";

    /** Second ground station, the one that gives the measurement, i.e. the delay. */
    private final GroundStation secondStation;

    /** Simple constructor.
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation ground station that gives the measurement
     * @param date date of the measurement
     * @param tdoa observed value (s)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public TDOA(final GroundStation primeStation, final GroundStation secondStation,
                final AbsoluteDate date, final double tdoa, final double sigma,
                final double baseWeight, final ObservableSatellite satellite) {
        super(primeStation, false, date, tdoa, sigma, baseWeight, satellite);

        // add parameter drivers for the secondary station
        addParameterDriver(secondStation.getClockOffsetDriver());
        addParameterDriver(secondStation.getEastOffsetDriver());
        addParameterDriver(secondStation.getNorthOffsetDriver());
        addParameterDriver(secondStation.getZenithOffsetDriver());
        addParameterDriver(secondStation.getPrimeMeridianOffsetDriver());
        addParameterDriver(secondStation.getPrimeMeridianDriftDriver());
        addParameterDriver(secondStation.getPolarOffsetXDriver());
        addParameterDriver(secondStation.getPolarDriftXDriver());
        addParameterDriver(secondStation.getPolarOffsetYDriver());
        addParameterDriver(secondStation.getPolarDriftYDriver());
        this.secondStation = secondStation;

    }

    /** Get the prime ground station, the one that gives the date of the measurement.
     * @return prime ground station
     */
    public GroundStation getPrimeStation() {
        return getStation();
    }

    /** Get the second ground station, the one that gives the measurement.
     * @return second ground station
     */
    public GroundStation getSecondStation() {
        return secondStation;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Override
    protected EstimatedMeasurementBase<TDOA> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
                                                                                     final SpacecraftState[] states) {

        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);
        final TimeStampedPVCoordinates emitterPV = common.getTransitPV();
        final AbsoluteDate emitterDate = emitterPV.getDate();

        // Approximate second location at transit time
        final Transform secondToInertial =
                getSecondStation().getOffsetToInertial(common.getState().getFrame(), emitterDate, true);
        final TimeStampedPVCoordinates secondApprox =
                secondToInertial.transformPVCoordinates(new TimeStampedPVCoordinates(emitterDate,
                        Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

        // Time of flight from emitter to second station
        final double tau2 = forwardSignalTimeOfFlight(secondApprox, emitterPV.getPosition(), emitterDate);

        // Secondary station PV in inertial frame at receive at second station
        final TimeStampedPVCoordinates secondPV = secondApprox.shiftedBy(tau2);

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final double offset1 = getPrimeStation().getClockOffsetDriver().getValue(emitterDate);
        final double offset2 = getSecondStation().getClockOffsetDriver().getValue(emitterDate);
        final double tdoa = (common.getTauD() + offset1) - (tau2 + offset2);

        // Evaluate the TDOA value
        // -------------------------------------------
        final EstimatedMeasurement<TDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] {
                            common.getTransitState()
                        },
                        new TimeStampedPVCoordinates[] {
                            emitterPV,
                            tdoa > 0.0 ? secondPV : common.getStationDownlink(),
                            tdoa > 0.0 ? common.getStationDownlink() : secondPV
                        });

        // set TDOA value
        estimated.setEstimatedValue(tdoa);

        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<TDOA> theoreticalEvaluation(final int iteration, final int evaluation,
                                                               final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // TDOA derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final int nbParams = common.getTauD().getFreeParameters();
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = common.getTransitPV();
        final FieldAbsoluteDate<Gradient> emitterDate = emitterPV.getDate();

        // Approximate secondary location (at emission time)
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(common.getTauD().getField());
        final FieldTransform<Gradient> secondToInertial =
                getSecondStation().getOffsetToInertial(state.getFrame(), emitterDate, nbParams, common.getIndices());
        final TimeStampedFieldPVCoordinates<Gradient> secondApprox =
                secondToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(emitterDate,
                        zero, zero, zero));

        // Time of flight from emitter to second station
        final Gradient tau2 = forwardSignalTimeOfFlight(secondApprox, emitterPV.getPosition(), emitterDate);

        // Second station coordinates at receive time
        final TimeStampedFieldPVCoordinates<Gradient> secondPV = secondApprox.shiftedBy(tau2);

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final Gradient offset1 = getPrimeStation().getClockOffsetDriver()
                .getValue(nbParams, common.getIndices(), emitterDate.toAbsoluteDate());
        final Gradient offset2 = getSecondStation().getClockOffsetDriver()
                .getValue(nbParams, common.getIndices(), emitterDate.toAbsoluteDate());
        final Gradient tdoaG   = common.getTauD().add(offset1).subtract(tau2.add(offset2));
        final double tdoa      = tdoaG.getValue();

        // Evaluate the TDOA value and derivatives
        // -------------------------------------------
        final TimeStampedPVCoordinates pv1 = common.getStationDownlink().toTimeStampedPVCoordinates();
        final TimeStampedPVCoordinates pv2 = secondPV.toTimeStampedPVCoordinates();
        final EstimatedMeasurement<TDOA> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getTransitState()
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       emitterPV.toTimeStampedPVCoordinates(),
                                                       tdoa > 0 ? pv2 : pv1,
                                                       tdoa > 0 ? pv1 : pv2
                                                   });

        // set TDOA value
        estimated.setEstimatedValue(tdoa);

        // set first order derivatives with respect to state
        final double[] derivatives = tdoaG.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        return estimated;
    }


    /** Compute propagation delay on a link leg (typically downlink or uplink).  This differs from signalTimeOfFlight
     * through <em>advancing</em> rather than delaying the emitter.
     *
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public static double forwardSignalTimeOfFlight(final TimeStampedPVCoordinates adjustableEmitterPV,
                                                   final Vector3D receiverPosition,
                                                   final AbsoluteDate signalArrivalDate) {

        // initialize emission date search loop assuming the state is already correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge very fast
        final double offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        double delay = offset;

        // search signal transit date, computing the signal travel in inertial frame
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous   = delay;
            final Vector3D transitP = adjustableEmitterPV.shiftedBy(delay - offset).getPosition();
            delay                   = receiverPosition.distance(transitP) * cReciprocal;
            delta                   = FastMath.abs(delay - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay));

        return delay;

    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).This differs from signalTimeOfFlight
     * through <em>advancing</em> rather than delaying the emitter.
     *
     * @param adjustableEmitterPV position/velocity of emitter that may be adjusted
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate},
     * in the same frame as {@code adjustableEmitterPV}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @return <em>positive</em> delay between signal emission and signal reception dates
     * @param <T> the type of the components
     */
    public static <T extends CalculusFieldElement<T>> T forwardSignalTimeOfFlight(final TimeStampedFieldPVCoordinates<T> adjustableEmitterPV,
                                                                                  final FieldVector3D<T> receiverPosition,
                                                                                  final FieldAbsoluteDate<T> signalArrivalDate) {

        // Initialize emission date search loop assuming the emitter PV is almost correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge extremely fast
        final T offset = signalArrivalDate.durationFrom(adjustableEmitterPV.getDate());
        T delay = offset;

        // search signal transit date, computing the signal travel in the frame shared by emitter and receiver
        final double cReciprocal = 1.0 / Constants.SPEED_OF_LIGHT;
        double delta;
        int count = 0;
        do {
            final double previous           = delay.getReal();
            final FieldVector3D<T> transitP = adjustableEmitterPV.shiftedBy(delay.subtract(offset)).getPosition();
            delay                           = receiverPosition.distance(transitP).multiply(cReciprocal);
            delta                           = FastMath.abs(delay.getReal() - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay.getReal()));

        return delay;
    }

}
