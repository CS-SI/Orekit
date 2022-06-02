/* Copyright 2002-2022 Mark Rutten
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
import java.util.Collections;
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
public class BistaticRange extends AbstractMeasurement<BistaticRange> {

    /**
     * Ground station from which transmission is made.
     */
    private final GroundStation emitter;

    /**
     * Ground station from which measurement is performed.
     */
    private final GroundStation receiver;

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
        super(date, range, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(emitter.getClockOffsetDriver());
        addParameterDriver(emitter.getEastOffsetDriver());
        addParameterDriver(emitter.getNorthOffsetDriver());
        addParameterDriver(emitter.getZenithOffsetDriver());
        addParameterDriver(emitter.getPrimeMeridianOffsetDriver());
        addParameterDriver(emitter.getPrimeMeridianDriftDriver());
        addParameterDriver(emitter.getPolarOffsetXDriver());
        addParameterDriver(emitter.getPolarDriftXDriver());
        addParameterDriver(emitter.getPolarOffsetYDriver());
        addParameterDriver(emitter.getPolarDriftYDriver());

        addParameterDriver(receiver.getClockOffsetDriver());
        addParameterDriver(receiver.getEastOffsetDriver());
        addParameterDriver(receiver.getNorthOffsetDriver());
        addParameterDriver(receiver.getZenithOffsetDriver());
        addParameterDriver(receiver.getPrimeMeridianOffsetDriver());
        addParameterDriver(receiver.getPrimeMeridianDriftDriver());
        addParameterDriver(receiver.getPolarOffsetXDriver());
        addParameterDriver(receiver.getPolarDriftXDriver());
        addParameterDriver(receiver.getPolarOffsetYDriver());
        addParameterDriver(receiver.getPolarDriftYDriver());

        this.emitter = emitter;
        this.receiver = receiver;
    }

    public GroundStation getEmitterStation() {
        return emitter;
    }

    public GroundStation getReceiverStation() {
        return receiver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EstimatedMeasurement<BistaticRange> theoreticalEvaluation(final int iteration,
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
                indices.put(driver.getName(), nbParams++);
            }
        }
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // transform between station and inertial frame, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialRx =
                receiver.getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> downlinkDateDS = offsetToInertialRx.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationReceiver =
                offsetToInertialRx.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                        zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final Gradient tauD = signalTimeOfFlight(pvaDS, stationReceiver.getPosition(), downlinkDateDS);

        // Transit state & Transit state (re)computed with gradients
        final Gradient delta = downlinkDateDS.durationFrom(state.getDate());
        final Gradient deltaMTauD = tauD.negate().add(delta);
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());
        final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

        // transform between secondary station topocentric frame (east-north-zenith) and inertial frame expressed as gradients
        // The components of secondary station's position in offset frame are the 3 last derivative parameters
        final FieldAbsoluteDate<Gradient> transitDate = downlinkDateDS.shiftedBy(tauD.negate());
        final FieldTransform<Gradient> offsetToInertialTxApprox =
                emitter.getOffsetToInertial(state.getFrame(), transitDate, nbParams, indices);

        // Secondary station PV in inertial frame at transit time
        final TimeStampedFieldPVCoordinates<Gradient> transmitApprox =
                offsetToInertialTxApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(transitDate,
                        zero, zero, zero));

        // Uplink time of flight from secondary station to transit state of leg2
        final Gradient tauU = signalTimeOfFlight(transmitApprox, transitStateDS.getPosition(), transitStateDS.getDate());

        // Total time of flight
        final Gradient tauTotal = deltaMTauD.negate().add(tauU);

        // Absolute date of transmission
        final FieldAbsoluteDate<Gradient> transmitDateDS = downlinkDateDS.shiftedBy(tauTotal);
        final FieldTransform<Gradient> transmitToInert =
                emitter.getOffsetToInertial(state.getFrame(), transmitDateDS, nbParams, indices);

        // Secondary station PV in inertial frame at rebound date on secondary station
        final TimeStampedFieldPVCoordinates<Gradient> stationTransmitter =
                transmitToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(transmitDateDS,
                        zero, zero, zero));

        // Prepare the evaluation
        final EstimatedMeasurement<BistaticRange> estimated = new EstimatedMeasurement<>(this,
                iteration, evaluation,
                new SpacecraftState[] {
                    transitState
                },
                new TimeStampedPVCoordinates[] {
                    stationReceiver.toTimeStampedPVCoordinates(),
                    transitStateDS.toTimeStampedPVCoordinates(),
                    stationTransmitter.toTimeStampedPVCoordinates()
                });

        // Range value
        final Gradient tau = tauD.add(tauU);
        final Gradient range = tau.multiply(Constants.SPEED_OF_LIGHT);

        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        final double[] derivatives = range.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index]);
            }
        }

        return estimated;
    }

}
