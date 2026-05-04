/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS range rate measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range rate measurement between a GNSS emitter
 * and a LEO satellite (receiver).
 * <p>
 * The one-way GNSS range rate measurement assumes knowledge of the orbit and
 * the clock offset of the emitting GNSS satellite. For instance, it is
 * possible to use a SP3 file or a GNSS navigation message to recover
 * the satellite's orbit and clock.
 * <p>
 * This class is very similar to {@link InterSatellitesOneWayRangeRate} measurement
 * class. However, using the one-way GNSS range measurement, the orbit and clock
 * of the emitting GNSS satellite are <b>NOT</b> estimated simultaneously with
 * LEO satellite coordinates.
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class OneWayGNSSRangeRate extends AbstractOneWayGNSS<OneWayGNSSRangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSRangeRate";

    /** Simple constructor.
     * @param observer object that sends GNSS signal
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     * @since 12.1
     */
    public OneWayGNSSRangeRate(final Observer observer,
                               final AbsoluteDate date,
                               final double rangeRate, final double sigma,
                               final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(observer, date, rangeRate, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), local);
    }

    /** Simple constructor.
     * @param observer object that sends GNSS signal
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param local satellite which receives the signal and perform the measurement
     * @since 14.0
     */
    public OneWayGNSSRangeRate(final Observer observer, final AbsoluteDate date,
                               final double rangeRate, final MeasurementQuality measurementQuality,
                               final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite local) {
        // Call super constructor
        super(observer, date, rangeRate, measurementQuality, signalTravelTimeModel, local);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSRangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                    final int evaluation,
                                                                                                    final SpacecraftState[] states) {

        final CommonParametersWithoutDerivatives common =
            computeLocalParametersWithout(states, getSatellites().getFirst(), getDate());

        // Estimated measurement
        final EstimatedMeasurementBase<OneWayGNSSRangeRate> estimatedRangeRate =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getRemotePV(),
                                                           common.getTransitPV()
                                                       });

        // Range rate value
        final PVCoordinates delta = new PVCoordinates(common.getRemotePV(), common.getTransitPV());
        final double rangeRate = Vector3D.dotProduct(delta.getVelocity(), delta.getPosition().normalize()) +
                                 Constants.SPEED_OF_LIGHT * (common.getLocalOffset().getRate() - common.getRemoteOffset().getRate());

        // Set value of the estimated measurement
        estimatedRangeRate.setEstimatedValue(rangeRate);

        // Return the estimated measurement
        return estimatedRangeRate;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<OneWayGNSSRangeRate> theoreticalEvaluation(final int iteration,
                                                                              final int evaluation,
                                                                              final SpacecraftState[] states) {

        final CommonParametersWithDerivatives common =
            computeLocalParametersWith(states, getSatellites().getFirst(), getDate());

        // Estimated measurement
        final EstimatedMeasurement<OneWayGNSSRangeRate> estimatedRangeRate =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getState()
                                                   }, new TimeStampedPVCoordinates[] {
                                                       common.getRemotePV().toTimeStampedPVCoordinates(),
                                                       common.getTransitPV().toTimeStampedPVCoordinates()
                                                   });

        // Range rate value
        final FieldPVCoordinates<Gradient> delta = new FieldPVCoordinates<>(common.getRemotePV(), common.getTransitPV());
        final Gradient rangeRate = FieldVector3D.dotProduct(delta.getVelocity(), delta.getPosition().normalize()).
                                   add(common.getLocalOffset().getRate().subtract(common.getRemoteOffset().getRate()).multiply(Constants.SPEED_OF_LIGHT));

        fillDerivatives(rangeRate, common.getIndices(), estimatedRangeRate);

        // Return the estimated measurement
        return estimatedRangeRate;

    }

}
