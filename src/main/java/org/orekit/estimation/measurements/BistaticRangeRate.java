/* Copyright 2002-2025 CS GROUP
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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a bistatic range rate measurement using
 *  an emitter ground station and a receiver ground station.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted from the emitter ground station</li>
 * <li>Reflected on the spacecraft</li>
 * <li>Received on the receiver ground station</li>
 * </ul>
 * The date of the measurement corresponds to the reception on ground of the reflected signal.
 * The quantity measured at the receiver is the bistatic radial velocity as the sum of the radial
 * velocities with respect to the two stations.
 * <p>
 * The motion of the stations and the spacecraft during the signal flight time are taken into account.
 * </p><p>
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 * </p>
 *
 * @author Pascal Parraud
 * @since 11.2
 */
public class BistaticRangeRate extends GroundReceiverMeasurement<BistaticRangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "BistaticRangeRate";

    /** Emitter ground station. */
    private final GroundStation emitter;

    /** Simple constructor.
     * @param emitter emitter ground station
     * @param receiver receiver ground station
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public BistaticRangeRate(final GroundStation emitter, final GroundStation receiver,
                             final AbsoluteDate date, final double rangeRate, final double sigma,
                             final double baseWeight, final ObservableSatellite satellite) {
        super(receiver, true, date, rangeRate, sigma, baseWeight, satellite);

        // add parameter drivers for the emitter, clock offset is not used
        addParameterDriver(emitter.getEastOffsetDriver());
        addParameterDriver(emitter.getNorthOffsetDriver());
        addParameterDriver(emitter.getZenithOffsetDriver());
        addParameterDriver(emitter.getPrimeMeridianOffsetDriver());
        addParameterDriver(emitter.getPrimeMeridianDriftDriver());
        addParameterDriver(emitter.getPolarOffsetXDriver());
        addParameterDriver(emitter.getPolarDriftXDriver());
        addParameterDriver(emitter.getPolarOffsetYDriver());
        addParameterDriver(emitter.getPolarDriftYDriver());

        this.emitter  = emitter;

    }

    /** Get the emitter ground station.
     * @return emitter ground station
     */
    public GroundStation getEmitterStation() {
        return emitter;
    }

    /** Get the receiver ground station.
     * @return receiver ground station
     */
    public GroundStation getReceiverStation() {
        return getStation();
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<BistaticRangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                  final int evaluation,
                                                                                                  final SpacecraftState[] states) {
        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);
        final TimeStampedPVCoordinates transitPV = common.getTransitPV();
        final AbsoluteDate transitDate = transitPV.getDate();

        // Approximate emitter location at transit time
        final Transform emitterToInertial =
                getEmitterStation().getOffsetToInertial(common.getState().getFrame(), transitDate, true);
        final TimeStampedPVCoordinates emitterApprox =
                emitterToInertial.transformPVCoordinates(new TimeStampedPVCoordinates(transitDate,
                        Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

        // Uplink time of flight from emitter station to transit state
        final double tauU = signalTimeOfFlightAdjustableEmitter(emitterApprox, transitPV.getPosition(), transitDate,
                                                                common.getState().getFrame());

        // Secondary station PV in inertial frame at rebound date on secondary station
        final TimeStampedPVCoordinates emitterPV = emitterApprox.shiftedBy(-tauU);

        // Prepare the evaluation
        final EstimatedMeasurementBase<BistaticRangeRate> estimated =
                new EstimatedMeasurementBase<>(this,
                        iteration, evaluation,
                        new SpacecraftState[] {
                            common.getTransitState()
                        },
                        new TimeStampedPVCoordinates[] {
                            common.getStationDownlink(),
                            transitPV,
                            emitterPV
                        });

        // Range-rate components
        final Vector3D receiverDirection = common.getStationDownlink().getPosition()
                .subtract(transitPV.getPosition()).normalize();
        final Vector3D emitterDirection = emitterPV.getPosition()
                .subtract(transitPV.getPosition()).normalize();

        final Vector3D receiverVelocity = common.getStationDownlink().getVelocity()
                .subtract(transitPV.getVelocity());
        final Vector3D emitterVelocity = emitterPV.getVelocity()
                .subtract(transitPV.getVelocity());

        // range rate
        final double rangeRate = Vector3D.dotProduct(receiverDirection, receiverVelocity) +
                 Vector3D.dotProduct(emitterDirection, emitterVelocity);
        estimated.setEstimatedValue(rangeRate);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<BistaticRangeRate> theoreticalEvaluation(final int iteration,
                                                                            final int evaluation,
                                                                            final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Bistatic range-rate derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final int nbParams = common.getTauD().getFreeParameters();
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = common.getTransitPV();
        final FieldAbsoluteDate<Gradient> transitDate = transitPV.getDate();

        // Approximate emitter location (at transit time)
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(common.getTauD().getField());
        final FieldTransform<Gradient> emitterToInertial =
                getEmitterStation().getOffsetToInertial(state.getFrame(), transitDate, nbParams, common.getIndices());
        final TimeStampedFieldPVCoordinates<Gradient> emitterApprox =
                emitterToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(transitDate,
                        zero, zero, zero));

        // Uplink time of flight from emiiter to transit state
        final Gradient tauU = signalTimeOfFlightAdjustableEmitter(emitterApprox, transitPV.getPosition(), transitPV.getDate(),
                                                                  state.getFrame());

        // Emitter coordinates at transmit time
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = emitterApprox.shiftedBy(tauU.negate());

        // Prepare the evaluation
        final EstimatedMeasurement<BistaticRangeRate> estimated = new EstimatedMeasurement<>(this,
                iteration, evaluation,
                new SpacecraftState[] {
                        common.getTransitState()
                },
                new TimeStampedPVCoordinates[] {
                        common.getStationDownlink().toTimeStampedPVCoordinates(),
                        common.getTransitPV().toTimeStampedPVCoordinates(),
                        emitterPV.toTimeStampedPVCoordinates()
                });

        // Range-rate components
        final FieldVector3D<Gradient> receiverDirection = common.getStationDownlink().getPosition()
                .subtract(transitPV.getPosition()).normalize();
        final FieldVector3D<Gradient> emitterDirection = emitterPV.getPosition()
                .subtract(transitPV.getPosition()).normalize();

        final FieldVector3D<Gradient> receiverVelocity = common.getStationDownlink().getVelocity()
                .subtract(transitPV.getVelocity());
        final FieldVector3D<Gradient> emitterVelocity = emitterPV.getVelocity()
                .subtract(transitPV.getVelocity());

        // range rate
        final Gradient rangeRate = FieldVector3D.dotProduct(receiverDirection, receiverVelocity)
                .add(FieldVector3D.dotProduct(emitterDirection, emitterVelocity));
        estimated.setEstimatedValue(rangeRate.getValue());

        // Range first order derivatives with respect to state
        final double[] derivatives = rangeRate.getGradient();
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

}
