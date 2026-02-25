/* Copyright 2002-2026 CS GROUP
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS range measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range measurement between a GNSS emitter
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
public class OneWayGNSSRange extends AbstractOneWayGNSS<OneWayGNSSRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSRange";

    /** Simple constructor.
     * @param observer object that sends GNSS signal
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     * @since 12.1
     */
    public OneWayGNSSRange(final Observer observer,
                           final AbsoluteDate date,
                           final double range, final double sigma,
                           final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(observer, date, range, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), local);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                final int evaluation,
                                                                                                final SpacecraftState[] states) {

        final CommonParametersWithoutDerivatives common =
            computeLocalParametersWithout(states, getSatellites().get(0), getDate());

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
        final double range = (common.getTauD() + common.getLocalOffset().getOffset() -
                              common.getRemoteOffset().getOffset()) *
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

        final CommonParametersWithDerivatives common =
            computeLocalParametersWith(states, getSatellites().get(0), getDate());

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
        final Gradient range            = common.getTauD().add(common.getLocalOffset().getOffset()).
                                          subtract(common.getRemoteOffset().getOffset()).
                                          multiply(Constants.SPEED_OF_LIGHT);
        fillDerivatives(range, common.getIndices(), estimatedRange);

        // Return the estimated measurement
        return estimatedRange;

    }

}
