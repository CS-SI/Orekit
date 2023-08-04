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
import java.util.Map;

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

/** Class modeling one-way or two-way range rate measurement between two vehicles.
 * One-way range rate (or Doppler) measurements generally apply to specific satellites
 * (e.g. GNSS, DORIS), where a signal is transmitted from a satellite to a
 * measuring station.
 * Two-way range rate measurements are applicable to any system. The signal is
 * transmitted to the (non-spinning) satellite and returned by a transponder
 * (or reflected back)to the same measuring station.
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 *
 * @author Thierry Ceolin
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRate extends GroundReceiverMeasurement<RangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "RangeRate";

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoway if true, this is a two-way measurement
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoway, final ObservableSatellite satellite) {
        super(station, twoway, date, rangeRate, sigma, baseWeight, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<RangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                          final int evaluation,
                                                                                          final SpacecraftState[] states) {

        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);
        final TimeStampedPVCoordinates transitPV = common.getTransitPV();

        // one-way (downlink) range-rate
        final EstimatedMeasurementBase<RangeRate> evalOneWay1 =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                    common.getStationDownlink(),
                                                    transitPV,
                                                    common.getTransitState());
        final EstimatedMeasurementBase<RangeRate> estimated;
        if (isTwoWay()) {
            // one-way (uplink) light time correction
            final Transform offsetToInertialApproxUplink =
                            getStation().getOffsetToInertial(common.getState().getFrame(),
                                                             common.getStationDownlink().getDate().shiftedBy(-2 * common.getTauD()),
                                                             false);
            final AbsoluteDate approxUplinkDate = offsetToInertialApproxUplink.getDate();

            final TimeStampedPVCoordinates stationApproxUplink =
                            offsetToInertialApproxUplink.transformPVCoordinates(new TimeStampedPVCoordinates(approxUplinkDate,
                                                                                                             Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

            final double tauU = signalTimeOfFlight(stationApproxUplink, transitPV.getPosition(), transitPV.getDate());

            final TimeStampedPVCoordinates stationUplink =
                            stationApproxUplink.shiftedBy(transitPV.getDate().durationFrom(approxUplinkDate) - tauU);

            final EstimatedMeasurementBase<RangeRate> evalOneWay2 =
                            oneWayTheoreticalEvaluation(iteration, evaluation, false,
                                                        stationUplink, transitPV, common.getTransitState());

            // combine uplink and downlink values
            estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       evalOneWay1.getStates(),
                                                       new TimeStampedPVCoordinates[] {
                                                           evalOneWay2.getParticipants()[0],
                                                           evalOneWay1.getParticipants()[0],
                                                           evalOneWay1.getParticipants()[1]
                                                       });
            estimated.setEstimatedValue(0.5 * (evalOneWay1.getEstimatedValue()[0] + evalOneWay2.getEstimatedValue()[0]));

        } else {
            estimated = evalOneWay1;
        }

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<RangeRate> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                    final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Range-rate derivatives are computed with respect to spacecraft state in inertial frame
        // and station position in station's offset frame
        // -------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, clock drift, station offsets, pole, prime meridian...)
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final int nbParams = common.getTauD().getFreeParameters();
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = common.getTransitPV();

        // one-way (downlink) range-rate
        final EstimatedMeasurement<RangeRate> evalOneWay1 =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                    common.getStationDownlink(), transitPV,
                                                    common.getTransitState(), common.getIndices(), nbParams);
        final EstimatedMeasurement<RangeRate> estimated;
        if (isTwoWay()) {
            // one-way (uplink) light time correction
            final FieldTransform<Gradient> offsetToInertialApproxUplink =
                            getStation().getOffsetToInertial(state.getFrame(),
                                                             common.getStationDownlink().getDate().shiftedBy(common.getTauD().multiply(-2)),
                                                             nbParams, common.getIndices());
            final FieldAbsoluteDate<Gradient> approxUplinkDateDS =
                            offsetToInertialApproxUplink.getFieldDate();

            final FieldVector3D<Gradient> zero = FieldVector3D.getZero(common.getTauD().getField());
            final TimeStampedFieldPVCoordinates<Gradient> stationApproxUplink =
                            offsetToInertialApproxUplink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxUplinkDateDS,
                                                                                                                    zero, zero, zero));

            final Gradient tauU = signalTimeOfFlight(stationApproxUplink, transitPV.getPosition(), transitPV.getDate());

            final TimeStampedFieldPVCoordinates<Gradient> stationUplink =
                            stationApproxUplink.shiftedBy(transitPV.getDate().durationFrom(approxUplinkDateDS).subtract(tauU));

            final EstimatedMeasurement<RangeRate> evalOneWay2 =
                            oneWayTheoreticalEvaluation(iteration, evaluation, false,
                                                        stationUplink, transitPV, common.getTransitState(),
                                                        common.getIndices(), nbParams);

            // combine uplink and downlink values
            estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   evalOneWay1.getStates(),
                                                   new TimeStampedPVCoordinates[] {
                                                       evalOneWay2.getParticipants()[0],
                                                       evalOneWay1.getParticipants()[0],
                                                       evalOneWay1.getParticipants()[1]
                                                   });
            estimated.setEstimatedValue(0.5 * (evalOneWay1.getEstimatedValue()[0] + evalOneWay2.getEstimatedValue()[0]));

            // combine uplink and downlink partial derivatives with respect to state
            final double[][] sd1 = evalOneWay1.getStateDerivatives(0);
            final double[][] sd2 = evalOneWay2.getStateDerivatives(0);
            final double[][] sd = new double[sd1.length][sd1[0].length];
            for (int i = 0; i < sd.length; ++i) {
                for (int j = 0; j < sd[0].length; ++j) {
                    sd[i][j] = 0.5 * (sd1[i][j] + sd2[i][j]);
                }
            }
            estimated.setStateDerivatives(0, sd);

            // combine uplink and downlink partial derivatives with respect to parameters
            evalOneWay1.getDerivativesDrivers().forEach(driver -> {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    final double[] pd1 = evalOneWay1.getParameterDerivatives(driver, span.getStart());
                    final double[] pd2 = evalOneWay2.getParameterDerivatives(driver, span.getStart());
                    final double[] pd = new double[pd1.length];
                    for (int i = 0; i < pd.length; ++i) {
                        pd[i] = 0.5 * (pd1[i] + pd2[i]);
                    }
                    estimated.setParameterDerivatives(driver, span.getStart(), pd);
                }
            });

        } else {
            estimated = evalOneWay1;
        }

        return estimated;

    }

    /** Evaluate measurement in one-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param downlink indicator for downlink leg
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @return theoretical value
     * @see #evaluate(SpacecraftStatet)
     * @since 12.0
     */
    private EstimatedMeasurementBase<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                            final TimeStampedPVCoordinates stationPV,
                                                                            final TimeStampedPVCoordinates transitPV,
                                                                            final SpacecraftState transitState) {

        // prepare the evaluation
        final EstimatedMeasurementBase<RangeRate> estimated =
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

        // line of sight velocity
        final double lineOfSightVelocity = Vector3D.dotProduct(relativeVelocity, lineOfSight);

        // range rate
        double rangeRate = lineOfSightVelocity;

        if (!isTwoWay()) {
            // clock drifts, taken in account only in case of one way
            final ObservableSatellite satellite    = getSatellites().get(0);
            final double              dtsDot       = satellite.getClockDriftDriver().getValue(transitState.getDate());
            final double              dtgDot       = getStation().getClockDriftDriver().getValue(stationPV.getDate());

            final double clockDriftBiais = (dtgDot - dtsDot) * Constants.SPEED_OF_LIGHT;

            rangeRate = rangeRate + clockDriftBiais;
        }

        estimated.setEstimatedValue(rangeRate);

        return estimated;

    }

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param downlink indicator for downlink leg
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @param indices indices of the estimated parameters in derivatives computations
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     * @see #evaluate(SpacecraftStatet)
     */
    private EstimatedMeasurement<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                        final TimeStampedFieldPVCoordinates<Gradient> stationPV,
                                                                        final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                                                        final SpacecraftState transitState,
                                                                        final Map<String, Integer> indices,
                                                                        final int nbParams) {

        // prepare the evaluation
        final EstimatedMeasurement<RangeRate> estimated =
                        new EstimatedMeasurement<RangeRate>(this, iteration, evaluation,
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

        // line of sight velocity
        final Gradient lineOfSightVelocity = FieldVector3D.dotProduct(relativeVelocity, lineOfSight);

        // range rate
        Gradient rangeRate = lineOfSightVelocity;

        if (!isTwoWay()) {
            // clock drifts, taken in account only in case of one way
            final ObservableSatellite satellite    = getSatellites().get(0);
            final Gradient            dtsDot       = satellite.getClockDriftDriver().getValue(nbParams, indices, transitState.getDate());
            final Gradient            dtgDot       = getStation().getClockDriftDriver().getValue(nbParams, indices, stationPV.getDate().toAbsoluteDate());

            final Gradient clockDriftBiais = dtgDot.subtract(dtsDot).multiply(Constants.SPEED_OF_LIGHT);

            rangeRate = rangeRate.add(clockDriftBiais);
        }

        estimated.setEstimatedValue(rangeRate.getValue());

        // compute partial derivatives of (rr) with respect to spacecraft state Cartesian coordinates
        final double[] derivatives = rangeRate.getGradient();
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
