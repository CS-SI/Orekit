/* Copyright 2002-2025 CS GROUP
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
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Phase measurement between two satellites.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a remote satellite and received by a local satellite.
 * Its value is the number of cycles between emission and reception.
 * The motion of both spacecraft during the signal flight time
 * are taken into account. The date of the measurement corresponds to the
 * reception on ground of the emitted signal.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class InterSatellitesPhase extends AbstractInterSatellitesMeasurement<InterSatellitesPhase> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "InterSatellitesPhase";

    /** Driver for ambiguity. */
    private final AmbiguityDriver ambiguityDriver;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote remote satellite which simply emits the signal
     * @param date date of the measurement
     * @param phase observed value (cycles)
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public InterSatellitesPhase(final ObservableSatellite local,
                                final ObservableSatellite remote,
                                final AbsoluteDate date, final double phase,
                                final double wavelength, final double sigma,
                                final double baseWeight,
                                final AmbiguityCache cache) {
        // Call to super constructor
        super(date, phase, sigma, baseWeight, local, remote);

        // Initialize phase ambiguity driver
        ambiguityDriver = cache.getAmbiguity(remote.getName(), local.getName(), wavelength);

        // Add parameter drivers
        addParameterDriver(ambiguityDriver);

        // Initialize fields
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
    public ParameterDriver getAmbiguityDriver() {
        return ambiguityDriver;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<InterSatellitesPhase> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                     final int evaluation,
                                                                                                     final SpacecraftState[] states) {

        final OnBoardCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states, false);

        // prepare the evaluation
        final EstimatedMeasurementBase<InterSatellitesPhase> estimatedPhase =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getState(),
                                                           states[1]
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getRemotePV(),
                                                           common.getTransitPV()
                                                       });

        // Phase value
        final double cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double ambiguity   = ambiguityDriver.getValue(common.getState().getDate());
        final double phase       = (common.getTauD() + common.getLocalOffset() - common.getRemoteOffset()) * cOverLambda +
                                   ambiguity;

        estimatedPhase.setEstimatedValue(phase);

        // Return the estimated measurement
        return estimatedPhase;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesPhase> theoreticalEvaluation(final int iteration,
                                                                               final int evaluation,
                                                                               final SpacecraftState[] states) {

        final OnBoardCommonParametersWithDerivatives common = computeCommonParametersWith(states, false);

       // prepare the evaluation
        final EstimatedMeasurement<InterSatellitesPhase> estimatedPhase =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getState(),
                                                       states[1]
                                                   }, new TimeStampedPVCoordinates[] {
                                                       common.getRemotePV().toTimeStampedPVCoordinates(),
                                                       common.getTransitPV().toTimeStampedPVCoordinates()
                                                   });

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity   = ambiguityDriver.getValue(common.getTauD().getFreeParameters(), common.getIndices(),
                                                              common.getState().getDate());
        final Gradient phase       = common.getTauD().add(common.getLocalOffset()).subtract(common.getRemoteOffset()).
                                     multiply(cOverLambda).
                                     add(ambiguity);

        estimatedPhase.setEstimatedValue(phase.getValue());

        // Range first order derivatives with respect to states
        final double[] derivatives = phase.getGradient();
        estimatedPhase.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));
        estimatedPhase.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 6, 12));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimatedPhase.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedPhase;

    }

}
