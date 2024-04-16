/* Copyright 2002-2024 Thales Alenia Space
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

import java.util.Arrays;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS range rate measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range rate measurement between a GNSS satellite (emitter)
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
public class OneWayGNSSRangeRate extends AbstractOneWayGNSSMeasurement<OneWayGNSSRangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSRangeRate";

    /** Simple constructor.
     * @param remote provider for GNSS satellite which simply emits the signal
     * @param dtRemote clock offset of the GNSS satellite, in seconds
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     */
    public OneWayGNSSRangeRate(final PVCoordinatesProvider remote,
                               final double dtRemote,
                               final AbsoluteDate date,
                               final double rangeRate, final double sigma,
                               final double baseWeight, final ObservableSatellite local) {
        this(remote, new QuadraticClockModel(date, dtRemote, 0.0, 0.0), date, rangeRate, sigma, baseWeight, local);
    }

    /** Simple constructor.
     * @param remote provider for GNSS satellite which simply emits the signal
     * @param remoteClock clock offset of the GNSS satellite
     * @param date date of the measurement
     * @param rangeRate observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     * @since 12.1
     */
    public OneWayGNSSRangeRate(final PVCoordinatesProvider remote,
                               final QuadraticClockModel remoteClock,
                               final AbsoluteDate date,
                               final double rangeRate, final double sigma,
                               final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(remote, remoteClock, date, rangeRate, sigma, baseWeight, local);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSRangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                    final int evaluation,
                                                                                                    final SpacecraftState[] states) {


        final OnBoardCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states, false);

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
                                 Constants.SPEED_OF_LIGHT * (common.getLocalRate() - common.getRemoteRate());

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

        final OnBoardCommonParametersWithDerivatives common = computeCommonParametersWith(states, false);

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
                                   add(common.getLocalRate().subtract(common.getRemoteRate()).multiply(Constants.SPEED_OF_LIGHT));
        final double[] rangeRateDerivatives = rangeRate.getGradient();

        // Set value and state first order derivatives of the estimated measurement
        estimatedRangeRate.setEstimatedValue(rangeRate.getValue());
        estimatedRangeRate.setStateDerivatives(0, Arrays.copyOfRange(rangeRateDerivatives, 0,  6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver measurementDriver : getParametersDrivers()) {
            for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimatedRangeRate.setParameterDerivatives(measurementDriver, span.getStart(), rangeRateDerivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedRangeRate;

    }

}
