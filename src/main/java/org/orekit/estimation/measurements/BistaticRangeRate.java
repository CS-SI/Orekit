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

        final SpacecraftState state = states[0];

        // coordinates of the spacecraft
        final TimeStampedPVCoordinates pva = state.getPVCoordinates();

        // transform between receiver station frame and inertial frame
        // at the real date of measurement, i.e. taking station clock offset into account
        final Transform receiverToInertial = getReceiverStation().getOffsetToInertial(state.getFrame(), getDate(), false);
        final AbsoluteDate measurementDate = receiverToInertial.getDate();

        // Receiver PV in inertial frame at the end of the downlink leg
        final TimeStampedPVCoordinates receiverPV =
                        receiverToInertial.transformPVCoordinates(new TimeStampedPVCoordinates(measurementDate,
                                                                                               Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final double tauD       = signalTimeOfFlight(pva, receiverPV.getPosition(), measurementDate);
        final double delta      = measurementDate.durationFrom(state.getDate());
        final double deltaMTauD = delta - tauD;

        // Transit state
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD);

        // Transit PV
        final TimeStampedPVCoordinates transitPV = pva.shiftedBy(deltaMTauD);

        // Downlink range-rate
        final EstimatedMeasurementBase<BistaticRangeRate> evalDownlink =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                    receiverPV, transitPV, transitState);

        // transform between emitter station frame and inertial frame at the transit date
        // clock offset from receiver is already compensated
        final Transform emitterToInertial = getEmitterStation().getOffsetToInertial(state.getFrame(), transitPV.getDate(), true);

        // emitter PV in inertial frame at the end of the uplink leg
        final TimeStampedPVCoordinates emitterPV =
                        emitterToInertial.transformPVCoordinates(new TimeStampedPVCoordinates(transitPV.getDate(),
                                                                                              Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

        // Uplink delay
        final double tauU = signalTimeOfFlight(emitterPV, transitPV.getPosition(), transitPV.getDate());

        // emitter position in inertial frame at the end of the uplink leg
        final TimeStampedPVCoordinates emitterUplink = emitterPV.shiftedBy(-tauU);

        // Uplink range-rate
        final EstimatedMeasurementBase<BistaticRangeRate> evalUplink =
                        oneWayTheoreticalEvaluation(iteration, evaluation, false,
                                                    emitterUplink, transitPV, transitState);

        // combine uplink and downlink values
        final EstimatedMeasurementBase<BistaticRangeRate> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       evalDownlink.getStates(),
                                                       new TimeStampedPVCoordinates[] {
                                                           evalUplink.getParticipants()[0],
                                                           evalDownlink.getParticipants()[0],
                                                           evalDownlink.getParticipants()[1]
                                                       });
        estimated.setEstimatedValue(evalDownlink.getEstimatedValue()[0] + evalUplink.getEstimatedValue()[0]);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<BistaticRangeRate> theoreticalEvaluation(final int iteration,
                                                                            final int evaluation,
                                                                            final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Bistatic range rate derivatives are computed with respect to:
        // - Spacecraft state in inertial frame
        // - Emitter station parameters
        // - Receiver station parameters
        // --------------------------
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - stations' parameters (stations' offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<String, Integer>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // we have to check for duplicate keys because emitter and receiver stations share
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
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // coordinates of the spacecraft as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaG = getCoordinates(state, 0, nbParams);

        // transform between receiver station frame and inertial frame
        // at the real date of measurement, i.e. taking station clock offset into account
        final FieldTransform<Gradient> receiverToInertial =
                        getReceiverStation().getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> measurementDateG = receiverToInertial.getFieldDate();

        // Receiver PV in inertial frame at the end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV =
                        receiverToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(measurementDateG,
                                                                                                      zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final Gradient tauD       = signalTimeOfFlight(pvaG, receiverPV.getPosition(), measurementDateG);
        final Gradient delta      = measurementDateG.durationFrom(state.getDate());
        final Gradient deltaMTauD = delta.subtract(tauD);

        // Transit state
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());

        // Transit PV
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = pvaG.shiftedBy(deltaMTauD);

        // Downlink range-rate
        final EstimatedMeasurement<BistaticRangeRate> evalDownlink =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                    receiverPV, transitPV, transitState, indices);

        // transform between emitter station frame and inertial frame at the transit date
        // clock offset from receiver is already compensated
        final FieldTransform<Gradient> emitterToInertial =
                        getEmitterStation().getOffsetToInertial(state.getFrame(), transitPV.getDate(), nbParams, indices);

        // emitter PV in inertial frame at the end of the uplink leg
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV =
                        emitterToInertial.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(transitPV.getDate(),
                                                                                                     zero, zero, zero));

        // Uplink delay
        final Gradient tauU = signalTimeOfFlight(emitterPV, transitPV.getPosition(), transitPV.getDate());

        // emitter position in inertial frame at the end of the uplink leg
        final TimeStampedFieldPVCoordinates<Gradient> emitterUplink = emitterPV.shiftedBy(tauU.negate());

        // Uplink range-rate
        final EstimatedMeasurement<BistaticRangeRate> evalUplink =
                        oneWayTheoreticalEvaluation(iteration, evaluation, false,
                                                    emitterUplink, transitPV, transitState, indices);

        // combine uplink and downlink values
        final EstimatedMeasurement<BistaticRangeRate> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   evalDownlink.getStates(),
                                                   new TimeStampedPVCoordinates[] {
                                                       evalUplink.getParticipants()[0],
                                                       evalDownlink.getParticipants()[0],
                                                       evalDownlink.getParticipants()[1]
                                                   });
        estimated.setEstimatedValue(evalDownlink.getEstimatedValue()[0] + evalUplink.getEstimatedValue()[0]);

        // combine uplink and downlink partial derivatives with respect to state
        final double[][] sd1 = evalDownlink.getStateDerivatives(0);
        final double[][] sd2 = evalUplink.getStateDerivatives(0);
        final double[][] sd  = new double[sd1.length][sd1[0].length];
        for (int i = 0; i < sd.length; ++i) {
            for (int j = 0; j < sd[0].length; ++j) {
                sd[i][j] = sd1[i][j] + sd2[i][j];
            }
        }
        estimated.setStateDerivatives(0, sd);

        // combine uplink and downlink partial derivatives with respect to parameters
        evalDownlink.getDerivativesDrivers().forEach(driver -> {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final double[] pd1 = evalDownlink.getParameterDerivatives(driver, span.getStart());
                final double[] pd2 = evalUplink.getParameterDerivatives(driver, span.getStart());
                final double[] pd  = new double[pd1.length];
                for (int i = 0; i < pd.length; ++i) {
                    pd[i] = pd1[i] + pd2[i];
                }
                estimated.setParameterDerivatives(driver, span.getStart(), pd);
            }
        });

        return estimated;

    }

    /** Evaluate range rate measurement in one-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param downlink indicator for downlink leg
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @return theoretical value for the current leg
     * @since 12.0
     */
    private EstimatedMeasurementBase<BistaticRangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation,
                                                                                    final boolean downlink,
                                                                                    final TimeStampedPVCoordinates stationPV,
                                                                                    final TimeStampedPVCoordinates transitPV,
                                                                                    final SpacecraftState transitState) {

        // prepare the evaluation
        final EstimatedMeasurementBase<BistaticRangeRate> estimated =
            new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                           new SpacecraftState[] {
                                               transitState
                                           }, new TimeStampedPVCoordinates[] {
                                               downlink ? transitPV : stationPV,
                                               downlink ? stationPV : transitPV
                                           });

        // range rate value
        final Vector3D stationPosition  = stationPV.getPosition();
        final Vector3D relativePosition = stationPosition.subtract(transitPV.getPosition());

        final Vector3D stationVelocity  = stationPV.getVelocity();
        final Vector3D relativeVelocity = stationVelocity.subtract(transitPV.getVelocity());

        // radial direction
        final Vector3D lineOfSight      = relativePosition.normalize();

        // range rate
        final double rangeRate = Vector3D.dotProduct(relativeVelocity, lineOfSight);

        estimated.setEstimatedValue(rangeRate);

        return estimated;

    }

    /** Evaluate range rate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param downlink indicator for downlink leg
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @param indices indices of the estimated parameters in derivatives computations
     * @return theoretical value for the current leg
     */
    private EstimatedMeasurement<BistaticRangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                                final TimeStampedFieldPVCoordinates<Gradient> stationPV,
                                                                                final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                                                                final SpacecraftState transitState,
                                                                                final Map<String, Integer> indices) {

        // prepare the evaluation
        final EstimatedMeasurement<BistaticRangeRate> estimated =
            new EstimatedMeasurement<BistaticRangeRate>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            transitState
                                                        }, new TimeStampedPVCoordinates[] {
                                                            (downlink ? transitPV : stationPV).toTimeStampedPVCoordinates(),
                                                            (downlink ? stationPV : transitPV).toTimeStampedPVCoordinates()
                                                        });

        // range rate value
        final FieldVector3D<Gradient> stationPosition  = stationPV.getPosition();
        final FieldVector3D<Gradient> relativePosition = stationPosition.subtract(transitPV.getPosition());

        final FieldVector3D<Gradient> stationVelocity  = stationPV.getVelocity();
        final FieldVector3D<Gradient> relativeVelocity = stationVelocity.subtract(transitPV.getVelocity());

        // radial direction
        final FieldVector3D<Gradient> lineOfSight      = relativePosition.normalize();

        // range rate
        final Gradient rangeRate = FieldVector3D.dotProduct(relativeVelocity, lineOfSight);

        estimated.setEstimatedValue(rangeRate.getValue());

        // compute partial derivatives of (rr) with respect to spacecraft state Cartesian coordinates
        final double[] derivatives = rangeRate.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
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
