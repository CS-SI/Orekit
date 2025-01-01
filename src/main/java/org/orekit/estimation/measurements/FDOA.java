/* Copyright 2002-2025 Mark Rutten
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
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

/** Class modeling a Frequency Difference of Arrival measurement with a satellite as emitter
 * and two ground stations as receivers.
 * <p>
 * FDOA measures the difference in signal arrival frequency between the emitter and receivers,
 * corresponding to a difference in range-rate from the two receivers to the emitter.
 * </p><p>
 * The date of the measurement corresponds to the reception of the signal by the prime station.
 * The measurement corresponds to the frequency of the signal received at the prime station at
 * the date of the measurement minus the frequency of the signal received at the second station:
 * <code>fdoa = f<sub>1</sub> - f<sub>2</sub></code>
 * </p><p>
 * The motion of the stations and the satellite during the signal flight time are taken into account.
 * </p>
 * @author Mark Rutten
 * @since 12.0
 */
public class FDOA extends GroundReceiverMeasurement<FDOA> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "FDOA";

    /** Centre frequency of the signal emitted from the satellite. */
    private final double centreFrequency;

    /** Second ground station, the one that gives the measurement, i.e. the delay. */
    private final GroundStation secondStation;

    /** Simple constructor.
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation ground station that gives the measurement
     * @param centreFrequency satellite emitter frequency (Hz)
     * @param date date of the measurement
     * @param fdoa observed value (Hz)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public FDOA(final GroundStation primeStation, final GroundStation secondStation,
                final double centreFrequency,
                final AbsoluteDate date, final double fdoa, final double sigma,
                final double baseWeight, final ObservableSatellite satellite) {
        super(primeStation, false, date, fdoa, sigma, baseWeight, satellite);

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
        this.centreFrequency = centreFrequency;
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
    @Override
    protected EstimatedMeasurementBase<FDOA> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
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
        final double tau2 = TDOA.forwardSignalTimeOfFlight(secondApprox, emitterPV.getPosition(), emitterDate);

        // Secondary station PV in inertial frame at receive at second station
        final TimeStampedPVCoordinates secondPV = secondApprox.shiftedBy(tau2);

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final double offset1 = getPrimeStation().getClockOffsetDriver().getValue(emitterDate);
        final double offset2 = getSecondStation().getClockOffsetDriver().getValue(emitterDate);
        final double tdoa = (common.getTauD() + offset1) - (tau2 + offset2);

        // Evaluate the FDOA value
        // -------------------------------------------
        final EstimatedMeasurement<FDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] {
                            common.getTransitState()
                        },
                        new TimeStampedPVCoordinates[] {
                            emitterPV,
                            tdoa > 0.0 ? secondPV : common.getStationDownlink(),
                            tdoa > 0.0 ? common.getStationDownlink() : secondPV
                        });

        // Range-rate components
        final Vector3D primeDirection = common.getStationDownlink().getPosition()
                .subtract(emitterPV.getPosition()).normalize();
        final Vector3D secondDirection = secondPV.getPosition()
                .subtract(emitterPV.getPosition()).normalize();

        final Vector3D primeVelocity = common.getStationDownlink().getVelocity()
                .subtract(emitterPV.getVelocity());
        final Vector3D secondVelocity = secondPV.getVelocity()
                .subtract(emitterPV.getVelocity());

        // range rate difference
        final double rangeRateDifference = Vector3D.dotProduct(primeDirection, primeVelocity) -
                Vector3D.dotProduct(secondDirection, secondVelocity);

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue(rangeRateDifference * rangeRateToHz);

        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<FDOA> theoreticalEvaluation(final int iteration, final int evaluation,
                                                               final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // FDOA derivatives are computed with respect to spacecraft state in inertial frame
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
        final Gradient tau2 = TDOA.forwardSignalTimeOfFlight(secondApprox, emitterPV.getPosition(), emitterDate);

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
        final EstimatedMeasurement<FDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] {
                            common.getTransitState()
                        },
                        new TimeStampedPVCoordinates[] {
                            emitterPV.toTimeStampedPVCoordinates(),
                            tdoa > 0 ? pv2 : pv1,
                            tdoa > 0 ? pv1 : pv2
                        });

        // Range-rate components
        final FieldVector3D<Gradient> primeDirection = common.getStationDownlink().getPosition()
                .subtract(emitterPV.getPosition()).normalize();
        final FieldVector3D<Gradient> secondDirection = secondPV.getPosition()
                .subtract(emitterPV.getPosition()).normalize();

        final FieldVector3D<Gradient> primeVelocity = common.getStationDownlink().getVelocity()
                .subtract(emitterPV.getVelocity());
        final FieldVector3D<Gradient> secondVelocity = secondPV.getVelocity()
                .subtract(emitterPV.getVelocity());

        // range rate difference
        final Gradient rangeRateDifference = FieldVector3D.dotProduct(primeDirection, primeVelocity)
                .subtract(FieldVector3D.dotProduct(secondDirection, secondVelocity));

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        final Gradient fdoa = rangeRateDifference.multiply(rangeRateToHz);
        estimated.setEstimatedValue(fdoa.getValue());

        // Range first order derivatives with respect to state
        final double[] derivatives = fdoa.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set first order derivatives with respect to parameters
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

}
