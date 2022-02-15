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
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
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
public class RangeRate extends AbstractMeasurement<RangeRate>
{

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Enum indicating the time tag specification of a range observation. */
    private final TimeTagSpecificationType timeTagSpecificationType;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoway if true, this is a two-way measurement
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the timetag configuration of the provided range rate observation
     * @since 9.3
     */
    private RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoway, final ObservableSatellite satellite, final TimeTagSpecificationType timeTagSpecificationType) {
        super(date, rangeRate, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getClockDriftDriver());
        addParameterDriver(satellite.getClockDriftDriver());
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
        this.timeTagSpecificationType = timeTagSpecificationType;
    }

    /**
     * Range rate constructor for one or two-way measurements with timetag of
     * observed value set to reception time.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoway if true, this is a two-way measurement
     * @param satellite satellite related to this measurement
     * @since xx.xx
     */
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoway, final ObservableSatellite satellite) {
        this(station, date, rangeRate, sigma, baseWeight, twoway, satellite, TimeTagSpecificationType.RX);
    }

    /**
     * Range rate constructor for two-way measurements with a user specified observed value timetag specification
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the timetag configuration of the provided range rate observation
     * @since xx.xx
     **/
    public RangeRate(final GroundStation station, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final ObservableSatellite satellite, TimeTagSpecificationType timeTagSpecificationType) {
        this(station, date, rangeRate, sigma, baseWeight, true, satellite, timeTagSpecificationType);
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
        //  - 6..n - station parameters (clock offset, clock drift, station offsets, pole, prime meridian...)
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
        final FieldTransform<Gradient> offsetToInertialObsEpoch =
                station.getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> obsEpochFieldDate =
                offsetToInertialObsEpoch.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationObsEpoch =
                offsetToInertialObsEpoch.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(obsEpochFieldDate,
                        zero, zero, zero));

        // Delta to account for the case in which the state is provided at a different time to the obs epoch
        final Gradient delta = obsEpochFieldDate.durationFrom(state.getDate());

        final EstimatedMeasurement<RangeRate> estimated;
        final Gradient tauD;
        final EstimatedMeasurement<RangeRate> evalOneWay1;

        if (twoway) {
            final Gradient tauU;
            final EstimatedMeasurement<RangeRate> evalOneWay2;

            if (timeTagSpecificationType == TimeTagSpecificationType.TX) {
                //Date = epoch of transmission.
                //Vary position of receiver -> in case of uplink leg, receiver is satellite
                tauU = signalTimeOfFlightFixedEmission(pvaDS, stationObsEpoch.getPosition(), stationObsEpoch.getDate());
                //Get state at transit

                final Gradient deltaMTauU = tauU.add(delta);
                final TimeStampedFieldPVCoordinates<Gradient> transitPV = pvaDS.shiftedBy(deltaMTauU);
                final SpacecraftState transitState = state.shiftedBy(deltaMTauU.getValue());

                evalOneWay2 = oneWayTheoreticalEvaluation(iteration, evaluation, false, stationObsEpoch, transitPV, transitState, indices, nbParams);

                //Get station at transit - although this is effectively an initial seed for fitting the downlink delay
                final TimeStampedFieldPVCoordinates<Gradient> stationTransit = stationObsEpoch.shiftedBy(deltaMTauU);

                //project time of flight forwards with 0 offset.
                tauD = signalTimeOfFlightFixedEmission(stationTransit, transitPV.getPosition(), transitPV.getDate());

                evalOneWay1 = oneWayTheoreticalEvaluation(iteration, evaluation, true, stationTransit.shiftedBy(tauD), transitPV, transitState, indices, nbParams);

            } else if (timeTagSpecificationType == TimeTagSpecificationType.TRANSIT) {
                final TimeStampedFieldPVCoordinates<Gradient> transitPV = pvaDS.shiftedBy(delta);
                final SpacecraftState transitState = state.shiftedBy(delta.getValue());

                //In transit obs case, do not correct the value for motion during time of flight.
                final Gradient transitRangeRate = getRangeRateValue(stationObsEpoch, transitPV);

                //Calculate time of flight for return measurement participants
                tauD = signalTimeOfFlightFixedEmission(stationObsEpoch, transitPV.getPosition(), transitPV.getDate());
                tauU = signalTimeOfFlight(stationObsEpoch, transitPV.getPosition(), transitPV.getDate());

                //shift station forwards to get downlink
                evalOneWay1 = oneWayTheoreticalEvaluation(iteration, evaluation, true, stationObsEpoch.shiftedBy(tauD), transitPV, transitState, indices, nbParams);
                evalOneWay1.setEstimatedValue(transitRangeRate.getValue());
                //shift station backwards to get uplink
                evalOneWay2 = oneWayTheoreticalEvaluation(iteration, evaluation, false, stationObsEpoch.shiftedBy(tauU.negate()), transitPV, transitState, indices, nbParams);
                evalOneWay2.setEstimatedValue(transitRangeRate.getValue());
            }
            else {
                // Compute propagation times
                // (if state has already been set up to pre-compensate propagation delay,
                //  we will have delta == tauD and transitState will be the same as state)

                // Downlink delay
                tauD = signalTimeOfFlight(pvaDS, stationObsEpoch.getPosition(), obsEpochFieldDate);

                // Transit state
                final Gradient        deltaMTauD   = tauD.negate().add(delta);
                final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());

                // Transit state (re)computed with gradients
                final TimeStampedFieldPVCoordinates<Gradient> transitPV = pvaDS.shiftedBy(deltaMTauD);

                evalOneWay1 =
                        oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                stationObsEpoch, transitPV, transitState, indices, nbParams);

                // one-way (uplink) light time correction
                final FieldTransform<Gradient> offsetToInertialApproxUplink =
                        station.getOffsetToInertial(state.getFrame(), obsEpochFieldDate.shiftedBy(tauD.multiply(-2)),
                                nbParams, indices);
                final FieldAbsoluteDate<Gradient> approxUplinkDateDS = offsetToInertialApproxUplink.getFieldDate();

                final TimeStampedFieldPVCoordinates<Gradient> stationApproxUplink =
                        offsetToInertialApproxUplink.transformPVCoordinates(
                                new TimeStampedFieldPVCoordinates<>(approxUplinkDateDS, zero, zero,
                                        zero));

                tauU =
                        signalTimeOfFlight(stationApproxUplink, transitPV.getPosition(), transitPV.getDate());

                final TimeStampedFieldPVCoordinates<Gradient> stationUplink = stationApproxUplink
                        .shiftedBy(transitPV.getDate().durationFrom(approxUplinkDateDS).subtract(tauU));

                evalOneWay2 =
                        oneWayTheoreticalEvaluation(iteration, evaluation, false, stationUplink,
                                transitPV, transitState, indices, nbParams);

            }
            // combine uplink and downlink values
            estimated = new EstimatedMeasurement<>(this, iteration, evaluation, evalOneWay1.getStates(),
                    new TimeStampedPVCoordinates[] {evalOneWay2.getParticipants()[0],
                    evalOneWay1.getParticipants()[0],
                    evalOneWay1.getParticipants()[1]});
            estimated.setEstimatedValue(0.5 * (evalOneWay1.getEstimatedValue()[0] + evalOneWay2.getEstimatedValue()[0]));

            // combine uplink and downlink partial derivatives with respect to state
            final double[][] sd1 = evalOneWay1.getStateDerivatives(0);
            final double[][] sd2 = evalOneWay2.getStateDerivatives(0);
            final double[][] sd = new double[sd1.length][sd1[0].length];
            for (int i = 0; i < sd.length; ++i)
            {
                for (int j = 0; j < sd[0].length; ++j)
                {
                    sd[i][j] = 0.5 * (sd1[i][j] + sd2[i][j]);
                }
            }
            estimated.setStateDerivatives(0, sd);

            // combine uplink and downlink partial derivatives with respect to parameters
            evalOneWay1.getDerivativesDrivers().forEach(driver ->
            {
                final double[] pd1 = evalOneWay1.getParameterDerivatives(
                        driver);
                final double[] pd2 = evalOneWay2.getParameterDerivatives(
                        driver);
                final double[] pd = new double[pd1.length];
                for (int i = 0; i < pd.length; ++i)
                {
                    pd[i] = 0.5 * (pd1[i] + pd2[i]);
                }
                estimated.setParameterDerivatives(
                        driver, pd);
            });
        } else {

            // Compute propagation times
            // (if state has already been set up to pre-compensate propagation delay,
            //  we will have delta == tauD and transitState will be the same as state)

            // Downlink delay
            tauD = signalTimeOfFlight(pvaDS, stationObsEpoch.getPosition(), obsEpochFieldDate);

            // Transit state
            final Gradient        deltaMTauD   = tauD.negate().add(delta);
            final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());

            // Transit state (re)computed with gradients
            final TimeStampedFieldPVCoordinates<Gradient> transitPV = pvaDS.shiftedBy(deltaMTauD);

            // one-way (downlink) range-rate
            evalOneWay1 =
                    oneWayTheoreticalEvaluation(iteration, evaluation, true,
                            stationObsEpoch, transitPV, transitState, indices, nbParams);


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
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     * //@see #evaluate(SpacecraftStatet)
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
                        new SpacecraftState[] {transitState},
                        new TimeStampedPVCoordinates[] {
                        (downlink ? transitPV : stationPV).toTimeStampedPVCoordinates(),
                        (downlink ? stationPV : transitPV).toTimeStampedPVCoordinates()
                        });

        // range rate
        Gradient rangeRate = getRangeRateValue(stationPV, transitPV);

        if (!twoway) {
            // clock drifts, taken in account only in case of one way
            final ObservableSatellite satellite    = getSatellites().get(0);
            final Gradient            dtsDot       = satellite.getClockDriftDriver().getValue(nbParams, indices);
            final Gradient            dtgDot       = station.getClockDriftDriver().getValue(nbParams, indices);

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
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index]);
            }
        }

        return estimated;

    }

    private static Gradient getRangeRateValue(final TimeStampedFieldPVCoordinates<Gradient> stationPV, final TimeStampedFieldPVCoordinates<Gradient> transitPV) {
        // range rate value
        final FieldVector3D<Gradient> stationPosition  = stationPV.getPosition();
        final FieldVector3D<Gradient> relativePosition = stationPosition.subtract(transitPV.getPosition());

        final FieldVector3D<Gradient> stationVelocity  = stationPV.getVelocity();
        final FieldVector3D<Gradient> relativeVelocity = stationVelocity.subtract(transitPV.getVelocity());

        // radial direction
        final FieldVector3D<Gradient> lineOfSight      = relativePosition.normalize();

        // line of sight velocity
        return FieldVector3D.dotProduct(relativeVelocity, lineOfSight);
    }

}
