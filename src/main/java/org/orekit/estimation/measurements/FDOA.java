/* Copyright 2002-2023 Mark Rutten
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
     * @param centreFrequency satellite emitter frequency
     * @param date date of the measurement
     * @param fdoa observed value (s)
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

        final SpacecraftState state = states[0];

        // coordinates of the spacecraft
        final TimeStampedPVCoordinates pva = state.getPVCoordinates();

        // transform between prime station frame and inertial frame
        // at the real date of measurement, i.e. taking station clock offset into account
        final Transform primeToInert = getStation().getOffsetToInertial(state.getFrame(), getDate(), false);
        final AbsoluteDate measurementDate = primeToInert.getDate();

        // prime station PV in inertial frame at the real date of the measurement
        final TimeStampedPVCoordinates primePV =
                        primeToInert.transformPVCoordinates(new TimeStampedPVCoordinates(measurementDate,
                                                                                         Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));

        // compute downlink delay from emitter to prime receiver
        final double tau1 = signalTimeOfFlight(pva, primePV.getPosition(), measurementDate);

        // elapsed time between state date and signal arrival to the prime receiver
        final double dtMtau1 = measurementDate.durationFrom(state.getDate()) - tau1;

        // satellite state at signal emission
        final SpacecraftState emitterState = state.shiftedBy(dtMtau1);

        // satellite pv at signal emission (re)computed with gradient
        final TimeStampedPVCoordinates emitterPV = pva.shiftedBy(dtMtau1);

        // second station PV in inertial frame at real date of signal reception
        TimeStampedPVCoordinates secondPV;
        // initialize search loop of the reception date by second station
        double tau2 = tau1;
        double delta;
        int count = 0;
        do {
            final double previous = tau2;
            // date of signal arrival on second receiver
            final AbsoluteDate dateAt2 = emitterState.getDate().shiftedBy(previous);
            // transform between second station frame and inertial frame
            // at the date of signal arrival, taking clock offset into account
            final Transform secondToInert = secondStation.getOffsetToInertial(state.getFrame(), dateAt2, false);
            // second receiver position in inertial frame at the real date of signal reception
            secondPV = secondToInert.transformPVCoordinates(new TimeStampedPVCoordinates(secondToInert.getDate(),
                                                                                         Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO));
            // downlink delay from emitter to second receiver
            tau2 = linkDelay(emitterPV.getPosition(), secondPV.getPosition());

            // Change in the computed downlink delay
            delta = FastMath.abs(tau2 - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(tau2));

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final double offset1 = getStation().getClockOffsetDriver().getValue(emitterState.getDate());
        final double offset2 = secondStation.getClockOffsetDriver().getValue(emitterState.getDate());
        final double tdoa    = (tau1 + offset1) - (tau2 + offset2);

        // Range-rate sat->primary station
        final EstimatedMeasurementBase<FDOA> evalPrimary = oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                                                       primePV, emitterPV, emitterState);

        // Range-rate sat->secondary station
        final EstimatedMeasurementBase<FDOA> evalSecondary = oneWayTheoreticalEvaluation(iteration, evaluation, true,
                                                                                         secondPV, emitterPV, emitterState);

        // Evaluate the FDOA value and derivatives
        // -------------------------------------------
        final EstimatedMeasurementBase<FDOA> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           emitterState
                                                       },
                                                       new TimeStampedPVCoordinates[] {
                                                           emitterPV,
                                                           tdoa > 0 ? secondPV : primePV,
                                                           tdoa > 0 ? primePV : secondPV
                                                       });

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue((evalPrimary.getEstimatedValue()[0] - evalSecondary.getEstimatedValue()[0]) * rangeRateToHz);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<FDOA> theoreticalEvaluation(final int iteration, final int evaluation,
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

        // transform between prime station frame and inertial frame
        // at the real date of measurement, i.e. taking station clock offset into account
        final FieldTransform<Gradient> primeToInert =
                        getStation().getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
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
        final Gradient offset1 = getStation().getClockOffsetDriver().getValue(nbParams, indices, emitterState.getDate());
        final Gradient offset2 = secondStation.getClockOffsetDriver().getValue(nbParams, indices, emitterState.getDate());
        final Gradient tdoaG   = tau1.add(offset1).subtract(tau2.add(offset2));
        final double tdoa      = tdoaG.getValue();

        // Range-rate sat->primary station
        final EstimatedMeasurement<FDOA> evalPrimary = oneWayTheoreticalEvaluation(iteration, evaluation, true,
                primePV, emitterPV, emitterState, indices);

        // Range-rate sat->secondary station
        final EstimatedMeasurement<FDOA> evalSecondary = oneWayTheoreticalEvaluation(iteration, evaluation, true,
                secondPV, emitterPV, emitterState, indices);

        // Evaluate the FDOA value and derivatives
        // -------------------------------------------
        final TimeStampedPVCoordinates pv1 = primePV.toTimeStampedPVCoordinates();
        final TimeStampedPVCoordinates pv2 = secondPV.toTimeStampedPVCoordinates();
        final EstimatedMeasurement<FDOA> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       emitterState
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       emitterPV.toTimeStampedPVCoordinates(),
                                                       tdoa > 0 ? pv2 : pv1,
                                                       tdoa > 0 ? pv1 : pv2
                                                   });

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue((evalPrimary.getEstimatedValue()[0] - evalSecondary.getEstimatedValue()[0]) * rangeRateToHz);

        // combine primary and secondary partial derivatives with respect to state
        final double[][] sd1 = evalPrimary.getStateDerivatives(0);
        final double[][] sd2 = evalSecondary.getStateDerivatives(0);
        final double[][] sd  = new double[sd1.length][sd1[0].length];
        for (int i = 0; i < sd.length; ++i) {
            for (int j = 0; j < sd[0].length; ++j) {
                sd[i][j] = (sd1[i][j] - sd2[i][j]) * rangeRateToHz;
            }
        }
        estimated.setStateDerivatives(0, sd);

        // combine primary and secondary partial derivatives with respect to parameters
        evalPrimary.getDerivativesDrivers().forEach(driver -> {
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final double[] pd1 = evalPrimary.getParameterDerivatives(driver, span.getStart());
                final double[] pd2 = evalSecondary.getParameterDerivatives(driver, span.getStart());
                final double[] pd  = new double[pd1.length];
                for (int i = 0; i < pd.length; ++i) {
                    pd[i] = (pd1[i] - pd2[i]) * rangeRateToHz;
                }
                estimated.setParameterDerivatives(driver, span.getStart(), pd);
            }
        });

        return estimated;

    }

    /** Compute propagation delay on a link.
     * @param emitter  the position of the emitter
     * @param receiver the position of the receiver (same frame as emitter)
     * @return the propagation delay
     */
    private double linkDelay(final Vector3D emitter,
                               final Vector3D receiver) {
        return receiver.distance(emitter) / Constants.SPEED_OF_LIGHT;
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

    /** Evaluate range rate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param downlink indicator for downlink leg
     * @param stationPV station coordinates when signal is at station
     * @param transitPV spacecraft coordinates at onboard signal transit
     * @param transitState orbital state at onboard signal transit
     * @return theoretical value for the current leg
     */
    private EstimatedMeasurementBase<FDOA> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                       final TimeStampedPVCoordinates stationPV,
                                                                       final TimeStampedPVCoordinates transitPV,
                                                                       final SpacecraftState transitState) {

        // prepare the evaluation
        final EstimatedMeasurementBase<FDOA> estimated =
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
    private EstimatedMeasurement<FDOA> oneWayTheoreticalEvaluation(final int iteration, final int evaluation, final boolean downlink,
                                                                                final TimeStampedFieldPVCoordinates<Gradient> stationPV,
                                                                                final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                                                                final SpacecraftState transitState,
                                                                                final Map<String, Integer> indices) {

        // prepare the evaluation
        final EstimatedMeasurement<FDOA> estimated =
            new EstimatedMeasurement<>(this, iteration, evaluation,
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
