/* Copyright 2002-2026 Mark Rutten
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableReceiver;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableReceiver;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
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
public class FDOA extends AbstractMeasurement<FDOA> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "FDOA";

    /** Centre frequency of the signal emitted from the satellite. */
    private final double centreFrequency;

    /** First ground station to receiver the measurement. */
    private final GroundStation primeStation;

    /** Second ground station, the one that gives the measurement, i.e. the delay. */
    private final GroundStation secondStation;

    /** Constructor with default signal travel time model.
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
                final double centreFrequency, final AbsoluteDate date, final double fdoa, final double sigma,
                final double baseWeight, final ObservableSatellite satellite) {
        this(primeStation, secondStation, centreFrequency, date, fdoa, sigma, baseWeight, new SignalTravelTimeModel(),
                satellite);
    }

    /** Constructor.
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation ground station that gives the measurement
     * @param centreFrequency satellite emitter frequency (Hz)
     * @param date date of the measurement
     * @param fdoa observed value (Hz)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public FDOA(final GroundStation primeStation, final GroundStation secondStation,
                final double centreFrequency, final AbsoluteDate date, final double fdoa, final double sigma,
                final double baseWeight, final SignalTravelTimeModel signalTravelTimeModel,
                final ObservableSatellite satellite) {
        super(date, false, new double[] {fdoa}, new double[] {sigma}, new double[] {baseWeight},
                signalTravelTimeModel, Collections.singletonList(satellite));

        // add parameter drivers for the secondary station
        addParametersDrivers(primeStation.getParametersDrivers());
        addParametersDrivers(secondStation.getParametersDrivers());

        // Set values
        this.primeStation    = primeStation;
        this.secondStation   = secondStation;
        this.centreFrequency = centreFrequency;
    }

    /** Get centre frequency of carrier wave.
     * @return frequency value (Hz)
     */
    public double getCentreFrequency() {
        return centreFrequency;
    }

    /** Get the prime ground station, the one that receives the signal first.
     * @return prime ground station
     */
    public GroundStation getPrimeStation() {
        return primeStation;
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

        final CommonParametersWithoutDerivatives common =
            getPrimeStation().computeRemoteParametersWithout(states, getSatellites().get(0), getDate(), false);
        final TimeStampedPVCoordinates emitterPV = common.getTransitPV();
        final AbsoluteDate emitterDate = emitterPV.getDate();

        // Time of flight from emitter to second station
        final PVCoordinatesProvider secondPVCoordinatesProvider = getSecondStation().getPVCoordinatesProvider();
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = getSignalTravelTimeModel().getAdjustableReceiverComputer(secondPVCoordinatesProvider);
        final double tau2 = signalTimeOfFlight.computeDelay(emitterPV.getPosition(), emitterDate, states[0].getFrame());

        // Secondary station PV in inertial frame at receive at second station
        final TimeStampedPVCoordinates secondPV = secondPVCoordinatesProvider.getPVCoordinates(emitterDate.shiftedBy(tau2), states[0].getFrame());

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final double offset1 = getPrimeStation().getClockOffsetDriver().getValue(emitterDate);
        final double offset2 = getSecondStation().getClockOffsetDriver().getValue(emitterDate);
        final double tdoa    = (common.getTauD() + offset1) - (tau2 + offset2);

        // Evaluate the FDOA value
        // -------------------------------------------
        final EstimatedMeasurement<FDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] {
                            common.getTransitState()
                        },
                        new TimeStampedPVCoordinates[] {
                            emitterPV,
                            tdoa > 0.0 ? secondPV : common.getRemotePV(),
                            tdoa > 0.0 ? common.getRemotePV() : secondPV
                        });

        // Range-rate components
        final Vector3D primeDirection = common.getRemotePV().getPosition()
                .subtract(emitterPV.getPosition()).normalize();
        final Vector3D secondDirection = secondPV.getPosition()
                .subtract(emitterPV.getPosition()).normalize();

        final Vector3D primeVelocity = common.getRemotePV().getVelocity()
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

        // FDOA derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        final CommonParametersWithDerivatives common =
            getPrimeStation().computeRemoteParametersWith(states, getSatellites().get(0), getDate(), getParametersDrivers());
        final int nbParams = common.getTauD().getFreeParameters();
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = common.getTransitPV();
        final FieldAbsoluteDate<Gradient> emitterDate = emitterPV.getDate();

        // Obtain time at which signal arrives at second station from emitter
        final FieldPVCoordinatesProvider<Gradient> fieldPvCoordinatesProvider = getSecondStation().getFieldPVCoordinatesProvider(nbParams, common.getIndices());
        final FieldSignalTravelTimeAdjustableReceiver<Gradient> fieldComputer = getSignalTravelTimeModel()
                .getFieldAdjustableReceiverComputer(emitterDate.getField(), fieldPvCoordinatesProvider);
        final Gradient tau2 = fieldComputer.computeDelay(emitterPV.getPosition(), emitterDate, emitterDate, states[0].getFrame());

        // Second station coordinates at receive time
        final TimeStampedFieldPVCoordinates<Gradient> secondPV =
                                fieldPvCoordinatesProvider.getPVCoordinates(emitterDate.shiftedBy(tau2), states[0].getFrame());

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final Gradient offset1 = getPrimeStation().getClockOffsetDriver()
                                .getValue(nbParams, common.getIndices(), emitterDate.toAbsoluteDate());
        final Gradient offset2 = getSecondStation().getClockOffsetDriver()
                                .getValue(nbParams, common.getIndices(), emitterDate.toAbsoluteDate());
        final Gradient tdoaG   = common.getTauD().add(offset1).subtract(tau2.add(offset2));
        final double   tdoa    = tdoaG.getValue();

        // Evaluate the TDOA value and derivatives
        // -------------------------------------------
        final TimeStampedPVCoordinates pv1 = common.getRemotePV().toTimeStampedPVCoordinates();
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
        final FieldVector3D<Gradient> primeDirection = common.getRemotePV().getPosition()
                .subtract(emitterPV.getPosition()).normalize();
        final FieldVector3D<Gradient> secondDirection = secondPV.getPosition()
                .subtract(emitterPV.getPosition()).normalize();

        final FieldVector3D<Gradient> primeVelocity = common.getRemotePV().getVelocity()
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
