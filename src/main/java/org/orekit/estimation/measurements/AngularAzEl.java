/* Copyright 2002-2022 CS GROUP
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * The motion of the spacecraft during the signal flight time is taken into
 * account. The date of the measurement corresponds to the reception on
 * ground of the reflected signal.
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularAzEl extends AbstractMeasurement<AngularAzEl>
{

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Enum indicating the time tag specification of a range observation. */
    private final TimeTagSpecificationType timeTagSpecificationType;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the timetag configuration of the provided angular AzEl observation
     * @since xx.xx
     */
    public AngularAzEl(final GroundStation station, final AbsoluteDate date,
                       final double[] angular, final double[] sigma, final double[] baseWeight,
                       final ObservableSatellite satellite, final TimeTagSpecificationType timeTagSpecificationType) {
        super(date, angular, sigma, baseWeight, Collections.singletonList(satellite));
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
        this.station = station;
        this.timeTagSpecificationType = timeTagSpecificationType;
    }


    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public AngularAzEl(final GroundStation station, final AbsoluteDate date,
                       final double[] angular, final double[] sigma, final double[] baseWeight,
                       final ObservableSatellite satellite) {
        this(station, date, angular, sigma, baseWeight, satellite, TimeTagSpecificationType.RX);
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<AngularAzEl> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                      final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Azimuth/elevation derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, station offsets, pole, prime meridian...)

        // Get the number of parameters used for derivation
        // Place the selected drivers into a map
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final Field<Gradient>         field   = GradientField.getField(nbParams);
        final FieldVector3D<Gradient> zero    = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pvaDS = getCoordinates(state, 0, nbParams);

        // Transform between station and inertial frame, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialObsEpoch =
                station.getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> obsEpochFieldDate =
                offsetToInertialObsEpoch.getFieldDate();

        // Station position/velocity in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationObsEpoch =
                offsetToInertialObsEpoch.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(obsEpochFieldDate,
                        zero, zero, zero));


        final Gradient delta = obsEpochFieldDate.durationFrom(state.getDate());

        final TimeStampedFieldPVCoordinates<Gradient> transitStateDS;
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink;

        /* The station position for relative position vector calculation - set to downlink for transmit and
         * receive. For transit/bounce time tag specification we use the station at bounce time */
        final TimeStampedFieldPVCoordinates<Gradient> stationPositionEstimated;

        final SpacecraftState transitState;
        final Gradient tauD;

        if (timeTagSpecificationType == TimeTagSpecificationType.TX) {
            //Date = epoch of transmission.
            //Vary position of receiver -> in case of uplink leg, receiver is satellite
            final Gradient tauU = signalTimeOfFlightFixedEmission(pvaDS, stationObsEpoch.getPosition(), stationObsEpoch.getDate());
            final Gradient deltaMTauU = tauU.add(delta);
            //Get state at transit
            transitStateDS = pvaDS.shiftedBy(deltaMTauU);
            transitState = state.shiftedBy(deltaMTauU.getValue());

            //Get station at transit - although this is effectively an initial seed for fitting the downlink delay
            final TimeStampedFieldPVCoordinates<Gradient> stationTransit = stationObsEpoch.shiftedBy(tauU);

            //project time of flight forwards with 0 offset.
            tauD = signalTimeOfFlightFixedEmission(stationTransit, transitStateDS.getPosition(), transitStateDS.getDate());

            stationDownlink = stationObsEpoch.shiftedBy(tauU.add(tauD));
            stationPositionEstimated = stationDownlink;
        }

        else if (timeTagSpecificationType == TimeTagSpecificationType.TRANSIT) {
            transitStateDS = pvaDS.shiftedBy(delta);
            transitState = state.shiftedBy(delta.getValue());

            tauD = signalTimeOfFlightFixedEmission(stationObsEpoch, transitStateDS.getPosition(), transitStateDS.getDate());

            stationDownlink = stationObsEpoch.shiftedBy(tauD);
            stationPositionEstimated = stationObsEpoch;
        }

        else {
            // Compute propagation times
            // (if state has already been set up to pre-compensate propagation delay,
            //  we will have delta == tauD and transitState will be the same as state)

            // Downlink delay
            tauD = signalTimeOfFlight(pvaDS, stationObsEpoch.getPosition(), obsEpochFieldDate);

            // Transit state
            final Gradient deltaMTauD = tauD.negate().add(delta);
            transitState = state.shiftedBy(deltaMTauD.getValue());

            // Transit state (re)computed with gradients
            transitStateDS = pvaDS.shiftedBy(deltaMTauD);
            stationDownlink = stationObsEpoch;
            stationPositionEstimated = stationDownlink;
        }

        final FieldTransform<Gradient> offsetToInertialEstimationTime = station.getOffsetToInertial(state.getFrame(), stationPositionEstimated.getDate(), nbParams, indices);

        // Station topocentric frame (east-north-zenith) in inertial frame expressed as Gradient
        final FieldVector3D<Gradient> east   = offsetToInertialEstimationTime.transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<Gradient> north  = offsetToInertialEstimationTime.transformVector(FieldVector3D.getPlusJ(field));
        final FieldVector3D<Gradient> zenith = offsetToInertialEstimationTime.transformVector(FieldVector3D.getPlusK(field));

        // Station-satellite vector expressed in inertial frame
        final FieldVector3D<Gradient> staSat = transitStateDS.getPosition().subtract(stationPositionEstimated.getPosition());

        // Compute azimuth/elevation
        final Gradient baseAzimuth = staSat.dotProduct(east).atan2(staSat.dotProduct(north));
        final double   twoPiWrap   = MathUtils.normalizeAngle(baseAzimuth.getReal(), getObservedValue()[0]) -
                baseAzimuth.getReal();
        final Gradient azimuth     = baseAzimuth.add(twoPiWrap);
        final Gradient elevation   = staSat.dotProduct(zenith).divide(staSat.getNorm()).asin();

        // Prepare the estimation
        final EstimatedMeasurement<AngularAzEl> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { transitState },
                        new TimeStampedPVCoordinates[] {
                        transitStateDS.toTimeStampedPVCoordinates(),
                        stationDownlink.toTimeStampedPVCoordinates()
                        });

        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth.getValue(), elevation.getValue());

        // Partial derivatives of azimuth/elevation with respect to state
        // (beware element at index 0 is the value, not a derivative)
        final double[] azDerivatives = azimuth.getGradient();
        final double[] elDerivatives = elevation.getGradient();
        estimated.setStateDerivatives(0,
                Arrays.copyOfRange(azDerivatives, 0, 6), Arrays.copyOfRange(elDerivatives, 0, 6));

        // Set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, azDerivatives[index], elDerivatives[index]);
            }
        }

        return estimated;
    }
}
