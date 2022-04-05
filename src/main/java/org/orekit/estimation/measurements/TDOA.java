/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
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
public class TDOA extends AbstractMeasurement<TDOA> {

    /** Prime ground station, the one that gives the date of the measurement. */
    private final GroundStation primeStation;

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
        super(date, tdoa, sigma, baseWeight, Collections.singletonList(satellite));
        // add parameter drivers for the primary station
        addParameterDriver(primeStation.getClockOffsetDriver());
        addParameterDriver(primeStation.getEastOffsetDriver());
        addParameterDriver(primeStation.getNorthOffsetDriver());
        addParameterDriver(primeStation.getZenithOffsetDriver());
        addParameterDriver(primeStation.getPrimeMeridianOffsetDriver());
        addParameterDriver(primeStation.getPrimeMeridianDriftDriver());
        addParameterDriver(primeStation.getPolarOffsetXDriver());
        addParameterDriver(primeStation.getPolarDriftXDriver());
        addParameterDriver(primeStation.getPolarOffsetYDriver());
        addParameterDriver(primeStation.getPolarDriftYDriver());
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
        this.primeStation  = primeStation;
        this.secondStation = secondStation;
    }

    /** Get the prime ground station, the one that gives the date of the measurement.
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
    protected EstimatedMeasurement<TDOA> theoreticalEvaluation(final int iteration, final int evaluation,
                                                               final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // TDOA derivatives are computed with respect to:
        // - Spacecraft state in inertial frame
        // - Prime station parameters
        // - Second station parameters
        // --------------------------
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - stations' parameters (clock offset, station offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // we have to check for duplicate keys because primary and secondary station share
            // pole and prime meridian parameters names that must be considered
            // as one set only (they are combined together by the estimation engine)
            if (driver.isSelected() && !indices.containsKey(driver.getName())) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // coordinates of the spacecraft as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaG = getCoordinates(state, 0, nbParams);

        // transform between prime station frame and inertial frame
        // at the real date of measurement, i.e. taking station clock offset into account
        final FieldTransform<Gradient> primeToInert =
                        primeStation.getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> measurementDateG = primeToInert.getFieldDate();

        // prime station PV in inertial frame at the real date of the measurement
        final TimeStampedFieldPVCoordinates<Gradient> primePV =
                        primeToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(measurementDateG,
                                                                                                zero, zero, zero));

        // compute downlink delay from emitter to prime receiver
        final Gradient tau1 = signalTimeOfFlight(pvaG, primePV.getPosition(), measurementDateG);

        // elapsed time between state date and signal arrival to the prime receiver
        final Gradient dtMtau1 = measurementDateG.durationFrom(state.getDate()).subtract(tau1);

        // satellite state at signal emission
        final SpacecraftState emitterState = state.shiftedBy(dtMtau1.getValue());

        // satellite pv at signal emission (re)computed with gradient
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = pvaG.shiftedBy(dtMtau1);

        // second station PV in inertial frame at real date of signal reception
        TimeStampedFieldPVCoordinates<Gradient> secondPV;
        // initialize search loop of the reception date by second station
        Gradient tau2 = tau1;
        double delta;
        int count = 0;
        do {
            final double previous = tau2.getValue();
            // date of signal arrival on second receiver
            final AbsoluteDate dateAt2 = emitterState.getDate().shiftedBy(previous);
            // transform between second station frame and inertial frame
            // at the date of signal arrival, taking clock offset into account
            final FieldTransform<Gradient> secondToInert =
                            secondStation.getOffsetToInertial(state.getFrame(), dateAt2,
                                                              nbParams, indices);
            // second receiver position in inertial frame at the real date of signal reception
            secondPV = secondToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(secondToInert.getFieldDate(),
                                                                                                zero, zero, zero));
            // downlink delay from emitter to second receiver
            tau2 = linkDelay(emitterPV.getPosition(), secondPV.getPosition());

            // Change in the computed downlink delay
            delta = FastMath.abs(tau2.getValue() - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(tau2.getValue()));

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final Gradient offset1 = primeStation.getClockOffsetDriver().getValue(nbParams, indices);
        final Gradient offset2 = secondStation.getClockOffsetDriver().getValue(nbParams, indices);
        final Gradient tdoaG   = tau1.add(offset1).subtract(tau2.add(offset2));
        final double tdoa      = tdoaG.getValue();

        // Evaluate the TDOA value and derivatives
        // -------------------------------------------
        final TimeStampedPVCoordinates pv1 = primePV.toTimeStampedPVCoordinates();
        final TimeStampedPVCoordinates pv2 = secondPV.toTimeStampedPVCoordinates();
        final EstimatedMeasurement<TDOA> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       emitterState
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       emitterPV.toTimeStampedPVCoordinates(),
                                                       tdoa > 0 ? pv2 : pv1,
                                                       tdoa > 0 ? pv1 : pv2
                                                   });

        // set TDOA value
        estimated.setEstimatedValue(tdoa);

        // set partial derivatives with respect to state
        final double[] derivatives = tdoaG.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index]);
            }
        }

        return estimated;

    }

    /** Compute propagation delay on a link.
     * @param emitter  the position of the emitter
     * @param receiver the position of the receiver (same frame as emitter)
     * @return the propagation delay
     */
    private Gradient linkDelay(final FieldVector3D<Gradient> emitter,
                               final FieldVector3D<Gradient> receiver) {
        return receiver.distance(emitter).divide(Constants.SPEED_OF_LIGHT);
    }
}
