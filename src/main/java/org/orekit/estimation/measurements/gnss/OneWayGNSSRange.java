/* Copyright 2002-2024 CS GROUP
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS range measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range measurement between a GNSS satellite (emitter)
 * and a LEO satellite (receiver).
 * <p>
 * The one-way GNSS range measurement assumes knowledge of the orbit and
 * the clock offset of the emitting GNSS satellite. For instance, it is
 * possible to use a SP3 file or a GNSS navigation message to recover
 * the satellite's orbit and clock.
 * <p>
 * This class is very similar to {@link InterSatellitesRange} measurement
 * class. However, using the one-way GNSS range measurement, the orbit and clock
 * of the emitting GNSS satellite are <b>NOT</b> estimated simultaneously with
 * LEO satellite coordinates.
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OneWayGNSSRange extends AbstractOneWayGNSSMeasurement<OneWayGNSSRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSRange";

    /** Simple constructor.
     * @param remote provider for GNSS satellite which simply emits the signal
     * @param dtRemote clock offset of the GNSS satellite, in seconds
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     */
    public OneWayGNSSRange(final PVCoordinatesProvider remote,
                           final double dtRemote,
                           final AbsoluteDate date,
                           final double range, final double sigma,
                           final double baseWeight, final ObservableSatellite local) {
        this(remote, new QuadraticClockModel(date, dtRemote, 0.0, 0.0), date, range, sigma, baseWeight, local);
    }

    /** Simple constructor.
     * @param remote provider for GNSS satellite which simply emits the signal
     * @param remoteClock clock offset of the GNSS satellite
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     * @since 12.1
     */
    public OneWayGNSSRange(final PVCoordinatesProvider remote,
                           final QuadraticClockModel remoteClock,
                           final AbsoluteDate date,
                           final double range, final double sigma,
                           final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(remote, remoteClock, date, range, sigma, baseWeight, local);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                final int evaluation,
                                                                                                final SpacecraftState[] states) {


        final OnBoardCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states, false);

        // Estimated measurement
        final EstimatedMeasurementBase<OneWayGNSSRange> estimatedRange =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getRemotePV(),
                                                           common.getTransitPV()
                                                       });

        // Range value
        final double range = (common.getTauD() + common.getLocalOffset() - common.getRemoteOffset()) *
                             Constants.SPEED_OF_LIGHT;

        // Set value of the estimated measurement
        estimatedRange.setEstimatedValue(range);

        // Return the estimated measurement
        return estimatedRange;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<OneWayGNSSRange> theoreticalEvaluation(final int iteration,
                                                                          final int evaluation,
                                                                          final SpacecraftState[] states) {

        final OnBoardCommonParametersWithDerivatives common = computeCommonParametersWith(states, false);

        // Estimated measurement
        final EstimatedMeasurement<OneWayGNSSRange> estimatedRange =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getState()
                                                   }, new TimeStampedPVCoordinates[] {
                                                       common.getRemotePV().toTimeStampedPVCoordinates(),
                                                       common.getTransitPV().toTimeStampedPVCoordinates()
                                                   });

        // Range value
        final Gradient range            = common.getTauD().add(common.getLocalOffset()).subtract(common.getRemoteOffset()).
                                          multiply(Constants.SPEED_OF_LIGHT);
        final double[] rangeDerivatives = range.getGradient();

        // Set value and state first order derivatives of the estimated measurement
        estimatedRange.setEstimatedValue(range.getValue());
        estimatedRange.setStateDerivatives(0, Arrays.copyOfRange(rangeDerivatives, 0,  6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver measurementDriver : getParametersDrivers()) {
            for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimatedRange.setParameterDerivatives(measurementDriver, span.getStart(), rangeDerivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedRange;

    }

}
