/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
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
public class RangeRate extends AbstractMeasurement<RangeRate> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

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
        super(date, rangeRate, sigma, baseWeight, Arrays.asList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        this.station = station;
        this.twoway  = twoway;
    }

    /** Check if the instance represents a two-way measurement.
     * @return true if the instance represents a two-way measurement
     */
    public boolean isTwoWay() {
        return twoway;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
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
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final DSFactory factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure> field = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a derivative structure
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS = getCoordinates(state, 0, factory);

        // transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), getDate(), factory, indices);
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS =
                        offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final DerivativeStructure tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state
        final DerivativeStructure   delta        = downlinkDateDS.durationFrom(state.getDate());
        final DerivativeStructure   deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(deltaMTauD.getValue());

        // Transit state (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitPV = pvaDS.shiftedBy(deltaMTauD);

        // one-way (downlink) range-rate
        final EstimatedMeasurement<RangeRate> evalOneWay1 =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                    stationDownlink, transitPV, transitState, indices);
        final EstimatedMeasurement<RangeRate> estimated;
        if (twoway) {
            // one-way (uplink) light time correction
            final FieldTransform<DerivativeStructure> offsetToInertialApproxUplink =
                            station.getOffsetToInertial(state.getFrame(),
                                                        downlinkDateDS.shiftedBy(tauD.multiply(-2)), factory, indices);
            final FieldAbsoluteDate<DerivativeStructure> approxUplinkDateDS =
                            offsetToInertialApproxUplink.getFieldDate();

            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationApproxUplink =
                            offsetToInertialApproxUplink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxUplinkDateDS,
                                                                                                                    zero, zero, zero));

            final DerivativeStructure tauU = signalTimeOfFlight(stationApproxUplink, transitPV.getPosition(), transitPV.getDate());

            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationUplink =
                            stationApproxUplink.shiftedBy(transitPV.getDate().durationFrom(approxUplinkDateDS).subtract(tauU));

            final EstimatedMeasurement<RangeRate> evalOneWay2 =
                            oneWayTheoreticalEvaluation(iteration, evaluation, false,
                                                        stationUplink, transitPV, transitState, indices);

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
                final double[] pd1 = evalOneWay1.getParameterDerivatives(driver);
                final double[] pd2 = evalOneWay2.getParameterDerivatives(driver);
                final double[] pd = new double[pd1.length];
                for (int i = 0; i < pd.length; ++i) {
                    pd[i] = 0.5 * (pd1[i] + pd2[i]);
                }
                estimated.setParameterDerivatives(driver, pd);
            });

        } else {
            estimated = evalOneWay1;
        }

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
     * @return theoretical value
     * @see #evaluate(SpacecraftStatet)
     */
    private EstimatedMeasurement<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationPV,
                                                                        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitPV,
                                                                        final SpacecraftState transitState,
                                                                        final Map<String, Integer> indices) {

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
        final FieldVector3D<DerivativeStructure> stationPosition  = stationPV.getPosition();
        final FieldVector3D<DerivativeStructure> relativePosition = stationPosition.subtract(transitPV.getPosition());

        final FieldVector3D<DerivativeStructure> stationVelocity  = stationPV.getVelocity();
        final FieldVector3D<DerivativeStructure> relativeVelocity = stationVelocity.subtract(transitPV.getVelocity());

        // radial direction
        final FieldVector3D<DerivativeStructure> lineOfSight      = relativePosition.normalize();

        // range rate
        final DerivativeStructure rangeRate = FieldVector3D.dotProduct(relativeVelocity, lineOfSight);

        estimated.setEstimatedValue(rangeRate.getValue());

        // compute partial derivatives of (rr) with respect to spacecraft state Cartesian coordinates
        final double[] derivatives = rangeRate.getAllDerivatives();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 1, 7));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index + 1]);
            }
        }

        return estimated;

    }

}
