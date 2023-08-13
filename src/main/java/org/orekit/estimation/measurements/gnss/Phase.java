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

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundReceiverCommonParametersWithDerivatives;
import org.orekit.estimation.measurements.GroundReceiverCommonParametersWithoutDerivatives;
import org.orekit.estimation.measurements.GroundReceiverMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a phase measurement from a ground station.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a spacecraft and received on a ground station.
 * Its value is the number of cycles between emission and
 * reception. The motion of both the station and the
 * spacecraft during the signal flight time are taken into
 * account. The date of the measurement corresponds to the
 * reception on ground of the emitted signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public class Phase extends GroundReceiverMeasurement<Phase> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "Phase";

    /** Name for ambiguity driver. */
    public static final String AMBIGUITY_NAME = "ambiguity";

    /** Driver for ambiguity. */
    private final ParameterDriver ambiguityDriver;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param phase observed value (cycles)
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Phase(final GroundStation station, final AbsoluteDate date,
                 final double phase, final double wavelength, final double sigma,
                 final double baseWeight, final ObservableSatellite satellite) {
        super(station, false, date, phase, sigma, baseWeight, satellite);
        ambiguityDriver = new ParameterDriver(AMBIGUITY_NAME,
                                               0.0, 1.0,
                                               Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        addParameterDriver(ambiguityDriver);
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
     * @since 10.3
     */
    public ParameterDriver getAmbiguityDriver() {
        return ambiguityDriver;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<Phase> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                      final int evaluation,
                                                                                      final SpacecraftState[] states) {

        final GroundReceiverCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states[0]);

        // prepare the evaluation
        final EstimatedMeasurementBase<Phase> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getTransitState()
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getTransitPV(),
                                                           common.getStationDownlink()
                                                       });

        // Clock offsets
        final ObservableSatellite satellite = getSatellites().get(0);
        final double              dts       = satellite.getClockOffsetDriver().getValue(common.getState().getDate());
        final double              dtg       = getStation().getClockOffsetDriver().getValue(getDate());

        // Phase value
        final double cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double ambiguity   = ambiguityDriver.getValue(common.getState().getDate());
        final double phase       = (common.getTauD() + dtg - dts) * cOverLambda + ambiguity;

        estimated.setEstimatedValue(phase);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Phase> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Phase derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (ambiguity, clock offset, station offsets, pole, prime meridian...)
        final GroundReceiverCommonParametersWithDerivatives common = computeCommonParametersWithDerivatives(state);
        final int nbParams = common.getTauD().getFreeParameters();

        // prepare the evaluation
        final EstimatedMeasurement<Phase> estimated =
                        new EstimatedMeasurement<Phase>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            common.getTransitState()
                                                        }, new TimeStampedPVCoordinates[] {
                                                            common.getTransitPV().toTimeStampedPVCoordinates(),
                                                            common.getStationDownlink().toTimeStampedPVCoordinates()
                                                        });

        // Clock offsets
        final ObservableSatellite satellite = getSatellites().get(0);
        final Gradient            dts       = satellite.getClockOffsetDriver().getValue(nbParams, common.getIndices(), state.getDate());
        final Gradient            dtg       = getStation().getClockOffsetDriver().getValue(nbParams, common.getIndices(), getDate());

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity   = ambiguityDriver.getValue(nbParams, common.getIndices(), state.getDate());
        final Gradient phase       = common.getTauD().add(dtg).subtract(dts).multiply(cOverLambda).add(ambiguity);

        estimated.setEstimatedValue(phase.getValue());

        // Phase partial derivatives with respect to state
        final double[] derivatives = phase.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        return estimated;

    }

}
