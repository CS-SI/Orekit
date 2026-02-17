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
import org.orekit.estimation.measurements.CommonParametersWithDerivatives;
import org.orekit.estimation.measurements.CommonParametersWithoutDerivatives;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS phase measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a phase measurement between a GNSS emitter
 * and a LEO satellite (receiver).
 * <p>
 * The one-way GNSS phase measurement assumes knowledge of the orbit and
 * the clock offset of the emitting GNSS satellite. For instance, it is
 * possible to use a SP3 file or a GNSS navigation message to recover
 * the satellite's orbit and clock.
 * <p>
 * This class is very similar to {@link InterSatellitesPhase} measurement
 * class. However, using the one-way GNSS phase measurement, the orbit and clock
 * of the emitting GNSS satellite are <b>NOT</b> estimated simultaneously with
 * LEO satellite coordinates.
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OneWayGNSSPhase extends AbstractOneWayGNSS<OneWayGNSSPhase> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSPhase";

    /** Driver for ambiguity. */
    private final AmbiguityDriver ambiguityDriver;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Simple constructor.
     * @param observer object that sends signal
     * @param date date of the measurement
     * @param phase observed value, in cycles
     * @param wavelength phase observed value wavelength, in meters
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public OneWayGNSSPhase(final Observer observer,
                           final AbsoluteDate date,
                           final double phase, final double wavelength, final double sigma,
                           final double baseWeight, final ObservableSatellite local,
                           final AmbiguityCache cache) {
        // Call super constructor
        super(observer, date, phase, sigma, baseWeight, new SignalTravelTimeModel(), local);

        // Initialize phase ambiguity driver
        ambiguityDriver = cache.getAmbiguity(observer.getName(), local.getName(), wavelength);

        // Add ambiguity parameter to measurement
        addParameterDriver(ambiguityDriver);

        // Initialise fields
        this.wavelength = wavelength;
    }

    /** Get the wavelength.
     * @return wavelength (m)
     */
    public double getWavelength() {
        return wavelength;
    }

    /** Get the driver for phase ambiguity.
     * @return the driver for phase ambiguity
     */
    public AmbiguityDriver getAmbiguityDriver() {
        return ambiguityDriver;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSPhase> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                final int evaluation,
                                                                                                final SpacecraftState[] states) {

        final CommonParametersWithoutDerivatives common = getObserver().
            computeLocalParametersWithout(states, getSatellites().get(0), getDate(), false);

        // prepare the evaluation
        final EstimatedMeasurementBase<OneWayGNSSPhase> estimatedPhase =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getRemotePV(),
                                                           common.getTransitPV()
                                                       });

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double   ambiguity   = ambiguityDriver.getValue(common.getState().getDate());
        final double   phase       = (common.getTauD() + common.getLocalOffset().getOffset() -
                                      common.getRemoteOffset().getOffset()) * cOverLambda + ambiguity;

        // Set value of the estimated measurement
        estimatedPhase.setEstimatedValue(phase);

        // Return the estimated measurement
        return estimatedPhase;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<OneWayGNSSPhase> theoreticalEvaluation(final int iteration,
                                                                          final int evaluation,
                                                                          final SpacecraftState[] states) {

        final CommonParametersWithDerivatives common = getObserver().
            computeLocalParametersWith(states, getSatellites().get(0), getDate(), false, getParametersDrivers());

        // prepare the evaluation
        final EstimatedMeasurement<OneWayGNSSPhase> estimatedPhase =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getState()
                                                   }, new TimeStampedPVCoordinates[] {
                                                     common.getRemotePV().toTimeStampedPVCoordinates(),
                                                     common.getTransitPV().toTimeStampedPVCoordinates()
                                                   });

        // Phase value
        final double   cOverLambda      = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity        = ambiguityDriver.getValue(common.getTauD().getFreeParameters(), common.getIndices(),
                                                                   common.getState().getDate());
        final Gradient phase            = common.getTauD().add(common.getLocalOffset().getOffset()).
                                          subtract(common.getRemoteOffset().getOffset()).
                                          multiply(cOverLambda).add(ambiguity);

        // Return the estimated measurement
        fillDerivatives(phase, common.getIndices(), estimatedPhase);
        return estimatedPhase;

    }

}
