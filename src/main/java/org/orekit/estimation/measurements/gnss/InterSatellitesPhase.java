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
package org.orekit.estimation.measurements.gnss;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Phase measurement between two satellites.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a remote satellite and received by a local satellite.
 * Its value is the number of cycles between emission and reception.
 * The motion of both spacecrafts during the signal flight time
 * are taken into account. The date of the measurement corresponds to the
 * reception on ground of the emitted signal.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class InterSatellitesPhase extends AbstractMeasurement<InterSatellitesPhase> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "InterSatellitesPhase";

    /** Name for ambiguity driver. */
    public static final String AMBIGUITY_NAME = "ambiguity";

    /** Driver for ambiguity. */
    private final ParameterDriver ambiguityDriver;

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
     */
    public InterSatellitesPhase(final ObservableSatellite local,
                                final ObservableSatellite remote,
                                final AbsoluteDate date, final double phase,
                                final double wavelength, final double sigma,
                                final double baseWeight) {
        // Call to super constructor
        super(date, phase, sigma, baseWeight, Arrays.asList(local, remote));

        // Initialize phase ambiguity driver
        ambiguityDriver = new ParameterDriver(AMBIGUITY_NAME, 0.0, 1.0,
                                              Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // Add parameter drivers
        addParameterDriver(ambiguityDriver);
        addParameterDriver(local.getClockOffsetDriver());
        addParameterDriver(remote.getClockOffsetDriver());

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

        // Coordinates of both satellites
        final SpacecraftState local = states[0];
        final TimeStampedPVCoordinates pvaL = local.getPVCoordinates();
        final SpacecraftState remote = states[1];
        final TimeStampedPVCoordinates pvaR = remote.getPVCoordinates();

        // Compute propagation times
        // Downlink delay
        final double dtl = getSatellites().get(0).getClockOffsetDriver().getValue(AbsoluteDate.ARBITRARY_EPOCH);
        final AbsoluteDate arrivalDate = getDate().shiftedBy(-dtl);

        final TimeStampedPVCoordinates s1Downlink = pvaL.shiftedBy(arrivalDate.durationFrom(pvaL.getDate()));
        final double tauD = signalTimeOfFlight(pvaR, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double delta      = getDate().durationFrom(remote.getDate());
        final double deltaMTauD = delta - tauD;

        // prepare the evaluation
        final EstimatedMeasurementBase<InterSatellitesPhase> estimatedPhase =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           local.shiftedBy(deltaMTauD),
                                                           remote.shiftedBy(deltaMTauD)
                                                       }, new TimeStampedPVCoordinates[] {
                                                           remote.shiftedBy(delta - tauD).getPVCoordinates(),
                                                           local.shiftedBy(delta).getPVCoordinates()
                                                       });

        // Clock offsets
        final double dtr = getSatellites().get(1).getClockOffsetDriver().getValue(AbsoluteDate.ARBITRARY_EPOCH);

        // Phase value
        final double cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double ambiguity   = ambiguityDriver.getValue(AbsoluteDate.ARBITRARY_EPOCH);
        final double phase       = (tauD + dtl - dtr) * cOverLambda + ambiguity;

        estimatedPhase.setEstimatedValue(phase);

        // Return the estimated measurement
        return estimatedPhase;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesPhase> theoreticalEvaluation(final int iteration,
                                                                               final int evaluation,
                                                                               final SpacecraftState[] states) {

        // Phase derivatives are computed with respect to spacecrafts states in inertial frame
        // ----------------------
        //
        // Parameters:
        //  - 0..2  - Position of the receiver satellite in inertial frame
        //  - 3..5  - Velocity of the receiver satellite in inertial frame
        //  - 6..8  - Position of the remote satellite in inertial frame
        //  - 9..11 - Velocity of the remote satellite in inertial frame
        //  - 12..  - Measurement parameters: ambiguity, local clock offset, remote clock offset...
        int nbParams = 12;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver phaseMeasurementDriver : getParametersDrivers()) {
            if (phaseMeasurementDriver.isSelected()) {
                for (Span<String> span = phaseMeasurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                    indices.put(span.getData(), nbParams++);
                }
            }
        }

        // Coordinates of both satellites
        final SpacecraftState local = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pvaL = getCoordinates(local, 0, nbParams);
        final SpacecraftState remote = states[1];
        final TimeStampedFieldPVCoordinates<Gradient> pvaR = getCoordinates(remote, 6, nbParams);

        // Compute propagation times
        // Downlink delay
        final Gradient dtl = getSatellites().get(0).getClockOffsetDriver().getValue(nbParams, indices, AbsoluteDate.ARBITRARY_EPOCH);
        final FieldAbsoluteDate<Gradient> arrivalDate = new FieldAbsoluteDate<>(getDate(), dtl.negate());

        final TimeStampedFieldPVCoordinates<Gradient> s1Downlink =
                        pvaL.shiftedBy(arrivalDate.durationFrom(pvaL.getDate()));
        final Gradient tauD = signalTimeOfFlight(pvaR, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double   delta      = getDate().durationFrom(remote.getDate());
        final Gradient deltaMTauD = tauD.negate().add(delta);

        // prepare the evaluation
        final EstimatedMeasurement<InterSatellitesPhase> estimatedPhase =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       local.shiftedBy(deltaMTauD.getValue()),
                                                       remote.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       remote.shiftedBy(delta - tauD.getValue()).getPVCoordinates(),
                                                       local.shiftedBy(delta).getPVCoordinates()
                                                   });

        // Clock offsets
        final Gradient dtr = getSatellites().get(1).getClockOffsetDriver().getValue(nbParams, indices, AbsoluteDate.ARBITRARY_EPOCH);

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity   = ambiguityDriver.getValue(nbParams, indices, AbsoluteDate.ARBITRARY_EPOCH);
        final Gradient phase       = tauD.add(dtl).subtract(dtr).multiply(cOverLambda).add(ambiguity);

        estimatedPhase.setEstimatedValue(phase.getValue());

        // Range partial derivatives with respect to states
        final double[] derivatives = phase.getGradient();
        estimatedPhase.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));
        estimatedPhase.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 6, 12));

        // Set partial derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = indices.get(span.getData());
                if (index != null) {
                    estimatedPhase.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedPhase;

    }

}
