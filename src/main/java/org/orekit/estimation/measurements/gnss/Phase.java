/* Copyright 2002-2020 CS GROUP
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
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
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
public class Phase extends AbstractMeasurement<Phase> {

    /** Name for ambiguity driver. */
    public static final String AMBIGUITY_NAME = "ambiguity";

    /** Driver for ambiguity. */
    private final ParameterDriver ambiguityDriver;

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

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
        super(date, phase, sigma, baseWeight, Arrays.asList(satellite));
        ambiguityDriver = new ParameterDriver(AMBIGUITY_NAME,
                                               0.0, 1.0,
                                               Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        addParameterDriver(ambiguityDriver);
        addParameterDriver(satellite.getClockOffsetDriver());
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        this.station    = station;
        this.wavelength = wavelength;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
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
        final FieldTransform<Gradient> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> downlinkDateDS =
                        offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final Gradient tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state & Transit state (re)computed with gradients
        final Gradient        delta        = downlinkDateDS.durationFrom(state.getDate());
        final Gradient        deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());
        final TimeStampedFieldPVCoordinates<Gradient> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

        // prepare the evaluation
        final EstimatedMeasurement<Phase> estimated =
                        new EstimatedMeasurement<Phase>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            transitState
                                                        }, new TimeStampedPVCoordinates[] {
                                                            transitStateDS.toTimeStampedPVCoordinates(),
                                                            stationDownlink.toTimeStampedPVCoordinates()
                                                        });

        // Clock offsets
        final ObservableSatellite satellite = getSatellites().get(0);
        final Gradient            dts       = satellite.getClockOffsetDriver().getValue(nbParams, indices);
        final Gradient            dtg       = station.getClockOffsetDriver().getValue(nbParams, indices);

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity   = ambiguityDriver.getValue(nbParams, indices);
        final Gradient phase       = tauD.add(dtg).subtract(dts).multiply(cOverLambda).add(ambiguity);

        estimated.setEstimatedValue(phase.getValue());

        // Phase partial derivatives with respect to state
        final double[] derivatives = phase.getGradient();
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

}
